package cloud.fogbow.ras.core.plugins.interoperability.azure.network;

import cloud.fogbow.common.exceptions.FogbowException;
import cloud.fogbow.common.models.AzureUser;
import cloud.fogbow.ras.api.http.response.NetworkInstance;
import cloud.fogbow.ras.constants.Messages;
import cloud.fogbow.ras.core.models.orders.NetworkOrder;
import cloud.fogbow.ras.core.plugins.interoperability.NetworkPlugin;
import cloud.fogbow.ras.core.plugins.interoperability.azure.network.sdk.AzureVirtualNetworkOperationSDK;
import org.apache.log4j.Logger;

public class AzureNetworkPlugin implements NetworkPlugin<AzureUser> {

    private static final Logger LOGGER = Logger.getLogger(AzureNetworkPlugin.class);

    private AzureVirtualNetworkOperationSDK azureVirtualNetworkOperationSDK;

    public AzureNetworkPlugin() {
        this.azureVirtualNetworkOperationSDK = new AzureVirtualNetworkOperationSDK();
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

        String cidr = networkOrder.getCidr();



        return null;
    }

    @Override
    public NetworkInstance getInstance(NetworkOrder networkOrder, AzureUser azureUser) throws FogbowException {
        return null;
    }

    @Override
    public void deleteInstance(NetworkOrder networkOrder, AzureUser azureUser) throws FogbowException {

    }

}
