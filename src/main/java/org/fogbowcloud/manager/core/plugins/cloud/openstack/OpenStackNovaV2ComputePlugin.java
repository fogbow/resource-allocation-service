package org.fogbowcloud.manager.core.plugins.cloud.openstack;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.TreeSet;
import java.util.UUID;

import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpDelete;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.StringEntity;
import org.apache.http.util.EntityUtils;
import org.apache.log4j.Logger;
import org.fogbowcloud.manager.core.HomeDir;
import org.fogbowcloud.manager.core.exceptions.PropertyNotSpecifiedException;
import org.fogbowcloud.manager.core.exceptions.RequestException;
import org.fogbowcloud.manager.core.models.*;
import org.fogbowcloud.manager.core.models.quotas.allocation.ComputeAllocation;
import org.fogbowcloud.manager.core.plugins.cloud.InstanceStateMapper;
import org.fogbowcloud.manager.core.plugins.cloud.openstack.util.DefaultLaunchCommandGenerator;
import org.fogbowcloud.manager.core.plugins.cloud.openstack.util.LaunchCommandGenerator;
import org.fogbowcloud.manager.core.plugins.cloud.ComputePlugin;
import org.fogbowcloud.manager.core.models.orders.ComputeOrder;
import org.fogbowcloud.manager.core.models.instances.ComputeInstance;
import org.fogbowcloud.manager.core.models.instances.InstanceState;
import org.fogbowcloud.manager.core.models.token.Token;
import org.fogbowcloud.manager.utils.*;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

public class OpenStackNovaV2ComputePlugin implements ComputePlugin {

    private static final String NOVAV2_PLUGIN_CONF_FILE = "openstack-nova-compute-plugin.conf";

    private static final String COMPUTE_NOVAV2_URL_KEY = "openstack_nova_v2_url";
    private static final String DEFAULT_NETWORK_ID_KEY = "default_network_id";

    private static final String ID_JSON_FIELD = "id";
    private static final String NAME_JSON_FIELD = "name";
    private static final String SERVER_JSON_FIELD = "server";
    private static final String FLAVOR_REF_JSON_FIELD = "flavorRef";
    private static final String FLAVOR_JSON_FIELD = "flavor";
    private static final String FLAVOR_ID_JSON_FIELD = "id";
    private static final String IMAGE_JSON_FIELD = "imageRef";
    private static final String USER_DATA_JSON_FIELD = "user_data";
    private static final String NETWORK_JSON_FIELD = "networks";
    private static final String STATUS_JSON_FIELD = "status";
    private static final String DISK_JSON_FIELD = "disk";
    private static final String VCPU_JSON_FIELD = "vcpus";
    private static final String MEMORY_JSON_FIELD = "ram";
    private static final String FLAVOR_JSON_OBJECT = "flavor";
    private static final String FLAVOR_JSON_KEY = "flavors";
    private static final String KEY_JSON_FIELD = "key_name";
    private static final String PUBLIC_KEY_JSON_FIELD = "public_key";
    private static final String KEYPAIR_JSON_FIELD = "keypair";
    private static final String UUID_JSON_FIELD = "uuid";
    private static final String FOGBOW_INSTANCE_NAME = "fogbow-instance-";
    private static final String TENANT_ID = "tenantId";

    private static final String SERVERS = "/servers";
    private static final String SUFFIX_ENDPOINT_KEYPAIRS = "/os-keypairs";
    private static final String SUFFIX_ENDPOINT_FLAVORS = "/flavors";
    private static final String COMPUTE_V2_API_ENDPOINT = "/v2/";

    private static final Logger LOGGER = Logger.getLogger(OpenStackNovaV2ComputePlugin.class);
    private static final String ADDRESS_FIELD = "addresses";
    private static final String PROVIDER_NETWORK_FIELD = "provider";
    private static final String ADDR_FIELD = "addr";

    private TreeSet<Flavor> flavors;
    private Properties properties;
    private HttpClient client;
    private LaunchCommandGenerator launchCommandGenerator;
    private InstanceStateMapper instanceStateMapper;

    public OpenStackNovaV2ComputePlugin() {
        HomeDir homeDir = HomeDir.getInstance();
        this.properties = PropertiesUtil.
                readProperties(homeDir.getPath() + File.separator + NOVAV2_PLUGIN_CONF_FILE);
        try {
            this.launchCommandGenerator = new DefaultLaunchCommandGenerator();
        } catch (PropertyNotSpecifiedException e) {
            LOGGER.error("failed to instantiate class with properties = " + this.properties.toString(), e);
        } catch (IOException e) {
            LOGGER.error("failed to instantiate class with properties = " + this.properties.toString(), e);
        }
        instantiateOtherAttributes();
    }
    
    /** Constructor used for testing only */
    protected OpenStackNovaV2ComputePlugin(Properties properties, LaunchCommandGenerator launchCommandGenerator) {
        LOGGER.debug("Creating OpenStackNovaV2ComputePlugin with properties=" + properties.toString());
        this.properties = properties;
        this.launchCommandGenerator = launchCommandGenerator;
        instantiateOtherAttributes();
    }
    
    private void instantiateOtherAttributes() {
        this.flavors = new TreeSet<Flavor>();
        this.instanceStateMapper = new OpenStackComputeInstanceStateMapper();
        this.initClient();
    }

    public String requestInstance(ComputeOrder computeOrder, Token localToken)
            throws RequestException {
        LOGGER.debug("Requesting instance with token=" + localToken);

        Flavor flavor = findSmallestFlavor(computeOrder, localToken);
        String flavorId = flavor.getId();

        String tenantId = getTenantId(localToken);

        List<String> networksId = getNetworksId(computeOrder);
        
        String imageId = computeOrder.getImageId();

        String userData = this.launchCommandGenerator.createLaunchCommand(computeOrder);

        String keyName = getKeyName(tenantId, localToken, computeOrder.getPublicKey());

        String endpoint = getComputeEndpoint(tenantId, SERVERS);

        try {
            JSONObject json = generateJsonRequest(imageId, flavorId, userData, keyName, networksId);
            String jsonResponse = doPostRequest(endpoint, localToken, json);

            String instanceId = getAttFromJson(ID_JSON_FIELD, jsonResponse);

            synchronized (computeOrder) {
                ComputeAllocation actualAllocation = new ComputeAllocation(flavor.getCpu(), flavor.getRam(), 1);
                computeOrder.setActualAllocation(actualAllocation);
            }
            return instanceId;
        } catch (JSONException e) {
            LOGGER.error("Invalid JSON key: " + e);
            throw new RequestException(ErrorType.BAD_REQUEST, ResponseConstants.IRREGULAR_SYNTAX);
        } finally {
            if (keyName != null) {
                try {
                    deleteKeyName(tenantId, localToken, keyName);
                } catch (Throwable t) {
                    LOGGER.warn("Could not delete key", t);
                }
            }
        }
    }

    private void initClient() {
        HttpRequestUtil.init();
        this.client = HttpRequestUtil.createHttpClient();
    }

    protected String getTenantId(Token localToken) {
        Map<String, String> tokenAttr = localToken.getAttributes();
        return tokenAttr.get(TENANT_ID);
    }

    protected List<String> getNetworksId(ComputeOrder computeOrder) {
        List<String> networksId = computeOrder.getNetworksId();
        String id = this.properties.getProperty(DEFAULT_NETWORK_ID_KEY);
        if (networksId == null) {
            networksId = new ArrayList<String>();            
            networksId.add(id);
        } else if (networksId.isEmpty()) {
            networksId.add(id);
        }
        return networksId;
    }

    protected String getKeyName(String tenantId, Token localToken, String publicKey)
            throws RequestException {
        String keyname = null;

        if (publicKey != null && !publicKey.isEmpty()) {
            String osKeypairEndpoint = getComputeEndpoint(tenantId, SUFFIX_ENDPOINT_KEYPAIRS);

            keyname = UUID.randomUUID().toString();
            JSONObject keypair = new JSONObject();

            try {
                keypair.put(NAME_JSON_FIELD, keyname);
                keypair.put(PUBLIC_KEY_JSON_FIELD, publicKey);
                JSONObject root = new JSONObject();
                root.put(KEYPAIR_JSON_FIELD, keypair);
                doPostRequest(osKeypairEndpoint, localToken, root);
            } catch (JSONException e) {
                LOGGER.error("Error while getting key name: " + e);
                throw new RequestException(
                        ErrorType.BAD_REQUEST, ResponseConstants.IRREGULAR_SYNTAX);
            }
        }

        return keyname;
    }

    protected String getComputeEndpoint(String tenantId, String suffix) {
        return this.properties.getProperty(COMPUTE_NOVAV2_URL_KEY)
                + COMPUTE_V2_API_ENDPOINT
                + tenantId
                + suffix;
    }

    private String getAttFromJson(String attName, String jsonStr) throws JSONException {
        JSONObject root = new JSONObject(jsonStr);
        JSONObject serveJson = root.getJSONObject(SERVER_JSON_FIELD);
        String jsonAttValue = serveJson.getString(attName);
        return jsonAttValue;
    }

    protected void deleteKeyName(String tenantId, Token localToken, String keyName)
            throws RequestException {
        String suffixEndpoint = SUFFIX_ENDPOINT_KEYPAIRS + "/" + keyName;
        String keynameEndpoint = getComputeEndpoint(tenantId, suffixEndpoint);

        doDeleteRequest(keynameEndpoint, localToken);
    }

    private void doDeleteRequest(String endpoint, Token localToken) throws RequestException {
        LOGGER.debug("Doing DELETE request to OpenStack on endpoint <" + endpoint + ">");

        HttpResponse response = null;

        try {
            HttpDelete request = new HttpDelete(endpoint);
            request.addHeader(RequestHeaders.X_AUTH_TOKEN.getValue(), localToken.getAccessId());
            response = this.client.execute(request);
        } catch (Exception e) {
            LOGGER.error("Unable to complete the DELETE request: ", e);
            throw new RequestException(ErrorType.BAD_REQUEST, ResponseConstants.IRREGULAR_SYNTAX);
        } finally {
            try {
                EntityUtils.consume(response.getEntity());
            } catch (Throwable t) {
                LOGGER.error("Error while consuming the response: ", t);
            }
        }

        /** Delete message does not have message */
        checkStatusResponse(response, "");
    }

    protected String doPostRequest(String endpoint, Token localToken, JSONObject jsonRequest)
            throws RequestException {
        LOGGER.debug("Doing POST request to OpenStack for creating an instance");

        HttpResponse response = null;
        String responseStr;

        try {
            HttpPost request = new HttpPost(endpoint);
            request.addHeader(
                    RequestHeaders.CONTENT_TYPE.getValue(),
                    RequestHeaders.JSON_CONTENT_TYPE.getValue());
            request.addHeader(
                    RequestHeaders.ACCEPT.getValue(), RequestHeaders.JSON_CONTENT_TYPE.getValue());
            request.addHeader(RequestHeaders.X_AUTH_TOKEN.getValue(), localToken.getAccessId());

            request.setEntity(new StringEntity(jsonRequest.toString(), StandardCharsets.UTF_8));
            response = this.client.execute(request);
            responseStr = EntityUtils.toString(response.getEntity(), StandardCharsets.UTF_8);
        } catch (Exception e) {
            LOGGER.error("Impossible to complete the POST request: " + e);
            throw new RequestException(ErrorType.BAD_REQUEST, ResponseConstants.IRREGULAR_SYNTAX);
        } finally {
            try {
                EntityUtils.consume(response.getEntity());
            } catch (Throwable t) {
                LOGGER.error("Error while consuming the response: " + t);
            }
        }

        checkStatusResponse(response, responseStr);

        return responseStr;
    }

    protected JSONObject generateJsonRequest(
            String imageRef, String flavorRef, String userdata, String keyName, List<String> networksId)
            throws JSONException {
        LOGGER.debug("Generating JSON to send as the body of instance POST request");

        JSONObject server = new JSONObject();
        server.put(NAME_JSON_FIELD, FOGBOW_INSTANCE_NAME + UUID.randomUUID().toString());
        server.put(IMAGE_JSON_FIELD, imageRef);
        server.put(FLAVOR_REF_JSON_FIELD, flavorRef);

        if (userdata != null) {
            server.put(USER_DATA_JSON_FIELD, userdata);
        }

        if (networksId != null && !networksId.isEmpty()) {
            JSONArray networks = new JSONArray();
                       
            for (String id : networksId) {
                JSONObject netId = new JSONObject();
                netId.put(UUID_JSON_FIELD, id);
                networks.put(netId);
            }
          
            server.put(NETWORK_JSON_FIELD, networks);
        }

        if (keyName != null && !keyName.isEmpty()) {
            server.put(KEY_JSON_FIELD, keyName);
        }

        JSONObject root = new JSONObject();
        root.put(SERVER_JSON_FIELD, server);

        return root;
    }

    private void checkStatusResponse(HttpResponse response, String message)
            throws RequestException {
        LOGGER.debug("Checking status response...");

        StatusResponseMap statusResponseMap = new StatusResponseMap(response, message);
        Integer statusCode = response.getStatusLine().getStatusCode();
        StatusResponse statusResponse = statusResponseMap.getStatusResponse(statusCode);

        if (statusResponse != null) {
            throw new RequestException(
                    statusResponse.getErrorType(), statusResponse.getResponseConstants());
        }
    }

    protected Flavor findSmallestFlavor(ComputeOrder computeOrder, Token localToken) {
        updateFlavors(localToken);

        LOGGER.debug("Finding smallest flavor");

        Flavor minimumFlavor =
                new Flavor(
                        null,
                        computeOrder.getvCPU(),
                        computeOrder.getMemory(),
                        computeOrder.getDisk());

        return this.flavors.ceiling(minimumFlavor);
    }

    protected synchronized void updateFlavors(Token localToken) {
        LOGGER.debug("Updating flavors from OpenStack");

        try {
            String tenantId = getTenantId(localToken);
            if (tenantId == null) {
                return;
            }

            String endpoint = getComputeEndpoint(tenantId, SUFFIX_ENDPOINT_FLAVORS);
            String jsonResponseFlavors = doGetRequest(endpoint, localToken);

            List<String> flavorsId = new ArrayList<>();

            JSONArray jsonArrayFlavors =
                    new JSONObject(jsonResponseFlavors).getJSONArray(FLAVOR_JSON_KEY);

            for (int i = 0; i < jsonArrayFlavors.length(); i++) {
                JSONObject itemFlavor = jsonArrayFlavors.getJSONObject(i);
                flavorsId.add(itemFlavor.getString(ID_JSON_FIELD));
            }

            TreeSet<Flavor> newFlavors = detailFlavors(endpoint, localToken, flavorsId);
            if (newFlavors != null) {
                this.flavors = newFlavors;
            }

        } catch (Exception e) {
            LOGGER.warn("Error while updating flavors", e);
        }
    }

    private TreeSet<Flavor> detailFlavors(String endpoint, Token localToken, List<String> flavorsId)
            throws JSONException, RequestException {
        TreeSet<Flavor> newFlavors = new TreeSet<>();
        TreeSet<Flavor> flavorsCopy = new TreeSet<>(this.flavors);

        for (String flavorId : flavorsId) {
            boolean containsFlavor = false;

            for (Flavor flavor : flavorsCopy) {
                if (flavor.getId().equals(flavorId)) {
                    containsFlavor = true;
                    newFlavors.add(flavor);
                    break;
                }
            }
            if (!containsFlavor) {
                String newEndpoint = endpoint + "/" + flavorId;
                String jsonResponseSpecificFlavor = doGetRequest(newEndpoint, localToken);

                JSONObject specificFlavor =
                        new JSONObject(jsonResponseSpecificFlavor)
                                .getJSONObject(FLAVOR_JSON_OBJECT);

                String id = specificFlavor.getString(ID_JSON_FIELD);
                String name = specificFlavor.getString(NAME_JSON_FIELD);
                int disk = specificFlavor.getInt(DISK_JSON_FIELD);
                int ram = specificFlavor.getInt(MEMORY_JSON_FIELD);
                int vcpus = specificFlavor.getInt(VCPU_JSON_FIELD);

                newFlavors.add(new Flavor(name, id, vcpus, ram, disk));
            }
        }

        return newFlavors;
    }

    protected String doGetRequest(String endpoint, Token localToken) throws RequestException {
        LOGGER.debug("Doing GET request to OpenStack on endpoint <" + endpoint + ">");

        HttpResponse response = null;
        String responseStr;

        try {
            HttpGet request = new HttpGet(endpoint);
            request.addHeader(
                    RequestHeaders.CONTENT_TYPE.getValue(),
                    RequestHeaders.JSON_CONTENT_TYPE.getValue());
            request.addHeader(
                    RequestHeaders.ACCEPT.getValue(), RequestHeaders.JSON_CONTENT_TYPE.getValue());
            request.addHeader(RequestHeaders.X_AUTH_TOKEN.getValue(), localToken.getAccessId());

            response = this.client.execute(request);
            responseStr = EntityUtils.toString(response.getEntity(), StandardCharsets.UTF_8);
        } catch (Exception e) {
            LOGGER.error("Could not make GET request.", e);
            throw new RequestException(ErrorType.BAD_REQUEST, ResponseConstants.IRREGULAR_SYNTAX);
        } finally {
            try {
                EntityUtils.consume(response.getEntity());
            } catch (Throwable t) {
                LOGGER.error("Error while consuming the response: " + t);
            }
        }

        checkStatusResponse(response, responseStr);

        return responseStr;
    }

    @Override
    public ComputeInstance getInstance(String instanceId, String orderId, Token localToken)
            throws RequestException {
        LOGGER.info("Getting instance " + instanceId + " with token " + localToken);

        String tenantId = getTenantId(localToken);
        String requestEndpoint = getComputeEndpoint(tenantId, SERVERS + "/" + instanceId);

        String jsonResponse = doGetRequest(requestEndpoint, localToken);

        LOGGER.debug("Getting instance from json: " + jsonResponse);
        ComputeInstance computeInstance = getInstanceFromJson(jsonResponse);
        addReverseTunnelInfo(orderId, computeInstance);

        return computeInstance;
    }

    private void addReverseTunnelInfo(String orderId, ComputeInstance computeInstance) {
        TunnelingServiceUtil tunnelingServiceUtil = TunnelingServiceUtil.getInstance();
        SshConnectivityUtil sshConnectivityUtil = SshConnectivityUtil.getInstance();

        ComputeInstanceConnectivityUtil computeInstanceConnectivity =
                new ComputeInstanceConnectivityUtil(tunnelingServiceUtil, sshConnectivityUtil);

        SshTunnelConnectionData sshTunnelConnectionData = computeInstanceConnectivity
                .getSshTunnelConnectionData(orderId);

        computeInstance.setSshTunnelConnectionData(sshTunnelConnectionData);
    }

    private ComputeInstance getInstanceFromJson(String jsonResponse) throws RequestException {
        try {
            JSONObject rootServer = new JSONObject(jsonResponse);
            JSONObject serverJson = rootServer.getJSONObject(SERVER_JSON_FIELD);

            String id = serverJson.getString(ID_JSON_FIELD);
            String hostName = serverJson.getString(NAME_JSON_FIELD);
            String localIpAddress = "";

            if (!serverJson.isNull(ADDRESS_FIELD)) {
                JSONObject addressField = serverJson.optJSONObject(ADDRESS_FIELD);
                if (!addressField.isNull(PROVIDER_NETWORK_FIELD)) {
                    JSONArray providerNetworkArray = addressField.optJSONArray(PROVIDER_NETWORK_FIELD);
                    JSONObject providerNetwork = (JSONObject) providerNetworkArray.get(0);
                    localIpAddress = providerNetwork.optString(ADDR_FIELD);
                }
            }

            int vCPU = -1;
            int memory = -1;
            // TODO: Do we want to ignore the disk attribute?
//            int disk = flavor.getDisk();

            Flavor flavor = retrieveFlavorFromResponse(serverJson);
            if (flavor != null) {
                vCPU = flavor.getCpu();
                memory = flavor.getRam();
            }

            InstanceState state = this.instanceStateMapper.getInstanceState(serverJson.getString(STATUS_JSON_FIELD));

            ComputeInstance computeInstance =
                    new ComputeInstance(
                            id,
                            hostName,
                            vCPU,
                            memory,
                            state,
                            localIpAddress);
            return computeInstance;
        } catch (JSONException e) {
            LOGGER.warn("There was an exception while getting instances from json", e);
            throw new RequestException();
        }
    }

    private Flavor retrieveFlavorFromResponse(JSONObject jsonResponse) {
        Flavor flavor = null;
        if (!jsonResponse.isNull(FLAVOR_JSON_FIELD)) {
            JSONObject flavorField = jsonResponse.optJSONObject(FLAVOR_JSON_FIELD);
            String flavorId = flavorField.optString(FLAVOR_ID_JSON_FIELD);
            flavor = getFlavorById(flavorId);
        }
        return flavor;
    }

    private Flavor getFlavorById(String id) {
        TreeSet<Flavor> flavorsCopy = new TreeSet<>(this.flavors);
        for (Flavor flavor : flavorsCopy) {
            if (flavor.getId().equals(id)) {
                return flavor;
            }
        }
        return null;
    }

    @Override
    public void deleteInstance(String instanceId, Token localToken) throws RequestException {
        if (instanceId == null) {
            throw new RequestException();
        }
        String endpoint =
                getComputeEndpoint(getTenantId(localToken), SERVERS + "/" + instanceId);

        doDeleteRequest(endpoint, localToken);
    }

}
