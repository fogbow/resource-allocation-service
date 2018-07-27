package org.fogbowcloud.manager.core.plugins.cloud.openstack;

import java.io.File;
import java.util.Map;
import java.util.Properties;
import org.apache.http.client.HttpResponseException;
import org.fogbowcloud.manager.core.HomeDir;
import org.fogbowcloud.manager.core.constants.DefaultConfigurationConstants;
import org.fogbowcloud.manager.core.exceptions.*;
import org.fogbowcloud.manager.core.models.instances.InstanceState;
import org.fogbowcloud.manager.core.models.instances.InstanceType;
import org.fogbowcloud.manager.core.plugins.cloud.AttachmentPlugin;
import org.fogbowcloud.manager.core.models.orders.AttachmentOrder;
import org.fogbowcloud.manager.core.models.instances.AttachmentInstance;
import org.fogbowcloud.manager.core.models.tokens.Token;
import org.fogbowcloud.manager.util.connectivity.HttpRequestClientUtil;
import org.fogbowcloud.manager.util.connectivity.HttpRequestUtil;
import org.fogbowcloud.manager.util.PropertiesUtil;
import org.json.JSONException;
import org.json.JSONObject;
import org.apache.log4j.Logger;

public class OpenStackNovaV2AttachmentPlugin implements AttachmentPlugin {

    private final String TENANT_ID_IS_NOT_SPECIFIED_ERROR = "Tenant id is not specified.";
    protected static final String COMPUTE_NOVAV2_URL_KEY = "openstack_nova_v2_url";
    private static final String COMPUTE_V2_API_ENDPOINT = "/v2/";
	private static final String ID_JSON_FIELD = "id";
    private static final String OS_VOLUME_ATTACHMENTS = "/os-volume_attachments";
    private static final String SEPARATOR_ID = " ";
    private static final String SERVERS = "/servers/";
    private static final String TENANT_ID = "tenantId";

    private final Logger LOGGER = Logger.getLogger(OpenStackNovaV2AttachmentPlugin.class);
    
    private Properties properties;
    private HttpRequestClientUtil client;
    
    public OpenStackNovaV2AttachmentPlugin() throws FatalErrorException {
        HomeDir homeDir = HomeDir.getInstance();
        this.properties = PropertiesUtil.readProperties(homeDir.getPath() + File.separator
                + DefaultConfigurationConstants.OPENSTACK_CONF_FILE_NAME);
        initClient();
    }

    @Override
    public String requestInstance(AttachmentOrder attachmentOrder, Token localToken)
            throws FogbowManagerException, UnexpectedException {
        String tenantId = getTenantId(localToken);
        
        String serverId = attachmentOrder.getSource();
        String volumeId = attachmentOrder.getTarget();

        JSONObject jsonRequest = null;
        try {
            jsonRequest = generateJsonToAttach(volumeId);
        } catch (JSONException e) {
            String errorMsg = "An error occurred when generating json.";
            LOGGER.error(errorMsg, e);
            throw new InvalidParameterException(errorMsg, e);
        }
        
        String endpoint = getPrefixEndpoint(tenantId) + SERVERS + serverId + OS_VOLUME_ATTACHMENTS;
        String responseStr = null;
        try {
            responseStr = this.client.doPostRequest(endpoint, localToken, jsonRequest);
        } catch (HttpResponseException e) {
            OpenStackHttpToFogbowManagerExceptionMapper.map(e);
        }
        return getAttachmentIdJson(responseStr);
    }
    
    @Override
    public void deleteInstance(String instanceId, Token localToken)
            throws FogbowManagerException, UnexpectedException {
        String tenantId = getTenantId(localToken);
        if (tenantId == null) {
            LOGGER.error(TENANT_ID_IS_NOT_SPECIFIED_ERROR);
            throw new UnauthenticatedUserException(TENANT_ID_IS_NOT_SPECIFIED_ERROR);
        }

        String[] separatorInstanceId = instanceId.split(SEPARATOR_ID);
        String serverId = separatorInstanceId[0];
        String volumeId = separatorInstanceId[1];
        String endpoint = getPrefixEndpoint(tenantId) + SERVERS + serverId + OS_VOLUME_ATTACHMENTS + "/" + volumeId;

        try {
            this.client.doDeleteRequest(endpoint, localToken);
        } catch (HttpResponseException e) {
            OpenStackHttpToFogbowManagerExceptionMapper.map(e);
        }
    }

    @Override
    public AttachmentInstance getInstance(String instanceId, Token localToken)
            throws FogbowManagerException, UnexpectedException {
        LOGGER.info("Getting instance " + instanceId + " with tokens " + localToken);
    	String tenantId = getTenantId(localToken);
    	
    	String[] separatorInstanceId = instanceId.split(SEPARATOR_ID);
    	
    	/** this variable refers to computeInstanceId received in the first part of the vector */
    	String serverId = separatorInstanceId[0];
    	
    	/** this variable refers to volumeInstanceId received in the second part of the vector */
    	String volumeId = separatorInstanceId[1];
        
        String requestEndpoint = getPrefixEndpoint(tenantId) + SERVERS + serverId + OS_VOLUME_ATTACHMENTS + "/" + volumeId;

        String jsonResponse = null;
        try {
            jsonResponse = this.client.doGetRequest(requestEndpoint, localToken);
        } catch (HttpResponseException e) {
            OpenStackHttpToFogbowManagerExceptionMapper.map(e);
        }

        LOGGER.debug("Getting instance from json: " + jsonResponse);        
        AttachmentInstance attachmentInstance = getInstanceFromJson(jsonResponse);
               
        return attachmentInstance;
    }
    
    protected AttachmentInstance getInstanceFromJson(String jsonResponse) throws UnexpectedException {
    	try {
            JSONObject rootServer = new JSONObject(jsonResponse);
        	rootServer = rootServer.getJSONObject("volumeAttachment");
        	
        	String id = rootServer.getString(ID_JSON_FIELD);
        	String serverId = rootServer.getString("serverId");
        	String volumeId = rootServer.getString("volumeId");
        	String device = rootServer.getString("device");

        	// There is no OpenStackState for attachments; we set it to empty string to allow its mapping
            // by the OpenStackStateMapper.map() function.
            String openStackState = "";
            InstanceState fogbowState = OpenStackStateMapper.map(InstanceType.ATTACHMENT, openStackState);

            AttachmentInstance attachmentInstance = new AttachmentInstance(id,fogbowState, serverId, volumeId, device);
        	return attachmentInstance;
        	
    	} catch (JSONException e) {
            String errorMsg = "There was an exception while getting attchment instance from json.";
    		LOGGER.error(errorMsg, e);
        	throw new UnexpectedException(errorMsg, e);
    	}
    }

    private String getAttachmentIdJson(String responseStr) throws UnexpectedException {
        try {
            JSONObject root = new JSONObject(responseStr);
            return root.getJSONObject("volumeAttachment").getString("volumeId").toString();
        } catch (JSONException e) {
            String errorMsg = "There was an exception while getting attchment instance.";
            LOGGER.error(errorMsg, e);
            throw new UnexpectedException(errorMsg, e);        }
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
        this.client = new HttpRequestClientUtil();
    }

    protected void setClient(HttpRequestClientUtil client) {
        this.client = client;
    }
    
    protected void setProperties(Properties properties) {
        this.properties = properties;
    }
}
