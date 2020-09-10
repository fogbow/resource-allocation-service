package cloud.fogbow.ras.core.plugins.interoperability.emulatedcloud.network;

import cloud.fogbow.common.exceptions.FogbowException;
import cloud.fogbow.common.exceptions.InstanceNotFoundException;
import cloud.fogbow.common.models.CloudUser;
import cloud.fogbow.common.util.PropertiesUtil;
import cloud.fogbow.ras.api.http.response.InstanceState;
import cloud.fogbow.ras.api.http.response.NetworkInstance;
import cloud.fogbow.ras.constants.Messages;
import cloud.fogbow.ras.core.models.NetworkAllocationMode;
import cloud.fogbow.ras.core.models.ResourceType;
import cloud.fogbow.ras.core.models.orders.NetworkOrder;
import cloud.fogbow.ras.core.plugins.interoperability.NetworkPlugin;
import cloud.fogbow.ras.core.plugins.interoperability.emulatedcloud.EmulatedCloudConstants;
import cloud.fogbow.ras.core.plugins.interoperability.emulatedcloud.EmulatedCloudStateMapper;
import cloud.fogbow.ras.core.plugins.interoperability.emulatedcloud.EmulatedCloudUtils;
import cloud.fogbow.ras.core.plugins.interoperability.emulatedcloud.sdk.network.models.EmulatedNetwork;
import cloud.fogbow.ras.core.plugins.interoperability.emulatedcloud.sdk.network.EmulatedCloudNetworkManager;
import org.apache.log4j.Logger;

import java.util.*;

public class EmulatedCloudNetworkPlugin implements NetworkPlugin<CloudUser> {

    private static final Logger LOGGER = Logger.getLogger(EmulatedCloudNetworkPlugin.class);

    private Properties properties;

    public EmulatedCloudNetworkPlugin(String confFilePath) {
        this.properties = PropertiesUtil.readProperties(confFilePath);
    }

    @Override
    public String requestInstance(NetworkOrder networkOrder, CloudUser cloudUser) throws FogbowException {
        LOGGER.info(Messages.Log.REQUESTING_INSTANCE_FROM_PROVIDER);
        EmulatedCloudNetworkManager networkManager = EmulatedCloudNetworkManager.getInstance();
        EmulatedNetwork network = createNetwork(networkOrder);
        String instanceId = networkManager.create(network);
        return instanceId;
    }

    @Override
    public NetworkInstance getInstance(NetworkOrder networkOrder, CloudUser cloudUser) throws FogbowException {
        String instanceId = networkOrder.getInstanceId();
        LOGGER.info(String.format(Messages.Log.GETTING_INSTANCE_S, instanceId));

        EmulatedCloudNetworkManager networkManager = EmulatedCloudNetworkManager.getInstance();
        Optional<EmulatedNetwork> emulatedNetwork = networkManager.find(instanceId);

        if (emulatedNetwork.isPresent()) {
            return buildNetworkInstance(emulatedNetwork.get());
        } else {
            throw new InstanceNotFoundException();
        }
    }

    private NetworkInstance buildNetworkInstance(EmulatedNetwork network) {
        String instanceId = network.getInstanceId();
        String cidr = network.getCidr();
        String cloudState = network.getCloudState();
        String gateway = network.getGateway();
        String interfaceState = network.getInterfaceState();
        String macInterface = network.getMacInterface();
        String name = network.getName();
        String networkInterface = network.getNetworkInterface();
        String vLAN = network.getvLAN();

        NetworkAllocationMode allocationMode = getAllocationMode(network.getAllocationMode());

        return new NetworkInstance(instanceId, cloudState, name, cidr, gateway, vLAN, allocationMode,
                networkInterface, macInterface, interfaceState);
    }

    private NetworkAllocationMode getAllocationMode(String networkAllocationModeStr) {
        switch (networkAllocationModeStr) {
            case EmulatedCloudConstants.NETWORK_ALLOCATION_MODE_DYNAMIC:
                return NetworkAllocationMode.DYNAMIC;
            default:
                return NetworkAllocationMode.STATIC;
        }
    }

    @Override
    public boolean isReady(String instanceState) {
        return EmulatedCloudStateMapper.map(ResourceType.NETWORK, instanceState).equals(InstanceState.READY);
    }

    @Override
    public boolean hasFailed(String instanceState) {
        return EmulatedCloudStateMapper.map(ResourceType.NETWORK, instanceState).equals(InstanceState.FAILED);
    }

    @Override
    public void deleteInstance(NetworkOrder networkOrder, CloudUser cloudUser) throws FogbowException {
        String instanceId = networkOrder.getInstanceId();
        LOGGER.info(String.format(Messages.Log.DELETING_INSTANCE_S, instanceId));
        EmulatedCloudNetworkManager networkManager = EmulatedCloudNetworkManager.getInstance();
        networkManager.delete(instanceId);
    }

    private EmulatedNetwork createNetwork(NetworkOrder networkOrder) {

        // Derived from order
        String networkName = EmulatedCloudUtils.getName(networkOrder.getName());
        String cidr = networkOrder.getCidr();
        String cloudName = networkOrder.getCloudName();
        String gateway = networkOrder.getGateway();
        String provider = networkOrder.getProvider();
        String allocationMode = networkOrder.getAllocationMode().getValue();

        // Created by the cloud
        String networkId = EmulatedCloudUtils.getRandomUUID();
        String macInterface = EmulatedCloudUtils.generateMac();
        String cloudState = EmulatedCloudStateMapper.ACTIVE_STATUS;

        EmulatedNetwork network = new EmulatedNetwork.Builder()
                .instanceId(networkId)
                .cloudName(cloudName)
                .provider(provider)
                .name(networkName)
                .cidr(cidr)
                .gateway(gateway)
                .macInterface(macInterface)
                .allocationMode(allocationMode)
                .cloudState(cloudState)
                .vLAN(EmulatedCloudConstants.NO_VALUE_STRING)
                .networkInterface(EmulatedCloudConstants.NO_VALUE_STRING)
                .interfaceState(EmulatedCloudConstants.NO_VALUE_STRING)
                .build();

        return network;
    }
}

