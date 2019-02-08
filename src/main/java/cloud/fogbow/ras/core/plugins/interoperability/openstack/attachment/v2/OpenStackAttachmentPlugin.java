package cloud.fogbow.ras.core.plugins.interoperability.openstack.attachment.v2;

import cloud.fogbow.common.exceptions.*;
import cloud.fogbow.common.models.CloudToken;
import cloud.fogbow.common.util.PropertiesUtil;
import cloud.fogbow.ras.core.constants.ConfigurationConstants;
import cloud.fogbow.ras.core.constants.DefaultConfigurationConstants;
import cloud.fogbow.ras.core.constants.Messages;
import org.apache.http.client.HttpResponseException;
import org.apache.log4j.Logger;
import cloud.fogbow.ras.core.PropertiesHolder;
import cloud.fogbow.ras.core.models.ResourceType;
import cloud.fogbow.ras.core.models.instances.AttachmentInstance;
import cloud.fogbow.ras.core.models.instances.InstanceState;
import cloud.fogbow.ras.core.models.orders.AttachmentOrder;
import cloud.fogbow.ras.core.plugins.interoperability.AttachmentPlugin;
import cloud.fogbow.ras.core.plugins.interoperability.openstack.OpenStackHttpToFogbowExceptionMapper;
import cloud.fogbow.ras.core.plugins.interoperability.openstack.OpenStackStateMapper;
import cloud.fogbow.ras.core.plugins.interoperability.openstack.OpenStackV3Token;
import cloud.fogbow.ras.util.connectivity.AuditableHttpRequestClient;
import org.json.JSONException;

import java.util.Properties;

public class OpenStackAttachmentPlugin implements AttachmentPlugin {
    private final Logger LOGGER = Logger.getLogger(OpenStackAttachmentPlugin.class);

    protected static final String COMPUTE_NOVAV2_URL_KEY = "openstack_nova_v2_url";
    private static final String COMPUTE_V2_API_ENDPOINT = "/v2/";
    private static final String OS_VOLUME_ATTACHMENTS = "/os-volume_attachments";
    private static final String SEPARATOR_ID = " ";
    private static final String SERVERS = "/servers/";
    private Properties properties;
    private AuditableHttpRequestClient client;

    public OpenStackAttachmentPlugin(String confFilePath) throws FatalErrorException {
        this.properties = PropertiesUtil.readProperties(confFilePath);
        initClient();
    }

    @Override
    public String requestInstance(AttachmentOrder attachmentOrder, CloudToken token) throws FogbowException {
        OpenStackV3Token openStackV3Token = (OpenStackV3Token) token;
        String projectId = openStackV3Token.getProjectId();
        String serverId = attachmentOrder.getComputeId();
        String volumeId = attachmentOrder.getVolumeId();
        String device = attachmentOrder.getDevice();

        String jsonRequest = null;
        try {
            jsonRequest = generateJsonToAttach(volumeId, device);
        } catch (JSONException e) {
            String message = Messages.Error.UNABLE_TO_GENERATE_JSON;
            LOGGER.error(message, e);
            throw new InvalidParameterException(message, e);
        }

        String endpoint = getPrefixEndpoint(projectId) + SERVERS + serverId + OS_VOLUME_ATTACHMENTS;
        try {
            this.client.doPostRequest(endpoint, openStackV3Token, jsonRequest);
        } catch (HttpResponseException e) {
            OpenStackHttpToFogbowExceptionMapper.map(e);
        }
        return attachmentOrder.getComputeId() + SEPARATOR_ID + attachmentOrder.getVolumeId();
    }

    @Override
    public void deleteInstance(String instanceId, CloudToken token) throws FogbowException {
        OpenStackV3Token openStackV3Token = (OpenStackV3Token) token;
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
            OpenStackHttpToFogbowExceptionMapper.map(e);
        }
    }

    @Override
    public AttachmentInstance getInstance(String instanceId, CloudToken token) throws FogbowException {
        LOGGER.info(String.format(Messages.Info.GETTING_INSTANCE, instanceId, token));
        OpenStackV3Token openStackV3Token = (OpenStackV3Token) token;
        String projectId = openStackV3Token.getProjectId();

        String[] separatorInstanceId = instanceId.split(SEPARATOR_ID);

        // this variable refers to computeInstanceId received in the first part of the vector
        String computeId = separatorInstanceId[0];

        // this variable refers to volumeInstanceId received in the second part of the vector
        String volumeId = separatorInstanceId[1];

        String requestEndpoint = getPrefixEndpoint(projectId) + SERVERS + computeId + OS_VOLUME_ATTACHMENTS + "/" + volumeId;

        String jsonResponse = null;
        try {
            jsonResponse = this.client.doGetRequest(requestEndpoint, openStackV3Token);
        } catch (HttpResponseException e) {
            OpenStackHttpToFogbowExceptionMapper.map(e);
        }
        AttachmentInstance attachmentInstance = getInstanceFromJson(jsonResponse);

        return attachmentInstance;
    }

    protected AttachmentInstance getInstanceFromJson(String jsonResponse) throws UnexpectedException {
        try {
            GetAttachmentResponse getAttachmentResponse = GetAttachmentResponse.fromJson(jsonResponse);
            String id = getAttachmentResponse.getId();
            String computeId = getAttachmentResponse.getServerId();
            String volumeId = getAttachmentResponse.getVolumeId();
            String device = getAttachmentResponse.getDevice();

            // There is no OpenStackState for attachments; we set it to empty string to allow its mapping
            // by the OpenStackStateMapper.map() function.
            String openStackState = "";
            InstanceState fogbowState = OpenStackStateMapper.map(ResourceType.ATTACHMENT, openStackState);

            AttachmentInstance attachmentInstance = new AttachmentInstance(id, fogbowState, computeId, volumeId, device);
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

    protected String generateJsonToAttach(String volumeId, String device) throws JSONException {
        CreateAttachmentRequest createAttachmentRequest = new CreateAttachmentRequest.Builder()
                .volumeId(volumeId)
                .device(device)
                .build();
        return createAttachmentRequest.toJson();
    }

    private void initClient() {
        this.client = new AuditableHttpRequestClient(
                new Integer(PropertiesHolder.getInstance().getProperty(ConfigurationConstants.HTTP_REQUEST_TIMEOUT_KEY,
                        DefaultConfigurationConstants.XMPP_TIMEOUT)));
    }

    protected void setClient(AuditableHttpRequestClient client) {
        this.client = client;
    }

    protected void setProperties(Properties properties) {
        this.properties = properties;
    }
}
