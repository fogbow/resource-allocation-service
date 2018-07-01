package org.fogbowcloud.manager.core.plugins.cloud.openstack;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyObject;
import static org.mockito.Matchers.anyString;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;
import org.fogbowcloud.manager.core.HomeDir;
import org.fogbowcloud.manager.core.PropertiesHolder;
import org.fogbowcloud.manager.core.exceptions.FogbowManagerException;
import org.fogbowcloud.manager.core.plugins.cloud.util.CloudInitUserDataBuilder;
import org.fogbowcloud.manager.core.plugins.cloud.util.LaunchCommandGenerator;
import org.fogbowcloud.manager.core.plugins.cloud.models.Flavor;
import org.fogbowcloud.manager.core.models.orders.ComputeOrder;
import org.fogbowcloud.manager.core.models.orders.UserData;
import org.fogbowcloud.manager.core.models.instances.ComputeInstance;
import org.fogbowcloud.manager.core.models.instances.InstanceState;
import org.fogbowcloud.manager.core.models.tokens.Token;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.junit.Before;
import org.junit.Test;

public class OpenStackComputePluginTest {

    protected static final String FAKE_USER_DATA_FILE = "fake-extra-user-data";
    protected static final String FAKE_FLAVOR_ID = "fake-flavor-id";
    protected static final String FAKE_NET_ID = "fake-net-id";
    protected static final String FAKE_KEYNAME = "fake-keyname";
    protected static final String FAKE_ENDPOINT = "fake-endpoint";
    protected static final String FAKE_IMAGE_ID = "fake-image-id";
    protected static final String FAKE_INSTANCE_ID = "fake-instance-id";
    protected static final String FAKE_POST_RETURN = "{\"server\": {\"id\": \"fake-instance-id\"}}";
    protected static final String FAKE_GET_RETURN_ACTIVE_INSTANCE =
            "{\"server\": {\"id\": \"fake-instance-id\", "
                    + "\"status\": \"active\", \"name\": \"host\", \"flavor\": {\"vcpus\": 1, \"ram\": 3}}}";
    protected static final String FAKE_GET_RETURN_FAILED_INSTANCE =
            "{\"server\": {\"id\": \"fake-instance-id\", "
                    + "\"status\": \"error\", \"name\": \"host\", \"flavor\": {\"vcpus\": 1, \"ram\": 3}}}";
    protected static final String FAKE_GET_RETURN_INACTIVE_INSTANCE =
            "{\"server\": {\"id\": \"fake-instance-id\", "
                    + "\"status\": \"build\", \"name\": \"host\", \"flavor\": {\"vcpus\": 1, \"ram\": 3}}}";
    protected static final String INVALID_FAKE_POST_RETURN = "invalid";
    private static final String COMPUTE_NOVAV2_URL_KEY = "compute_novav2_url";
    private static final String COMPUTE_NOVAV2_NETWORK_KEY = "compute_novav2_network_id";

    private OpenStackNovaV2ComputePlugin novaV2ComputeOpenStack;
    private Token localToken;
    private ComputeOrder computeOrder;
    private LaunchCommandGenerator launchCommandGenerator;

    @Before
    public void setUp() throws Exception {
        HomeDir.getInstance().setPath("src/test/resources/private");
        PropertiesHolder propertiesHolder = PropertiesHolder.getInstance();
        Properties properties = propertiesHolder.getProperties();
        properties.put(COMPUTE_NOVAV2_URL_KEY, FAKE_ENDPOINT);
        properties.put(COMPUTE_NOVAV2_NETWORK_KEY, FAKE_NET_ID);

        this.localToken = mock(Token.class);

        this.computeOrder =
                new ComputeOrder(
                        null,
                        null,
                        null,
                        1,
                        2000,
                        20,
                        null,
                        new UserData(
                                FAKE_USER_DATA_FILE, 
                                CloudInitUserDataBuilder.FileType.SHELL_SCRIPT),
                        null,
                        null);

        this.launchCommandGenerator = mock(LaunchCommandGenerator.class);

        this.novaV2ComputeOpenStack =
                spy(new OpenStackNovaV2ComputePlugin(properties, this.launchCommandGenerator));
    }

    @Test
    public void testRequestInstance() throws IOException, FogbowManagerException {
        Flavor flavor = mock(Flavor.class);
        doReturn(flavor)
                .when(this.novaV2ComputeOpenStack)
                .findSmallestFlavor(any(ComputeOrder.class), eq(this.localToken));

        String fakeCommand = "fake-command";
        doReturn(fakeCommand)
                .when(this.launchCommandGenerator)
                .createLaunchCommand(any(ComputeOrder.class));

        doReturn(FAKE_ENDPOINT)
                .when(this.novaV2ComputeOpenStack)
                .getComputeEndpoint(anyString(), anyString());

        List<String> fakeNetIds = new ArrayList<String>();
        fakeNetIds.add(FAKE_NET_ID);
        
        JSONObject json =
                this.novaV2ComputeOpenStack.generateJsonRequest(
                        FAKE_IMAGE_ID,
                        FAKE_FLAVOR_ID,
                        FAKE_USER_DATA_FILE,
                        FAKE_KEYNAME,
                        fakeNetIds);
        doReturn(json)
                .when(this.novaV2ComputeOpenStack)
                .generateJsonRequest(
                        anyString(), anyString(), anyString(), anyString(), anyObject());

        doReturn(FAKE_POST_RETURN)
                .when(this.novaV2ComputeOpenStack)
                .doPostRequest(eq(FAKE_ENDPOINT), eq(localToken), eq(json));

        String instanceId = novaV2ComputeOpenStack.requestInstance(computeOrder, localToken);

        assertEquals(instanceId, FAKE_INSTANCE_ID);
    }

    @Test(expected = FogbowManagerException.class)
    public void testRequestInstanceIrregularSyntax() throws IOException, FogbowManagerException {
        Flavor flavor = mock(Flavor.class);
        doReturn(flavor)
                .when(novaV2ComputeOpenStack)
                .findSmallestFlavor(any(ComputeOrder.class), eq(localToken));

        String fakeCommand = "fake-command";
        doReturn(fakeCommand)
                .when(launchCommandGenerator)
                .createLaunchCommand(any(ComputeOrder.class));

        doReturn(INVALID_FAKE_POST_RETURN)
                .when(novaV2ComputeOpenStack)
                .doPostRequest(anyString(), any(Token.class), any(JSONObject.class));

        novaV2ComputeOpenStack.requestInstance(computeOrder, localToken);
    }

    @Test
    public void testGenerateJsonRequest() {
        List<String> netIds = new ArrayList<String>();
        netIds.add("netId");
        
        JSONObject json =
                novaV2ComputeOpenStack.generateJsonRequest(
                        "imageRef", "flavorRef", "user-data", "keyName", netIds);

        assertNotNull(json);
        JSONObject server = json.getJSONObject("server");
        assertNotNull(server);

        String name = server.getString("name");
        assertNotNull(name);

        String image = server.getString("imageRef");
        assertEquals("imageRef", image);

        String key = server.getString("key_name");
        assertEquals("keyName", key);

        String flavor = server.getString("flavorRef");
        assertEquals("flavorRef", flavor);

        String user = server.getString("user_data");
        assertEquals("user-data", user);

        JSONArray net = server.getJSONArray("networks");
        assertNotNull(net);
        assertEquals(1, net.length());
        assertEquals("netId", net.getJSONObject(0).getString("uuid"));
    }

    @Test(expected = JSONException.class)
    public void testGenerateJsonRequestWithoutUserData() {
        List<String> netIds = new ArrayList<String>();
        netIds.add("netId");
        
        JSONObject json =
                novaV2ComputeOpenStack.generateJsonRequest(
                        "imageRef", "flavorRef", null, "keyName", netIds);

        assertNotNull(json);
        JSONObject server = json.getJSONObject("server");
        assertNotNull(server);

        String name = server.getString("name");
        assertNotNull(name);

        String image = server.getString("imageRef");
        assertEquals("imageRef", image);

        String key = server.getString("key_name");
        assertEquals("keyName", key);

        String flavor = server.getString("flavorRef");
        assertEquals("flavorRef", flavor);

        JSONArray net = server.getJSONArray("networks");
        assertNotNull(net);
        assertEquals(1, net.length());
        assertEquals("netId", net.getJSONObject(0).getString("uuid"));

        server.get("user_data");
    }

    @Test(expected = JSONException.class)
    public void testGenerateJsonRequestWithoutNetwork() {
        JSONObject json =
                novaV2ComputeOpenStack.generateJsonRequest(
                        "imageRef", "flavorRef", "user-data", "keyName", null);

        assertNotNull(json);
        JSONObject server = json.getJSONObject("server");
        assertNotNull(server);

        String name = server.getString("name");
        assertNotNull(name);

        String image = server.getString("imageRef");
        assertEquals("imageRef", image);

        String key = server.getString("key_name");
        assertEquals("keyName", key);

        String flavor = server.getString("flavorRef");
        assertEquals("flavorRef", flavor);

        server.getJSONArray("networks");
    }

    @Test(expected = JSONException.class)
    public void testGenerateJsonRequestWithoutKeyName() {
        List<String> netIds = new ArrayList<String>();
        netIds.add("netId");
        
        JSONObject json =
                novaV2ComputeOpenStack.generateJsonRequest(
                        "imageRef", "flavorRef", "user-data", null, netIds);

        assertNotNull(json);
        JSONObject server = json.getJSONObject("server");
        assertNotNull(server);

        String name = server.getString("name");
        assertNotNull(name);

        String image = server.getString("imageRef");
        assertEquals("imageRef", image);

        String flavor = server.getString("flavorRef");
        assertEquals("flavorRef", flavor);

        JSONArray net = server.getJSONArray("networks");
        assertNotNull(net);
        assertEquals(1, net.length());
        assertEquals("netId", net.getJSONObject(0).getString("uuid"));

        server.getString("key_name");
    }

    @Test
    public void testGetActivateInstance() throws FogbowManagerException {
        doReturn(FAKE_ENDPOINT)
                .when(novaV2ComputeOpenStack)
                .getComputeEndpoint(anyString(), anyString());

        doReturn(FAKE_GET_RETURN_ACTIVE_INSTANCE)
                .when(novaV2ComputeOpenStack)
                .doGetRequest(eq(FAKE_ENDPOINT), eq(localToken));

        ComputeInstance computeInstance =
                this.novaV2ComputeOpenStack.getInstance(FAKE_INSTANCE_ID, this.localToken);

        assertEquals(computeInstance.getId(), FAKE_INSTANCE_ID);
        assertEquals(computeInstance.getState(), InstanceState.READY);
    }

    @Test
    public void testGetFailedInstance() throws FogbowManagerException {
        doReturn(FAKE_ENDPOINT)
                .when(novaV2ComputeOpenStack)
                .getComputeEndpoint(anyString(), anyString());

        doReturn(FAKE_GET_RETURN_FAILED_INSTANCE)
                .when(novaV2ComputeOpenStack)
                .doGetRequest(eq(FAKE_ENDPOINT), eq(localToken));

        ComputeInstance computeInstance =
                this.novaV2ComputeOpenStack.getInstance(FAKE_INSTANCE_ID, this.localToken);

        assertEquals(computeInstance.getId(), FAKE_INSTANCE_ID);
        assertEquals(computeInstance.getState(), InstanceState.FAILED);
    }

    @Test
    public void testGetInactiveInstance() throws FogbowManagerException {
        doReturn(FAKE_ENDPOINT)
                .when(this.novaV2ComputeOpenStack)
                .getComputeEndpoint(anyString(), anyString());

        doReturn(FAKE_GET_RETURN_INACTIVE_INSTANCE)
                .when(this.novaV2ComputeOpenStack)
                .doGetRequest(eq(FAKE_ENDPOINT), eq(this.localToken));

        ComputeInstance computeInstance =
                this.novaV2ComputeOpenStack.getInstance(FAKE_INSTANCE_ID, this.localToken);

        assertEquals(computeInstance.getId(), FAKE_INSTANCE_ID);
        assertEquals(computeInstance.getState(), InstanceState.SPAWNING);
    }

    @Test(expected = Exception.class)
    public void testGetInstanceWithJSONException() throws FogbowManagerException {
        doReturn(INVALID_FAKE_POST_RETURN)
                .when(this.novaV2ComputeOpenStack)
                .doGetRequest(anyString(), any(Token.class));

        this.novaV2ComputeOpenStack.getInstance(FAKE_INSTANCE_ID, this.localToken);
    }
}
