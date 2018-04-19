package org.fogbowcloud.manager.core.plugins.compute.openstack;

import org.apache.http.HttpResponse;
import org.apache.http.HttpStatus;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.StringEntity;
import org.apache.http.util.EntityUtils;
import org.apache.log4j.Logger;
import org.fogbowcloud.manager.core.models.Flavor;
import org.fogbowcloud.manager.core.models.RequestHeaders;
import org.fogbowcloud.manager.core.models.ResponseConstants;
import org.fogbowcloud.manager.core.models.StorageLink;
import org.fogbowcloud.manager.core.models.exceptions.RequestException;
import org.fogbowcloud.manager.core.models.orders.ComputeOrder;
import org.fogbowcloud.manager.core.models.orders.UserData;
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
	private static final String KEY_JSON_FIELD = "key_name";
	private static final String UUID_JSON_FIELD = "uuid";
	private static final String FOGBOW_INSTANCE_NAME = "fogbow-instance-";

	private static final String SERVERS = "/servers";
	private static final String SUFFIX_ENDPOINT_FLAVORS = "/flavors";

	private static final Logger LOGGER = Logger.getLogger(OpenStackNovaV2ComputePlugin.class);
	
	private HttpClient client;
	private List<Flavor> flavors;

	public OpenStackNovaV2ComputePlugin() {
		this.client = HttpRequestUtil.createHttpClient(60000, null, null);
		this.flavors = new ArrayList<>();
	}

	public String requestInstance(ComputeOrder computeOrder, String imageId) throws RequestException {
		String flavorId = getFlavorId(computeOrder);
		String tenantId = getTenantId();
		String networkId = getNetworkId();
		UserData userData = getUserData();
		String keyName = getKeyName();
		String endpoint = getComputeEndpoint(tenantId, SERVERS);
		String token = getAuthToken();

		try {
			JSONObject json = generateJsonRequest(imageId, flavorId, userData, keyName, networkId);
			String jsonResponse = doPostRequest(endpoint, token, json);

			return getAttFromJson(ID_JSON_FIELD, jsonResponse);
		} catch (JSONException e) {
			LOGGER.error(e);
			throw new RequestException(HttpStatus.SC_BAD_REQUEST, ResponseConstants.IRREGULAR_SYNTAX);
		} finally {
			if (keyName != null) {
				try {
					deleteKeyName(token, keyName);
				} catch (Throwable t) {
					LOGGER.warn("Could not delete key.", t);
				}
			}
		}
	}

	private String getFlavorId(ComputeOrder computeOrder) {
		return findSmallestFlavor(computeOrder).getId();
		// return "f820a0a0-ccb2-4478-ab39-c4ae0cdd55c9";
	}

	private String getTenantId() {
		return "3324431f606d4a74a060cf78c16fcb21";
	}

	private String getNetworkId() {
		return "64ee4355-4d7f-4170-80b4-5e8348af6a61";
	}

	private String getAuthToken() {
		return "gAAAAABa2IriNHs7stOEHh4KVjwmGL9BnpTubynyr9Cjnw-Sc7mjg6DMxwcM0QRzEy14mfKyxo1" +
				"yWP1D0vh7OXdzQ7eCZrqJRPlLy5R5d3UjYC87l586eZH_pUg_iC-7hCL-VPfpQh9eHYfHoBsju" +
				"s9ND0nuU9PgONOcKm9eu58ZeIK-jGUJShLdUo0OIyRtAzNkguIsLH2Q2_QhdUAhcq48bOEmVsh" +
				"IgVEdFzWPkis1KY7YoC1MqMM";
	}

	private UserData getUserData() {
		return null;
	}

	private String getKeyName() {
		return null;
	}

	private String getComputeEndpoint(String tenantId, String suffix) {
		return "https://cloud.lsd.ufcg.edu.br:8774/v2.1/" + tenantId + suffix;
	}

	private String getAttFromJson(String attName, String jsonStr) throws JSONException {
		JSONObject root = new JSONObject(jsonStr);
		return root.getJSONObject(SERVER_JSON_FIELD).getString(attName);
	}

	private void deleteKeyName(String authToken, String keyName) {
		// TODO: implement this method
	}

	private String doPostRequest(String endpoint, String authToken, JSONObject jsonRequest) throws RequestException {
		HttpResponse response = null;
		String responseStr;

		try {
			HttpPost request = new HttpPost(endpoint);
			request.addHeader(RequestHeaders.CONTENT_TYPE.getValue(), RequestHeaders.JSON_CONTENT_TYPE.getValue());
			request.addHeader(RequestHeaders.ACCEPT.getValue(), RequestHeaders.JSON_CONTENT_TYPE.getValue());
			request.addHeader(RequestHeaders.X_AUTH_TOKEN.getValue(), authToken);

			request.setEntity(new StringEntity(jsonRequest.toString(), StandardCharsets.UTF_8));
			response = client.execute(request);
			responseStr = EntityUtils.toString(response.getEntity(), StandardCharsets.UTF_8);
		} catch (Exception e) {
			LOGGER.error(e);
			throw new RequestException(HttpStatus.SC_BAD_REQUEST, ResponseConstants.IRREGULAR_SYNTAX);
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

	private JSONObject generateJsonRequest(String imageRef, String flavorRef, UserData userdata,
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
		
		if (keyName != null && !keyName.isEmpty()){
			server.put(KEY_JSON_FIELD, keyName);
		}

		JSONObject root = new JSONObject();
		root.put(SERVER_JSON_FIELD, server);
				
		return root;
	}

	private void checkStatusResponse(HttpResponse response, String responseStr) {
		// TODO: implement this method
	}

	private Flavor findSmallestFlavor(ComputeOrder computeOrder) {
		updateFlavors();

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
		if (flavor.getDisk() < computeOrder.getDisk()) {
			return false;
		}

		if (flavor.getCpu() < computeOrder.getvCPU()) {
			return false;
		}

		if (flavor.getMem() < computeOrder.getMemory()) {
			return false;
		}

		return true;
	}

	private synchronized void updateFlavors() {
		try {
			String tenantId = getTenantId();
			if (tenantId == null) {
				return;
			}

			String endpoint = getComputeEndpoint(tenantId, SUFFIX_ENDPOINT_FLAVORS);
			String authToken = getAuthToken();
			String jsonResponseFlavors = doGetRequest(endpoint, authToken);

			Map<String, String> nameToFlavorId = new HashMap<>();

			JSONArray jsonArrayFlavors = new JSONObject(jsonResponseFlavors).getJSONArray("flavors");

			for (int i = 0; i < jsonArrayFlavors.length(); i++) {
				JSONObject itemFlavor = jsonArrayFlavors.getJSONObject(i);
				nameToFlavorId.put(itemFlavor.getString("name"), itemFlavor.getString("id"));
			}

			List<Flavor> newFlavors = detailFlavors(endpoint, authToken, nameToFlavorId);
			if (newFlavors != null) {
				this.flavors.addAll(newFlavors);
			}

			removeInvalidFlavors(nameToFlavorId);

		} catch (Exception e) {
			LOGGER.warn("Error while updating flavors.", e);
		}
	}

	private List<Flavor> detailFlavors(String endpoint, String authToken,
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
			String jsonResponseSpecificFlavor = doGetRequest(newEndpoint, authToken);

			JSONObject specificFlavor = new JSONObject(jsonResponseSpecificFlavor)
					.getJSONObject("flavor");

			String id = specificFlavor.getString("id");
			String name = specificFlavor.getString("name");
			int disk = specificFlavor.getInt("disk");
			int ram = specificFlavor.getInt("ram");
			int vcpus = specificFlavor.getInt("vcpus");

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

	protected String doGetRequest(String endpoint, String authToken) throws RequestException {
		HttpResponse response = null;
		String responseStr;

		try {
			HttpGet request = new HttpGet(endpoint);
			request.addHeader(RequestHeaders.CONTENT_TYPE.getValue(), RequestHeaders.JSON_CONTENT_TYPE.getValue());
			request.addHeader(RequestHeaders.ACCEPT.getValue(), RequestHeaders.JSON_CONTENT_TYPE.getValue());
			request.addHeader(RequestHeaders.X_AUTH_TOKEN.getValue(), authToken);

			response = client.execute(request);
			responseStr = EntityUtils.toString(response.getEntity(), StandardCharsets.UTF_8);
		} catch (Exception e) {
			LOGGER.error("Could not make GET request.", e);
			throw new RequestException(HttpStatus.SC_BAD_REQUEST, ResponseConstants.IRREGULAR_SYNTAX);
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

	@Override
	public ComputeOrderInstance getInstance(Token localToken, String instanceId) {
		return null;
	}

	@Override
	public List<ComputeOrderInstance> getInstances(Token localToken) {
		return null;
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

	public static void main(String[] args) {
		OpenStackNovaV2ComputePlugin compute = new OpenStackNovaV2ComputePlugin();

		ComputeOrder computeOrder = new ComputeOrder(null, null, null, null,
				null, null, null, 6000,
		1, 2000, 20, null, null, null);

		try {
			compute.requestInstance(computeOrder, "0bd03dd3-ba50-4eb8-a71f-3c46b4290471");
		} catch (RequestException e) {
			e.printStackTrace();
		}
	}
}
