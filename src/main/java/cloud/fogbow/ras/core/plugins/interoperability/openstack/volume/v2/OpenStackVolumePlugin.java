package cloud.fogbow.ras.core.plugins.interoperability.openstack.volume.v2;

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
import cloud.fogbow.ras.core.models.instances.InstanceState;
import cloud.fogbow.ras.core.models.instances.VolumeInstance;
import cloud.fogbow.ras.core.models.orders.VolumeOrder;
import cloud.fogbow.ras.core.plugins.interoperability.VolumePlugin;
import cloud.fogbow.ras.core.plugins.interoperability.openstack.OpenStackHttpToFogbowExceptionMapper;
import cloud.fogbow.ras.core.plugins.interoperability.openstack.OpenStackStateMapper;
import cloud.fogbow.ras.core.plugins.interoperability.openstack.OpenStackV3Token;
import cloud.fogbow.ras.util.connectivity.AuditableHttpRequestClient;
import org.json.JSONException;

import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.UUID;

public class OpenStackVolumePlugin implements VolumePlugin {
    private static final Logger LOGGER = Logger.getLogger(OpenStackVolumePlugin.class);

    private final String V2_API_ENDPOINT = "/v2/";
    protected static final String SUFIX_ENDPOINT_VOLUMES = "/volumes";
    protected static final String SUFIX_ENDPOINT_VOLUME_TYPES = "/types";
    protected static final String FOGBOW_INSTANCE_NAME = "ras-volume-";
    public static final String VOLUME_NOVAV2_URL_KEY = "openstack_cinder_url";
    private AuditableHttpRequestClient client;
    private String volumeV2APIEndpoint;

    public OpenStackVolumePlugin(String confFilePath) throws FatalErrorException {
        Properties properties = PropertiesUtil.readProperties(confFilePath);
        this.volumeV2APIEndpoint = properties.getProperty(VOLUME_NOVAV2_URL_KEY) + V2_API_ENDPOINT;
        initClient();
    }

    @Override
    public String requestInstance(VolumeOrder order, CloudToken token) throws FogbowException {
        OpenStackV3Token openStackV3Token = (OpenStackV3Token) token;
        String tenantId = openStackV3Token.getProjectId();
        if (tenantId == null) {
            String message = Messages.Error.UNSPECIFIED_PROJECT_ID;
            LOGGER.error(message);
            throw new UnauthenticatedUserException(message);
        }

        Map<String, String> requirements = order.getRequirements();


        String jsonRequest = null;
        try {
            String size = String.valueOf(order.getVolumeSize());
            String instanceName = order.getName();
            String name = instanceName == null ? FOGBOW_INSTANCE_NAME + getRandomUUID() : instanceName;
            String volumeTypeId = null;
            if(requirements != null && requirements.size() > 0) {
                volumeTypeId = getValidVolumeTypeId(requirements, tenantId, openStackV3Token);
            }
            jsonRequest = generateJsonEntityToCreateInstance(size, name, volumeTypeId);


        } catch (JSONException e) {
            String message = Messages.Error.UNABLE_TO_GENERATE_JSON;
            LOGGER.error(message, e);
            throw new InvalidParameterException(message, e);
        }

        String endpoint = this.volumeV2APIEndpoint + tenantId + SUFIX_ENDPOINT_VOLUMES;
        String responseStr = null;
        try {
            responseStr = this.client.doPostRequest(endpoint, openStackV3Token, jsonRequest);
        } catch (HttpResponseException e) {
            OpenStackHttpToFogbowExceptionMapper.map(e);
        }
        VolumeInstance instanceFromJson = getInstanceFromJson(responseStr);
        return instanceFromJson != null ? instanceFromJson.getId() : null;
    }

    @Override
    public VolumeInstance getInstance(String storageOrderInstanceId, CloudToken token) throws FogbowException {
        OpenStackV3Token openStackV3Token = (OpenStackV3Token) token;
        String tenantId = openStackV3Token.getProjectId();
        if (tenantId == null) {
            String message = Messages.Error.UNSPECIFIED_PROJECT_ID;
            LOGGER.error(message);
            throw new UnauthenticatedUserException(message);
        }

        String endpoint = this.volumeV2APIEndpoint + tenantId
                + SUFIX_ENDPOINT_VOLUMES + "/" + storageOrderInstanceId;
        String responseStr = null;
        try {
            responseStr = this.client.doGetRequest(endpoint, openStackV3Token);
        } catch (HttpResponseException e) {
            OpenStackHttpToFogbowExceptionMapper.map(e);
        }
        return getInstanceFromJson(responseStr);
    }

    @Override
    public void deleteInstance(String storageOrderInstanceId, CloudToken token) throws FogbowException {
        OpenStackV3Token openStackV3Token = (OpenStackV3Token) token;
        String tenantId = openStackV3Token.getProjectId();
        if (tenantId == null) {
            String message = Messages.Error.UNSPECIFIED_PROJECT_ID;
            LOGGER.error(message);
            throw new UnauthenticatedUserException(message);
        }

        String endpoint = this.volumeV2APIEndpoint + tenantId
                + SUFIX_ENDPOINT_VOLUMES + "/" + storageOrderInstanceId;
        try {
            this.client.doDeleteRequest(endpoint, openStackV3Token);
        } catch (HttpResponseException e) {
            OpenStackHttpToFogbowExceptionMapper.map(e);
        }
    }

    protected String getRandomUUID() {
        return UUID.randomUUID().toString();
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
            String message = Messages.Error.ERROR_WHILE_GETTING_VOLUME_INSTANCE;
            LOGGER.error(message, e);
            throw new UnexpectedException(message, e);
        }
    }

    protected List<GetAllTypesResponse.Type> getRequirementsFromJson(String json) throws UnexpectedException {
        try {
            GetAllTypesResponse getAllTypesResponse = GetAllTypesResponse.fromJson(json);
            return getAllTypesResponse.getTypes();
        } catch (Exception e) {
            String message = Messages.Error.ERROR_WHILE_PROCESSING_VOLUME_REQUIREMENTS;
            LOGGER.error(message, e);
            throw new UnexpectedException(message, e);
        }
    }

    protected String generateJsonEntityToCreateInstance(String size, String name, String volmeTypeId) throws JSONException {
        CreateVolumeRequest createVolumeRequest =
                new CreateVolumeRequest.Builder()
                        .name(name)
                        .size(size)
                        .volume_type(volmeTypeId)
                        .build();

        return createVolumeRequest.toJson();
    }

    private String getValidVolumeTypeId(Map<String, String> requirements, String tenantId, CloudToken token)
            throws FogbowException, UnexpectedException {

        String endpoint = this.volumeV2APIEndpoint + tenantId + SUFIX_ENDPOINT_VOLUME_TYPES;
        String responseStr = null;
        try {
            responseStr = this.client.doGetRequest(endpoint, token);
        } catch (HttpResponseException e) {
            OpenStackHttpToFogbowExceptionMapper.map(e);
        }

        List<GetAllTypesResponse.Type> instanceFromJson = getRequirementsFromJson(responseStr);

        for(GetAllTypesResponse.Type type : instanceFromJson){

            boolean match = true;

            Map<String, String> specs = type.getExtraSpecs();

            for(Map.Entry<String, String> pair : requirements.entrySet()){
                String key = pair.getKey();
                String value = pair.getValue();
                if(!specs.containsKey(key) || !value.equals(specs.get(key))){
                    match = false;
                    break;
                }
            }

            if(!match) continue;

            return type.getId();
        }

        String message = Messages.Exception.UNABLE_TO_MATCH_REQUIREMENTS;
        throw new NoAvailableResourcesException(message);
    }

    private void initClient() {
        this.client = new AuditableHttpRequestClient(
                new Integer(PropertiesHolder.getInstance().getProperty(ConfigurationConstants.HTTP_REQUEST_TIMEOUT_KEY,
                        DefaultConfigurationConstants.XMPP_TIMEOUT)));
    }
    public void setClient(AuditableHttpRequestClient client) {
        this.client = client;
    }
}