package cloud.fogbow.ras.core.plugins.interoperability.azure.compute;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Properties;

import org.apache.log4j.Logger;

import com.google.common.annotations.VisibleForTesting;
import com.microsoft.azure.management.compute.VirtualMachineSize;
import com.microsoft.azure.management.resources.fluentcore.utils.SdkContext;

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
import cloud.fogbow.ras.core.plugins.interoperability.azure.compute.sdk.AzureVirtualMachineOperationSDK;
import cloud.fogbow.ras.core.plugins.interoperability.azure.compute.sdk.model.AzureCreateVirtualMachineRef;
import cloud.fogbow.ras.core.plugins.interoperability.azure.compute.sdk.model.AzureGetImageRef;
import cloud.fogbow.ras.core.plugins.interoperability.azure.compute.sdk.model.AzureGetVirtualMachineRef;
import cloud.fogbow.ras.core.plugins.interoperability.azure.util.AzureGeneralPolicy;
import cloud.fogbow.ras.core.plugins.interoperability.azure.util.AzureImageOperationUtil;
import cloud.fogbow.ras.core.plugins.interoperability.azure.util.AzureResourceIdBuilder;
import cloud.fogbow.ras.core.plugins.interoperability.azure.util.AzureStateMapper;
import cloud.fogbow.ras.core.plugins.interoperability.util.DefaultLaunchCommandGenerator;

public class AzureComputePlugin implements ComputePlugin<AzureUser> {

    private static final Logger LOGGER = Logger.getLogger(AzureComputePlugin.class);
    private static final int INSTANCES_LAUNCH_NUMBER = 1;
    
    @VisibleForTesting
    static final String DEFAULT_OS_USER_NAME = "fogbow";

    private AzureVirtualMachineOperationSDK azureVirtualMachineOperation;
    private final DefaultLaunchCommandGenerator launchCommandGenerator;
    private final String defaultNetworkInterfaceName;
    private final String defaultRegionName;
    private final String defaultResourceGroupName;

    public AzureComputePlugin(String confFilePath) {
        Properties properties = PropertiesUtil.readProperties(confFilePath);
        this.defaultNetworkInterfaceName = properties.getProperty(AzureConstants.DEFAULT_NETWORK_INTERFACE_NAME_KEY);
        this.defaultRegionName = properties.getProperty(AzureConstants.DEFAULT_REGION_NAME_KEY);
        this.defaultResourceGroupName = properties.getProperty(AzureConstants.DEFAULT_RESOURCE_GROUP_NAME_KEY);
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
        String networkInterfaceId = getNetworkInterfaceId(computeOrder, azureUser);
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
                .networkInterfaceId(networkInterfaceId)
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

        this.azureVirtualMachineOperation.doCreateInstance(azureCreateVirtualMachineRef, azureUser);
        updateInstanceAllocation(computeOrder, virtualMachineSize);
        return getInstanceId(azureCreateVirtualMachineRef);
    }

    @VisibleForTesting
    String getInstanceId(AzureCreateVirtualMachineRef azureCreateVirtualMachineRef) {
        return azureCreateVirtualMachineRef.getResourceName();
    }

    @VisibleForTesting
    String getNetworkInterfaceId(ComputeOrder computeOrder, AzureUser azureCloudUser) throws FogbowException {
        List<String> networkIds = computeOrder.getNetworkIds();
        if (networkIds.isEmpty()) {
            return AzureResourceIdBuilder.networkInterfaceId()
                    .withSubscriptionId(azureCloudUser.getSubscriptionId())
                    .withResourceGroupName(this.defaultResourceGroupName)
                    .withResourceName(this.defaultNetworkInterfaceName)
                    .build();
        } else {
            if (networkIds.size() > AzureGeneralPolicy.MAXIMUM_NETWORK_PER_VIRTUAL_MACHINE) {
                throw new FogbowException(Messages.Error.ERROR_MULTIPLE_NETWORKS_NOT_ALLOWED);
            }
            return networkIds.stream().findFirst().get();
        }
    }

    @VisibleForTesting
    VirtualMachineSize getVirtualMachineSize(ComputeOrder computeOrder, AzureUser azureCloudUser)
            throws FogbowException {

        return this.azureVirtualMachineOperation.findVirtualMachineSize(computeOrder.getMemory(),
                computeOrder.getvCPU(), this.defaultRegionName, azureCloudUser);
    }

    @Override
    public ComputeInstance getInstance(ComputeOrder computeOrder, AzureUser azureUser)
            throws FogbowException {

        LOGGER.info(String.format(Messages.Info.GETTING_INSTANCE_S, computeOrder.getInstanceId()));
        String resourceName = computeOrder.getInstanceId();
        String subscriptionId = azureUser.getSubscriptionId();
        String instanceIdUrl = buildResourceIdUrl(subscriptionId, resourceName);
                
        AzureGetVirtualMachineRef azureGetVirtualMachineRef = this.azureVirtualMachineOperation
                .doGetInstance(instanceIdUrl, azureUser);
        
        return buildComputeInstance(azureGetVirtualMachineRef, azureUser);
    }

    @VisibleForTesting
    String buildResourceIdUrl(String subscriptionId, String resourceName) {
        String resourceIdUrl = AzureResourceIdBuilder.virtualMachineId()
                .withSubscriptionId(subscriptionId)
                .withResourceGroupName(this.defaultResourceGroupName)
                .withResourceName(resourceName)
                .build();
        
        return resourceIdUrl;
    }

    @VisibleForTesting
    void updateInstanceAllocation(ComputeOrder computeOrder, VirtualMachineSize virtualMachineSize) {
        synchronized (computeOrder) {
            int vCPU = virtualMachineSize.numberOfCores();
            int memory = virtualMachineSize.memoryInMB();
            int disk = computeOrder.getDisk();
            int instances = INSTANCES_LAUNCH_NUMBER;
            ComputeAllocation actualAllocation = new ComputeAllocation(vCPU, memory, instances, disk);
            computeOrder.setActualAllocation(actualAllocation);
        }
    }

    @VisibleForTesting
    ComputeInstance buildComputeInstance(AzureGetVirtualMachineRef azureGetVirtualMachineRef, AzureUser azureUser) {
        String subscriptionId = azureUser.getSubscriptionId();
        String virtualMachineName = azureGetVirtualMachineRef.getName();
        String id = buildResourceIdUrl(subscriptionId, virtualMachineName);
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
        String instanceIdUrl = buildResourceIdUrl(subscriptionId, resourceName);
        this.azureVirtualMachineOperation.doDeleteInstance(instanceIdUrl, azureUser);
    }

    @VisibleForTesting
    void setAzureVirtualMachineOperation(AzureVirtualMachineOperationSDK azureVirtualMachineOperation) {
        this.azureVirtualMachineOperation = azureVirtualMachineOperation;
    }

}
