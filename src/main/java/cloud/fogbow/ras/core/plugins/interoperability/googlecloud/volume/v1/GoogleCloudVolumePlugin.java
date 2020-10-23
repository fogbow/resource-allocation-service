package cloud.fogbow.ras.core.plugins.interoperability.googlecloud.volume.v1;

import cloud.fogbow.common.exceptions.FatalErrorException;
import cloud.fogbow.common.exceptions.FogbowException;
import cloud.fogbow.common.exceptions.InternalServerErrorException;
import cloud.fogbow.common.models.GoogleCloudUser;
import cloud.fogbow.common.util.PropertiesUtil;
import cloud.fogbow.common.util.connectivity.cloud.googlecloud.GoogleCloudHttpClient;
import cloud.fogbow.ras.api.http.response.VolumeInstance;
import cloud.fogbow.ras.api.http.response.quotas.allocation.VolumeAllocation;
import cloud.fogbow.ras.constants.Messages;
import cloud.fogbow.ras.core.models.orders.VolumeOrder;
import cloud.fogbow.ras.core.plugins.interoperability.VolumePlugin;
import cloud.fogbow.ras.core.plugins.interoperability.googlecloud.models.volume.CreateVolumeRequest;
import cloud.fogbow.ras.core.plugins.interoperability.googlecloud.models.volume.GetAllTypesResponse;
import cloud.fogbow.ras.core.plugins.interoperability.googlecloud.models.volume.GetVolumeResponse;
import cloud.fogbow.ras.core.plugins.interoperability.googlecloud.util.GoogleCloudConstants;
import cloud.fogbow.ras.core.plugins.interoperability.googlecloud.util.GoogleCloudPluginUtils;
import com.google.common.annotations.VisibleForTesting;
import com.google.gson.JsonSyntaxException;
import org.apache.log4j.Logger;
import org.json.JSONException;

import java.util.Map;
import java.util.Properties;

public class GoogleCloudVolumePlugin implements VolumePlugin<GoogleCloudUser> {
    private static final Logger LOGGER = Logger.getLogger(GoogleCloudVolumePlugin.class);
    private static final String EMPTY_STRING = "";


    private Properties properties;
    private GoogleCloudHttpClient client;

    public GoogleCloudVolumePlugin( String confFilePath) throws FatalErrorException {
        this.properties = PropertiesUtil.readProperties(confFilePath);
        initClient();
    }

    @Override
    public boolean isReady(String instanceState) {
        return false; //TODO
    }

    @Override
    public boolean hasFailed(String instanceState) {
        return false; //TODO
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
    String getPrefixEndpoint(String projectId) {
        return this.properties.getProperty(GoogleCloudPluginUtils.VOLUME_COMPUTE_URL_KEY) +
                GoogleCloudConstants.COMPUTE_V1_API_ENDPOINT + GoogleCloudConstants.ENDPOINT_SEPARATOR + projectId;
    }

    @Override
    public String requestInstance(VolumeOrder volumeOrder, GoogleCloudUser cloudUser) throws FogbowException {
        LOGGER.info(Messages.Log.REQUESTING_INSTANCE_FROM_PROVIDER);
        String projectId = GoogleCloudPluginUtils.getProjectIdFrom(cloudUser);
        String size = String.valueOf(volumeOrder.getVolumeSize());
        String name = volumeOrder.getName();
        String volumeTypeId = findVolumeTypeId(volumeOrder.getRequirements(), projectId, cloudUser);
        String jsonRequest = generateJsonRequest(size, name, volumeTypeId);
        String endpoint = getPrefixEndpoint(projectId) + GoogleCloudConstants.VOLUME_ENDPOINT;

        GetVolumeResponse volumeResponse = doRequestInstance(endpoint, jsonRequest, cloudUser);
        setAllocationToOrder(volumeOrder);

        return volumeResponse.getId();
    }

    @VisibleForTesting
    GetVolumeResponse doRequestInstance(String endpoint, String jsonRequest, GoogleCloudUser cloudUser)
            throws FogbowException {
        return null;    //  TODO
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
    void setAllocationToOrder(VolumeOrder order) {
        synchronized (order) {
            int size = order.getVolumeSize();
            VolumeAllocation volumeAllocation = new VolumeAllocation(size);
            order.setActualAllocation(volumeAllocation);
        }
    }



    @Override
    public VolumeInstance getInstance(VolumeOrder volumeOrder, GoogleCloudUser cloudUser) throws FogbowException {
        String instanceId = volumeOrder.getInstanceId();
        LOGGER.info(String.format(Messages.Log.GETTING_INSTANCE_S, instanceId));
        String projectId = GoogleCloudPluginUtils.getProjectIdFrom(cloudUser);
        String endpoint = getPrefixEndpoint(projectId)
                + GoogleCloudConstants.VOLUME_ENDPOINT
                + GoogleCloudConstants.ENDPOINT_SEPARATOR
                + volumeOrder.getInstanceId();

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
        return new VolumeInstance(id, status, name, size);
    }


    @Override
    public void deleteInstance(VolumeOrder volumeOrder, GoogleCloudUser cloudUser) throws FogbowException {
        String instanceId = volumeOrder.getInstanceId();
        LOGGER.info(String.format(Messages.Log.DELETING_INSTANCE_S, instanceId));
        String projectId = GoogleCloudPluginUtils.getProjectIdFrom(cloudUser);
        String endpoint = getPrefixEndpoint(projectId)
                + GoogleCloudConstants.VOLUME_ENDPOINT
                + GoogleCloudConstants.ENDPOINT_SEPARATOR
                + volumeOrder.getInstanceId();

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
    String findVolumeTypeId(Map<String, String> requirements, String projectId, GoogleCloudUser cloudUser)
            throws FogbowException {
        return EMPTY_STRING; // TODO
    }
    private void initClient() {
        this.client = new GoogleCloudHttpClient();
    }
}
