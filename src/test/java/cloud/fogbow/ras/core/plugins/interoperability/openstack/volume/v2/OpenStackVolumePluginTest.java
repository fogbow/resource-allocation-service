package cloud.fogbow.ras.core.plugins.interoperability.openstack.volume.v2;

import cloud.fogbow.common.constants.OpenStackConstants;
import cloud.fogbow.common.exceptions.FogbowException;
import cloud.fogbow.common.models.OpenStackV3User;
import cloud.fogbow.common.util.HomeDir;
import cloud.fogbow.common.util.connectivity.cloud.openstack.OpenStackHttpClient;
import cloud.fogbow.ras.constants.SystemConstants;
import cloud.fogbow.ras.core.PropertiesHolder;
import cloud.fogbow.ras.api.http.response.VolumeInstance;
import cloud.fogbow.ras.core.models.orders.VolumeOrder;
import org.apache.http.client.HttpResponseException;
import org.json.JSONException;
import org.json.JSONObject;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;

import java.io.File;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;

public class OpenStackVolumePluginTest {

    private final String FAKE_STORAGE_URL = "http://localhost:0000";
    private final String FAKE_SIZE = "2";
    private final String FAKE_VOLUME_ID = "fake-id";
    private final String FAKE_NAME = "fake-name";
    private final String FAKE_VOLUME_TYPE = "fake-type";
    private final String FAKE_TOKEN_VALUE = "fake-token-value";
    private final String FAKE_USER_ID = "fake-user-id";
    private final String FAKE_PROJECT_ID = "fake-project-id";
    private final String FAKE_INSTANCE_ID = "instance-id";

    // TODO create this json with a library
    private final String FAKE_VOLUME_JSON = "{\"volume\":{\"size\":2,\"name\":\"fake-name\", " +
            "\"id\": \"fake-id\", \"status\": \"fake-status\"}}";
    private final String FAKE_TYPES_JSON = "{\"volume_types\": [" +
          "{\"extra_specs\": {\"fake-capabilities\": \"fake-value\" }," +
          "\"id\": \"fake-id\"," +
          "\"name\": \"SSD\"}]}";

    private OpenStackVolumePlugin openStackVolumePlugin;
    private OpenStackV3User openStackV3Token;
    private OpenStackHttpClient client;

    @Before
    public void setUp() throws Exception {
        PropertiesHolder propertiesHolder = PropertiesHolder.getInstance();
        Properties properties = propertiesHolder.getProperties();
        properties.put(OpenStackVolumePlugin.VOLUME_NOVAV2_URL_KEY, FAKE_STORAGE_URL);
        String cloudConfPath = HomeDir.getPath() + SystemConstants.CLOUDS_CONFIGURATION_DIRECTORY_NAME + File.separator
                + "default" + File.separator + SystemConstants.CLOUD_SPECIFICITY_CONF_FILE_NAME;
        this.openStackVolumePlugin = Mockito.spy(new OpenStackVolumePlugin(cloudConfPath));
        this.client = Mockito.mock(OpenStackHttpClient.class);
        this.openStackVolumePlugin.setClient(this.client);
        this.openStackV3Token = new OpenStackV3User(FAKE_USER_ID, FAKE_NAME, FAKE_TOKEN_VALUE, FAKE_PROJECT_ID);
    }

    // test case: Check if the request in requestInstance() is executed properly with the right parameters.
    @Test
    public void testRequestInstance() throws FogbowException, HttpResponseException {
        // set up
        VolumeOrder volumeOrder = Mockito.mock(VolumeOrder.class);

        Mockito.doReturn(FAKE_VOLUME_JSON).when(this.client).doPostRequest(
                Mockito.anyString(), Mockito.any(), Mockito.any(OpenStackV3User.class));

        // exercise
        String instanceString = this.openStackVolumePlugin.requestInstance(volumeOrder, this.openStackV3Token);

        // verify
        Mockito.verify(this.client).doPostRequest(Mockito.anyString(), Mockito.any(), Mockito.any(OpenStackV3User.class)
        );
        Assert.assertEquals(FAKE_VOLUME_ID, instanceString);
    }

    // test case: Check if the request in requestInstance() is executed properly with the right parameters even when
    // there is volume extra requirements.
    @Test
    public void testRequestInstanceWithRequirements() throws FogbowException, HttpResponseException {
        // set up
        VolumeOrder volumeOrder = new VolumeOrder(null, null, "fake-name", 2);
        Map<String, String> requirements = new HashMap<>();
        requirements.put("fake-capabilities", "fake-value");
        volumeOrder.setRequirements(requirements);

        Mockito.doReturn(FAKE_TYPES_JSON).when(this.client).doGetRequest(
                Mockito.anyString(), Mockito.any(OpenStackV3User.class));
        Mockito.doReturn(FAKE_VOLUME_JSON).when(this.client).doPostRequest(
                Mockito.anyString(), Mockito.any(), Mockito.any(OpenStackV3User.class));

        // exercise
        String instanceString = this.openStackVolumePlugin.requestInstance(volumeOrder, this.openStackV3Token);

        // verify
        Mockito.verify(this.client).doGetRequest(Mockito.anyString(), Mockito.any(OpenStackV3User.class));
        Mockito.verify(this.client).doPostRequest(Mockito.anyString(), Mockito.any(), Mockito.any(OpenStackV3User.class)
        );
        Assert.assertEquals(FAKE_VOLUME_ID, instanceString);
    }

    // test case: requestInstance() should raise FogbowException in case requirement is not found
    @Test(expected = FogbowException.class)
    public void testRequestInstanceWithRequirementsFail() throws FogbowException, HttpResponseException {
        // set up
        VolumeOrder volumeOrder = new VolumeOrder(null, null, "fake-name", 2);
        Map<String, String> requirements = new HashMap<>();
        requirements.put("fake-capabilities", "fake-value");
        requirements.put("additional-fake-capabilities", "additional-fake-value");
        volumeOrder.setRequirements(requirements);

        Mockito.doReturn(FAKE_TYPES_JSON).when(this.client).doGetRequest(
                Mockito.anyString(), Mockito.any(OpenStackV3User.class));

        // exercise
        String instanceString = this.openStackVolumePlugin.requestInstance(volumeOrder, this.openStackV3Token);

        // verify
        Mockito.verify(this.client).doGetRequest(Mockito.anyString(), Mockito.any(OpenStackV3User.class));
    }

    // test case: Tests if generateJsonEntityToCreateInstance is returning the volume Json properly.
    @Test
    public void testGenerateJsonEntityToCreateInstance() {
        // exercise
        String entity = this.openStackVolumePlugin.generateJsonEntityToCreateInstance(FAKE_SIZE, FAKE_NAME, FAKE_VOLUME_TYPE);
        JSONObject jsonEntity = new JSONObject(entity);

        // verify
        Assert.assertEquals(FAKE_SIZE, jsonEntity.getJSONObject(OpenStackConstants.Volume.VOLUME_KEY_JSON)
                .getString(OpenStackConstants.Volume.SIZE_KEY_JSON));
    }

    // test case: Tests if given a volume Json, the getInstanceFromJson() returns the right VolumeInstance.
    @Test
    public void testGetInstanceFromJson() throws FogbowException, JSONException {
        // exercise
        VolumeInstance instance = this.openStackVolumePlugin.getInstanceFromJson(FAKE_VOLUME_JSON);

        // verify
        Assert.assertEquals(FAKE_VOLUME_ID, instance.getId());
    }

    // test case: Check if the request in getInstance() is executed properly with the right parameters.
    @Test
    public void testGetInstance() throws FogbowException, HttpResponseException {
        // set up
        Mockito.doReturn(FAKE_VOLUME_JSON).when(
                this.client).doGetRequest(Mockito.anyString(), Mockito.any(OpenStackV3User.class));

        VolumeOrder volumeOrder = new VolumeOrder();
        volumeOrder.setInstanceId(FAKE_INSTANCE_ID);

        // exercise
        VolumeInstance volumeInstance = this.openStackVolumePlugin.getInstance(volumeOrder,
                this.openStackV3Token);

        // verify
        Mockito.verify(this.client).doGetRequest(Mockito.anyString(), Mockito.any(OpenStackV3User.class));
        Assert.assertEquals(FAKE_NAME, volumeInstance.getName());
        Assert.assertEquals(Integer.parseInt(FAKE_SIZE), volumeInstance.getVolumeSize());
    }

    // test case: Check if the request in deleteInstance() is executed properly with the right parameters.
    @Test
    public void removeInstance() throws FogbowException, HttpResponseException {
        // set up
        Mockito.doNothing().when(this.client).doDeleteRequest(Mockito.anyString(),
                Mockito.any(OpenStackV3User.class));

        VolumeOrder volumeOrder = new VolumeOrder();
        volumeOrder.setInstanceId(FAKE_INSTANCE_ID);

        // exercise
        this.openStackVolumePlugin.deleteInstance(volumeOrder, this.openStackV3Token);

        // verify
        Mockito.verify(this.client).doDeleteRequest(Mockito.anyString(), Mockito.any(OpenStackV3User.class));
    }

    // test case: Deleting an instance without a project ID must raise FogbowException.
    @Test(expected = FogbowException.class)
    public void testRemoveInstanceWithoutProjectId() throws Exception {
        // set up
        this.openStackV3Token.setProjectId(null);

        VolumeOrder volumeOrder = new VolumeOrder();
        volumeOrder.setInstanceId(FAKE_INSTANCE_ID);

        // exercise
        this.openStackVolumePlugin.deleteInstance(volumeOrder, this.openStackV3Token);
    }

    @Test
    public void getInstanceState() {
        // TODO
    }
}
