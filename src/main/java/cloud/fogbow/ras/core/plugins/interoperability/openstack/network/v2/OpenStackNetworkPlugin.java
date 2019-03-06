package cloud.fogbow.ras.core.plugins.interoperability.openstack.network.v2;

import cloud.fogbow.common.constants.OpenStackConstants;
import cloud.fogbow.common.exceptions.*;
import cloud.fogbow.common.models.OpenStackV3User;
import cloud.fogbow.common.util.PropertiesUtil;
import cloud.fogbow.common.util.connectivity.cloud.openstack.OpenStackHttpClient;
import cloud.fogbow.ras.constants.Messages;
import cloud.fogbow.ras.core.models.NetworkAllocationMode;
import cloud.fogbow.ras.core.models.ResourceType;
import cloud.fogbow.ras.api.http.response.InstanceState;
import cloud.fogbow.ras.api.http.response.NetworkInstance;
import cloud.fogbow.ras.core.models.orders.NetworkOrder;
import cloud.fogbow.ras.core.plugins.interoperability.NetworkPlugin;
import cloud.fogbow.common.util.connectivity.cloud.openstack.OpenStackHttpToFogbowExceptionMapper;
import cloud.fogbow.ras.core.plugins.interoperability.openstack.OpenStackStateMapper;
import org.apache.http.client.HttpResponseException;
import org.apache.log4j.Logger;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.Arrays;
import java.util.List;
import java.util.Properties;
import java.util.UUID;

public class OpenStackNetworkPlugin implements NetworkPlugin<OpenStackV3User> {
    private static final Logger LOGGER = Logger.getLogger(OpenStackNetworkPlugin.class);

    public static final String NETWORK_NEUTRONV2_URL_KEY = "openstack_neutron_v2_url";
    protected static final String SUFFIX_ENDPOINT_NETWORK = "/networks";
    protected static final String SUFFIX_ENDPOINT_SUBNET = "/subnets";
    protected static final String SUFFIX_ENDPOINT_SECURITY_GROUP_RULES = "/security-group-rules";
    protected static final String SUFFIX_ENDPOINT_SECURITY_GROUP = "/security-groups";
    protected static final String V2_API_ENDPOINT = "/v2.0";
    public static final String KEY_EXTERNAL_GATEWAY_INFO = "external_gateway_info";
    protected static final String KEY_DNS_NAMESERVERS = "dns_nameservers";
    protected static final String QUERY_NAME = "name";
    protected static final int DEFAULT_IP_VERSION = 4;
    protected static final String DEFAULT_NETWORK_NAME = "ras-network";
    protected static final String DEFAULT_SUBNET_NAME = "ras-subnet";
    protected static final String[] DEFAULT_DNS_NAME_SERVERS = new String[]{"8.8.8.8", "8.8.4.4"};
    protected static final String DEFAULT_NETWORK_CIDR = "192.168.0.1/24";
    // security group properties
    protected static final String INGRESS_DIRECTION = "ingress";
    protected static final String TCP_PROTOCOL = "tcp";
    protected static final String UDP_PROTOCOL = "udp";
    protected static final String ICMP_PROTOCOL = "icmp";

    protected static final String SECURITY_GROUP_PREFIX = "ras-sg-pn-";

    private OpenStackHttpClient client;
    private String networkV2APIEndpoint;
    private String[] dnsList;

    public OpenStackNetworkPlugin(String confFilePath) throws FatalErrorException {
        Properties properties = PropertiesUtil.readProperties(confFilePath);
        this.networkV2APIEndpoint = properties.getProperty(NETWORK_NEUTRONV2_URL_KEY) + V2_API_ENDPOINT;
        setDNSList(properties);
        initClient();
    }

    @Override
    public String requestInstance(NetworkOrder order, OpenStackV3User cloudUser) throws FogbowException {
        String tenantId = cloudUser.getProjectId();

        CreateNetworkResponse createNetworkResponse = createNetwork(order.getName(), cloudUser, tenantId);
        String createdNetworkId = createNetworkResponse.getId();
        createSubNet(cloudUser, order, createdNetworkId, tenantId);
        String securityGroupName = getSGNameForPrivateNetwork(createdNetworkId);
        CreateSecurityGroupResponse securityGroupResponse = createSecurityGroup(cloudUser, securityGroupName,
                tenantId, createdNetworkId);
        createSecurityGroupRules(order, cloudUser, createdNetworkId, securityGroupResponse.getId());
        return createdNetworkId;
    }

    @Override
    public NetworkInstance getInstance(String instanceId, OpenStackV3User cloudUser) throws FogbowException {
        String endpoint = this.networkV2APIEndpoint + SUFFIX_ENDPOINT_NETWORK + "/" + instanceId;
        String responseStr = null;
        try {
            responseStr = this.client.doGetRequest(endpoint, cloudUser);
        } catch (HttpResponseException e) {
            OpenStackHttpToFogbowExceptionMapper.map(e);
        }
        return getInstanceFromJson(responseStr, cloudUser);
    }

    @Override
    public void deleteInstance(String networkId, OpenStackV3User cloudUser) throws FogbowException {
        try {
            removeNetwork(cloudUser, networkId);
        } catch (InstanceNotFoundException e) {
            // continue and try to delete the security group
            LOGGER.warn(String.format(Messages.Warn.NETWORK_NOT_FOUND, networkId), e);
        } catch (FogbowException e) {
            LOGGER.error(String.format(Messages.Error.UNABLE_TO_DELETE_NETWORK, networkId), e);
            throw e;
        }

        String securityGroupId = retrieveSecurityGroupId(getSGNameForPrivateNetwork(networkId), cloudUser);

        try {
            removeSecurityGroup(cloudUser, securityGroupId);
        } catch (FogbowException e) {
            LOGGER.error(String.format(Messages.Error.UNABLE_TO_DELETE_SECURITY_GROUP, securityGroupId), e);
            throw e;
        }
    }

    protected void setDNSList(Properties properties) {
        String dnsProperty = properties.getProperty(KEY_DNS_NAMESERVERS);
        if (dnsProperty != null) {
            this.dnsList = dnsProperty.split(",");
        }
    }

    private CreateNetworkResponse createNetwork(String name, OpenStackV3User cloudUser, String tenantId) throws FogbowException {
        CreateNetworkResponse createNetworkResponse = null;
        try {
            String prefixName = name == null ? getRandomUUID() : name;
            String networkName = DEFAULT_NETWORK_NAME + "-" + prefixName;

            CreateNetworkRequest createNetworkRequest = new CreateNetworkRequest.Builder()
                    .name(networkName)
                    .projectId(tenantId)
                    .build();

            String endpoint = this.networkV2APIEndpoint + SUFFIX_ENDPOINT_NETWORK;
            String response = this.client.doPostRequest(endpoint, createNetworkRequest.toJson(), cloudUser);
            createNetworkResponse = CreateNetworkResponse.fromJson(response);
        } catch (JSONException e) {
            String message = Messages.Error.UNABLE_TO_GENERATE_JSON;
            LOGGER.error(message, e);
            throw new InvalidParameterException(message, e);
        } catch (HttpResponseException e) {
            OpenStackHttpToFogbowExceptionMapper.map(e);
        }

        return createNetworkResponse;
    }

    private void createSubNet(OpenStackV3User cloudUser, NetworkOrder order, String networkId, String tenantId)
            throws FogbowException {
        try {
            String jsonRequest = generateJsonEntityToCreateSubnet(networkId, tenantId, order);
            String endpoint = this.networkV2APIEndpoint + SUFFIX_ENDPOINT_SUBNET;
            this.client.doPostRequest(endpoint, jsonRequest, cloudUser);
        } catch (HttpResponseException e) {
            removeNetwork(cloudUser, networkId);
            OpenStackHttpToFogbowExceptionMapper.map(e);
        }
    }

    private CreateSecurityGroupResponse createSecurityGroup(OpenStackV3User cloudUser, String name,
                                                            String tenantId, String networkId) throws FogbowException {
        CreateSecurityGroupResponse creationResponse = null;
        try {
            CreateSecurityGroupRequest createSecurityGroupRequest = new CreateSecurityGroupRequest.Builder()
                    .name(name)
                    .projectId(tenantId)
                    .build();

            String endpoint = this.networkV2APIEndpoint + SUFFIX_ENDPOINT_SECURITY_GROUP;
            String jsonRequest = createSecurityGroupRequest.toJson();
            String response = this.client.doPostRequest(endpoint, jsonRequest, cloudUser);
            creationResponse = CreateSecurityGroupResponse.fromJson(response);
        } catch (HttpResponseException e) {
            removeNetwork(cloudUser, networkId);
            OpenStackHttpToFogbowExceptionMapper.map(e);
        }
        return creationResponse;
    }

    private CreateSecurityGroupRuleRequest createIcmpRuleRequest(String remoteIpPrefix, String securityGroupId) {
        return new CreateSecurityGroupRuleRequest.Builder()
                .direction(INGRESS_DIRECTION)
                .securityGroupId(securityGroupId)
                .remoteIpPrefix(remoteIpPrefix)
                .protocol(ICMP_PROTOCOL)
                .build();
    }

    private CreateSecurityGroupRuleRequest createAllTcpRuleRequest(String remoteIpPrefix, String securityGroupId,
                                                                   String protocol) {
        return new CreateSecurityGroupRuleRequest.Builder()
                .direction(INGRESS_DIRECTION)
                .securityGroupId(securityGroupId)
                .remoteIpPrefix(remoteIpPrefix)
                .protocol(protocol)
                .build();
    }

    private void createSecurityGroupRules(NetworkOrder order, OpenStackV3User cloudUser, String networkId, String securityGroupId)
            throws FogbowException {
        try {
            CreateSecurityGroupRuleRequest allTcp = createAllTcpRuleRequest(order.getCidr(), securityGroupId, TCP_PROTOCOL);
            CreateSecurityGroupRuleRequest allUdp = createAllTcpRuleRequest(order.getCidr(), securityGroupId, UDP_PROTOCOL);
            CreateSecurityGroupRuleRequest icmpRuleRequest = createIcmpRuleRequest(order.getCidr(), securityGroupId);

            String endpoint = this.networkV2APIEndpoint + SUFFIX_ENDPOINT_SECURITY_GROUP_RULES;
            this.client.doPostRequest(endpoint, allTcp.toJson(), cloudUser);
            this.client.doPostRequest(endpoint, allUdp.toJson(), cloudUser);
            this.client.doPostRequest(endpoint, icmpRuleRequest.toJson(), cloudUser);
        } catch (HttpResponseException e) {
            removeNetwork(cloudUser, networkId);
            removeSecurityGroup(cloudUser, securityGroupId);
            OpenStackHttpToFogbowExceptionMapper.map(e);
        }
    }

    protected String getRandomUUID() {
        return UUID.randomUUID().toString();
    }

    protected String getSecurityGroupIdFromGetResponse(String json) throws UnexpectedException {
        String securityGroupId = null;
        try {
            JSONObject response = new JSONObject(json);
            JSONArray securityGroupJSONArray = response.getJSONArray(OpenStackConstants.Network.SECURITY_GROUPS_KEY_JSON);
            JSONObject securityGroup = securityGroupJSONArray.optJSONObject(0);
            securityGroupId = securityGroup.getString(OpenStackConstants.Network.ID_KEY_JSON);
        } catch (JSONException e) {
            String message = String.format(Messages.Error.UNABLE_TO_RETRIEVE_NETWORK_ID, json);
            LOGGER.error(message, e);
            throw new UnexpectedException(message, e);
        }
        return securityGroupId;
    }

    protected String retrieveSecurityGroupId(String securityGroupName, OpenStackV3User cloudUser) throws FogbowException {
        String endpoint = this.networkV2APIEndpoint + SUFFIX_ENDPOINT_SECURITY_GROUP + "?" + QUERY_NAME + "=" + securityGroupName;
        String responseStr = null;
        try {
            responseStr = this.client.doGetRequest(endpoint, cloudUser);
        } catch (HttpResponseException e) {
            OpenStackHttpToFogbowExceptionMapper.map(e);
        }
        return getSecurityGroupIdFromGetResponse(responseStr);
    }

    protected boolean removeSecurityGroup(OpenStackV3User cloudUser, String securityGroupId) throws FogbowException {
        try {
            String endpoint = this.networkV2APIEndpoint + SUFFIX_ENDPOINT_SECURITY_GROUP + "/" + securityGroupId;
            this.client.doDeleteRequest(endpoint, cloudUser);
            return true;
        } catch (HttpResponseException e) {
            OpenStackHttpToFogbowExceptionMapper.map(e);
            return false;
        }
    }

    protected NetworkInstance getInstanceFromJson(String json, OpenStackV3User cloudUser) throws FogbowException {
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
            String message = String.format(Messages.Error.UNABLE_TO_GET_NETWORK, json);
            LOGGER.error(message, e);
            throw new InvalidParameterException(message, e);
        }

        NetworkInstance instance = null;
        if (networkId != null) {
            InstanceState fogbowState = OpenStackStateMapper.map(ResourceType.NETWORK, instanceState);
            instance = new NetworkInstance(networkId, fogbowState, name, cidr, gateway,
                    vlan, allocationMode, null, null, null);
        }
        return instance;
    }

    private GetSubnetResponse getSubnetInformation(OpenStackV3User cloudUser, String subnetId) throws FogbowException {
        String endpoint = this.networkV2APIEndpoint + SUFFIX_ENDPOINT_SUBNET + "/" + subnetId;
        GetSubnetResponse getSubnetResponse = null;
        try {
            String response = this.client.doGetRequest(endpoint, cloudUser);
            getSubnetResponse = GetSubnetResponse.fromJson(response);
        } catch (HttpResponseException e) {
            OpenStackHttpToFogbowExceptionMapper.map(e);
        }
        return getSubnetResponse;
    }

    protected String getNetworkIdFromJson(String json) throws UnexpectedException {
        String networkId = null;
        try {
            JSONObject rootServer = new JSONObject(json);
            JSONObject networkJSONObject = rootServer.optJSONObject(OpenStackConstants.Network.NETWORK_KEY_JSON);
            networkId = networkJSONObject.optString(OpenStackConstants.Network.ID_KEY_JSON);
        } catch (JSONException e) {
            String message = String.format(Messages.Error.UNABLE_TO_RETRIEVE_NETWORK_ID, json);
            LOGGER.error(message, e);
            throw new UnexpectedException(message, e);
        }
        return networkId;
    }

    protected String generateJsonEntityToCreateSubnet(String networkId, String tenantId,
                                                      NetworkOrder order) {
        String subnetName = DEFAULT_SUBNET_NAME + "-" + order.getName();
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

    protected boolean removeNetwork(OpenStackV3User cloudUser, String networkId) throws UnexpectedException, FogbowException {
        String endpoint = this.networkV2APIEndpoint + SUFFIX_ENDPOINT_NETWORK + "/" + networkId;
        try {
            this.client.doDeleteRequest(endpoint, cloudUser);
            return true;
        } catch (HttpResponseException e) {
            OpenStackHttpToFogbowExceptionMapper.map(e);
            return false;
        }
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

    public static String getSGNameForPrivateNetwork(String networkId) {
        return SECURITY_GROUP_PREFIX + networkId;
    }
}