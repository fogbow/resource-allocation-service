package org.fogbowcloud.ras.core.plugins.interoperability.openstack.volume.v2;

import org.apache.http.client.HttpResponseException;
import org.fogbowcloud.ras.core.PropertiesHolder;
import org.fogbowcloud.ras.core.exceptions.FogbowRasException;
import org.fogbowcloud.ras.core.exceptions.UnexpectedException;
import org.fogbowcloud.ras.core.models.instances.VolumeInstance;
import org.fogbowcloud.ras.core.models.orders.VolumeOrder;
import org.fogbowcloud.ras.core.models.tokens.OpenStackV3Token;
import org.fogbowcloud.ras.core.models.tokens.Token;
import org.fogbowcloud.ras.core.plugins.interoperability.openstack.OpenstackRestApiConstants;
import org.fogbowcloud.ras.util.connectivity.HttpRequestClientUtil;
import org.json.JSONException;
import org.json.JSONObject;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;

import java.util.Properties;

public class OpenStackV2VolumePluginTest {

    private final String FAKE_STORAGE_URL = "http://localhost:0000";
    private final String FAKE_SIZE = "2";
    private final String FAKE_VOLUME_ID = "fake-id";
    private final String FAKE_NAME = "fake-name";
    private final String FAKE_TOKEN_PROVIDER = "fake-token-provider";
    private final String FAKE_TOKEN_VALUE = "fake-token-value";
    private final String FAKE_USER_ID = "fake-user-id";
    private final String FAKE_PROJECT_ID = "fake-project-id";
    private final String FAKE_PROJECT_NAME = "fake-project-name";
    private final String FAKE_INSTANCE_ID = "instance-id";

    // TODO create this json with a library
    private final String FAKE_VOLUME_JSON = "{\"volume\":{\"size\":2,\"name\":\"fake-name\", " +
            "\"id\": \"fake-id\", \"status\": \"fake-status\"}}";

    private OpenStackV2VolumePlugin openStackV2VolumePlugin;
    private OpenStackV3Token openStackV3Token;
    private HttpRequestClientUtil httpRequestClientUtil;

    @Before
    public void setUp() throws Exception {
        PropertiesHolder propertiesHolder = PropertiesHolder.getInstance();
        Properties properties = propertiesHolder.getProperties();
        properties.put(OpenStackV2VolumePlugin.VOLUME_NOVAV2_URL_KEY, FAKE_STORAGE_URL);

        this.openStackV2VolumePlugin = Mockito.spy(new OpenStackV2VolumePlugin());
        this.httpRequestClientUtil = Mockito.mock(HttpRequestClientUtil.class);
        this.openStackV2VolumePlugin.setClient(this.httpRequestClientUtil);
        this.openStackV3Token = new OpenStackV3Token(FAKE_TOKEN_PROVIDER, FAKE_TOKEN_VALUE, FAKE_USER_ID,
                FAKE_NAME, FAKE_PROJECT_ID, FAKE_PROJECT_NAME);
    }

    // test case: Check if the request in requestInstance() is executed properly with the right parameters.
    @Test
    public void testRequestInstance() throws FogbowRasException, UnexpectedException, HttpResponseException {
        // set up
        VolumeOrder volumeOrder = Mockito.mock(VolumeOrder.class);

        Mockito.doReturn(FAKE_VOLUME_JSON).when(this.httpRequestClientUtil).doPostRequest(
                Mockito.anyString(), Mockito.any(Token.class), Mockito.any());

        // exercise
        String instanceString = this.openStackV2VolumePlugin.requestInstance(volumeOrder, this.openStackV3Token);

        // verify
        Mockito.verify(this.httpRequestClientUtil).doPostRequest(Mockito.anyString(), Mockito.any(Token.class),
                Mockito.any());
        Assert.assertEquals(FAKE_VOLUME_ID, instanceString);
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
    public void testGetInstanceFromJson() throws FogbowRasException, JSONException, UnexpectedException {
        // exercise
        VolumeInstance instance = this.openStackV2VolumePlugin.getInstanceFromJson(FAKE_VOLUME_JSON);

        // verify
        Assert.assertEquals(FAKE_VOLUME_ID, instance.getId());
    }

    // test case: Check if the request in getInstance() is executed properly with the right parameters.
    @Test
    public void testGetInstance() throws UnexpectedException, FogbowRasException, HttpResponseException {
        // set up
        Mockito.doReturn(FAKE_VOLUME_JSON).when(
                this.httpRequestClientUtil).doGetRequest(Mockito.anyString(), Mockito.any(Token.class));

        // exercise
        VolumeInstance volumeInstance = this.openStackV2VolumePlugin.getInstance(FAKE_INSTANCE_ID,
                this.openStackV3Token);

        // verify
        Mockito.verify(this.httpRequestClientUtil).doGetRequest(Mockito.anyString(), Mockito.any(Token.class));
        Assert.assertEquals(FAKE_NAME, volumeInstance.getName());
        Assert.assertEquals(Integer.parseInt(FAKE_SIZE), volumeInstance.getSize());
    }

    // test case: Check if the request in deleteInstance() is executed properly with the right parameters.
    @Test
    public void removeInstance() throws UnexpectedException, FogbowRasException, HttpResponseException {
        // set up
        Mockito.doNothing().when(this.httpRequestClientUtil).doDeleteRequest(Mockito.anyString(),
                Mockito.any(Token.class));

        // exercise
        this.openStackV2VolumePlugin.deleteInstance(FAKE_INSTANCE_ID, this.openStackV3Token);

        // verify
        Mockito.verify(this.httpRequestClientUtil).doDeleteRequest(Mockito.anyString(), Mockito.any(Token.class));
    }

    // test case: Deleting an instance without a project ID must raise FogbowRasException.
    @Test(expected = FogbowRasException.class)
    public void testRemoveInstanceWithoutProjectId() throws Exception {
        // set up
        this.openStackV3Token.setProjectId(null);

        // exercise
        this.openStackV2VolumePlugin.deleteInstance(FAKE_INSTANCE_ID, this.openStackV3Token);
    }

    @Test
    public void getInstanceState() {
        // TODO
    }

}