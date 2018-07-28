package org.fogbowcloud.manager.core.plugins.cloud.openstack;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.TreeSet;
import java.util.UUID;

import org.apache.http.client.HttpResponseException;
import org.apache.log4j.Logger;
import org.fogbowcloud.manager.core.HomeDir;
import org.fogbowcloud.manager.core.constants.DefaultConfigurationConstants;
import org.fogbowcloud.manager.core.exceptions.FatalErrorException;
import org.fogbowcloud.manager.core.exceptions.FogbowManagerException;
import org.fogbowcloud.manager.core.exceptions.InvalidParameterException;
import org.fogbowcloud.manager.core.exceptions.NoAvailableResourcesException;
import org.fogbowcloud.manager.core.exceptions.UnexpectedException;
import org.fogbowcloud.manager.core.models.HardwareRequirements;
import org.fogbowcloud.manager.core.models.instances.ComputeInstance;
import org.fogbowcloud.manager.core.models.instances.InstanceState;
import org.fogbowcloud.manager.core.models.ResourceType;
import org.fogbowcloud.manager.core.models.orders.ComputeOrder;
import org.fogbowcloud.manager.core.models.quotas.allocation.ComputeAllocation;
import org.fogbowcloud.manager.core.models.tokens.Token;
import org.fogbowcloud.manager.core.plugins.cloud.ComputePlugin;
import org.fogbowcloud.manager.core.plugins.cloud.util.DefaultLaunchCommandGenerator;
import org.fogbowcloud.manager.core.plugins.cloud.util.LaunchCommandGenerator;
import org.fogbowcloud.manager.util.PropertiesUtil;
import org.fogbowcloud.manager.util.connectivity.HttpRequestClientUtil;
import org.fogbowcloud.manager.util.connectivity.HttpRequestUtil;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

public class OpenStackNovaV2ComputePlugin implements ComputePlugin {

	protected static final String COMPUTE_NOVAV2_URL_KEY = "openstack_nova_v2_url";
	protected static final String DEFAULT_NETWORK_ID_KEY = "default_network_id";

	protected static final String ID_JSON_FIELD = "id";
	protected static final String NAME_JSON_FIELD = "name";
	protected static final String SERVER_JSON_FIELD = "server";
	protected static final String FLAVOR_REF_JSON_FIELD = "flavorRef";
	protected static final String FLAVOR_JSON_FIELD = "flavor";
	protected static final String FLAVOR_ID_JSON_FIELD = "id";
	protected static final String IMAGE_JSON_FIELD = "imageRef";
	protected static final String USER_DATA_JSON_FIELD = "user_data";
	protected static final String NETWORK_JSON_FIELD = "networks";
	protected static final String STATUS_JSON_FIELD = "status";
	protected static final String DISK_JSON_FIELD = "disk";
	protected static final String VCPU_JSON_FIELD = "vcpus";
	protected static final String MEMORY_JSON_FIELD = "ram";
	protected static final String SECURITY_JSON_FIELD = "security_groups";
	protected static final String FLAVOR_JSON_OBJECT = "flavor";
	protected static final String FLAVOR_JSON_KEY = "flavors";
	protected static final String KEY_JSON_FIELD = "key_name";
	protected static final String PUBLIC_KEY_JSON_FIELD = "public_key";
	protected static final String KEYPAIR_JSON_FIELD = "keypair";
	protected static final String UUID_JSON_FIELD = "uuid";
	protected static final String FOGBOW_INSTANCE_NAME = "fogbow-instance-";
	protected static final String TENANT_ID = "tenantId";

	protected static final String SERVERS = "/servers";
	protected static final String SUFFIX_ENDPOINT_KEYPAIRS = "/os-keypairs";
	protected static final String SUFFIX_ENDPOINT_FLAVORS = "/flavors";
	protected static final String COMPUTE_V2_API_ENDPOINT = "/v2/";

	protected static final Logger LOGGER = Logger.getLogger(OpenStackNovaV2ComputePlugin.class);
	protected static final String ADDRESS_FIELD = "addresses";
	protected static final String PROVIDER_NETWORK_FIELD = "provider";
	protected static final String ADDR_FIELD = "addr";

	private TreeSet<HardwareRequirements> hardwareRequirementsList;
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
	protected OpenStackNovaV2ComputePlugin(Properties properties, 
			LaunchCommandGenerator launchCommandGenerator,
			HttpRequestClientUtil client) {
		LOGGER.debug("Creating OpenStackNovaV2ComputePlugin with properties=" + properties.toString());
		this.properties = properties;
		this.launchCommandGenerator = launchCommandGenerator;
		this.client = client;
		this.hardwareRequirementsList = new TreeSet<HardwareRequirements>();
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
				ComputeAllocation actualAllocation = new ComputeAllocation(
						hardwareRequirements.getCpu(),
						hardwareRequirements.getRam(), 
						1);
				computeOrder.setActualAllocation(actualAllocation);
			}
		} catch (HttpResponseException e) {
			OpenStackHttpToFogbowManagerExceptionMapper.map(e);
		} finally {
			if (keyName != null) {
				deleteKeyName(tenantId, localToken, keyName);
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
		ComputeInstance computeInstance = getInstanceFromJson(jsonResponse, localToken);

		return computeInstance;
	}

	@Override
	public void deleteInstance(String instanceId, Token localToken) throws FogbowManagerException, UnexpectedException {
		LOGGER.info("Deleting instance " + instanceId + " with tokens " + localToken);
		String endpoint = getComputeEndpoint(getTenantId(localToken), SERVERS + "/" + instanceId);
		try {
			this.client.doDeleteRequest(endpoint, localToken);
		} catch (HttpResponseException e) {
			OpenStackHttpToFogbowManagerExceptionMapper.map(e);
		}
	}

	private void instantiateOtherAttributes() {
		this.hardwareRequirementsList = new TreeSet<HardwareRequirements>();
		this.initClient();
	}

	private void initClient() {
		HttpRequestUtil.init();
		this.client = new HttpRequestClientUtil();
	}

	private String getTenantId(Token localToken) throws InvalidParameterException {
		Map<String, String> tokenAttr = localToken.getAttributes();
		String tenantId = tokenAttr.get(TENANT_ID);
		if (tenantId == null) {
			throw new InvalidParameterException("No tenantId in local token.");
		}
		return tenantId;
	}

	private List<String> resolveNetworksId(ComputeOrder computeOrder) {
		List<String> requestedNetworksId = new ArrayList<>();
		String defaultNetworkId = this.properties.getProperty(DEFAULT_NETWORK_ID_KEY);
		//We add the default network before any other network, because the order is very important to Openstack
		//request. Openstack will configure the routes to the external network by the first network found on request body.
		requestedNetworksId.add(defaultNetworkId);
		requestedNetworksId.addAll(computeOrder.getNetworksId());
		computeOrder.setNetworksId(requestedNetworksId);
		return requestedNetworksId;
	}

	private String getKeyName(String tenantId, Token localToken, String publicKey)
			throws FogbowManagerException, UnexpectedException {
		String keyname = null;

		if (publicKey != null && !publicKey.isEmpty()) {
			String osKeypairEndpoint = getComputeEndpoint(tenantId, SUFFIX_ENDPOINT_KEYPAIRS);

			keyname = getRandomUUID();
			JSONObject keypair = new JSONObject();
			
			keypair.put(NAME_JSON_FIELD, keyname);
			keypair.put(PUBLIC_KEY_JSON_FIELD, publicKey);
			JSONObject root = new JSONObject();
			root.put(KEYPAIR_JSON_FIELD, keypair);
			try {
				this.client.doPostRequest(osKeypairEndpoint, localToken, root);
			} catch (HttpResponseException e) {
				OpenStackHttpToFogbowManagerExceptionMapper.map(e);
			}
		}

		return keyname;
	}

	private String getComputeEndpoint(String tenantId, String suffix) {
		return this.properties.getProperty(COMPUTE_NOVAV2_URL_KEY) + COMPUTE_V2_API_ENDPOINT + tenantId + suffix;
	}

	private String getAttFromJson(String attName, String jsonStr) throws JSONException {
		JSONObject root = new JSONObject(jsonStr);
		JSONObject serveJson = root.getJSONObject(SERVER_JSON_FIELD);
		String jsonAttValue = serveJson.getString(attName);
		return jsonAttValue;
	}

	private void deleteKeyName(String tenantId, Token localToken, String keyName) throws FogbowManagerException, UnexpectedException {
		String suffixEndpoint = SUFFIX_ENDPOINT_KEYPAIRS + "/" + keyName;
		String keynameEndpoint = getComputeEndpoint(tenantId, suffixEndpoint);
		
		try {
			this.client.doDeleteRequest(keynameEndpoint, localToken);
		} catch (HttpResponseException e) {
			OpenStackHttpToFogbowManagerExceptionMapper.map(e);
		}
	}

	private JSONObject generateJsonRequest(String imageRef, String flavorRef, String userdata, String keyName,
			List<String> networksId) {
		LOGGER.debug("Generating JSON to send as the body of instance POST request");

		JSONObject server = new JSONObject();
		server.put(NAME_JSON_FIELD, FOGBOW_INSTANCE_NAME + getRandomUUID());
		server.put(IMAGE_JSON_FIELD, imageRef);
		server.put(FLAVOR_REF_JSON_FIELD, flavorRef);

		if (userdata != null) {
			server.put(USER_DATA_JSON_FIELD, userdata);
		}

		JSONArray networks = new JSONArray();
		for (String id : networksId) {
			JSONObject netId = new JSONObject();
			netId.put(UUID_JSON_FIELD, id);
			networks.put(netId);
			String defaultNetworkId = this.properties.getProperty(DEFAULT_NETWORK_ID_KEY);
			if (!id.equals(defaultNetworkId)) {
				// if this is not the default network, we need to open SSH port, and also ICMP connection.
				JSONArray securityGroup = new JSONArray();
				JSONObject securityGroupName = new JSONObject();
				String securityGroupProperty = OpenStackV2NetworkPlugin.DEFAULT_SECURITY_GROUP_NAME + "-" + id;
				securityGroupName.put(NAME_JSON_FIELD, securityGroupProperty);
				securityGroup.put(securityGroupName);
				server.put(SECURITY_JSON_FIELD, securityGroup);
			}
		}
		server.put(NETWORK_JSON_FIELD, networks);
	
		if (keyName != null) {
			server.put(KEY_JSON_FIELD, keyName);
		}

		JSONObject root = new JSONObject();
		root.put(SERVER_JSON_FIELD, server);

		return root;
	}

	private HardwareRequirements findSmallestFlavor(ComputeOrder computeOrder, Token localToken)
			throws UnexpectedException, FogbowManagerException {
		HardwareRequirements bestFlavor = getBestFlavor(computeOrder, localToken);
		if (bestFlavor == null) {
			throw new NoAvailableResourcesException();
		}
		return bestFlavor;
	}

	private HardwareRequirements getBestFlavor(ComputeOrder computeOrder, Token localToken) throws FogbowManagerException, UnexpectedException {
		updateFlavors(localToken);
		TreeSet<HardwareRequirements> hardwareRequirementsList = getHardwareRequirementsList();
		for (HardwareRequirements hardwareRequirements : hardwareRequirementsList) {
			if (hardwareRequirements.getCpu() >= computeOrder.getvCPU()
					&& hardwareRequirements.getRam() >= computeOrder.getMemory()
					&& hardwareRequirements.getDisk() >= computeOrder.getDisk()) {
				return hardwareRequirements;
			}
		}
		return null;
	}

	private void updateFlavors(Token localToken)
			throws FogbowManagerException, UnexpectedException {
		LOGGER.debug("Updating hardwareRequirements from OpenStack");

			String tenantId = getTenantId(localToken);
			String endpoint = getComputeEndpoint(tenantId, SUFFIX_ENDPOINT_FLAVORS);
			
			String jsonResponseFlavors = null;
			try {
				jsonResponseFlavors = this.client.doGetRequest(endpoint, localToken);
			} catch (HttpResponseException e) {
				OpenStackHttpToFogbowManagerExceptionMapper.map(e);
			}

			List<String> flavorsId = new ArrayList<>();

			JSONArray jsonArrayFlavors = new JSONObject(jsonResponseFlavors).getJSONArray(FLAVOR_JSON_KEY);

			for (int i = 0; i < jsonArrayFlavors.length(); i++) {
				JSONObject itemFlavor = jsonArrayFlavors.getJSONObject(i);
				flavorsId.add(itemFlavor.getString(ID_JSON_FIELD));
			}

			TreeSet<HardwareRequirements> newHardwareRequirements = detailFlavors(endpoint, localToken, flavorsId);

			setHardwareRequirementsList(newHardwareRequirements);

	}

	private TreeSet<HardwareRequirements> detailFlavors(String endpoint, Token localToken, List<String> flavorsId) 
			throws FogbowManagerException, UnexpectedException 
			{
		TreeSet<HardwareRequirements> newHardwareRequirements = new TreeSet<>();
		TreeSet<HardwareRequirements> flavorsCopy = new TreeSet<>(getHardwareRequirementsList());

		for (String flavorId : flavorsId) {
			boolean containsFlavorForCaching = false;

			for (HardwareRequirements flavor : flavorsCopy) {
				if (flavor.getId().equals(flavorId)) {
					containsFlavorForCaching = true;
					newHardwareRequirements.add(flavor);
					break;
				}
			}
			if (!containsFlavorForCaching) {
				String newEndpoint = endpoint + "/" + flavorId;
				
				String jsonResponseSpecificFlavor = null;
				
				try {
					jsonResponseSpecificFlavor = this.client.doGetRequest(newEndpoint, localToken);
				} catch (HttpResponseException e) {
					OpenStackHttpToFogbowManagerExceptionMapper.map(e);
				}
				
				JSONObject specificFlavor = new JSONObject(jsonResponseSpecificFlavor)
						.getJSONObject(FLAVOR_JSON_OBJECT);

				String id = specificFlavor.getString(ID_JSON_FIELD);
				String name = specificFlavor.getString(NAME_JSON_FIELD);
				int disk = specificFlavor.getInt(DISK_JSON_FIELD);
				int ram = specificFlavor.getInt(MEMORY_JSON_FIELD);
				int vcpus = specificFlavor.getInt(VCPU_JSON_FIELD);

				newHardwareRequirements.add(new HardwareRequirements(name, id, vcpus, ram, disk));
			}
		}

		return newHardwareRequirements;
	}

	private ComputeInstance getInstanceFromJson(String jsonResponse, Token localToken) throws FogbowManagerException, UnexpectedException {
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
		
		
		String flavorId = retrieveFlavorIdFromResponse(serverJson);
		HardwareRequirements hardwareRequirements = getFlavorById(flavorId, localToken);
		if (hardwareRequirements != null) {
			vCPU = hardwareRequirements.getCpu();
			memory = hardwareRequirements.getRam();
			disk = hardwareRequirements.getDisk();
		} else {
			throw new NoAvailableResourcesException("No matching flavor");
		}

		String openStackState = serverJson.getString(STATUS_JSON_FIELD);
		InstanceState fogbowState = OpenStackStateMapper.map(ResourceType.COMPUTE, openStackState);

		ComputeInstance computeInstance = new ComputeInstance(id, fogbowState, hostName, vCPU, memory, disk,
				localIpAddress);
		return computeInstance;
	}

	private String retrieveFlavorIdFromResponse(JSONObject jsonResponse) {
		JSONObject flavorField = jsonResponse.optJSONObject(FLAVOR_JSON_FIELD);
		return flavorField.optString(FLAVOR_ID_JSON_FIELD);
	}

	private HardwareRequirements getFlavorById(String id, Token localToken) throws FogbowManagerException, UnexpectedException {
		updateFlavors(localToken);
		TreeSet<HardwareRequirements> flavorsCopy = new TreeSet<>(getHardwareRequirementsList());
		for (HardwareRequirements hardwareRequirements : flavorsCopy) {
			if (hardwareRequirements.getId().equals(id)) {
				return hardwareRequirements;
			}
		}
		return null;
	}

	protected String getRandomUUID() {
		return UUID.randomUUID().toString();
	}
	
	private TreeSet<HardwareRequirements> getHardwareRequirementsList() {
		synchronized (this.hardwareRequirementsList) {
			return new TreeSet<HardwareRequirements>(this.hardwareRequirementsList);
		}
	}
	
	private void setHardwareRequirementsList(TreeSet<HardwareRequirements> hardwareRequirementsList) {
		synchronized (this.hardwareRequirementsList) {
			this.hardwareRequirementsList = hardwareRequirementsList;
		}
	}
}
