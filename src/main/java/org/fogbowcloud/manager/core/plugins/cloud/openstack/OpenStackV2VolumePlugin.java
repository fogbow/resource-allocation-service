package org.fogbowcloud.manager.core.plugins.cloud.openstack;

import java.io.File;
import java.util.Properties;
import org.apache.http.client.HttpResponseException;
import org.apache.log4j.Logger;
import org.fogbowcloud.manager.core.HomeDir;
import org.fogbowcloud.manager.core.exceptions.*;
import org.fogbowcloud.manager.core.models.instances.InstanceState;
import org.fogbowcloud.manager.core.models.instances.InstanceType;
import org.fogbowcloud.manager.core.plugins.cloud.VolumePlugin;
import org.fogbowcloud.manager.core.models.orders.VolumeOrder;
import org.fogbowcloud.manager.core.models.instances.VolumeInstance;
import org.fogbowcloud.manager.core.models.tokens.Token;
import org.fogbowcloud.manager.util.connectivity.HttpRequestClientUtil;
import org.fogbowcloud.manager.util.PropertiesUtil;
import org.json.JSONException;
import org.json.JSONObject;

public class OpenStackV2VolumePlugin implements VolumePlugin {

	private static final String CINDER_PLUGIN_CONF_FILE = "openstack-cinder-volume-plugin.conf";
	public static final String VOLUME_NOVAV2_URL_KEY = "openstack_cinder_url";

	private final String TENANT_ID_IS_NOT_SPECIFIED_ERROR = "Tenant id is not specified.";

	private final String V2_API_ENDPOINT = "/v2/";

	protected static final String KEY_JSON_VOLUME = "volume";
	protected static final String KEY_JSON_STATUS = "status";
	protected static final String KEY_JSON_SIZE = "size";
	protected static final String KEY_JSON_NAME = "name";
	protected static final String KEY_JSON_ID = "id";
	protected static final String SUFIX_ENDPOINT_VOLUMES = "/volumes";

	private HttpRequestClientUtil client;
	private String volumeV2APIEndpoint;

	private static final Logger LOGGER = Logger.getLogger(OpenStackV2VolumePlugin.class);

	public OpenStackV2VolumePlugin() throws FatalErrorException {
		HomeDir homeDir = HomeDir.getInstance();
		Properties properties = PropertiesUtil.
				readProperties(homeDir.getPath() + File.separator + CINDER_PLUGIN_CONF_FILE);
		this.volumeV2APIEndpoint = properties.getProperty(VOLUME_NOVAV2_URL_KEY) + V2_API_ENDPOINT;

		initClient();
	}
	
	@Override
	public String requestInstance(VolumeOrder order, Token localToken)
			throws FogbowManagerException, UnexpectedException {
		String tenantId = localToken.getAttributes().get(KeystoneV3IdentityPlugin.TENANT_ID);
		if (tenantId == null) {
			LOGGER.error(TENANT_ID_IS_NOT_SPECIFIED_ERROR);
			throw new UnauthenticatedUserException(TENANT_ID_IS_NOT_SPECIFIED_ERROR);
		}
		String size = String.valueOf(order.getVolumeSize());
		String name = order.getVolumeName();

		JSONObject jsonRequest = null;
		try {
			jsonRequest = generateJsonEntityToCreateInstance(size, name);
		} catch (JSONException e) {
			String errorMsg = "An error occurred when generating json.";
			LOGGER.error(errorMsg, e);
			throw new InvalidParameterException(errorMsg, e);
		}

		String endpoint = this.volumeV2APIEndpoint + tenantId + SUFIX_ENDPOINT_VOLUMES;
		String responseStr = null;
		try {
			responseStr = this.client.doPostRequest(endpoint, localToken, jsonRequest);
		} catch (HttpResponseException e) {
			OpenStackHttpToFogbowManagerExceptionMapper.map(e);
		}
		VolumeInstance instanceFromJson = getInstanceFromJson(responseStr);
		return instanceFromJson != null ? instanceFromJson.getId() : null;
	}

	@Override
	public VolumeInstance getInstance(String storageOrderInstanceId, Token localToken)
			throws FogbowManagerException, UnexpectedException {
		String tenantId = localToken.getAttributes().get(KeystoneV3IdentityPlugin.TENANT_ID);
		if (tenantId == null) {
			LOGGER.error(TENANT_ID_IS_NOT_SPECIFIED_ERROR);
			throw new UnauthenticatedUserException(TENANT_ID_IS_NOT_SPECIFIED_ERROR);
		}		
		
		String endpoint = this.volumeV2APIEndpoint + tenantId
				+ SUFIX_ENDPOINT_VOLUMES + "/" + storageOrderInstanceId;
		String responseStr = null;
		try {
			responseStr = this.client.doGetRequest(endpoint, localToken);
		} catch (HttpResponseException e) {
			OpenStackHttpToFogbowManagerExceptionMapper.map(e);
		}
		return getInstanceFromJson(responseStr);
	}

	@Override
	public void deleteInstance(String storageOrderInstanceId, Token localToken)
			throws FogbowManagerException, UnexpectedException {
		String tenantId = localToken.getAttributes().get(KeystoneV3IdentityPlugin.TENANT_ID);
		if (tenantId == null) {
			LOGGER.error(TENANT_ID_IS_NOT_SPECIFIED_ERROR);
			throw new UnauthenticatedUserException(TENANT_ID_IS_NOT_SPECIFIED_ERROR);
		}		
		
		String endpoint = this.volumeV2APIEndpoint + tenantId
				+ SUFIX_ENDPOINT_VOLUMES + "/" + storageOrderInstanceId;
		try {
			this.client.doDeleteRequest(endpoint, localToken);
		} catch (HttpResponseException e) {
			OpenStackHttpToFogbowManagerExceptionMapper.map(e);
		}
	}

	protected VolumeInstance getInstanceFromJson(String json) throws UnexpectedException {
		try {
			JSONObject rootServer = new JSONObject(json);
			JSONObject volumeJson = rootServer.getJSONObject(KEY_JSON_VOLUME);
			String id = volumeJson.getString(KEY_JSON_ID);
			
			String name = volumeJson.optString(KEY_JSON_NAME);
			String statusOpenstack = volumeJson.optString(KEY_JSON_STATUS);
			InstanceState fogbowState = OpenStackStateMapper.map(InstanceType.VOLUME, statusOpenstack);
			String sizeStr = volumeJson.optString(KEY_JSON_SIZE);
			int size = Integer.valueOf(sizeStr);

			return new VolumeInstance(id, fogbowState, name, size);
		} catch (Exception e) {
			String errorMsg = "There was an exception while getting volume instance.";
			LOGGER.error(errorMsg, e);
			throw new UnexpectedException(errorMsg, e);
		}
	}

	protected JSONObject generateJsonEntityToCreateInstance(String size, String name) throws JSONException {
		JSONObject volumeContent = new JSONObject();
		volumeContent.put(KEY_JSON_SIZE, size);
		volumeContent.put(KEY_JSON_NAME, name);

		JSONObject volume = new JSONObject();
		volume.put(KEY_JSON_VOLUME, volumeContent);
		
		return volume;
	}	
	
	private void initClient() {
		this.client = new HttpRequestClientUtil();
	}	
	
	public void setClient(HttpRequestClientUtil client) {
		this.client = client;
	}
}