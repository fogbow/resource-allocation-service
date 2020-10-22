package cloud.fogbow.ras.core.plugins.interoperability.googlecloud.network.v1;

import cloud.fogbow.common.exceptions.FatalErrorException;
import cloud.fogbow.common.exceptions.FogbowException;
import cloud.fogbow.common.models.GoogleCloudUser;
import cloud.fogbow.ras.api.http.response.NetworkInstance;
import cloud.fogbow.ras.constants.Messages;
import cloud.fogbow.ras.core.models.orders.NetworkOrder;
import cloud.fogbow.ras.core.plugins.interoperability.NetworkPlugin;
import cloud.fogbow.ras.core.plugins.interoperability.googlecloud.sdk.v1.network.models.InsertNetworkRequest;
import cloud.fogbow.ras.core.plugins.interoperability.googlecloud.sdk.v1.network.models.InsertNetworkResponse;
import cloud.fogbow.ras.core.plugins.interoperability.googlecloud.sdk.v1.network.models.enums.RoutingMode;
import com.google.common.annotations.VisibleForTesting;
import org.apache.log4j.Logger;

public class GoogleCloudNetworkPlugin implements NetworkPlugin<GoogleCloudUser> {
    private static final Logger LOGGER = Logger.getLogger(GoogleCloudNetworkPlugin.class);

    @VisibleForTesting
    static final boolean DEFAULT_AUTO_CREATE_SUBNETWORKS = false;
    @VisibleForTesting
    static final RoutingMode DEFAULT_ROUTING_MODE = RoutingMode.GLOBAL;

    public GoogleCloudNetworkPlugin(String confFilePath) throws FatalErrorException{
        // TODO: wait until the cloud.conf is ready
    }

    @Override
    public String requestInstance(NetworkOrder networkOrder, GoogleCloudUser cloudUser) throws FogbowException {
        LOGGER.info(Messages.Log.REQUESTING_INSTANCE_FROM_PROVIDER);
        InsertNetworkResponse insertNetworkResponse = insertNetwork(networkOrder.getName(), cloudUser);
        return null;
    }

    @Override
    public NetworkInstance getInstance(NetworkOrder networkOrder, GoogleCloudUser cloudUser) throws FogbowException {
        //TODO: implement getInstance
        return null;
    }

    @Override
    public boolean isReady(String instanceState) {
        //TODO: implement isReady
        return false;
    }

    @Override
    public boolean hasFailed(String instanceState) {
        //TODO: implement hasFailed
        return false;
    }

    @Override
    public void deleteInstance(NetworkOrder networkOrder, GoogleCloudUser cloudUser) throws FogbowException {
        //TODO: implement deleteInstance
    }

    private InsertNetworkResponse insertNetwork(String networkName, GoogleCloudUser cloudUser) {
        InsertNetworkResponse insertNetworkResponse = null;
        InsertNetworkRequest insertNetworkRequest = new InsertNetworkRequest.Builder()
                .name(networkName)
                .autoCreateSubnetworks(DEFAULT_AUTO_CREATE_SUBNETWORKS)
                .routingMode(DEFAULT_ROUTING_MODE)
                .build();
        //TODO: build endpoint with the given path
        return  insertNetworkResponse;

    }
}
