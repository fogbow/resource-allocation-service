package org.fogbowcloud.ras.core.plugins.interoperability.openstack.network.v2;

import java.util.Arrays;
import java.util.List;
import java.util.Properties;

import org.apache.http.client.HttpResponseException;
import org.apache.log4j.Logger;
import org.fogbowcloud.ras.core.HomeDir;
import org.fogbowcloud.ras.core.constants.DefaultConfigurationConstants;
import org.fogbowcloud.ras.core.constants.Messages;
import org.fogbowcloud.ras.core.exceptions.FatalErrorException;
import org.fogbowcloud.ras.core.exceptions.FogbowRasException;
import org.fogbowcloud.ras.core.exceptions.InstanceNotFoundException;
import org.fogbowcloud.ras.core.exceptions.InvalidParameterException;
import org.fogbowcloud.ras.core.exceptions.UnexpectedException;
import org.fogbowcloud.ras.core.models.ResourceType;
import org.fogbowcloud.ras.core.models.instances.InstanceState;
import org.fogbowcloud.ras.core.models.instances.NetworkInstance;
import org.fogbowcloud.ras.core.models.orders.NetworkAllocationMode;
import org.fogbowcloud.ras.core.models.orders.NetworkOrder;
import org.fogbowcloud.ras.core.models.tokens.OpenStackV3Token;
import org.fogbowcloud.ras.core.plugins.interoperability.NetworkPlugin;
import org.fogbowcloud.ras.core.plugins.interoperability.openstack.OpenStackHttpToFogbowRasExceptionMapper;
import org.fogbowcloud.ras.core.plugins.interoperability.openstack.OpenStackStateMapper;
import org.fogbowcloud.ras.util.PropertiesUtil;
import org.fogbowcloud.ras.util.connectivity.HttpRequestClientUtil;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

public class OpenStackV2NetworkPlugin implements NetworkPlugin<OpenStackV3Token> {
    private static final Logger LOGGER = Logger.getLogger(OpenStackV2NetworkPlugin.class);

    public static final String NETWORK_NEUTRONV2_URL_KEY = "openstack_neutron_v2_url";
    protected static final String SUFFIX_ENDPOINT_NETWORK = "/networks";
    protected static final String SUFFIX_ENDPOINT_SUBNET = "/subnets";
    protected static final String SUFFIX_ENDPOINT_SECURITY_GROUP_RULES = "/security-group-rules";
    protected static final String SUFFIX_ENDPOINT_SECURITY_GROUP = "/security-groups";
    protected static final String V2_API_ENDPOINT = "/v2.0";
    protected static final String KEY_PROVIDER_SEGMENTATION_ID = "provider:segmentation_id";
    protected static final String KEY_EXTERNAL_GATEWAY_INFO = "external_gateway_info";
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
    protected static final String DEFAULT_NETWORK_ADDRESS = "192.168.0.1/24";
    // security group properties
    protected static final String INGRESS_DIRECTION = "ingress";
    protected static final String TCP_PROTOCOL = "tcp";
    protected static final String ICMP_PROTOCOL = "icmp";
    protected static final int SSH_PORT = 22;
    public static final String SECURITY_GROUP_PREFIX = "ras-sg";
    private HttpRequestClientUtil client;
    private String networkV2APIEndpoint;
    private String[] dnsList;

    public OpenStackV2NetworkPlugin() throws FatalErrorException {
        Properties properties = PropertiesUtil.readProperties(HomeDir.getPath() +
                DefaultConfigurationConstants.OPENSTACK_CONF_FILE_NAME);
        this.networkV2APIEndpoint = properties.getProperty(NETWORK_NEUTRONV2_URL_KEY) + V2_API_ENDPOINT;
        setDNSList(properties);
        initClient();
    }

    protected void setDNSList(Properties properties) {
        String dnsProperty = properties.getProperty(KEY_DNS_NAMESERVERS);
        if (dnsProperty != null) {
            this.dnsList = dnsProperty.split(",");
        }
    }

    @Override
    public String requestInstance(NetworkOrder order, OpenStackV3Token openStackV3Token)
            throws FogbowRasException, UnexpectedException {
        String tenantId = openStackV3Token.getProjectId();

        CreateNetworkResponse createNetworkResponse = createNetwork(order.getName(), openStackV3Token, tenantId);
        String createdNetworkId = createNetworkResponse.getId();
        createSubNet(openStackV3Token, order, createdNetworkId, tenantId);
        String securityGroupName = SECURITY_GROUP_PREFIX + "-" + createdNetworkId;
        CreateSecurityGroupResponse securityGroupResponse = createSecurityGroup(openStackV3Token, securityGroupName,
                tenantId, createdNetworkId);
        createSecurityGroupRules(order, openStackV3Token, createdNetworkId, securityGroupResponse.getId());
        return createdNetworkId;
    }

    private CreateNetworkResponse createNetwork(String name, OpenStackV3Token openStackV3Token, String tenantId)
            throws FogbowRasException, UnexpectedException {
        CreateNetworkResponse createNetworkResponse = null;
        try {
            String networkName = DEFAULT_NETWORK_NAME + "-" + name;

            CreateNetworkRequest createNetworkRequest = new CreateNetworkRequest.Builder()
                    .name(networkName)
                    .projectId(tenantId)
                    .build();

            String endpoint = this.networkV2APIEndpoint + SUFFIX_ENDPOINT_NETWORK;
            String response = this.client.doPostRequest(endpoint, openStackV3Token, createNetworkRequest.toJson());
            createNetworkResponse = CreateNetworkResponse.fromJson(response);
        } catch (JSONException e) {
        	String message = Messages.Error.COULD_NOT_GENERATING_JSON;
            LOGGER.error(message, e);
            throw new InvalidParameterException(message, e);
        } catch (HttpResponseException e) {
            OpenStackHttpToFogbowRasExceptionMapper.map(e);
        }

        return createNetworkResponse;
    }

    private void createSubNet(OpenStackV3Token openStackV3Token, NetworkOrder order, String networkId, String tenantId)
            throws UnexpectedException, FogbowRasException {
        try {
            String jsonRequest = generateJsonEntityToCreateSubnet(networkId, tenantId, order);
            String endpoint = this.networkV2APIEndpoint + SUFFIX_ENDPOINT_SUBNET;
            this.client.doPostRequest(endpoint, openStackV3Token, jsonRequest);
        } catch (HttpResponseException e) {
            removeNetwork(openStackV3Token, networkId);
            OpenStackHttpToFogbowRasExceptionMapper.map(e);
        }
    }

    private CreateSecurityGroupResponse createSecurityGroup(OpenStackV3Token openStackV3Token, String name,
                                                            String tenantId, String networkId)
            throws UnexpectedException, FogbowRasException {
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
            OpenStackHttpToFogbowRasExceptionMapper.map(e);
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

    private CreateSecurityGroupRuleRequest createSshRuleRequest(String remoteIpPrefix, String securityGroupId) {
        return new CreateSecurityGroupRuleRequest.Builder()
                .direction(INGRESS_DIRECTION)
                .securityGroupId(securityGroupId)
                .remoteIpPrefix(remoteIpPrefix)
                .protocol(TCP_PROTOCOL)
                .minPort(SSH_PORT)
                .maxPort(SSH_PORT)
                .build();
    }

    private void createSecurityGroupRules(NetworkOrder order, OpenStackV3Token openStackV3Token, String networkId, String securityGroupId)
            throws UnexpectedException, FogbowRasException {
        try {
            CreateSecurityGroupRuleRequest sshRuleRequest = createSshRuleRequest(order.getAddress(), securityGroupId);
            CreateSecurityGroupRuleRequest icmpRuleRequest = createIcmpRuleRequest(order.getAddress(), securityGroupId);

            String endpoint = this.networkV2APIEndpoint + SUFFIX_ENDPOINT_SECURITY_GROUP_RULES;
            this.client.doPostRequest(endpoint, openStackV3Token, sshRuleRequest.toJson());
            this.client.doPostRequest(endpoint, openStackV3Token, icmpRuleRequest.toJson());
        } catch (HttpResponseException e) {
            removeNetwork(openStackV3Token, networkId);
            removeSecurityGroup(openStackV3Token, securityGroupId);
            OpenStackHttpToFogbowRasExceptionMapper.map(e);
        }
    }

    protected String getSecurityGroupIdFromGetResponse(String json) throws UnexpectedException {
        String securityGroupId = null;
        try {
            JSONObject response = new JSONObject(json);
            JSONArray securityGroupJSONArray = response.getJSONArray(KEY_SECURITY_GROUPS);
            JSONObject securityGroup = securityGroupJSONArray.optJSONObject(0);
            securityGroupId = securityGroup.getString(KEY_ID);
        } catch (JSONException e) {
            String message = String.format(Messages.Error.NOT_POSSIBLE_RETRIEVE_NETWORK_ID, json);
            LOGGER.error(message);
            throw new UnexpectedException(message, e);
        }
        return securityGroupId;
    }

    @Override
    public NetworkInstance getInstance(String instanceId, OpenStackV3Token openStackV3Token)
            throws FogbowRasException, UnexpectedException {
        String endpoint = this.networkV2APIEndpoint + SUFFIX_ENDPOINT_NETWORK + "/" + instanceId;
        String responseStr = null;
        try {
            responseStr = this.client.doGetRequest(endpoint, openStackV3Token);
        } catch (HttpResponseException e) {
            OpenStackHttpToFogbowRasExceptionMapper.map(e);
        }
        return getInstanceFromJson(responseStr, openStackV3Token);
    }

    @Override
    public void deleteInstance(String networkId, OpenStackV3Token openStackV3Token) throws FogbowRasException, UnexpectedException {
        try {
            removeNetwork(openStackV3Token, networkId);
        } catch (InstanceNotFoundException e) {
            // continue and try to delete the security group
            LOGGER.warn(String.format(Messages.Warn.NETWORK_NOT_FOUND, networkId));
        } catch (UnexpectedException | FogbowRasException e) {
            LOGGER.error(String.format(Messages.Error.NOT_POSSIBLE_DELETE_NETWORK, networkId));
            throw e;
        }

        String securityGroupId = retrieveSecurityGroupId(networkId, openStackV3Token);

        try {
            removeSecurityGroup(openStackV3Token, securityGroupId);
        } catch (UnexpectedException | FogbowRasException e) {
            LOGGER.error(String.format(Messages.Error.NOT_POSSIBLE_DELETE_SECURITY_GROUP, securityGroupId));
            throw e;
        }
    }

    protected String retrieveSecurityGroupId(String networkId, OpenStackV3Token openStackV3Token) throws FogbowRasException, UnexpectedException {
        String securityGroupName = SECURITY_GROUP_PREFIX + "-" + networkId;
        String endpoint = this.networkV2APIEndpoint + SUFFIX_ENDPOINT_SECURITY_GROUP + "?" + QUERY_NAME + "=" + securityGroupName;
        String responseStr = null;
        try {
            responseStr = this.client.doGetRequest(endpoint, openStackV3Token);
        } catch (HttpResponseException e) {
            OpenStackHttpToFogbowRasExceptionMapper.map(e);
        }
        return getSecurityGroupIdFromGetResponse(responseStr);
    }

    protected boolean removeSecurityGroup(OpenStackV3Token openStackV3Token, String securityGroupId)
            throws FogbowRasException, UnexpectedException {
        try {
            String endpoint = this.networkV2APIEndpoint + SUFFIX_ENDPOINT_SECURITY_GROUP + "/" + securityGroupId;
            this.client.doDeleteRequest(endpoint, openStackV3Token);
            return true;
        } catch (HttpResponseException e) {
            OpenStackHttpToFogbowRasExceptionMapper.map(e);
            return false;
        }
    }

    protected NetworkInstance getInstanceFromJson(String json, OpenStackV3Token openStackV3Token)
            throws FogbowRasException, UnexpectedException {
        GetNetworkResponse getNetworkResponse = GetNetworkResponse.fromJson(json);
        String networkId = getNetworkResponse.getId();
        String label = getNetworkResponse.getName();
        String instanceState = getNetworkResponse.getStatus();
        String vlan = getNetworkResponse.getSegmentationId();

        List<String> subnets = getNetworkResponse.getSubnets();
        String subnetId = subnets == null || subnets.size() == 0 ? null : subnets.get(0);

        String address = null;
        String gateway = null;
        NetworkAllocationMode allocationMode = null;
        try {
            GetSubnetResponse subnetInformation = getSubnetInformation(openStackV3Token, subnetId);

            gateway = subnetInformation.getGatewayIp();
            address = subnetInformation.getSubnetAddress();
            boolean dhcpEnabled = subnetInformation.isDhcpEnabled();

            allocationMode = dhcpEnabled ? NetworkAllocationMode.DYNAMIC : NetworkAllocationMode.STATIC;
        } catch (JSONException e) {
        	String message = String.format(Messages.Error.NOT_POSSIBLE_GET_NETWORK, json);
            LOGGER.error(message, e);
            throw new InvalidParameterException(message, e);
        }

        NetworkInstance instance = null;
        if (networkId != null) {
            InstanceState fogbowState = OpenStackStateMapper.map(ResourceType.NETWORK, instanceState);
            instance = new NetworkInstance(networkId, fogbowState, label, address, gateway,
                    vlan, allocationMode, null, null, null);
        }
        return instance;
    }

    private GetSubnetResponse getSubnetInformation(OpenStackV3Token openStackV3Token, String subnetId)
            throws FogbowRasException, UnexpectedException {
        String endpoint = this.networkV2APIEndpoint + SUFFIX_ENDPOINT_SUBNET + "/" + subnetId;
        GetSubnetResponse getSubnetResponse = null;
        try {
            String response = this.client.doGetRequest(endpoint, openStackV3Token);
            getSubnetResponse = GetSubnetResponse.fromJson(response);
        } catch (HttpResponseException e) {
            OpenStackHttpToFogbowRasExceptionMapper.map(e);
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
        	String message = String.format(Messages.Error.NOT_POSSIBLE_RETRIEVE_NETWORK_ID, json);
            LOGGER.error(message);
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

        String networkAddress = order.getAddress();
        networkAddress = networkAddress == null ? DEFAULT_NETWORK_ADDRESS : networkAddress;

        boolean dhcpEnabled = !NetworkAllocationMode.STATIC.equals(order.getAllocation());
        String[] dnsNamesServers = this.dnsList == null ? DEFAULT_DNS_NAME_SERVERS : this.dnsList;

        CreateSubnetRequest createSubnetRequest = new CreateSubnetRequest.Builder()
                .name(subnetName)
                .projectId(tenantId)
                .networkId(networkId)
                .ipVersion(ipVersion)
                .gatewayIp(gateway)
                .networkAddress(networkAddress)
                .dhcpEnabled(dhcpEnabled)
                .dnsNameServers(Arrays.asList(dnsNamesServers))
                .build();

        return createSubnetRequest.toJson();
    }

    protected boolean removeNetwork(OpenStackV3Token openStackV3Token, String networkId) throws UnexpectedException, FogbowRasException {
        String endpoint = this.networkV2APIEndpoint + SUFFIX_ENDPOINT_NETWORK + "/" + networkId;
        try {
            this.client.doDeleteRequest(endpoint, openStackV3Token);
            return true;
        } catch (HttpResponseException e) {
            OpenStackHttpToFogbowRasExceptionMapper.map(e);
            return false;
        }
    }

    private void initClient() {
        this.client = new HttpRequestClientUtil();
    }

    protected void setClient(HttpRequestClientUtil client) {
        this.client = client;
    }

    protected String[] getDnsList() {
        return dnsList;
    }
}