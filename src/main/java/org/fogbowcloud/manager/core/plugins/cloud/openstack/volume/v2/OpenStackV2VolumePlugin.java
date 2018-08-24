package org.fogbowcloud.manager.core.plugins.cloud.openstack.volume.v2;

import org.apache.http.client.HttpResponseException;
import org.apache.log4j.Logger;
import org.fogbowcloud.manager.core.HomeDir;
import org.fogbowcloud.manager.core.constants.DefaultConfigurationConstants;
import org.fogbowcloud.manager.core.exceptions.*;
import org.fogbowcloud.manager.core.models.ResourceType;
import org.fogbowcloud.manager.core.models.instances.InstanceState;
import org.fogbowcloud.manager.core.models.instances.VolumeInstance;
import org.fogbowcloud.manager.core.models.orders.VolumeOrder;
import org.fogbowcloud.manager.core.models.tokens.OpenStackV3Token;
import org.fogbowcloud.manager.core.plugins.cloud.VolumePlugin;
import org.fogbowcloud.manager.core.plugins.cloud.openstack.OpenStackHttpToFogbowManagerExceptionMapper;
import org.fogbowcloud.manager.core.plugins.cloud.openstack.OpenStackStateMapper;
import org.fogbowcloud.manager.util.PropertiesUtil;
import org.fogbowcloud.manager.util.connectivity.HttpRequestClientUtil;
import org.json.JSONException;

import java.util.Properties;

public class OpenStackV2VolumePlugin implements VolumePlugin<OpenStackV3Token> {

    private final String PROJECT_ID_IS_NOT_SPECIFIED_ERROR = "Project id is not specified.";

    private final String V2_API_ENDPOINT = "/v2/";
    protected static final String SUFIX_ENDPOINT_VOLUMES = "/volumes";

    public static final String VOLUME_NOVAV2_URL_KEY = "openstack_cinder_url";
    protected static final String DEFAULT_VOLUME_NAME = "fogbow-volume";

    private HttpRequestClientUtil client;
    private String volumeV2APIEndpoint;

    private static final Logger LOGGER = Logger.getLogger(OpenStackV2VolumePlugin.class);

    public OpenStackV2VolumePlugin() throws FatalErrorException {
        Properties properties = PropertiesUtil.readProperties(HomeDir.getPath() +
                DefaultConfigurationConstants.OPENSTACK_CONF_FILE_NAME);
        this.volumeV2APIEndpoint = properties.getProperty(VOLUME_NOVAV2_URL_KEY) + V2_API_ENDPOINT;

        initClient();
    }

    @Override
    public String requestInstance(VolumeOrder order, OpenStackV3Token openStackV3Token)
            throws FogbowManagerException, UnexpectedException {
        String tenantId = openStackV3Token.getProjectId();
        if (tenantId == null) {
            LOGGER.error(PROJECT_ID_IS_NOT_SPECIFIED_ERROR);
            throw new UnauthenticatedUserException(PROJECT_ID_IS_NOT_SPECIFIED_ERROR);
        }

        String jsonRequest = null;
        try {
            String size = String.valueOf(order.getVolumeSize());
            String name = order.getVolumeName();
            jsonRequest = generateJsonEntityToCreateInstance(size, name);
        } catch (JSONException e) {
            String errorMsg = "An error occurred when generating json.";
            LOGGER.error(errorMsg, e);
            throw new InvalidParameterException(errorMsg, e);
        }

        String endpoint = this.volumeV2APIEndpoint + tenantId + SUFIX_ENDPOINT_VOLUMES;
        String responseStr = null;
        try {
            responseStr = this.client.doPostRequest(endpoint, openStackV3Token, jsonRequest);
        } catch (HttpResponseException e) {
            OpenStackHttpToFogbowManagerExceptionMapper.map(e);
        }
        VolumeInstance instanceFromJson = getInstanceFromJson(responseStr);
        return instanceFromJson != null ? instanceFromJson.getId() : null;
    }

    @Override
    public VolumeInstance getInstance(String storageOrderInstanceId, OpenStackV3Token openStackV3Token)
            throws FogbowManagerException, UnexpectedException {
        String tenantId = openStackV3Token.getProjectId();
        if (tenantId == null) {
            LOGGER.error(PROJECT_ID_IS_NOT_SPECIFIED_ERROR);
            throw new UnauthenticatedUserException(PROJECT_ID_IS_NOT_SPECIFIED_ERROR);
        }

        String endpoint = this.volumeV2APIEndpoint + tenantId
                + SUFIX_ENDPOINT_VOLUMES + "/" + storageOrderInstanceId;
        String responseStr = null;
        try {
            responseStr = this.client.doGetRequest(endpoint, openStackV3Token);
        } catch (HttpResponseException e) {
            OpenStackHttpToFogbowManagerExceptionMapper.map(e);
        }
        return getInstanceFromJson(responseStr);
    }

    @Override
    public void deleteInstance(String storageOrderInstanceId, OpenStackV3Token openStackV3Token)
            throws FogbowManagerException, UnexpectedException {
        String tenantId = openStackV3Token.getProjectId();
        if (tenantId == null) {
            LOGGER.error(PROJECT_ID_IS_NOT_SPECIFIED_ERROR);
            throw new UnauthenticatedUserException(PROJECT_ID_IS_NOT_SPECIFIED_ERROR);
        }

        String endpoint = this.volumeV2APIEndpoint + tenantId
                + SUFIX_ENDPOINT_VOLUMES + "/" + storageOrderInstanceId;
        try {
            this.client.doDeleteRequest(endpoint, openStackV3Token);
        } catch (HttpResponseException e) {
            OpenStackHttpToFogbowManagerExceptionMapper.map(e);
        }
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
            String errorMsg = "There was an exception while getting volume instance.";
            LOGGER.error(errorMsg, e);
            throw new UnexpectedException(errorMsg, e);
        }
    }

    protected String generateJsonEntityToCreateInstance(String size, String name) throws JSONException {
        CreateVolumeRequest createVolumeRequest =
                new CreateVolumeRequest.Builder()
                        .name(name)
                        .size(size)
                        .build();

        return createVolumeRequest.toJson();
    }

    private void initClient() {
        this.client = new HttpRequestClientUtil();
    }

    public void setClient(HttpRequestClientUtil client) {
        this.client = client;
    }
}