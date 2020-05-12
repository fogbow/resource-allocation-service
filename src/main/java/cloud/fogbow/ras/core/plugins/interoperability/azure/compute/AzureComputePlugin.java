package cloud.fogbow.ras.core.plugins.interoperability.azure.compute;

import cloud.fogbow.common.constants.AzureConstants;
import cloud.fogbow.common.exceptions.FogbowException;
import cloud.fogbow.common.models.AzureUser;
import cloud.fogbow.common.util.PropertiesUtil;
import cloud.fogbow.ras.api.http.response.ComputeInstance;
import cloud.fogbow.ras.api.http.response.InstanceState;
import cloud.fogbow.ras.api.http.response.quotas.allocation.ComputeAllocation;
import cloud.fogbow.ras.constants.Messages;
import cloud.fogbow.ras.constants.SystemConstants;
import cloud.fogbow.ras.core.models.ResourceType;
import cloud.fogbow.ras.core.models.orders.ComputeOrder;
import cloud.fogbow.ras.core.plugins.interoperability.ComputePlugin;
import cloud.fogbow.ras.core.plugins.interoperability.azure.AzureAsync;
import cloud.fogbow.ras.core.plugins.interoperability.azure.compute.sdk.AzureVirtualMachineOperationSDK;
import cloud.fogbow.ras.core.plugins.interoperability.azure.compute.sdk.model.AzureCreateVirtualMachineRef;
import cloud.fogbow.ras.core.plugins.interoperability.azure.compute.sdk.model.AzureGetImageRef;
import cloud.fogbow.ras.core.plugins.interoperability.azure.compute.sdk.model.AzureGetVirtualMachineRef;
import cloud.fogbow.ras.core.plugins.interoperability.azure.util.*;
import cloud.fogbow.ras.core.plugins.interoperability.util.DefaultLaunchCommandGenerator;
import com.google.common.annotations.VisibleForTesting;
import com.microsoft.azure.management.compute.VirtualMachineSize;
import com.microsoft.azure.management.resources.fluentcore.utils.SdkContext;
import org.apache.log4j.Logger;

import java.util.*;

public class AzureComputePlugin implements ComputePlugin<AzureUser>, AzureAsync<ComputeInstance> {

    private static final Logger LOGGER = Logger.getLogger(AzureComputePlugin.class);
    private static final int INSTANCES_LAUNCH_NUMBER = 1;
    
    @VisibleForTesting
    static final String DEFAULT_OS_USER_NAME = "fogbow";

    private AzureVirtualMachineOperationSDK azureVirtualMachineOperation;
    private final DefaultLaunchCommandGenerator launchCommandGenerator;
    private final String defaultRegionName;
    private final String defaultResourceGroupName;
    private final String defaultVirtualNetworkName;

    public AzureComputePlugin(String confFilePath) {
        Properties properties = PropertiesUtil.readProperties(confFilePath);
        this.defaultRegionName = properties.getProperty(AzureConstants.DEFAULT_REGION_NAME_KEY);
        this.defaultResourceGroupName = properties.getProperty(AzureConstants.DEFAULT_RESOURCE_GROUP_NAME_KEY);
        this.defaultVirtualNetworkName = properties.getProperty(AzureConstants.DEFAULT_VIRTUAL_NETWORK_NAME_KEY);
        this.launchCommandGenerator = new DefaultLaunchCommandGenerator();
        this.azureVirtualMachineOperation = new AzureVirtualMachineOperationSDK(this.defaultRegionName);
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
    public String requestInstance(ComputeOrder computeOrder, AzureUser azureUser) throws FogbowException {
        LOGGER.info(Messages.Info.REQUESTING_INSTANCE_FROM_PROVIDER);

        String name = computeOrder.getName();
        String regionName = this.defaultRegionName;
        String resourceGroupName = this.defaultResourceGroupName;
        String resourceName = generateResourceName();
        String virtualNetworkId = getVirtualNetworkId(computeOrder, azureUser);
        String imageId = computeOrder.getImageId();
        AzureGetImageRef imageRef = AzureImageOperationUtil.buildAzureVirtualMachineImageBy(imageId);
        String osUserName = DEFAULT_OS_USER_NAME;
        String osUserPassword = AzureGeneralPolicy.generatePassword();
        String osComputeName = name;
        String userData = getUserData(computeOrder);
        VirtualMachineSize virtualMachineSize = getVirtualMachineSize(computeOrder, azureUser);
        String size = virtualMachineSize.name();
        int diskSize = AzureGeneralPolicy.getDisk(computeOrder);
        Map tags = Collections.singletonMap(AzureConstants.TAG_NAME, name);

        AzureCreateVirtualMachineRef azureCreateVirtualMachineRef = AzureCreateVirtualMachineRef.builder()
                .regionName(regionName)
                .resourceGroupName(resourceGroupName)
                .resourceName(resourceName)
                .virtualNetworkId(virtualNetworkId)
                .azureGetImageRef(imageRef)
                .osComputeName(osComputeName)
                .osUserName(osUserName)
                .osUserPassword(osUserPassword)
                .userData(userData)
                .diskSize(diskSize)
                .size(size)
                .tags(tags)
                .checkAndBuild();

        return doRequestInstance(computeOrder, azureUser, azureCreateVirtualMachineRef, virtualMachineSize);
    }

    @VisibleForTesting
    String generateResourceName() {
        return SdkContext.randomResourceName(SystemConstants.FOGBOW_INSTANCE_NAME_PREFIX, AzureConstants.MAXIMUM_RESOURCE_NAME_LENGTH);
    }

    @VisibleForTesting
    String getUserData(ComputeOrder computeOrder) {
        return this.launchCommandGenerator.createLaunchCommand(computeOrder);
    }

    @VisibleForTesting
    String doRequestInstance(
            ComputeOrder computeOrder, 
            AzureUser azureUser,
            AzureCreateVirtualMachineRef azureCreateVirtualMachineRef, 
            VirtualMachineSize virtualMachineSize) throws FogbowException {

        String instanceId = getInstanceId(azureCreateVirtualMachineRef);
        Runnable finishCreationCallback = this.startInstanceCreation(instanceId);
        doCreateInstance(azureUser, azureCreateVirtualMachineRef, finishCreationCallback);
        updateInstanceAllocation(computeOrder, virtualMachineSize);
        return instanceId;
    }

    @VisibleForTesting
    void doCreateInstance(AzureUser azureUser, AzureCreateVirtualMachineRef azureCreateVirtualMachineRef, Runnable finishCreationCallback) throws FogbowException {
        try {
            this.azureVirtualMachineOperation.doCreateInstance(azureCreateVirtualMachineRef, azureUser, finishCreationCallback);
        } catch (Exception e) {
            finishCreationCallback.run();
            throw e;
        }
    }

    @VisibleForTesting
    String getInstanceId(AzureCreateVirtualMachineRef azureCreateVirtualMachineRef) {
        return AzureGeneralUtil.defineInstanceId(azureCreateVirtualMachineRef.getResourceName());
    }
    
    @VisibleForTesting
    String getVirtualNetworkId(ComputeOrder computeOrder, AzureUser azureUser) {
        List<String> networkIdList = computeOrder.getNetworkIds();
        String resourceName = this.defaultVirtualNetworkName;
        if (!networkIdList.isEmpty()) {
            resourceName = networkIdList.listIterator().next();
        }
        return AzureResourceIdBuilder.networkId()
                .withSubscriptionId(azureUser.getSubscriptionId())
                .withResourceGroupName(this.defaultResourceGroupName)
                .withResourceName(resourceName)
                .build();
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
        LOGGER.info(String.format(Messages.Info.GETTING_INSTANCE_S, instanceId));

        ComputeInstance creatingInstance = getCreatingInstance(instanceId);
        if (creatingInstance != null) {
            return creatingInstance;
        }

        String resourceName = AzureGeneralUtil.defineResourceName(instanceId);
        String subscriptionId = azureUser.getSubscriptionId();
        String resourceId = buildResourceId(subscriptionId, resourceName);
        AzureGetVirtualMachineRef azureGetVirtualMachineRef = this.azureVirtualMachineOperation
                .doGetInstance(resourceId, azureUser);
        
        return buildComputeInstance(azureGetVirtualMachineRef, azureUser);
    }

    @VisibleForTesting
    String buildResourceId(String subscriptionId, String resourceName) {
        String resourceId = AzureResourceIdBuilder.virtualMachineId()
                .withSubscriptionId(subscriptionId)
                .withResourceGroupName(this.defaultResourceGroupName)
                .withResourceName(resourceName)
                .build();
        
        return resourceId;
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
    ComputeInstance buildComputeInstance(AzureGetVirtualMachineRef azureGetVirtualMachineRef, AzureUser azureUser) {
        String subscriptionId = azureUser.getSubscriptionId();
        String virtualMachineName = azureGetVirtualMachineRef.getName();
        String id = buildResourceId(subscriptionId, virtualMachineName);
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

        LOGGER.info(String.format(Messages.Info.DELETING_INSTANCE_S, computeOrder.getInstanceId()));
        String resourceName = computeOrder.getInstanceId();
        String subscriptionId = azureUser.getSubscriptionId();
        String instanceId = buildResourceId(subscriptionId, resourceName);
        this.azureVirtualMachineOperation.doDeleteInstance(instanceId, azureUser);
    }

    @VisibleForTesting
    void setAzureVirtualMachineOperation(AzureVirtualMachineOperationSDK azureVirtualMachineOperation) {
        this.azureVirtualMachineOperation = azureVirtualMachineOperation;
    }

    @Override
    public ComputeInstance buildCreatingInstance(String instanceId) {
        return new ComputeInstance(instanceId, InstanceState.CREATING.getValue()
                , AzureGeneralUtil.NO_INFORMATION, new ArrayList<>());
    }
}
