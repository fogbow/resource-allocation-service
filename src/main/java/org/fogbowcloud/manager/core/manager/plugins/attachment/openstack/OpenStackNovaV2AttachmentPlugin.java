package org.fogbowcloud.manager.core.manager.plugins.attachment.openstack;

import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.Properties;
import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpDelete;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.StringEntity;
import org.apache.http.util.EntityUtils;
import org.fogbowcloud.manager.core.exceptions.RequestException;
import org.fogbowcloud.manager.core.manager.constants.OpenStackConfigurationConstants;
import org.fogbowcloud.manager.core.manager.plugins.InstanceStateMapper;
import org.fogbowcloud.manager.core.manager.plugins.attachment.AttachmentPlugin;
import org.fogbowcloud.manager.core.models.ErrorType;
import org.fogbowcloud.manager.core.models.RequestHeaders;
import org.fogbowcloud.manager.core.models.ResponseConstants;
import org.fogbowcloud.manager.core.models.StatusResponse;
import org.fogbowcloud.manager.core.models.StatusResponseMap;
import org.fogbowcloud.manager.core.models.orders.AttachmentOrder;
import org.fogbowcloud.manager.core.models.orders.Order;
import org.fogbowcloud.manager.core.models.orders.instances.InstanceState;
import org.fogbowcloud.manager.core.models.orders.instances.AttachmentInstance;
import org.fogbowcloud.manager.core.models.token.Token;
import org.json.JSONException;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class OpenStackNovaV2AttachmentPlugin implements AttachmentPlugin {

    private static final String COMPUTE_V2_API_ENDPOINT = "/v2/";
	private static final String ID_JSON_FIELD = "id";
    private static final String OS_VOLUME_ATTACHMENTS = "/os-volume_attachments";
    private static final String SEPARATOR = "/";
    private static final String SERVERS = "/servers/";
	private static final String SERVER_JSON_FIELD = "server";
	private static final String STATUS_JSON_FIELD = "status";
    private static final String TENANT_ID = "tenantId";
    
    private final Logger LOGGER = LoggerFactory.getLogger(OpenStackNovaV2AttachmentPlugin.class);
    
    private Properties properties;
    private HttpClient client;
	private InstanceStateMapper instanceStateMapper;
    
    public OpenStackNovaV2AttachmentPlugin(Properties properties) {
        this.properties = properties;
    }

    @Override
    public String attachVolume(Token localToken, AttachmentOrder attachmentOrder) throws RequestException {
        String tenantId = getTenantId(localToken);
        
        String serverId = attachmentOrder.getSource();
        String volumeId = attachmentOrder.getTarget();        
        String mountPoint = attachmentOrder.getDevice();
        
        JSONObject jsonRequest = null;
        try {           
            jsonRequest = generateJsonToAttach(volumeId, mountPoint);
        } catch (JSONException e) {
            LOGGER.error("An error occurred when generating json.", e);
            throw new RequestException(ErrorType.BAD_REQUEST, ResponseConstants.IRREGULAR_SYNTAX);
        }      
        
        String endpoint = getPrefixEndpoint(tenantId) + SERVERS + serverId + OS_VOLUME_ATTACHMENTS;
        String responseStr = doPostRequest(endpoint, localToken.getAccessId(), jsonRequest);
        
        return getAttachmentIdJson(responseStr);
    }
    
    @Override
    public void detachVolume(Token localToken, Order order) throws RequestException {
        AttachmentOrder attachmentOrder = (AttachmentOrder) order;
        String tenantId = getTenantId(localToken);
        
        String serverId = attachmentOrder.getSource();
        String volumeId = attachmentOrder.getTarget();
        
        String endpoint = getPrefixEndpoint(tenantId) + SERVERS + serverId + OS_VOLUME_ATTACHMENTS + SEPARATOR + volumeId;
        doDeleteRequest(endpoint, localToken.getAccessId());
    }

    @Override
    public AttachmentInstance getAttachment(Token localToken, Order order ) throws RequestException {
    	LOGGER.info("Getting instance " + order.getInstanceId() + " with token " + localToken);
    	AttachmentOrder attachmentOrder = (AttachmentOrder) order;
    	String tenantId = getTenantId(localToken);
        
        String serverId = attachmentOrder.getSource();
        String volumeId = attachmentOrder.getTarget();
        
        String requestEndpoint = getPrefixEndpoint(tenantId) + SERVERS + serverId + OS_VOLUME_ATTACHMENTS + SEPARATOR + volumeId;
        String jsonResponse = doGetRequest(requestEndpoint, localToken);
        
        LOGGER.debug("Getting instance from json: " + jsonResponse);        
        AttachmentInstance attachmentInstance = getInstanceFromJson(jsonResponse);
               
        return attachmentInstance;
    }
    
    private AttachmentInstance getInstanceFromJson(String jsonResponse) throws RequestException {
    	try {
        	JSONObject rootServer = new JSONObject(jsonResponse);
        	JSONObject serverJson = rootServer.getJSONObject(SERVER_JSON_FIELD);
            
        	// TODO Implementation incomplete, get more information about the interface 'InstanceStateMapper' 
        	String id = serverJson.getString(ID_JSON_FIELD);
        	InstanceState state = instanceStateMapper.getInstanceState(serverJson.getString(STATUS_JSON_FIELD));
        	String serverId = "";
        	String volumeId = "";
        	String device = "";

        	AttachmentInstance attachmentInstance = new AttachmentInstance(id, state, serverId, volumeId, device);
        	return attachmentInstance;
        	
    	} catch (JSONException e) {
    		LOGGER.warn("There was an exception while getting instances from json", e);
        	throw new RequestException();
    	}
    }

    private String doGetRequest(String endpoint, Token localToken) throws RequestException {
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

    private String getAttachmentIdJson(String responseStr) {
        try {
            JSONObject root = new JSONObject(responseStr);
            return root.getJSONObject("volumeAttachment").getString("id").toString();
        } catch (JSONException e) {
            return null;
        }
    }
    
    private void doDeleteRequest(String endpoint, String federationTokenValue) throws RequestException {
        LOGGER.debug("Doing DELETE request to OpenStack on endpoint <" + endpoint + ">");

        HttpResponse response = null;

        try {
            HttpDelete request = new HttpDelete(endpoint);
            request.addHeader(RequestHeaders.X_AUTH_TOKEN.getValue(), federationTokenValue);
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
        
        // TODO delete message does not have message
        checkStatusResponse(response, "");
    }
    
    private String doPostRequest(String endpoint, String federationTokenValue, JSONObject jsonRequest) throws RequestException {
        LOGGER.debug("Doing POST request to OpenStack for creating an instance");
        HttpResponse response = null;
        String responseStr = null;
        try {
            HttpPost request = new HttpPost(endpoint);
            request.addHeader(
                    RequestHeaders.CONTENT_TYPE.getValue(),
                    RequestHeaders.JSON_CONTENT_TYPE.getValue());
            request.addHeader(
                    RequestHeaders.ACCEPT.getValue(), RequestHeaders.JSON_CONTENT_TYPE.getValue());
            request.addHeader(RequestHeaders.X_AUTH_TOKEN.getValue(), federationTokenValue);

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
    
    private void checkStatusResponse(HttpResponse response, String message) throws RequestException {
        LOGGER.debug("Checking status response...");

        StatusResponseMap statusResponseMap = new StatusResponseMap(response, message);
        Integer statusCode = response.getStatusLine().getStatusCode();
        StatusResponse statusResponse = statusResponseMap.getStatusResponse(statusCode);

        if (statusResponse != null) {
            throw new RequestException(statusResponse.getErrorType(), statusResponse.getResponseConstants());
        }
    }
    
    private String getPrefixEndpoint(String tenantId) {
        return this.properties.getProperty(OpenStackConfigurationConstants.COMPUTE_NOVAV2_URL_KEY) + COMPUTE_V2_API_ENDPOINT + tenantId;
    }

    protected JSONObject generateJsonToAttach(String volume, String mountpoint) throws JSONException {
        JSONObject osAttachContent = new JSONObject();
        osAttachContent.put("volumeId", volume);

        JSONObject osAttach = new JSONObject();
        osAttach.put("volumeAttachment", osAttachContent);
        
        return osAttach;
    }
    
    protected String getTenantId(Token localToken) {
        Map<String, String> tokenAttributes = localToken.getAttributes();
        return tokenAttributes.get(TENANT_ID);
    }

}
