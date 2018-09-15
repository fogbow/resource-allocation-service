package org.fogbowcloud.ras.core.plugins.interoperability.openstack.attachment.v2;

import org.apache.http.client.HttpResponseException;
import org.apache.log4j.Logger;
import org.fogbowcloud.ras.core.HomeDir;
import org.fogbowcloud.ras.core.constants.DefaultConfigurationConstants;
import org.fogbowcloud.ras.core.constants.Messages;
import org.fogbowcloud.ras.core.exceptions.*;
import org.fogbowcloud.ras.core.models.ResourceType;
import org.fogbowcloud.ras.core.models.instances.AttachmentInstance;
import org.fogbowcloud.ras.core.models.instances.InstanceState;
import org.fogbowcloud.ras.core.models.orders.AttachmentOrder;
import org.fogbowcloud.ras.core.models.tokens.OpenStackV3Token;
import org.fogbowcloud.ras.core.plugins.interoperability.AttachmentPlugin;
import org.fogbowcloud.ras.core.plugins.interoperability.openstack.OpenStackHttpToFogbowRasExceptionMapper;
import org.fogbowcloud.ras.core.plugins.interoperability.openstack.OpenStackStateMapper;
import org.fogbowcloud.ras.util.PropertiesUtil;
import org.fogbowcloud.ras.util.connectivity.HttpRequestClientUtil;
import org.fogbowcloud.ras.util.connectivity.HttpRequestUtil;
import org.json.JSONException;

import java.util.Properties;

public class OpenStackAttachmentPlugin implements AttachmentPlugin<OpenStackV3Token> {
    private final Logger LOGGER = Logger.getLogger(OpenStackAttachmentPlugin.class);

    protected static final String COMPUTE_NOVAV2_URL_KEY = "openstack_nova_v2_url";
    private static final String COMPUTE_V2_API_ENDPOINT = "/v2/";
    private static final String OS_VOLUME_ATTACHMENTS = "/os-volume_attachments";
    private static final String SEPARATOR_ID = " ";
    private static final String SERVERS = "/servers/";
    private Properties properties;
    private HttpRequestClientUtil client;

    public OpenStackAttachmentPlugin() throws FatalErrorException {
        this.properties = PropertiesUtil.readProperties(HomeDir.getPath() +
                DefaultConfigurationConstants.OPENSTACK_CONF_FILE_NAME);
        initClient();
    }

    @Override
    public String requestInstance(AttachmentOrder attachmentOrder, OpenStackV3Token openStackV3Token)
            throws FogbowRasException, UnexpectedException {
        String projectId = openStackV3Token.getProjectId();
        String serverId = attachmentOrder.getSource();
        String volumeId = attachmentOrder.getTarget();

        String jsonRequest = null;
        try {
            jsonRequest = generateJsonToAttach(volumeId);
        } catch (JSONException e) {
            String message = Messages.Error.UNABLE_TO_GENERATE_JSON;
            LOGGER.error(message, e);
            throw new InvalidParameterException(message, e);
        }

        String endpoint = getPrefixEndpoint(projectId) + SERVERS + serverId + OS_VOLUME_ATTACHMENTS;
        try {
            this.client.doPostRequest(endpoint, openStackV3Token, jsonRequest);
        } catch (HttpResponseException e) {
            OpenStackHttpToFogbowRasExceptionMapper.map(e);
        }
        return attachmentOrder.getSource() + SEPARATOR_ID + attachmentOrder.getTarget();
    }

    @Override
    public void deleteInstance(String instanceId, OpenStackV3Token openStackV3Token)
            throws FogbowRasException, UnexpectedException {
        String projectId = openStackV3Token.getProjectId();
        if (projectId == null) {
            String message = Messages.Error.UNSPECIFIED_PROJECT_ID;
            LOGGER.error(message);
            throw new UnauthenticatedUserException(message);
        }

        String[] separatorInstanceId = instanceId.split(SEPARATOR_ID);
        String serverId = separatorInstanceId[0];
        String volumeId = separatorInstanceId[1];
        String endpoint = getPrefixEndpoint(projectId) + SERVERS + serverId + OS_VOLUME_ATTACHMENTS + "/" + volumeId;

        try {
            this.client.doDeleteRequest(endpoint, openStackV3Token);
        } catch (HttpResponseException e) {
            OpenStackHttpToFogbowRasExceptionMapper.map(e);
        }
    }

    @Override
    public AttachmentInstance getInstance(String instanceId, OpenStackV3Token openStackV3Token)
            throws FogbowRasException, UnexpectedException {
        LOGGER.info(String.format(Messages.Info.GETTING_INSTANCE, instanceId, openStackV3Token));
        String projectId = openStackV3Token.getProjectId();

        String[] separatorInstanceId = instanceId.split(SEPARATOR_ID);

        // this variable refers to computeInstanceId received in the first part of the vector
        String serverId = separatorInstanceId[0];

        // this variable refers to volumeInstanceId received in the second part of the vector
        String volumeId = separatorInstanceId[1];

        String requestEndpoint = getPrefixEndpoint(projectId) + SERVERS + serverId + OS_VOLUME_ATTACHMENTS + "/" + volumeId;

        String jsonResponse = null;
        try {
            jsonResponse = this.client.doGetRequest(requestEndpoint, openStackV3Token);
        } catch (HttpResponseException e) {
            OpenStackHttpToFogbowRasExceptionMapper.map(e);
        }
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

            AttachmentInstance attachmentInstance = new AttachmentInstance(id, fogbowState, serverId, volumeId, device);
            return attachmentInstance;

        } catch (JSONException e) {
            String message = Messages.Error.UNABLE_TO_GET_ATTACHMENT_INSTANCE;
            LOGGER.error(message, e);
            throw new UnexpectedException(message, e);
        }
    }

    private String getPrefixEndpoint(String projectId) {
        return this.properties.getProperty(COMPUTE_NOVAV2_URL_KEY) + COMPUTE_V2_API_ENDPOINT + projectId;
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
