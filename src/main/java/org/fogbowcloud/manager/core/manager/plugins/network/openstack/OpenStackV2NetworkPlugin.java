package org.fogbowcloud.manager.core.manager.plugins.network.openstack;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;
import java.util.UUID;
import org.apache.http.HttpEntity;
import org.apache.http.HttpMessage;
import org.apache.http.HttpResponse;
import org.apache.http.HttpStatus;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpDelete;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.methods.HttpPut;
import org.apache.http.entity.StringEntity;
import org.apache.http.util.EntityUtils;
import org.apache.log4j.Logger;
import org.fogbowcloud.manager.core.exceptions.RequestException;
import org.fogbowcloud.manager.core.manager.constants.OpenStackConfigurationConstants;
import org.fogbowcloud.manager.core.manager.plugins.InstanceStateMapper;
import org.fogbowcloud.manager.core.manager.plugins.network.NetworkPlugin;
import org.fogbowcloud.manager.core.models.ErrorType;
import org.fogbowcloud.manager.core.models.RequestHeaders;
import org.fogbowcloud.manager.core.models.ResponseConstants;
import org.fogbowcloud.manager.core.models.orders.NetworkAllocation;
import org.fogbowcloud.manager.core.models.orders.NetworkOrder;
import org.fogbowcloud.manager.core.models.orders.instances.InstanceState;
import org.fogbowcloud.manager.core.models.orders.instances.NetworkOrderInstance;
import org.fogbowcloud.manager.core.models.token.Token;
import org.fogbowcloud.manager.utils.HttpRequestUtil;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

public class OpenStackV2NetworkPlugin implements NetworkPlugin {
    protected static final String STATUS_OPENSTACK_ACTIVE = "ACTIVE";

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

    private HttpClient client;
    private String networkV2APIEndpoint;
    private String externalNetworkId;
    private String[] dnsList;
    private InstanceStateMapper instanceStateMapper;

    private static final Logger LOGGER = Logger.getLogger(OpenStackV2NetworkPlugin.class);

    public OpenStackV2NetworkPlugin(Properties properties) {
        this.externalNetworkId = properties.getProperty(KEY_EXTERNAL_GATEWAY_INFO);
        this.networkV2APIEndpoint =
                properties.getProperty(OpenStackConfigurationConstants.NETWORK_NOVAV2_URL_KEY)
                        + V2_API_ENDPOINT;
        this.instanceStateMapper = new OpenStackNetworkInstanceStateMapper();
        setDNSList(properties);

        initClient();
    }

    private void setDNSList(Properties properties) {
        String dnsProperty = properties.getProperty(KEY_DNS_NAMESERVERS);
        if (dnsProperty != null) {
            this.dnsList = dnsProperty.split(",");
        }
    }

    @Override
    public String requestInstance(Token localToken, NetworkOrder order) throws RequestException {
        JSONObject jsonRequest = null;
        String tenantId = localToken.getAttributes().get(TENANT_ID);

        // Creating router
        try {
            jsonRequest = generateJsonEntityToCreateRouter();
        } catch (JSONException e) {
            LOGGER.error("An error occurred when generating json.", e);
            throw new RequestException(ErrorType.BAD_REQUEST, ResponseConstants.IRREGULAR_SYNTAX);
        }
        String endpoint = this.networkV2APIEndpoint + SUFFIX_ENDPOINT_ROUTER;
        String responseStr = doPostRequest(endpoint, localToken.getAccessId(), jsonRequest);
        String routerId = getRouterIdFromJson(responseStr);

        // Creating network
        try {
            jsonRequest = generateJsonEntityToCreateNetwork(tenantId);
        } catch (JSONException e) {
            String messageTemplate = "Error while trying to generate json data";
            LOGGER.error(messageTemplate, e);
            throw new RequestException(ErrorType.BAD_REQUEST, ResponseConstants.IRREGULAR_SYNTAX);
        }
        try {
            endpoint = this.networkV2APIEndpoint + SUFFIX_ENDPOINT_NETWORK;
            responseStr = doPostRequest(endpoint, localToken.getAccessId(), jsonRequest);
        } catch (RequestException e) {
            removeRouter(localToken, routerId);
            throw e;
        }
        String networkId = getNetworkIdFromJson(responseStr);

        // Creating subnet
        try {
            jsonRequest = generateJsonEntityToCreateSubnet(networkId, tenantId, order);
        } catch (JSONException e) {
            String messageTemplate =
                    "Error while trying to generate json subnet entity with networkId %s for order %s";
            LOGGER.error(String.format(messageTemplate, networkId, order), e);
            throw new RequestException(ErrorType.BAD_REQUEST, ResponseConstants.IRREGULAR_SYNTAX);
        }
        try {
            endpoint = this.networkV2APIEndpoint + SUFFIX_ENDPOINT_SUBNET;
            responseStr = doPostRequest(endpoint, localToken.getAccessId(), jsonRequest);
        } catch (RequestException e) {
            String messageTemplate = "Error while trying to create subnet for order %s";
            LOGGER.error(String.format(messageTemplate, order));
            removeRouter(localToken, routerId);
            removeNetwork(localToken, networkId);
            throw e;
        }

        String subnetId = getSubnetIdFromJson(responseStr);
        try {
            jsonRequest = generateJsonEntitySubnetId(subnetId);
        } catch (JSONException e) {
            String messageTemplate = "Error while trying to generate json entity with subnetId %s";
            LOGGER.error(String.format(messageTemplate, subnetId), e);
            throw new RequestException(ErrorType.BAD_REQUEST, ResponseConstants.IRREGULAR_SYNTAX);
        }

        // Adding router interface
        try {
            endpoint = this.networkV2APIEndpoint + SUFFIX_ENDPOINT_ROUTER + "/" + routerId + "/"
                    + SUFFIX_ENDPOINT_ADD_ROUTER_INTERFACE_ADD;
            doPutRequest(endpoint, localToken.getAccessId(), jsonRequest);
        } catch (RequestException e) {
            String messageTemplate = "Error while trying to add router interface with json %s";
            LOGGER.error(String.format(messageTemplate, jsonRequest));
            removeRouter(localToken, routerId);
            removeNetwork(localToken, networkId);
            throw e;
        }

        return networkId;
    }

    @Override
    public NetworkOrderInstance getInstance(Token token, String instanceId)
            throws RequestException {
        String endpoint = this.networkV2APIEndpoint + SUFFIX_ENDPOINT_NETWORK + "/" + instanceId;
        String responseStr = doGetRequest(endpoint, token.getAccessId());
        NetworkOrderInstance instance = getInstanceFromJson(responseStr, token);
        return instance;
    }

    @Override
    public void deleteInstance(Token token, String instanceId) throws RequestException {
        // TODO: ensure that all the necessary token attributes are set.
        String tenantId = token.getAttributes().get(TENANT_ID);
        String endpoint = this.networkV2APIEndpoint + SUFFIX_ENDPOINT_PORTS + "?" + KEY_TENANT_ID
                + "=" + tenantId;
        String responseStr = doGetRequest(endpoint, token.getAccessId());

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
                        throw new RequestException(ErrorType.BAD_REQUEST,
                                MSG_LOG_THERE_IS_INSTANCE_ASSOCIATED + "( " + instanceId + " ).");
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
                    doPutRequest(endpoint, token.getAccessId(), jsonRequest);
                }
            }
        } catch (JSONException e) {
            LOGGER.error(MSG_LOG_ERROR_MANIPULATE_JSON);
            throw new RequestException(ErrorType.BAD_REQUEST, ResponseConstants.IRREGULAR_SYNTAX);
        } catch (NullPointerException e) {
            LOGGER.error(MSG_LOG_ERROR_MANIPULATE_JSON);
            throw new RequestException(ErrorType.BAD_REQUEST, ResponseConstants.IRREGULAR_SYNTAX);
        }


        if (routerId != null) {
            removeRouter(token, routerId);
        }
        if (!removeNetwork(token, instanceId)) {
            String messageTemplate = "Was not possible delete network with id %s";
            LOGGER.error(String.format(messageTemplate, instanceId));
            throw new RequestException(ErrorType.BAD_REQUEST, ResponseConstants.IRREGULAR_SYNTAX);
        }
    }

    protected NetworkOrderInstance getInstanceFromJson(String json, Token token) {
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
            String messageTemplate = "Was not possible to get network informations from json %s";
            LOGGER.warn(String.format(messageTemplate, json), e);
        }

        String gateway = null;
        String address = null;
        NetworkAllocation allocation = null;
        try {
            JSONObject subnetJSONObject = getSubnetInformation(token, subnetId);

            if (subnetJSONObject != null) {
                gateway = subnetJSONObject.optString(KEY_GATEWAY_IP);
                allocation = null;
                boolean enableDHCP = subnetJSONObject.optBoolean(KEY_ENABLE_DHCP);
                if (enableDHCP) {
                    allocation = NetworkAllocation.DYNAMIC;
                } else {
                    allocation = NetworkAllocation.STATIC;
                }
                address = subnetJSONObject.optString(KEY_CIDR);
            }
        } catch (Exception e) {
            String messageTemplate = "Was not possible to get subnet informations of subnet id %s";
            LOGGER.warn(String.format(messageTemplate, subnetId), e);
        }

        NetworkOrderInstance instance = null;
        if (networkId != null) {
            instance = new NetworkOrderInstance(networkId, label, instanceState, address, gateway,
                    vlan, allocation, null, null, null);
        }

        return instance;
    }

    private JSONObject getSubnetInformation(Token token, String subnetId) throws RequestException {
        String endpoint = this.networkV2APIEndpoint + SUFFIX_ENDPOINT_SUBNET + "/" + subnetId;
        String responseStr = doGetRequest(endpoint, token.getAccessId());

        JSONObject rootServer = new JSONObject(responseStr);
        JSONObject subnetJSON = rootServer.optJSONObject(KEY_JSON_SUBNET);
        return subnetJSON;
    }

    protected String[] getRoutersFromJson(String json) {
        try {
            JSONObject rootServer = new JSONObject(json);
            JSONArray routersJSONArray = rootServer.optJSONArray(KEY_JSON_ROUTERS);
            String[] routerIds = new String[routersJSONArray.length()];
            for (int i = 0; i < routersJSONArray.length(); i++) {
                routerIds[i] = routersJSONArray.optJSONObject(i).optString(KEY_ID);
            }
            return routerIds;
        } catch (JSONException e) {
            LOGGER.warn("There was an exception while getting routers from json.", e);
        }
        return null;
    }

    protected String getNetworkIdFromJson(String json) {
        String networkId = null;
        try {
            JSONObject rootServer = new JSONObject(json);
            JSONObject networkJSONObject = rootServer.optJSONObject(KEY_JSON_NETWORK);
            networkId = networkJSONObject.optString(KEY_ID);
        } catch (JSONException e) {
            String messageTemplate = "Was not possible to get the network id from json %s";
            LOGGER.warn(String.format(messageTemplate, json.toString()), e);
        }
        return networkId;
    }

    protected String getSubnetIdFromJson(String json) {
        String subnetId = null;
        try {
            JSONObject rootServer = new JSONObject(json);
            JSONObject networkJSONObject = rootServer.optJSONObject(KEY_JSON_SUBNET);
            subnetId = networkJSONObject.optString(KEY_ID);
        } catch (JSONException e) {
            String messageTemplate = "Was not possible to get the subnet id from json %s";
            LOGGER.warn(String.format(messageTemplate, json.toString()), e);
        }
        return subnetId;
    }

    protected String getRouterIdFromJson(String json) {
        String routerId = null;
        try {
            JSONObject rootServer = new JSONObject(json);
            JSONObject networkJSONObject = rootServer.optJSONObject(KEY_JSON_ROUTER);
            routerId = networkJSONObject.optString(KEY_ID);
        } catch (JSONException e) {
            String messageTemplate = "Was not possible to get the router id from json %s";
            LOGGER.warn(String.format(messageTemplate, json.toString()), e);
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

        NetworkAllocation networkAllocation = order.getAllocation();
        if (networkAllocation != null) {
            if (networkAllocation.equals(NetworkAllocation.DYNAMIC)) {
                subnetContent.put(KEY_ENABLE_DHCP, true);
            } else if (networkAllocation.equals(NetworkAllocation.STATIC)) {
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

    protected boolean removeNetwork(Token token, String networkId) {
        boolean deleted = false;
        String messageTemplate = "Removing network %s";
        LOGGER.debug(String.format(messageTemplate, networkId));
        try {
            String endpoint = this.networkV2APIEndpoint + SUFFIX_ENDPOINT_NETWORK + "/" + networkId;
            doDeleteRequest(endpoint, token.getAccessId());
            deleted = true;
        } catch (RequestException e) {
            messageTemplate = "Was not possible remove network %s";
            LOGGER.error(String.format(messageTemplate, networkId), e);
        }
        return deleted;
    }

    protected boolean removeRouter(Token token, String routerId) {
        boolean deleted = false;
        String messageTemplate = "Removing router %s";
        LOGGER.debug(String.format(messageTemplate, routerId));
        try {
            String endpoint = this.networkV2APIEndpoint + SUFFIX_ENDPOINT_ROUTER + "/" + routerId;
            doDeleteRequest(endpoint, token.getAccessId());
            deleted = true;
        } catch (RequestException e) {
            messageTemplate = "Was not possible remove router %s";
            LOGGER.error(String.format(messageTemplate, routerId), e);
        }
        return deleted;
    }

    protected String doPostRequest(String endpoint, String authToken, JSONObject data)
            throws RequestException {
        HttpPost request = new HttpPost(endpoint);
        addRequestHeaders(request, authToken);
        request.setEntity(new StringEntity(data.toString(), StandardCharsets.UTF_8));

        HttpResponse response = null;
        String responseStr = null;
        String messageTemplate = "Posting %s at %s with auth token %s";
        LOGGER.debug(String.format(messageTemplate, data.toString(), endpoint, authToken));
        try {
            response = this.client.execute(request);
            responseStr = EntityUtils.toString(response.getEntity(), StandardCharsets.UTF_8);
        } catch (Exception e) {
            messageTemplate = "Error while trying to post";
            LOGGER.error(messageTemplate, e);
            throw new RequestException(ErrorType.BAD_REQUEST, ResponseConstants.IRREGULAR_SYNTAX);
        } finally {
            consumeResponseStream(response);
        }

        checkStatusResponse(response, responseStr);

        return responseStr;
    }

    protected String doGetRequest(String endpoint, String authToken) throws RequestException {
        HttpGet request = new HttpGet(endpoint);
        addRequestHeaders(request, authToken);

        HttpResponse response = null;
        String responseStr = null;
        String messageTemplate = "Getting at %s with auth token %s";
        LOGGER.debug(String.format(messageTemplate, endpoint, authToken));
        try {
            response = this.client.execute(request);
            responseStr = EntityUtils.toString(response.getEntity(), StandardCharsets.UTF_8);
        } catch (Exception e) {
            messageTemplate = "Error while trying to get";
            LOGGER.error(messageTemplate, e);
            throw new RequestException(ErrorType.BAD_REQUEST, ResponseConstants.IRREGULAR_SYNTAX);
        } finally {
            consumeResponseStream(response);
        }

        checkStatusResponse(response, responseStr);

        return responseStr;
    }

    protected String doPutRequest(String endpoint, String authToken, JSONObject data)
            throws RequestException {
        HttpPut request = new HttpPut(endpoint);
        addRequestHeaders(request, authToken);
        // TODO: check it (ensure that data is never null).
        if (data != null) {
            request.setEntity(new StringEntity(data.toString(), StandardCharsets.UTF_8));
        }

        HttpResponse response = null;
        String responseStr = null;
        String messageTemplate = "Putting %s at %s with auth token %s";
        LOGGER.debug(String.format(messageTemplate, data.toString(), endpoint, authToken));
        try {
            response = this.client.execute(request);
            responseStr = EntityUtils.toString(response.getEntity(), StandardCharsets.UTF_8);
        } catch (Exception e) {
            messageTemplate = "Error while trying to put";
            LOGGER.error(messageTemplate, e);
            throw new RequestException(ErrorType.BAD_REQUEST, ResponseConstants.IRREGULAR_SYNTAX);
        } finally {
            consumeResponseStream(response);
        }

        checkStatusResponse(response, responseStr);

        return responseStr;
    }

    protected void doDeleteRequest(String endpoint, String authToken) throws RequestException {
        HttpDelete request = new HttpDelete(endpoint);
        addRequestHeaders(request, authToken);

        HttpResponse response = null;
        String responseStr = null;
        String messageTemplate = "Deleting at %s with auth token %s";
        LOGGER.debug(String.format(messageTemplate, endpoint, authToken));
        try {
            response = this.client.execute(request);
            HttpEntity entity = response.getEntity();
            if (entity != null) {
                responseStr = EntityUtils.toString(entity, StandardCharsets.UTF_8);
            }
        } catch (Exception e) {
            messageTemplate = "Error while trying to delete";
            LOGGER.error(messageTemplate, e);
            throw new RequestException(ErrorType.BAD_REQUEST, ResponseConstants.IRREGULAR_SYNTAX);
        } finally {
            consumeResponseStream(response);
        }

        checkStatusResponse(response, responseStr);
    }

    private void checkStatusResponse(HttpResponse response, String message)
            throws RequestException {
        if (response.getStatusLine().getStatusCode() == HttpStatus.SC_UNAUTHORIZED) {
            throw new RequestException(ErrorType.UNAUTHORIZED, ResponseConstants.UNAUTHORIZED);
        } else if (response.getStatusLine().getStatusCode() == HttpStatus.SC_NOT_FOUND) {
            throw new RequestException(ErrorType.NOT_FOUND, ResponseConstants.NOT_FOUND);
        } else if (response.getStatusLine().getStatusCode() == HttpStatus.SC_BAD_REQUEST) {
            throw new RequestException(ErrorType.BAD_REQUEST, message);
        } else if (response.getStatusLine().getStatusCode() == HttpStatus.SC_REQUEST_TOO_LONG) {
            if (message != null
                    && message.contains(ResponseConstants.QUOTA_EXCEEDED_FOR_INSTANCES)) {
                throw new RequestException(ErrorType.QUOTA_EXCEEDED,
                        ResponseConstants.QUOTA_EXCEEDED_FOR_INSTANCES);
            }
            throw new RequestException(ErrorType.BAD_REQUEST, message);
        } else if (response.getStatusLine().getStatusCode() > 204) {
            throw new RequestException(ErrorType.BAD_REQUEST, "Status code: "
                    + response.getStatusLine().toString() + " | Message:" + message);
        }
    }

    private void addRequestHeaders(HttpMessage request, String authToken) {
        request.addHeader(RequestHeaders.CONTENT_TYPE.getValue(),
                RequestHeaders.JSON_CONTENT_TYPE.getValue());
        request.addHeader(RequestHeaders.ACCEPT.getValue(),
                RequestHeaders.JSON_CONTENT_TYPE.getValue());
        request.addHeader(RequestHeaders.X_AUTH_TOKEN.getValue(), authToken);
    }

    private void consumeResponseStream(HttpResponse response) {
        try {
            EntityUtils.consume(response.getEntity());
        } catch (IOException e) {
            String messageTemplate = "Error while trying to consume entity stream";
            LOGGER.error(messageTemplate, e);
        }
    }

    private void initClient() {
        this.client = HttpRequestUtil.createHttpClient();
    }

    protected void setClient(HttpClient client) {
        this.client = client;
    }

    protected String[] getDnsList() {
        return dnsList;
    }

}
