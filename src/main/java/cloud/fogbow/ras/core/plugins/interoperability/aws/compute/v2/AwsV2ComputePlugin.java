package cloud.fogbow.ras.core.plugins.interoperability.aws.compute.v2;

import java.util.ArrayList;
import java.util.List;
import java.util.Properties;
import java.util.TreeSet;

import org.apache.log4j.Logger;

import cloud.fogbow.common.exceptions.FogbowException;
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
import cloud.fogbow.ras.core.plugins.interoperability.aws.AwsV2StateMapper;
import cloud.fogbow.ras.core.plugins.interoperability.util.DefaultLaunchCommandGenerator;
import cloud.fogbow.ras.core.plugins.interoperability.util.LaunchCommandGenerator;
import software.amazon.awssdk.services.ec2.Ec2Client;
import software.amazon.awssdk.services.ec2.model.DescribeInstancesRequest;
import software.amazon.awssdk.services.ec2.model.DescribeInstancesResponse;
import software.amazon.awssdk.services.ec2.model.Instance;
import software.amazon.awssdk.services.ec2.model.InstanceNetworkInterfaceSpecification;
import software.amazon.awssdk.services.ec2.model.InstanceType;
import software.amazon.awssdk.services.ec2.model.Reservation;
import software.amazon.awssdk.services.ec2.model.RunInstancesRequest;
import software.amazon.awssdk.services.ec2.model.RunInstancesResponse;

public class AwsV2ComputePlugin implements ComputePlugin<AwsV2User> {

    private static final Logger LOGGER = Logger.getLogger(AwsV2ComputePlugin.class);
	private static final int FIRST_POSITION = 0;
	
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
		
		InstanceNetworkInterfaceSpecification networkSpecification = InstanceNetworkInterfaceSpecification.builder()
				.deviceIndex(FIRST_POSITION) // The first position of the network interface in the attachment order.
				.subnetId(this.subnetId)
				.groups(this.securityGroup)
				.build();

		String userData = this.launchCommandGenerator.createLaunchCommand(computeOrder);

		RunInstancesRequest request = RunInstancesRequest.builder()
				.imageId(imageId)
				.instanceType(InstanceType.T1_MICRO)
				.maxCount(1)
				.minCount(1)
				.networkInterfaces(networkSpecification)
				.userData(userData)
				.build();

		Ec2Client client = AwsV2ClientUtil.createEc2Client(cloudUser.getToken(), this.region);
		RunInstancesResponse response = client.runInstances(request);
		
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
    	DescribeInstancesResponse response = client.describeInstances(request);
    	ComputeInstance computeInstance = mountComputeInstance(response);
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
    
    protected ComputeInstance mountComputeInstance(DescribeInstancesResponse response) {
    	String id = null;
    	String cloudState = null;
    	String name = null;
    	int cpu = 0;
    	int memory = 0;
    	int disk = 0;
    	List<String> ipAddresses = null;
    	List<UserData> userData = null;
    	String imageId = null;
    	String publicKey = null;
    	
		if (!response.reservations().isEmpty()) {
			Reservation reservation = response.reservations().get(FIRST_POSITION);
			Instance instance = reservation.instances().get(FIRST_POSITION);
			id = instance.instanceId();
			cloudState = instance.state().nameAsString();
			cpu = instance.cpuOptions().coreCount();
			memory = getRamDiskValue(instance);
			disk = getVolumeSize(instance);
			ipAddresses = getIpAddresses(instance);
			return new ComputeInstance(id, cloudState, name, cpu, memory, disk, ipAddresses, imageId, publicKey, userData);
		}
    	// TODO Auto-generated method stub
		return null;
	}

	private List<String> getIpAddresses(Instance instance) {
		List<String> ipAddresses = new ArrayList<String>();
		for (int i = 0; i < instance.networkInterfaces().size(); i++) {
			ipAddresses.add(instance.networkInterfaces().get(i).privateIpAddress());
		}
		return ipAddresses;
	}

	protected int getVolumeSize(Instance instance) {
		// get all volumeId under blockDeviceMappings
		String volumeId = instance.blockDeviceMappings().get(FIRST_POSITION).ebs().volumeId();
		// describe all volumes passing all volumeIds
		// for each volume get size value
		return 0;
	}

	protected int getRamDiskValue(Instance instance) {
		String ramDiskId = instance.ramdiskId();
		return 0;
	}

}
