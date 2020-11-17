package cloud.fogbow.ras.core.plugins.interoperability.googlecloud.volume.v1;

import cloud.fogbow.common.constants.GoogleCloudConstants;
import cloud.fogbow.common.exceptions.FatalErrorException;
import cloud.fogbow.common.exceptions.FogbowException;
import cloud.fogbow.common.exceptions.InternalServerErrorException;
import cloud.fogbow.common.exceptions.UnacceptableOperationException;
import cloud.fogbow.common.models.GoogleCloudUser;
import cloud.fogbow.common.util.PropertiesUtil;
import cloud.fogbow.common.util.connectivity.cloud.googlecloud.GoogleCloudHttpClient;
import cloud.fogbow.ras.api.http.response.InstanceState;
import cloud.fogbow.ras.api.http.response.VolumeInstance;
import cloud.fogbow.ras.api.http.response.quotas.allocation.VolumeAllocation;
import cloud.fogbow.ras.constants.Messages;
import cloud.fogbow.ras.core.models.ResourceType;
import cloud.fogbow.ras.core.models.orders.VolumeOrder;
import cloud.fogbow.ras.core.plugins.interoperability.VolumePlugin;
import cloud.fogbow.ras.core.plugins.interoperability.googlecloud.sdk.v1.volume.models.CreateVolumeRequest;
import cloud.fogbow.ras.core.plugins.interoperability.googlecloud.sdk.v1.volume.models.CreateVolumeResponse;
import cloud.fogbow.ras.core.plugins.interoperability.googlecloud.sdk.v1.volume.models.GetAllTypesResponse;
import cloud.fogbow.ras.core.plugins.interoperability.googlecloud.sdk.v1.volume.models.GetAllTypesResponse.Type;
import cloud.fogbow.ras.core.plugins.interoperability.googlecloud.sdk.v1.volume.models.GetVolumeResponse;
import cloud.fogbow.ras.core.plugins.interoperability.googlecloud.util.GoogleCloudPluginUtils;
import cloud.fogbow.ras.core.plugins.interoperability.googlecloud.util.GoogleCloudStateMapper;
import com.google.common.annotations.VisibleForTesting;
import com.google.gson.JsonSyntaxException;
import org.apache.log4j.Logger;
import org.json.JSONException;

import java.util.Map;
import java.util.List;
import java.util.Properties;

public class GoogleCloudVolumePlugin implements VolumePlugin<GoogleCloudUser> {
    private static final Logger LOGGER = Logger.getLogger(GoogleCloudVolumePlugin.class);


    private Properties properties;
    private GoogleCloudHttpClient client;

    public GoogleCloudVolumePlugin(String confFilePath) throws FatalErrorException {
        this.properties = PropertiesUtil.readProperties(confFilePath);
        initClient();
    }

    @Override
    public boolean isReady(String instanceState) {
        return GoogleCloudStateMapper.map(ResourceType.VOLUME, instanceState).equals(InstanceState.READY);
    }

    @Override
    public boolean hasFailed(String instanceState) {
        return GoogleCloudStateMapper.map(ResourceType.VOLUME, instanceState).equals(InstanceState.FAILED);
    }

    @VisibleForTesting
    String generateJsonRequest(String size, String name, String volumeTypeId) throws JSONException {
        CreateVolumeRequest createVolumeRequest = new CreateVolumeRequest.Builder()
                .name(name)
                .size(size)
                .volumeType(volumeTypeId)
                .build();

        return createVolumeRequest.toJson();
    }


    @VisibleForTesting
    String getZoneEndpoint(String zone){
        return GoogleCloudConstants.ZONES_ENDPOINT + GoogleCloudConstants.ENDPOINT_SEPARATOR + zone;
    }

    @VisibleForTesting
    String getPrefixEndpoint(String projectId) {
        return GoogleCloudConstants.BASE_COMPUTE_API_URL + GoogleCloudConstants.COMPUTE_ENGINE_V1_ENDPOINT
                + GoogleCloudConstants.PROJECT_ENDPOINT + GoogleCloudConstants.ENDPOINT_SEPARATOR + projectId;
    }

    @Override
    public String requestInstance(VolumeOrder volumeOrder, GoogleCloudUser cloudUser) throws FogbowException {
        LOGGER.info(Messages.Log.REQUESTING_INSTANCE_FROM_PROVIDER);

        String projectId = GoogleCloudPluginUtils.getProjectIdFrom(cloudUser);
        String size = String.valueOf(volumeOrder.getVolumeSize());
        String name = volumeOrder.getName();
        String volumeTypeId = findVolumeTypeSource(volumeOrder.getRequirements(), projectId, cloudUser);
        String jsonRequest = generateJsonRequest(size, name, volumeTypeId);
        String endpoint = getPrefixEndpoint(projectId)
                + getZoneEndpoint(GoogleCloudConstants.DEFAULT_ZONE)
                + GoogleCloudConstants.VOLUME_ENDPOINT;

        GetVolumeResponse volumeResponse = doRequestInstance(endpoint, jsonRequest, cloudUser);
        setAllocationToOrder(volumeOrder);

        return volumeResponse.getName();
    }

    @VisibleForTesting
    GetVolumeResponse doRequestInstance(String endpoint, String jsonRequest, GoogleCloudUser cloudUser)
            throws FogbowException {
        GetVolumeResponse volumeResponse = null;
        try {
            String jsonOperationResponse = this.client.doPostRequest(endpoint, jsonRequest, cloudUser);
            CreateVolumeResponse operation = doCreateVolumeResponseFrom(jsonOperationResponse);
            String jsonVolumeResponse = doGetResponseFromCloud(operation.getTargetLink(), cloudUser);
            volumeResponse = doGetVolumeResponseFrom(jsonVolumeResponse);
        } catch (FogbowException exception){ throw exception; }

        return volumeResponse;
    }

    @VisibleForTesting
    GetVolumeResponse doGetVolumeResponseFrom(String jsonResponse) throws InternalServerErrorException {
        try {
            return GetVolumeResponse.fromJson(jsonResponse);
        } catch (JsonSyntaxException e) {
            LOGGER.error(Messages.Log.ERROR_WHILE_GETTING_VOLUME_INSTANCE, e);
            throw new InternalServerErrorException(Messages.Exception.ERROR_WHILE_GETTING_VOLUME_INSTANCE);
        }
    }

    @VisibleForTesting
    CreateVolumeResponse doCreateVolumeResponseFrom(String jsonResponse) throws InternalServerErrorException {
        try {
            return CreateVolumeResponse.fromJson(jsonResponse);
        } catch (JsonSyntaxException e) {
            LOGGER.error(Messages.Log.ERROR_WHILE_GETTING_VOLUME_INSTANCE, e);
            throw new InternalServerErrorException(Messages.Exception.ERROR_WHILE_GETTING_VOLUME_INSTANCE);
        }
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
    public VolumeInstance getInstance(VolumeOrder volumeOrder, GoogleCloudUser cloudUser) throws FogbowException {
        String instanceName = volumeOrder.getName();
        LOGGER.info(String.format(Messages.Log.GETTING_INSTANCE_S, instanceName));
        String projectId = GoogleCloudPluginUtils.getProjectIdFrom(cloudUser);
        String endpoint = getPrefixEndpoint(projectId)
                + getZoneEndpoint(GoogleCloudConstants.DEFAULT_ZONE)
                + GoogleCloudConstants.VOLUME_ENDPOINT
                + GoogleCloudConstants.ENDPOINT_SEPARATOR
                + instanceName;

        return doGetInstance(endpoint, cloudUser);
    }

    @VisibleForTesting
    VolumeInstance doGetInstance(String endpoint, GoogleCloudUser cloudUser) throws FogbowException {
        String json = doGetResponseFromCloud(endpoint, cloudUser);
        GetVolumeResponse response = doGetVolumeResponseFrom(json);
        return buildVolumeInstanceFrom(response);
    }

    @VisibleForTesting
    String doGetResponseFromCloud(String endpoint, GoogleCloudUser cloudUser) throws FogbowException {
        String jsonResponse = this.client.doGetRequest(endpoint, cloudUser);
        return jsonResponse;
    }

    @VisibleForTesting
    VolumeInstance buildVolumeInstanceFrom(GetVolumeResponse response) {
        String id = response.getId();
        String status = response.getStatus();
        String name = response.getName();
        int size = response.getSize();
        return new VolumeInstance(id, status.toLowerCase(), name, size);
    }


    @Override
    public void deleteInstance(VolumeOrder volumeOrder, GoogleCloudUser cloudUser) throws FogbowException {
        String instanceId = volumeOrder.getInstanceId();
        LOGGER.info(String.format(Messages.Log.DELETING_INSTANCE_S, instanceId));
        String projectId = GoogleCloudPluginUtils.getProjectIdFrom(cloudUser);
        String endpoint = getPrefixEndpoint(projectId)
                + getZoneEndpoint(GoogleCloudConstants.DEFAULT_ZONE)
                + GoogleCloudConstants.VOLUME_ENDPOINT
                + GoogleCloudConstants.ENDPOINT_SEPARATOR
                + instanceId;

        doDeleteInstance(endpoint, cloudUser);
    }

    @VisibleForTesting
    void doDeleteInstance(String endpoint, GoogleCloudUser cloudUser) throws FogbowException {
        this.client.doDeleteRequest(endpoint, cloudUser);
    }

    @VisibleForTesting
    GetAllTypesResponse doGetAllTypesResponseFrom(String json) throws InternalServerErrorException {
        try {
            return GetAllTypesResponse.fromJson(json);
        } catch (Exception e) {
            LOGGER.error(Messages.Log.ERROR_WHILE_PROCESSING_VOLUME_REQUIREMENTS, e);
            throw new InternalServerErrorException(Messages.Exception.ERROR_WHILE_PROCESSING_VOLUME_REQUIREMENTS);
        }
    }

    @VisibleForTesting
    String findVolumeTypeSource(Map<String, String> requirements, String projectId, GoogleCloudUser cloudUser)
            throws FogbowException {

        if (requirements == null || requirements.isEmpty() || !requirements.containsKey(GoogleCloudConstants.Volume.VOLUME_TYPE)) {
            throw new InternalServerErrorException(Messages.Exception.ERROR_WHILE_PROCESSING_VOLUME_REQUIREMENTS);
        }

        String endpoint = getPrefixEndpoint(projectId)
                + getZoneEndpoint(GoogleCloudConstants.DEFAULT_ZONE)
                + GoogleCloudConstants.VOLUME_TYPES_ENDPOINT;

        String json = doGetResponseFromCloud(endpoint, cloudUser);
        GetAllTypesResponse response = doGetAllTypesResponseFrom(json);
        List<Type> types = response.getTypes();

        for (Type type : types) {
            if (type.getName().equals(requirements.get(GoogleCloudConstants.Volume.VOLUME_TYPE))) {
                return type.getSelfLink();
            }
        }
        String message = Messages.Exception.UNABLE_TO_MATCH_REQUIREMENTS;
        throw new UnacceptableOperationException(message);
    }

    private void initClient() {
        this.client = new GoogleCloudHttpClient();
    }
}