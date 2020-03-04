package cloud.fogbow.ras.core.plugins.interoperability.azure.network;

import cloud.fogbow.common.exceptions.FogbowException;
import cloud.fogbow.common.models.CloudUser;
import cloud.fogbow.ras.api.http.response.NetworkInstance;
import cloud.fogbow.ras.core.models.orders.NetworkOrder;
import cloud.fogbow.ras.core.plugins.interoperability.NetworkPlugin;

public class AzureNetworkPlugin implements NetworkPlugin {

    @Override
    public boolean isReady(String instanceState) {
        return false;
    }

    @Override
    public boolean hasFailed(String instanceState) {
        return false;
    }

    @Override
    public String requestInstance(NetworkOrder networkOrder, CloudUser cloudUser) throws FogbowException {
        return null;
    }

    @Override
    public NetworkInstance getInstance(NetworkOrder networkOrder, CloudUser cloudUser) throws FogbowException {
        return null;
    }

    @Override
    public void deleteInstance(NetworkOrder networkOrder, CloudUser cloudUser) throws FogbowException {

    }

}
