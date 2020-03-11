package cloud.fogbow.ras.core.plugins.interoperability.azure.network;

import cloud.fogbow.common.constants.AzureConstants;
import cloud.fogbow.common.exceptions.FogbowException;
import cloud.fogbow.common.models.AzureUser;
import cloud.fogbow.common.util.PropertiesUtil;
import cloud.fogbow.ras.api.http.response.NetworkInstance;
import cloud.fogbow.ras.constants.Messages;
import cloud.fogbow.ras.core.models.NetworkAllocationMode;
import cloud.fogbow.ras.core.models.orders.NetworkOrder;
import cloud.fogbow.ras.core.plugins.interoperability.NetworkPlugin;
import cloud.fogbow.ras.core.plugins.interoperability.azure.network.sdk.AzureVirtualNetworkOperationSDK;
import cloud.fogbow.ras.core.plugins.interoperability.azure.network.sdk.model.AzureCreateVirtualNetworkRef;
import cloud.fogbow.ras.core.plugins.interoperability.azure.network.sdk.model.AzureGetVirtualNetworkRef;
import cloud.fogbow.ras.core.plugins.interoperability.azure.util.AzureInstancePolicy;
import com.google.common.annotations.VisibleForTesting;
import org.apache.log4j.Logger;

import java.util.Properties;

public class AzureNetworkPlugin implements NetworkPlugin<AzureUser> {

    private static final Logger LOGGER = Logger.getLogger(AzureNetworkPlugin.class);

    private static final String NO_INFORMATION = null;

    private AzureVirtualNetworkOperationSDK azureVirtualNetworkOperationSDK;
    private final String defaultResourceGroupName;

    public AzureNetworkPlugin(String confFilePath) {
        Properties properties = PropertiesUtil.readProperties(confFilePath);
        String defaultRegionName = properties.getProperty(AzureConstants.DEFAULT_REGION_NAME_KEY);
        this.defaultResourceGroupName = properties.getProperty(AzureConstants.DEFAULT_RESOURCE_GROUP_NAME_KEY);
        this.azureVirtualNetworkOperationSDK = new AzureVirtualNetworkOperationSDK(defaultRegionName);
    }

    @Override
    public boolean isReady(String instanceState) {
        return false;
    }

    @Override
    public boolean hasFailed(String instanceState) {
        return false;
    }

    @Override
    public String requestInstance(NetworkOrder networkOrder, AzureUser azureUser) throws FogbowException {
        LOGGER.info(Messages.Info.REQUESTING_INSTANCE_FROM_PROVIDER);

        String name = networkOrder.getName();
        String cidr = networkOrder.getCidr();

        AzureCreateVirtualNetworkRef azureCreateVirtualNetworkRef = AzureCreateVirtualNetworkRef.builder()
                .name(name)
                .cidr(cidr)
                .resourceGroupName(this.defaultResourceGroupName)
                .checkAndBuild();
        this.azureVirtualNetworkOperationSDK.doCreateInstance(azureCreateVirtualNetworkRef, azureUser);

        return AzureInstancePolicy.generateFogbowInstanceId(networkOrder, azureUser, this.defaultResourceGroupName);
    }

    // TODO(chico) - Implement tests
    @Override
    public NetworkInstance getInstance(NetworkOrder networkOrder, AzureUser azureUser) throws FogbowException {
        LOGGER.info(String.format(Messages.Info.GETTING_INSTANCE_S, networkOrder.getInstanceId()));

        String instanceId = networkOrder.getInstanceId();

        AzureGetVirtualNetworkRef azureGetVirtualNetworkRef = this.azureVirtualNetworkOperationSDK
                .doGetInstance(instanceId, azureUser);

        return buildNetworkInstance(azureGetVirtualNetworkRef);
    }

    // TODO(chico) - Implement tests
    @VisibleForTesting
    NetworkInstance buildNetworkInstance(AzureGetVirtualNetworkRef azureGetVirtualNetworkRef) {
        String id = azureGetVirtualNetworkRef.getId();
        String cidr = azureGetVirtualNetworkRef.getCidr();
        String name = azureGetVirtualNetworkRef.getName();
        String state = azureGetVirtualNetworkRef.getState();

        String gateway = NO_INFORMATION;
        String vlan = NO_INFORMATION;
        String networkInterface = NO_INFORMATION;
        String macInterface = NO_INFORMATION;
        String interfaceState = NO_INFORMATION;

        NetworkAllocationMode allocationMode = NetworkAllocationMode.DYNAMIC;
        return new NetworkInstance(id, state, name, cidr, gateway,
                vlan, allocationMode, networkInterface, macInterface, interfaceState);
    }

    @Override
    public void deleteInstance(NetworkOrder networkOrder, AzureUser azureUser) throws FogbowException {
    }

    @VisibleForTesting
    void setAzureVirtualNetworkOperationSDK(AzureVirtualNetworkOperationSDK azureVirtualNetworkOperationSDK) {
        this.azureVirtualNetworkOperationSDK = azureVirtualNetworkOperationSDK;
    }

}
