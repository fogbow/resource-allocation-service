package org.fogbowcloud.manager.core.manager.plugins.volume.openstack;

import java.nio.charset.StandardCharsets;
import java.util.Properties;

import org.apache.http.HttpResponse;
import org.apache.http.HttpStatus;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpDelete;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.StringEntity;
import org.apache.http.util.EntityUtils;
import org.apache.log4j.Logger;
import org.fogbowcloud.manager.core.exceptions.RequestException;
import org.fogbowcloud.manager.core.manager.plugins.volume.VolumePlugin;
import org.fogbowcloud.manager.core.models.ErrorType;
import org.fogbowcloud.manager.core.models.RequestHeaders;
import org.fogbowcloud.manager.core.models.ResponseConstants;
import org.fogbowcloud.manager.core.models.orders.instances.InstanceState;
import org.fogbowcloud.manager.core.models.orders.instances.StorageOrderInstance;
import org.fogbowcloud.manager.core.models.token.Token;
import org.fogbowcloud.manager.utils.HttpRequestUtil;
import org.json.JSONException;
import org.json.JSONObject;

// TODO implement tests
// TODO review all Exceptions
public class OpenStackV2VolumePlugin implements VolumePlugin {

	private static final String COULD_NOT_CONSUME_ENTITY = "Could not consume entity";
	
	protected static final String KEY_JSON_INSTANCE_UUID = "instance_uuid";
	protected static final String KEY_JSON_MOUNTPOINT = "mountpoint";
	protected static final String KEY_JSON_VOLUMES = "volumes";
	protected static final String KEY_JSON_VOLUME = "volume";
	protected static final String KEY_JSON_STATUS = "status";
	protected static final String KEY_JSON_SIZE = "size";
	protected static final String KEY_JSON_NAME = "name";
	protected static final String KEY_JSON_ID = "id";
	
	private static final String VALUE_AVAILABLE_STATUS = "available";
	
	protected static final String SUFIX_ENDPOINT_VOLUMES = "/volumes";
	protected static final String V2_API_ENDPOINT = "/v2/";
	
	// TODO put in the properties examples
	public static final String VOLUME_NOVAV2_URL_KEY = "volume_v2_url";

	// TODO change to a common class. KeystoneV3IdentityPlugin and
	// OpenStackV2VolumePlugin class have the same attr
	protected static final String TENANT_ID = "tenantId";

	private HttpClient client;
	private String volumeV2APIEndpoint;

	private static final Logger LOGGER = Logger.getLogger(OpenStackV2VolumePlugin.class);

	public OpenStackV2VolumePlugin(Properties properties) {
		this.volumeV2APIEndpoint = properties.getProperty(VOLUME_NOVAV2_URL_KEY) + V2_API_ENDPOINT;

		initClient();
	}
	
	@Override
	public String requestInstance(Token localToken, StorageOrderInstance storageOrderInstance) throws RequestException {
		String tenantId = localToken.getAttributes().get(TENANT_ID);
		if (tenantId == null) {
			// TODO check this exception. Put a message
			throw new RequestException(ErrorType.BAD_REQUEST, "");
		}
		String size = String.valueOf(storageOrderInstance.getSize());

		JSONObject jsonRequest = null;
		try {
			jsonRequest = generateJsonEntityToCreateInstance(size);
		} catch (JSONException e) {
			LOGGER.error("An error occurred when generating json.", e);
			// TODO check this exception. Put a message
			throw new RequestException(ErrorType.BAD_REQUEST, "");
		}

		String endpoint = this.volumeV2APIEndpoint + tenantId + SUFIX_ENDPOINT_VOLUMES;
		String responseStr = doPostRequest(endpoint, localToken.getAccessId(), jsonRequest);
		StorageOrderInstance instanceFromJson = getInstanceFromJson(responseStr);
		return instanceFromJson != null ? instanceFromJson.getId() : null;
	}

	@Override
	public StorageOrderInstance getInstance(Token localToken, String storageOrderInstanceId) throws RequestException {
		String tenantId = localToken.getAttributes().get(TENANT_ID);
		if (tenantId == null) {
			// TODO check this exception. Put a message
			throw new RequestException(ErrorType.BAD_REQUEST, "");
		}		
		
		String endpoint = this.volumeV2APIEndpoint + tenantId 
				+ SUFIX_ENDPOINT_VOLUMES + "/" + storageOrderInstanceId;
		String responseStr = doGetRequest(endpoint, localToken.getAccessId());
		return getInstanceFromJson(responseStr);
	}

	@Override
	public void removeInstance(Token localToken, String storageOrderInstanceId) throws RequestException {
		String tenantId = localToken.getAttributes().get(TENANT_ID);
		if (tenantId == null) {
			// TODO check this exception. Put a message			
			throw new RequestException(ErrorType.BAD_REQUEST, "");
		}		
		
		String endpoint = this.volumeV2APIEndpoint + tenantId 
				+ SUFIX_ENDPOINT_VOLUMES + "/" + storageOrderInstanceId;
		doDeleteRequest(endpoint, localToken.getAccessId());
	}

	// TODO change to a common class. KeystoneV3IdentityPlugin and OpenStackV2VolumePlugin class have the same method	
	protected String doPostRequest(String endpoint, String authToken, JSONObject json) throws RequestException {
		HttpResponse response = null;
		String responseStr = null;
		try {
			HttpPost request = new HttpPost(endpoint);
            request.addHeader(RequestHeaders.CONTENT_TYPE.getValue(),
                    RequestHeaders.JSON_CONTENT_TYPE.getValue());
            request.addHeader(RequestHeaders.ACCEPT.getValue(), 
            		RequestHeaders.JSON_CONTENT_TYPE.getValue());
            request.addHeader(RequestHeaders.X_AUTH_TOKEN.getValue(), authToken);
			request.setEntity(new StringEntity(json.toString(), StandardCharsets.UTF_8));
			response = this.client.execute(request);
			responseStr = EntityUtils.toString(response.getEntity(), StandardCharsets.UTF_8);
		} catch (Exception e) {
			String errorMsg = "Could not make POST request";
			LOGGER.error(errorMsg, e);
			// TODO check this exception.
			throw new RequestException(ErrorType.BAD_REQUEST, errorMsg);
		} finally {
			try {
				EntityUtils.consume(response.getEntity());
			} catch (Throwable t) {
				LOGGER.warn(COULD_NOT_CONSUME_ENTITY, t);
			}
		}
		checkStatusResponse(response, responseStr);
		return responseStr;
	}	
	
	// TODO change to a common class. KeystoneV3IdentityPlugin and OpenStackV2VolumePlugin class have the same method	
	protected String doGetRequest(String endpoint, String authToken) throws RequestException {
		HttpResponse response = null;
		String responseStr = null;
		try {
			HttpGet request = new HttpGet(endpoint);			
            request.addHeader(RequestHeaders.CONTENT_TYPE.getValue(),
                    RequestHeaders.JSON_CONTENT_TYPE.getValue());
            request.addHeader(RequestHeaders.ACCEPT.getValue(), 
            		RequestHeaders.JSON_CONTENT_TYPE.getValue());
            request.addHeader(RequestHeaders.X_AUTH_TOKEN.getValue(), authToken);
			response = this.client.execute(request);
			responseStr = EntityUtils.toString(response.getEntity(), StandardCharsets.UTF_8);
		} catch (Exception e) {
			String errorMsg = "Could not make GET request";
			LOGGER.error(errorMsg, e);
			// TODO check this exception.
			throw new RequestException(ErrorType.BAD_REQUEST, errorMsg);
		} finally {
			try {
				EntityUtils.consume(response.getEntity());
			} catch (Throwable t) {
				LOGGER.warn(COULD_NOT_CONSUME_ENTITY, t);
			}
		}
		checkStatusResponse(response, responseStr);
		return responseStr;
	}
	
	protected void doDeleteRequest(String endpoint, String authToken) throws RequestException {
		HttpResponse response = null;
		try {
			HttpDelete request = new HttpDelete(endpoint);
			request.addHeader(RequestHeaders.X_AUTH_TOKEN.getValue(), authToken);
			response = this.client.execute(request);
		} catch (Exception e) {
			String errorMsg = "Could not make DELETE request";
			LOGGER.error(errorMsg, e);
			throw new RequestException(ErrorType.BAD_REQUEST, errorMsg);
		} finally {
			try {
				EntityUtils.consume(response.getEntity());
			} catch (Throwable t) {
				LOGGER.warn(COULD_NOT_CONSUME_ENTITY, t);
			}
		}
		String emptyMessage = "";
		checkStatusResponse(response, emptyMessage);
	}	
	
	// TODO change to a common class. KeystoneV3IdentityPlugin and OpenStackV2VolumePlugin class have the same method
	// TODO check this method. Why is necessary ?
	private void checkStatusResponse(HttpResponse response, String message) throws RequestException {
		if (response.getStatusLine().getStatusCode() == HttpStatus.SC_UNAUTHORIZED) {
			throw new RequestException(ErrorType.UNAUTHORIZED, "");
		} else if (response.getStatusLine().getStatusCode() == HttpStatus.SC_NOT_FOUND) {
			throw new RequestException(ErrorType.NOT_FOUND, "");
		} else if (response.getStatusLine().getStatusCode() == HttpStatus.SC_BAD_REQUEST) {
			throw new RequestException(ErrorType.BAD_REQUEST, message);
		} else if (response.getStatusLine().getStatusCode() == HttpStatus.SC_REQUEST_TOO_LONG) {
			if (message.contains(ResponseConstants.QUOTA_EXCEEDED_FOR_INSTANCES)) {
				throw new RequestException(ErrorType.QUOTA_EXCEEDED,
						ResponseConstants.QUOTA_EXCEEDED_FOR_INSTANCES);
			}
			throw new RequestException(ErrorType.BAD_REQUEST, message);
		}
		else if (response.getStatusLine().getStatusCode() > 204) {
			throw new RequestException(ErrorType.BAD_REQUEST, 
					"Status code: " + response.getStatusLine().toString() + " | Message:" + message);
		}
	}	
	
	protected StorageOrderInstance getInstanceFromJson(String json) throws RequestException {
		try {
			JSONObject rootServer = new JSONObject(json);
			JSONObject volumeJson = rootServer.getJSONObject(KEY_JSON_VOLUME);
			String id = volumeJson.getString(KEY_JSON_ID);
			
			String name = volumeJson.optString(KEY_JSON_NAME);
			String statusOpenstack = volumeJson.optString(KEY_JSON_STATUS);
			InstanceState status = getInstanceState(statusOpenstack);
			String sizeStr = volumeJson.optString(KEY_JSON_SIZE);
			int size = Integer.valueOf(sizeStr);

			// TODO check if is necessary to use "int" in the size attr
			return new StorageOrderInstance(id, name, status, size);
		} catch (Exception e) {
			LOGGER.error("There was an exception while getting instance storage.", e);
			throw new RequestException(ErrorType.BAD_REQUEST, "");
		}
	}
	
	// TODO check openstack documentation. https://developer.openstack.org/api-ref/block-storage/v2/
	// TODO check what are the InstanceStates to Volume. 
	protected InstanceState getInstanceState(String statusOpenstack) {
        switch (statusOpenstack.toLowerCase()) {
	        case VALUE_AVAILABLE_STATUS:
	            return InstanceState.ACTIVE;
	        default:
	            return InstanceState.INACTIVE;            
        }
	}

	protected JSONObject generateJsonEntityToCreateInstance(String size) throws JSONException {
		JSONObject volumeContent = new JSONObject();
		volumeContent.put(KEY_JSON_SIZE, size);

		JSONObject volume = new JSONObject();
		volume.put(KEY_JSON_VOLUME, volumeContent);
		
		return volume;
	}	
	
	private void initClient() {
		this.client = HttpRequestUtil.createHttpClient();
	}	
	
	public void setClient(HttpClient client) {
		this.client = client;
	}
}