package cloud.fogbow.ras.core.plugins.interoperability.aws.compute.v2;

import java.util.ArrayList;
import java.util.List;
import java.util.Properties;
import java.util.TreeSet;

import org.apache.log4j.Logger;

import cloud.fogbow.common.exceptions.FogbowException;
import cloud.fogbow.common.exceptions.InstanceNotFoundException;
import cloud.fogbow.common.models.AwsV2User;
import cloud.fogbow.common.util.PropertiesUtil;
import cloud.fogbow.ras.api.http.response.ComputeInstance;
import cloud.fogbow.ras.api.http.response.InstanceState;
import cloud.fogbow.ras.constants.Messages;
import cloud.fogbow.ras.core.models.HardwareRequirements;
import cloud.fogbow.ras.core.models.ResourceType;
import cloud.fogbow.ras.core.models.UserData;
import cloud.fogbow.ras.core.models.orders.ComputeOrder;
import cloud.fogbow.ras.core.plugins.interoperability.ComputePlugin;
import cloud.fogbow.ras.core.plugins.interoperability.aws.AwsV2ClientUtil;
import cloud.fogbow.ras.core.plugins.interoperability.aws.AwsV2ConfigurationPropertyKeys;
import cloud.fogbow.ras.core.plugins.interoperability.aws.AwsV2InstanceTypeMapper;
import cloud.fogbow.ras.core.plugins.interoperability.aws.AwsV2StateMapper;
import cloud.fogbow.ras.core.plugins.interoperability.util.DefaultLaunchCommandGenerator;
import cloud.fogbow.ras.core.plugins.interoperability.util.LaunchCommandGenerator;
import software.amazon.awssdk.services.ec2.Ec2Client;
import software.amazon.awssdk.services.ec2.model.DescribeInstancesRequest;
import software.amazon.awssdk.services.ec2.model.DescribeInstancesResponse;
import software.amazon.awssdk.services.ec2.model.DescribeVolumesRequest;
import software.amazon.awssdk.services.ec2.model.DescribeVolumesResponse;
import software.amazon.awssdk.services.ec2.model.Instance;
import software.amazon.awssdk.services.ec2.model.InstanceNetworkInterfaceSpecification;
import software.amazon.awssdk.services.ec2.model.InstanceType;
import software.amazon.awssdk.services.ec2.model.Reservation;
import software.amazon.awssdk.services.ec2.model.RunInstancesRequest;
import software.amazon.awssdk.services.ec2.model.RunInstancesResponse;
import software.amazon.awssdk.services.ec2.model.Volume;

public class AwsV2ComputePlugin implements ComputePlugin<AwsV2User> {

    private static final Logger LOGGER = Logger.getLogger(AwsV2ComputePlugin.class);
	private static final int FIRST_POSITION = 0;
	private static final int ONE_GIGABYTE = 1024;
	
	private TreeSet<HardwareRequirements> flavors;
	private LaunchCommandGenerator launchCommandGenerator;
    private String region;
    private String subnetId;
    private String securityGroup;

    public AwsV2ComputePlugin(String confFilePath) {
        Properties properties = PropertiesUtil.readProperties(confFilePath);
        this.region = properties.getProperty(AwsV2ConfigurationPropertyKeys.AWS_REGION_SELECTION_KEY);
        this.subnetId = properties.getProperty(AwsV2ConfigurationPropertyKeys.AWS_SUBNET_ID_KEY);
        this.securityGroup = properties.getProperty(AwsV2ConfigurationPropertyKeys.AWS_DEFAULT_VPC_SECURITY_GROUP_KEY); 
        this.launchCommandGenerator = new DefaultLaunchCommandGenerator();
        this.flavors = new TreeSet<HardwareRequirements>();
    }

	@Override
	public String requestInstance(ComputeOrder computeOrder, AwsV2User cloudUser) throws FogbowException {
		LOGGER.info(String.format(Messages.Info.REQUESTING_INSTANCE, cloudUser.getToken()));

		// Implements flavors to get an image compatible with the requirements of the order
		String imageId = computeOrder.getImageId();
		
		int cpu = computeOrder.getvCPU();
		double memory = computeOrder.getMemory() / ONE_GIGABYTE; // The AWS cloud calculate this value in gigabytes
		InstanceType instanceType = AwsV2InstanceTypeMapper.map(cpu, memory);
		
		InstanceNetworkInterfaceSpecification networkSpecification = InstanceNetworkInterfaceSpecification.builder()
				.deviceIndex(FIRST_POSITION) // The first position of the network interface in the attachment order.
				.subnetId(this.subnetId)
				.groups(this.securityGroup)
				.build();

		String userData = this.launchCommandGenerator.createLaunchCommand(computeOrder);

		RunInstancesRequest request = RunInstancesRequest.builder()
				.imageId(imageId)
				.instanceType(instanceType)
				.maxCount(1)
				.minCount(1)
				.networkInterfaces(networkSpecification)
				.userData(userData)
				.build();

		Ec2Client client = AwsV2ClientUtil.createEc2Client(cloudUser.getToken(), this.region);
		RunInstancesResponse response = client.runInstances(request);
		
		// Create tag name to define the name of instance
		
		String instanceId = null;
		if (!response.instances().isEmpty()) {
			instanceId = response.instances().get(FIRST_POSITION).instanceId();
		}
		return instanceId;
	}

    @Override
    public ComputeInstance getInstance(ComputeOrder computeOrder, AwsV2User cloudUser) throws FogbowException {
    	LOGGER.info(String.format(Messages.Info.GETTING_INSTANCE, computeOrder.getInstanceId(), cloudUser.getToken()));
    	
    	DescribeInstancesRequest request = DescribeInstancesRequest.builder()
    			.instanceIds(computeOrder.getInstanceId())
    			.build();
    	
    	Ec2Client client = AwsV2ClientUtil.createEc2Client(cloudUser.getToken(), this.region);
    	ComputeInstance computeInstance = mountComputeInstance(request, client);
    	return computeInstance;
    }

	@Override
    public void deleteInstance(ComputeOrder computeOrder, AwsV2User cloudUser) throws FogbowException {
    	throw new FogbowException("This feature has not been implemented for aws cloud, yet.");
    }

    @Override
    public boolean isReady(String instanceState) {
    	return AwsV2StateMapper.map(ResourceType.COMPUTE, instanceState).equals(InstanceState.READY);
    }

    @Override
    public boolean hasFailed(String instanceState) {
    	return AwsV2StateMapper.map(ResourceType.COMPUTE, instanceState).equals(InstanceState.FAILED);
    }
    
	protected ComputeInstance mountComputeInstance(DescribeInstancesRequest request, Ec2Client client)
			throws InstanceNotFoundException {

		DescribeInstancesResponse instancesResponse = client.describeInstances(request);
		Instance instance = getInstanceReservation(instancesResponse);

		List<String> volumeIds = getVolumeIds(instance);
		List<Volume> volumes = getInstanceVolumes(volumeIds, client);

		String id = instance.instanceId();
		String cloudState = instance.state().nameAsString();
		String name = null; // FIXME
		int cpu = instance.cpuOptions().coreCount();
		int memory = getMemoryValueFrom(instance.instanceType());
		int disk = getAllDisksSize(volumes);
		List<String> ipAddresses = getIpAddresses(instance);
		List<UserData> userData = null; // FIXME
		String imageId = instance.imageId();
		String publicKey = null; // FIXME
		return new ComputeInstance(id, cloudState, name, cpu, memory, disk, ipAddresses, imageId, publicKey, userData);
	}

	protected List<String> getIpAddresses(Instance instance) {
		List<String> ipAddresses = new ArrayList<String>();
		for (int i = 0; i < instance.networkInterfaces().size(); i++) {
			ipAddresses.add(instance.networkInterfaces().get(i).privateIpAddress());
		}
		return ipAddresses;
	}

	protected int getAllDisksSize(List<Volume> volumes) {
		int size = 0;
		for (Volume volume : volumes) {
			size += volume.size();
		}
		return size;
	}
	
	protected int getMemoryValueFrom(InstanceType instanceType) {
		Double memory = AwsFlavour.valueOf(instanceType.name()).getMemory() * ONE_GIGABYTE; 
		return memory.intValue();
	}
	
	protected List<Volume> getInstanceVolumes(List<String> volumeIds, Ec2Client client) {
		DescribeVolumesResponse response;
		for (String volumeId : volumeIds) {
			response = getDescribeVolume(volumeId, client);
			return response.volumes();

		}
		return null;
	}

	protected DescribeVolumesResponse getDescribeVolume(String volumeId, Ec2Client client) {
		DescribeVolumesRequest request = DescribeVolumesRequest.builder()
				.volumeIds(volumeId)
				.build();

		return client.describeVolumes(request);
	}

	protected List<String> getVolumeIds(Instance instance) {
		List<String> volumeIds = new ArrayList<String>();
		for (int i = 0; i < instance.blockDeviceMappings().size(); i++) {
			volumeIds.add(instance.blockDeviceMappings().get(i).ebs().volumeId());
		}
		return volumeIds;
	}

	protected Instance getInstanceReservation(DescribeInstancesResponse response) throws InstanceNotFoundException {
		if (!response.reservations().isEmpty()) {
			Reservation reservation = response.reservations().get(FIRST_POSITION);
			if (!reservation.instances().isEmpty()) {
				return reservation.instances().get(FIRST_POSITION);
			}
		}
		throw new InstanceNotFoundException();
	}
	
}
