package org.fogbowcloud.manager.core.plugins.compute.openstack;

import org.apache.http.HttpResponse;
import org.apache.http.HttpStatus;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpDelete;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.StringEntity;
import org.apache.http.util.EntityUtils;
import org.apache.log4j.Logger;
import org.fogbowcloud.manager.core.models.*;
import org.fogbowcloud.manager.core.models.exceptions.RequestException;
import org.fogbowcloud.manager.core.models.orders.ComputeOrder;
import org.fogbowcloud.manager.core.models.orders.instances.ComputeOrderInstance;
import org.fogbowcloud.manager.core.models.token.Token;
import org.fogbowcloud.manager.core.plugins.compute.ComputePlugin;
import org.fogbowcloud.manager.core.utils.HttpRequestUtil;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.nio.charset.StandardCharsets;
import java.util.*;

public class OpenStackNovaV2ComputePlugin implements ComputePlugin {

    private static final String ID_JSON_FIELD = "id";
    private static final String NAME_JSON_FIELD = "name";
    private static final String SERVER_JSON_FIELD = "server";
    private static final String FLAVOR_JSON_FIELD = "flavorRef";
    private static final String IMAGE_JSON_FIELD = "imageRef";
    private static final String USER_DATA_JSON_FIELD = "user_data";
    private static final String NETWORK_JSON_FIELD = "networks";
    private static final String DISK_JSON_FIELD = "disk";
    private static final String VCPU_JSON_FIELD = "vcpus";
    private static final String MEMORY_JSON_FIELD = "ram";
    private static final String FLAVOR_JSON_OBJECT = "flavor";
    private static final String KEY_JSON_FIELD = "key_name";
    private static final String PUBLIC_KEY_JSON_FIELD = "public_key";
    private static final String KEYPAIR_JSON_FIELD = "keypair";
    private static final String UUID_JSON_FIELD = "uuid";
    private static final String FOGBOW_INSTANCE_NAME = "fogbow-instance-";

    private static final String TENANT_ID = "tenantId";

    private static final String SERVERS = "/servers";
    private static final String SUFFIX_ENDPOINT_KEYPAIRS = "/os-keypairs";
    private static final String SUFFIX_ENDPOINT_FLAVORS = "/flavors";
    private static final String COMPUTE_NOVAV2_URL_KEY = "compute_novav2_url";
    private static final String NO_VALID_HOST_WAS_FOUND = "No valid host was found";
    private final String COMPUTE_V2_API_ENDPOINT = "/v2/";

    private static final String COMPUTE_NOVAV2_NETWORK_KEY = "compute_novav2_network_id";

    private static final Logger LOGGER = Logger.getLogger(OpenStackNovaV2ComputePlugin.class);

    private HttpClient client;
    private List<Flavor> flavors;

    private Properties properties;

    public OpenStackNovaV2ComputePlugin(Properties properties) {
        this.client = HttpRequestUtil.createHttpClient(60000, null, null);
        this.flavors = new ArrayList<>();
        this.properties = properties;
    }

    public String requestInstance(ComputeOrder computeOrder, String imageId) throws RequestException {
        Token localToken = computeOrder.getLocalToken();
        String flavorId = getFlavor(computeOrder).getId();
        String tenantId = getTenantId(localToken);
        String networkId = getNetworkId();
        String userData = computeOrder.getUserData().getContent();
        String keyName = getKeyName(tenantId, localToken, computeOrder.getPublicKey());
        String endpoint = getComputeEndpoint(tenantId, SERVERS);

        try {
            JSONObject json = generateJsonRequest(imageId, flavorId, userData, keyName, networkId);
            String jsonResponse = doPostRequest(endpoint, localToken, json);

            return getAttFromJson(ID_JSON_FIELD, jsonResponse);
        } catch (JSONException e) {
            LOGGER.error(e);
            throw new RequestException(ErrorType.BAD_REQUEST, ResponseConstants.IRREGULAR_SYNTAX);
        } finally {
            if (keyName != null) {
                try {
                    deleteKeyName(tenantId, localToken, keyName);
                } catch (Throwable t) {
                    LOGGER.warn("Could not delete key.", t);
                }
            }
        }
    }

    protected String getTenantId(Token localToken) {
        return localToken.getAttributes().get(TENANT_ID);
    }

    protected String getNetworkId() {
        return properties.getProperty(COMPUTE_NOVAV2_NETWORK_KEY);
    }

    protected String getKeyName(String tenantId, Token localToken, String publicKey) throws RequestException {
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
                LOGGER.error(e);
                throw new RequestException(ErrorType.BAD_REQUEST, ResponseConstants.IRREGULAR_SYNTAX);
            }
        }

        return keyname;
    }

    protected String getComputeEndpoint(String tenantId, String suffix) {
        return properties.getProperty(COMPUTE_NOVAV2_URL_KEY) + COMPUTE_V2_API_ENDPOINT + tenantId + suffix;
    }

    private String getAttFromJson(String attName, String jsonStr) throws JSONException {
        JSONObject root = new JSONObject(jsonStr);
        return root.getJSONObject(SERVER_JSON_FIELD).getString(attName);
    }

    protected void deleteKeyName(String tenantId, Token localToken, String keyName) throws RequestException {
        String suffixEndpoint = SUFFIX_ENDPOINT_KEYPAIRS + "/" + keyName;
        String keynameEndpoint = getComputeEndpoint(tenantId, suffixEndpoint);

        doDeleteRequest(keynameEndpoint, localToken);
    }

    private void doDeleteRequest(String endpoint, Token localToken) throws RequestException {
        HttpResponse response = null;

        try {
            HttpDelete request = new HttpDelete(endpoint);
            request.addHeader(RequestHeaders.X_AUTH_TOKEN.getValue(), localToken.getAccessId());
            response = client.execute(request);
        } catch (Exception e) {
            LOGGER.error(e);
            throw new RequestException(ErrorType.BAD_REQUEST, ResponseConstants.IRREGULAR_SYNTAX);
        } finally {
            try {
                EntityUtils.consume(response.getEntity());
            } catch (Throwable t) {
                // Do nothing
            }
        }

        // delete message does not have message
        checkStatusResponse(response, "");
    }

    protected String doPostRequest(String endpoint, Token localToken, JSONObject jsonRequest) throws RequestException {
        HttpResponse response = null;
        String responseStr;

        try {
            HttpPost request = new HttpPost(endpoint);
            request.addHeader(RequestHeaders.CONTENT_TYPE.getValue(), RequestHeaders.JSON_CONTENT_TYPE.getValue());
            request.addHeader(RequestHeaders.ACCEPT.getValue(), RequestHeaders.JSON_CONTENT_TYPE.getValue());
            request.addHeader(RequestHeaders.X_AUTH_TOKEN.getValue(), localToken.getAccessId());

            request.setEntity(new StringEntity(jsonRequest.toString(), StandardCharsets.UTF_8));
            response = client.execute(request);
            responseStr = EntityUtils.toString(response.getEntity(), StandardCharsets.UTF_8);
        } catch (Exception e) {
            LOGGER.error(e);
            throw new RequestException(ErrorType.BAD_REQUEST, ResponseConstants.IRREGULAR_SYNTAX);
        } finally {
            try {
                EntityUtils.consume(response.getEntity());
            } catch (Throwable t) {
                // Do nothing
            }
        }

        checkStatusResponse(response, responseStr);

        return responseStr;
    }

    protected JSONObject generateJsonRequest(String imageRef, String flavorRef, String userdata,
                                           String keyName, String networkId) throws JSONException {

        JSONObject server = new JSONObject();
        server.put(NAME_JSON_FIELD, FOGBOW_INSTANCE_NAME + UUID.randomUUID().toString());
        server.put(IMAGE_JSON_FIELD, imageRef);
        server.put(FLAVOR_JSON_FIELD, flavorRef);

        if (userdata != null) {
            server.put(USER_DATA_JSON_FIELD, userdata);
        }

        if (networkId != null && !networkId.isEmpty()) {
            List<JSONObject> nets = new ArrayList<>();
            JSONObject net = new JSONObject();
            net.put(UUID_JSON_FIELD, networkId);
            nets.add(net);
            server.put(NETWORK_JSON_FIELD, nets);
        }

        if (keyName != null && !keyName.isEmpty()) {
            server.put(KEY_JSON_FIELD, keyName);
        }

        JSONObject root = new JSONObject();
        root.put(SERVER_JSON_FIELD, server);

        return root;
    }

    private void checkStatusResponse(HttpResponse response, String message) throws RequestException {
        if (response.getStatusLine().getStatusCode() == HttpStatus.SC_UNAUTHORIZED) {
            throw new RequestException(ErrorType.UNAUTHORIZED, ResponseConstants.UNAUTHORIZED);
        } else if (response.getStatusLine().getStatusCode() == HttpStatus.SC_NOT_FOUND) {
            throw new RequestException(ErrorType.NOT_FOUND, ResponseConstants.NOT_FOUND);
        } else if (response.getStatusLine().getStatusCode() == HttpStatus.SC_BAD_REQUEST) {
            throw new RequestException(ErrorType.BAD_REQUEST, message);
        } else if (response.getStatusLine().getStatusCode() == HttpStatus.SC_REQUEST_TOO_LONG
                || response.getStatusLine().getStatusCode() == HttpStatus.SC_FORBIDDEN) {
            if (message.contains(ResponseConstants.QUOTA_EXCEEDED_FOR_INSTANCES)) {
                throw new RequestException(ErrorType.QUOTA_EXCEEDED,
                        ResponseConstants.QUOTA_EXCEEDED_FOR_INSTANCES);
            }
            throw new RequestException(ErrorType.BAD_REQUEST, message);
        } else if ((response.getStatusLine().getStatusCode() == HttpStatus.SC_INTERNAL_SERVER_ERROR) &&
                (message.contains(NO_VALID_HOST_WAS_FOUND))){
            throw new RequestException(ErrorType.NO_VALID_HOST_FOUND, ResponseConstants.NO_VALID_HOST_FOUND);
        }
        else if (response.getStatusLine().getStatusCode() > 204) {
            throw new RequestException(ErrorType.BAD_REQUEST,
                    "Status code: " + response.getStatusLine().toString() + " | Message:" + message);
        }
    }

    protected Flavor getFlavor(ComputeOrder computeOrder) {
        updateFlavors(computeOrder.getLocalToken());

        return findSmallestFlavor(computeOrder);
    }

    private Flavor findSmallestFlavor(ComputeOrder computeOrder) {
        List<Flavor> listFlavor = new ArrayList<>();
        for (Flavor flavor : flavors) {
            if (matches(flavor, computeOrder)) {
                listFlavor.add(flavor);
            }
        }

        if (listFlavor.isEmpty()) {
            return null;
        }

        Collections.sort(listFlavor);

        return listFlavor.get(0);
    }

    private boolean matches(Flavor flavor, ComputeOrder computeOrder) {
        if (flavor.getDisk() < computeOrder.getDisk()
                || flavor.getCpu() < computeOrder.getvCPU()
                || flavor.getMem() < computeOrder.getMemory()) {
            return false;
        }

        return true;
    }

    protected synchronized void updateFlavors(Token localToken) {
        try {
            String tenantId = getTenantId(localToken);
            if (tenantId == null) {
                return;
            }

            String endpoint = getComputeEndpoint(tenantId, SUFFIX_ENDPOINT_FLAVORS);
            String jsonResponseFlavors = doGetRequest(endpoint, localToken);

            Map<String, String> nameToFlavorId = new HashMap<>();

            JSONArray jsonArrayFlavors = new JSONObject(jsonResponseFlavors).getJSONArray("flavors");

            for (int i = 0; i < jsonArrayFlavors.length(); i++) {
                JSONObject itemFlavor = jsonArrayFlavors.getJSONObject(i);
                nameToFlavorId.put(itemFlavor.getString(NAME_JSON_FIELD), itemFlavor.getString(ID_JSON_FIELD));
            }

            List<Flavor> newFlavors = detailFlavors(endpoint, localToken, nameToFlavorId);
            if (newFlavors != null) {
                this.flavors.addAll(newFlavors);
            }

            removeInvalidFlavors(nameToFlavorId);

        } catch (Exception e) {
            LOGGER.warn("Error while updating flavors.", e);
        }
    }

    private List<Flavor> detailFlavors(String endpoint, Token localToken,
                                       Map<String, String> nameToIdFlavor) throws JSONException, RequestException {
        List<Flavor> newFlavors = new ArrayList<>();
        List<Flavor> flavorsCopy = new ArrayList<>(flavors);

        for (String flavorName : nameToIdFlavor.keySet()) {
            boolean containsFlavor = false;
            for (Flavor flavor : flavorsCopy) {
                if (flavor.getName().equals(flavorName)) {
                    containsFlavor = true;
                    break;
                }
            }
            if (containsFlavor) {
                continue;
            }

            String newEndpoint = endpoint + "/" + nameToIdFlavor.get(flavorName);
            String jsonResponseSpecificFlavor = doGetRequest(newEndpoint, localToken);

            JSONObject specificFlavor = new JSONObject(jsonResponseSpecificFlavor).getJSONObject(FLAVOR_JSON_OBJECT);

            String id = specificFlavor.getString(ID_JSON_FIELD);
            String name = specificFlavor.getString(NAME_JSON_FIELD);
            int disk = specificFlavor.getInt(DISK_JSON_FIELD);
            int ram = specificFlavor.getInt(MEMORY_JSON_FIELD);
            int vcpus = specificFlavor.getInt(VCPU_JSON_FIELD);

            newFlavors.add(new Flavor(name, id, vcpus, ram, disk));
        }

        return newFlavors;
    }

    private void removeInvalidFlavors(Map<String, String> nameToIdFlavor) {
        ArrayList<Flavor> copyFlavors = new ArrayList<>(flavors);

        for (Flavor flavor : copyFlavors) {
            boolean containsFlavor = false;
            for (String flavorName : nameToIdFlavor.keySet()) {
                if (flavorName.equals(flavor.getName())) {
                    containsFlavor = true;
                }
            }
            if (!containsFlavor) {
                this.flavors.remove(flavor);
            }
        }
    }

    protected String doGetRequest(String endpoint, Token localToken) throws RequestException {
        HttpResponse response = null;
        String responseStr;

        try {
            HttpGet request = new HttpGet(endpoint);
            request.addHeader(RequestHeaders.CONTENT_TYPE.getValue(), RequestHeaders.JSON_CONTENT_TYPE.getValue());
            request.addHeader(RequestHeaders.ACCEPT.getValue(), RequestHeaders.JSON_CONTENT_TYPE.getValue());
            request.addHeader(RequestHeaders.X_AUTH_TOKEN.getValue(), localToken.getAccessId());

            response = client.execute(request);
            responseStr = EntityUtils.toString(response.getEntity(), StandardCharsets.UTF_8);
        } catch (Exception e) {
            LOGGER.error("Could not make GET request.", e);
            throw new RequestException(ErrorType.BAD_REQUEST, ResponseConstants.IRREGULAR_SYNTAX);
        } finally {
            try {
                EntityUtils.consume(response.getEntity());
            } catch (Throwable t) {
                // Do nothing
            }
        }

        checkStatusResponse(response, responseStr);

        return responseStr;
    }

    public HttpClient getClient() {
        return client;
    }

    public void setClient(HttpClient client) {
        this.client = client;
    }

    public List<Flavor> getFlavors() {
        return flavors;
    }

    public void setFlavors(List<Flavor> flavors) {
        this.flavors = flavors;
    }

    @Override
    public ComputeOrderInstance getInstance(Token localToken, String instanceId) throws RequestException {
        return null;
    }

    @Override
    public List<ComputeOrderInstance> getInstances(Token localToken) throws RequestException {
        String requestEndpoint = getComputeEndpoint(getTenantId(localToken), SERVERS);
        String jsonResponse = doGetRequest(requestEndpoint, localToken);
        return getInstancesFromJson(jsonResponse);
    }

    private List<ComputeOrderInstance> getInstancesFromJson(String json) {
        LOGGER.debug("Getting instances from json: " + json);
        List<ComputeOrderInstance> instances = new ArrayList<>();
        JSONObject root;

        try {
            root = new JSONObject(json);
            JSONArray servers = root.getJSONArray("servers");
            for (int i = 0; i < servers.length(); i++) {
                JSONObject currentServer = servers.getJSONObject(i);
                instances.add(new ComputeOrderInstance(currentServer.getString(ID_JSON_FIELD)));
            }
        } catch (JSONException e) {
            LOGGER.warn("There was an exception while getting instances from json.", e);
        }
        return instances;
    }

    @Override
    public void removeInstance(Token localToken, String instanceId) {

    }

    @Override
    public void removeInstances(Token localToken) {

    }

    @Override
    public String attachStorage(Token localToken, StorageLink storageLink) {
        return null;
    }

    @Override
    public String detachStorage(Token localToken, StorageLink storageLink) {
        return null;
    }

    @Override
    public String getImageId(Token localToken, String imageName) {
        return null;
    }
}
