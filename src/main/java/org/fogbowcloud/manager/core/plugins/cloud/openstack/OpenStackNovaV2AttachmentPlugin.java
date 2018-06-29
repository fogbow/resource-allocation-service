package org.fogbowcloud.manager.core.plugins.cloud.openstack;

import java.io.File;
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
import org.fogbowcloud.manager.core.HomeDir;
import org.fogbowcloud.manager.core.exceptions.FatalErrorException;
import org.fogbowcloud.manager.core.exceptions.FogbowManagerException;
import org.fogbowcloud.manager.core.plugins.cloud.AttachmentPlugin;
import org.fogbowcloud.manager.core.models.ErrorType;
import org.fogbowcloud.manager.core.models.RequestHeaders;
import org.fogbowcloud.manager.core.models.ResponseConstants;
import org.fogbowcloud.manager.core.models.ErrorResponse;
import org.fogbowcloud.manager.core.models.ErrorResponseMap;
import org.fogbowcloud.manager.core.models.orders.AttachmentOrder;
import org.fogbowcloud.manager.core.models.instances.InstanceState;
import org.fogbowcloud.manager.core.models.instances.AttachmentInstance;
import org.fogbowcloud.manager.core.models.token.Token;
import org.fogbowcloud.manager.utils.HttpRequestUtil;
import org.fogbowcloud.manager.utils.PropertiesUtil;
import org.json.JSONException;
import org.json.JSONObject;
import org.apache.log4j.Logger;

public class OpenStackNovaV2AttachmentPlugin implements AttachmentPlugin {

    private static final String OPENSTACK_NOVAV2_ATTACHMENT_PLUGIN_CONF = "openstack-nova-attachment-plugin.conf";

    private static final String COMPUTE_NOVAV2_URL_KEY = "openstack_nova_v2_url";

    private static final String COMPUTE_V2_API_ENDPOINT = "/v2/";
	private static final String ID_JSON_FIELD = "id";
    private static final String OS_VOLUME_ATTACHMENTS = "/os-volume_attachments";
    private static final String SEPARATOR_ID = " ";
    private static final String SERVERS = "/servers/";
    private static final String TENANT_ID = "tenantId";
    
    private final Logger LOGGER = Logger.getLogger(OpenStackNovaV2AttachmentPlugin.class);
    
    private Properties properties;
    private HttpClient client;
    
    public OpenStackNovaV2AttachmentPlugin() throws FatalErrorException {
        HomeDir homeDir = HomeDir.getInstance();
        this.properties = PropertiesUtil.readProperties(homeDir.getPath() +
                File.separator + OPENSTACK_NOVAV2_ATTACHMENT_PLUGIN_CONF);
        initClient();
    }

    @Override
    public String requestInstance(AttachmentOrder attachmentOrder, Token localToken) throws FogbowManagerException {
        String tenantId = getTenantId(localToken);
        
        String serverId = attachmentOrder.getSource();
        String volumeId = attachmentOrder.getTarget();        

        JSONObject jsonRequest = null;
        try {
            jsonRequest = generateJsonToAttach(volumeId);
        } catch (JSONException e) {
            LOGGER.error("An error occurred when generating json.", e);
            throw new FogbowManagerException(ErrorType.BAD_REQUEST, ResponseConstants.IRREGULAR_SYNTAX);
        }      
        
        String endpoint = getPrefixEndpoint(tenantId) + SERVERS + serverId + OS_VOLUME_ATTACHMENTS;
        String responseStr = doPostRequest(endpoint, localToken.getAccessId(), jsonRequest);
        
        return getAttachmentIdJson(responseStr);
    }
    
    @Override
    public void deleteInstance(String instanceId, Token localToken) throws FogbowManagerException {
        String tenantId = getTenantId(localToken);
        
        String[] separatorInstanceId = instanceId.split(SEPARATOR_ID);
        
        String serverId = separatorInstanceId[0];
        String volumeId = separatorInstanceId[1];
        
        String endpoint = getPrefixEndpoint(tenantId) + SERVERS + serverId + OS_VOLUME_ATTACHMENTS + "/" + volumeId;
        doDeleteRequest(endpoint, localToken.getAccessId());
    }

    @Override
    public AttachmentInstance getInstance(String instanceId, Token localToken) throws FogbowManagerException {
        LOGGER.info("Getting instance " + instanceId + " with token " + localToken);
    	String tenantId = getTenantId(localToken);
    	
    	String[] separatorInstanceId = instanceId.split(SEPARATOR_ID);
    	
    	/** this variable refers to computeInstanceId received in the first part of the vector */
    	String serverId = separatorInstanceId[0];
    	
    	/** this variable refers to volumeInstanceId received in the second part of the vector */
    	String volumeId = separatorInstanceId[1];
        
        String requestEndpoint = getPrefixEndpoint(tenantId) + SERVERS + serverId + OS_VOLUME_ATTACHMENTS + "/" + volumeId;
        String jsonResponse = doGetRequest(requestEndpoint, localToken);
        
        LOGGER.debug("Getting instance from json: " + jsonResponse);        
        AttachmentInstance attachmentInstance = getInstanceFromJson(jsonResponse);
               
        return attachmentInstance;
    }
    
    protected AttachmentInstance getInstanceFromJson(String jsonResponse) throws FogbowManagerException {
    	try {
        	JSONObject rootServer = new JSONObject(jsonResponse);
        	rootServer = rootServer.getJSONObject("volumeAttachment");
        	
        	String id = rootServer.getString(ID_JSON_FIELD);
        	String serverId = rootServer.getString("serverId");
        	String volumeId = rootServer.getString("volumeId");
        	String device = rootServer.getString("device");

        	AttachmentInstance attachmentInstance = new AttachmentInstance(id, InstanceState.READY, serverId, volumeId, device);
        	return attachmentInstance;
        	
    	} catch (JSONException e) {
    		LOGGER.warn("There was an exception while getting instances from json", e);
        	throw new FogbowManagerException();
    	}
    }

    protected String doGetRequest(String endpoint, Token localToken) throws FogbowManagerException {
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
            throw new FogbowManagerException(ErrorType.BAD_REQUEST, ResponseConstants.IRREGULAR_SYNTAX);
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
            return root.getJSONObject("volumeAttachment").getString("volumeId").toString();
        } catch (JSONException e) {
            return null;
        }
    }
    
    private void doDeleteRequest(String endpoint, String federationTokenValue) throws FogbowManagerException {
        LOGGER.debug("Doing DELETE request to OpenStack on endpoint <" + endpoint + ">");

        HttpResponse response = null;

        try {
            HttpDelete request = new HttpDelete(endpoint);
            request.addHeader(RequestHeaders.X_AUTH_TOKEN.getValue(), federationTokenValue);
            response = this.client.execute(request);
        } catch (Exception e) {
            LOGGER.error("Unable to complete the DELETE request: ", e);
            throw new FogbowManagerException(ErrorType.BAD_REQUEST, ResponseConstants.IRREGULAR_SYNTAX);
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
    
    protected String doPostRequest(String endpoint, String federationTokenValue, JSONObject jsonRequest) throws FogbowManagerException {
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
            throw new FogbowManagerException(ErrorType.BAD_REQUEST, ResponseConstants.IRREGULAR_SYNTAX);
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
    
    private void checkStatusResponse(HttpResponse response, String message) throws FogbowManagerException {
        LOGGER.debug("Checking status response...");

        ErrorResponseMap errorResponseMap = new ErrorResponseMap(response, message);
        Integer statusCode = response.getStatusLine().getStatusCode();
        ErrorResponse errorResponse = errorResponseMap.getStatusResponse(statusCode);

        if (errorResponse != null) {
            throw new FogbowManagerException(errorResponse.getErrorType(), errorResponse.getResponseConstants());
        }
    }
    
    private String getPrefixEndpoint(String tenantId) {
        return this.properties.getProperty(COMPUTE_NOVAV2_URL_KEY) + COMPUTE_V2_API_ENDPOINT + tenantId;
    }

    protected JSONObject generateJsonToAttach(String volume) throws JSONException {
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
    
    private void initClient() {
        HttpRequestUtil.init();
        this.client = HttpRequestUtil.createHttpClient();
    }

    protected void setClient(HttpClient client) {
        this.client = client;
    }
    
}
