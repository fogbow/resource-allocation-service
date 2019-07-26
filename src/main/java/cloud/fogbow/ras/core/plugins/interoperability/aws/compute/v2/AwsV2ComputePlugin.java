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
import cloud.fogbow.ras.api.http.response.quotas.allocation.ComputeAllocation;
import cloud.fogbow.ras.constants.Messages;
import cloud.fogbow.ras.core.models.ResourceType;
import cloud.fogbow.ras.core.models.orders.ComputeOrder;
import cloud.fogbow.ras.core.plugins.interoperability.ComputePlugin;
import cloud.fogbow.ras.core.plugins.interoperability.aws.AwsV2ClientUtil;
import cloud.fogbow.ras.core.plugins.interoperability.aws.AwsV2CloudUtil;
import cloud.fogbow.ras.core.plugins.interoperability.aws.AwsV2ConfigurationPropertyKeys;
import cloud.fogbow.ras.core.plugins.interoperability.aws.AwsV2StateMapper;
import cloud.fogbow.ras.core.plugins.interoperability.util.DefaultLaunchCommandGenerator;
import cloud.fogbow.ras.core.plugins.interoperability.util.FogbowCloudUtil;
import cloud.fogbow.ras.core.plugins.interoperability.util.LaunchCommandGenerator;
import software.amazon.awssdk.core.exception.SdkException;
import software.amazon.awssdk.services.ec2.Ec2Client;
import software.amazon.awssdk.services.ec2.model.BlockDeviceMapping;
import software.amazon.awssdk.services.ec2.model.DescribeImagesRequest;
import software.amazon.awssdk.services.ec2.model.DescribeImagesResponse;
import software.amazon.awssdk.services.ec2.model.DescribeInstancesRequest;
import software.amazon.awssdk.services.ec2.model.DescribeInstancesResponse;
import software.amazon.awssdk.services.ec2.model.DescribeVolumesRequest;
import software.amazon.awssdk.services.ec2.model.DescribeVolumesResponse;
import software.amazon.awssdk.services.ec2.model.Image;
import software.amazon.awssdk.services.ec2.model.Instance;
import software.amazon.awssdk.services.ec2.model.InstanceNetworkInterfaceAssociation;
import software.amazon.awssdk.services.ec2.model.InstanceNetworkInterfaceSpecification;
import software.amazon.awssdk.services.ec2.model.InstancePrivateIpAddress;
import software.amazon.awssdk.services.ec2.model.InstanceType;
import software.amazon.awssdk.services.ec2.model.Reservation;
import software.amazon.awssdk.services.ec2.model.RunInstancesRequest;
import software.amazon.awssdk.services.ec2.model.RunInstancesResponse;
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
	private static final int GRAPHIC_PROCESSOR_COLUMN = 7;
	private static final int GRAPHIC_MEMORY_COLUMN = 8;
	private static final int GRAPHIC_SHARING_COLUMN = 9;
	private static final int GRAPHIC_EMULATION_COLUMN = 10;

	protected static final String BANDWIDTH_REQUIREMENT = "bandwidth";
	protected static final String PERFORMANCE_REQUIREMENT = "performance";
	protected static final String PROCESSOR_REQUIREMENT = "processor";
	protected static final String STORAGE_REQUIREMENT = "storage";
	protected static final String GRAPHIC_PROCESSOR_REQUIREMENT = "GPUs";
	protected static final String GRAPHIC_MEMORY_REQUIREMENT = "memory-GPU";
	protected static final String GRAPHIC_SHARING_REQUIREMENT = "p2p-between-GPUs";
	protected static final String GRAPHIC_EMULATION_REQUIREMENT = "FPGAs";

	protected static final int INSTANCES_LAUNCH_NUMBER = 1;
	protected static final int ONE_GIGABYTE = 1024;

	private String defaultGroupId;
	private String defaultSubnetId;
	private String flavorsFilePath;
	private String region;
	private TreeSet<AwsHardwareRequirements> flavors;
	private LaunchCommandGenerator launchCommandGenerator;

	public AwsV2ComputePlugin(String confFilePath) {
		Properties properties = PropertiesUtil.readProperties(confFilePath);
		this.region = properties.getProperty(AwsV2ConfigurationPropertyKeys.AWS_REGION_SELECTION_KEY);
		this.defaultSubnetId = properties.getProperty(AwsV2ConfigurationPropertyKeys.AWS_DEFAULT_SUBNET_ID_KEY);
		this.defaultGroupId = properties.getProperty(AwsV2ConfigurationPropertyKeys.AWS_DEFAULT_SECURITY_GROUP_ID_KEY);
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
		List<String> subnetIds = getSubnetIdsFrom(computeOrder);
		List<InstanceNetworkInterfaceSpecification> networkInterfaces = loadNetworkInterfaces(subnetIds);
		String userData = this.launchCommandGenerator.createLaunchCommand(computeOrder);

		RunInstancesRequest request = RunInstancesRequest.builder()
				.imageId(imageId)
				.instanceType(instanceType)
				.maxCount(INSTANCES_LAUNCH_NUMBER)
				.minCount(INSTANCES_LAUNCH_NUMBER)
				.networkInterfaces(networkInterfaces)
				.userData(userData)
				.build();

		Ec2Client client = AwsV2ClientUtil.createEc2Client(cloudUser.getToken(), this.region);
		return doRunInstancesRequests(computeOrder, flavor, request, client);
	}

	@Override
	public ComputeInstance getInstance(ComputeOrder computeOrder, AwsV2User cloudUser) throws FogbowException {
		LOGGER.info(String.format(Messages.Info.GETTING_INSTANCE, computeOrder.getInstanceId(), cloudUser.getToken()));

		DescribeInstancesRequest request = DescribeInstancesRequest.builder()
				.instanceIds(computeOrder.getInstanceId())
				.build();

		updateHardwareRequirements(cloudUser);
		Ec2Client client = AwsV2ClientUtil.createEc2Client(cloudUser.getToken(), this.region);
		ComputeInstance computeInstance = doDescribeInstancesRequests(request, client);
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
		doTerminateInstancesRequests(request, client);
	}

	@Override
	public boolean isReady(String instanceState) {
		return AwsV2StateMapper.map(ResourceType.COMPUTE, instanceState).equals(InstanceState.READY);
	}

	@Override
	public boolean hasFailed(String instanceState) {
		return false;
	}

	private void doTerminateInstancesRequests(TerminateInstancesRequest request, Ec2Client client)
			throws InvalidParameterException, UnexpectedException {
		try {
			client.terminateInstances(request);
		} catch (Exception e) {
			String instanceId = request.instanceIds().listIterator().next();
			LOGGER.error(String.format(Messages.Error.ERROR_WHILE_REMOVING_RESOURCE, RESOURCE_NAME, instanceId), e);
			throw new UnexpectedException();
		}
	}
	
	private ComputeInstance doDescribeInstancesRequests(DescribeInstancesRequest request, Ec2Client client)
			throws FogbowException {

		try {
			DescribeInstancesResponse response = client.describeInstances(request);
			Instance instance = getInstanceReservation(response);
			List<Volume> volumes = getInstanceVolumes(client, instance);
			return mountComputeInstance(instance, volumes);
		} catch (Exception e) {
			throw new UnexpectedException(String.format(Messages.Exception.GENERIC_EXCEPTION, e), e);
		}
	}
	
	private String doRunInstancesRequests(ComputeOrder computeOrder, AwsHardwareRequirements flavor,
			RunInstancesRequest request, Ec2Client client) throws UnexpectedException {
		try {
			RunInstancesResponse response = client.runInstances(request);
			String instanceId = null;
			Instance instance;
			if (response != null && !response.instances().isEmpty()) {
				instance = response.instances().listIterator().next();
				instanceId = instance.instanceId();
				String name = FogbowCloudUtil.defineInstanceName(computeOrder.getName());
				AwsV2CloudUtil.doCreateTagsRequest(client, instanceId, AWS_TAG_NAME, name);
				updateInstanceAllocation(computeOrder, flavor, instance, client);
			}
			return instanceId;
		} catch (Exception e) {
			throw new UnexpectedException(String.format(Messages.Exception.GENERIC_EXCEPTION, e), e);
		}
	}

	private void updateInstanceAllocation(ComputeOrder computeOrder, AwsHardwareRequirements flavor, Instance instance,
			Ec2Client client) throws FogbowException {
		
		synchronized (computeOrder) {
			int vCPU = instance.cpuOptions().coreCount();
			int memory = flavor.getMemory();
			String imageId = flavor.getImageId();
			Image image = getImageById(imageId, client);
			int disk = getImageSize(image);
			int instances = INSTANCES_LAUNCH_NUMBER;
			ComputeAllocation actualAllocation = new ComputeAllocation(vCPU, memory, instances, disk);
			computeOrder.setActualAllocation(actualAllocation);
		}
	}

	protected Image getImageById(String imageId, Ec2Client client) throws FogbowException {
		DescribeImagesRequest request = DescribeImagesRequest.builder()
				.imageIds(imageId)
				.build();
		
		DescribeImagesResponse response = doDescribeImagesRequests(request, client);
		if (response != null && !response.images().isEmpty()) {
			return response.images().listIterator().next();
		}
		throw new InstanceNotFoundException(Messages.Exception.IMAGE_NOT_FOUND);
	}

	protected ComputeInstance mountComputeInstance(Instance instance, List<Volume> volumes)
			throws InstanceNotFoundException {

		String id = instance.instanceId();
		String cloudState = instance.state().nameAsString();
		String name = instance.tags().listIterator().next().value();
		int cpu = instance.cpuOptions().coreCount();
		int memory = getMemoryValueFrom(instance.instanceType());
		int disk = getAllDisksSize(volumes);
		List<String> ipAddresses = getIpAddresses(instance);
		return new ComputeInstance(id, cloudState, name, cpu, memory, disk, ipAddresses);
	}

	protected List<String> getIpAddresses(Instance instance) {
		List<String> ipAddresses = new ArrayList<>();
		List<String> privateIpaddresses;
		String publicIpAddress;
		for (int i = 0; i < instance.networkInterfaces().size(); i++) {
			privateIpaddresses = addPrivateIpAddresses(instance, i);
			if (!privateIpaddresses.isEmpty()) {
				ipAddresses.addAll(privateIpaddresses);
			}
			publicIpAddress = addPublicIpAddress(instance, i);
			if (publicIpAddress != null) {
				ipAddresses.add(publicIpAddress);
			}
		}
		return ipAddresses;
	}

	private String addPublicIpAddress(Instance instance, int index) {
		String ipAddress = null;
		InstanceNetworkInterfaceAssociation association;
		association = instance.networkInterfaces().get(index).association();
		if (association != null) {
			ipAddress = association.publicIp();
		}
		return ipAddress;
	}

	private List<String> addPrivateIpAddresses(Instance instance, int index) {
		List<String> ipAddresses = new ArrayList<String>();
		List<InstancePrivateIpAddress> instancePrivateIpAddresses;
		if (!instance.networkInterfaces().isEmpty()) {
			instancePrivateIpAddresses = instance.networkInterfaces().get(index).privateIpAddresses();
			if (instancePrivateIpAddresses != null && !instancePrivateIpAddresses.isEmpty()) {
				for (InstancePrivateIpAddress instancePrivateIpAddress : instancePrivateIpAddresses) {
					ipAddresses.add(instancePrivateIpAddress.privateIpAddress());
				}
			}
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
		for (AwsHardwareRequirements flavors : getFlavors()) {
			if (flavors.getName().equals(instanceType.toString())) {
				return flavors.getMemory();
			}
		}
		return 0;
	}

	protected List<Volume> getInstanceVolumes(Ec2Client client, Instance instance) throws FogbowException {
		List<Volume> volumes = new ArrayList<>();
		DescribeVolumesRequest request;
		DescribeVolumesResponse response;
		
		List<String> volumeIds = getVolumeIds(instance);
		for (String volumeId : volumeIds) {
			request = DescribeVolumesRequest.builder().volumeIds(volumeId).build();
		    response = AwsV2CloudUtil.doDescribeVolumesRequest(client, request);
			volumes.addAll(response.volumes());
		}
		return volumes;
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
			Reservation reservation = response.reservations().listIterator().next();
			if (!reservation.instances().isEmpty()) {
				return reservation.instances().listIterator().next();
			}
		}
		throw new InstanceNotFoundException(Messages.Exception.INSTANCE_NOT_FOUND);
	}

	protected List<String> getSubnetIdsFrom(ComputeOrder computeOrder) {
		List<String> subnetIds = new ArrayList<String>();
		// Only the first network from the network's list can get a public ip for now.
		subnetIds.addAll(computeOrder.getNetworkIds());
		subnetIds.add(this.defaultSubnetId);
		return subnetIds;
	}

	protected List<InstanceNetworkInterfaceSpecification> loadNetworkInterfaces(List<String> subnetIds) {
		List<InstanceNetworkInterfaceSpecification> networkInterfaces = new ArrayList<>();
		InstanceNetworkInterfaceSpecification nis;
		String subnetId;
		for (int i = 0; i < subnetIds.size(); i++) {
			subnetId = subnetIds.get(i);
			nis = buildNetworkInterfaces(subnetId, i);
			networkInterfaces.add(nis);
		}
		return networkInterfaces;
	}

	protected InstanceNetworkInterfaceSpecification buildNetworkInterfaces(String subnetId, int deviceIndex) {
		InstanceNetworkInterfaceSpecification networkInterface = InstanceNetworkInterfaceSpecification.builder()
				.subnetId(subnetId)
				.deviceIndex(deviceIndex)
				.groups(this.defaultGroupId)
				.build();

		return networkInterface;
	}

	protected AwsHardwareRequirements findSmallestFlavor(ComputeOrder computeOrder, AwsV2User cloudUser)
			throws FogbowException {

		AwsHardwareRequirements bestFlavor = getBestFlavor(computeOrder, cloudUser);
		if (bestFlavor == null) {
			throw new NoAvailableResourcesException(Messages.Exception.NO_MATCHING_FLAVOR);
		}
		return bestFlavor;
	}

	protected AwsHardwareRequirements getBestFlavor(ComputeOrder computeOrder, AwsV2User cloudUser)
			throws FogbowException {

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

	/**
	 * this method attempts to filter the set of flavors available from a
	 * requirements map passed by parameter. If there are valid requirements on the
	 * map, it will filter these insights, returning only the common flavors to the
	 * requested order. Otherwise returns the complete flavor set without changes.
	 * 
	 * @param orderRequirements: a map of requirements in the compute order request.
	 * @return a set of requirements-filtered flavors or the current set of flavors.
	 */
	protected TreeSet<AwsHardwareRequirements> getFlavorsByRequirements(Map<String, String> orderRequirements) {
		TreeSet<AwsHardwareRequirements> resultSet = getFlavors();
		List<AwsHardwareRequirements> resultList = null;
		if (orderRequirements != null && !orderRequirements.isEmpty()) {
			for (Entry<String, String> requirements : orderRequirements.entrySet()) {
				resultList = filterFlavors(resultSet, requirements);
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

	protected List<AwsHardwareRequirements> filterFlavors(TreeSet<AwsHardwareRequirements> flavors,
			Entry<String, String> requirements) {
		
		String key = requirements.getKey().trim();
		String value = requirements.getValue().trim();
		return flavors.stream().filter(flavor -> flavor.getRequirements().get(key).equalsIgnoreCase(value))
				.collect(Collectors.toList());
	}

	protected TreeSet<AwsHardwareRequirements> getFlavors() {
		synchronized (this.flavors) {
			return this.flavors;
		}
	}

	/**
	 * This method loads all of the flavor file lines containing the hardware
	 * requirements by instance type AWS, and a map of available images in the
	 * cloud, to assemble a set of flavors ordered by the simplest requirements.
	 * 
	 * @param cloudUser: the user of the AWS cloud.
	 * @throws FogbowException: if any error occurs.
	 */
	protected void updateHardwareRequirements(AwsV2User cloudUser) throws FogbowException {
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
		String flavorId = FogbowCloudUtil.getRandomUUID();
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
		String graphicsProcessor = requirements[GRAPHIC_PROCESSOR_COLUMN];
		String graphicsMemory = requirements[GRAPHIC_MEMORY_COLUMN];
		String graphicSharing = requirements[GRAPHIC_SHARING_COLUMN];
		String graphicEmulation = requirements[GRAPHIC_EMULATION_COLUMN];
		requirementsMap.put(PROCESSOR_REQUIREMENT, processor);
		requirementsMap.put(PERFORMANCE_REQUIREMENT, performance);
		requirementsMap.put(STORAGE_REQUIREMENT, storage);
		requirementsMap.put(BANDWIDTH_REQUIREMENT, bandwidth);
		requirementsMap.put(GRAPHIC_PROCESSOR_REQUIREMENT, graphicsProcessor);
		requirementsMap.put(GRAPHIC_MEMORY_REQUIREMENT, graphicsMemory);
		requirementsMap.put(GRAPHIC_SHARING_REQUIREMENT, graphicSharing);
		requirementsMap.put(GRAPHIC_EMULATION_REQUIREMENT, graphicEmulation);
		return requirementsMap;
	}

	protected Map<String, Integer> getImagesMap(AwsV2User cloudUser)
			throws InvalidParameterException, UnexpectedException {

		Map<String, Integer> imageMap = new HashMap<String, Integer>();
		String cloudUserId = cloudUser.getId();
		DescribeImagesRequest request = DescribeImagesRequest.builder()
				.owners(cloudUserId)
				.build();

		Ec2Client client = AwsV2ClientUtil.createEc2Client(cloudUser.getToken(), this.region);
		DescribeImagesResponse response = doDescribeImagesRequests(request, client);

		List<Image> images = response.images();
		for (Image image : images) {
			int size = getImageSize(image);
			imageMap.put(image.imageId(), size);
		}
		return imageMap;
	}
	
	protected DescribeImagesResponse doDescribeImagesRequests(DescribeImagesRequest request, Ec2Client client)
			throws UnexpectedException {
		try {
			return client.describeImages(request);
		} catch (SdkException e) {
			throw new UnexpectedException(String.format(Messages.Exception.GENERIC_EXCEPTION, e), e);
		}
	}

	protected List<String> loadLinesFromFlavorFile() throws ConfigurationErrorException {
		String file = getFlavorsFilePath();
		Path path = Paths.get(file);
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
	protected String getFlavorsFilePath() {
		return flavorsFilePath;
	}

}