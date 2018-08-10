package org.fogbowcloud.manager.core.plugins.cloud.openstack.attachment.v2;

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
import org.fogbowcloud.manager.core.models.instances.AttachmentInstance;
import org.fogbowcloud.manager.core.models.instances.InstanceState;
import org.fogbowcloud.manager.core.models.orders.AttachmentOrder;
import org.fogbowcloud.manager.core.models.tokens.OpenStackV3Token;
import org.fogbowcloud.manager.core.plugins.cloud.AttachmentPlugin;
import org.fogbowcloud.manager.core.plugins.cloud.openstack.OpenStackHttpToFogbowManagerExceptionMapper;
import org.fogbowcloud.manager.core.plugins.cloud.openstack.OpenStackStateMapper;
import org.fogbowcloud.manager.util.PropertiesUtil;
import org.fogbowcloud.manager.util.connectivity.HttpRequestClientUtil;
import org.fogbowcloud.manager.util.connectivity.HttpRequestUtil;
import org.json.JSONException;

public class OpenStackNovaV2AttachmentPlugin implements AttachmentPlugin<OpenStackV3Token> {

    private final String PROJECT_ID_IS_NOT_SPECIFIED_ERROR = "Project id is not specified.";
    protected static final String COMPUTE_NOVAV2_URL_KEY = "openstack_nova_v2_url";
    private static final String COMPUTE_V2_API_ENDPOINT = "/v2/";
    private static final String OS_VOLUME_ATTACHMENTS = "/os-volume_attachments";
    private static final String SEPARATOR_ID = " ";
    private static final String SERVERS = "/servers/";

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
    public String requestInstance(AttachmentOrder attachmentOrder, OpenStackV3Token openStackV3Token)
            throws FogbowManagerException, UnexpectedException {
        String tenantId = openStackV3Token.getProjectId();
        
        String serverId = attachmentOrder.getSource();
        String volumeId = attachmentOrder.getTarget();

        String jsonRequest = null;
        try {
            jsonRequest = generateJsonToAttach(volumeId);
        } catch (JSONException e) {
            String errorMsg = "An error occurred when generating json.";
            LOGGER.error(errorMsg, e);
            throw new InvalidParameterException(errorMsg, e);
        }
        
        String endpoint = getPrefixEndpoint(tenantId) + SERVERS + serverId + OS_VOLUME_ATTACHMENTS;
        try {
            this.client.doPostRequest(endpoint, openStackV3Token, jsonRequest);
        } catch (HttpResponseException e) {
            OpenStackHttpToFogbowManagerExceptionMapper.map(e);
        }
        return attachmentOrder.getSource() + SEPARATOR_ID + attachmentOrder.getTarget();
    }
    
    @Override
    public void deleteInstance(String instanceId, OpenStackV3Token openStackV3Token)
            throws FogbowManagerException, UnexpectedException {
        String tenantId = openStackV3Token.getProjectId();
        if (tenantId == null) {
            LOGGER.error(PROJECT_ID_IS_NOT_SPECIFIED_ERROR);
            throw new UnauthenticatedUserException(PROJECT_ID_IS_NOT_SPECIFIED_ERROR);
        }

        String[] separatorInstanceId = instanceId.split(SEPARATOR_ID);
        String serverId = separatorInstanceId[0];
        String volumeId = separatorInstanceId[1];
        String endpoint = getPrefixEndpoint(tenantId) + SERVERS + serverId + OS_VOLUME_ATTACHMENTS + "/" + volumeId;

        try {
            this.client.doDeleteRequest(endpoint, openStackV3Token);
        } catch (HttpResponseException e) {
            OpenStackHttpToFogbowManagerExceptionMapper.map(e);
        }
    }

    @Override
    public AttachmentInstance getInstance(String instanceId, OpenStackV3Token openStackV3Token)
            throws FogbowManagerException, UnexpectedException {
        LOGGER.info("Getting instance " + instanceId + " with tokens " + openStackV3Token);
    	String tenantId = openStackV3Token.getProjectId();
    	
    	String[] separatorInstanceId = instanceId.split(SEPARATOR_ID);
    	
    	// this variable refers to computeInstanceId received in the first part of the vector
    	String serverId = separatorInstanceId[0];
    	
    	// this variable refers to volumeInstanceId received in the second part of the vector
    	String volumeId = separatorInstanceId[1];
        
        String requestEndpoint = getPrefixEndpoint(tenantId) + SERVERS + serverId + OS_VOLUME_ATTACHMENTS + "/" + volumeId;

        String jsonResponse = null;
        try {
            jsonResponse = this.client.doGetRequest(requestEndpoint, openStackV3Token);
        } catch (HttpResponseException e) {
            OpenStackHttpToFogbowManagerExceptionMapper.map(e);
        }

        LOGGER.debug("Getting instance from json: " + jsonResponse);        
        AttachmentInstance attachmentInstance = getInstanceFromJson(jsonResponse);
               
        return attachmentInstance;
    }
    
    protected AttachmentInstance getInstanceFromJson(String jsonResponse) throws UnexpectedException {
    	try {
    		GetAttachmentResponse getAttachmentResponse = GetAttachmentResponse.fromJson(jsonResponse);
            String id = getAttachmentResponse.getId();
            String serverId = getAttachmentResponse.getServerId();
            String volumeId = getAttachmentResponse.getVolumeId();
            String device = getAttachmentResponse.getDevice();

        	// There is no OpenStackState for attachments; we set it to empty string to allow its mapping
            // by the OpenStackStateMapper.map() function.
            String openStackState = "";
            InstanceState fogbowState = OpenStackStateMapper.map(ResourceType.ATTACHMENT, openStackState);

            AttachmentInstance attachmentInstance = new AttachmentInstance(id,fogbowState, serverId, volumeId, device);
        	return attachmentInstance;
        	
    	} catch (JSONException e) {
            String errorMsg = "There was an exception while getting attchment instance from json.";
    		LOGGER.error(errorMsg, e);
        	throw new UnexpectedException(errorMsg, e);
    	}
    }

    private String getPrefixEndpoint(String tenantId) {
        return this.properties.getProperty(COMPUTE_NOVAV2_URL_KEY) + COMPUTE_V2_API_ENDPOINT + tenantId;
    }

    protected String generateJsonToAttach(String volume) throws JSONException {
    	CreateAttachmentRequest createAttachmentRequest = new CreateAttachmentRequest.Builder()
    			.volumeId(volume)
    			.build();
        return createAttachmentRequest.toJson();
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
