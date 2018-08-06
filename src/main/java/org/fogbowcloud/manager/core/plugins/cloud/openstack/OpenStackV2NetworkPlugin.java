package org.fogbowcloud.manager.core.plugins.cloud.openstack;

import java.io.File;
import java.util.Arrays;
import java.util.List;
import java.util.Properties;
import java.util.UUID;

import org.apache.http.client.HttpResponseException;
import org.apache.log4j.Logger;
import org.fogbowcloud.manager.core.HomeDir;
import org.fogbowcloud.manager.core.constants.DefaultConfigurationConstants;
import org.fogbowcloud.manager.core.exceptions.*;
import org.fogbowcloud.manager.core.models.instances.InstanceState;
import org.fogbowcloud.manager.core.models.ResourceType;
import org.fogbowcloud.manager.core.models.instances.NetworkInstance;
import org.fogbowcloud.manager.core.models.orders.NetworkAllocationMode;
import org.fogbowcloud.manager.core.models.orders.NetworkOrder;
import org.fogbowcloud.manager.core.models.tokens.Token;
import org.fogbowcloud.manager.core.plugins.cloud.NetworkPlugin;
import org.fogbowcloud.manager.core.plugins.serialization.openstack.network.v2.*;
import org.fogbowcloud.manager.util.PropertiesUtil;
import org.fogbowcloud.manager.util.connectivity.HttpRequestClientUtil;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

public class OpenStackV2NetworkPlugin implements NetworkPlugin {
    private static final String NETWORK_NEUTRONV2_URL_KEY = "openstack_neutron_v2_url";

    protected static final String SUFFIX_ENDPOINT_NETWORK = "/networks";
    protected static final String SUFFIX_ENDPOINT_SUBNET = "/subnets";
    protected static final String SUFFIX_ENDPOINT_SECURITY_GROUP_RULES = "/security-group-rules";
    protected static final String SUFFIX_ENDPOINT_SECURITY_GROUP = "/security-groups";
    protected static final String V2_API_ENDPOINT = "/v2.0";

    protected static final String TENANT_ID = "tenantId";
    protected static final String KEY_PROVIDER_SEGMENTATION_ID = "provider:segmentation_id";
    protected static final String KEY_EXTERNAL_GATEWAY_INFO = "external_gateway_info";
    protected static final String QUERY_NAME = "name";

    protected static final String KEY_DNS_NAMESERVERS = "dns_nameservers";
    protected static final String KEY_ENABLE_DHCP = "enable_dhcp";
    protected static final String KEY_IP_VERSION = "ip_version";
    protected static final String KEY_GATEWAY_IP = "gateway_ip";
    protected static final String KEY_TENANT_ID = "tenant_id";
    protected static final String KEY_JSON_NETWORK = "network";
    protected static final String KEY_NETWORK_ID = "network_id";
    protected static final String KEY_JSON_SUBNET = "subnet";
    protected static final String KEY_SUBNETS = "subnets";
    protected static final String KEY_SECURITY_GROUP = "security_group";
    protected static final String KEY_SECURITY_GROUPS = "security_groups";
    protected static final String KEY_STATUS = "status";
    protected static final String KEY_NAME = "name";
    protected static final String KEY_CIDR = "cidr";
    protected static final String KEY_ID = "id";

    protected static final int DEFAULT_IP_VERSION = 4;
    protected static final String DEFAULT_NETWORK_NAME = "fogbow-network";
    protected static final String DEFAULT_SUBNET_NAME = "fogbow-subnet";
    protected static final String[] DEFAULT_DNS_NAME_SERVERS = new String[] {"8.8.8.8", "8.8.4.4"};
    protected static final String DEFAULT_NETWORK_ADDRESS = "192.168.0.1/24";

    // security group properties
    protected static final String INGRESS_DIRECTION = "ingress";
    protected static final String TCP_PROTOCOL = "tcp";
    protected static final String ICMP_PROTOCOL = "icmp";
    protected static final int SSH_PORT = 22;
    protected static final int ANY_PORT = -1;
    public static final String SECURITY_GROUP_PREFIX = "fogbow-sg";

    private HttpRequestClientUtil client;
    private String networkV2APIEndpoint;
    private String[] dnsList;

    private static final Logger LOGGER = Logger.getLogger(OpenStackV2NetworkPlugin.class);

    public OpenStackV2NetworkPlugin() throws FatalErrorException {
        HomeDir homeDir = HomeDir.getInstance();
        Properties properties = PropertiesUtil.readProperties(homeDir.getPath() + File.separator
                + DefaultConfigurationConstants.OPENSTACK_CONF_FILE_NAME);

        this.networkV2APIEndpoint =
                properties.getProperty(NETWORK_NEUTRONV2_URL_KEY)
                        + V2_API_ENDPOINT;
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
    public String requestInstance(NetworkOrder order, Token localToken)
            throws FogbowManagerException, UnexpectedException {
        String tenantId = localToken.getAttributes().get(TENANT_ID);

        CreateNetworkResponse createNetworkResponse = createNetwork(localToken, tenantId);
        String createdNetworkId = createNetworkResponse.getId();

        createSubNet(localToken, order, createdNetworkId, tenantId);

        String securityGroupName = SECURITY_GROUP_PREFIX + "-" + createdNetworkId;
        CreateSecurityGroupResponse securityGroupResponse = createSecurityGroup(localToken, securityGroupName, tenantId, createdNetworkId);

        createSecurityGroupRules(order, localToken, createdNetworkId, securityGroupResponse.getId());
        return createdNetworkId;
    }

    private CreateNetworkResponse createNetwork(Token localToken, String tenantId) throws FogbowManagerException, UnexpectedException {
        CreateNetworkResponse createNetworkResponse = null;
        try {
            String networkName = DEFAULT_NETWORK_NAME + "-" + UUID.randomUUID();

            CreateNetworkRequest createNetworkRequest = new CreateNetworkRequest.Builder()
                    .name(networkName)
                    .tenantId(tenantId)
                    .build();

            String endpoint = this.networkV2APIEndpoint + SUFFIX_ENDPOINT_NETWORK;
            String response = this.client.doPostRequest(endpoint, localToken, new JSONObject(createNetworkRequest.toJson()));
            createNetworkResponse = CreateNetworkResponse.fromJson(response);
        } catch (JSONException e) {
            String errorMsg = "An error occurred when generating json.";
            LOGGER.error(errorMsg, e);
            throw new InvalidParameterException(errorMsg, e);
        } catch (HttpResponseException e) {
            OpenStackHttpToFogbowManagerExceptionMapper.map(e);
        }

        return createNetworkResponse;
    }

    private void createSubNet(Token localToken, NetworkOrder order, String networkId, String tenantId) throws UnexpectedException, FogbowManagerException {
        try {
            JSONObject jsonRequest = generateJsonEntityToCreateSubnet(networkId, tenantId, order);
            String endpoint = this.networkV2APIEndpoint + SUFFIX_ENDPOINT_SUBNET;
            this.client.doPostRequest(endpoint, localToken, jsonRequest);
        } catch (HttpResponseException e) {
            removeNetwork(localToken, networkId);
            OpenStackHttpToFogbowManagerExceptionMapper.map(e);
        }
    }

    private CreateSecurityGroupResponse createSecurityGroup(Token localToken, String name, String tenantId, String networkId) throws UnexpectedException, FogbowManagerException {
        CreateSecurityGroupResponse creationResponse = null;
        try {
            CreateSecurityGroupRequest createSecurityGroupRequest = new CreateSecurityGroupRequest.Builder()
                    .name(name)
                    .tenantId(tenantId)
                    .build();

            String endpoint = this.networkV2APIEndpoint + SUFFIX_ENDPOINT_SECURITY_GROUP;
            JSONObject jsonRequest = new JSONObject(createSecurityGroupRequest.toJson());
            String response = this.client.doPostRequest(endpoint, localToken, jsonRequest);
            creationResponse = CreateSecurityGroupResponse.fromJson(response);
        } catch (HttpResponseException e) {
            removeNetwork(localToken, networkId);
            OpenStackHttpToFogbowManagerExceptionMapper.map(e);
        }
        return creationResponse;
    }

    private CreateSecurityGroupRuleRequest createIcmpRuleRequest(String remoteIpPrefix, String securityGroupId) {
        return new CreateSecurityGroupRuleRequest.Builder()
                .direction(INGRESS_DIRECTION)
                .securityGroupId(securityGroupId)
                .remoteIpPrefix(remoteIpPrefix)
                .protocol(ICMP_PROTOCOL)
                .minPort(ANY_PORT)
                .maxPort(ANY_PORT)
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

    private void createSecurityGroupRules(NetworkOrder order, Token localToken, String networkId, String securityGroupId)
            throws UnexpectedException, FogbowManagerException {
        try {
            CreateSecurityGroupRuleRequest sshRuleRequest = createSshRuleRequest(order.getAddress(), securityGroupId);
            CreateSecurityGroupRuleRequest icmpRuleRequest = createIcmpRuleRequest(order.getAddress(), securityGroupId);

            String endpoint = this.networkV2APIEndpoint + SUFFIX_ENDPOINT_SECURITY_GROUP_RULES;
            this.client.doPostRequest(endpoint, localToken, new JSONObject(sshRuleRequest.toJson()));
            this.client.doPostRequest(endpoint, localToken, new JSONObject(icmpRuleRequest.toJson()));
        } catch (HttpResponseException e) {
            removeNetwork(localToken, networkId);
            removeSecurityGroup(localToken, securityGroupId);
            OpenStackHttpToFogbowManagerExceptionMapper.map(e);
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
            String errorMsg = String.format("It was not possible retrieve network id from json %s", json);
            LOGGER.error(errorMsg);
            throw new UnexpectedException(errorMsg, e);
        }
        return securityGroupId;
    }

    @Override
    public NetworkInstance getInstance(String instanceId, Token token)
            throws FogbowManagerException, UnexpectedException {
        String endpoint = this.networkV2APIEndpoint + SUFFIX_ENDPOINT_NETWORK + "/" + instanceId;
        String responseStr = null;
        try {
            responseStr = this.client.doGetRequest(endpoint, token);
        } catch (HttpResponseException e) {
            OpenStackHttpToFogbowManagerExceptionMapper.map(e);
        }
        return getInstanceFromJson(responseStr, token);

    }

    @Override
    public void deleteInstance(String networkId, Token token) throws FogbowManagerException, UnexpectedException {
        try {
            removeNetwork(token, networkId);
        } catch (InstanceNotFoundException e) {
            // continue and try to delete the security group
            String msg = String.format("Network with id %s not found, trying to delete security group with",
                    networkId);
            LOGGER.warn(msg);
        } catch (UnexpectedException | FogbowManagerException e) {
            String errorMsg = String.format("It was not possible delete network with id %s", networkId);
            LOGGER.error(errorMsg);
            throw e;
        }

        String securityGroupId = retrieveSecurityGroupId(networkId, token);

        try {
            removeSecurityGroup(token, securityGroupId);
        } catch (UnexpectedException | FogbowManagerException e) {
            String errorMsg = String.format("It was not possible delete security group with id %s", securityGroupId);
            LOGGER.error(errorMsg);
            throw e;
        }
    }

    protected String retrieveSecurityGroupId(String networkId, Token token) throws FogbowManagerException, UnexpectedException {
        String securityGroupName = SECURITY_GROUP_PREFIX + "-" + networkId;
        String endpoint = this.networkV2APIEndpoint + SUFFIX_ENDPOINT_SECURITY_GROUP + "?" + QUERY_NAME + "=" + securityGroupName;
        String responseStr = null;
        try {
            responseStr = this.client.doGetRequest(endpoint, token);
        } catch (HttpResponseException e) {
            OpenStackHttpToFogbowManagerExceptionMapper.map(e);
        }
        return getSecurityGroupIdFromGetResponse(responseStr);

    }

    protected boolean removeSecurityGroup(Token token, String securityGroupId)
            throws FogbowManagerException, UnexpectedException {
        LOGGER.debug(String.format("Removing security group %s", securityGroupId));
        try {
            String endpoint = this.networkV2APIEndpoint + SUFFIX_ENDPOINT_SECURITY_GROUP + "/" + securityGroupId;
            this.client.doDeleteRequest(endpoint, token);
            return true;
        } catch (HttpResponseException e) {
            OpenStackHttpToFogbowManagerExceptionMapper.map(e);
            return false;
        }
    }

    protected NetworkInstance getInstanceFromJson(String json, Token token)
            throws FogbowManagerException, UnexpectedException {

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
            GetSubnetResponse subnetInformation = getSubnetInformation(token, subnetId);

            gateway = subnetInformation.getGatewayIp();
            address = subnetInformation.getSubnetAddress();
            boolean dhcpEnabled = subnetInformation.isDhcpEnabled();

            allocationMode = dhcpEnabled ? NetworkAllocationMode.DYNAMIC : NetworkAllocationMode.STATIC;
        } catch (JSONException e) {
            String errorMsg = String.format("It was not possible to get network informations from json %s", json);
            LOGGER.error(errorMsg, e);
            throw new InvalidParameterException(errorMsg, e);
        }

        NetworkInstance instance = null;
        if (networkId != null) {
            InstanceState fogbowState = OpenStackStateMapper.map(ResourceType.NETWORK, instanceState);
            instance = new NetworkInstance(networkId, fogbowState, label, address, gateway,
                    vlan, allocationMode, null, null, null);
        }

        return instance;
    }

    private GetSubnetResponse getSubnetInformation(Token token, String subnetId)
            throws FogbowManagerException, UnexpectedException {
        String endpoint = this.networkV2APIEndpoint + SUFFIX_ENDPOINT_SUBNET + "/" + subnetId;
        GetSubnetResponse getSubnetResponse = null;
        try {
            String response = this.client.doGetRequest(endpoint, token);
            getSubnetResponse = GetSubnetResponse.fromJson(response);
        } catch (HttpResponseException e) {
            OpenStackHttpToFogbowManagerExceptionMapper.map(e);
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
            String errorMsg = String.format("It was not possible retrieve network id from json %s", json);
            LOGGER.error(errorMsg);
            throw new UnexpectedException(errorMsg, e);
        }
        return networkId;
    }

    protected JSONObject generateJsonEntityToCreateSubnet(String networkId, String tenantId,
                                                          NetworkOrder order) {
        String subnetName = DEFAULT_SUBNET_NAME + "-" + UUID.randomUUID();
        int ipVersion = DEFAULT_IP_VERSION;

        String gateway = order.getGateway();
        gateway = gateway == null || gateway.isEmpty() ? null : gateway;

        String networkAddress = order.getAddress();
        networkAddress = networkAddress == null ? DEFAULT_NETWORK_ADDRESS : networkAddress;

        boolean dhcpEnabled = !NetworkAllocationMode.STATIC.equals(order.getAllocation());
        String[] dnsNamesServers = this.dnsList == null ? DEFAULT_DNS_NAME_SERVERS : this.dnsList;

        CreateSubnetRequest createSubnetRequest = new CreateSubnetRequest.Builder()
                .name(subnetName)
                .tenantId(tenantId)
                .networkId(networkId)
                .ipVersion(ipVersion)
                .gatewayIp(gateway)
                .networkAddress(networkAddress)
                .dhcpEnabled(dhcpEnabled)
                .dnsNameServers(Arrays.asList(dnsNamesServers))
                .build();

        return new JSONObject(createSubnetRequest.toJson());

    }

    protected boolean removeNetwork(Token token, String networkId) throws UnexpectedException, FogbowManagerException {
        String messageTemplate = "Removing network %s";
        LOGGER.debug(String.format(messageTemplate, networkId));
        String endpoint = this.networkV2APIEndpoint + SUFFIX_ENDPOINT_NETWORK + "/" + networkId;
        try {
            this.client.doDeleteRequest(endpoint, token);
            return true;
        } catch (HttpResponseException e) {
            OpenStackHttpToFogbowManagerExceptionMapper.map(e);
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