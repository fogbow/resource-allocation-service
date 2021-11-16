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
import cloud.fogbow.common.models.GoogleCloudUser;
import cloud.fogbow.common.util.BinaryUnit;
import cloud.fogbow.ras.core.models.orders.Order;
import cloud.fogbow.ras.core.plugins.interoperability.aws.sdk.v2.compute.model.AwsHardwareRequirements;
import com.google.common.annotations.VisibleForTesting;
import org.apache.log4j.Logger;

import cloud.fogbow.common.models.AwsV2User;
import cloud.fogbow.common.models.CloudUser;
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

    @VisibleForTesting
    static final String BANDWIDTH_REQUIREMENT = "bandwidth";
    @VisibleForTesting
    static final String GRAPHIC_EMULATION_REQUIREMENT = "FPGAs";
    @VisibleForTesting
    static final String GRAPHIC_MEMORY_REQUIREMENT = "memory-GPU";
    @VisibleForTesting
    static final String GRAPHIC_PROCESSOR_REQUIREMENT = "GPUs";
    @VisibleForTesting
    static final String GRAPHIC_SHARING_REQUIREMENT = "p2p-between-GPUs";
    @VisibleForTesting
    static final String PERFORMANCE_REQUIREMENT = "performance";
    @VisibleForTesting
    static final String PROCESSOR_REQUIREMENT = "processor";
    @VisibleForTesting
    static final String RESOURCE_NAME = "Compute";
    @VisibleForTesting
    static final String STORAGE_REQUIREMENT = "storage";
	
    @VisibleForTesting
    static final int DEFAULT_DEVICE_INDEX = 0;
    @VisibleForTesting
    static final int INSTANCES_LAUNCH_NUMBER = 1;
    @VisibleForTesting
    static final int MAXIMUM_SIZE_ALLOWED = 1;

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
    public void takeSnapshot(ComputeOrder order, String name, AwsV2User cloudUser) throws FogbowException {
        // TODO implement
        throw new NotImplementedOperationException();
    }

    @Override
    public void pauseInstance(ComputeOrder order, AwsV2User cloudUser) throws FogbowException {
        throw new NotImplementedOperationException();
    }

    @Override
    public void hibernateInstance(ComputeOrder computeOrder, AwsV2User cloudUser) throws FogbowException {
        LOGGER.info(String.format(Messages.Log.HIBERNATING_INSTANCE_S, computeOrder.getInstanceId()));
        Ec2Client client = AwsV2ClientUtil.createEc2Client(cloudUser.getToken(), this.region);
        String instanceId = computeOrder.getInstanceId();
        doHibernateInstance(instanceId, client);
    }

    @Override
    public void resumeInstance(ComputeOrder computeOrder, AwsV2User cloudUser) throws FogbowException {
        LOGGER.info(String.format(Messages.Log.RESUMING_INSTANCE_S, computeOrder.getInstanceId()));
        Ec2Client client = AwsV2ClientUtil.createEc2Client(cloudUser.getToken(), this.region);
        String instanceId = computeOrder.getInstanceId();
        doResumeInstance(instanceId, client);
    }

    @Override
    public void stopInstance(ComputeOrder computeOrder, AwsV2User cloudUser) throws FogbowException {
        LOGGER.info(String.format(Messages.Log.STOPPING_INSTANCE_S, computeOrder.getInstanceId()));
        Ec2Client client = AwsV2ClientUtil.createEc2Client(cloudUser.getToken(), this.region);
        String instanceId = computeOrder.getInstanceId();
        doStopInstance(instanceId, client);
    }

    @Override
    public boolean isPaused(String cloudState) throws FogbowException {
        return false;
    }

    @Override
    public boolean isHibernated(String cloudState) throws FogbowException {
        return AwsV2StateMapper.map(ResourceType.COMPUTE, cloudState).equals(InstanceState.STOPPED);
    }
    
    @Override
    public boolean isStopped(String cloudState) throws FogbowException {
        return AwsV2StateMapper.map(ResourceType.COMPUTE, cloudState).equals(InstanceState.STOPPED);
    }

    @Override
    public boolean isReady(String instanceState) {
        return AwsV2StateMapper.map(ResourceType.COMPUTE, instanceState).equals(InstanceState.READY);
    }

    @Override
    public boolean hasFailed(String instanceState) {
        return false;
    }

    @VisibleForTesting
    void doDeleteInstance(String instanceId, Ec2Client client) throws InternalServerErrorException {
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
	
    @VisibleForTesting
    void doHibernateInstance(String instanceId, Ec2Client client) throws InternalServerErrorException {
        StopInstancesRequest request = StopInstancesRequest.builder()
                .hibernate(true)
                .instanceIds(instanceId)
                .build();
        
        try {
            client.stopInstances(request);
        } catch (SdkException e) {
            LOGGER.error(String.format(Messages.Log.ERROR_WHILE_HIBERNATING_INSTANCE_S, instanceId), e);
            throw new InternalServerErrorException(String.format(Messages.Exception.ERROR_WHILE_HIBERNATING_INSTANCE_S, instanceId));
        }
    }
    
    @VisibleForTesting
    void doResumeInstance(String instanceId, Ec2Client client) throws InternalServerErrorException {
        StartInstancesRequest request = StartInstancesRequest.builder()
                .instanceIds(instanceId)
                .build();
        
        try {
            client.startInstances(request);
        } catch (SdkException e) {
            LOGGER.error(String.format(Messages.Log.ERROR_WHILE_RESUMING_INSTANCE_S, instanceId), e);
            throw new InternalServerErrorException(String.format(Messages.Exception.ERROR_WHILE_RESUMING_INSTANCE_S, instanceId));
        }
    }
    
    @VisibleForTesting
    void doStopInstance(String instanceId, Ec2Client client) throws InternalServerErrorException {
        StopInstancesRequest request = StopInstancesRequest.builder()
                .instanceIds(instanceId)
                .build();
        
        try {
            client.stopInstances(request);
        } catch (SdkException e) {
            LOGGER.error(String.format(Messages.Log.ERROR_WHILE_STOPPING_INSTANCE_S, instanceId), e);
            throw new InternalServerErrorException(String.format(Messages.Exception.ERROR_WHILE_STOPPING_INSTANCE_S, instanceId));
        }
    }

    @VisibleForTesting
    ComputeInstance doGetInstance(String instanceId, Ec2Client client) throws FogbowException {
        DescribeInstancesResponse response = AwsV2CloudUtil.doDescribeInstanceById(instanceId, client);
        Instance instance = AwsV2CloudUtil.getInstanceFrom(response);
        checkTerminatedStateFrom(instance);
        List<Volume> volumes = AwsV2CloudUtil.getInstanceVolumes(instance, client);
        return buildComputeInstance(instance, volumes);
    }
	
    @VisibleForTesting
    ComputeInstance buildComputeInstance(Instance instance, List<Volume> volumes) {
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
    String getFaultMessage(Instance instance) {
        String faultMessage = null;
        String stateTransitionReason = instance.stateTransitionReason();
        if (!stateTransitionReason.isEmpty()) {
            faultMessage = instance.stateReason().message();
        }
        return faultMessage;
    }

    @VisibleForTesting
    List<String> getIpAddresses(Instance instance) {
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

    @VisibleForTesting
    String getPublicIpAddresses(Instance awsInstance, int index) {
        String ipAddress = null;
        InstanceNetworkInterfaceAssociation association;
        association = awsInstance.networkInterfaces().get(index).association();
        if (association != null) {
            ipAddress = association.publicIp();
        }
        return ipAddress;
    }

    @VisibleForTesting
    List<String> getPrivateIpAddresses(Instance awsInstance, int index) {
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

    @VisibleForTesting
    int getAllDisksSize(List<Volume> volumes) {
        int size = 0;
        for (Volume volume : volumes) {
            size += volume.size();
        }
        return size;
    }

    @VisibleForTesting
    int getMemoryValueFrom(InstanceType instanceType) {
        for (AwsHardwareRequirements flavor : getFlavors()) {
            if (flavor.getName().equals(instanceType.toString())) {
                return flavor.getRam();
            }
        }
        return 0;
    }

    @VisibleForTesting
    void checkTerminatedStateFrom(Instance instance) throws InstanceNotFoundException {
        String state = instance.state().nameAsString();
        if (state.equals(AwsV2StateMapper.TERMINATED_STATE)) {
            throw new InstanceNotFoundException(Messages.Exception.INSTANCE_NOT_FOUND);
        }
    }

    @VisibleForTesting
    String doRequestInstance(ComputeOrder computeOrder, AwsHardwareRequirements flavor,
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

    @VisibleForTesting
    void updateInstanceAllocation(ComputeOrder computeOrder, AwsHardwareRequirements flavor, Instance instance,
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
    
    @VisibleForTesting
    Image getImageById(String imageId, Ec2Client client) throws FogbowException {
        DescribeImagesRequest request = DescribeImagesRequest.builder()
                .imageIds(imageId)
                .build();

        DescribeImagesResponse response = AwsV2CloudUtil.doDescribeImagesRequest(request, client);
        if (response != null && !response.images().isEmpty()) {
            return response.images().listIterator().next();
        }
        throw new InstanceNotFoundException(Messages.Exception.IMAGE_NOT_FOUND);
    }

    @VisibleForTesting
    RunInstancesRequest buildRequestInstance(ComputeOrder order, AwsHardwareRequirements flavor, Subnet subnet)
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
	
    @VisibleForTesting
    InstanceNetworkInterfaceSpecification buildNetworkInterface(Subnet subnet) throws FogbowException {
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

    @VisibleForTesting
    Subnet getNetworkSelected(ComputeOrder order, Ec2Client client) throws FogbowException {
        List<String> networkIds = order.getNetworkIds();
        String subnetId = selectNetworkId(networkIds);
        return AwsV2CloudUtil.getSubnetById(subnetId, client);
    }

    @VisibleForTesting
    String selectNetworkId(List<String> networkIdList) throws FogbowException {
        String networkId = this.defaultSubnetId;
        if (!networkIdList.isEmpty()) {
            checkNetworkIdListIntegrity(networkIdList);
            networkId = networkIdList.listIterator().next();
        }
        return networkId;
    }

    @VisibleForTesting
    void checkNetworkIdListIntegrity(List<String> networkIdList) throws FogbowException {
        if (networkIdList.size() > MAXIMUM_SIZE_ALLOWED) {
            throw new InvalidParameterException(Messages.Exception.MANY_NETWORKS_NOT_ALLOWED);
        }
    }

    @VisibleForTesting
    AwsHardwareRequirements findSmallestFlavor(ComputeOrder computeOrder, AwsV2User cloudUser)
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
    @VisibleForTesting
    TreeSet<AwsHardwareRequirements> getFlavorsByRequirements(Map<String, String> orderRequirements) {
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

    @VisibleForTesting
    TreeSet<AwsHardwareRequirements> parseToTreeSet(List<AwsHardwareRequirements> list) {
        TreeSet<AwsHardwareRequirements> resultSet = new TreeSet<AwsHardwareRequirements>();
        resultSet.addAll(list);
        return resultSet;
    }

    @VisibleForTesting
    List<AwsHardwareRequirements> filterFlavors(TreeSet<AwsHardwareRequirements> flavors,
            Entry<String, String> requirements) {

        String key = requirements.getKey().trim();
        String value = requirements.getValue().trim();
        return flavors.stream()
                .filter(flavor -> value.equalsIgnoreCase(flavor.getRequirements().get(key)))
                .collect(Collectors.toList());
    }

    @VisibleForTesting
    TreeSet<AwsHardwareRequirements> getFlavors() {
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
    @VisibleForTesting
    void updateHardwareRequirements(AwsV2User cloudUser) throws FogbowException {
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

    @VisibleForTesting
    List<String> loadLinesFromFlavorFile() throws ConfigurationErrorException {
        String file = getFlavorsFilePath();
        Path path = Paths.get(file);
        try {
            return Files.readAllLines(path);
        } catch (IOException e) {
            LOGGER.error(String.format(Messages.Log.UNABLE_TO_LOAD_FLAVOURS, e), e);
            throw new ConfigurationErrorException(String.format(Messages.Exception.UNABLE_TO_LOAD_FLAVOURS, e));
        }
    }
	
    @VisibleForTesting
    AwsHardwareRequirements buildHardwareRequirements(Entry<String, Integer> imageEntry,
            String[] requirements) {

        String name = requirements[INSTANCE_TYPE_COLUMN];
        String flavorId = generateFlavorId();
        int cpu = Integer.parseInt(requirements[VCPU_COLUMN]);

        double memoryInGB = Double.parseDouble(requirements[MEMORY_COLUMN]);
        double memoryInMB = BinaryUnit.gigabytes(memoryInGB).asMegabytes();
        int memory = Double.valueOf(memoryInMB).intValue();

        int disk = imageEntry.getValue();
        String imageId = imageEntry.getKey();
        Map<String, String> requirementsMap = loadRequirementsMap(requirements);
        return new AwsHardwareRequirements(name, flavorId, cpu, memory, disk, imageId, requirementsMap);
    }

    @VisibleForTesting
    Map<String, String> loadRequirementsMap(String[] requirements) {
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

    @VisibleForTesting
    Map<String, Integer> generateImagesSizeMap(AwsV2User cloudUser) throws FogbowException {

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

    @VisibleForTesting
    int getImageSize(Image image) {
        int size = 0;
        for (BlockDeviceMapping device : image.blockDeviceMappings()) {
            size = device.ebs().volumeSize();
        }
        return size;
    }

    // The following methods are used to assist in testing.
	
    @VisibleForTesting
    String generateFlavorId() {
        return UUID.randomUUID().toString();
    }
	
    @VisibleForTesting
    String getFlavorsFilePath() {
        return flavorsFilePath;
    }
	
    @VisibleForTesting
    void setLaunchCommandGenerator(LaunchCommandGenerator launchCommandGenerator) {
        this.launchCommandGenerator = launchCommandGenerator;
    }

}