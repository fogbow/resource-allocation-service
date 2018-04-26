package org.fogbowcloud.manager.core.plugins.compute.openstack;

import org.fogbowcloud.manager.core.models.Flavor;
import org.fogbowcloud.manager.core.models.exceptions.RequestException;
import org.fogbowcloud.manager.core.models.orders.ComputeOrder;
import org.fogbowcloud.manager.core.models.orders.UserData;
import org.fogbowcloud.manager.core.models.token.Token;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.junit.Before;
import org.junit.Test;

import java.io.IOException;
import java.util.Properties;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.mockito.Mockito.*;

public class TestOpenStackComputePlugin {

    protected static final String FAKE_TENANT_ID = "fake-tenant-id";
    protected static final String FAKE_TOKEN = "fake-token";
    protected static final String FAKE_USER_DATA = "fake-user-data";
    protected static final String FAKE_FLAVOR_ID = "fake-flavor-id";
    protected static final String FAKE_NET_ID = "fake-net-id";
    protected static final String FAKE_KEYNAME = "fake-keyname";
    protected static final String FAKE_ENDPOINT = "fake-endpoint";
    protected static final String FAKE_IMAGE_ID = "fake-image-id";
    protected static final String FAKE_INSTANCE_ID = "fake-instance-id";
    protected static final String FAKE_POST_RETURN = "{\"server\": {\"id\": \"fake-instance-id\"}}";
    protected static final String INVALID_FAKE_POST_RETURN = "invalid";

    private OpenStackNovaV2ComputePlugin novaV2ComputeOpenStack;
    private Token localToken;
    private ComputeOrder computeOrder;

    @Before
    public void setUp() throws Exception {
        // Do mock of computeOrder, localToken and spy of novaV2ComputeOpenStack.
        Properties properties = mock(Properties.class);
        novaV2ComputeOpenStack = spy(new OpenStackNovaV2ComputePlugin(properties));
        localToken = mock(Token.class);
        computeOrder = mock(ComputeOrder.class);
    }

    @Test
    public void testRequestInstance() throws IOException, RequestException {
        // 1. Mocking following methods:
        //  1.1 localToken
        //  1.2 flavorId
        //  1.3 tenantId
        //  1.4 networkId
        //  1.5 userData
        //  1.6 keyName
        //  1.7 endpoint
        // 2. Call requestInstance(args) to check if it runs successfully.
        when(localToken.getAccessId()).thenReturn(FAKE_TOKEN);
        when(computeOrder.getLocalToken()).thenReturn(localToken);

        UserData userData = mock(UserData.class);
        when(userData.getContent()).thenReturn(FAKE_USER_DATA);
        when(computeOrder.getUserData()).thenReturn(userData);
        String user = computeOrder.getUserData().getContent();

        Flavor flavor = mock(Flavor.class);
        when(flavor.getId()).thenReturn(FAKE_FLAVOR_ID);
        doReturn(flavor).when(novaV2ComputeOpenStack).updateAndFindSmallestFlavor(any(ComputeOrder.class));
        String flavorId = novaV2ComputeOpenStack.updateAndFindSmallestFlavor(any(ComputeOrder.class)).getId();

        doReturn(FAKE_TENANT_ID).when(novaV2ComputeOpenStack).getTenantId(any(Token.class));

        doReturn(FAKE_NET_ID).when(novaV2ComputeOpenStack).getNetworkId();
        String netId = novaV2ComputeOpenStack.getNetworkId();

        doReturn(FAKE_KEYNAME).when(novaV2ComputeOpenStack).getKeyName(anyString(), any(Token.class), anyString());
        String key = novaV2ComputeOpenStack.getKeyName(anyString(), any(Token.class), anyString());

        doReturn(FAKE_ENDPOINT).when(novaV2ComputeOpenStack).getComputeEndpoint(anyString(), anyString());

        JSONObject json = novaV2ComputeOpenStack.generateJsonRequest(FAKE_IMAGE_ID, flavorId, user, key, netId);
        doReturn(json).when(novaV2ComputeOpenStack)
                .generateJsonRequest(anyString(), anyString(), anyString(), anyString(), anyString());

        doReturn(FAKE_POST_RETURN).when(novaV2ComputeOpenStack).doPostRequest(eq(FAKE_ENDPOINT), eq(localToken), eq(json));

        doNothing().when(novaV2ComputeOpenStack).deleteKeyName(anyString(), any(Token.class), anyString());

        String instanceId = novaV2ComputeOpenStack.requestInstance(computeOrder, FAKE_IMAGE_ID);
        assertEquals(instanceId, FAKE_INSTANCE_ID);
    }

    @Test(expected = RequestException.class)
    public void testRequestInstanceIrregularSyntax() throws IOException, RequestException {
        // 1. Mocking following methods:
        //  1.1 localToken
        //  1.2 flavorId
        //  1.3 tenantId
        //  1.4 networkId
        //  1.5 userData
        //  1.6 keyName
        //  1.7 endpoint
        // 2. With an invalid JSON response a RequestException will be raised.
        when(localToken.getAccessId()).thenReturn(FAKE_TOKEN);
        when(computeOrder.getLocalToken()).thenReturn(localToken);

        UserData userData = mock(UserData.class);
        when(userData.getContent()).thenReturn(FAKE_USER_DATA);
        when(computeOrder.getUserData()).thenReturn(userData);
        String user = computeOrder.getUserData().getContent();

        Flavor flavor = mock(Flavor.class);
        when(flavor.getId()).thenReturn(FAKE_FLAVOR_ID);
        doReturn(flavor).when(novaV2ComputeOpenStack).updateAndFindSmallestFlavor(any(ComputeOrder.class));
        String flavorId = novaV2ComputeOpenStack.updateAndFindSmallestFlavor(any(ComputeOrder.class)).getId();

        doReturn(FAKE_TENANT_ID).when(novaV2ComputeOpenStack).getTenantId(any(Token.class));

        doReturn(FAKE_NET_ID).when(novaV2ComputeOpenStack).getNetworkId();
        String netId = novaV2ComputeOpenStack.getNetworkId();

        doReturn(FAKE_KEYNAME).when(novaV2ComputeOpenStack).getKeyName(anyString(), any(Token.class), anyString());
        String key = novaV2ComputeOpenStack.getKeyName(anyString(), any(Token.class), anyString());

        doReturn(FAKE_ENDPOINT).when(novaV2ComputeOpenStack).getComputeEndpoint(anyString(), anyString());

        JSONObject json = novaV2ComputeOpenStack.generateJsonRequest(FAKE_IMAGE_ID, flavorId, user, key, netId);
        doReturn(json).when(novaV2ComputeOpenStack)
                .generateJsonRequest(anyString(), anyString(), anyString(), anyString(), anyString());

        doReturn(INVALID_FAKE_POST_RETURN).when(novaV2ComputeOpenStack).
                doPostRequest(eq(FAKE_ENDPOINT), eq(localToken), eq(json));

        doNothing().when(novaV2ComputeOpenStack).deleteKeyName(anyString(), any(Token.class), anyString());

        novaV2ComputeOpenStack.requestInstance(computeOrder, FAKE_IMAGE_ID);
    }

    @Test
    public void testGenerateJsonRequest() {
        JSONObject json = novaV2ComputeOpenStack.generateJsonRequest("imageRef",
                "flavorRef", "user-data", "keyName", "netId");

        assertNotNull(json);
        JSONObject server = json.getJSONObject("server");
        assertNotNull(server);

        String name = server.getString("name");
        assertNotNull(name);

        String image = server.getString("imageRef");
        assertNotNull(image);

        String key = server.getString("key_name");
        assertNotNull(key);

        String flavor = server.getString("flavorRef");
        assertNotNull(flavor);

        String user = server.getString("user_data");
        assertNotNull(user);

        JSONArray net = server.getJSONArray("networks");
        assertNotNull(net);
        assertEquals(1, net.length());
        assertEquals("netId", net.getJSONObject(0).getString("uuid"));
    }

    @Test
    public void testGenerateJsonRequestWithoutUserData() {
        JSONObject json = novaV2ComputeOpenStack.generateJsonRequest("imageRef",
                "flavorRef", null, "keyName", "netId");

        assertNotNull(json);
        JSONObject server = json.getJSONObject("server");
        assertNotNull(server);

        String name = server.getString("name");
        assertNotNull(name);

        String image = server.getString("imageRef");
        assertNotNull(image);

        String key = server.getString("key_name");
        assertNotNull(key);

        String flavor = server.getString("flavorRef");
        assertNotNull(flavor);

        JSONArray net = server.getJSONArray("networks");
        assertNotNull(net);
        assertEquals(1, net.length());
        assertEquals("netId", net.getJSONObject(0).getString("uuid"));

        try {
            server.get("user_data");
        } catch (JSONException exception) {
            // do nothing
        }
    }

    @Test
    public void testGenerateJsonRequestWithoutNetwork() {
        JSONObject json = novaV2ComputeOpenStack.generateJsonRequest("imageRef",
                "flavorRef", "user-data", "keyName", null);

        assertNotNull(json);
        JSONObject server = json.getJSONObject("server");
        assertNotNull(server);

        String name = server.getString("name");
        assertNotNull(name);

        String key = server.getString("key_name");
        assertNotNull(key);

        String image = server.getString("imageRef");
        assertNotNull(image);

        String flavor = server.getString("flavorRef");
        assertNotNull(flavor);

        try {
            server.getJSONArray("networks");
        } catch (JSONException e) {
            // do nothing
        }

        String user = server.getString("user_data");
        assertNotNull(user);
    }

    @Test
    public void testGenerateJsonRequestWithoutKeyName() {
        JSONObject json = novaV2ComputeOpenStack.generateJsonRequest("imageRef",
                "flavorRef", "user-data", null, "netId");

        assertNotNull(json);
        JSONObject server = json.getJSONObject("server");
        assertNotNull(server);

        String name = server.getString("name");
        assertNotNull(name);

        String image = server.getString("imageRef");
        assertNotNull(image);

        try {
            server.getString("key_name");
        } catch (JSONException e) {
            // do nothing
        }

        String flavor = server.getString("flavorRef");
        assertNotNull(flavor);

        String user = server.getString("user_data");
        assertNotNull(user);

        JSONArray net = server.getJSONArray("networks");
        assertNotNull(net);
        assertEquals(1, net.length());
        assertEquals("netId", net.getJSONObject(0).getString("uuid"));
    }
}
