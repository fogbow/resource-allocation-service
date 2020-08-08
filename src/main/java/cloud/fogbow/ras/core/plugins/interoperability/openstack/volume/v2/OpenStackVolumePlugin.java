package cloud.fogbow.ras.core.plugins.interoperability.openstack.volume.v2;

import java.util.List;
import java.util.Map;
import java.util.Properties;

import cloud.fogbow.common.constants.OpenStackConstants;
import cloud.fogbow.ras.api.http.response.quotas.allocation.VolumeAllocation;
import cloud.fogbow.ras.core.plugins.interoperability.openstack.util.v2.serializables.requests.CreateVolumeRequest;
import cloud.fogbow.ras.core.plugins.interoperability.openstack.util.v2.serializables.responses.GetAllTypesResponse;
import cloud.fogbow.ras.core.plugins.interoperability.openstack.util.v2.serializables.responses.GetVolumeResponse;
import com.google.common.annotations.VisibleForTesting;
import org.apache.log4j.Logger;
import org.json.JSONException;

import com.google.gson.JsonSyntaxException;

import cloud.fogbow.common.exceptions.FatalErrorException;
import cloud.fogbow.common.exceptions.FogbowException;
import cloud.fogbow.common.exceptions.UnacceptableOperationException;
import cloud.fogbow.common.exceptions.InternalServerErrorException;
import cloud.fogbow.common.models.OpenStackV3User;
import cloud.fogbow.common.util.PropertiesUtil;
import cloud.fogbow.common.util.connectivity.cloud.openstack.OpenStackHttpClient;
import cloud.fogbow.ras.api.http.response.InstanceState;
import cloud.fogbow.ras.api.http.response.VolumeInstance;
import cloud.fogbow.ras.constants.Messages;
import cloud.fogbow.ras.core.models.ResourceType;
import cloud.fogbow.ras.core.models.orders.VolumeOrder;
import cloud.fogbow.ras.core.plugins.interoperability.VolumePlugin;
import cloud.fogbow.ras.core.plugins.interoperability.openstack.util.OpenStackPluginUtils;
import cloud.fogbow.ras.core.plugins.interoperability.openstack.util.OpenStackStateMapper;
import cloud.fogbow.ras.core.plugins.interoperability.openstack.util.v2.serializables.responses.GetAllTypesResponse.Type;

public class OpenStackVolumePlugin implements VolumePlugin<OpenStackV3User> {
    
    private static final Logger LOGGER = Logger.getLogger(OpenStackVolumePlugin.class);

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
        LOGGER.info(Messages.Log.REQUESTING_INSTANCE_FROM_PROVIDER);
        String projectId = OpenStackPluginUtils.getProjectIdFrom(cloudUser);
        String size = String.valueOf(order.getVolumeSize());
        String name = order.getName();
        String volumeTypeId = findVolumeTypeId(order.getRequirements(), projectId, cloudUser);
        String jsonRequest = generateJsonRequest(size, name, volumeTypeId);
        String endpoint = getPrefixEndpoint(projectId) + OpenStackConstants.VOLUMES_ENDPOINT;
        
        GetVolumeResponse volumeResponse = doRequestInstance(endpoint, jsonRequest, cloudUser);
        setAllocationToOrder(order);
        return volumeResponse.getId();
    }

    @VisibleForTesting
    void setAllocationToOrder(VolumeOrder order) {
        synchronized (order) {
            int size = order.getVolumeSize();
            VolumeAllocation volumeAllocation = new VolumeAllocation(size);
            order.setActualAllocation(volumeAllocation);
        }
    }

    @Override
    public VolumeInstance getInstance(VolumeOrder order, OpenStackV3User cloudUser) throws FogbowException {
        String instanceId = order.getInstanceId();
        LOGGER.info(String.format(Messages.Log.GETTING_INSTANCE_S, instanceId));
        String projectId = OpenStackPluginUtils.getProjectIdFrom(cloudUser);
        String endpoint = getPrefixEndpoint(projectId) 
                + OpenStackConstants.VOLUMES_ENDPOINT
                + OpenStackConstants.ENDPOINT_SEPARATOR
                + order.getInstanceId();

        return doGetInstance(endpoint, cloudUser);
    }

    @Override
    public void deleteInstance(VolumeOrder order, OpenStackV3User cloudUser) throws FogbowException {
        String instanceId = order.getInstanceId();
        LOGGER.info(String.format(Messages.Log.DELETING_INSTANCE_S, instanceId));
        String projectId = OpenStackPluginUtils.getProjectIdFrom(cloudUser);
        String endpoint = getPrefixEndpoint(projectId) 
                + OpenStackConstants.VOLUMES_ENDPOINT
                + OpenStackConstants.ENDPOINT_SEPARATOR
                + order.getInstanceId();
        
        doDeleteInstance(endpoint, cloudUser);
    }

    protected void doDeleteInstance(String endpoint, OpenStackV3User cloudUser) throws FogbowException {
        this.client.doDeleteRequest(endpoint, cloudUser);
    }
    
    protected VolumeInstance doGetInstance(String endpoint, OpenStackV3User cloudUser) throws FogbowException {
        String json = doGetResponseFromCloud(endpoint, cloudUser);
        GetVolumeResponse response = doGetVolumeResponseFrom(json);
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
        
        String jsonResponse = this.client.doPostRequest(endpoint, jsonRequest, cloudUser);
        return doGetVolumeResponseFrom(jsonResponse);
    }

    protected GetVolumeResponse doGetVolumeResponseFrom(String jsonResponse) throws InternalServerErrorException {
        try {
            return GetVolumeResponse.fromJson(jsonResponse);
        } catch (JsonSyntaxException e) {
            LOGGER.error(Messages.Log.ERROR_WHILE_GETTING_VOLUME_INSTANCE, e);
            throw new InternalServerErrorException(Messages.Exception.ERROR_WHILE_GETTING_VOLUME_INSTANCE);
        }
    }
    
    protected String getPrefixEndpoint(String projectId) {
        return this.properties.getProperty(OpenStackPluginUtils.VOLUME_NOVA_URL_KEY) +
                OpenStackConstants.CINDER_V2_API_ENDPOINT + OpenStackConstants.ENDPOINT_SEPARATOR + projectId;
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
        
        String endpoint = getPrefixEndpoint(projectId) + OpenStackConstants.TYPES_ENDPOINT;
        String json = doGetResponseFromCloud(endpoint, cloudUser);
        GetAllTypesResponse response = doGetAllTypesResponseFrom(json);
        List<Type> types = response.getTypes();
        
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
        throw new UnacceptableOperationException(message);
    }
    
    protected GetAllTypesResponse doGetAllTypesResponseFrom(String json) throws InternalServerErrorException {
        try {
            return GetAllTypesResponse.fromJson(json);
        } catch (Exception e) {
            LOGGER.error(Messages.Log.ERROR_WHILE_PROCESSING_VOLUME_REQUIREMENTS, e);
            throw new InternalServerErrorException(Messages.Exception.ERROR_WHILE_PROCESSING_VOLUME_REQUIREMENTS);
        }
    }

    protected String doGetResponseFromCloud(String endpoint, OpenStackV3User cloudUser) throws FogbowException {
        String jsonResponse = this.client.doGetRequest(endpoint, cloudUser);
        return jsonResponse;
    }

    private void initClient() {
        this.client = new OpenStackHttpClient();
    }

    public void setClient(OpenStackHttpClient client) {
        this.client = client;
    }
}