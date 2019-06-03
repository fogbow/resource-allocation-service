package cloud.fogbow.ras.core.plugins.interoperability.aws.compute.v2;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Properties;
import java.util.TreeSet;
import java.util.UUID;
import java.util.stream.Collectors;

import org.apache.log4j.Logger;

import cloud.fogbow.common.exceptions.ConfigurationErrorException;
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
import cloud.fogbow.ras.core.models.ResourceType;
import cloud.fogbow.ras.core.models.orders.ComputeOrder;
import cloud.fogbow.ras.core.plugins.interoperability.ComputePlugin;
import cloud.fogbow.ras.core.plugins.interoperability.aws.AwsV2ClientUtil;
import cloud.fogbow.ras.core.plugins.interoperability.aws.AwsV2ConfigurationPropertyKeys;
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
import software.amazon.awssdk.services.ec2.model.DescribeVolumesRequest;
import software.amazon.awssdk.services.ec2.model.DescribeVolumesResponse;
import software.amazon.awssdk.services.ec2.model.Image;
import software.amazon.awssdk.services.ec2.model.Instance;
import software.amazon.awssdk.services.ec2.model.InstanceNetworkInterfaceSpecification;
import software.amazon.awssdk.services.ec2.model.InstanceType;
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
	private static final String COMMENTED_LINE_PREFIX = "#";
	private static final String CSV_COLUMN_SEPARATOR = ",";
	
	private static final int INSTANCE_TYPE_COLUMN = 0;
	private static final int VCPU_COLUMN = 1;
	private static final int MEMORY_COLUMN = 2;
	private static final int STORAGE_COLUMN = 3;
	private static final int PROCESSOR_COLUMN = 4;
	private static final int NETWORK_PERFORMANCE_COLUMN = 5;
	private static final int DEDICATED_EBS_BANDWIDTH_COLUMN = 6;
	
	protected static final String BANDWIDTH_REQUIREMENT = "bandwidth";
	protected static final String PERFORMANCE_REQUIREMENT = "performance";
	protected static final String PROCESSOR_REQUIREMENT = "processor";
	protected static final String STORAGE_REQUIREMENT = "storage";

	protected static final int FIRST_POSITION = 0;
	protected static final int INSTANCES_LAUNCH_NUMBER = 1;
	protected static final int ONE_GIGABYTE = 1024;

	private TreeSet<AwsHardwareRequirements> flavors;
	private LaunchCommandGenerator launchCommandGenerator;
	private String region;
	private String subnetId;
	private String securityGroup;
	private String flavorsFilePath;

	public AwsV2ComputePlugin(String confFilePath) {
		Properties properties = PropertiesUtil.readProperties(confFilePath);
		this.region = properties.getProperty(AwsV2ConfigurationPropertyKeys.AWS_REGION_SELECTION_KEY);
		this.subnetId = properties.getProperty(AwsV2ConfigurationPropertyKeys.AWS_SUBNET_ID_KEY);
		this.securityGroup = properties.getProperty(AwsV2ConfigurationPropertyKeys.AWS_DEFAULT_VPC_SECURITY_GROUP_KEY);
		this.flavorsFilePath = properties.getProperty(AwsV2ConfigurationPropertyKeys.AWS_FLAVORS_TYPES_FILE_PATH_KEY);
		this.launchCommandGenerator = new DefaultLaunchCommandGenerator();
		this.flavors = new TreeSet<AwsHardwareRequirements>();
	}
	
	@Override
	public String requestInstance(ComputeOrder computeOrder, AwsV2User cloudUser) throws FogbowException {
		LOGGER.info(String.format(Messages.Info.REQUESTING_INSTANCE, cloudUser.getToken()));

		AwsHardwareRequirements flavor = findSmallestFlavor(computeOrder, cloudUser);
		String imageId = flavor.getImageId();
		InstanceType instanceType = InstanceType.fromValue(flavor.getName());
		List<InstanceNetworkInterfaceSpecification> networkInterfaces = loadNetworkInterfaces(computeOrder);
		String userData = this.launchCommandGenerator.createLaunchCommand(computeOrder);

		RunInstancesRequest instanceRequest = RunInstancesRequest.builder()
				.imageId(imageId)
				.instanceType(instanceType)
				.maxCount(INSTANCES_LAUNCH_NUMBER)
				.minCount(INSTANCES_LAUNCH_NUMBER)
				.networkInterfaces(networkInterfaces)
				.userData(userData)
				.build();

		Ec2Client client = AwsV2ClientUtil.createEc2Client(cloudUser.getToken(), this.region);
		try {
			RunInstancesResponse response = client.runInstances(instanceRequest);
			String instanceId = null;
			if (response != null && !response.instances().isEmpty()) {
				instanceId = response.instances().get(FIRST_POSITION).instanceId();
				CreateTagsRequest tagRequest = createInstanceTagName(computeOrder, instanceId);
				client.createTags(tagRequest);
			} 
			return instanceId;
		} catch (Exception e) {
			throw new UnexpectedException(String.format(Messages.Exception.GENERIC_EXCEPTION, e), e);
		}
	}

	@Override
	public ComputeInstance getInstance(ComputeOrder computeOrder, AwsV2User cloudUser) throws FogbowException {
		LOGGER.info(String.format(Messages.Info.GETTING_INSTANCE, computeOrder.getInstanceId(), cloudUser.getToken()));

		DescribeInstancesRequest request = DescribeInstancesRequest.builder()
				.instanceIds(computeOrder.getInstanceId())
				.build();

		Ec2Client client = AwsV2ClientUtil.createEc2Client(cloudUser.getToken(), this.region);
		try {
			DescribeInstancesResponse response = client.describeInstances(request);
			Instance instance = getInstanceReservation(response);
			List<Volume> volumes = getInstanceVolumes(instance, client);
			ComputeInstance computeInstance = mountComputeInstance(instance, volumes);
			return computeInstance;
		} catch (Exception e) {
			throw new UnexpectedException(String.format(Messages.Exception.GENERIC_EXCEPTION, e), e);
		}
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
		return false;
	}

	protected ComputeInstance mountComputeInstance(Instance instance, List<Volume> volumes)
			throws InstanceNotFoundException {

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
		int memory = 0;
		for (AwsHardwareRequirements flavors : getFlavors()) {
			if (flavors.getName().equals(instanceType.toString())) {
				memory = flavors.getMemory();
			}
		}
		return memory;
	}

	protected List<Volume> getInstanceVolumes(Instance instance, Ec2Client client) {
		List<Volume> volumes = null;
		DescribeVolumesResponse response;
		List<String> volumeIds = getVolumeIds(instance);
		for (String volumeId : volumeIds) {
			response = getDescribeVolume(volumeId, client);
			volumes = response.volumes();

		}
		return volumes;
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
	
	protected List<InstanceNetworkInterfaceSpecification> loadNetworkInterfaces(ComputeOrder computeOrder) {
		List<String> networkIds = getNetworkIds(computeOrder);
		List<InstanceNetworkInterfaceSpecification> networkInterfaces = new ArrayList<>();
		InstanceNetworkInterfaceSpecification nis = null;
		for (String networkId : networkIds) {
			nis = createInstanceNetworkInterface(networkId);
			networkInterfaces.add(nis);
		}
		return networkInterfaces;
	}

	protected List<String> getNetworkIds(ComputeOrder computeOrder) {
		List<String> networkIds = new ArrayList<String>();
		networkIds.add(this.subnetId);
		if (computeOrder.getNetworkIds() != null) {
			networkIds.addAll(computeOrder.getNetworkIds());
		}
		return networkIds;
	}

	protected InstanceNetworkInterfaceSpecification createInstanceNetworkInterface(String networkId) {
		InstanceNetworkInterfaceSpecification networkInterface = InstanceNetworkInterfaceSpecification.builder()
				.subnetId(networkId) // Default sub-net in the available zone of the selected region.
				.deviceIndex(FIRST_POSITION) // The first position of the network interface.
				.groups(this.securityGroup).build();
		
		return networkInterface;
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

	protected AwsHardwareRequirements findSmallestFlavor(ComputeOrder computeOrder, AwsV2User cloudUser)
			throws InvalidParameterException, UnexpectedException, ConfigurationErrorException,
			NoAvailableResourcesException {

		AwsHardwareRequirements bestFlavor = getBestFlavor(computeOrder, cloudUser);
		if (bestFlavor == null) {
			throw new NoAvailableResourcesException(Messages.Exception.NO_MATCHING_FLAVOR);
		}
		return bestFlavor;
	}

	protected AwsHardwareRequirements getBestFlavor(ComputeOrder computeOrder, AwsV2User cloudUser)
			throws InvalidParameterException, UnexpectedException, ConfigurationErrorException {

		updateHardwareRequirements(cloudUser);
		TreeSet<AwsHardwareRequirements> resultset = getFlavorsByRequirements(computeOrder.getRequirements());
		for (AwsHardwareRequirements hardwareRequirements : resultset) {
			if (hardwareRequirements.getCpu() >= computeOrder.getvCPU()
					&& hardwareRequirements.getMemory() >= computeOrder.getMemory()
					&& hardwareRequirements.getDisk() >= computeOrder.getDisk()) {
				return hardwareRequirements;
			}
		}
		return null;
	}

	protected TreeSet<AwsHardwareRequirements> getFlavorsByRequirements(Map<String, String> orderRequirements) {
		TreeSet<AwsHardwareRequirements> resultSet = getFlavors();
		List<AwsHardwareRequirements> resultList = null;
		if (orderRequirements != null && !orderRequirements.isEmpty()) {
			for (Entry<String, String> requirements : orderRequirements.entrySet()) {
				resultList = filterFlavors(requirements);
				if (resultList.size() < resultSet.size()) {
					resultSet = parseToTreeSet(resultList);
				}
			}
		}
		return resultSet;
	}
	
	protected TreeSet<AwsHardwareRequirements> parseToTreeSet(List<AwsHardwareRequirements> list) {
		TreeSet<AwsHardwareRequirements> resultSet = new TreeSet<AwsHardwareRequirements>();
		resultSet.addAll(list);
		return resultSet;
	}

	protected List<AwsHardwareRequirements> filterFlavors(Entry<String, String> requirements) {
		String key = requirements.getKey().trim();
		String value = requirements.getValue().trim();
		return getFlavors().stream()
				.filter(flavor -> flavor.getRequirements().get(key).equalsIgnoreCase(value))
				.collect(Collectors.toList());
	}
	
	protected TreeSet<AwsHardwareRequirements> getFlavors() {
		synchronized (this.flavors) {
			return this.flavors;
		}
	}

	protected void updateHardwareRequirements(AwsV2User cloudUser)
			throws ConfigurationErrorException, InvalidParameterException, UnexpectedException {

		List<String> lines = loadLinesFromFlavorFile();
		String[] requirements = null;
		AwsHardwareRequirements flavor = null;

		Map<String, Integer> imagesMap = getImagesMap(cloudUser);
		for (Entry<String, Integer> images : imagesMap.entrySet()) {
			for (String line : lines) {
				if (!line.startsWith(COMMENTED_LINE_PREFIX)) {
					requirements = line.split(CSV_COLUMN_SEPARATOR);
					flavor = mountHardwareRequirements(images, requirements);
					this.flavors.add(flavor);
				}
			}
		}
	}

	protected AwsHardwareRequirements mountHardwareRequirements(Entry<String, Integer> image, String[] requirements) {
		String name = requirements[INSTANCE_TYPE_COLUMN];
		String flavorId = getRandomUUID();
		int cpu = Integer.parseInt(requirements[VCPU_COLUMN]);
		Double memory = Double.parseDouble(requirements[MEMORY_COLUMN]) * ONE_GIGABYTE;
		int disk = image.getValue();
		String imageId = image.getKey();
		Map<String, String> requirementsMap = loadRequirementsMap(requirements);
		return new AwsHardwareRequirements(name, flavorId, cpu, memory.intValue(), disk, imageId, requirementsMap);
	}

	protected Map<String, String> loadRequirementsMap(String[] requirements) {
		Map<String, String> requirementsMap = new HashMap<String, String>();
		String processor = requirements[PROCESSOR_COLUMN];
		String performance = requirements[NETWORK_PERFORMANCE_COLUMN];
		String storage = requirements[STORAGE_COLUMN];
		String bandwidth = requirements[DEDICATED_EBS_BANDWIDTH_COLUMN];
		requirementsMap.put(PROCESSOR_REQUIREMENT, processor);
		requirementsMap.put(PERFORMANCE_REQUIREMENT, performance);
		requirementsMap.put(STORAGE_REQUIREMENT, storage);
		requirementsMap.put(BANDWIDTH_REQUIREMENT, bandwidth);
		return requirementsMap;
	}

	protected Map<String, Integer> getImagesMap(AwsV2User cloudUser)
			throws InvalidParameterException, UnexpectedException {

		Map<String, Integer> imageMap = new HashMap<String, Integer>();
		DescribeImagesRequest request = DescribeImagesRequest.builder()
				.owners(cloudUser.getId())
				.build();

		Ec2Client client = AwsV2ClientUtil.createEc2Client(cloudUser.getToken(), this.region);
		DescribeImagesResponse response = client.describeImages(request);

		List<Image> images = response.images();
		for (Image image : images) {
			int size = getImageSize(image);
			imageMap.put(image.imageId(), size);
		}
		return imageMap;
	}

	protected List<String> loadLinesFromFlavorFile() throws ConfigurationErrorException {
		Path path = Paths.get(this.flavorsFilePath);
		try {
			return Files.readAllLines(path);
		} catch (IOException e) {
			LOGGER.error(String.format(Messages.Error.ERROR_MESSAGE, e), e);
			throw new ConfigurationErrorException();
		}
	}

	protected int getImageSize(Image image) {
		int size = 0;
		for (BlockDeviceMapping device : image.blockDeviceMappings()) {
			size = device.ebs().volumeSize();
		}
		return size;
	}
	
	// This method is used to aid in the tests
	protected void setFlavorsFilePath(String flavorsFilePath) {
		this.flavorsFilePath = flavorsFilePath;
	}

}