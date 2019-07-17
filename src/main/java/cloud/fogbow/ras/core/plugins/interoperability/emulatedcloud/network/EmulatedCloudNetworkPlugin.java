package cloud.fogbow.ras.core.plugins.interoperability.emulatedcloud.network;

import cloud.fogbow.common.exceptions.FogbowException;
import cloud.fogbow.common.exceptions.InstanceNotFoundException;
import cloud.fogbow.common.models.CloudUser;
import cloud.fogbow.common.util.PropertiesUtil;
import cloud.fogbow.ras.api.http.response.NetworkInstance;
import cloud.fogbow.ras.constants.Messages;
import cloud.fogbow.ras.core.models.NetworkAllocationMode;
import cloud.fogbow.ras.core.models.orders.NetworkOrder;
import cloud.fogbow.ras.core.plugins.interoperability.NetworkPlugin;
import cloud.fogbow.ras.core.plugins.interoperability.emulatedcloud.EmulatedCloudUtils;
import org.apache.log4j.Logger;

import java.io.IOException;
import java.util.HashMap;
import java.util.Properties;
import java.util.Random;

public class EmulatedCloudNetworkPlugin implements NetworkPlugin<CloudUser> {

    private static final Logger LOGGER = Logger.getLogger(EmulatedCloudNetworkPlugin.class);

    private Properties properties;

    private static final String NETWORK_ALLOCATION_MODE = "allocationMode";
    private static final String NETWORK_CIDR = "cidr";
    private static final String NETWORK_CLOUD_NAME = "cloudName";
    private static final String NETWORK_CLOUD_STATE = "cloudState";
    private static final String NETWORK_GATEWAY = "gateway";
    private static final String NETWORK_ID = "id";
    private static final String NETWORK_INTERFACE = "networkInterface";
    private static final String NETWORK_INTERFACE_STATE = "interfaceState";
    private static final String NETWORK_MAC_INTERFACE = "macinterface";
    private static final String NETWORK_NAME = "name";
    private static final String NETWORK_PROVIDER = "provider";
    private static final String NETWORK_VLAN = "vLAN";

    public EmulatedCloudNetworkPlugin(String confFilePath) {
        this.properties = PropertiesUtil.readProperties(confFilePath);
    }

    @Override
    public String requestInstance(NetworkOrder networkOrder, CloudUser cloudUser) throws FogbowException {
        HashMap<String, String> network = createNetwork(networkOrder);

        String networkId = network.get(NETWORK_ID);
        String networkPath = EmulatedCloudUtils.getResourcePath(this.properties, networkId);

        try {
            EmulatedCloudUtils.saveHashMapAsJson(networkPath, network);
        } catch (IOException e) {
            throw new FogbowException((e.getMessage()));
        }

        return networkId;
    }

    @Override
    public NetworkInstance getInstance(NetworkOrder networkOrder, CloudUser cloudUser) throws FogbowException {

        String networkId = networkOrder.getInstanceId();
        HashMap<String, String> network;

        try {
            String networkPath = EmulatedCloudUtils.getResourcePath(this.properties, networkId);
            network = EmulatedCloudUtils.readJsonAsHashMap(networkPath);
        } catch (IOException e) {

            LOGGER.error(Messages.Exception.INSTANCE_NOT_FOUND);
            throw new InstanceNotFoundException(e.getMessage());
        }

        String cloudState = network.get(NETWORK_CLOUD_STATE);
        String name = network.get(NETWORK_NAME);
        String cidr = network.get(NETWORK_CIDR);
        String gateway = network.get(NETWORK_GATEWAY);
        String vLAN = network.get(NETWORK_VLAN);
        String networkAllocationModeStr = network.get(NETWORK_ALLOCATION_MODE);
        String networkInterface = network.get(NETWORK_INTERFACE);
        String MACInterface = network.get(NETWORK_MAC_INTERFACE);
        String interfaceState = network.get(NETWORK_INTERFACE_STATE);


        NetworkAllocationMode networkAllocationMode = getAllocationMode(networkAllocationModeStr);

        return new NetworkInstance(networkId, cloudState, name, cidr, gateway,
                vLAN, networkAllocationMode, networkInterface,
                MACInterface, interfaceState);
    }

    private NetworkAllocationMode getAllocationMode(String networkAllocationModeStr) {
        switch(networkAllocationModeStr){
            case "dynamic":
                return NetworkAllocationMode.DYNAMIC;
            default:
                return NetworkAllocationMode.STATIC;
        }
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
        String networkId = networkOrder.getId();
        String networkPath = EmulatedCloudUtils.getResourcePath(this.properties, networkId);

        EmulatedCloudUtils.deleteFile(networkPath);
    }

    private HashMap<String, String> createNetwork(NetworkOrder networkOrder) {

        // Derived from order
        String networkName = EmulatedCloudUtils.getName(networkOrder.getName());
        String cidr = networkOrder.getCidr();
        String cloudName = networkOrder.getCloudName();
        String gateway = networkOrder.getGateway();
        String provider = networkOrder.getProvider();
        String allocationMode = networkOrder.getAllocationMode().getValue();

        // Created by the cloud
        String networkId = EmulatedCloudUtils.getRandomUUID();
        String macInterface = generateMac();
        String cloudState = "READY";

        HashMap<String, String> network = new HashMap<String, String>();

        network.put(NETWORK_ALLOCATION_MODE, allocationMode);
        network.put(NETWORK_NAME, networkName);
        network.put(NETWORK_CIDR, cidr);
        network.put(NETWORK_CLOUD_NAME, cloudName);
        network.put(NETWORK_GATEWAY, gateway);
        network.put(NETWORK_PROVIDER, provider);

        network.put(NETWORK_ID, networkId);
        network.put(NETWORK_MAC_INTERFACE, macInterface);
        network.put(NETWORK_CLOUD_STATE, cloudState);

        return network;
    }


    protected static String generateMac(){
        char[] hexas = "0123456789abcdef".toCharArray();
        String newMac = "";
        Random random = new Random();
        for (int i = 0; i < 12; i++){
            if (i > 0 && (i & 1) == 0) {
                newMac += ':';
            }

            int index = random.nextInt(16);

            newMac += hexas[index];

        }
        return newMac;
    }
}

