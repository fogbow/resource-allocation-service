package cloud.fogbow.ras.core.plugins.interoperability.azure.network;

import cloud.fogbow.common.constants.AzureConstants;
import cloud.fogbow.common.exceptions.FogbowException;
import cloud.fogbow.common.models.AzureUser;
import cloud.fogbow.common.util.PropertiesUtil;
import cloud.fogbow.ras.api.http.response.InstanceState;
import cloud.fogbow.ras.api.http.response.NetworkInstance;
import cloud.fogbow.ras.constants.Messages;
import cloud.fogbow.ras.core.models.NetworkAllocationMode;
import cloud.fogbow.ras.core.models.ResourceType;
import cloud.fogbow.ras.core.models.orders.NetworkOrder;
import cloud.fogbow.ras.core.plugins.interoperability.NetworkPlugin;
import cloud.fogbow.ras.core.plugins.interoperability.azure.network.sdk.AzureVirtualNetworkOperationSDK;
import cloud.fogbow.ras.core.plugins.interoperability.azure.network.sdk.model.AzureCreateVirtualNetworkRef;
import cloud.fogbow.ras.core.plugins.interoperability.azure.network.sdk.model.AzureGetVirtualNetworkRef;
import cloud.fogbow.ras.core.plugins.interoperability.azure.util.AzureGeneralUtil;
import cloud.fogbow.ras.core.plugins.interoperability.azure.util.AzureStateMapper;
import com.google.common.annotations.VisibleForTesting;
import org.apache.log4j.Logger;

import java.util.Collections;
import java.util.Map;
import java.util.Properties;

public class AzureNetworkPlugin extends AzurePluginAsync implements NetworkPlugin<AzureUser> {

    private static final Logger LOGGER = Logger.getLogger(AzureNetworkPlugin.class);

    private AzureVirtualNetworkOperationSDK azureVirtualNetworkOperationSDK;

    public AzureNetworkPlugin(String confFilePath) {
        super();
        Properties properties = PropertiesUtil.readProperties(confFilePath);
        String defaultRegionName = properties.getProperty(AzureConstants.DEFAULT_REGION_NAME_KEY);
        String defaultResourceGroupName = properties.getProperty(AzureConstants.DEFAULT_RESOURCE_GROUP_NAME_KEY);
        this.azureVirtualNetworkOperationSDK = new AzureVirtualNetworkOperationSDK(defaultRegionName, defaultResourceGroupName);
    }

    @Override
    public boolean isReady(String instanceState) {
        return AzureStateMapper.map(ResourceType.NETWORK, instanceState).equals(InstanceState.READY);
    }

    // This method always must return false, because there is no state failed in the Azure context.
    @Override
    public boolean hasFailed(String instanceState) {
        return false;
    }

    @Override
    public String requestInstance(NetworkOrder networkOrder, AzureUser azureUser) throws FogbowException {
        LOGGER.info(Messages.Info.REQUESTING_INSTANCE_FROM_PROVIDER);

        String resourceName = AzureGeneralUtil.generateResourceName();
        String cidr = networkOrder.getCidr();
        String name = networkOrder.getName();
        Map tags = Collections.singletonMap(AzureConstants.TAG_NAME, name);

        String instanceId = AzureGeneralUtil.defineInstanceId(resourceName);
        AzureCreateVirtualNetworkRef azureCreateVirtualNetworkRef = AzureCreateVirtualNetworkRef.builder()
                .resourceName(resourceName)
                .cidr(cidr)
                .tags(tags)
                .checkAndBuild();

        Runnable finishAsyncCreationCallback = this.startAsyncCreation(instanceId);
        this.azureVirtualNetworkOperationSDK
                .doCreateInstance(azureCreateVirtualNetworkRef, azureUser, finishAsyncCreationCallback);

        return instanceId;
    }

    @Override
    public NetworkInstance getInstance(NetworkOrder networkOrder, AzureUser azureUser) throws FogbowException {
        LOGGER.info(String.format(Messages.Info.GETTING_INSTANCE_S, networkOrder.getInstanceId()));

        String instanceId = networkOrder.getInstanceId();
        if (this.isCreatingAsync(instanceId)) {
            return new NetworkInstance(instanceId, InstanceState.CREATING.getValue());
        }

        String resourceName = AzureGeneralUtil.defineResourceName(instanceId);
        AzureGetVirtualNetworkRef azureGetVirtualNetworkRef = this.azureVirtualNetworkOperationSDK
                .doGetInstance(resourceName, azureUser);

        return buildNetworkInstance(azureGetVirtualNetworkRef);
    }

    @VisibleForTesting
    NetworkInstance buildNetworkInstance(AzureGetVirtualNetworkRef azureGetVirtualNetworkRef) {
        String id = azureGetVirtualNetworkRef.getId();
        String cidr = azureGetVirtualNetworkRef.getCidr();
        String name = azureGetVirtualNetworkRef.getName();
        String state = azureGetVirtualNetworkRef.getState();

        String gateway = AzureGeneralUtil.NO_INFORMATION;
        String vlan = AzureGeneralUtil.NO_INFORMATION;
        String networkInterface = AzureGeneralUtil.NO_INFORMATION;
        String macInterface = AzureGeneralUtil.NO_INFORMATION;
        String interfaceState = AzureGeneralUtil.NO_INFORMATION;
        NetworkAllocationMode allocationMode = NetworkAllocationMode.DYNAMIC;

        return new NetworkInstance(id, state, name, cidr, gateway,
                vlan, allocationMode, networkInterface, macInterface, interfaceState);
    }

    @Override
    public void deleteInstance(NetworkOrder networkOrder, AzureUser azureUser) throws FogbowException {
        LOGGER.info(String.format(Messages.Info.DELETING_INSTANCE_S, networkOrder.getInstanceId()));

        String instanceId = networkOrder.getInstanceId();
        String resourceName = AzureGeneralUtil.defineResourceName(instanceId);
        this.azureVirtualNetworkOperationSDK.doDeleteInstance(resourceName, azureUser);
    }

    @VisibleForTesting
    void setAzureVirtualNetworkOperationSDK(AzureVirtualNetworkOperationSDK azureVirtualNetworkOperationSDK) {
        this.azureVirtualNetworkOperationSDK = azureVirtualNetworkOperationSDK;
    }

}
