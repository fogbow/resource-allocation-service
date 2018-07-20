package org.fogbowcloud.manager.core.plugins.cloud.openstack;

import org.apache.http.client.HttpResponseException;
import org.apache.log4j.Logger;
import org.fogbowcloud.manager.core.HomeDir;
import org.fogbowcloud.manager.core.constants.DefaultConfigurationConstants;
import org.fogbowcloud.manager.core.exceptions.*;
import org.fogbowcloud.manager.core.models.HardwareRequirements;
import org.fogbowcloud.manager.core.models.instances.ComputeInstance;
import org.fogbowcloud.manager.core.models.instances.InstanceState;
import org.fogbowcloud.manager.core.models.instances.InstanceType;
import org.fogbowcloud.manager.core.models.orders.ComputeOrder;
import org.fogbowcloud.manager.core.models.quotas.allocation.ComputeAllocation;
import org.fogbowcloud.manager.core.models.tokens.Token;
import org.fogbowcloud.manager.core.plugins.cloud.ComputePlugin;
import org.fogbowcloud.manager.core.plugins.cloud.util.DefaultLaunchCommandGenerator;
import org.fogbowcloud.manager.core.plugins.cloud.util.LaunchCommandGenerator;
import org.fogbowcloud.manager.util.*;
import org.fogbowcloud.manager.util.connectivity.*;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.File;
import java.util.*;

public class OpenStackNovaV2ComputePlugin implements ComputePlugin {

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

    private TreeSet<HardwareRequirements> hardwareRequirements;
    private Properties properties;
    private HttpRequestClientUtil client;
    private LaunchCommandGenerator launchCommandGenerator;

    public OpenStackNovaV2ComputePlugin() throws FatalErrorException {
        HomeDir homeDir = HomeDir.getInstance();
        this.properties = PropertiesUtil.readProperties(homeDir.getPath() + File.separator
                + DefaultConfigurationConstants.OPENSTACK_CONF_FILE_NAME);
        this.launchCommandGenerator = new DefaultLaunchCommandGenerator();
        instantiateOtherAttributes();
    }
    
    /** Constructor used for testing only */
    protected OpenStackNovaV2ComputePlugin(Properties properties, LaunchCommandGenerator launchCommandGenerator) {
        LOGGER.debug("Creating OpenStackNovaV2ComputePlugin with properties=" + properties.toString());
        this.properties = properties;
        this.launchCommandGenerator = launchCommandGenerator;
        instantiateOtherAttributes();
    }

    public String requestInstance(ComputeOrder computeOrder, Token localToken)
            throws FogbowManagerException, UnexpectedException {
        LOGGER.debug("Requesting instance with tokens=" + localToken);

        HardwareRequirements hardwareRequirements = findSmallestFlavor(computeOrder, localToken);
        String flavorId = hardwareRequirements.getId();
        String tenantId = getTenantId(localToken);
        List<String> networksId = resolveNetworksId(computeOrder);
        String imageId = computeOrder.getImageId();
        String userData = this.launchCommandGenerator.createLaunchCommand(computeOrder);
        String keyName = getKeyName(tenantId, localToken, computeOrder.getPublicKey());
        String endpoint = getComputeEndpoint(tenantId, SERVERS);
        String instanceId = null;

        try {
            JSONObject json = generateJsonRequest(imageId, flavorId, userData, keyName, networksId);
            String jsonResponse = this.client.doPostRequest(endpoint, localToken, json);

            instanceId = getAttFromJson(ID_JSON_FIELD, jsonResponse);

            synchronized (computeOrder) {
                ComputeAllocation actualAllocation = new ComputeAllocation(hardwareRequirements.getCpu(), hardwareRequirements.getRam(), 1);
                computeOrder.setActualAllocation(actualAllocation);
            }
        } catch (JSONException e) {
            String errorMsg = "Invalid JSON key: " + e.getMessage();
            LOGGER.error(errorMsg);
            throw new InvalidParameterException(errorMsg);
        } catch (HttpResponseException e) {
            OpenStackHttpToFogbowManagerExceptionMapper.map(e);
        } finally {
            if (keyName != null) {
                try {
                    deleteKeyName(tenantId, localToken, keyName);
                } catch (HttpResponseException e) {
                    OpenStackHttpToFogbowManagerExceptionMapper.map(e);
                }
            }
        }
        return instanceId;
    }

    @Override
    public ComputeInstance getInstance(String instanceId, Token localToken)
            throws FogbowManagerException, UnexpectedException {
        LOGGER.info("Getting instance " + instanceId + " with tokens " + localToken);

        String tenantId = getTenantId(localToken);
        String requestEndpoint = getComputeEndpoint(tenantId, SERVERS + "/" + instanceId);

        String jsonResponse = null;
        try {
            jsonResponse = this.client.doGetRequest(requestEndpoint, localToken);
        } catch (HttpResponseException e) {
            OpenStackHttpToFogbowManagerExceptionMapper.map(e);
        }

        LOGGER.debug("Getting instance from json: " + jsonResponse);
        ComputeInstance computeInstance = getInstanceFromJson(jsonResponse);

        return computeInstance;
    }

    @Override
    public void deleteInstance(String instanceId, Token localToken) throws FogbowManagerException, UnexpectedException {
        if (instanceId == null) {
            throw new FogbowManagerException();
        }
        String endpoint =
                getComputeEndpoint(getTenantId(localToken), SERVERS + "/" + instanceId);

        try {
            this.client.doDeleteRequest(endpoint, localToken);
        } catch (HttpResponseException e) {
            OpenStackHttpToFogbowManagerExceptionMapper.map(e);
        }
    }

    private void instantiateOtherAttributes() {
        this.hardwareRequirements = new TreeSet<HardwareRequirements>();
        this.initClient();
    }

    private void initClient() {
        HttpRequestUtil.init();
        this.client = new HttpRequestClientUtil();
    }

    protected String getTenantId(Token localToken) {
        Map<String, String> tokenAttr = localToken.getAttributes();
        return tokenAttr.get(TENANT_ID);
    }

    protected List<String> resolveNetworksId(ComputeOrder computeOrder) {
        List<String> requestedNetworksId = new ArrayList<>();
        String defaultNetworkId = this.properties.getProperty(DEFAULT_NETWORK_ID_KEY);
        requestedNetworksId.add(defaultNetworkId);
        requestedNetworksId.addAll(computeOrder.getNetworksId());
        computeOrder.setNetworksId(requestedNetworksId);
        return requestedNetworksId;
    }

    protected String getKeyName(String tenantId, Token localToken, String publicKey)
            throws FogbowManagerException, UnexpectedException {
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
                try {
                    this.client.doPostRequest(osKeypairEndpoint, localToken, root);
                } catch (HttpResponseException e) {
                    OpenStackHttpToFogbowManagerExceptionMapper.map(e);
                }
            } catch (JSONException e) {
                String errorMsg = "Error while getting key name: " + e;
                LOGGER.error(errorMsg);
                throw new UnexpectedException(errorMsg);
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
            throws HttpResponseException, UnavailableProviderException {
        String suffixEndpoint = SUFFIX_ENDPOINT_KEYPAIRS + "/" + keyName;
        String keynameEndpoint = getComputeEndpoint(tenantId, suffixEndpoint);

        this.client.doDeleteRequest(keynameEndpoint, localToken);
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

    protected HardwareRequirements findSmallestFlavor(ComputeOrder computeOrder, Token localToken)
            throws UnexpectedException, FogbowManagerException {
        updateFlavors(localToken, computeOrder);

        LOGGER.debug("Finding smallest flavor");

        return this.hardwareRequirements.first();
    }

    protected synchronized void updateFlavors(Token localToken, ComputeOrder order)
            throws FogbowManagerException, UnexpectedException {
        LOGGER.debug("Updating hardwareRequirements from OpenStack");

        try {
            String tenantId = getTenantId(localToken);
            if (tenantId == null) {
                return;
            }

            String endpoint = getComputeEndpoint(tenantId, SUFFIX_ENDPOINT_FLAVORS);
            String jsonResponseFlavors = this.client.doGetRequest(endpoint, localToken);

            List<String> flavorsId = new ArrayList<>();

            JSONArray jsonArrayFlavors =
                    new JSONObject(jsonResponseFlavors).getJSONArray(FLAVOR_JSON_KEY);

            for (int i = 0; i < jsonArrayFlavors.length(); i++) {
                JSONObject itemFlavor = jsonArrayFlavors.getJSONObject(i);
                flavorsId.add(itemFlavor.getString(ID_JSON_FIELD));
            }

            TreeSet<HardwareRequirements> newHardwareRequirements = detailFlavors(endpoint, localToken, flavorsId, order);
            if (newHardwareRequirements != null) {
                this.hardwareRequirements = newHardwareRequirements;
            }
        } catch (HttpResponseException e) {
            OpenStackHttpToFogbowManagerExceptionMapper.map(e);
        }
    }

    private TreeSet<HardwareRequirements> detailFlavors(String endpoint, Token localToken, List<String> flavorsId, ComputeOrder order)
            throws UnavailableProviderException, HttpResponseException {
        TreeSet<HardwareRequirements> newHardwareRequirements = new TreeSet<>();
        TreeSet<HardwareRequirements> flavorsCopy = new TreeSet<>(this.hardwareRequirements);

        for (String flavorId : flavorsId) {
            boolean containsFlavor = false;

            for (HardwareRequirements flavor : flavorsCopy) {
                if (flavor.getId().equals(flavorId)) {
                    containsFlavor = true;
                    newHardwareRequirements.add(flavor);
                    break;
                }
            }
            if (!containsFlavor) {
                String newEndpoint = endpoint + "/" + flavorId;
                String jsonResponseSpecificFlavor = this.client.doGetRequest(newEndpoint, localToken);

                JSONObject specificFlavor =
                        new JSONObject(jsonResponseSpecificFlavor)
                                .getJSONObject(FLAVOR_JSON_OBJECT);

                String id = specificFlavor.getString(ID_JSON_FIELD);
                String name = specificFlavor.getString(NAME_JSON_FIELD);
                int disk = specificFlavor.getInt(DISK_JSON_FIELD);
                int ram = specificFlavor.getInt(MEMORY_JSON_FIELD);
                int vcpus = specificFlavor.getInt(VCPU_JSON_FIELD);

                if (vcpus >= order.getvCPU() && ram >= order.getMemory() && disk >= order.getDisk()) {
                    newHardwareRequirements.add(new HardwareRequirements(name, id, vcpus, ram, disk));
                }
            }
        }

        return newHardwareRequirements;
    }

    private ComputeInstance getInstanceFromJson(String jsonResponse) throws FogbowManagerException {
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
            int disk = -1;

            // FIXME: Here, we need to update the HardwareRequirements TreeSet, because this Set is updated only in post request.
            // In case we try to retrieve the get, before doing a post request, the vcpu, memory and disk will be -1;

            HardwareRequirements hardwareRequirements = retrieveFlavorFromResponse(serverJson);
            if (hardwareRequirements != null) {
                vCPU = hardwareRequirements.getCpu();
                memory = hardwareRequirements.getRam();
                disk = hardwareRequirements.getDisk();
            }

            String openStackState = serverJson.getString(STATUS_JSON_FIELD);
            InstanceState fogbowState = OpenStackStateMapper.map(InstanceType.COMPUTE, openStackState);


            ComputeInstance computeInstance = new ComputeInstance(id, fogbowState, hostName, vCPU, memory,
                            disk, localIpAddress);
            return computeInstance;
        } catch (JSONException e) {
            LOGGER.warn("There was an exception while getting instances from json", e);
            throw new FogbowManagerException();
        }
    }

    private HardwareRequirements retrieveFlavorFromResponse(JSONObject jsonResponse) {
        HardwareRequirements hardwareRequirements = null;
        if (!jsonResponse.isNull(FLAVOR_JSON_FIELD)) {
            JSONObject flavorField = jsonResponse.optJSONObject(FLAVOR_JSON_FIELD);
            String flavorId = flavorField.optString(FLAVOR_ID_JSON_FIELD);
            hardwareRequirements = getFlavorById(flavorId);
        }
        return hardwareRequirements;
    }

    private HardwareRequirements getFlavorById(String id) {
        TreeSet<HardwareRequirements> flavorsCopy = new TreeSet<>(this.hardwareRequirements);
        for (HardwareRequirements hardwareRequirements : flavorsCopy) {
            if (hardwareRequirements.getId().equals(id)) {
                return hardwareRequirements;
            }
        }
        return null;
    }
    
    protected void setClient(HttpRequestClientUtil client) {
    	this.client = client;
    }
}
