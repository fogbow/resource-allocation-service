package org.fogbowcloud.manager.core.plugins.cloud.openstack;

import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;

import org.apache.http.HttpStatus;
import org.apache.http.client.HttpResponseException;
import org.fogbowcloud.manager.core.HomeDir;
import org.fogbowcloud.manager.core.PropertiesHolder;
import org.fogbowcloud.manager.core.exceptions.FogbowManagerException;
import org.fogbowcloud.manager.core.exceptions.InvalidParameterException;
import org.fogbowcloud.manager.core.exceptions.UnauthenticatedUserException;
import org.fogbowcloud.manager.core.exceptions.UnauthorizedRequestException;
import org.fogbowcloud.manager.core.exceptions.UnavailableProviderException;
import org.fogbowcloud.manager.core.exceptions.UnexpectedException;
import org.fogbowcloud.manager.core.models.instances.ComputeInstance;
import org.fogbowcloud.manager.core.models.instances.InstanceState;
import org.fogbowcloud.manager.core.models.instances.InstanceType;
import org.fogbowcloud.manager.core.models.orders.ComputeOrder;
import org.fogbowcloud.manager.core.models.orders.UserData;
import org.fogbowcloud.manager.core.models.tokens.Token;
import org.fogbowcloud.manager.core.models.tokens.Token.User;
import org.fogbowcloud.manager.core.plugins.cloud.util.CloudInitUserDataBuilder;
import org.fogbowcloud.manager.core.plugins.cloud.util.LaunchCommandGenerator;
import org.fogbowcloud.manager.util.connectivity.HttpRequestClientUtil;
import org.json.JSONArray;
import org.json.JSONObject;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;

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
    private final String computeNovaV2UrlKey = "compute-nova-v2-url-key";
    
	private ArgumentCaptor<String> argString  = ArgumentCaptor.forClass(String.class);
	private ArgumentCaptor<Token> argToken = ArgumentCaptor.forClass(Token.class);
	private ArgumentCaptor<JSONObject> argJson = ArgumentCaptor.forClass(JSONObject.class);
	
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
        
    	Mockito.when(
    			this.propertiesMock.getProperty(OpenStackNovaV2ComputePlugin.COMPUTE_NOVAV2_URL_KEY)).
    			thenReturn(this.computeNovaV2UrlKey);
    }

    @Test
    public void testRequestInstance() throws IOException, FogbowManagerException, UnexpectedException {
    	String bestFlavorId = "best-flavor";
    	int bestCpu = 2;
    	int bestMemory = 1024;
    	int bestDisk = 8;
    	
    	mockGetFlavorsRequest(bestFlavorId, bestCpu, bestMemory, bestDisk);
    	
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

    	String idKeyName = "493315b3-dd01-4b38-974f-289570f8e7ee";
    	
    	JSONObject rootKeypairJson = generateRootKeyPairJson(idKeyName, publicKey);
    	
    	Mockito.when(this.httpRequestClientUtilMock.doPostRequest(this.argString.capture(), 
    				this.argToken.capture(), 
    				this.argJson.capture())).thenReturn("");
    	        
    	ComputeOrder computeOrder =
                new ComputeOrder(
                        null,
                        null,
                        null,
                        bestCpu,
                        bestMemory,
                        bestDisk,
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

    	String idInstanceName = "12345678-dd01-4b38-974f-289570f8e7ee";
    	
    	doReturn(idKeyName).doReturn(idInstanceName).when(this.novaV2ComputeOpenStack).getRandomUUID();
    	
    	JSONObject computeJson =
    			generateJsonRequest(imageId, bestFlavorId, userData, idKeyName, networksId, idInstanceName);
    	
    	String expectedInstanceId = "instance-id-00";
    	String expectedInstanceIdJson = generateInstaceId("instance-id-00");
    	
    	Mockito.doReturn(expectedInstanceIdJson).when(this.httpRequestClientUtilMock).doPostRequest(argString.capture(), 
    			argToken.capture(), 
    			argJson.capture());
    	
    	String instanceId = this.novaV2ComputeOpenStack.requestInstance(computeOrder, this.localToken);
    	
    	Assert.assertEquals(this.argString.getAllValues().get(0), osKeyPairEndpoint);
    	Assert.assertEquals(this.argToken.getAllValues().get(0), this.localToken);
    	Assert.assertEquals(this.argJson.getAllValues().get(0).toString(), rootKeypairJson.toString());
    	
    	Assert.assertEquals(this.argString.getAllValues().get(1), computeEndpoint);
    	Assert.assertEquals(this.argToken.getAllValues().get(1), this.localToken);
    	Assert.assertEquals(this.argJson.getAllValues().get(1).toString(), computeJson.toString());
    	
    	Assert.assertEquals(expectedInstanceId, instanceId);
    }

    private void mockGetFlavorsRequest(String bestFlavorId, int bestVcpu, int bestMemory, int bestDisk) throws HttpResponseException, UnavailableProviderException {
    	
    	int qtdFlavors = 100;
    	String flavorEndpoint = this.computeNovaV2UrlKey
                + OpenStackNovaV2ComputePlugin.COMPUTE_V2_API_ENDPOINT
                + this.localToken.getAttributes().get(OpenStackNovaV2ComputePlugin.TENANT_ID)
                + OpenStackNovaV2ComputePlugin.SUFFIX_ENDPOINT_FLAVORS;
    	
    	Mockito.when(this.httpRequestClientUtilMock.doGetRequest(flavorEndpoint, this.localToken))
    			.thenReturn(generateJsonFlavors(qtdFlavors, bestFlavorId));
    	
    	for (int i = 0; i < qtdFlavors - 1; i++) {
    		String flavorId = "flavor" + Integer.toString(i);
    		String newEndpoint = flavorEndpoint + "/" + flavorId;
    		String flavorJson = generateJsonFlavor(
    				flavorId, 
    				"nameflavor" + Integer.toString(i), 
    				Integer.toString(Math.max(1, bestVcpu - 1 - i)), 
    				Integer.toString(Math.max(1, bestMemory - 1 - i)), 
    				Integer.toString(Math.max(1, bestDisk - 1 - i)));
    		Mockito.when(this.httpRequestClientUtilMock.doGetRequest(newEndpoint, this.localToken))
    				.thenReturn(flavorJson);
    	}
    	
    	String newEndpoint = flavorEndpoint + "/" + bestFlavorId;
		String flavorJson = generateJsonFlavor(
				bestFlavorId, 
				"nameflavor" + bestFlavorId, 
				Integer.toString(bestVcpu), 
				Integer.toString(bestMemory), 
				Integer.toString(bestDisk));
		
		Mockito.when(this.httpRequestClientUtilMock.doGetRequest(newEndpoint, this.localToken))
				.thenReturn(flavorJson);
    }
    
    private String generateJsonFlavors(int qtd, String bestFlavorId) {
    	Map <String, Object> jsonFlavorsMap = new HashMap<String, Object>();
    	List<Map <String, String>> jsonArrayFlavors = new ArrayList<Map<String, String>>();
    	for (int i = 0; i < qtd - 1; i++) {
    		Map <String, String> flavor = new HashMap<String, String>();
    		flavor.put(OpenStackNovaV2ComputePlugin.ID_JSON_FIELD, "flavor" + Integer.toString(i));
    		jsonArrayFlavors.add(flavor);
    	}
		
    	Map <String, String> flavor = new HashMap<String, String>();
		flavor.put(OpenStackNovaV2ComputePlugin.ID_JSON_FIELD, bestFlavorId);
		jsonArrayFlavors.add(flavor);
		
    	jsonFlavorsMap.put(OpenStackNovaV2ComputePlugin.FLAVOR_JSON_KEY, jsonArrayFlavors);
    	Gson gson = new Gson();
    	return gson.toJson(jsonFlavorsMap);
    }
    
    private String generateJsonFlavor(String id, String name, String vcpu, String memory, String disk) {
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
    	
        if (keyName != null) {
        	server.put(OpenStackNovaV2ComputePlugin.KEY_JSON_FIELD, keyName);
        }
        
    	JSONObject root = new JSONObject();
    	root.put(OpenStackNovaV2ComputePlugin.SERVER_JSON_FIELD, server);
    	return root;
    }
    
    @Test
    public void testGetInstance() throws FogbowManagerException, UnexpectedException, HttpResponseException {
    	
    	String instanceId = "compute-instance-id";
    	String openstackState = OpenStackStateMapper.ACTIVE_STATUS;
    	InstanceState fogbowState = OpenStackStateMapper.map(InstanceType.COMPUTE, openstackState);
    	String hostName = "hostName";
    	int vCPU = 10;
    	int ram = 15;
    	int disk = 20;
    	String localIpAddress = "localIpAddress";
    	
    	String computeEndpoint = computeNovaV2UrlKey
                + OpenStackNovaV2ComputePlugin.COMPUTE_V2_API_ENDPOINT
                + this.localToken.getAttributes().get(OpenStackNovaV2ComputePlugin.TENANT_ID)
                + OpenStackNovaV2ComputePlugin.SERVERS + "/" + instanceId;
    	
    	String flavorId = "flavorId";
    	
    	String computeInstanceJson = generateComputeInstanceJson(instanceId, hostName, localIpAddress, flavorId, openstackState);
    	Mockito.when(this.httpRequestClientUtilMock.doGetRequest(computeEndpoint, this.localToken)).thenReturn(computeInstanceJson);
    	
    	mockGetFlavorsRequest(flavorId, vCPU, ram, disk);

    	ComputeInstance expectedComputeInstance = new ComputeInstance(instanceId, fogbowState, hostName, vCPU, ram, disk, localIpAddress);
    	ComputeInstance pluginComputeInstance = this.novaV2ComputeOpenStack.getInstance(instanceId, this.localToken);
    	
    	Assert.assertEquals(expectedComputeInstance.getHostName(), pluginComputeInstance.getHostName());
    	Assert.assertEquals(expectedComputeInstance.getId(), pluginComputeInstance.getId());
    	Assert.assertEquals(expectedComputeInstance.getLocalIpAddress(), pluginComputeInstance.getLocalIpAddress());
    	Assert.assertEquals(expectedComputeInstance.getDisk(), pluginComputeInstance.getDisk());
    	Assert.assertEquals(expectedComputeInstance.getRam(), pluginComputeInstance.getRam());
    	Assert.assertEquals(expectedComputeInstance.getSshTunnelConnectionData(), pluginComputeInstance.getSshTunnelConnectionData());
    	Assert.assertEquals(expectedComputeInstance.getState(), pluginComputeInstance.getState());
    	Assert.assertEquals(expectedComputeInstance.getvCPU(), pluginComputeInstance.getvCPU());
    }
    
    private String generateComputeInstanceJson(String instanceId, String hostName, String localIpAddress, String flavorId, String status) {
    	Map<String, Object> root = new HashMap<String, Object>();
    	Map<String, Object> computeInstance = new HashMap<String, Object>();
    	computeInstance.put(OpenStackNovaV2ComputePlugin.ID_JSON_FIELD, instanceId);
    	computeInstance.put(OpenStackNovaV2ComputePlugin.NAME_JSON_FIELD, hostName);
    	
    	Map<String, Object> addressField = new HashMap<String, Object>();
    	List <Object> providerNetworkArray = new ArrayList<Object>();
    	Map<String, String> providerNetwork = new HashMap<String, String>();
    	providerNetwork.put(OpenStackNovaV2ComputePlugin.ADDR_FIELD, localIpAddress);
    	providerNetworkArray.add(providerNetwork);
    	addressField.put(OpenStackNovaV2ComputePlugin.PROVIDER_NETWORK_FIELD, providerNetworkArray);
    	
    	Map<String, Object> flavor = new HashMap<String, Object>();
    	flavor.put(OpenStackNovaV2ComputePlugin.FLAVOR_ID_JSON_FIELD, flavorId);
    	
    	computeInstance.put(OpenStackNovaV2ComputePlugin.FLAVOR_JSON_FIELD, flavor);
    	computeInstance.put(OpenStackNovaV2ComputePlugin.ADDRESS_FIELD, addressField);
    	computeInstance.put(OpenStackNovaV2ComputePlugin.STATUS_JSON_FIELD, status);
    	
    	root.put(OpenStackNovaV2ComputePlugin.SERVER_JSON_FIELD, computeInstance);
    	return new Gson().toJson(root);
    }
    
    @Test
    public void deleteInstanceTest() throws HttpResponseException, FogbowManagerException, UnexpectedException { 
    	String instanceId = "instance-id";
    	String deleteEndpoint =  this.computeNovaV2UrlKey
					    			+ OpenStackNovaV2ComputePlugin.COMPUTE_V2_API_ENDPOINT 
					    			+ this.localToken.getAttributes().get(OpenStackNovaV2ComputePlugin.TENANT_ID)
					    			+ OpenStackNovaV2ComputePlugin.SERVERS + "/" + instanceId;
    	
    	ArgumentCaptor<String> argEndpoint = ArgumentCaptor.forClass(String.class);
    	ArgumentCaptor<Token> argLocalToken = ArgumentCaptor.forClass(Token.class);
    	
    	Mockito.doNothing()
    		.when(this.httpRequestClientUtilMock)
    		.doDeleteRequest(argEndpoint.capture(), argLocalToken.capture());

    	this.novaV2ComputeOpenStack.deleteInstance(instanceId, this.localToken);
    	Assert.assertEquals(argEndpoint.getValue(), deleteEndpoint);
    	Assert.assertEquals(argLocalToken.getValue(), this.localToken);
    }
    
    @Test (expected = UnauthorizedRequestException.class)
    public void testGetInstanceOnForbidden() throws FogbowManagerException, UnexpectedException, HttpResponseException {	
    	String instanceId = "compute-instance-id";
    	String computeEndpoint = computeNovaV2UrlKey
                + OpenStackNovaV2ComputePlugin.COMPUTE_V2_API_ENDPOINT
                + this.localToken.getAttributes().get(OpenStackNovaV2ComputePlugin.TENANT_ID)
                + OpenStackNovaV2ComputePlugin.SERVERS + "/" + instanceId;
    	Mockito.when(this.httpRequestClientUtilMock.doGetRequest(computeEndpoint, this.localToken))
    		.thenThrow(new HttpResponseException(HttpStatus.SC_FORBIDDEN, ""));
    	this.novaV2ComputeOpenStack.getInstance(instanceId, this.localToken);
    }
    
    @Test (expected = UnauthorizedRequestException.class)
    public void deleteInstanceTestOnForbidden() throws HttpResponseException, FogbowManagerException, UnexpectedException { 
    	String instanceId = "instance-id";
    	String deleteEndpoint =  this.computeNovaV2UrlKey
					    			+ OpenStackNovaV2ComputePlugin.COMPUTE_V2_API_ENDPOINT 
					    			+ this.localToken.getAttributes().get(OpenStackNovaV2ComputePlugin.TENANT_ID)
					    			+ OpenStackNovaV2ComputePlugin.SERVERS + "/" + instanceId;
    	Mockito.doThrow(new HttpResponseException(HttpStatus.SC_FORBIDDEN, ""))
    		.when(this.httpRequestClientUtilMock)
    		.doDeleteRequest(deleteEndpoint, this.localToken);
    	this.novaV2ComputeOpenStack.deleteInstance(instanceId, this.localToken);
    }
    
    @Test (expected = UnauthenticatedUserException.class)
    public void testRequestInstanceOnAnauthorizedComputePost() throws IOException, FogbowManagerException, UnexpectedException {
    	
    	String bestFlavorId = "best-flavor";
    	int bestCpu = 2;
    	int bestMemory = 1024;
    	int bestDisk = 8;
    	
    	mockGetFlavorsRequest(bestFlavorId, bestCpu, bestMemory, bestDisk);
    	    	
    	Mockito.when(this.propertiesMock.getProperty(OpenStackNovaV2ComputePlugin.DEFAULT_NETWORK_ID_KEY))
    			.thenReturn("");

    	Mockito.when(this.httpRequestClientUtilMock.doPostRequest(Mockito.any(), 
    			Mockito.any(), 
    			Mockito.any())).thenReturn("");
    	
    	ComputeOrder computeOrder =
                new ComputeOrder(
                        null,
                        null,
                        null,
                        bestCpu,
                        bestMemory,
                        bestDisk,
                        "",
                        new UserData(
                                FAKE_USER_DATA_FILE,
                                CloudInitUserDataBuilder.FileType.SHELL_SCRIPT),
                        "",
                        null);
    	
    	
    	Mockito.when(this.launchCommandGeneratorMock.createLaunchCommand(Mockito.any()))
    			.thenReturn("");
    	
    	Mockito.when(this.httpRequestClientUtilMock.doPostRequest(
    			Mockito.any(), 
    			Mockito.any(), 
    			Mockito.any()))
    			.thenThrow(new HttpResponseException(HttpStatus.SC_UNAUTHORIZED, ""));
    	
    	this.novaV2ComputeOpenStack.requestInstance(computeOrder, this.localToken);
    }
    
    @Test
    public void testRequestInstanceWhenPublicKeyIsNull() throws IOException, FogbowManagerException, UnexpectedException {
    	String bestFlavorId = "best-flavor";
    	int bestCpu = 2;
    	int bestMemory = 1024;
    	int bestDisk = 8;
    	
    	mockGetFlavorsRequest(bestFlavorId, bestCpu, bestMemory, bestDisk);
    	
    	String defaultNetworkIp = "192.168.0.2";
    	
    	Mockito.when(this.propertiesMock.getProperty(OpenStackNovaV2ComputePlugin.DEFAULT_NETWORK_ID_KEY))
    			.thenReturn(defaultNetworkIp);
    	
    	String imageId = "image-id";
    	
    	String publicKey = null;
    
    	ComputeOrder computeOrder =
                new ComputeOrder(
                        null,
                        null,
                        null,
                        bestCpu,
                        bestMemory,
                        bestDisk,
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
    	
    	String idKeyName = null;
    	
    	String idInstanceName = "12345678-dd01-4b38-974f-289570f8e7ee";
    	
    	doReturn(idInstanceName).when(this.novaV2ComputeOpenStack).getRandomUUID();
    	
    	JSONObject computeJson =
    			generateJsonRequest(imageId, bestFlavorId, userData, idKeyName, networksId, idInstanceName);
    	
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
    
    @Test (expected = InvalidParameterException.class)
    public void testRequestInstanceOnBadRequestKeyNamePost() throws IOException, FogbowManagerException, UnexpectedException {
    	String bestFlavorId = "best-flavor";
    	int bestCpu = 2;
    	int bestMemory = 1024;
    	int bestDisk = 8;
    	
    	mockGetFlavorsRequest(bestFlavorId, bestCpu, bestMemory, bestDisk);
    	
    	String defaultNetworkIp = "192.168.0.2";
    	
    	Mockito.when(this.propertiesMock.getProperty(OpenStackNovaV2ComputePlugin.DEFAULT_NETWORK_ID_KEY))
    			.thenReturn(defaultNetworkIp);
    	
    	String imageId = "image-id";
    	String publicKey = "public-key";
    	String idKeyName = "493315b3-dd01-4b38-974f-289570f8e7ee";
    	
    	Mockito.when(this.httpRequestClientUtilMock.doPostRequest(Mockito.any(), 
    			Mockito.any(), 
    			Mockito.any())).thenThrow(new HttpResponseException(HttpStatus.SC_BAD_REQUEST, ""));
    	        
    	ComputeOrder computeOrder =
                new ComputeOrder(
                        null,
                        null,
                        null,
                        bestCpu,
                        bestMemory,
                        bestDisk,
                        imageId,
                        new UserData(
                                FAKE_USER_DATA_FILE,
                                CloudInitUserDataBuilder.FileType.SHELL_SCRIPT),
                        publicKey,
                        null);
    	
    	String userData = "userDataFromLauchCommand";
    	Mockito.when(this.launchCommandGeneratorMock.createLaunchCommand(computeOrder))
    			.thenReturn(userData);
    	List <String> networksId =  new ArrayList<String>();
    	networksId.add(defaultNetworkIp);
    	String idInstanceName = "12345678-dd01-4b38-974f-289570f8e7ee";
    	doReturn(idKeyName).doReturn(idInstanceName).when(this.novaV2ComputeOpenStack).getRandomUUID();
    	this.novaV2ComputeOpenStack.requestInstance(computeOrder, this.localToken);
    }
    
    @Test (expected = UnauthenticatedUserException.class)
    public void testRequestInstanceWhenDeleteKeyUnauthorized() throws IOException, FogbowManagerException, UnexpectedException {
    	String bestFlavorId = "best-flavor";
    	int bestCpu = 2;
    	int bestMemory = 1024;
    	int bestDisk = 8;
    	
    	mockGetFlavorsRequest(bestFlavorId, bestCpu, bestMemory, bestDisk);
    	
    	String defaultNetworkIp = "192.168.0.2";
    	
    	Mockito.when(this.propertiesMock.getProperty(OpenStackNovaV2ComputePlugin.DEFAULT_NETWORK_ID_KEY))
    			.thenReturn(defaultNetworkIp);
    	
    	String imageId = "image-id";
    	
    	String publicKey = "public-key";

    	String idKeyName = "493315b3-dd01-4b38-974f-289570f8e7ee";
    	
    	Mockito.when(this.httpRequestClientUtilMock.doPostRequest(this.argString.capture(), 
    				this.argToken.capture(), 
    				this.argJson.capture())).thenReturn("");
    	        
    	ComputeOrder computeOrder =
                new ComputeOrder(
                        null,
                        null,
                        null,
                        bestCpu,
                        bestMemory,
                        bestDisk,
                        imageId,
                        new UserData(
                                FAKE_USER_DATA_FILE,
                                CloudInitUserDataBuilder.FileType.SHELL_SCRIPT),
                        publicKey,
                        null);
    	
    	String userData = "userDataFromLauchCommand";
    	Mockito.when(this.launchCommandGeneratorMock.createLaunchCommand(computeOrder))
    			.thenReturn(userData);    	
    	List <String> networksId =  new ArrayList<String>();
    	networksId.add(defaultNetworkIp);

    	String idInstanceName = "12345678-dd01-4b38-974f-289570f8e7ee";
    	
    	doReturn(idKeyName).doReturn(idInstanceName).when(this.novaV2ComputeOpenStack).getRandomUUID();
    	String expectedInstanceIdJson = generateInstaceId("instance-id-00");
    	
    	Mockito.when(this.httpRequestClientUtilMock.doPostRequest(
    			argString.capture(), 
    			argToken.capture(), 
    			argJson.capture()))
    			.thenReturn(expectedInstanceIdJson);
    	
    	
    	Mockito.doThrow(new HttpResponseException(HttpStatus.SC_UNAUTHORIZED, "")).
    			when(this.httpRequestClientUtilMock).doDeleteRequest(Mockito.any(), Mockito.any());
    	
    	this.novaV2ComputeOpenStack.requestInstance(computeOrder, this.localToken);
    	
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
