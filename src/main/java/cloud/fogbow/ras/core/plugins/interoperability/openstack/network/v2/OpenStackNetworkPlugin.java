package cloud.fogbow.ras.core.plugins.interoperability.openstack.network.v2;

import cloud.fogbow.common.constants.OpenStackConstants;
import cloud.fogbow.common.exceptions.FatalErrorException;
import cloud.fogbow.common.exceptions.FogbowException;
import cloud.fogbow.common.exceptions.InstanceNotFoundException;
import cloud.fogbow.common.exceptions.InternalServerErrorException;
import cloud.fogbow.common.models.OpenStackV3User;
import cloud.fogbow.common.util.PropertiesUtil;
import cloud.fogbow.common.util.connectivity.cloud.openstack.OpenStackHttpClient;
import cloud.fogbow.ras.api.http.response.InstanceState;
import cloud.fogbow.ras.api.http.response.NetworkInstance;
import cloud.fogbow.ras.constants.Messages;
import cloud.fogbow.ras.core.models.NetworkAllocationMode;
import cloud.fogbow.ras.core.models.ResourceType;
import cloud.fogbow.ras.core.models.orders.NetworkOrder;
import cloud.fogbow.ras.core.plugins.interoperability.NetworkPlugin;
import cloud.fogbow.ras.core.plugins.interoperability.openstack.util.OpenStackPluginUtils;
import cloud.fogbow.ras.core.plugins.interoperability.openstack.util.OpenStackStateMapper;
import cloud.fogbow.ras.core.plugins.interoperability.openstack.util.v2.OpenStackCloudUtils;
import cloud.fogbow.ras.core.plugins.interoperability.openstack.util.v2.serializables.requests.CreateNetworkRequest;
import cloud.fogbow.ras.core.plugins.interoperability.openstack.util.v2.serializables.requests.CreateSecurityGroupRequest;
import cloud.fogbow.ras.core.plugins.interoperability.openstack.util.v2.serializables.requests.CreateSecurityGroupRuleRequest;
import cloud.fogbow.ras.core.plugins.interoperability.openstack.util.v2.serializables.requests.CreateSubnetRequest;
import cloud.fogbow.ras.core.plugins.interoperability.openstack.util.v2.serializables.responses.CreateNetworkResponse;
import cloud.fogbow.ras.core.plugins.interoperability.openstack.util.v2.serializables.responses.CreateSecurityGroupResponse;
import cloud.fogbow.ras.core.plugins.interoperability.openstack.util.v2.serializables.responses.GetNetworkResponse;
import cloud.fogbow.ras.core.plugins.interoperability.openstack.util.v2.serializables.responses.GetSubnetResponse;
import com.google.gson.JsonSyntaxException;
import org.apache.log4j.Logger;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.Arrays;
import java.util.List;
import java.util.Properties;
import java.util.UUID;

public class OpenStackNetworkPlugin implements NetworkPlugin<OpenStackV3User> {
    private static final Logger LOGGER = Logger.getLogger(OpenStackNetworkPlugin.class);

    protected static final String KEY_DNS_NAMESERVERS = "dns_nameservers";
    protected static final int DEFAULT_IP_VERSION = 4;
    protected static final String[] DEFAULT_DNS_NAME_SERVERS = new String[]{"8.8.8.8", "8.8.4.4"};
    protected static final String DEFAULT_NETWORK_CIDR = "192.168.0.1/24";

    private OpenStackHttpClient client;
    private String networkV2APIEndpoint;
    private String[] dnsList;

    public OpenStackNetworkPlugin(String confFilePath) throws FatalErrorException {
        Properties properties = PropertiesUtil.readProperties(confFilePath);
        this.networkV2APIEndpoint = properties.getProperty(OpenStackPluginUtils.NETWORK_NEUTRON_URL_KEY) +
                OpenStackConstants.NEUTRON_V2_API_ENDPOINT;
        setDNSList(properties);
        initClient();
    }

    @Override
    public boolean isReady(String cloudState) {
        return OpenStackStateMapper.map(ResourceType.NETWORK, cloudState).equals(InstanceState.READY);
    }

    @Override
    public boolean hasFailed(String cloudState) {
        return OpenStackStateMapper.map(ResourceType.NETWORK, cloudState).equals(InstanceState.FAILED);
    }

    @Override
    public String requestInstance(NetworkOrder order, OpenStackV3User cloudUser) throws FogbowException {
        LOGGER.info(Messages.Log.REQUESTING_INSTANCE_FROM_PROVIDER);
        String tenantId = OpenStackPluginUtils.getProjectIdFrom(cloudUser);

        CreateNetworkResponse createNetworkResponse = createNetwork(order.getName(), cloudUser, tenantId);
        String createdNetworkId = createNetworkResponse.getId();
        createSubNet(cloudUser, order, createdNetworkId, tenantId);
        String securityGroupName = OpenStackPluginUtils.getNetworkSecurityGroupName(createdNetworkId);
        CreateSecurityGroupResponse securityGroupResponse = createSecurityGroup(cloudUser, securityGroupName,
                tenantId, createdNetworkId);
        createSecurityGroupRules(order, cloudUser, createdNetworkId, securityGroupResponse.getId());
        return createdNetworkId;
    }

    @Override
    public NetworkInstance getInstance(NetworkOrder order, OpenStackV3User cloudUser) throws FogbowException {
        String instanceId = order.getInstanceId();
        LOGGER.info(String.format(Messages.Log.GETTING_INSTANCE_S, instanceId));
        String endpoint = this.networkV2APIEndpoint + OpenStackConstants.NETWORK_ENDPOINT + "/" + order.getInstanceId();
        String responseStr = doGetInstance(endpoint, cloudUser);
        return buildInstance(responseStr, cloudUser);
    }

    @Override
    public void deleteInstance(NetworkOrder order, OpenStackV3User cloudUser) throws FogbowException {
        String instanceId = order.getInstanceId();
        LOGGER.info(String.format(Messages.Log.DELETING_INSTANCE_S, instanceId));
        String securityGroupName = OpenStackPluginUtils.getNetworkSecurityGroupName(instanceId);
        String securityGroupId = retrieveSecurityGroupId(securityGroupName, cloudUser);
        doDeleteInstance(instanceId, securityGroupId, cloudUser);
    }

    protected String doGetInstance(String endpoint, OpenStackV3User cloudUser) throws FogbowException {
        String responseStr = doGetRequest(cloudUser, endpoint);
        return responseStr;
    }

    protected String doGetRequest(OpenStackV3User cloudUser, String endpoint) throws FogbowException {
        String responseStr = this.client.doGetRequest(endpoint, cloudUser);
        return responseStr;
    }

    protected void doDeleteInstance(String instanceId, String securityGroupId, OpenStackV3User cloudUser) throws FogbowException{
        try {
            removeNetwork(cloudUser, instanceId);
        } catch (InstanceNotFoundException e) {
            // continue and try to delete the security group
            LOGGER.warn(String.format(Messages.Log.NETWORK_NOT_FOUND_S, instanceId), e);
        } catch (FogbowException e) {
            LOGGER.error(String.format(Messages.Log.UNABLE_TO_DELETE_NETWORK_WITH_ID_S, instanceId), e);
            throw e;
        }

        doDeleteSecurityGroup(securityGroupId, cloudUser);
    }

    protected void doDeleteSecurityGroup(String securityGroupId, OpenStackV3User cloudUser) throws FogbowException{
        try {
            removeSecurityGroup(cloudUser, securityGroupId);
        } catch (FogbowException e) {
            LOGGER.error(String.format(Messages.Log.UNABLE_TO_DELETE_SECURITY_GROUP_WITH_ID_S, securityGroupId), e);
            throw e;
        }
    }

    protected void setDNSList(Properties properties) {
        String dnsProperty = properties.getProperty(KEY_DNS_NAMESERVERS);
        if (dnsProperty != null) {
            this.dnsList = dnsProperty.split(",");
        }
    }

    protected CreateNetworkResponse createNetwork(String networkName, OpenStackV3User cloudUser, String tenantId) throws FogbowException {
        CreateNetworkResponse createNetworkResponse = null;

        CreateNetworkRequest createNetworkRequest = new CreateNetworkRequest.Builder()
                .name(networkName)
                .projectId(tenantId)
                .build();

        String endpoint = this.networkV2APIEndpoint + OpenStackConstants.NETWORK_ENDPOINT;

        try {
            String response = this.client.doPostRequest(endpoint, createNetworkRequest.toJson(), cloudUser);
            createNetworkResponse = CreateNetworkResponse.fromJson(response);
        } catch (JsonSyntaxException e) {
            LOGGER.error(Messages.Log.UNABLE_TO_GENERATE_JSON, e);
            throw new InternalServerErrorException(Messages.Exception.UNABLE_TO_GENERATE_JSON);
        }

        return createNetworkResponse;
    }

    protected void createSubNet(OpenStackV3User cloudUser, NetworkOrder order, String networkId, String tenantId)
            throws FogbowException {
        try {
            String jsonRequest = generateJsonEntityToCreateSubnet(networkId, tenantId, order);
            String endpoint = this.networkV2APIEndpoint + OpenStackConstants.SUBNET_ENDPOINT;
            this.client.doPostRequest(endpoint, jsonRequest, cloudUser);
        } catch (FogbowException e) {
            removeNetwork(cloudUser, networkId);
            throw e;
        }
    }

    protected CreateSecurityGroupResponse createSecurityGroup(OpenStackV3User cloudUser, String name,
                                                            String tenantId, String networkId) throws FogbowException {
        CreateSecurityGroupResponse creationResponse = null;
        try {
            CreateSecurityGroupRequest createSecurityGroupRequest = new CreateSecurityGroupRequest.Builder()
                    .name(name)
                    .projectId(tenantId)
                    .build();

            String endpoint = this.networkV2APIEndpoint + OpenStackConstants.SECURITY_GROUPS_ENDPOINT;
            String jsonRequest = createSecurityGroupRequest.toJson();
            String response = this.client.doPostRequest(endpoint, jsonRequest, cloudUser);
            creationResponse = CreateSecurityGroupResponse.fromJson(response);
        } catch (FogbowException e) {
            removeNetwork(cloudUser, networkId);
            throw e;
        }
        return creationResponse;
    }

    protected CreateSecurityGroupRuleRequest createIcmpRuleRequest(String remoteIpPrefix, String securityGroupId) {
        return new CreateSecurityGroupRuleRequest.Builder()
                .direction(OpenStackConstants.INGRESS_DIRECTION)
                .securityGroupId(securityGroupId)
                .remoteIpPrefix(remoteIpPrefix)
                .protocol(OpenStackConstants.ICMP_PROTOCOL)
                .build();
    }

    protected CreateSecurityGroupRuleRequest createAllTcpRuleRequest(String remoteIpPrefix, String securityGroupId,
                                                                   String protocol) {
        return new CreateSecurityGroupRuleRequest.Builder()
                .direction(OpenStackConstants.INGRESS_DIRECTION)
                .securityGroupId(securityGroupId)
                .remoteIpPrefix(remoteIpPrefix)
                .protocol(protocol)
                .build();
    }

    protected void createSecurityGroupRules(NetworkOrder order, OpenStackV3User cloudUser, String networkId, String securityGroupId)
            throws FogbowException {
        try {
            CreateSecurityGroupRuleRequest allTcp = createAllTcpRuleRequest(order.getCidr(), securityGroupId, OpenStackConstants.TCP_PROTOCOL);
            CreateSecurityGroupRuleRequest allUdp = createAllTcpRuleRequest(order.getCidr(), securityGroupId, OpenStackConstants.UDP_PROTOCOL);
            CreateSecurityGroupRuleRequest icmpRuleRequest = createIcmpRuleRequest(order.getCidr(), securityGroupId);

            String endpoint = this.networkV2APIEndpoint + OpenStackConstants.SECURITY_GROUP_RULES_ENDPOINT;
            this.client.doPostRequest(endpoint, allTcp.toJson(), cloudUser);
            this.client.doPostRequest(endpoint, allUdp.toJson(), cloudUser);
            this.client.doPostRequest(endpoint, icmpRuleRequest.toJson(), cloudUser);
        } catch (FogbowException e) {
            removeNetwork(cloudUser, networkId);
            removeSecurityGroup(cloudUser, securityGroupId);
            throw e;
        }
    }

    protected String getRandomUUID() {
        return UUID.randomUUID().toString();
    }

    protected String retrieveSecurityGroupId(String securityGroupName, OpenStackV3User cloudUser) throws FogbowException {
        String endpoint = this.networkV2APIEndpoint + OpenStackConstants.SECURITY_GROUPS_ENDPOINT +
                OpenStackConstants.QUERY_NAME + securityGroupName;
        String responseStr = doGetRequest(cloudUser, endpoint);
        return OpenStackCloudUtils.getSecurityGroupIdFromGetResponse(responseStr);
    }

    protected void removeSecurityGroup(OpenStackV3User cloudUser, String securityGroupId) throws FogbowException {
        String endpoint = this.networkV2APIEndpoint + OpenStackConstants.SECURITY_GROUPS_ENDPOINT + "/" + securityGroupId;
        this.client.doDeleteRequest(endpoint, cloudUser);
    }

    protected NetworkInstance buildInstance(String json, OpenStackV3User cloudUser) throws FogbowException {
        GetNetworkResponse getNetworkResponse = GetNetworkResponse.fromJson(json);
        String networkId = getNetworkResponse.getId();
        String name = getNetworkResponse.getName();
        String instanceState = getNetworkResponse.getStatus();
        String vlan = getNetworkResponse.getSegmentationId();

        List<String> subnets = getNetworkResponse.getSubnets();
        String subnetId = subnets == null || subnets.size() == 0 ? null : subnets.get(0);

        String cidr = null;
        String gateway = null;
        NetworkAllocationMode allocationMode = null;
        try {
            GetSubnetResponse subnetInformation = getSubnetInformation(cloudUser, subnetId);

            gateway = subnetInformation.getGatewayIp();
            cidr = subnetInformation.getSubnetCidr();
            boolean dhcpEnabled = subnetInformation.isDhcpEnabled();

            allocationMode = dhcpEnabled ? NetworkAllocationMode.DYNAMIC : NetworkAllocationMode.STATIC;
        } catch (JSONException e) {
            LOGGER.error(String.format(Messages.Log.UNABLE_TO_GET_NETWORK_S, json), e);
            throw new InternalServerErrorException(String.format(Messages.Exception.UNABLE_TO_GET_NETWORK_S, json));
        }

        NetworkInstance instance = null;
        if (networkId != null) {
            instance = new NetworkInstance(networkId, instanceState, name, cidr, gateway,
                    vlan, allocationMode, null, null, null);
        }
        return instance;
    }

    protected GetSubnetResponse getSubnetInformation(OpenStackV3User cloudUser, String subnetId) throws FogbowException {
        String endpoint = this.networkV2APIEndpoint + OpenStackConstants.SUBNET_ENDPOINT + "/" + subnetId;
        String response = this.client.doGetRequest(endpoint, cloudUser);
        GetSubnetResponse getSubnetResponse = GetSubnetResponse.fromJson(response);
        return getSubnetResponse;
    }

    protected String getNetworkIdFromJson(String json) throws InternalServerErrorException {
        String networkId = null;
        try {
            JSONObject rootServer = new JSONObject(json);
            JSONObject networkJSONObject = rootServer.optJSONObject(OpenStackConstants.Network.NETWORK_KEY_JSON);
            networkId = networkJSONObject.optString(OpenStackConstants.Network.ID_KEY_JSON);
        } catch (JSONException e) {
            LOGGER.error(String.format(Messages.Log.UNABLE_TO_RETRIEVE_NETWORK_ID_S, json), e);
            throw new InternalServerErrorException(String.format(Messages.Exception.UNABLE_TO_RETRIEVE_NETWORK_ID_S, json));
        }
        return networkId;
    }

    protected String generateJsonEntityToCreateSubnet(String networkId, String tenantId, NetworkOrder order) {
        String subnetName = order.getName() + "-subnet";
        int ipVersion = DEFAULT_IP_VERSION;

        String gateway = order.getGateway();
        gateway = gateway == null || gateway.isEmpty() ? null : gateway;

        String networkCidr = order.getCidr();
        networkCidr = networkCidr == null ? DEFAULT_NETWORK_CIDR : networkCidr;

        boolean dhcpEnabled = !NetworkAllocationMode.STATIC.equals(order.getAllocationMode());
        String[] dnsNamesServers = this.dnsList == null ? DEFAULT_DNS_NAME_SERVERS : this.dnsList;

        CreateSubnetRequest createSubnetRequest = new CreateSubnetRequest.Builder()
                .name(subnetName)
                .projectId(tenantId)
                .networkId(networkId)
                .ipVersion(ipVersion)
                .gatewayIp(gateway)
                .networkCidr(networkCidr)
                .dhcpEnabled(dhcpEnabled)
                .dnsNameServers(Arrays.asList(dnsNamesServers))
                .build();

        return createSubnetRequest.toJson();
    }

    protected void removeNetwork(OpenStackV3User cloudUser, String networkId) throws InternalServerErrorException, FogbowException {
        String endpoint = this.networkV2APIEndpoint + OpenStackConstants.NETWORK_ENDPOINT + "/" + networkId;
        this.client.doDeleteRequest(endpoint, cloudUser);
    }

    private void initClient() {
        this.client = new OpenStackHttpClient();
    }

    protected void setClient(OpenStackHttpClient client) {
        this.client = client;
    }

    protected String[] getDnsList() {
        return dnsList;
    }
}