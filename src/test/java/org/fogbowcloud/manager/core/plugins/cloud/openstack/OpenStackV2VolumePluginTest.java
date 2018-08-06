package org.fogbowcloud.manager.core.plugins.cloud.openstack;

import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;

import org.apache.http.client.HttpResponseException;
import org.fogbowcloud.manager.core.HomeDir;
import org.fogbowcloud.manager.core.PropertiesHolder;
import org.fogbowcloud.manager.core.exceptions.FogbowManagerException;
import org.fogbowcloud.manager.core.exceptions.UnexpectedException;
import org.fogbowcloud.manager.core.models.instances.VolumeInstance;
import org.fogbowcloud.manager.core.models.orders.VolumeOrder;
import org.fogbowcloud.manager.core.models.tokens.Token;
import org.fogbowcloud.manager.core.plugins.serialization.openstack.OpenstackRestApiConstants;
import org.fogbowcloud.manager.util.connectivity.HttpRequestClientUtil;
import org.json.JSONException;
import org.json.JSONObject;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;

import com.google.gson.JsonObject;

public class OpenStackV2VolumePluginTest {

    private final String FAKE_STORAGE_URL = "http://localhost:0000";
    private final String FAKE_SIZE = "2";
    private final String FAKE_VOLUME_ID = "fake-id";
    private final String FAKE_NAME = "fake-name";
    private final String FAKE_ACCESS_ID = "access-id";
    private final String FAKE_INSTANCE_ID = "instance-id";

    // TODO create this json with a libary
    private final String FAKE_VOLUME_JSON = "{\"volume\":{\"size\":2,\"name\":\"fake-name\", " +
            "\"id\": \"fake-id\", \"status\": \"fake-status\"}}";

    private OpenStackV2VolumePlugin openStackV2VolumePlugin;
    private Token tokenDefault;
    private HttpRequestClientUtil httpRequestClientUtil;

    @Before
    public void setUp() throws Exception {
        HomeDir.getInstance().setPath("src/test/resources/private");
        PropertiesHolder propertiesHolder = PropertiesHolder.getInstance();
        Properties properties = propertiesHolder.getProperties();
        properties.put(OpenStackV2VolumePlugin.VOLUME_NOVAV2_URL_KEY, FAKE_STORAGE_URL);

        this.openStackV2VolumePlugin = Mockito.spy(new OpenStackV2VolumePlugin());
        this.httpRequestClientUtil = Mockito.mock(HttpRequestClientUtil.class);
        this.openStackV2VolumePlugin.setClient(this.httpRequestClientUtil);

        Map<String, String> attributes = new HashMap<>();
        attributes.put("tenantId", "tenant-id");
        Token.User tokenUser = new Token.User("user", "user");
        this.tokenDefault = new Token(FAKE_ACCESS_ID, tokenUser, new Date(), attributes);
    }

    // test case: Check if the request in requestInstance() is executed properly with the right parameters.
    @Test
    public void testRequestInstance() throws FogbowManagerException, UnexpectedException, HttpResponseException {
        // set up
        VolumeOrder volumeOrder = Mockito.mock(VolumeOrder.class);

        Mockito.doReturn(FAKE_VOLUME_JSON).when(this.httpRequestClientUtil).doPostRequest(
                Mockito.anyString(), Mockito.any(Token.class), Mockito.any());

        // exercise
        String instanceString = this.openStackV2VolumePlugin.requestInstance(volumeOrder, this.tokenDefault);

        // verify
        Mockito.verify(this.httpRequestClientUtil).doPostRequest(Mockito.anyString(), Mockito.any(Token.class), Mockito.any());
        Assert.assertEquals(FAKE_VOLUME_ID, instanceString);
    }

    // test case: Requesting an instance without tenant ID must return an exception.
    @Test(expected=FogbowManagerException.class)
    public void testRequestInstanceWithoutTenantId() throws FogbowManagerException, UnexpectedException {
        // set up
        this.tokenDefault.getAttributes().clear();

        int size = 2;
        VolumeOrder order = new VolumeOrder(FAKE_VOLUME_ID, null, "", "", size, FAKE_NAME);

        // exercise
        this.openStackV2VolumePlugin.requestInstance(order, this.tokenDefault);
    }

    // test case: Tests if generateJsonEntityToCreateInstance is returning the volume Json properly.
    @Test
    public void testGenerateJsonEntityToCreateInstance() {
        // exercise
        String entity = this.openStackV2VolumePlugin.generateJsonEntityToCreateInstance(FAKE_SIZE, FAKE_NAME);
        JSONObject jsonEntity = new JSONObject(entity);

        // verify
        Assert.assertEquals(FAKE_SIZE, jsonEntity.getJSONObject(OpenstackRestApiConstants.Volume.VOLUME_KEY_JSON)
                .getString(OpenstackRestApiConstants.Volume.SIZE_KEY_JSON));
    }

    // test case: Tests if given a volume Json, the getInstanceFromJson() returns the right VolumeInstance.
    @Test
    public void testGetInstanceFromJson() throws FogbowManagerException, JSONException, UnexpectedException {
        // exercise
        String instanceJsonResponseStr = generateInstanceJsonResponse(FAKE_INSTANCE_ID).toString();
		VolumeInstance instance = this.openStackV2VolumePlugin.getInstanceFromJson(instanceJsonResponseStr);

        // verify
        Assert.assertEquals(FAKE_INSTANCE_ID, instance.getId());
    }

    // test case: Check if the request in getInstance() is executed properly with the right parameters.
    @Test
    public void testGetInstance() throws UnexpectedException, FogbowManagerException, HttpResponseException {
        // set up
        Mockito.doReturn(FAKE_VOLUME_JSON).when(
                this.httpRequestClientUtil).doGetRequest(Mockito.anyString(), Mockito.any(Token.class));

        // exercise
        VolumeInstance volumeInstance = this.openStackV2VolumePlugin.getInstance(FAKE_INSTANCE_ID, this.tokenDefault);

        // verify
        Mockito.verify(this.httpRequestClientUtil).doGetRequest(Mockito.anyString(), Mockito.any(Token.class));
        Assert.assertEquals(FAKE_NAME, volumeInstance.getName());
        Assert.assertEquals(Integer.parseInt(FAKE_SIZE), volumeInstance.getSize());
    }

    // test case: Getting an instance without a tenant ID must raise FogbowManagerException.
    @Test(expected=FogbowManagerException.class)
    public void testGetInstancesWithoutTenantId() throws Exception {
        // set up
        this.tokenDefault.getAttributes().clear();

        // exercise
        this.openStackV2VolumePlugin.getInstance(FAKE_ACCESS_ID, this.tokenDefault);
    }

    // test case: Check if the request in deleteInstance() is executed properly with the right parameters.
    @Test
    public void removeInstance() throws UnexpectedException, FogbowManagerException, HttpResponseException {
        // set up
        Mockito.doNothing().when(this.httpRequestClientUtil).doDeleteRequest(Mockito.anyString(), Mockito.any(Token.class));

        // exercise
        this.openStackV2VolumePlugin.deleteInstance(FAKE_INSTANCE_ID, this.tokenDefault);

        // verify
        Mockito.verify(this.httpRequestClientUtil).doDeleteRequest(Mockito.anyString(), Mockito.any(Token.class));
    }

    // test case: Deleting an instance without a tenant ID must raise FogbowManagerException.
    @Test(expected=FogbowManagerException.class)
    public void testRemoveInstanceWithoutTenantId() throws Exception {
        // set up
        this.tokenDefault.getAttributes().clear();

        // exercise
        this.openStackV2VolumePlugin.deleteInstance(FAKE_INSTANCE_ID, tokenDefault);
    }

    private JsonObject generateInstanceJsonResponse(String instanceId) throws JSONException {
        JsonObject instanceJsonResponse = new JsonObject();
        
        instanceJsonResponse.addProperty(OpenstackRestApiConstants.Volume.ID_KEY_JSON, instanceId);
        instanceJsonResponse.addProperty(OpenstackRestApiConstants.Volume.SIZE_KEY_JSON, 0);
        instanceJsonResponse.addProperty(OpenstackRestApiConstants.Volume.STATUS_KEY_JSON, OpenStackStateMapper.ACTIVE_STATUS);
        instanceJsonResponse.addProperty(OpenstackRestApiConstants.Volume.NAME_KEY_JSON, "anyName");
        JsonObject volumeJsonObject = new JsonObject();
        volumeJsonObject.add(OpenstackRestApiConstants.Volume.VOLUME_KEY_JSON, instanceJsonResponse);
        return volumeJsonObject;
    }
}