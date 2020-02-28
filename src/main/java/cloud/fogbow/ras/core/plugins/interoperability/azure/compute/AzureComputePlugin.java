package cloud.fogbow.ras.core.plugins.interoperability.azure.compute;

import java.util.List;
import java.util.Properties;

import org.apache.log4j.Logger;

import com.google.common.annotations.VisibleForTesting;
import com.microsoft.azure.management.compute.VirtualMachineSize;

import cloud.fogbow.common.constants.AzureConstants;
import cloud.fogbow.common.exceptions.FogbowException;
import cloud.fogbow.common.models.AzureUser;
import cloud.fogbow.common.util.PropertiesUtil;
import cloud.fogbow.ras.api.http.response.ComputeInstance;
import cloud.fogbow.ras.api.http.response.InstanceState;
import cloud.fogbow.ras.api.http.response.quotas.allocation.ComputeAllocation;
import cloud.fogbow.ras.constants.Messages;
import cloud.fogbow.ras.core.models.ResourceType;
import cloud.fogbow.ras.core.models.orders.ComputeOrder;
import cloud.fogbow.ras.core.plugins.interoperability.ComputePlugin;
import cloud.fogbow.ras.core.plugins.interoperability.azure.compute.sdk.AzureVirtualMachineOperationSDK;
import cloud.fogbow.ras.core.plugins.interoperability.azure.compute.sdk.model.AzureCreateVirtualMachineRef;
import cloud.fogbow.ras.core.plugins.interoperability.azure.compute.sdk.model.AzureGetImageRef;
import cloud.fogbow.ras.core.plugins.interoperability.azure.compute.sdk.model.AzureGetVirtualMachineRef;
import cloud.fogbow.ras.core.plugins.interoperability.azure.util.AzureGeneralPolicy;
import cloud.fogbow.ras.core.plugins.interoperability.azure.util.AzureImageOperationUtil;
import cloud.fogbow.ras.core.plugins.interoperability.azure.util.AzureInstancePolicy;
import cloud.fogbow.ras.core.plugins.interoperability.azure.util.AzureResourceIdBuilder;
import cloud.fogbow.ras.core.plugins.interoperability.azure.util.AzureStateMapper;
import cloud.fogbow.ras.core.plugins.interoperability.util.DefaultLaunchCommandGenerator;

public class AzureComputePlugin implements ComputePlugin<AzureUser> {

    private static final Logger LOGGER = Logger.getLogger(AzureComputePlugin.class);
    private static final int INSTANCES_LAUNCH_NUMBER = 1;
    
    @VisibleForTesting
    static final String DEFAULT_OS_USER_NAME = "fogbow";

    private AzureVirtualMachineOperation azureVirtualMachineOperation;
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

        String networkInterfaceId = getNetworkInterfaceId(computeOrder, azureUser);
        VirtualMachineSize virtualMachineSize = getVirtualMachineSize(computeOrder, azureUser);
        int diskSize = AzureGeneralPolicy.getDisk(computeOrder);
        String imageId = computeOrder.getImageId();
        AzureGetImageRef imageRef = AzureImageOperationUtil.buildAzureVirtualMachineImageBy(imageId);
        String regionName = this.defaultRegionName;
        String resourceGroupName = this.defaultResourceGroupName;
        String virtualMachineName = AzureInstancePolicy.checkAzureResourceName(computeOrder, azureUser, resourceGroupName);
        String userData = getUserData(computeOrder);
        String osUserName = DEFAULT_OS_USER_NAME;
        String osUserPassword = AzureGeneralPolicy.generatePassword();
        String osComputeName = computeOrder.getName();

        AzureCreateVirtualMachineRef azureCreateVirtualMachineRef = AzureCreateVirtualMachineRef.builder()
                .virtualMachineName(virtualMachineName)
                .azureGetImageRef(imageRef)
                .networkInterfaceId(networkInterfaceId)
                .diskSize(diskSize)
                .size(virtualMachineSize.name())
                .osComputeName(osComputeName)
                .osUserName(osUserName)
                .osUserPassword(osUserPassword)
                .regionName(regionName)
                .resourceGroupName(resourceGroupName)
                .userData(userData)
                .checkAndBuild();

        return doRequestInstance(computeOrder, azureUser, azureCreateVirtualMachineRef, virtualMachineSize);
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
        return AzureInstancePolicy.generateFogbowInstanceId(computeOrder, azureUser, this.defaultResourceGroupName);
    }

    @VisibleForTesting
    String getNetworkInterfaceId(ComputeOrder computeOrder, AzureUser azureCloudUser) throws FogbowException {
        List<String> networkIds = computeOrder.getNetworkIds();
        if (networkIds.isEmpty()) {
            return AzureResourceIdBuilder.configure(AzureConstants.NETWORK_INTERFACE_STRUCTURE)
                    .withSubscriptionId(azureCloudUser.getSubscriptionId())
                    .withResourceGroupName(this.defaultResourceGroupName)
                    .withResourceName(this.defaultNetworkInterfaceName)
                    .buildResourceId();
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
        String azureVirtualMachineId = computeOrder.getInstanceId();

        AzureGetVirtualMachineRef azureGetVirtualMachineRef = this.azureVirtualMachineOperation
                .doGetInstance(azureVirtualMachineId, azureUser);
        
        return buildComputeInstance(azureGetVirtualMachineRef, azureUser);
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
        String virtualMachineName = azureGetVirtualMachineRef.getName();
        String virtualMachineId = AzureResourceIdBuilder.configure(AzureConstants.VIRTUAL_MACHINE_STRUCTURE)
                .withSubscriptionId(azureUser.getSubscriptionId())
                .withResourceGroupName(this.defaultResourceGroupName)
                .withResourceName(virtualMachineName)
                .buildResourceId();
        
        String cloudState = azureGetVirtualMachineRef.getCloudState();
        int vCPU = azureGetVirtualMachineRef.getvCPU();
        int memory = azureGetVirtualMachineRef.getMemory();
        int disk = azureGetVirtualMachineRef.getDisk();
        List<String> ipAddresses = azureGetVirtualMachineRef.getIpAddresses();

        return new ComputeInstance(virtualMachineId, cloudState, virtualMachineName, vCPU, memory, disk, ipAddresses);
    }

    @Override
    public void deleteInstance(ComputeOrder computeOrder, AzureUser azureCloudUser)
            throws FogbowException {

        LOGGER.info(String.format(Messages.Info.DELETING_INSTANCE_S, computeOrder.getInstanceId()));

        String azureVirtualMachineId = computeOrder.getInstanceId();
        this.azureVirtualMachineOperation.doDeleteInstance(azureVirtualMachineId, azureCloudUser);
    }

    @VisibleForTesting
    void setAzureVirtualMachineOperation(AzureVirtualMachineOperation azureVirtualMachineOperation) {
        this.azureVirtualMachineOperation = azureVirtualMachineOperation;
    }

}
