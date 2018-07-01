package org.fogbowcloud.manager.core.plugins.cloud.openstack;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;
import java.util.UUID;
import org.apache.http.client.HttpResponseException;
import org.apache.log4j.Logger;
import org.fogbowcloud.manager.core.HomeDir;
import org.fogbowcloud.manager.core.exceptions.*;
import org.fogbowcloud.manager.core.plugins.cloud.InstanceStateMapper;
import org.fogbowcloud.manager.core.plugins.cloud.NetworkPlugin;
import org.fogbowcloud.manager.core.models.orders.NetworkAllocationMode;
import org.fogbowcloud.manager.core.models.orders.NetworkOrder;
import org.fogbowcloud.manager.core.models.instances.InstanceState;
import org.fogbowcloud.manager.core.models.instances.NetworkInstance;
import org.fogbowcloud.manager.core.models.tokens.Token;
import org.fogbowcloud.manager.util.connectivity.HttpRequestClientUtil;
import org.fogbowcloud.manager.util.PropertiesUtil;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

public class OpenStackV2NetworkPlugin implements NetworkPlugin {

    private static final String NEUTRON_PLUGIN_CONF_FILE = "openstack-neutron-network-plugin.conf";
    private static final String NETWORK_NEUTRONV2_URL_KEY = "openstack_neutron_v2_url";

    private static final String MSG_LOG_ERROR_MANIPULATE_JSON =
            "An error occurred when manipulate json.";
    protected static final String MSG_LOG_THERE_IS_INSTANCE_ASSOCIATED =
            "There is instance associated to the network ";

    private static final String SUFFIX_ENDPOINT_ADD_ROUTER_INTERFACE_ADD = "add_router_interface";
    private static final String SUFFIX_ENDPOINT_REMOVE_ROUTER_INTERFACE_ADD =
            "remove_router_interface";
    protected static final String SUFFIX_ENDPOINT_NETWORK = "/networks";
    protected static final String SUFFIX_ENDPOINT_SUBNET = "/subnets";
    protected static final String SUFFIX_ENDPOINT_ROUTER = "/routers";
    protected static final String SUFFIX_ENDPOINT_PORTS = "/ports";
    protected static final String V2_API_ENDPOINT = "/v2.0";

    protected static final String TENANT_ID = "tenantId";
    protected static final String KEY_PROVIDER_SEGMENTATION_ID = "provider:segmentation_id";
    protected static final String KEY_EXTERNAL_GATEWAY_INFO = "external_gateway_info";
    protected static final String KEY_DNS_NAMESERVERS = "dns_nameservers";
    protected static final String KEY_DEVICE_OWNER = "device_owner";
    protected static final String KEY_JSON_SUBNET_ID = "subnet_id";
    protected static final String KEY_ENABLE_DHCP = "enable_dhcp";
    protected static final String KEY_IP_VERSION = "ip_version";
    protected static final String KEY_GATEWAY_IP = "gateway_ip";
    protected static final String KEY_FIXES_IPS = "fixed_ips";
    protected static final String KEY_TENANT_ID = "tenant_id";
    protected static final String KEY_JSON_ROUTERS = "routers";
    protected static final String KEY_JSON_NETWORK = "network";
    protected static final String KEY_NETWORK_ID = "network_id";
    protected static final String KEY_JSON_SUBNET = "subnet";
    protected static final String KEY_SUBNETS = "subnets";
    protected static final String KEY_JSON_ROUTER = "router";
    protected static final String KEY_JSON_PORTS = "ports";
    protected static final String KEY_DEVICE_ID = "device_id";
    protected static final String KEY_STATUS = "status";
    protected static final String KEY_NAME = "name";
    protected static final String KEY_CIDR = "cidr";
    protected static final String KEY_ID = "id";

    protected static final String DEFAULT_IP_VERSION = "4";
    protected static final String DEFAULT_NETWORK_NAME = "network-fogbow";
    protected static final String DEFAULT_ROUTER_NAME = "router-fogbow";
    protected static final String DEFAULT_SUBNET_NAME = "subnet-fogbow";
    protected static final String[] DEFAULT_DNS_NAME_SERVERS = new String[] {"8.8.8.8", "8.8.4.4"};
    protected static final String DEFAULT_NETWORK_ADDRESS = "192.168.0.1/24";
    protected static final String NETWORK_DHCP = "network:dhcp";
    protected static final String COMPUTE_NOVA = "compute:nova";
    protected static final String NETWORK_ROUTER = "network:ha_router_replicated_interface";

    private HttpRequestClientUtil client;
    private String networkV2APIEndpoint;
    private String externalNetworkId;
    private String[] dnsList;
    private InstanceStateMapper instanceStateMapper;

    private static final Logger LOGGER = Logger.getLogger(OpenStackV2NetworkPlugin.class);

    public OpenStackV2NetworkPlugin() throws FatalErrorException {
        HomeDir homeDir = HomeDir.getInstance();
        Properties properties = PropertiesUtil.
                readProperties(homeDir.getPath() + File.separator + NEUTRON_PLUGIN_CONF_FILE);

        this.externalNetworkId = properties.getProperty(KEY_EXTERNAL_GATEWAY_INFO);
        this.networkV2APIEndpoint =
                properties.getProperty(NETWORK_NEUTRONV2_URL_KEY)
                        + V2_API_ENDPOINT;
        this.instanceStateMapper = new OpenStackNetworkInstanceStateMapper();
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
        JSONObject jsonRequest = null;
        String tenantId = localToken.getAttributes().get(TENANT_ID);

        // Creating router
        try {
            jsonRequest = generateJsonEntityToCreateRouter();
        } catch (JSONException e) {
            String errorMsg = "An error occurred when generating json.";
            LOGGER.error(errorMsg, e);
            throw new InvalidParameterException(errorMsg, e);
        }
        String endpoint = this.networkV2APIEndpoint + SUFFIX_ENDPOINT_ROUTER;
        String responseStr = null;
        try {
            responseStr = this.client.doPostRequest(endpoint, localToken, jsonRequest);
        } catch (HttpResponseException e) {
            OpenStackHttpToFogbowManagerExceptionMapper.map(e);
        }
        String routerId = getRouterIdFromJson(responseStr);

        // Creating network
        try {
            jsonRequest = generateJsonEntityToCreateNetwork(tenantId);
        } catch (JSONException e) {
            String errorMsg = "An error occurred when generating json.";
            LOGGER.error(errorMsg, e);
            throw new InvalidParameterException(errorMsg, e);
        }
        try {
            endpoint = this.networkV2APIEndpoint + SUFFIX_ENDPOINT_NETWORK;
            responseStr = this.client.doPostRequest(endpoint, localToken, jsonRequest);
        } catch (HttpResponseException e) {
            removeRouter(localToken, routerId);
            OpenStackHttpToFogbowManagerExceptionMapper.map(e);
        }
        String networkId = getNetworkIdFromJson(responseStr);

        // Creating subnet
        try {
            jsonRequest = generateJsonEntityToCreateSubnet(networkId, tenantId, order);
        } catch (JSONException e) {
            String errorMsg =
                    String.format("Error while trying to generate json subnet entity with networkId %s for order %s",
                            networkId, order);
            LOGGER.error(errorMsg, e);
            throw new InvalidParameterException(errorMsg, e);
        }
        try {
            endpoint = this.networkV2APIEndpoint + SUFFIX_ENDPOINT_SUBNET;
            responseStr = this.client.doPostRequest(endpoint, localToken, jsonRequest);
        } catch (HttpResponseException e) {
            removeRouter(localToken, routerId);
            removeNetwork(localToken, networkId);
            OpenStackHttpToFogbowManagerExceptionMapper.map(e);
        }

        String subnetId = getSubnetIdFromJson(responseStr);
        try {
            jsonRequest = generateJsonEntitySubnetId(subnetId);
        } catch (JSONException e) {
            String errorMsg = String.format("Error while trying to generate json entity with subnetId %s", subnetId);
            LOGGER.error(errorMsg, e);
            throw new InvalidParameterException(errorMsg, e);
        }

        // Adding router interface
        try {
            endpoint = this.networkV2APIEndpoint + SUFFIX_ENDPOINT_ROUTER + "/" + routerId + "/"
                    + SUFFIX_ENDPOINT_ADD_ROUTER_INTERFACE_ADD;
            this.client.doPutRequest(endpoint, localToken, jsonRequest);
        } catch (HttpResponseException e) {
            removeRouter(localToken, routerId);
            removeNetwork(localToken, networkId);
            OpenStackHttpToFogbowManagerExceptionMapper.map(e);
        }
        return networkId;
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
    public void deleteInstance(String instanceId, Token token) throws FogbowManagerException, UnexpectedException {
        // TODO: ensure that all the necessary tokens attributes are set.
        String tenantId = token.getAttributes().get(TENANT_ID);
        String endpoint = this.networkV2APIEndpoint + SUFFIX_ENDPOINT_PORTS + "?" + KEY_TENANT_ID + "=" + tenantId;

        String responseStr = null;
        try {
            responseStr = this.client.doGetRequest(endpoint, token);
        } catch (HttpResponseException e) {
            OpenStackHttpToFogbowManagerExceptionMapper.map(e);
        }

        String routerId = null;
        List<String> subnets = new ArrayList<String>();
        JSONObject rootServer = new JSONObject(responseStr);
        try {
            JSONArray routerPortsJSONArray = rootServer.optJSONArray(KEY_JSON_PORTS);
            for (int i = 0; i < routerPortsJSONArray.length(); i++) {
                String networkId = routerPortsJSONArray.optJSONObject(i).optString(KEY_NETWORK_ID);

                if (networkId.equals(instanceId)) {
                    String deviceOwner =
                            routerPortsJSONArray.optJSONObject(i).optString(KEY_DEVICE_OWNER);

                    boolean thereIsInstance = deviceOwner.equals(COMPUTE_NOVA);
                    if (thereIsInstance) {
                        throw new InvalidParameterException();
                    }

                    if (deviceOwner.equals(NETWORK_ROUTER)) {
                        routerId = routerPortsJSONArray.optJSONObject(i).optString(KEY_DEVICE_ID);
                    }

                    if (!deviceOwner.equals(NETWORK_DHCP)) {
                        String subnetId =
                                routerPortsJSONArray.optJSONObject(i).optJSONArray(KEY_FIXES_IPS)
                                        .optJSONObject(0).optString(KEY_JSON_SUBNET_ID);
                        subnets.add(subnetId);
                    }
                }
            }
            for (String subnetId : subnets) {
                JSONObject jsonRequest = generateJsonEntitySubnetId(subnetId);
                if (routerId != null) {
                    endpoint = this.networkV2APIEndpoint + SUFFIX_ENDPOINT_ROUTER + "/" + routerId
                            + "/" + SUFFIX_ENDPOINT_REMOVE_ROUTER_INTERFACE_ADD;
                    try {
                        this.client.doPutRequest(endpoint, token, jsonRequest);
                    } catch (HttpResponseException e) {
                        OpenStackHttpToFogbowManagerExceptionMapper.map(e);
                    }
                }
            }
        } catch (JSONException e) {
            LOGGER.error(MSG_LOG_ERROR_MANIPULATE_JSON);
            throw new UnexpectedException(MSG_LOG_ERROR_MANIPULATE_JSON, e);
        } catch (NullPointerException e) {
            LOGGER.error(MSG_LOG_ERROR_MANIPULATE_JSON);
            throw new UnexpectedException(MSG_LOG_ERROR_MANIPULATE_JSON, e);
        }

        if (routerId != null) {
            removeRouter(token, routerId);
        }
        if (!removeNetwork(token, instanceId)) {
            String errorMsg = String.format("Was not possible delete network with id %s", instanceId);
            LOGGER.error(errorMsg);
            throw new UnexpectedException(errorMsg);
        }
    }

    protected NetworkInstance getInstanceFromJson(String json, Token token)
            throws FogbowManagerException, UnexpectedException {
        String networkId = null;
        String label = null;
        InstanceState instanceState = null;
        String vlan = null;
        String subnetId = null;
        try {
            JSONObject rootServer = new JSONObject(json);
            JSONObject networkJSONObject = rootServer.optJSONObject(KEY_JSON_NETWORK);
            networkId = networkJSONObject.optString(KEY_ID);

            vlan = networkJSONObject.optString(KEY_PROVIDER_SEGMENTATION_ID);
            String instanceStatus = networkJSONObject.optString(KEY_STATUS);
            instanceState = this.instanceStateMapper.getInstanceState(instanceStatus);
            label = networkJSONObject.optString(KEY_NAME);

            subnetId = networkJSONObject.optJSONArray(KEY_SUBNETS).optString(0);
        } catch (JSONException e) {
            String errorMsg = String.format("Was not possible to get network informations from json %s", json);
            LOGGER.error(errorMsg, e);
            throw new InvalidParameterException(errorMsg, e);
        }

        String gateway = null;
        String address = null;
        NetworkAllocationMode allocation = null;
        try {
            JSONObject subnetJSONObject = getSubnetInformation(token, subnetId);

            if (subnetJSONObject != null) {
                gateway = subnetJSONObject.optString(KEY_GATEWAY_IP);
                allocation = null;
                boolean enableDHCP = subnetJSONObject.optBoolean(KEY_ENABLE_DHCP);
                if (enableDHCP) {
                    allocation = NetworkAllocationMode.DYNAMIC;
                } else {
                    allocation = NetworkAllocationMode.STATIC;
                }
                address = subnetJSONObject.optString(KEY_CIDR);
            }
        } catch (JSONException e) {
            String errorMsg = String.format("Was not possible to get network informations from json %s", json);
            LOGGER.error(errorMsg, e);
            throw new InvalidParameterException(errorMsg, e);
        }

        NetworkInstance instance = null;
        if (networkId != null) {
            instance = new NetworkInstance(networkId, instanceState, label, address, gateway,
                    vlan, allocation, null, null, null);
        }

        return instance;
    }

    private JSONObject getSubnetInformation(Token token, String subnetId)
            throws FogbowManagerException, UnexpectedException {
        String endpoint = this.networkV2APIEndpoint + SUFFIX_ENDPOINT_SUBNET + "/" + subnetId;
        String responseStr = null;
        try {
            responseStr = this.client.doGetRequest(endpoint, token);
        } catch (HttpResponseException e) {
            OpenStackHttpToFogbowManagerExceptionMapper.map(e);
        }

        JSONObject rootServer = new JSONObject(responseStr);
        JSONObject subnetJSON = rootServer.optJSONObject(KEY_JSON_SUBNET);
        return subnetJSON;
    }

    protected String[] getRoutersFromJson(String json) throws UnexpectedException {
        try {
            JSONObject rootServer = new JSONObject(json);
            JSONArray routersJSONArray = rootServer.optJSONArray(KEY_JSON_ROUTERS);
            String[] routerIds = new String[routersJSONArray.length()];
            for (int i = 0; i < routersJSONArray.length(); i++) {
                routerIds[i] = routersJSONArray.optJSONObject(i).optString(KEY_ID);
            }
            return routerIds;
        } catch (JSONException e) {
            String errorMsg = String.format("Was not possible retrieve routers from json %s", json);
            LOGGER.error(errorMsg);
            throw new UnexpectedException(errorMsg, e);
        }
    }

    protected String getNetworkIdFromJson(String json) throws UnexpectedException {
        String networkId = null;
        try {
            JSONObject rootServer = new JSONObject(json);
            JSONObject networkJSONObject = rootServer.optJSONObject(KEY_JSON_NETWORK);
            networkId = networkJSONObject.optString(KEY_ID);
        } catch (JSONException e) {
            String errorMsg = String.format("Was not possible retrieve network id from json %s", json);
            LOGGER.error(errorMsg);
            throw new UnexpectedException(errorMsg, e);
        }
        return networkId;
    }

    protected String getSubnetIdFromJson(String json) throws UnexpectedException {
        String subnetId = null;
        try {
            JSONObject rootServer = new JSONObject(json);
            JSONObject networkJSONObject = rootServer.optJSONObject(KEY_JSON_SUBNET);
            subnetId = networkJSONObject.optString(KEY_ID);
        } catch (JSONException e) {
            String errorMsg = String.format("Was not possible retrieve subnet id from json %s", json);
            LOGGER.error(errorMsg);
            throw new UnexpectedException(errorMsg, e);
        }
        return subnetId;
    }

    protected String getRouterIdFromJson(String json) throws UnexpectedException {
        String routerId = null;
        try {
            JSONObject rootServer = new JSONObject(json);
            JSONObject networkJSONObject = rootServer.optJSONObject(KEY_JSON_ROUTER);
            routerId = networkJSONObject.optString(KEY_ID);
        } catch (JSONException e) {
            String errorMsg = String.format("Was not possible retrieve router id from json %s", json);
            LOGGER.error(errorMsg);
            throw new UnexpectedException(errorMsg, e);
        }
        return routerId;
    }

    protected JSONObject generateJsonEntityToCreateRouter() throws JSONException {
        JSONObject networkId = new JSONObject();
        networkId.put(KEY_NETWORK_ID, this.externalNetworkId);

        JSONObject routerContent = new JSONObject();
        routerContent.put(KEY_EXTERNAL_GATEWAY_INFO, networkId);
        routerContent.put(KEY_NAME, DEFAULT_ROUTER_NAME + "-" + UUID.randomUUID());

        JSONObject router = new JSONObject();
        router.put(KEY_JSON_ROUTER, routerContent);

        return router;
    }

    protected JSONObject generateJsonEntityToCreateNetwork(String tenantId) throws JSONException {
        JSONObject networkContent = new JSONObject();
        networkContent.put(KEY_NAME, DEFAULT_NETWORK_NAME + "-" + UUID.randomUUID());
        networkContent.put(KEY_TENANT_ID, tenantId);

        JSONObject network = new JSONObject();
        network.put(KEY_JSON_NETWORK, networkContent);

        return network;
    }

    protected JSONObject generateJsonEntitySubnetId(String subnetId) throws JSONException {
        JSONObject subnet = new JSONObject();
        subnet.put(KEY_JSON_SUBNET_ID, subnetId);

        return subnet;
    }

    protected JSONObject generateJsonEntityToCreateSubnet(String networkId, String tenantId,
            NetworkOrder order) throws JSONException {
        JSONObject subnetContent = new JSONObject();
        subnetContent.put(KEY_NAME, DEFAULT_SUBNET_NAME + "-" + UUID.randomUUID());
        subnetContent.put(KEY_TENANT_ID, tenantId);
        subnetContent.put(KEY_NETWORK_ID, networkId);
        subnetContent.put(KEY_IP_VERSION, DEFAULT_IP_VERSION);

        String gateway = order.getGateway();
        if (gateway != null && !gateway.isEmpty()) {
            subnetContent.put(KEY_GATEWAY_IP, gateway);
        }

        String networkAddress = order.getAddress();
        if (networkAddress == null) {
            networkAddress = DEFAULT_NETWORK_ADDRESS;
        }
        subnetContent.put(KEY_CIDR, networkAddress);

        NetworkAllocationMode networkAllocationMode = order.getAllocation();
        if (networkAllocationMode != null) {
            if (networkAllocationMode.equals(NetworkAllocationMode.DYNAMIC)) {
                subnetContent.put(KEY_ENABLE_DHCP, true);
            } else if (networkAllocationMode.equals(NetworkAllocationMode.STATIC)) {
                subnetContent.put(KEY_ENABLE_DHCP, false);
            }
        }

        String[] dnsNamesServers = this.dnsList;
        if (dnsNamesServers == null) {
            dnsNamesServers = DEFAULT_DNS_NAME_SERVERS;
        }
        JSONArray dnsNameServersArray = new JSONArray();
        for (int i = 0; i < dnsNamesServers.length; i++) {
            dnsNameServersArray.put(dnsNamesServers[i]);
        }
        subnetContent.put(KEY_DNS_NAMESERVERS, dnsNameServersArray);

        JSONObject subnet = new JSONObject();
        subnet.put(KEY_JSON_SUBNET, subnetContent);

        return subnet;
    }

    protected boolean removeNetwork(Token token, String networkId) throws UnexpectedException, FogbowManagerException {
        boolean deleted = false;
        String messageTemplate = "Removing network %s";
        LOGGER.debug(String.format(messageTemplate, networkId));
        String endpoint = this.networkV2APIEndpoint + SUFFIX_ENDPOINT_NETWORK + "/" + networkId;
        try {
            this.client.doDeleteRequest(endpoint, token);
            deleted = true;
        } catch (HttpResponseException e) {
            OpenStackHttpToFogbowManagerExceptionMapper.map(e);
        }
        return deleted;
    }

    protected boolean removeRouter(Token token, String routerId) throws FogbowManagerException, UnexpectedException {
        boolean deleted = false;
        String messageTemplate = "Removing router %s";
        LOGGER.debug(String.format(messageTemplate, routerId));
        String endpoint = this.networkV2APIEndpoint + SUFFIX_ENDPOINT_ROUTER + "/" + routerId;
        try {
            this.client.doDeleteRequest(endpoint, token);
            deleted = true;
        } catch (HttpResponseException e) {
            OpenStackHttpToFogbowManagerExceptionMapper.map(e);
        }
        return deleted;
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
