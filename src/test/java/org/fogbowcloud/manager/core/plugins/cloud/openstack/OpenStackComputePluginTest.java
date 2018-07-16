package org.fogbowcloud.manager.core.plugins.cloud.openstack;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyString;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.UUID;

import org.apache.http.client.HttpResponseException;
import org.fogbowcloud.manager.core.HomeDir;
import org.fogbowcloud.manager.core.PropertiesHolder;
import org.fogbowcloud.manager.core.exceptions.FogbowManagerException;
import org.fogbowcloud.manager.core.exceptions.UnexpectedException;
import org.fogbowcloud.manager.core.models.HardwareRequirements;
import org.fogbowcloud.manager.core.models.instances.ComputeInstance;
import org.fogbowcloud.manager.core.models.instances.InstanceState;
import org.fogbowcloud.manager.core.models.orders.ComputeOrder;
import org.fogbowcloud.manager.core.models.orders.UserData;
import org.fogbowcloud.manager.core.models.tokens.Token;
import org.fogbowcloud.manager.core.models.tokens.Token.User;
import org.fogbowcloud.manager.core.plugins.cloud.util.CloudInitUserDataBuilder;
import org.fogbowcloud.manager.core.plugins.cloud.util.LaunchCommandGenerator;
import org.fogbowcloud.manager.util.connectivity.HttpRequestClientUtil;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;

import com.google.gson.Gson;

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

    private OpenStackNovaV2ComputePlugin novaV2ComputeOpenStack;
    private Token localToken;
    private ComputeOrder computeOrder;
    private LaunchCommandGenerator launchCommandGeneratorMock;
    private HttpRequestClientUtil httpRequestClientUtilMock;
    private Properties propertiesMock;
    private PropertiesHolder propertiesHolderMock;
    
    @Before
    public void setUp() throws Exception {
  
        this.propertiesHolderMock = Mockito.mock(PropertiesHolder.class);
        this.propertiesMock = Mockito.mock(Properties.class);
        
        Mockito.when(propertiesHolderMock.getProperties()).thenReturn(propertiesMock);
        Mockito.when(propertiesMock.getProperty(OpenStackNovaV2ComputePlugin.COMPUTE_NOVAV2_URL_KEY)).thenReturn(FAKE_ENDPOINT);
        
        this.httpRequestClientUtilMock = Mockito.mock(HttpRequestClientUtil.class);
        
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

        this.launchCommandGeneratorMock = mock(LaunchCommandGenerator.class);
        
        String accessId = "accessID";
    	String tenantId = "tenant-id";
    	Map <String, String> attributes = new HashMap<String, String>();
    	attributes.put(OpenStackNovaV2ComputePlugin.TENANT_ID, tenantId);
    	User user = new User("iduser", "nameuser");
    	Date expirationTime = new Date();
        this.localToken = new Token(accessId, user, expirationTime, attributes);
        
        HomeDir.getInstance().setPath("src/test/resources/private");
        
        this.novaV2ComputeOpenStack =
              Mockito.spy(new OpenStackNovaV2ComputePlugin(this.propertiesMock, this.launchCommandGeneratorMock));
        
        this.novaV2ComputeOpenStack.setClient(this.httpRequestClientUtilMock);
    }

    @Test
    public void testRequestInstance() throws IOException, FogbowManagerException, UnexpectedException {
    	//this.novaV2ComputeOpenStack.requestInstance(null, null);
    	
    	String computeNovaV2UrlKey = "compute-nova-v2-url-key";
    	Mockito.when(
    			this.propertiesMock.getProperty(OpenStackNovaV2ComputePlugin.COMPUTE_NOVAV2_URL_KEY)).
    			thenReturn(computeNovaV2UrlKey);
    
    	String flavorEndpoint = computeNovaV2UrlKey
                + OpenStackNovaV2ComputePlugin.COMPUTE_V2_API_ENDPOINT
                + this.localToken.getAttributes().get(OpenStackNovaV2ComputePlugin.TENANT_ID)
                + OpenStackNovaV2ComputePlugin.SUFFIX_ENDPOINT_FLAVORS;
    	
    	Mockito.when(this.httpRequestClientUtilMock.doGetRequest(flavorEndpoint, this.localToken))
    			.thenReturn(generateJsonFlavors(10));
    	
    	for (int i = 0; i < 10; i++) {
    		String flavorId = "flavor" + Integer.toString(i);
    		String newEndpoint = flavorEndpoint + "/" + flavorId;
    		String flavorJson = generateJsonFlavor(
    				flavorId, 
    				"nameflavor" + Integer.toString(i), 
    				Integer.toString(i), 
    				Integer.toString(i), 
    				Integer.toString(i));
    		Mockito.when(this.httpRequestClientUtilMock.doGetRequest(newEndpoint, this.localToken))
    				.thenReturn(flavorJson);
    		
    	}
    	
    	int flavorSpecifications = 5;
    	String flavorId = "flavor" + Integer.toString(flavorSpecifications);
    	
    	String defaultNetworkIp = "192.168.0.2";
    	
    	Mockito.when(this.propertiesMock.getProperty(OpenStackNovaV2ComputePlugin.DEFAULT_NETWORK_ID_KEY))
    			.thenReturn(defaultNetworkIp);
    	
    	String imageId = "image-id";
    	
    	String publicKey = "public-key";
    	String osKeyPairEndpoint = 
    					computeNovaV2UrlKey
    	                + OpenStackNovaV2ComputePlugin.COMPUTE_V2_API_ENDPOINT
    	                + this.localToken.getAttributes().get(OpenStackNovaV2ComputePlugin.TENANT_ID)
    	                + OpenStackNovaV2ComputePlugin.SUFFIX_ENDPOINT_KEYPAIRS;

    	String keyName = "keyname";
    	
    	JSONObject rootKeypairJson = generateRootKeyPairJson(keyName, publicKey);
    	
    	Mockito.when(this.httpRequestClientUtilMock.doPostRequest(osKeyPairEndpoint, 
    				this.localToken, 
    				rootKeypairJson)).thenReturn("");
    	
    	        
    	ComputeOrder computeOrder =
                new ComputeOrder(
                        null,
                        null,
                        null,
                        flavorSpecifications,
                        flavorSpecifications,
                        flavorSpecifications,
                        imageId,
                        new UserData(
                                FAKE_USER_DATA_FILE,
                                CloudInitUserDataBuilder.FileType.SHELL_SCRIPT),
                        publicKey,
                        null);
    	
    	
    	String userData = "userDataFromLauchCommand";
    	Mockito.when(this.launchCommandGeneratorMock.createLaunchCommand(computeOrder))
    			.thenReturn(userData);
    	
    	String computeEndpoint = computeNovaV2UrlKey
                + OpenStackNovaV2ComputePlugin.COMPUTE_V2_API_ENDPOINT
                + this.localToken.getAttributes().get(OpenStackNovaV2ComputePlugin.TENANT_ID)
                + OpenStackNovaV2ComputePlugin.SERVERS;
    	
    	List <String> networksId =  new ArrayList<String>();
    	networksId.add(defaultNetworkIp);
    	
    	String idKeyName = "493315b3-dd01-4b38-974f-289570f8e7ee";
    	
    	String idInstanceName = "12345678-dd01-4b38-974f-289570f8e7ee";
    	
    	doReturn(idKeyName).doReturn(idInstanceName).when(this.novaV2ComputeOpenStack).getRandomUUID();
    	
    	JSONObject computeJson =
    			generateJsonRequest(imageId, flavorId, userData, idKeyName, networksId, idInstanceName);
    	
    	String expectedInstanceId = "instance-id-00";
    	String expectedInstanceIdJson = generateInstaceId("instance-id-00");

    	ArgumentCaptor<String> argComputeEndpoint  = ArgumentCaptor.forClass(String.class);
    	ArgumentCaptor<Token> argLocalToken = ArgumentCaptor.forClass(Token.class);
    	ArgumentCaptor<JSONObject> argComputeJson = ArgumentCaptor.forClass(JSONObject.class);
    	
    	Mockito.when(this.httpRequestClientUtilMock.doPostRequest(
    			argComputeEndpoint.capture(), 
    			argLocalToken.capture(), 
    			argComputeJson.capture()))
    			.thenReturn(expectedInstanceIdJson);
    	
    	String instanceId = this.novaV2ComputeOpenStack.requestInstance(computeOrder, this.localToken);
    	
    	Assert.assertEquals(argComputeEndpoint.getValue(), computeEndpoint);
    	Assert.assertEquals(argLocalToken.getValue(), this.localToken);
    	Assert.assertEquals(argComputeJson.getValue().toString(), computeJson.toString());
    	
    	Assert.assertEquals(expectedInstanceId, instanceId);
    }
    
    
    private String generateJsonFlavors(int qtd) {
    	Map <String, Object> jsonFlavorsMap = new HashMap<String, Object>();
    	List<Map <String, String>> jsonArrayFlavors = new ArrayList<Map<String, String>>();
    	for (int i = 0; i < qtd; i++) {
    		Map <String, String> flavor = new HashMap<String, String>();
    		flavor.put(OpenStackNovaV2ComputePlugin.ID_JSON_FIELD, "flavor" + Integer.toString(i));
    		jsonArrayFlavors.add(flavor);
    	}
    	jsonFlavorsMap.put(OpenStackNovaV2ComputePlugin.FLAVOR_JSON_KEY, jsonArrayFlavors);
    	Gson gson = new Gson();
    	return gson.toJson(jsonFlavorsMap);
    }
    
    private String generateJsonFlavor(String id, String name, String disk, String memory, String vcpu) {
    	Map<String, Object> flavorMap = new HashMap<String, Object>();
    	Map<String, String> flavorAttributes = new HashMap<String, String>();
    	flavorAttributes.put(OpenStackNovaV2ComputePlugin.ID_JSON_FIELD, id);
    	flavorAttributes.put(OpenStackNovaV2ComputePlugin.NAME_JSON_FIELD, name);
    	flavorAttributes.put(OpenStackNovaV2ComputePlugin.DISK_JSON_FIELD, disk);
    	flavorAttributes.put(OpenStackNovaV2ComputePlugin.MEMORY_JSON_FIELD, memory);
    	flavorAttributes.put(OpenStackNovaV2ComputePlugin.VCPU_JSON_FIELD, vcpu);
    	flavorMap.put(OpenStackNovaV2ComputePlugin.FLAVOR_JSON_OBJECT, flavorAttributes);
    	return new Gson().toJson(flavorMap);
    }
    
    private String generateInstaceId(String id) {
    	Map <String, String> instanceMap = new HashMap<String, String>();
    	instanceMap.put(OpenStackNovaV2ComputePlugin.ID_JSON_FIELD, id);
    	Map<String, Object> root = new HashMap<String, Object>();
    	root.put(OpenStackNovaV2ComputePlugin.SERVER_JSON_FIELD, instanceMap);
    	return new Gson().toJson(root);
    }
    
    private JSONObject generateRootKeyPairJson(String keyName, String publicKey) {
    	JSONObject keypair = new JSONObject();
    	keypair.put(OpenStackNovaV2ComputePlugin.NAME_JSON_FIELD, keyName);
    	keypair.put(OpenStackNovaV2ComputePlugin.PUBLIC_KEY_JSON_FIELD, publicKey);
    	JSONObject root = new JSONObject();
    	root.put(OpenStackNovaV2ComputePlugin.KEYPAIR_JSON_FIELD, keypair);
    	return root;
    }
    
    private JSONObject generateJsonRequest(String imageId, String flavorId, String userData, String keyName, List<String> networksId, String randomUUID) {
    	JSONObject server = new JSONObject();
    	server.put(OpenStackNovaV2ComputePlugin.NAME_JSON_FIELD, OpenStackNovaV2ComputePlugin.FOGBOW_INSTANCE_NAME + randomUUID);
    	server.put(OpenStackNovaV2ComputePlugin.IMAGE_JSON_FIELD, imageId);
    	server.put(OpenStackNovaV2ComputePlugin.FLAVOR_REF_JSON_FIELD, flavorId);
    	server.put(OpenStackNovaV2ComputePlugin.USER_DATA_JSON_FIELD, userData);
    	
    	JSONArray networks = new JSONArray();
    	
        for (String id : networksId) {
            JSONObject netId = new JSONObject();
            netId.put(OpenStackNovaV2ComputePlugin.UUID_JSON_FIELD, id);
            networks.put(netId);
        }
        
        server.put(OpenStackNovaV2ComputePlugin.NETWORK_JSON_FIELD, networks);
    	
    	server.put(OpenStackNovaV2ComputePlugin.KEY_JSON_FIELD, keyName);
    	JSONObject root = new JSONObject();
    	root.put(OpenStackNovaV2ComputePlugin.SERVER_JSON_FIELD, server);
    	return root;
    }
    
//    
//    @Test(expected = FogbowManagerException.class)
//    public void testRequestInstanceIrregularSyntax() throws IOException, FogbowManagerException, UnexpectedException {
//        HardwareRequirements flavor = mock(HardwareRequirements.class);
//        doReturn(flavor)
//                .when(novaV2ComputeOpenStack)
//                .findSmallestFlavor(any(ComputeOrder.class), eq(localToken));
//
//        String fakeCommand = "fake-command";
//        doReturn(fakeCommand)
//                .when(launchCommandGenerator)
//                .createLaunchCommand(any(ComputeOrder.class));
//        
//        HttpRequestClientUtil httpRequestClientUtil = Mockito.mock(HttpRequestClientUtil.class);
//        this.novaV2ComputeOpenStack.setClient(httpRequestClientUtil);
//        
//        doReturn(INVALID_FAKE_POST_RETURN)
//                .when(httpRequestClientUtil)
//                .doPostRequest(anyString(), any(Token.class), any(JSONObject.class));
//
//        novaV2ComputeOpenStack.requestInstance(computeOrder, localToken);
//    }
//
//    @Test
//    public void testGenerateJsonRequest() {
//        List<String> netIds = new ArrayList<String>();
//        netIds.add("netId");
//
//        JSONObject json =
//                novaV2ComputeOpenStack.generateJsonRequest(
//                        "imageRef", "flavorRef", "user-data", "keyName", netIds);
//
//        assertNotNull(json);
//        JSONObject server = json.getJSONObject("server");
//        assertNotNull(server);
//
//        String name = server.getString("name");
//        assertNotNull(name);
//
//        String image = server.getString("imageRef");
//        assertEquals("imageRef", image);
//
//        String key = server.getString("key_name");
//        assertEquals("keyName", key);
//
//        String flavor = server.getString("flavorRef");
//        assertEquals("flavorRef", flavor);
//
//        String user = server.getString("user_data");
//        assertEquals("user-data", user);
//
//        JSONArray net = server.getJSONArray("networks");
//        assertNotNull(net);
//        assertEquals(1, net.length());
//        assertEquals("netId", net.getJSONObject(0).getString("uuid"));
//    }
//
//    @Test(expected = JSONException.class)
//    public void testGenerateJsonRequestWithoutUserData() {
//        List<String> netIds = new ArrayList<String>();
//        netIds.add("netId");
//
//        JSONObject json =
//                novaV2ComputeOpenStack.generateJsonRequest(
//                        "imageRef", "flavorRef", null, "keyName", netIds);
//
//        assertNotNull(json);
//        JSONObject server = json.getJSONObject("server");
//        assertNotNull(server);
//
//        String name = server.getString("name");
//        assertNotNull(name);
//
//        String image = server.getString("imageRef");
//        assertEquals("imageRef", image);
//
//        String key = server.getString("key_name");
//        assertEquals("keyName", key);
//
//        String flavor = server.getString("flavorRef");
//        assertEquals("flavorRef", flavor);
//
//        JSONArray net = server.getJSONArray("networks");
//        assertNotNull(net);
//        assertEquals(1, net.length());
//        assertEquals("netId", net.getJSONObject(0).getString("uuid"));
//
//        server.get("user_data");
//    }
//
//    @Test(expected = JSONException.class)
//    public void testGenerateJsonRequestWithoutNetwork() {
//        JSONObject json =
//                novaV2ComputeOpenStack.generateJsonRequest(
//                        "imageRef", "flavorRef", "user-data", "keyName", null);
//
//        assertNotNull(json);
//        JSONObject server = json.getJSONObject("server");
//        assertNotNull(server);
//
//        String name = server.getString("name");
//        assertNotNull(name);
//
//        String image = server.getString("imageRef");
//        assertEquals("imageRef", image);
//
//        String key = server.getString("key_name");
//        assertEquals("keyName", key);
//
//        String flavor = server.getString("flavorRef");
//        assertEquals("flavorRef", flavor);
//
//        server.getJSONArray("networks");
//    }
//
//    @Test(expected = JSONException.class)
//    public void testGenerateJsonRequestWithoutKeyName() {
//        List<String> netIds = new ArrayList<String>();
//        netIds.add("netId");
//
//        JSONObject json =
//                novaV2ComputeOpenStack.generateJsonRequest(
//                        "imageRef", "flavorRef", "user-data", null, netIds);
//
//        assertNotNull(json);
//        JSONObject server = json.getJSONObject("server");
//        assertNotNull(server);
//
//        String name = server.getString("name");
//        assertNotNull(name);
//
//        String image = server.getString("imageRef");
//        assertEquals("imageRef", image);
//
//        String flavor = server.getString("flavorRef");
//        assertEquals("flavorRef", flavor);
//
//        JSONArray net = server.getJSONArray("networks");
//        assertNotNull(net);
//        assertEquals(1, net.length());
//        assertEquals("netId", net.getJSONObject(0).getString("uuid"));
//
//        server.getString("key_name");
//    }
//
//    @Test
//    public void testGetActivateInstance() throws FogbowManagerException, UnexpectedException, HttpResponseException {
//        doReturn(FAKE_ENDPOINT)
//                .when(novaV2ComputeOpenStack)
//                .getComputeEndpoint(anyString(), anyString());
//        
//        HttpRequestClientUtil httpRequestClientUtil = Mockito.mock(HttpRequestClientUtil.class);
//        this.novaV2ComputeOpenStack.setClient(httpRequestClientUtil);
//        
//        doReturn(FAKE_GET_RETURN_ACTIVE_INSTANCE)
//                .when(httpRequestClientUtil)
//                .doGetRequest(eq(FAKE_ENDPOINT), eq(localToken));
//
//        ComputeInstance computeInstance =
//                this.novaV2ComputeOpenStack.getInstance(FAKE_INSTANCE_ID, this.localToken);
//
//        assertEquals(computeInstance.getId(), FAKE_INSTANCE_ID);
//        assertEquals(computeInstance.getState(), InstanceState.READY);
//    }
//
//    @Test
//    public void testGetFailedInstance() throws FogbowManagerException, HttpResponseException, UnexpectedException {
//        doReturn(FAKE_ENDPOINT)
//                .when(novaV2ComputeOpenStack)
//                .getComputeEndpoint(anyString(), anyString());
//        
//        HttpRequestClientUtil httpRequestClientUtil = Mockito.mock(HttpRequestClientUtil.class);
//        this.novaV2ComputeOpenStack.setClient(httpRequestClientUtil);
//        
//        doReturn(FAKE_GET_RETURN_FAILED_INSTANCE)
//                .when(httpRequestClientUtil)
//                .doGetRequest(eq(FAKE_ENDPOINT), eq(localToken));
//
//        ComputeInstance computeInstance =
//                this.novaV2ComputeOpenStack.getInstance(FAKE_INSTANCE_ID, this.localToken);
//
//        assertEquals(computeInstance.getId(), FAKE_INSTANCE_ID);
//        assertEquals(computeInstance.getState(), InstanceState.FAILED);
//    }
//
//    @Test
//    public void testGetInactiveInstance() throws FogbowManagerException, HttpResponseException, UnexpectedException {
//        doReturn(FAKE_ENDPOINT)
//                .when(this.novaV2ComputeOpenStack)
//                .getComputeEndpoint(anyString(), anyString());
//        
//        HttpRequestClientUtil httpRequestClientUtil = Mockito.mock(HttpRequestClientUtil.class);
//        this.novaV2ComputeOpenStack.setClient(httpRequestClientUtil);
//        
//        doReturn(FAKE_GET_RETURN_INACTIVE_INSTANCE)
//                .when(httpRequestClientUtil)
//                .doGetRequest(eq(FAKE_ENDPOINT), eq(this.localToken));
//
//        ComputeInstance computeInstance =
//                this.novaV2ComputeOpenStack.getInstance(FAKE_INSTANCE_ID, this.localToken);
//
//        assertEquals(computeInstance.getId(), FAKE_INSTANCE_ID);
//        assertEquals(computeInstance.getState(), InstanceState.SPAWNING);
//    }
//
//    @Test(expected = Exception.class)
//    public void testGetInstanceWithJSONException() throws FogbowManagerException, HttpResponseException, UnexpectedException {
//    	
//    	HttpRequestClientUtil httpRequestClientUtil = Mockito.mock(HttpRequestClientUtil.class);
//        this.novaV2ComputeOpenStack.setClient(httpRequestClientUtil);
//    	
//        doReturn(INVALID_FAKE_POST_RETURN)
//                .when(httpRequestClientUtil)
//                .doGetRequest(anyString(), any(Token.class));
//
//        this.novaV2ComputeOpenStack.getInstance(FAKE_INSTANCE_ID, this.localToken);
//    }
}
