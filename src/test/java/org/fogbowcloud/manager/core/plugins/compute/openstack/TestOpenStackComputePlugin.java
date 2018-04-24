package org.fogbowcloud.manager.core.plugins.compute.openstack;

import org.fogbowcloud.manager.core.models.Flavor;
import org.fogbowcloud.manager.core.models.exceptions.RequestException;
import org.fogbowcloud.manager.core.models.orders.ComputeOrder;
import org.fogbowcloud.manager.core.models.orders.UserData;
import org.fogbowcloud.manager.core.models.token.Token;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;
import java.util.UUID;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.mockito.Mockito.*;

public class TestOpenStackComputePlugin {

    protected static final String TENANT_ID = "tenantId";


    private OpenStackNovaV2ComputePlugin novaV2ComputeOpenStack;
    private Token localToken;
    private ComputeOrder computeOrder;

    @Before
    public void setUp() throws Exception {
        Properties properties = mock(Properties.class);
        novaV2ComputeOpenStack = spy(new OpenStackNovaV2ComputePlugin(properties));
        localToken = mock(Token.class);
        computeOrder = mock(ComputeOrder.class);
    }

    @Test
    public void testRequestInstance() throws IOException, RequestException {
        when(localToken.getAccessId()).thenReturn("fake-token");
        when(computeOrder.getLocalToken()).thenReturn(localToken);

        UserData userData = mock(UserData.class);
        when(userData.getContent()).thenReturn("fake-user-data");
        when(computeOrder.getUserData()).thenReturn(userData);
        String user = computeOrder.getUserData().getContent();

        Flavor flavor = mock(Flavor.class);
        when(flavor.getId()).thenReturn("fake-flavor-id");
        doReturn(flavor).when(novaV2ComputeOpenStack).getFlavor(computeOrder);
        String flavorId = novaV2ComputeOpenStack.getFlavor(computeOrder).getId();

        doReturn("fake-tenant-id").when(novaV2ComputeOpenStack).getTenantId(any(Token.class));
        String tenantId = novaV2ComputeOpenStack.getTenantId(any(Token.class));

        doReturn("fake-net-id").when(novaV2ComputeOpenStack).getNetworkId();
        String netId = novaV2ComputeOpenStack.getNetworkId();

        doReturn("fake-keyname").when(novaV2ComputeOpenStack).getKeyName(tenantId, localToken, "fake-public-key");
        String key = novaV2ComputeOpenStack.getKeyName(tenantId, localToken, "fake-public-key");

        doReturn("fake-endpoint").when(novaV2ComputeOpenStack).getComputeEndpoint(any(), anyString());
        String endpoint = novaV2ComputeOpenStack.getComputeEndpoint(any(), anyString());

        JSONObject json = novaV2ComputeOpenStack.generateJsonRequest("fake-image-id", flavorId, user, key, netId);

        doReturn("{\"server\": {\"id\": \"fake-id\"}}").when(novaV2ComputeOpenStack).
                doPostRequest(anyString(), any(Token.class), any(JSONObject.class));
        novaV2ComputeOpenStack.requestInstance(computeOrder, "fake-image-id");
    }

    @Test
    public void testDoPostRequest() throws RequestException {
        String responseStr = "fake-response";
        doReturn(responseStr).when(this.novaV2ComputeOpenStack).doPostRequest(Mockito.anyString(), Mockito.any(Token.class), Mockito.any(JSONObject.class));

        Assert.assertEquals(responseStr, this.novaV2ComputeOpenStack.doPostRequest("", localToken, new JSONObject()));
    }

    @Test
    public void testGenerateJsonRequest() {
        String imageRef = "imageRef";
        String flavorRef = "flavorRef";
        String userdata = "user-data";
        String keyName = "keyName";
        String networkId = "netId";

        JSONObject json = novaV2ComputeOpenStack.generateJsonRequest(imageRef, flavorRef, userdata, keyName, networkId);

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
        String imageRef = "imageRef";
        String flavorRef = "flavorRef";
        String userdata = null;
        String keyName = "keyName";
        String networkId = "netId";

        JSONObject json = novaV2ComputeOpenStack.generateJsonRequest(imageRef, flavorRef, userdata, keyName, networkId);

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
        String imageRef = "imageRef";
        String flavorRef = "flavorRef";
        String userdata = "user-data";
        String keyName = "keyName";
        String networkId = null;

        JSONObject json = novaV2ComputeOpenStack.generateJsonRequest(imageRef, flavorRef, userdata, keyName, networkId);

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
        String imageRef = "imageRef";
        String flavorRef = "flavorRef";
        String userdata = "user-data";
        String keyName = null;
        String networkId = "netId";

        JSONObject json = novaV2ComputeOpenStack.generateJsonRequest(imageRef, flavorRef, userdata, keyName, networkId);

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
