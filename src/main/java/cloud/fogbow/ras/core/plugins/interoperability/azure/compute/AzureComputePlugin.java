package cloud.fogbow.ras.core.plugins.interoperability.azure.compute;

import cloud.fogbow.common.constants.AzureConstants;
import cloud.fogbow.common.exceptions.FogbowException;
import cloud.fogbow.common.exceptions.InternalServerErrorException;
import cloud.fogbow.common.exceptions.InvalidParameterException;
import cloud.fogbow.common.exceptions.NotImplementedOperationException;
import cloud.fogbow.common.models.AwsV2User;
import cloud.fogbow.common.models.AzureUser;
import cloud.fogbow.common.models.CloudUser;
import cloud.fogbow.common.util.PropertiesUtil;
import cloud.fogbow.ras.api.http.response.ComputeInstance;
import cloud.fogbow.ras.api.http.response.InstanceState;
import cloud.fogbow.ras.api.http.response.quotas.allocation.ComputeAllocation;
import cloud.fogbow.ras.constants.Messages;
import cloud.fogbow.ras.core.models.ResourceType;
import cloud.fogbow.ras.core.models.orders.ComputeOrder;
import cloud.fogbow.ras.core.models.orders.Order;
import cloud.fogbow.ras.core.plugins.interoperability.ComputePlugin;
import cloud.fogbow.ras.core.plugins.interoperability.azure.AzureAsync;
import cloud.fogbow.ras.core.plugins.interoperability.azure.sdk.compute.AzureVirtualMachineOperationSDK;
import cloud.fogbow.ras.core.plugins.interoperability.azure.sdk.compute.model.AzureCreateVirtualMachineRef;
import cloud.fogbow.ras.core.plugins.interoperability.azure.sdk.compute.model.AzureGetImageRef;
import cloud.fogbow.ras.core.plugins.interoperability.azure.sdk.compute.model.AzureGetVirtualMachineRef;
import cloud.fogbow.ras.core.plugins.interoperability.azure.util.*;
import cloud.fogbow.ras.core.plugins.interoperability.util.DefaultLaunchCommandGenerator;
import com.google.common.annotations.VisibleForTesting;
import com.microsoft.azure.management.compute.VirtualMachineSize;
import org.apache.log4j.Logger;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Properties;

public class AzureComputePlugin implements ComputePlugin<AzureUser>, AzureAsync<ComputeInstance> {

    private static final Logger LOGGER = Logger.getLogger(AzureComputePlugin.class);
    private static final int INSTANCES_LAUNCH_NUMBER = 1;
    
    @VisibleForTesting
    static final String DEFAULT_OS_USER_NAME = "fogbow";
    @VisibleForTesting
    static final int MAXIMUM_SIZE_ALLOWED = 1;

    private AzureVirtualMachineOperationSDK azureVirtualMachineOperation;
    private DefaultLaunchCommandGenerator launchCommandGenerator;
    private final String defaultRegionName;
    private final String defaultResourceGroupName;
    private final String defaultVirtualNetworkName;

    public AzureComputePlugin(String confFilePath) {
        Properties properties = PropertiesUtil.readProperties(confFilePath);
        this.defaultRegionName = properties.getProperty(AzureConstants.DEFAULT_REGION_NAME_KEY);
        this.defaultResourceGroupName = properties.getProperty(AzureConstants.DEFAULT_RESOURCE_GROUP_NAME_KEY);
        this.defaultVirtualNetworkName = properties.getProperty(AzureConstants.DEFAULT_VIRTUAL_NETWORK_NAME_KEY);
        this.launchCommandGenerator = new DefaultLaunchCommandGenerator();
        this.azureVirtualMachineOperation = new AzureVirtualMachineOperationSDK(this.defaultRegionName,
                this.defaultResourceGroupName);
    }

    @Override
    public boolean isReady(String instanceState) {
        return AzureStateMapper.map(ResourceType.COMPUTE, instanceState).equals(InstanceState.READY);
    }

    @Override
    public boolean hasFailed(String instanceState) {
        return AzureStateMapper.map(ResourceType.COMPUTE, instanceState).equals(InstanceState.FAILED);
    }
    
    @Override
    public boolean isPaused(String cloudState) {
        return false;
    }

    @Override
    public boolean isHibernated(String cloudState) {
        return false;
    }
    
    @Override
    public boolean isStopped(String instanceState) {
        return AzureStateMapper.map(ResourceType.COMPUTE, instanceState).equals(InstanceState.STOPPED);
    }

    @Override
    public String requestInstance(ComputeOrder computeOrder, AzureUser azureUser) throws FogbowException {
        LOGGER.info(Messages.Log.REQUESTING_INSTANCE_FROM_PROVIDER);
        String name = computeOrder.getName();
        String regionName = this.defaultRegionName;
        String resourceName = AzureGeneralUtil.generateResourceName();
        String virtualNetworkName = getVirtualNetworkResourceName(computeOrder);
        String imageId = computeOrder.getImageId();
        String decodedImageId = AzureImageOperationUtil.decode(imageId);
        AzureGetImageRef imageRef = AzureImageOperationUtil.buildAzureVirtualMachineImageBy(decodedImageId);
        String osUserName = DEFAULT_OS_USER_NAME;
        String osUserPassword = AzureGeneralPolicy.generatePassword();
        String osComputeName = name;
        String userData = getUserData(computeOrder);
        VirtualMachineSize virtualMachineSize = getVirtualMachineSize(computeOrder, azureUser);
        String size = virtualMachineSize.name();
        int diskSize = computeOrder.getDisk();
        Map tags = Collections.singletonMap(AzureConstants.TAG_NAME, name);

        AzureCreateVirtualMachineRef virtualMachineRef = AzureCreateVirtualMachineRef.builder()
                .regionName(regionName)
                .resourceName(resourceName)
                .virtualNetworkName(virtualNetworkName)
                .azureGetImageRef(imageRef)
                .osComputeName(osComputeName)
                .osUserName(osUserName)
                .osUserPassword(osUserPassword)
                .userData(userData)
                .diskSize(diskSize)
                .size(size)
                .tags(tags)
                .checkAndBuild();

        return doRequestInstance(computeOrder, azureUser, virtualMachineRef, virtualMachineSize);
    }

    @VisibleForTesting
    String getUserData(ComputeOrder computeOrder) throws InternalServerErrorException {
        return this.launchCommandGenerator.createLaunchCommand(computeOrder);
    }

    @VisibleForTesting
    String doRequestInstance(ComputeOrder computeOrder, AzureUser azureUser, 
            AzureCreateVirtualMachineRef virtualMachineRef, VirtualMachineSize virtualMachineSize)
            throws FogbowException {

        String instanceId = getInstanceId(virtualMachineRef);
        AsyncInstanceCreationManager.Callbacks finishCreationCallback = startInstanceCreation(instanceId);
        doCreateInstance(azureUser, virtualMachineRef, finishCreationCallback);
        waitAndCheckForInstanceCreationFailed(instanceId);
        updateInstanceAllocation(computeOrder, virtualMachineSize);
        return instanceId;
    }

    @VisibleForTesting
    void doCreateInstance(AzureUser azureUser, AzureCreateVirtualMachineRef virtualMachineRef,
                          AsyncInstanceCreationManager.Callbacks finishCreationCallbacks) throws FogbowException {
        
        try {
            this.azureVirtualMachineOperation
                    .doCreateInstance(virtualMachineRef, finishCreationCallbacks, azureUser);
        } catch (Exception e) {
            finishCreationCallbacks.runOnError(e.getMessage());
            throw e;
        }
    }

    @VisibleForTesting
    String getInstanceId(AzureCreateVirtualMachineRef azureCreateVirtualMachineRef) {
        return AzureGeneralUtil.defineInstanceId(azureCreateVirtualMachineRef.getResourceName());
    }
    
    @VisibleForTesting
    String getVirtualNetworkResourceName(ComputeOrder computeOrder) throws FogbowException {
        List<String> networkIdList = computeOrder.getNetworkIds();
        String resourceName = this.defaultVirtualNetworkName;
        if (!networkIdList.isEmpty()) {
            checkNetworkIdListIntegrity(networkIdList);
            resourceName = networkIdList.listIterator().next();
        }
        return resourceName;
    }

    @VisibleForTesting
    void checkNetworkIdListIntegrity(List<String> networkIdList) throws InvalidParameterException {
        if (networkIdList.size() > MAXIMUM_SIZE_ALLOWED) {
            throw new InvalidParameterException(Messages.Exception.MANY_NETWORKS_NOT_ALLOWED);
        }
    }

    @VisibleForTesting
    VirtualMachineSize getVirtualMachineSize(ComputeOrder computeOrder, AzureUser azureCloudUser)
            throws FogbowException {

        return this.azureVirtualMachineOperation.findVirtualMachineSize(computeOrder.getRam(),
                computeOrder.getvCPU(), this.defaultRegionName, azureCloudUser);
    }

    @Override
    public ComputeInstance getInstance(ComputeOrder computeOrder, AzureUser azureUser) throws FogbowException {
        String instanceId = computeOrder.getInstanceId();
        LOGGER.info(String.format(Messages.Log.GETTING_INSTANCE_S, instanceId));

        ComputeInstance creatingInstance = getCreatingInstance(instanceId);
        if (creatingInstance != null) {
            return creatingInstance;
        }
        
        String resourceName = AzureGeneralUtil.defineResourceName(instanceId);
        AzureGetVirtualMachineRef azureGetVirtualMachineRef = this.azureVirtualMachineOperation
                .doGetInstance(azureUser, resourceName);
        
        return buildComputeInstance(azureGetVirtualMachineRef);
    }

    @VisibleForTesting
    void updateInstanceAllocation(ComputeOrder computeOrder, VirtualMachineSize virtualMachineSize) {
        synchronized (computeOrder) {
            int vCPU = virtualMachineSize.numberOfCores();
            int memory = virtualMachineSize.memoryInMB();
            int disk = computeOrder.getDisk();
            int instances = INSTANCES_LAUNCH_NUMBER;
            ComputeAllocation actualAllocation = new ComputeAllocation(instances, vCPU, memory, disk);
            computeOrder.setActualAllocation(actualAllocation);
        }
    }

    @VisibleForTesting
    ComputeInstance buildComputeInstance(AzureGetVirtualMachineRef azureGetVirtualMachineRef) {
        String id = azureGetVirtualMachineRef.getId();
        String cloudState = azureGetVirtualMachineRef.getCloudState();
        String name = azureGetVirtualMachineRef.getTags().get(AzureConstants.TAG_NAME);
        int vCPU = azureGetVirtualMachineRef.getvCPU();
        int memory = azureGetVirtualMachineRef.getMemory();
        int disk = azureGetVirtualMachineRef.getDisk();
        List<String> ipAddresses = azureGetVirtualMachineRef.getIpAddresses();

        return new ComputeInstance(id, cloudState, name, vCPU, memory, disk, ipAddresses);
    }

    @Override
    public void deleteInstance(ComputeOrder computeOrder, AzureUser azureUser)
            throws FogbowException {

        LOGGER.info(String.format(Messages.Log.DELETING_INSTANCE_S, computeOrder.getInstanceId()));
        String instanceId = computeOrder.getInstanceId();
        String resourceName = AzureGeneralUtil.defineResourceName(instanceId);
        this.azureVirtualMachineOperation.doDeleteInstance(azureUser, resourceName);
        endInstanceCreation(instanceId);
    }

    @Override
    public void takeSnapshot(ComputeOrder computeOrder, String name, AzureUser cloudUser) throws FogbowException {
        throw new NotImplementedOperationException();
    }

    @Override
    public void pauseInstance(ComputeOrder order, AzureUser cloudUser) throws FogbowException {
        throw new NotImplementedOperationException();
    }

    @Override
    public void hibernateInstance(ComputeOrder order, AzureUser cloudUser) throws FogbowException {
    	throw new NotImplementedOperationException();
    }

    @Override
    public void stopInstance(ComputeOrder computeOrder, AzureUser azureUser) throws FogbowException {
        LOGGER.info(String.format(Messages.Log.STOPPING_INSTANCE_S, computeOrder.getInstanceId()));
        String instanceId = computeOrder.getInstanceId();
        String resourceName = AzureGeneralUtil.defineResourceName(instanceId);
        
        this.azureVirtualMachineOperation.doStopInstance(azureUser, resourceName);
    }
    
    @Override
    public void resumeInstance(ComputeOrder computeOrder, AzureUser azureUser) throws FogbowException {
        LOGGER.info(String.format(Messages.Log.RESUMING_INSTANCE_S, computeOrder.getInstanceId()));
    	String instanceId = computeOrder.getInstanceId();
        String resourceName = AzureGeneralUtil.defineResourceName(instanceId);
        
        this.azureVirtualMachineOperation.doResumeInstance(azureUser, resourceName);
    }

    @VisibleForTesting
    void setAzureVirtualMachineOperation(AzureVirtualMachineOperationSDK azureVirtualMachineOperation) {
        this.azureVirtualMachineOperation = azureVirtualMachineOperation;
    }

    @VisibleForTesting
    void setLaunchCommandGenerator(DefaultLaunchCommandGenerator launchCommandGenerator) {
        this.launchCommandGenerator = launchCommandGenerator;
    }

    @Override
    public ComputeInstance buildCreatingInstance(String instanceId) {
        return new ComputeInstance(instanceId);
    }

}
