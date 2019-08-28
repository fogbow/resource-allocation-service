package cloud.fogbow.ras.core.plugins.interoperability.openstack.volume.v2;

import java.util.List;
import java.util.Map;
import java.util.Properties;

import org.apache.http.client.HttpResponseException;
import org.apache.log4j.Logger;
import org.json.JSONException;

import com.google.gson.JsonSyntaxException;

import cloud.fogbow.common.exceptions.FatalErrorException;
import cloud.fogbow.common.exceptions.FogbowException;
import cloud.fogbow.common.exceptions.InvalidParameterException;
import cloud.fogbow.common.exceptions.NoAvailableResourcesException;
import cloud.fogbow.common.exceptions.UnexpectedException;
import cloud.fogbow.common.models.OpenStackV3User;
import cloud.fogbow.common.util.PropertiesUtil;
import cloud.fogbow.common.util.connectivity.cloud.openstack.OpenStackHttpClient;
import cloud.fogbow.common.util.connectivity.cloud.openstack.OpenStackHttpToFogbowExceptionMapper;
import cloud.fogbow.ras.api.http.response.InstanceState;
import cloud.fogbow.ras.api.http.response.VolumeInstance;
import cloud.fogbow.ras.constants.Messages;
import cloud.fogbow.ras.core.models.ResourceType;
import cloud.fogbow.ras.core.models.orders.VolumeOrder;
import cloud.fogbow.ras.core.plugins.interoperability.VolumePlugin;
import cloud.fogbow.ras.core.plugins.interoperability.openstack.OpenStackCloudUtils;
import cloud.fogbow.ras.core.plugins.interoperability.openstack.OpenStackStateMapper;
import cloud.fogbow.ras.core.plugins.interoperability.openstack.volume.v2.GetAllTypesResponse.Type;
import cloud.fogbow.ras.core.plugins.interoperability.util.FogbowCloudUtil;

public class OpenStackVolumePlugin implements VolumePlugin<OpenStackV3User> {
    
    private static final Logger LOGGER = Logger.getLogger(OpenStackVolumePlugin.class);

    protected static final String ENDPOINT_SEPARATOR = "/";
    protected static final String V2_API_ENDPOINT = "/v2/";
    protected static final String VOLUMES = "/volumes";
    protected static final String VOLUME_NOVAV2_URL_KEY = "openstack_cinder_url";
    protected static final String TYPES = "/types";
    
    private Properties properties;
    private OpenStackHttpClient client;

    public OpenStackVolumePlugin(String confFilePath) throws FatalErrorException {
        this.properties = PropertiesUtil.readProperties(confFilePath);
        initClient();
    }

    @Override
    public boolean isReady(String cloudState) {
        return OpenStackStateMapper.map(ResourceType.VOLUME, cloudState).equals(InstanceState.READY);
    }

    @Override
    public boolean hasFailed(String cloudState) {
        return OpenStackStateMapper.map(ResourceType.VOLUME, cloudState).equals(InstanceState.FAILED);
    }

    @Override
    public String requestInstance(VolumeOrder order, OpenStackV3User cloudUser) throws FogbowException {
        String projectId = OpenStackCloudUtils.getProjectIdFrom(cloudUser);
        String size = String.valueOf(order.getVolumeSize());
        String name = FogbowCloudUtil.defineInstanceName(order.getName());
        String volumeTypeId = findVolumeTypeId(order.getRequirements(), projectId, cloudUser);
        String jsonRequest = generateJsonRequest(size, name, volumeTypeId);
        String endpoint = getPrefixEndpoint(projectId) + VOLUMES;
        
        GetVolumeResponse response = doRequestInstance(endpoint, jsonRequest, cloudUser); // FIXME CreateVolumeResponse...
        return response.getId();
    }

    @Override
    public VolumeInstance getInstance(VolumeOrder order, OpenStackV3User cloudUser) throws FogbowException {
        String projectId = OpenStackCloudUtils.getProjectIdFrom(cloudUser);
        String endpoint = getPrefixEndpoint(projectId) 
                + VOLUMES 
                + ENDPOINT_SEPARATOR 
                + order.getInstanceId();

        return doGetInstance(endpoint, cloudUser);
    }

    @Override
    public void deleteInstance(VolumeOrder order, OpenStackV3User cloudUser) throws FogbowException {
        String projectId = OpenStackCloudUtils.getProjectIdFrom(cloudUser);
        String endpoint = getPrefixEndpoint(projectId) 
                + VOLUMES 
                + ENDPOINT_SEPARATOR 
                + order.getInstanceId();
        
        doDeleteInstance(endpoint, cloudUser);
    }

    protected void doDeleteInstance(String endpoint, OpenStackV3User cloudUser) throws FogbowException {
        try {
            this.client.doDeleteRequest(endpoint, cloudUser);
        } catch (HttpResponseException e) {
            OpenStackHttpToFogbowExceptionMapper.map(e);
        }
    }
    
    protected VolumeInstance doGetInstance(String endpoint, OpenStackV3User cloudUser) throws FogbowException {
        String json = doGetResponseFromCloud(endpoint, cloudUser);
        GetVolumeResponse response = null;
        try {
            response = GetVolumeResponse.fromJson(json);
        } catch (JsonSyntaxException e) {
            String message = Messages.Error.ERROR_WHILE_GETTING_VOLUME_INSTANCE;
            LOGGER.error(message, e);
            throw new UnexpectedException(message, e);
        }
        return buildVolumeInstanceFrom(response);
    }
    
    protected VolumeInstance buildVolumeInstanceFrom(GetVolumeResponse response) {
        String id = response.getId();
        String status = response.getStatus();
        String name = response.getName();
        int size = response.getSize();
        return new VolumeInstance(id, status, name, size);
    }

    protected GetVolumeResponse doRequestInstance(String endpoint, String jsonRequest, OpenStackV3User cloudUser)
            throws FogbowException {
        
        String jsonResponse = null;
        try {
            jsonResponse = this.client.doPostRequest(endpoint, jsonRequest, cloudUser);
        } catch (HttpResponseException e) {
            OpenStackHttpToFogbowExceptionMapper.map(e);
        }
        return doCreateAttachmentResponseFrom(jsonResponse);
    }

    protected GetVolumeResponse doCreateAttachmentResponseFrom(String jsonResponse) throws InvalidParameterException {
        try {
            return GetVolumeResponse.fromJson(jsonResponse); // FIXME create a new class to reflect this response...
        } catch (JsonSyntaxException e) {
            String message = Messages.Error.UNABLE_TO_GENERATE_JSON;
            LOGGER.error(message, e);
            throw new InvalidParameterException(message, e);
        }
    }
    
    protected String getPrefixEndpoint(String projectId) {
        return this.properties.getProperty(VOLUME_NOVAV2_URL_KEY) + V2_API_ENDPOINT + projectId;
    }
    
    protected String generateJsonRequest(String size, String name, String volumeTypeId) throws JSONException {
        CreateVolumeRequest createVolumeRequest = new CreateVolumeRequest.Builder()
                .name(name)
                .size(size)
                .volume_type(volumeTypeId)
                .build();

        return createVolumeRequest.toJson();
    }

    protected String findVolumeTypeId(Map<String, String> requirements, String projectId, OpenStackV3User cloudUser)
            throws FogbowException {

        if (requirements == null || requirements.isEmpty()) {
            return null;
        }
        
        String endpoint = getPrefixEndpoint(projectId) + TYPES;
        String json = doGetResponseFromCloud(endpoint, cloudUser);
        GetAllTypesResponse response = GetAllTypesResponse.fromJson(json);
        List<Type> types = doGetRequirementsFrom(response);
        
        for (Type type : types){
            boolean match = true;
            Map specs = type.getExtraSpecs();
            for (Map.Entry<String, String> pair : requirements.entrySet()){
                String key = pair.getKey();
                String value = pair.getValue();
                if (!specs.containsKey(key) || !value.equals(specs.get(key))){
                    match = false;
                    break;
                }
            }
            if (!match) continue;
            return type.getId();
        }

        String message = Messages.Exception.UNABLE_TO_MATCH_REQUIREMENTS;
        throw new NoAvailableResourcesException(message);
    }
    
    protected List<Type> doGetRequirementsFrom(GetAllTypesResponse response) throws UnexpectedException {
        try {
            return response.getTypes();
        } catch (Exception e) {
            String message = Messages.Error.ERROR_WHILE_PROCESSING_VOLUME_REQUIREMENTS;
            LOGGER.error(message, e);
            throw new UnexpectedException(message, e);
        }
    }
    
    protected String doGetResponseFromCloud(String endpoint, OpenStackV3User cloudUser) throws FogbowException {
        String jsonResponse = null;
        try {
            jsonResponse = this.client.doGetRequest(endpoint, cloudUser);
        } catch (HttpResponseException e) {
            OpenStackHttpToFogbowExceptionMapper.map(e);
        }
        return jsonResponse;
    }

    private void initClient() {
        this.client = new OpenStackHttpClient();
    }

    public void setClient(OpenStackHttpClient client) {
        this.client = client;
    }
}