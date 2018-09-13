package org.fogbowcloud.ras.core.plugins.interoperability.openstack.volume.v2;

import org.apache.http.client.HttpResponseException;
import org.apache.log4j.Logger;
import org.fogbowcloud.ras.core.HomeDir;
import org.fogbowcloud.ras.core.constants.DefaultConfigurationConstants;
import org.fogbowcloud.ras.core.constants.Messages;
import org.fogbowcloud.ras.core.exceptions.*;
import org.fogbowcloud.ras.core.models.ResourceType;
import org.fogbowcloud.ras.core.models.instances.InstanceState;
import org.fogbowcloud.ras.core.models.instances.VolumeInstance;
import org.fogbowcloud.ras.core.models.orders.VolumeOrder;
import org.fogbowcloud.ras.core.models.tokens.OpenStackV3Token;
import org.fogbowcloud.ras.core.plugins.interoperability.VolumePlugin;
import org.fogbowcloud.ras.core.plugins.interoperability.openstack.OpenStackHttpToFogbowRasExceptionMapper;
import org.fogbowcloud.ras.core.plugins.interoperability.openstack.OpenStackStateMapper;
import org.fogbowcloud.ras.util.PropertiesUtil;
import org.fogbowcloud.ras.util.connectivity.HttpRequestClientUtil;
import org.json.JSONException;

import java.util.Properties;

public class OpenStackV2VolumePlugin implements VolumePlugin<OpenStackV3Token> {
    private static final Logger LOGGER = Logger.getLogger(OpenStackV2VolumePlugin.class);

    private final String V2_API_ENDPOINT = "/v2/";
    protected static final String SUFIX_ENDPOINT_VOLUMES = "/volumes";
    public static final String VOLUME_NOVAV2_URL_KEY = "openstack_cinder_url";
    private HttpRequestClientUtil client;
    private String volumeV2APIEndpoint;

    public OpenStackV2VolumePlugin() throws FatalErrorException {
        Properties properties = PropertiesUtil.readProperties(HomeDir.getPath() +
                DefaultConfigurationConstants.OPENSTACK_CONF_FILE_NAME);
        this.volumeV2APIEndpoint = properties.getProperty(VOLUME_NOVAV2_URL_KEY) + V2_API_ENDPOINT;
        initClient();
    }

    @Override
    public String requestInstance(VolumeOrder order, OpenStackV3Token openStackV3Token)
            throws FogbowRasException, UnexpectedException {
        String tenantId = openStackV3Token.getProjectId();
        if (tenantId == null) {
            String message = Messages.Error.PROJECT_ID_NOT_SPECIFIED;
        	LOGGER.error(message);
            throw new UnauthenticatedUserException(message);
        }

        String jsonRequest = null;
        try {
            String size = String.valueOf(order.getVolumeSize());
            String name = order.getName();
            jsonRequest = generateJsonEntityToCreateInstance(size, name);
        } catch (JSONException e) {
        	String message = Messages.Error.COULD_NOT_GENERATING_JSON;
            LOGGER.error(message, e);
            throw new InvalidParameterException(message, e);
        }

        String endpoint = this.volumeV2APIEndpoint + tenantId + SUFIX_ENDPOINT_VOLUMES;
        String responseStr = null;
        try {
            responseStr = this.client.doPostRequest(endpoint, openStackV3Token, jsonRequest);
        } catch (HttpResponseException e) {
            OpenStackHttpToFogbowRasExceptionMapper.map(e);
        }
        VolumeInstance instanceFromJson = getInstanceFromJson(responseStr);
        return instanceFromJson != null ? instanceFromJson.getId() : null;
    }

    @Override
    public VolumeInstance getInstance(String storageOrderInstanceId, OpenStackV3Token openStackV3Token)
            throws FogbowRasException, UnexpectedException {
        String tenantId = openStackV3Token.getProjectId();
        if (tenantId == null) {
            String message = Messages.Error.PROJECT_ID_NOT_SPECIFIED;
        	LOGGER.error(message);
            throw new UnauthenticatedUserException(message);
        }

        String endpoint = this.volumeV2APIEndpoint + tenantId
                + SUFIX_ENDPOINT_VOLUMES + "/" + storageOrderInstanceId;
        String responseStr = null;
        try {
            responseStr = this.client.doGetRequest(endpoint, openStackV3Token);
        } catch (HttpResponseException e) {
            OpenStackHttpToFogbowRasExceptionMapper.map(e);
        }
        return getInstanceFromJson(responseStr);
    }

    @Override
    public void deleteInstance(String storageOrderInstanceId, OpenStackV3Token openStackV3Token)
            throws FogbowRasException, UnexpectedException {
        String tenantId = openStackV3Token.getProjectId();
        if (tenantId == null) {
            String message = Messages.Error.PROJECT_ID_NOT_SPECIFIED;
        	LOGGER.error(message);
            throw new UnauthenticatedUserException(message);
        }

        String endpoint = this.volumeV2APIEndpoint + tenantId
                + SUFIX_ENDPOINT_VOLUMES + "/" + storageOrderInstanceId;
        try {
            this.client.doDeleteRequest(endpoint, openStackV3Token);
        } catch (HttpResponseException e) {
            OpenStackHttpToFogbowRasExceptionMapper.map(e);
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
        	String message = Messages.Error.WHILE_GETTING_VOLUME_INSTANCE;
            LOGGER.error(message, e);
            throw new UnexpectedException(message, e);
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