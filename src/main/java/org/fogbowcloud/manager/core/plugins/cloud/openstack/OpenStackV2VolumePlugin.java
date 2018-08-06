package org.fogbowcloud.manager.core.plugins.cloud.openstack;

import java.io.File;
import java.util.Properties;

import org.apache.http.client.HttpResponseException;
import org.apache.log4j.Logger;
import org.fogbowcloud.manager.core.HomeDir;
import org.fogbowcloud.manager.core.constants.DefaultConfigurationConstants;
import org.fogbowcloud.manager.core.exceptions.FatalErrorException;
import org.fogbowcloud.manager.core.exceptions.FogbowManagerException;
import org.fogbowcloud.manager.core.exceptions.InvalidParameterException;
import org.fogbowcloud.manager.core.exceptions.UnauthenticatedUserException;
import org.fogbowcloud.manager.core.exceptions.UnexpectedException;
import org.fogbowcloud.manager.core.models.ResourceType;
import org.fogbowcloud.manager.core.models.instances.InstanceState;
import org.fogbowcloud.manager.core.models.instances.VolumeInstance;
import org.fogbowcloud.manager.core.models.orders.VolumeOrder;
import org.fogbowcloud.manager.core.models.tokens.KeystoneV3TokenGenerator;
import org.fogbowcloud.manager.core.models.tokens.Token;
import org.fogbowcloud.manager.core.plugins.cloud.VolumePlugin;
import org.fogbowcloud.manager.core.plugins.serialization.openstack.volume.v2.CreateVolumeRequest;
import org.fogbowcloud.manager.core.plugins.serialization.openstack.volume.v2.GetVolumeResponse;
import org.fogbowcloud.manager.util.PropertiesUtil;
import org.fogbowcloud.manager.util.connectivity.HttpRequestClientUtil;
import org.json.JSONException;

public class OpenStackV2VolumePlugin implements VolumePlugin {

	private final String TENANT_ID_IS_NOT_SPECIFIED_ERROR = "Tenant id is not specified.";

	private final String V2_API_ENDPOINT = "/v2/";
	protected static final String SUFIX_ENDPOINT_VOLUMES = "/volumes";
	
	public static final String VOLUME_NOVAV2_URL_KEY = "openstack_cinder_url";
	protected static final String DEFAULT_VOLUME_NAME = "fogbow-volume";

	private HttpRequestClientUtil client;
	private String volumeV2APIEndpoint;

	private static final Logger LOGGER = Logger.getLogger(OpenStackV2VolumePlugin.class);

	public OpenStackV2VolumePlugin() throws FatalErrorException {
		HomeDir homeDir = HomeDir.getInstance();
        Properties properties = PropertiesUtil.readProperties(homeDir.getPath() + File.separator
                + DefaultConfigurationConstants.OPENSTACK_CONF_FILE_NAME);
		this.volumeV2APIEndpoint = properties.getProperty(VOLUME_NOVAV2_URL_KEY) + V2_API_ENDPOINT;

		initClient();
	}
	
	@Override
	public String requestInstance(VolumeOrder order, Token localToken)
			throws FogbowManagerException, UnexpectedException {
		String tenantId = localToken.getAttributes().get(KeystoneV3TokenGenerator.TENANT_ID);
		if (tenantId == null) {
			LOGGER.error(TENANT_ID_IS_NOT_SPECIFIED_ERROR);
			throw new UnauthenticatedUserException(TENANT_ID_IS_NOT_SPECIFIED_ERROR);
		}

		String jsonRequest = null;
		try {
			String size = String.valueOf(order.getVolumeSize());
			String name = order.getVolumeName();
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
		String tenantId = localToken.getAttributes().get(KeystoneV3TokenGenerator.TENANT_ID);
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
		String tenantId = localToken.getAttributes().get(KeystoneV3TokenGenerator.TENANT_ID);
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
			GetVolumeResponse getVolumeResponse = GetVolumeResponse.fromJson(json);
			String id = getVolumeResponse.getId();
			String name = getVolumeResponse.getName();
			int size = getVolumeResponse.getSize();
			String status = getVolumeResponse.getStatus();
			InstanceState fogbowState = OpenStackStateMapper.map(ResourceType.VOLUME, status);

			return new VolumeInstance(id, fogbowState, name, size);
		} catch (Exception e) {
			String errorMsg = "There was an exception while getting volume instance.";
			LOGGER.error(errorMsg, e);
			throw new UnexpectedException(errorMsg, e);
		}
	}

	protected String generateJsonEntityToCreateInstance(String size, String name) throws JSONException {
		CreateVolumeRequest createVolumeRequest =
				new CreateVolumeRequest.Builder()
				.name(name)
				.size(size)
				.build();
		
		return createVolumeRequest.toJson();
	}	
	
	private void initClient() {
		this.client = new HttpRequestClientUtil();
	}	
	
	public void setClient(HttpRequestClientUtil client) {
		this.client = client;
	}
}