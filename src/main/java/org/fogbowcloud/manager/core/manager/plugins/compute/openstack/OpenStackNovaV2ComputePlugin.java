package org.fogbowcloud.manager.core.manager.plugins.compute.openstack;

import java.nio.charset.StandardCharsets;
import java.util.*;
import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpDelete;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.StringEntity;
import org.apache.http.util.EntityUtils;
import org.apache.log4j.Logger;
import org.fogbowcloud.manager.core.exceptions.RequestException;
import org.fogbowcloud.manager.core.manager.plugins.compute.ComputePlugin;
import org.fogbowcloud.manager.core.manager.plugins.compute.DefaultLaunchCommandGenerator;
import org.fogbowcloud.manager.core.manager.plugins.compute.LaunchCommandGenerator;
import org.fogbowcloud.manager.core.models.*;
import org.fogbowcloud.manager.core.models.orders.ComputeOrder;
import org.fogbowcloud.manager.core.models.orders.instances.ComputeOrderInstance;
import org.fogbowcloud.manager.core.models.orders.instances.InstanceState;
import org.fogbowcloud.manager.core.models.orders.instances.OrderInstance;
import org.fogbowcloud.manager.core.models.token.Token;
import org.fogbowcloud.manager.utils.HttpRequestUtil;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

public class OpenStackNovaV2ComputePlugin implements ComputePlugin {

    private static final String ID_JSON_FIELD = "id";
    private static final String NAME_JSON_FIELD = "name";
    private static final String SERVER_JSON_FIELD = "server";
    private static final String FLAVOR_JSON_FIELD = "flavorRef";
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
    private static final String ACTIVE_STATUS = "active";
    private static final String BUILD_STATUS = "build";
    private static final String SERVERS = "/servers";
    private static final String SUFFIX_ENDPOINT_KEYPAIRS = "/os-keypairs";
    private static final String SUFFIX_ENDPOINT_FLAVORS = "/flavors";
    private static final String COMPUTE_NOVAV2_URL_KEY = "compute_novav2_url";
    private final String COMPUTE_V2_API_ENDPOINT = "/v2.1/";

    private static final String COMPUTE_NOVAV2_NETWORK_KEY = "compute_novav2_network_id";

    private static final Logger LOGGER = Logger.getLogger(OpenStackNovaV2ComputePlugin.class);

    private TreeSet<Flavor> flavors;
    private Properties properties;
    private HttpClient client;
    private LaunchCommandGenerator launchCommandGenerator;

    public OpenStackNovaV2ComputePlugin(Properties properties) throws Exception {
        this(properties, new DefaultLaunchCommandGenerator(properties));
    }

    protected OpenStackNovaV2ComputePlugin(
            Properties properties, LaunchCommandGenerator launchCommandGenerator) throws Exception {
        LOGGER.debug(
                "Creating OpenStackNovaV2ComputePlugin with properties=" + properties.toString());

        this.flavors = new TreeSet<>();
        this.properties = properties;
        this.launchCommandGenerator = launchCommandGenerator;
        this.initClient();
    }

    public String requestInstance(ComputeOrder computeOrder, Token localToken, String imageId)
            throws RequestException {
        LOGGER.debug("Requesting instance with token=" + localToken);

        Flavor flavor = findSmallestFlavor(computeOrder, localToken);
        String flavorId = flavor.getId();

        String tenantId = getTenantId(localToken);

        String networkId = getNetworkId();

        String userData = this.launchCommandGenerator.createLaunchCommand(computeOrder);

        String keyName = getKeyName(tenantId, localToken, computeOrder.getPublicKey());

        String endpoint = getComputeEndpoint(tenantId, SERVERS);

        try {
            JSONObject json = generateJsonRequest(imageId, flavorId, userData, keyName, networkId);
            String jsonResponse = doPostRequest(endpoint, localToken, json);

            String instanceId = getAttFromJson(ID_JSON_FIELD, jsonResponse);
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
        HttpRequestUtil.init(this.properties);
        this.client = HttpRequestUtil.createHttpClient();
    }

    protected String getTenantId(Token localToken) {
        Map<String, String> tokenAttr = localToken.getAttributes();
        return tokenAttr.get(TENANT_ID);
    }

    protected String getNetworkId() {
        return this.properties.getProperty(COMPUTE_NOVAV2_NETWORK_KEY);
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

        // delete message does not have message
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
            String imageRef, String flavorRef, String userdata, String keyName, String networkId)
            throws JSONException {
        LOGGER.debug("Generating JSON to send as the body of instance POST request");

        JSONObject server = new JSONObject();
        server.put(NAME_JSON_FIELD, FOGBOW_INSTANCE_NAME + UUID.randomUUID().toString());
        server.put(IMAGE_JSON_FIELD, imageRef);
        server.put(FLAVOR_JSON_FIELD, flavorRef);

        if (userdata != null) {
            server.put(USER_DATA_JSON_FIELD, userdata);
        }

        if (networkId != null && !networkId.isEmpty()) {
            List<JSONObject> networks = new ArrayList<>();
            JSONObject net = new JSONObject();
            net.put(UUID_JSON_FIELD, networkId);
            networks.add(net);
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
        LOGGER.debug("Doing DELETE request to OpenStack on endpoint <" + endpoint + ">");

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
    public ComputeOrderInstance getInstance(Token localToken, String instanceId)
            throws RequestException {
        LOGGER.info("Getting instance " + instanceId + " with token " + localToken);

        String tenantId = getTenantId(localToken);
        String requestEndpoint = getComputeEndpoint(tenantId, SERVERS + "/" + instanceId);

        String jsonResponse = doGetRequest(requestEndpoint, localToken);

        LOGGER.debug("Getting instance from json: " + jsonResponse);
        ComputeOrderInstance computeOrderInstance = getInstanceFromJson(jsonResponse);

        return computeOrderInstance;
    }

    private ComputeOrderInstance getInstanceFromJson(String jsonResponse) throws RequestException {
        try {
            JSONObject rootServer = new JSONObject(jsonResponse);
            JSONObject serverJson = rootServer.getJSONObject(SERVER_JSON_FIELD);

            String id = serverJson.getString(ID_JSON_FIELD);
            String hostName = serverJson.getString(NAME_JSON_FIELD);
            int vCPU = serverJson.getJSONObject(FLAVOR_JSON_OBJECT).getInt(VCPU_JSON_FIELD);
            int memory = serverJson.getJSONObject(FLAVOR_JSON_OBJECT).getInt(MEMORY_JSON_FIELD);
            InstanceState state = getInstanceState(serverJson.getString(STATUS_JSON_FIELD));

            // TODO: Why should I pass all this attributes for computeOrderInstance if they are
            // all related to tunneling? We don't have it on the cloud provider JSON response.
            String localIpAddress = "";
            String sshPublicAddress = "";
            String sshUserName = "";
            String sshExtraPorts = "";

            ComputeOrderInstance computeOrderInstance =
                    new ComputeOrderInstance(
                            id,
                            hostName,
                            vCPU,
                            memory,
                            state,
                            localIpAddress,
                            sshPublicAddress,
                            sshUserName,
                            sshExtraPorts);

            return computeOrderInstance;
        } catch (JSONException e) {
            LOGGER.warn("There was an exception while getting instances from json", e);
            throw new RequestException();
        }
    }

    /**
     * This method will map Openstack instance status to Fogbow instance status.
     *
     * @param instanceStatus status from JSON.
     * @return {@link InstanceState}
     */
    private InstanceState getInstanceState(String instanceStatus) {
        switch (instanceStatus.toLowerCase()) {
            case ACTIVE_STATUS:
                return InstanceState.ACTIVE;

            case BUILD_STATUS:
                return InstanceState.INACTIVE;

            default:
                return InstanceState.FAILED;
        }
    }

    // TODO: Is it necessary?
    @Override
    public List<ComputeOrderInstance> getInstances(Token localToken) {
        return null;
    }

    @Override
    public void deleteInstance(Token localToken, OrderInstance instance) throws RequestException {
        if (instance.getId() == null) {
            throw new RequestException();
        }
        String endpoint =
                getComputeEndpoint(getTenantId(localToken), SERVERS + "/" + instance.getId());

        doDeleteRequest(endpoint, localToken);
    }

    @Override
    public void deleteInstances(Token localToken) {}

    @Override
    public String attachStorage(Token localToken, VolumeLink storageLink) {
        return null;
    }

    @Override
    public String detachStorage(Token localToken, VolumeLink storageLink) {
        return null;
    }

    @Override
    public String getImageId(Token localToken, String imageName) {
        return null;
    }
}
