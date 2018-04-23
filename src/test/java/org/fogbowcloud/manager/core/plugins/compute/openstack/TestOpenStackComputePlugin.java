package org.fogbowcloud.manager.core.plugins.compute.openstack;

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
import java.util.Date;
import java.util.HashMap;
import java.util.Properties;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

public class TestOpenStackComputePlugin {

    public static final long LONG_TIME = 24 * 60 * 60 * 1000; //one day
    private static final String GLACE_V2 = "glaceV2";
    private static final String FIRST_INSTANCE_ID = "0";
    private static final String SECOND_INSTANCE_ID = "1";
    private static final String UTF_8 = "UTF-8";

    private OpenStackNovaV2ComputePlugin novaV2ComputeOpenStack;

    private Token defaultToken;

    private ComputeOrder computeOrder;

    @Before
    public void setUp() throws Exception {
        Properties properties = Mockito.mock(Properties.class);
//        properties.put("compute_novav2_url", "oi");
//        properties.put("compute_novav2_network_id", "64ee4355-4d7f-4170-80b4-5e8348af6a61");

        novaV2ComputeOpenStack = Mockito.spy(new OpenStackNovaV2ComputePlugin(properties));

        HashMap<String, String> tokenAtt = new HashMap<>();
        tokenAtt.put("tenantId", "tenantId");
        defaultToken = new Token("accessId", new Token.User("id", "name"), new Date(), tokenAtt);

        computeOrder = new ComputeOrder("id", null, defaultToken, null,
                "requestingMember", "providingMember", null,
                6000, 1, 2000, 20, null, null,
                null, "publicKey");
    }

    @Test
    public void testRequestInstance() throws IOException, RequestException {
        //1. create mock for computeOrder
        //2. mock calls from 73:78
        String instanceId = "fake-instance-id";
        Mockito.doReturn(instanceId).when(this.novaV2ComputeOpenStack).requestInstance(Mockito.any(ComputeOrder.class), Mockito.anyString());

        Assert.assertEquals(instanceId, novaV2ComputeOpenStack.requestInstance(computeOrder, "....."));
    }

    @Test
    public void testDoPostRequest() throws RequestException {
        String responseStr = "fake-response";
        Mockito.doReturn(responseStr).when(this.novaV2ComputeOpenStack).doPostRequest(Mockito.anyString(), Mockito.any(Token.class), Mockito.any(JSONObject.class));

        Assert.assertEquals(responseStr, this.novaV2ComputeOpenStack.doPostRequest("", defaultToken, new JSONObject()));
    }

    @Test
    public void testGenerateJsonRequest() {
        String imageRef = "imageRef";
        String flavorRef = "flavorRef";
        UserData userdata = new UserData();
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

        UserData user = (UserData) server.get("user_data");
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
        UserData userdata = null;
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
        UserData userdata = new UserData();
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

        UserData user = (UserData) server.get("user_data");
        assertNotNull(user);
    }

    @Test
    public void testGenerateJsonRequestWithoutKeyName() {
        String imageRef = "imageRef";
        String flavorRef = "flavorRef";
        UserData userdata = new UserData();
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

        UserData user = (UserData) server.get("user_data");
        assertNotNull(user);

        JSONArray net = server.getJSONArray("networks");
        assertNotNull(net);
        assertEquals(1, net.length());
        assertEquals("netId", net.getJSONObject(0).getString("uuid"));
    }
}
