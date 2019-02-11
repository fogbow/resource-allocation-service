package cloud.fogbow.ras.core.plugins.interoperability.openstack.network.v2;

import cloud.fogbow.common.exceptions.*;
import cloud.fogbow.common.models.CloudToken;
import cloud.fogbow.common.util.PropertiesUtil;
import cloud.fogbow.ras.constants.ConfigurationPropertyDefaults;
import cloud.fogbow.ras.constants.ConfigurationPropertyKeys;
import cloud.fogbow.ras.constants.Messages;
import cloud.fogbow.ras.core.PropertiesHolder;
import cloud.fogbow.ras.core.models.NetworkAllocationMode;
import cloud.fogbow.ras.core.models.ResourceType;
import cloud.fogbow.ras.core.models.instances.InstanceState;
import cloud.fogbow.ras.core.models.instances.NetworkInstance;
import cloud.fogbow.ras.core.models.orders.NetworkOrder;
import cloud.fogbow.ras.core.plugins.interoperability.NetworkPlugin;
import cloud.fogbow.ras.core.plugins.interoperability.openstack.OpenStackHttpClient;
import cloud.fogbow.ras.core.plugins.interoperability.openstack.OpenStackHttpToFogbowExceptionMapper;
import cloud.fogbow.ras.core.plugins.interoperability.openstack.OpenStackStateMapper;
import cloud.fogbow.ras.core.plugins.interoperability.openstack.OpenStackV3Token;
import org.apache.http.client.HttpResponseException;
import org.apache.log4j.Logger;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.Arrays;
import java.util.List;
import java.util.Properties;
import java.util.UUID;

public class OpenStackNetworkPlugin implements NetworkPlugin {
    private static final Logger LOGGER = Logger.getLogger(OpenStackNetworkPlugin.class);

    public static final String NETWORK_NEUTRONV2_URL_KEY = "openstack_neutron_v2_url";
    protected static final String SUFFIX_ENDPOINT_NETWORK = "/networks";
    protected static final String SUFFIX_ENDPOINT_SUBNET = "/subnets";
    protected static final String SUFFIX_ENDPOINT_SECURITY_GROUP_RULES = "/security-group-rules";
    protected static final String SUFFIX_ENDPOINT_SECURITY_GROUP = "/security-groups";
    protected static final String V2_API_ENDPOINT = "/v2.0";
    protected static final String KEY_PROVIDER_SEGMENTATION_ID = "provider:segmentation_id";
    public static final String KEY_EXTERNAL_GATEWAY_INFO = "external_gateway_info";
    protected static final String QUERY_NAME = "name";
    protected static final String KEY_DNS_NAMESERVERS = "dns_nameservers";
    protected static final String KEY_ENABLE_DHCP = "enable_dhcp";
    protected static final String KEY_IP_VERSION = "ip_version";
    protected static final String KEY_GATEWAY_IP = "gateway_ip";
    protected static final String KEY_PROJECT_ID = "project_id";
    protected static final String KEY_JSON_NETWORK = "network";
    protected static final String KEY_NETWORK_ID = "network_id";
    protected static final String KEY_JSON_SUBNET = "subnet";
    protected static final String KEY_SUBNETS = "subnets";
    protected static final String KEY_SECURITY_GROUPS = "security_groups";
    protected static final String KEY_STATUS = "status";
    protected static final String KEY_NAME = "name";
    protected static final String KEY_CIDR = "cidr";
    protected static final String KEY_ID = "id";
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
    public String requestInstance(NetworkOrder order, CloudToken token) throws FogbowException {
        OpenStackV3Token openStackV3Token = (OpenStackV3Token) token;
        String tenantId = openStackV3Token.getProjectId();

        CreateNetworkResponse createNetworkResponse = createNetwork(order.getName(), openStackV3Token, tenantId);
        String createdNetworkId = createNetworkResponse.getId();
        createSubNet(openStackV3Token, order, createdNetworkId, tenantId);
        String securityGroupName = getSGNameForPrivateNetwork(createdNetworkId);
        CreateSecurityGroupResponse securityGroupResponse = createSecurityGroup(openStackV3Token, securityGroupName,
                tenantId, createdNetworkId);
        createSecurityGroupRules(order, openStackV3Token, createdNetworkId, securityGroupResponse.getId());
        return createdNetworkId;
    }

    @Override
    public NetworkInstance getInstance(String instanceId, CloudToken openStackV3Token) throws FogbowException {
        String endpoint = this.networkV2APIEndpoint + SUFFIX_ENDPOINT_NETWORK + "/" + instanceId;
        String responseStr = null;
        try {
            responseStr = this.client.doGetRequest(endpoint, openStackV3Token);
        } catch (HttpResponseException e) {
            OpenStackHttpToFogbowExceptionMapper.map(e);
        }
        return getInstanceFromJson(responseStr, openStackV3Token);
    }

    @Override
    public void deleteInstance(String networkId, CloudToken openStackV3Token) throws FogbowException {
        try {
            removeNetwork(openStackV3Token, networkId);
        } catch (InstanceNotFoundException e) {
            // continue and try to delete the security group
            LOGGER.warn(String.format(Messages.Warn.NETWORK_NOT_FOUND, networkId), e);
        } catch (FogbowException e) {
            LOGGER.error(String.format(Messages.Error.UNABLE_TO_DELETE_NETWORK, networkId), e);
            throw e;
        }

        String securityGroupId = retrieveSecurityGroupId(getSGNameForPrivateNetwork(networkId), openStackV3Token);

        try {
            removeSecurityGroup(openStackV3Token, securityGroupId);
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

    private CreateNetworkResponse createNetwork(String name, CloudToken openStackV3Token, String tenantId)
            throws FogbowException, UnexpectedException {
        CreateNetworkResponse createNetworkResponse = null;
        try {
            String prefixName = name == null ? getRandomUUID() : name;
            String networkName = DEFAULT_NETWORK_NAME + "-" + prefixName;

            CreateNetworkRequest createNetworkRequest = new CreateNetworkRequest.Builder()
                    .name(networkName)
                    .projectId(tenantId)
                    .build();

            String endpoint = this.networkV2APIEndpoint + SUFFIX_ENDPOINT_NETWORK;
            String response = this.client.doPostRequest(endpoint, openStackV3Token, createNetworkRequest.toJson());
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

    private void createSubNet(CloudToken openStackV3Token, NetworkOrder order, String networkId, String tenantId)
            throws UnexpectedException, FogbowException {
        try {
            String jsonRequest = generateJsonEntityToCreateSubnet(networkId, tenantId, order);
            String endpoint = this.networkV2APIEndpoint + SUFFIX_ENDPOINT_SUBNET;
            this.client.doPostRequest(endpoint, openStackV3Token, jsonRequest);
        } catch (HttpResponseException e) {
            removeNetwork(openStackV3Token, networkId);
            OpenStackHttpToFogbowExceptionMapper.map(e);
        }
    }

    private CreateSecurityGroupResponse createSecurityGroup(CloudToken openStackV3Token, String name,
                                                            String tenantId, String networkId)
            throws UnexpectedException, FogbowException {
        CreateSecurityGroupResponse creationResponse = null;
        try {
            CreateSecurityGroupRequest createSecurityGroupRequest = new CreateSecurityGroupRequest.Builder()
                    .name(name)
                    .projectId(tenantId)
                    .build();

            String endpoint = this.networkV2APIEndpoint + SUFFIX_ENDPOINT_SECURITY_GROUP;
            String jsonRequest = createSecurityGroupRequest.toJson();
            String response = this.client.doPostRequest(endpoint, openStackV3Token, jsonRequest);
            creationResponse = CreateSecurityGroupResponse.fromJson(response);
        } catch (HttpResponseException e) {
            removeNetwork(openStackV3Token, networkId);
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

    private void createSecurityGroupRules(NetworkOrder order, CloudToken openStackV3Token, String networkId, String securityGroupId)
            throws UnexpectedException, FogbowException {
        try {
            CreateSecurityGroupRuleRequest allTcp = createAllTcpRuleRequest(order.getCidr(), securityGroupId, TCP_PROTOCOL);
            CreateSecurityGroupRuleRequest allUdp = createAllTcpRuleRequest(order.getCidr(), securityGroupId, UDP_PROTOCOL);
            CreateSecurityGroupRuleRequest icmpRuleRequest = createIcmpRuleRequest(order.getCidr(), securityGroupId);

            String endpoint = this.networkV2APIEndpoint + SUFFIX_ENDPOINT_SECURITY_GROUP_RULES;
            this.client.doPostRequest(endpoint, openStackV3Token, allTcp.toJson());
            this.client.doPostRequest(endpoint, openStackV3Token, allUdp.toJson());
            this.client.doPostRequest(endpoint, openStackV3Token, icmpRuleRequest.toJson());
        } catch (HttpResponseException e) {
            removeNetwork(openStackV3Token, networkId);
            removeSecurityGroup(openStackV3Token, securityGroupId);
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
            JSONArray securityGroupJSONArray = response.getJSONArray(KEY_SECURITY_GROUPS);
            JSONObject securityGroup = securityGroupJSONArray.optJSONObject(0);
            securityGroupId = securityGroup.getString(KEY_ID);
        } catch (JSONException e) {
            String message = String.format(Messages.Error.UNABLE_TO_RETRIEVE_NETWORK_ID, json);
            LOGGER.error(message, e);
            throw new UnexpectedException(message, e);
        }
        return securityGroupId;
    }

    protected String retrieveSecurityGroupId(String securityGroupName, CloudToken openStackV3Token) throws FogbowException, UnexpectedException {
        String endpoint = this.networkV2APIEndpoint + SUFFIX_ENDPOINT_SECURITY_GROUP + "?" + QUERY_NAME + "=" + securityGroupName;
        String responseStr = null;
        try {
            responseStr = this.client.doGetRequest(endpoint, openStackV3Token);
        } catch (HttpResponseException e) {
            OpenStackHttpToFogbowExceptionMapper.map(e);
        }
        return getSecurityGroupIdFromGetResponse(responseStr);
    }

    protected boolean removeSecurityGroup(CloudToken openStackV3Token, String securityGroupId)
            throws FogbowException, UnexpectedException {
        try {
            String endpoint = this.networkV2APIEndpoint + SUFFIX_ENDPOINT_SECURITY_GROUP + "/" + securityGroupId;
            this.client.doDeleteRequest(endpoint, openStackV3Token);
            return true;
        } catch (HttpResponseException e) {
            OpenStackHttpToFogbowExceptionMapper.map(e);
            return false;
        }
    }

    protected NetworkInstance getInstanceFromJson(String json, CloudToken openStackV3Token)
            throws FogbowException, UnexpectedException {
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
            GetSubnetResponse subnetInformation = getSubnetInformation(openStackV3Token, subnetId);

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

    private GetSubnetResponse getSubnetInformation(CloudToken openStackV3Token, String subnetId)
            throws FogbowException, UnexpectedException {
        String endpoint = this.networkV2APIEndpoint + SUFFIX_ENDPOINT_SUBNET + "/" + subnetId;
        GetSubnetResponse getSubnetResponse = null;
        try {
            String response = this.client.doGetRequest(endpoint, openStackV3Token);
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
            JSONObject networkJSONObject = rootServer.optJSONObject(KEY_JSON_NETWORK);
            networkId = networkJSONObject.optString(KEY_ID);
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

    protected boolean removeNetwork(CloudToken openStackV3Token, String networkId) throws UnexpectedException, FogbowException {
        String endpoint = this.networkV2APIEndpoint + SUFFIX_ENDPOINT_NETWORK + "/" + networkId;
        try {
            this.client.doDeleteRequest(endpoint, openStackV3Token);
            return true;
        } catch (HttpResponseException e) {
            OpenStackHttpToFogbowExceptionMapper.map(e);
            return false;
        }
    }

    private void initClient() {
        this.client = new OpenStackHttpClient(
                new Integer(PropertiesHolder.getInstance().getProperty(ConfigurationPropertyKeys.HTTP_REQUEST_TIMEOUT_KEY,
                        ConfigurationPropertyDefaults.XMPP_TIMEOUT)));
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