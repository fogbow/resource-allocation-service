package cloud.fogbow.ras.core.plugins.interoperability.emulatedcloud.network;

import cloud.fogbow.common.exceptions.FogbowException;
import cloud.fogbow.common.exceptions.InstanceNotFoundException;
import cloud.fogbow.common.models.CloudUser;
import cloud.fogbow.common.util.PropertiesUtil;
import cloud.fogbow.ras.api.http.response.InstanceState;
import cloud.fogbow.ras.api.http.response.NetworkInstance;
import cloud.fogbow.ras.constants.Messages;
import cloud.fogbow.ras.constants.SystemConstants;
import cloud.fogbow.ras.core.models.NetworkAllocationMode;
import cloud.fogbow.ras.core.models.ResourceType;
import cloud.fogbow.ras.core.models.orders.NetworkOrder;
import cloud.fogbow.ras.core.plugins.interoperability.NetworkPlugin;
import cloud.fogbow.ras.core.plugins.interoperability.emulatedcloud.EmulatedCloudStateMapper;
import cloud.fogbow.ras.core.plugins.interoperability.emulatedcloud.EmulatedCloudUtils;
import cloud.fogbow.ras.core.plugins.interoperability.emulatedcloud.emulatedmodels.EmulatedNetwork;
import cloud.fogbow.ras.core.plugins.interoperability.emulatedcloud.emulatedmodels.EmulatedSecurityGroup;
import cloud.fogbow.ras.core.plugins.interoperability.emulatedcloud.emulatedmodels.EmulatedSecurityRule;
import org.apache.log4j.Logger;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;
import java.util.Random;

public class EmulatedCloudNetworkPlugin implements NetworkPlugin<CloudUser> {

    private static final Logger LOGGER = Logger.getLogger(EmulatedCloudNetworkPlugin.class);

    private Properties properties;

    public EmulatedCloudNetworkPlugin(String confFilePath) {
        this.properties = PropertiesUtil.readProperties(confFilePath);
    }

    @Override
    public String requestInstance(NetworkOrder networkOrder, CloudUser cloudUser) throws FogbowException {
        EmulatedNetwork network = createNetwork(networkOrder);

        String networkId = network.getInstanceId();
        String networkPath = EmulatedCloudUtils.getResourcePath(this.properties, networkId);

        try {
            this.createSecurityGroup(networkId);
            EmulatedCloudUtils.saveFileContent(networkPath, network.toJson());
        } catch (IOException e) {
            throw new FogbowException((e.getMessage()));
        }

        return networkId;
    }

    private void createSecurityGroup(String instanceId) throws IOException {
        String securityGroupId = EmulatedCloudUtils.getNetworkSecurityGroupId(instanceId);
        EmulatedSecurityGroup securityGroup = this.buildSecurityGroup(securityGroupId);
        String networkPath = EmulatedCloudUtils.getResourcePath(this.properties, securityGroupId);
        EmulatedCloudUtils.saveFileContent(networkPath, securityGroup.toJson());
    }

    private EmulatedSecurityGroup buildSecurityGroup(String securityGroupId) {
        List<String> securityRuleIdList = new ArrayList<>();

        return new EmulatedSecurityGroup.Builder()
                .id(securityGroupId)
                .securityRules(securityRuleIdList)
                .build();
    }

    @Override
    public NetworkInstance getInstance(NetworkOrder networkOrder, CloudUser cloudUser) throws FogbowException {
        String networkId = networkOrder.getInstanceId();
        EmulatedNetwork network;

        try {
            String computePath = EmulatedCloudUtils.getResourcePath(this.properties, networkId);
            String jsonContent = EmulatedCloudUtils.getFileContent(computePath);
            network = EmulatedNetwork.fromJson(jsonContent);
        } catch (IOException e) {
            LOGGER.error(Messages.Exception.INSTANCE_NOT_FOUND);
            throw new InstanceNotFoundException(e.getMessage());
        }

        return buildNetworkInstance(network);
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
        switch(networkAllocationModeStr){
            case "dynamic":
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
        String networkId = networkOrder.getInstanceId();
        String networkPath = EmulatedCloudUtils.getResourcePath(this.properties, networkId);
        EmulatedCloudUtils.deleteFile(networkPath);

        String securityGroupId = EmulatedCloudUtils.getNetworkSecurityGroupId(networkId);
        deleteSecurityRules(securityGroupId);
        deleteSecurityGroup(securityGroupId);
    }

    private void deleteSecurityRules(String securityGroupId) throws FogbowException {
        EmulatedSecurityGroup securityGroup = this.getSecurityGroup(securityGroupId);

        for (String securityRuleId : securityGroup.getSecurityRules()) {
            String path = EmulatedCloudUtils.getResourcePath(properties, securityRuleId);
            EmulatedCloudUtils.deleteFile(path);
        }
    }

    private void deleteSecurityGroup(String securityGroupId) {
        String securityGroupPath = EmulatedCloudUtils.getResourcePath(this.properties, securityGroupId);
        EmulatedCloudUtils.deleteFile(securityGroupPath);
    }

    private EmulatedSecurityGroup getSecurityGroup(String securityGroupId) throws FogbowException {
        try {
            String content = EmulatedCloudUtils.getFileContentById(properties, securityGroupId);
            return EmulatedSecurityGroup.fromJson(content);
        } catch (IOException e) {
            throw new FogbowException(e.getMessage());
        }
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
        String macInterface = generateMac();
        String cloudState = "READY";

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
                .vLAN("")
                .networkInterface("")
                .interfaceState("")
                .build();

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

