package cloud.fogbow.ras.core.plugins.interoperability.aws.compute.v2;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Properties;
import java.util.TreeSet;
import java.util.UUID;

import org.apache.log4j.Logger;

import cloud.fogbow.common.exceptions.FogbowException;
import cloud.fogbow.common.exceptions.InstanceNotFoundException;
import cloud.fogbow.common.exceptions.InvalidParameterException;
import cloud.fogbow.common.exceptions.NoAvailableResourcesException;
import cloud.fogbow.common.exceptions.UnexpectedException;
import cloud.fogbow.common.models.AwsV2User;
import cloud.fogbow.common.util.PropertiesUtil;
import cloud.fogbow.ras.api.http.response.ComputeInstance;
import cloud.fogbow.ras.api.http.response.InstanceState;
import cloud.fogbow.ras.constants.Messages;
import cloud.fogbow.ras.constants.SystemConstants;
import cloud.fogbow.ras.core.models.HardwareRequirements;
import cloud.fogbow.ras.core.models.ResourceType;
import cloud.fogbow.ras.core.models.orders.ComputeOrder;
import cloud.fogbow.ras.core.plugins.interoperability.ComputePlugin;
import cloud.fogbow.ras.core.plugins.interoperability.aws.AwsV2ClientUtil;
import cloud.fogbow.ras.core.plugins.interoperability.aws.AwsV2ConfigurationPropertyKeys;
import cloud.fogbow.ras.core.plugins.interoperability.aws.AwsV2InstanceTypeMapper;
import cloud.fogbow.ras.core.plugins.interoperability.aws.AwsV2StateMapper;
import cloud.fogbow.ras.core.plugins.interoperability.util.DefaultLaunchCommandGenerator;
import cloud.fogbow.ras.core.plugins.interoperability.util.LaunchCommandGenerator;
import software.amazon.awssdk.services.ec2.Ec2Client;
import software.amazon.awssdk.services.ec2.model.BlockDeviceMapping;
import software.amazon.awssdk.services.ec2.model.CreateTagsRequest;
import software.amazon.awssdk.services.ec2.model.DescribeImagesRequest;
import software.amazon.awssdk.services.ec2.model.DescribeImagesResponse;
import software.amazon.awssdk.services.ec2.model.DescribeInstancesRequest;
import software.amazon.awssdk.services.ec2.model.DescribeInstancesResponse;
import software.amazon.awssdk.services.ec2.model.DescribeNetworkInterfacesRequest;
import software.amazon.awssdk.services.ec2.model.DescribeNetworkInterfacesResponse;
import software.amazon.awssdk.services.ec2.model.DescribeVolumesRequest;
import software.amazon.awssdk.services.ec2.model.DescribeVolumesResponse;
import software.amazon.awssdk.services.ec2.model.Image;
import software.amazon.awssdk.services.ec2.model.Instance;
import software.amazon.awssdk.services.ec2.model.InstanceNetworkInterfaceSpecification;
import software.amazon.awssdk.services.ec2.model.InstanceType;
import software.amazon.awssdk.services.ec2.model.NetworkInterface;
import software.amazon.awssdk.services.ec2.model.PrivateIpAddressSpecification;
import software.amazon.awssdk.services.ec2.model.Reservation;
import software.amazon.awssdk.services.ec2.model.RunInstancesRequest;
import software.amazon.awssdk.services.ec2.model.RunInstancesResponse;
import software.amazon.awssdk.services.ec2.model.Tag;
import software.amazon.awssdk.services.ec2.model.TerminateInstancesRequest;
import software.amazon.awssdk.services.ec2.model.Volume;

public class AwsV2ComputePlugin implements ComputePlugin<AwsV2User> {

	private static final Logger LOGGER = Logger.getLogger(AwsV2ComputePlugin.class);
	private static final String AWS_TAG_NAME = "Name";
	private static final String RESOURCE_NAME = "Compute";
	private static final int FIRST_POSITION = 0;
	private static final int INSTANCES_LAUNCH_NUMBER = 1;
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

		HardwareRequirements flavour = findSmallestFlavour(computeOrder, cloudUser);
		String imageId = flavour.getFlavorId();
		InstanceType instanceType = selectInstanceTypeBy(flavour);

		List<PrivateIpAddressSpecification> ipAddresses = getIpAddressesSpecificationFrom(computeOrder, cloudUser);

		InstanceNetworkInterfaceSpecification networkInterface = InstanceNetworkInterfaceSpecification.builder()
				.subnetId(this.subnetId) // Default subnet in the available zone of the selected region.
				.deviceIndex(FIRST_POSITION) // The first position of the network interface.
				.groups(this.securityGroup)
				.privateIpAddresses(ipAddresses)
				.build();

		String userData = this.launchCommandGenerator.createLaunchCommand(computeOrder);

		RunInstancesRequest instanceRequest = RunInstancesRequest.builder()
				.imageId(imageId)
				.instanceType(instanceType)
				.maxCount(INSTANCES_LAUNCH_NUMBER)
				.minCount(INSTANCES_LAUNCH_NUMBER)
				.networkInterfaces(networkInterface)
				.userData(userData)
				.build();

		Ec2Client client = AwsV2ClientUtil.createEc2Client(cloudUser.getToken(), this.region);
		RunInstancesResponse response = client.runInstances(instanceRequest);

		String instanceId = null;
		if (!response.instances().isEmpty()) {
			instanceId = response.instances().get(FIRST_POSITION).instanceId();
			CreateTagsRequest tagRequest = createInstanceTagName(computeOrder, instanceId);
			client.createTags(tagRequest);
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
		LOGGER.info(String.format(Messages.Info.DELETING_INSTANCE, computeOrder.getInstanceId(), cloudUser.getToken()));

		String instanceId = computeOrder.getInstanceId();
		TerminateInstancesRequest request = TerminateInstancesRequest.builder()
				.instanceIds(instanceId)
				.build();

		Ec2Client client = AwsV2ClientUtil.createEc2Client(cloudUser.getToken(), this.region);
		try {
			client.terminateInstances(request);
		} catch (Exception e) {
			LOGGER.error(String.format(Messages.Error.ERROR_WHILE_REMOVING_RESOURCE, RESOURCE_NAME, instanceId), e);
			throw new UnexpectedException();
		}
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

		DescribeInstancesResponse response = client.describeInstances(request);
		Instance instance = getInstanceReservation(response);

		List<String> volumeIds = getVolumeIds(instance);
		List<Volume> volumes = getInstanceVolumes(volumeIds, client);

		String id = instance.instanceId();
		String cloudState = instance.state().nameAsString();
		String name = instance.tags().get(FIRST_POSITION).value();
		int cpu = instance.cpuOptions().coreCount();
		int memory = getMemoryValueFrom(instance.instanceType());
		int disk = getAllDisksSize(volumes);
		List<String> ipAddresses = getIpAddresses(instance);
		return new ComputeInstance(id, cloudState, name, cpu, memory, disk, ipAddresses);
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

	protected CreateTagsRequest createInstanceTagName(ComputeOrder computeOrder, String instanceId) {
		String name = defineInstanceName(computeOrder.getName());

		Tag tagName = Tag.builder()
				.key(AWS_TAG_NAME)
				.value(name)
				.build();

		CreateTagsRequest tagRequest = CreateTagsRequest.builder()
				.resources(instanceId)
				.tags(tagName)
				.build();

		return tagRequest;
	}

	protected String defineInstanceName(String instanceName) {
		return instanceName == null ? SystemConstants.FOGBOW_INSTANCE_NAME_PREFIX + getRandomUUID() : instanceName;
	}

	// This method is used to aid in the tests
	protected String getRandomUUID() {
		return UUID.randomUUID().toString();
	}

	protected List<PrivateIpAddressSpecification> getIpAddressesSpecificationFrom(ComputeOrder computeOrder,
			AwsV2User cloudUser) throws InvalidParameterException, UnexpectedException {

		List<PrivateIpAddressSpecification> ipAddressList = new ArrayList<PrivateIpAddressSpecification>();

		PrivateIpAddressSpecification ipAddress;
		for (String networkId : computeOrder.getNetworkIds()) {
			ipAddress = loadPrivateIpAddress(networkId, cloudUser);
			if (ipAddress != null) {
				ipAddressList.add(ipAddress);
			}
		}
		return ipAddressList;
	}

	protected PrivateIpAddressSpecification loadPrivateIpAddress(String networkId, AwsV2User cloudUser)
			throws InvalidParameterException, UnexpectedException {

		String ipAddress;
		List<NetworkInterface> networkInterfaceList = getNetworkInterfaces(networkId, cloudUser);
		if (!networkInterfaceList.isEmpty()) {
			ipAddress = networkInterfaceList.get(FIRST_POSITION).privateIpAddress();
			return PrivateIpAddressSpecification.builder()
					.privateIpAddress(ipAddress)
					.build();
		}
		return null;
	}

	protected List<NetworkInterface> getNetworkInterfaces(String networkId, AwsV2User cloudUser)
			throws InvalidParameterException, UnexpectedException {

		DescribeNetworkInterfacesRequest request = DescribeNetworkInterfacesRequest.builder()
				.networkInterfaceIds(networkId)
				.build();

		Ec2Client client = AwsV2ClientUtil.createEc2Client(cloudUser.getToken(), this.region);
		DescribeNetworkInterfacesResponse response = client.describeNetworkInterfaces(request);
		return response.networkInterfaces();
	}

	protected InstanceType selectInstanceTypeBy(HardwareRequirements flavour) throws NoAvailableResourcesException {
		int cpu = flavour.getCpu();
		double memory = (double) flavour.getMemory() / ONE_GIGABYTE; // AWS calculate memory value in gigabytes
		return AwsV2InstanceTypeMapper.map(cpu, memory);
	}

	protected HardwareRequirements findSmallestFlavour(ComputeOrder computeOrder, AwsV2User cloudUser)
			throws NoAvailableResourcesException, InvalidParameterException, UnexpectedException {

		HardwareRequirements bestFlavor = getBestFlavor(computeOrder, cloudUser);
		if (bestFlavor == null) {
			throw new NoAvailableResourcesException();
		}
		return bestFlavor;
	}

	protected HardwareRequirements getBestFlavor(ComputeOrder computeOrder, AwsV2User cloudUser)
			throws InvalidParameterException, UnexpectedException {

		updateHardwareRequirements(cloudUser);
		for (HardwareRequirements hardwareRequirements : getFlavors()) {
			if (hardwareRequirements.getCpu() >= computeOrder.getvCPU()
					&& hardwareRequirements.getMemory() >= computeOrder.getMemory()
					&& hardwareRequirements.getDisk() >= computeOrder.getDisk()) {
				return hardwareRequirements;
			}
		}
		return null;
	}

	protected TreeSet<HardwareRequirements> getFlavors() {
		synchronized (this.flavors) {
			return this.flavors;
		}
	}

	protected void updateHardwareRequirements(AwsV2User cloudUser)
			throws InvalidParameterException, UnexpectedException {

		HardwareRequirements flavour = null;
		Map<String, Integer> imagesMap = getImagesMap(cloudUser);
		for (Entry<String, Integer> image : imagesMap.entrySet()) {
			for (AwsFlavour awsflavour : AwsFlavour.values()) {
				flavour = mountFlavour(image, awsflavour);
				this.flavors.add(flavour);
			}
		}
	}

	protected HardwareRequirements mountFlavour(Entry<String, Integer> image, AwsFlavour awsflavour) {
		String name = awsflavour.getName();
		String flavourId = image.getKey();
		int cpu = awsflavour.getVCpu();
		Double memory = awsflavour.getMemory() * ONE_GIGABYTE; // AWS calculate memory value in gigabytes
		int disk = image.getValue();
		return new HardwareRequirements(name, flavourId, cpu, memory.intValue(), disk);
	}

	protected Map<String, Integer> getImagesMap(AwsV2User cloudUser)
			throws InvalidParameterException, UnexpectedException {

		Map<String, Integer> imageMap = new HashMap<String, Integer>();
		DescribeImagesRequest request = DescribeImagesRequest.builder().owners(cloudUser.getId()).build();

		Ec2Client client = AwsV2ClientUtil.createEc2Client(cloudUser.getToken(), this.region);
		DescribeImagesResponse response = client.describeImages(request);

		List<Image> images = response.images();
		for (Image image : images) {
			int size = getImageSize(image);
			imageMap.put(image.imageId(), size);
		}
		return imageMap;
	}

	protected int getImageSize(Image image) {
		int size = 0;
		for (BlockDeviceMapping device : image.blockDeviceMappings()) {
			size = device.ebs().volumeSize();
		}
		return size;
	}

	public void setLaunchCommandGenerator(LaunchCommandGenerator launchCommandGenerator) {
		this.launchCommandGenerator = launchCommandGenerator;
	}

}