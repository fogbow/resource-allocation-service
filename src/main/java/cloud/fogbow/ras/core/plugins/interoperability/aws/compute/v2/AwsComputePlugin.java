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

import cloud.fogbow.common.exceptions.*;
import com.google.common.annotations.VisibleForTesting;
import org.apache.log4j.Logger;

import cloud.fogbow.common.exceptions.InternalServerErrorException;
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
import cloud.fogbow.ras.core.plugins.interoperability.util.LaunchCommandGenerator;
import software.amazon.awssdk.core.exception.SdkException;
import software.amazon.awssdk.services.ec2.Ec2Client;
import software.amazon.awssdk.services.ec2.model.*;

public class AwsComputePlugin implements ComputePlugin<AwsV2User> {

    private static final Logger LOGGER = Logger.getLogger(AwsComputePlugin.class);
	
    private static final String COMMENTED_LINE_PREFIX = "#";
    private static final String CSV_COLUMN_SEPARATOR = ",";
	
    private static final int DEDICATED_EBS_BANDWIDTH_COLUMN = 6;
    private static final int GRAPHIC_PROCESSOR_COLUMN = 7;
    private static final int GRAPHIC_MEMORY_COLUMN = 8;
    private static final int GRAPHIC_SHARING_COLUMN = 9;
    private static final int GRAPHIC_EMULATION_COLUMN = 10;
    private static final int INSTANCE_TYPE_COLUMN = 0;
    private static final int MEMORY_COLUMN = 2;
    private static final int NETWORK_PERFORMANCE_COLUMN = 5;
    private static final int PROCESSOR_COLUMN = 4;
    private static final int STORAGE_COLUMN = 3;
    private static final int VCPU_COLUMN = 1;

    protected static final String BANDWIDTH_REQUIREMENT = "bandwidth";
    protected static final String GRAPHIC_EMULATION_REQUIREMENT = "FPGAs";
    protected static final String GRAPHIC_MEMORY_REQUIREMENT = "memory-GPU";
    protected static final String GRAPHIC_PROCESSOR_REQUIREMENT = "GPUs";
    protected static final String GRAPHIC_SHARING_REQUIREMENT = "p2p-between-GPUs";
    protected static final String PERFORMANCE_REQUIREMENT = "performance";
    protected static final String PROCESSOR_REQUIREMENT = "processor";
    protected static final String RESOURCE_NAME = "Compute";
    protected static final String STORAGE_REQUIREMENT = "storage";
	
    protected static final int DEFAULT_DEVICE_INDEX = 0;
    protected static final int INSTANCES_LAUNCH_NUMBER = 1;
    protected static final int MAXIMUM_SIZE_ALLOWED = 1;
    protected static final int ONE_GIGABYTE = 1024;

    private String defaultSubnetId;
    private String flavorsFilePath;
    private String region;
    private TreeSet<AwsHardwareRequirements> flavors;
    private LaunchCommandGenerator launchCommandGenerator;

    public AwsComputePlugin(String confFilePath) {
        Properties properties = PropertiesUtil.readProperties(confFilePath);
        this.region = properties.getProperty(AwsV2ConfigurationPropertyKeys.AWS_REGION_SELECTION_KEY);
        this.defaultSubnetId = properties.getProperty(AwsV2ConfigurationPropertyKeys.AWS_DEFAULT_SUBNET_ID_KEY);
        this.flavorsFilePath = properties.getProperty(AwsV2ConfigurationPropertyKeys.AWS_FLAVORS_TYPES_FILE_PATH_KEY);
        this.launchCommandGenerator = new DefaultLaunchCommandGenerator();
        this.flavors = new TreeSet<AwsHardwareRequirements>();
    }

    @Override
    public String requestInstance(ComputeOrder computeOrder, AwsV2User cloudUser) throws FogbowException {
        LOGGER.info(String.format(Messages.Log.REQUESTING_INSTANCE_FROM_PROVIDER));
        Ec2Client client = AwsV2ClientUtil.createEc2Client(cloudUser.getToken(), this.region);
        AwsHardwareRequirements flavor = findSmallestFlavor(computeOrder, cloudUser);
        Subnet subnet = getNetworkSelected(computeOrder, client);
        RunInstancesRequest request = buildRequestInstance(computeOrder, flavor, subnet);
        return doRequestInstance(computeOrder, flavor, request, client);
    }

    @Override
    public ComputeInstance getInstance(ComputeOrder computeOrder, AwsV2User cloudUser) throws FogbowException {
        LOGGER.info(String.format(Messages.Log.GETTING_INSTANCE_S, computeOrder.getInstanceId()));
        Ec2Client client = AwsV2ClientUtil.createEc2Client(cloudUser.getToken(), this.region);
        updateHardwareRequirements(cloudUser);
        String instanceId = computeOrder.getInstanceId();
        return doGetInstance(instanceId, client);
    }

    @Override
    public void deleteInstance(ComputeOrder computeOrder, AwsV2User cloudUser) throws FogbowException {
        LOGGER.info(String.format(Messages.Log.DELETING_INSTANCE_S, computeOrder.getInstanceId()));
        Ec2Client client = AwsV2ClientUtil.createEc2Client(cloudUser.getToken(), this.region);
        String instanceId = computeOrder.getInstanceId();
        doDeleteInstance(instanceId, client);
    }

    @Override
    public boolean isReady(String instanceState) {
        return AwsV2StateMapper.map(ResourceType.COMPUTE, instanceState).equals(InstanceState.READY);
    }

    @Override
    public boolean hasFailed(String instanceState) {
        return false;
    }

    protected void doDeleteInstance(String instanceId, Ec2Client client) throws InternalServerErrorException {
        TerminateInstancesRequest request = TerminateInstancesRequest.builder()
                .instanceIds(instanceId)
                .build();
        try {
            client.terminateInstances(request);
        } catch (SdkException e) {
            LOGGER.error(String.format(Messages.Log.ERROR_WHILE_REMOVING_RESOURCE_S_S, RESOURCE_NAME, instanceId), e);
            throw new InternalServerErrorException(String.format(Messages.Exception.ERROR_WHILE_REMOVING_RESOURCE_S_S, RESOURCE_NAME, instanceId));
        }
    }
	
    protected ComputeInstance doGetInstance(String instanceId, Ec2Client client) throws FogbowException {
        DescribeInstancesResponse response = AwsV2CloudUtil.doDescribeInstanceById(instanceId, client);
        Instance instance = AwsV2CloudUtil.getInstanceFrom(response);
        List<Volume> volumes = AwsV2CloudUtil.getInstanceVolumes(instance, client);
        return buildComputeInstance(instance, volumes);
    }
	
    protected ComputeInstance buildComputeInstance(Instance instance, List<Volume> volumes) {
        String id = instance.instanceId();
        String cloudState = instance.state().nameAsString();
        String name = instance.tags().listIterator().next().value();
        int cpu = instance.cpuOptions().coreCount();
        int memory = getMemoryValueFrom(instance.instanceType());
        int disk = getAllDisksSize(volumes);
        List<String> ipAddresses = getIpAddresses(instance);
        String faultMessage = getFaultMessage(instance);
        return new ComputeInstance(id, cloudState, name, cpu, memory, disk, ipAddresses, faultMessage);
    }

    @VisibleForTesting
    protected String getFaultMessage(Instance instance) {
        String faultMessage = null;
        String stateTransitionReason = instance.stateTransitionReason();
        if (!stateTransitionReason.isEmpty()) {
            faultMessage = instance.stateReason().message();
        }
        return faultMessage;
    }

    protected List<String> getIpAddresses(Instance instance) {
        List<String> ipAddresses = new ArrayList<>();
        List<String> privateIpaddresses;
        String publicIpAddress;
        for (int i = 0; i < instance.networkInterfaces().size(); i++) {
            privateIpaddresses = getPrivateIpAddresses(instance, i);
            if (!privateIpaddresses.isEmpty()) {
                ipAddresses.addAll(privateIpaddresses);
            }
            publicIpAddress = getPublicIpAddresses(instance, i);
            if (publicIpAddress != null) {
                ipAddresses.add(publicIpAddress);
            }
        }
        return ipAddresses;
    }

    protected String getPublicIpAddresses(Instance awsInstance, int index) {
        String ipAddress = null;
        InstanceNetworkInterfaceAssociation association;
        association = awsInstance.networkInterfaces().get(index).association();
        if (association != null) {
            ipAddress = association.publicIp();
        }
        return ipAddress;
    }

    protected List<String> getPrivateIpAddresses(Instance awsInstance, int index) {
        List<String> ipAddresses = new ArrayList<String>();
        List<InstancePrivateIpAddress> instancePrivateIpAddresses;
        instancePrivateIpAddresses = awsInstance.networkInterfaces().get(index).privateIpAddresses();
        if (instancePrivateIpAddresses != null && !instancePrivateIpAddresses.isEmpty()) {
            for (InstancePrivateIpAddress instancePrivateIpAddress : instancePrivateIpAddresses) {
                ipAddresses.add(instancePrivateIpAddress.privateIpAddress());
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
        for (AwsHardwareRequirements flavor : getFlavors()) {
            if (flavor.getName().equals(instanceType.toString())) {
                return flavor.getRam();
            }
        }
        return 0;
    }
	
    protected String doRequestInstance(ComputeOrder computeOrder, AwsHardwareRequirements flavor,
            RunInstancesRequest request, Ec2Client client) throws InternalServerErrorException {
        
        try {
            RunInstancesResponse response = client.runInstances(request);
            String instanceId = null;
            Instance instance;
            if (response != null && !response.instances().isEmpty()) {
                instance = response.instances().listIterator().next();
                instanceId = instance.instanceId();
                String instanceName = computeOrder.getName();
                AwsV2CloudUtil.createTagsRequest(instanceId, AwsV2CloudUtil.AWS_TAG_NAME, instanceName, client);
                updateInstanceAllocation(computeOrder, flavor, instance, client);
            }
            return instanceId;
        } catch (Exception e) {
            throw new InternalServerErrorException(e.getMessage());
        }
    }

    protected void updateInstanceAllocation(ComputeOrder computeOrder, AwsHardwareRequirements flavor, Instance instance,
            Ec2Client client) throws FogbowException {

        synchronized (computeOrder) {
            int vCPU = flavor.getCpu();
            int memory = flavor.getRam();
            String imageId = flavor.getImageId();
            Image image = getImageById(imageId, client);
            int disk = getImageSize(image);
            int instances = INSTANCES_LAUNCH_NUMBER;
            ComputeAllocation actualAllocation = new ComputeAllocation(instances, vCPU, memory, disk);
            computeOrder.setActualAllocation(actualAllocation);
        }
    }
    
    protected Image getImageById(String imageId, Ec2Client client) throws FogbowException {
        DescribeImagesRequest request = DescribeImagesRequest.builder()
                .imageIds(imageId)
                .build();

        DescribeImagesResponse response = AwsV2CloudUtil.doDescribeImagesRequest(request, client);
        if (response != null && !response.images().isEmpty()) {
            return response.images().listIterator().next();
        }
        throw new InstanceNotFoundException(Messages.Exception.IMAGE_NOT_FOUND);
    }

    protected RunInstancesRequest buildRequestInstance(ComputeOrder order, AwsHardwareRequirements flavor, Subnet subnet)
            throws FogbowException {

        String imageId = flavor.getImageId();
        InstanceType instanceType = InstanceType.fromValue(flavor.getName());
        InstanceNetworkInterfaceSpecification networkInterface = buildNetworkInterface(subnet);
        String userData = this.launchCommandGenerator.createLaunchCommand(order);

        RunInstancesRequest request = RunInstancesRequest.builder()
                .imageId(imageId)
                .instanceType(instanceType)
                .maxCount(INSTANCES_LAUNCH_NUMBER)
                .minCount(INSTANCES_LAUNCH_NUMBER)
                .networkInterfaces(networkInterface)
                .userData(userData)
                .build();

        return request;
    }
	
    protected InstanceNetworkInterfaceSpecification buildNetworkInterface(Subnet subnet) throws FogbowException {
        List<Tag> subnetTags = subnet.tags();
        String groupId = AwsV2CloudUtil.getGroupIdFrom(subnetTags);
        String subnetId = subnet.subnetId();

        InstanceNetworkInterfaceSpecification networkInterface = InstanceNetworkInterfaceSpecification.builder()
                .subnetId(subnetId)
                .deviceIndex(DEFAULT_DEVICE_INDEX)
                .groups(groupId)
                .build();

        return networkInterface;
    }

    protected Subnet getNetworkSelected(ComputeOrder order, Ec2Client client) throws FogbowException {
        List<String> networkIds = order.getNetworkIds();
        String subnetId = selectNetworkId(networkIds);
        return AwsV2CloudUtil.getSubnetById(subnetId, client);
    }

    protected String selectNetworkId(List<String> networkIdList) throws FogbowException {
        String networkId = this.defaultSubnetId;
        if (!networkIdList.isEmpty()) {
            checkNetworkIdListIntegrity(networkIdList);
            networkId = networkIdList.listIterator().next();
        }
        return networkId;
    }

    protected void checkNetworkIdListIntegrity(List<String> networkIdList) throws FogbowException {
        if (networkIdList.size() > MAXIMUM_SIZE_ALLOWED) {
            throw new InvalidParameterException(Messages.Exception.MANY_NETWORKS_NOT_ALLOWED);
        }
    }

    protected AwsHardwareRequirements findSmallestFlavor(ComputeOrder computeOrder, AwsV2User cloudUser)
            throws FogbowException {

        updateHardwareRequirements(cloudUser);
        TreeSet<AwsHardwareRequirements> resultset = getFlavorsByRequirements(computeOrder.getRequirements());
        for (AwsHardwareRequirements hardwareRequirements : resultset) {
            if (hardwareRequirements.getCpu() >= computeOrder.getvCPU()
                    && hardwareRequirements.getRam() >= computeOrder.getRam()
                    && hardwareRequirements.getDisk() >= computeOrder.getDisk()) {
                return hardwareRequirements;
            }
        }
        throw new UnacceptableOperationException(Messages.Exception.NO_MATCHING_FLAVOR);
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
        return flavors.stream()
                .filter(flavor -> value.equalsIgnoreCase(flavor.getRequirements().get(key)))
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

        Map<String, Integer> imagesMap = generateImagesSizeMap(cloudUser);
        for (Entry<String, Integer> imageEntry : imagesMap.entrySet()) {
            for (String line : lines) {
                if (!line.startsWith(COMMENTED_LINE_PREFIX)) {
                    requirements = line.split(CSV_COLUMN_SEPARATOR);
                    flavor = buildHardwareRequirements(imageEntry, requirements);
                    this.flavors.add(flavor);
                }
            }
        }
    }

    protected List<String> loadLinesFromFlavorFile() throws ConfigurationErrorException {
        String file = getFlavorsFilePath();
        Path path = Paths.get(file);
        try {
            return Files.readAllLines(path);
        } catch (IOException e) {
            LOGGER.error(String.format(Messages.Log.UNABLE_TO_LOAD_FLAVOURS, e), e);
            throw new ConfigurationErrorException(String.format(Messages.Exception.UNABLE_TO_LOAD_FLAVOURS, e));
        }
    }
	
    protected AwsHardwareRequirements buildHardwareRequirements(Entry<String, Integer> imageEntry,
            String[] requirements) {

        String name = requirements[INSTANCE_TYPE_COLUMN];
        String flavorId = generateFlavorId();
        int cpu = Integer.parseInt(requirements[VCPU_COLUMN]);
        Double memory = Double.parseDouble(requirements[MEMORY_COLUMN]) * ONE_GIGABYTE;
        int disk = imageEntry.getValue();
        String imageId = imageEntry.getKey();
        Map<String, String> requirementsMap = loadRequirementsMap(requirements);
        return new AwsHardwareRequirements(name, flavorId, cpu, memory.intValue(), disk, imageId, requirementsMap);
    }

    protected Map<String, String> loadRequirementsMap(String[] requirements) {
        Map<String, String> requirementsMap = new HashMap<String, String>();
        requirementsMap.put(BANDWIDTH_REQUIREMENT, requirements[DEDICATED_EBS_BANDWIDTH_COLUMN]);
        requirementsMap.put(GRAPHIC_EMULATION_REQUIREMENT, requirements[GRAPHIC_EMULATION_COLUMN]);
        requirementsMap.put(GRAPHIC_MEMORY_REQUIREMENT, requirements[GRAPHIC_MEMORY_COLUMN]);
        requirementsMap.put(GRAPHIC_PROCESSOR_REQUIREMENT, requirements[GRAPHIC_PROCESSOR_COLUMN]);
        requirementsMap.put(GRAPHIC_SHARING_REQUIREMENT, requirements[GRAPHIC_SHARING_COLUMN]);
        requirementsMap.put(PERFORMANCE_REQUIREMENT, requirements[NETWORK_PERFORMANCE_COLUMN]);
        requirementsMap.put(PROCESSOR_REQUIREMENT, requirements[PROCESSOR_COLUMN]);
        requirementsMap.put(STORAGE_REQUIREMENT, requirements[STORAGE_COLUMN]);
        return requirementsMap;
    }

    protected Map<String, Integer> generateImagesSizeMap(AwsV2User cloudUser) throws FogbowException {

        Map<String, Integer> imageMap = new HashMap<String, Integer>();
        String cloudUserId = cloudUser.getId();
        DescribeImagesRequest request = DescribeImagesRequest.builder()
                .owners(cloudUserId)
                .build();

        Ec2Client client = AwsV2ClientUtil.createEc2Client(cloudUser.getToken(), this.region);
        DescribeImagesResponse response = AwsV2CloudUtil.doDescribeImagesRequest(request, client);

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

    // The following methods are used to assist in testing.
	
    protected String generateFlavorId() {
        return UUID.randomUUID().toString();
    }
	
    protected String getFlavorsFilePath() {
        return flavorsFilePath;
    }
	
    protected void setLaunchCommandGenerator(LaunchCommandGenerator launchCommandGenerator) {
        this.launchCommandGenerator = launchCommandGenerator;
    }

}