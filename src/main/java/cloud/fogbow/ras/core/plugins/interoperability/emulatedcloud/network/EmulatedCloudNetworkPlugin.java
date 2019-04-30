package cloud.fogbow.ras.core.plugins.interoperability.emulatedcloud.network;

import cloud.fogbow.common.exceptions.FogbowException;
import cloud.fogbow.common.models.CloudUser;
import cloud.fogbow.common.util.PropertiesUtil;
import cloud.fogbow.ras.api.http.response.NetworkInstance;
import cloud.fogbow.ras.constants.SystemConstants;
import cloud.fogbow.ras.core.models.orders.NetworkOrder;
import cloud.fogbow.ras.core.plugins.interoperability.NetworkPlugin;
import cloud.fogbow.ras.core.plugins.interoperability.emulatedcloud.EmulatedPluginFileUtils;

import java.io.IOException;
import java.util.HashMap;
import java.util.Properties;
import java.util.UUID;

public class EmulatedCloudNetworkPlugin implements NetworkPlugin<CloudUser> {

    private Properties properties;

    private static final String NETOWRK_NAME = "name";
    private static final String NETOWRK_ID = "id";
    private static final String NETOWRK_CIDR = "cidr";
    private static final String NETOWRK_CLOUD_NAME = "cloudName";
    private static final String NETOWRK_GATEWAY = "gateway";
    private static final String NETOWRK_PROVIDER = "provider";

    public EmulatedCloudNetworkPlugin(String confFilePath) {
        this.properties = PropertiesUtil.readProperties(confFilePath);
    }

    @Override
    public String requestInstance(NetworkOrder networkOrder, CloudUser cloudUser) throws FogbowException {
        HashMap<String, String> network = createNetowrk(networkOrder);

        String networkId = network.get(NETOWRK_ID);
        String networkPath = EmulatedPluginFileUtils.getResourcePath(this.properties, networkId);

        try {
            EmulatedPluginFileUtils.saveHashMapAsJson(networkPath, network);
        } catch (IOException e) {
            throw new FogbowException((e.getMessage()));
        }

        return networkId;
    }

    @Override
    public NetworkInstance getInstance(NetworkOrder networkOrder, CloudUser cloudUser) throws FogbowException {
        return null;
    }

    @Override
    public boolean isReady(String instanceState) {
        return true;
    }

    @Override
    public boolean hasFailed(String instanceState) {
        return false;
    }

    @Override
    public void deleteInstance(NetworkOrder networkOrder, CloudUser cloudUser) throws FogbowException {

    }

    private HashMap<String, String> createNetowrk(NetworkOrder networkOrder) {

        String networkId = getRandomUUID();
        String networkName = getName(networkOrder);
        String cidr = networkOrder.getCidr();
        String cloudName = networkOrder.getCloudName();
        String gateway = networkOrder.getGateway();
        String provider = networkOrder.getProvider();

        HashMap<String, String> network = new HashMap<String, String>();

        network.put(NETOWRK_NAME, networkName);
        network.put(NETOWRK_ID, networkId);
        network.put(NETOWRK_CIDR, cidr);
        network.put(NETOWRK_CLOUD_NAME, cloudName);
        network.put(NETOWRK_GATEWAY, gateway);
        network.put(NETOWRK_PROVIDER, provider);

        return network;
    }


    protected String getRandomUUID() {
        return UUID.randomUUID().toString();
    }

    protected String getName(NetworkOrder networkOrder){
        String name = networkOrder.getName();
        return (name == null ? SystemConstants.FOGBOW_INSTANCE_NAME_PREFIX + getRandomUUID() : name);
    }
}

