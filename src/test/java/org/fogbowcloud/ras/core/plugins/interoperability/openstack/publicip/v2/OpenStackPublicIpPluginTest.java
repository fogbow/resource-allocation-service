package org.fogbowcloud.ras.core.plugins.interoperability.openstack.publicip.v2;

import com.google.gson.Gson;
import org.apache.http.HttpStatus;
import org.apache.http.client.HttpResponseException;
import org.apache.http.client.utils.URIBuilder;
import org.fogbowcloud.ras.core.exceptions.*;
import org.fogbowcloud.ras.core.models.instances.InstanceState;
import org.fogbowcloud.ras.core.models.instances.PublicIpInstance;
import org.fogbowcloud.ras.core.models.orders.PublicIpOrder;
import org.fogbowcloud.ras.core.models.tokens.OpenStackV3Token;
import org.fogbowcloud.ras.core.models.tokens.Token;
import org.fogbowcloud.ras.core.plugins.interoperability.openstack.OpenStackStateMapper;
import org.fogbowcloud.ras.core.plugins.interoperability.openstack.OpenstackRestApiConstants;
import org.fogbowcloud.ras.util.connectivity.HttpRequestClientUtil;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;

import java.net.URISyntaxException;
import java.util.*;

public class OpenStackPublicIpPluginTest {

	private OpenStackPublicIpPlugin openStackPublicIpPlugin;
	private HttpRequestClientUtil httpClient;
	private OpenStackV3Token openStackV3Token;
	
    private static final String FAKE_TOKEN_PROVIDER = "fake-token-provider";
    private static final String FAKE_TOKEN_VALUE = "fake-token-value";
    private static final String FAKE_USER_ID = "fake-user-id";
    private static final String FAKE_NAME = "fake-name";
    private static final String FAKE_PROJECT_ID = "fake-project-id";

	@Before
	public void setUp() {
        this.openStackV3Token = new OpenStackV3Token(FAKE_TOKEN_PROVIDER, FAKE_TOKEN_VALUE, FAKE_USER_ID, FAKE_NAME, FAKE_PROJECT_ID, null);
        this.httpClient = Mockito.mock(HttpRequestClientUtil.class);
        
        boolean notCheckProperties = false;
		this.openStackPublicIpPlugin = Mockito.spy(new OpenStackPublicIpPlugin(notCheckProperties));
        
        this.openStackPublicIpPlugin.setClient(this.httpClient);
	}
	
	// test case: success case
	@Test
	public void testROequestInstanceTest() throws Exception {
		// set up
		String computeInstanceId = "computeInstanceId";
		PublicIpOrder publicIpOrder = new PublicIpOrder();
		String externalNetworkId = "externalNetId";
		String floatingIpEndpoint = "http://endpoint";
		String portId = "portId";
		String createFloatingIpRequestBody = createFloatingIpRequestBody(externalNetworkId, FAKE_PROJECT_ID, portId);
		String floatingIpId = "floatingIpId";
		String responseCreateFloatingIp = getCreateFloatingIpResponseJson(floatingIpId);
		
		Mockito.doReturn(portId).when(this.openStackPublicIpPlugin).getNetworkPortIp(
				Mockito.eq(computeInstanceId), Mockito.eq(this.openStackV3Token));
		Mockito.when(this.openStackPublicIpPlugin.getExternalNetworkId()).thenReturn(externalNetworkId);
		Mockito.when(this.openStackPublicIpPlugin.getFloatingIpEndpoint()).thenReturn(floatingIpEndpoint);
		Mockito.when(this.httpClient.doPostRequest(
				Mockito.anyString(), Mockito.any(OpenStackV3Token.class), Mockito.anyString()))
				.thenReturn(responseCreateFloatingIp);
		
		// exercise
		String publicIpId = this.openStackPublicIpPlugin.requestInstance(publicIpOrder, computeInstanceId, this.openStackV3Token);

		// verify
		Mockito.verify(this.httpClient, Mockito.times(1)).doPostRequest(
				Mockito.eq(floatingIpEndpoint), 
				Mockito.eq(this.openStackV3Token), 
				Mockito.eq(createFloatingIpRequestBody));
		Assert.assertEquals(floatingIpId, publicIpId);
	}
	
	// test case: throw exception when trying to get the network port
	@Test(expected=FogbowRasException.class)
	public void testRequestInstanceErrorOnGetPortTest() throws HttpResponseException, URISyntaxException, FogbowRasException, UnexpectedException {
		// set up
		String computeInstanceId = "computeInstanceId";
		PublicIpOrder publicIpOrder = new PublicIpOrder();
		Mockito.doThrow(new FogbowRasException()).when(this.openStackPublicIpPlugin).getNetworkPortIp(
				Mockito.eq(computeInstanceId), Mockito.eq(this.openStackV3Token));
		
		// exercise
		this.openStackPublicIpPlugin.requestInstance(publicIpOrder, computeInstanceId, this.openStackV3Token);
		
		// verify
		Mockito.verify(this.httpClient, Mockito.times(0)).doPostRequest(
				Mockito.anyString(), 
				Mockito.any(OpenStackV3Token.class), 
				Mockito.anyString());	
	}
	
	// test case: success case
	@Test
	public void testGetNetworkPortIp()
			throws HttpResponseException, URISyntaxException, FogbowRasException, UnexpectedException {
		// set up
		String portId = "portId";
		String portsEndpoint = "http://endpoint";
		String defaultNetworkId = "defaultNetworkId";
		String computeInstanceId = "computeInstanceId";
		Mockito.doReturn(defaultNetworkId).when(this.openStackPublicIpPlugin).getDefaultNetworkId();
		Mockito.doReturn(portsEndpoint).when(this.openStackPublicIpPlugin).getNetworkPortsEndpoint();
		
		URIBuilder portsEndpointBuilder = new URIBuilder(defaultNetworkId);
		portsEndpointBuilder.addParameter(GetNetworkPortsResquest.DEVICE_ID_KEY, computeInstanceId);
		portsEndpointBuilder.addParameter(GetNetworkPortsResquest.NETWORK_ID_KEY, defaultNetworkId);
		
		String responseGetPorts = getPortsResponseJson(portId);
		Mockito.when(this.httpClient.doGetRequest(
				Mockito.anyString(), Mockito.any(OpenStackV3Token.class)))
				.thenReturn(responseGetPorts);
		
		// exercise
		String networkPortIp = this.openStackPublicIpPlugin.getNetworkPortIp(computeInstanceId, this.openStackV3Token);
		
		// verify
		Assert.assertEquals(portId, networkPortIp);
	}
	
	// test case: throw exception when happens an error on the request
	@Test(expected=InstanceNotFoundException.class)
	public void testGetNetworkPortIpErrorWhenRequest()
			throws HttpResponseException, URISyntaxException, FogbowRasException, UnexpectedException {
		// set up
		String portsEndpoint = "http://endpoint";
		String defaultNetworkId = "defaultNetworkId";
		String computeInstanceId = "computeInstanceId";
		Mockito.doReturn(defaultNetworkId).when(this.openStackPublicIpPlugin).getDefaultNetworkId();
		Mockito.doReturn(portsEndpoint).when(this.openStackPublicIpPlugin).getNetworkPortsEndpoint();
		
		URIBuilder portsEndpointBuilder = new URIBuilder(defaultNetworkId);
		portsEndpointBuilder.addParameter(GetNetworkPortsResquest.DEVICE_ID_KEY, computeInstanceId);
		portsEndpointBuilder.addParameter(GetNetworkPortsResquest.NETWORK_ID_KEY, defaultNetworkId);
		
		HttpResponseException notFoundException = new HttpResponseException(HttpStatus.SC_NOT_FOUND, "");
		Mockito.doThrow(notFoundException).when(this.httpClient).doGetRequest(
				Mockito.anyString(), Mockito.any(OpenStackV3Token.class));
		
		// exercise
		this.openStackPublicIpPlugin.getNetworkPortIp(computeInstanceId, this.openStackV3Token);		
	}	
	
	// test case: throw exception when endpoint is null
	@Test
	public void testGetNetworkPortIpWrongEndpoint()
			throws HttpResponseException, URISyntaxException, FogbowRasException, UnexpectedException {
		// set up
		String portsEndpoint = null;
		String defaultNetworkId = "defaultNetworkId";
		String computeInstanceId = "computeInstanceId";
		Mockito.doReturn(defaultNetworkId).when(this.openStackPublicIpPlugin).getDefaultNetworkId();
		Mockito.doReturn(portsEndpoint).when(this.openStackPublicIpPlugin).getNetworkPortsEndpoint();
		
		// exercise
		try {
			this.openStackPublicIpPlugin.getNetworkPortIp(computeInstanceId, this.openStackV3Token);	
			Assert.fail();
		} catch (Exception e) {}
		
		// verify
		Mockito.verify(this.httpClient, Mockito.never())
				.doGetRequest(Mockito.anyString(), Mockito.any(Token.class));
	}	
	
	// test case: throw FogbowRasException because the cloud found two or more ports. In the Fogbow scenario is not allowed
	@Test
	public void testGetNetworkPortIpPortsSizeIrregular()
			throws HttpResponseException, URISyntaxException, FogbowRasException, UnexpectedException {
		// set up
		String portId = "portId";
		String portsEndpoint = "http://endpoint";
		String defaultNetworkId = "defaultNetworkId";
		String computeInstanceId = "computeInstanceId";
		Mockito.doReturn(defaultNetworkId).when(this.openStackPublicIpPlugin).getDefaultNetworkId();
		Mockito.doReturn(portsEndpoint).when(this.openStackPublicIpPlugin).getNetworkPortsEndpoint();
		
		URIBuilder portsEndpointBuilder = new URIBuilder(defaultNetworkId);
		portsEndpointBuilder.addParameter(GetNetworkPortsResquest.DEVICE_ID_KEY, computeInstanceId);
		portsEndpointBuilder.addParameter(GetNetworkPortsResquest.NETWORK_ID_KEY, defaultNetworkId);
		
		String irregularPordId = "irregularPortId";
		String responseGetPorts = getPortsResponseJson(portId, irregularPordId);
		Mockito.when(this.httpClient.doGetRequest(
				Mockito.anyString(), Mockito.any(OpenStackV3Token.class)))
				.thenReturn(responseGetPorts);
		
		GetNetworkPortsResquest getNetworkPortsResquest = new GetNetworkPortsResquest.Builder()
				.url(portsEndpoint).deviceId(computeInstanceId).networkId(defaultNetworkId).build();
		String endpointExcepeted = getNetworkPortsResquest.getUrl();
		
		// exercise
		try {
			this.openStackPublicIpPlugin.getNetworkPortIp(computeInstanceId, this.openStackV3Token);
			Assert.fail();
		} catch (FogbowRasException e) {}
		
		// verify
		Mockito.verify(this.httpClient, Mockito.times(1)).doGetRequest(
				Mockito.eq(endpointExcepeted), 
				Mockito.eq(this.openStackV3Token));		
	}	
	
	// test case: success case
	@Test
	public void testDeleteInstance()
			throws Exception {
		// set up
		String floatingIpId = "floatingIpId";
		String floatingIpEndpoint = "http://endpoint";
		Mockito.doReturn(floatingIpEndpoint).when(this.openStackPublicIpPlugin).getFloatingIpEndpoint();
		String endpointExcepted = String.format("%s/%s", floatingIpEndpoint, floatingIpId);		
		
		// exercise
		this.openStackPublicIpPlugin.deleteInstance(floatingIpId, openStackV3Token);
		
		// verify
		Mockito.verify(this.httpClient, Mockito.times(1))
				.doDeleteRequest(Mockito.eq(endpointExcepted), Mockito.eq(this.openStackV3Token));
	}
	
	// test case: the cloud throws http exception
	@Test(expected=InstanceNotFoundException.class)
	public void testDeleteInstanceHttpException()
			throws Exception {
		// set up
		String floatingIpId = "floatingIpId";
		String floatingIpEndpoint = "http://endpoint";
		Mockito.doReturn(floatingIpEndpoint).when(this.openStackPublicIpPlugin).getFloatingIpEndpoint();

		HttpResponseException notFoundException = new HttpResponseException(HttpStatus.SC_NOT_FOUND, "");
		Mockito.doThrow(notFoundException).when(
				this.httpClient).doDeleteRequest(Mockito.anyString(), Mockito.any(Token.class));
		
		// exercise
		this.openStackPublicIpPlugin.deleteInstance(floatingIpId, openStackV3Token);
	}	
	
	// test case: success case
	@Test
	public void testDefaultNetworkId() {
		// set up
		Properties properties = new Properties();
		String networkIdExpected = "id";
		properties.setProperty(OpenStackPublicIpPlugin.DEFAULT_NETWORK_ID_KEY, networkIdExpected);
		this.openStackPublicIpPlugin.setProperties(properties);
		
		// exercise
		String defaultNetworkId = this.openStackPublicIpPlugin.getDefaultNetworkId();
		
		//verify
		Assert.assertEquals(networkIdExpected, defaultNetworkId);
	}
	
	// test case: success case
	@Test
	public void testExternalNetworkId() {
		// set up
		Properties properties = new Properties();
		String networkIdExpected = "id";
		properties.setProperty(OpenStackPublicIpPlugin.EXTERNAL_NETWORK_ID_KEY, networkIdExpected);
		this.openStackPublicIpPlugin.setProperties(properties);
		
		// exercise
		String externalNetworkId = this.openStackPublicIpPlugin.getExternalNetworkId();
		
		//verify
		Assert.assertEquals(networkIdExpected, externalNetworkId);
	}
	
	// test case: success case
	@Test
	public void testNeutroApiEndpoint() {
		// set up
		Properties properties = new Properties();
		String neutroEndpoint = "id";
		properties.setProperty(OpenStackPublicIpPlugin.NETWORK_NEUTRONV2_URL_KEY, neutroEndpoint);
		this.openStackPublicIpPlugin.setProperties(properties);
		
		// exercise
		String neutroApiEndpoint = this.openStackPublicIpPlugin.getNeutroApiEndpoint();
		
		//verify
		Assert.assertEquals(neutroEndpoint, neutroApiEndpoint);
	}	
	
	// test case: success case
	@Test
	public void testCheckProperties() {
		// set up
		Properties properties = new Properties();
		properties.setProperty(OpenStackPublicIpPlugin.DEFAULT_NETWORK_ID_KEY, "something");
		properties.setProperty(OpenStackPublicIpPlugin.EXTERNAL_NETWORK_ID_KEY, "something");
		properties.setProperty(OpenStackPublicIpPlugin.NETWORK_NEUTRONV2_URL_KEY, "something");
		this.openStackPublicIpPlugin.setProperties(properties);
		
		// exercise
		this.openStackPublicIpPlugin.checkProperties(true);
		
		// verify 
		Mockito.verify(this.openStackPublicIpPlugin, Mockito.times(1))
				.getDefaultNetworkId();
		Mockito.verify(this.openStackPublicIpPlugin, Mockito.times(1))
				.getExternalNetworkId();
		Mockito.verify(this.openStackPublicIpPlugin, Mockito.times(1))
				.getNeutroApiEndpoint();
	}
	
	// test case: without default network in properties
	@Test
	public void testCheckPropertiesWithoutDefaultNetworkPropertie() {	
		try {
			// exercise
			this.openStackPublicIpPlugin.checkProperties(true);
			Assert.fail();
		} catch (FatalErrorException e) {
			// verify 
			Mockito.verify(this.openStackPublicIpPlugin, Mockito.timeout(1))
					.getDefaultNetworkId();
			Mockito.verify(this.openStackPublicIpPlugin, Mockito.never())
					.getExternalNetworkId();			
		} catch (Exception e) {
			Assert.fail();
		}
	}
	
	// test case: without external network in properties
	@Test
	public void testCheckPropertiesWithoutExternalNetworkPropertie() {	
		// set up
		Properties properties = new Properties();
		properties.setProperty(OpenStackPublicIpPlugin.DEFAULT_NETWORK_ID_KEY, "something");
		this.openStackPublicIpPlugin.setProperties(properties);
		
		try {
			// exercise
			this.openStackPublicIpPlugin.checkProperties(true);
			Assert.fail();
		} catch (FatalErrorException e) {
			// verify 
			Mockito.verify(this.openStackPublicIpPlugin, Mockito.timeout(1))
					.getDefaultNetworkId();
			Mockito.verify(this.openStackPublicIpPlugin, Mockito.timeout(1))
					.getExternalNetworkId();		
			Mockito.verify(this.openStackPublicIpPlugin, Mockito.never())
					.getNeutroApiEndpoint();			
		} catch (Exception e) {
			Assert.fail();
		}
	}	
	
	// test case: without external network in properties
	@Test
	public void testCheckPropertiesWithoutNeutroEndpointPropertie() {	
		// set up
		Properties properties = new Properties();
		properties.setProperty(OpenStackPublicIpPlugin.DEFAULT_NETWORK_ID_KEY, "something");
		properties.setProperty(OpenStackPublicIpPlugin.EXTERNAL_NETWORK_ID_KEY, "something");
		this.openStackPublicIpPlugin.setProperties(properties);
		
		try {
			// exercise
			this.openStackPublicIpPlugin.checkProperties(true);
			Assert.fail();
		} catch (FatalErrorException e) {
			// verify 
			Mockito.verify(this.openStackPublicIpPlugin, Mockito.timeout(1))
					.getDefaultNetworkId();
			Mockito.verify(this.openStackPublicIpPlugin, Mockito.timeout(1))
					.getExternalNetworkId();		
			Mockito.verify(this.openStackPublicIpPlugin, Mockito.timeout(1))
					.getNeutroApiEndpoint();			
		} catch (Exception e) {
			Assert.fail();
		}
	}
	
	// test case: success case with active status
	@Test
	public void testGetInstanceActive() throws HttpResponseException, FogbowRasException, UnexpectedException {	
		// set up
		String floatingIpId = "floatingIpId";
		String floatingIpAddress = "floatingIpAddress";
		String floatingIpStatus = OpenStackStateMapper.ACTIVE_STATUS;
		String responseGetFloatingIp = getGetFloatingIpResponseJson(floatingIpId, floatingIpAddress,
				floatingIpStatus);
		
		Mockito.when(this.httpClient.doGetRequest(
				Mockito.anyString(), Mockito.any(OpenStackV3Token.class)))
				.thenReturn(responseGetFloatingIp);
		
		// exercise
		PublicIpInstance publicIpInstance = this.openStackPublicIpPlugin
				.getInstance(floatingIpId, this.openStackV3Token);
		
		// verify
		Assert.assertEquals(floatingIpId, publicIpInstance.getId());
		Assert.assertEquals(floatingIpAddress, publicIpInstance.getIp());
		Assert.assertEquals(InstanceState.READY, publicIpInstance.getState());		
	}
	
	// test case: success case with unavailable status
	@Test
	public void testGetInstanceUnavailable() throws HttpResponseException, FogbowRasException, UnexpectedException {	
		// set up
		String floatingIpId = "floatingIpId";
		String floatingIpAddress = "floatingIpAddress";
		String floatingIpStatus = "";
		String responseGetFloatingIp = getGetFloatingIpResponseJson(floatingIpId, floatingIpAddress,
				floatingIpStatus);
		
		Mockito.when(this.httpClient.doGetRequest(
				Mockito.anyString(), Mockito.any(OpenStackV3Token.class)))
				.thenReturn(responseGetFloatingIp);
		
		// exercise
		PublicIpInstance publicIpInstance = this.openStackPublicIpPlugin
				.getInstance(floatingIpId, this.openStackV3Token);
		
		// verify
		Assert.assertEquals(floatingIpId, publicIpInstance.getId());
		Assert.assertEquals(floatingIpAddress, publicIpInstance.getIp());
		Assert.assertEquals(InstanceState.UNAVAILABLE, publicIpInstance.getState());		
	}
	
	// test case: throws FogbowRasException
	@Test
	public void testGetInstanceThrowFogbowRasException() throws HttpResponseException, FogbowRasException, UnexpectedException {	
		// set up
		String floatingIpId = "floatingIpId";
		
		HttpResponseException badRequestException = new HttpResponseException(HttpStatus.SC_BAD_REQUEST, "");
		Mockito.doThrow(badRequestException).when(this.httpClient).doGetRequest(
				Mockito.anyString(), Mockito.any(OpenStackV3Token.class));
		
		// exercise
		try {
			this.openStackPublicIpPlugin.getInstance(floatingIpId, this.openStackV3Token);
			Assert.fail();
		} catch (InvalidParameterException e) {}
		
		// verify
		Mockito.verify(this.httpClient, Mockito.times(1)).doGetRequest(
				Mockito.anyString(), 
				Mockito.eq(this.openStackV3Token));				
	}
	

	
    private String getPortsResponseJson(String... portsId) {
    	List<Map<String, Object>> idsJsonKey = new ArrayList<Map<String, Object>>();
    	
    	for (int i = 0; i < portsId.length; i++) {
    		Map<String, Object> idJsonKey = new HashMap<String, Object>();
    		idJsonKey.put(OpenstackRestApiConstants.PublicIp.ID_KEY_JSON, portsId[i]);
    		idsJsonKey.add(idJsonKey);			
		}
    	
        Map<String, Object> portsJsonKey = new HashMap<String, Object>();
        portsJsonKey.put(OpenstackRestApiConstants.PublicIp.PORTS_KEY_JSON, idsJsonKey);
        
        Gson gson = new Gson();
        return gson.toJson(portsJsonKey);
	}

	private String getCreateFloatingIpResponseJson(String floatingIpId) {
    	Map<String, Object> idJsonKey = new HashMap<String, Object>();
    	idJsonKey.put(OpenstackRestApiConstants.PublicIp.ID_KEY_JSON, floatingIpId);
        Map<String, Object> floatingipJsonKey = new HashMap<String, Object>();
        floatingipJsonKey.put(OpenstackRestApiConstants.PublicIp.FLOATING_IP_KEY_JSON, idJsonKey);
        
        Gson gson = new Gson();
        return gson.toJson(floatingipJsonKey);
    }
	
	private String getGetFloatingIpResponseJson(String floatingIpId,
			String floatingIpAddress, String status) {
    	Map<String, Object> idJsonKey = new HashMap<String, Object>();
    	idJsonKey.put(OpenstackRestApiConstants.PublicIp.ID_KEY_JSON, floatingIpId);
    	idJsonKey.put(OpenstackRestApiConstants.PublicIp.FLOATING_IP_ADDRESS_KEY_JSON, floatingIpAddress);
    	idJsonKey.put(OpenstackRestApiConstants.PublicIp.STATUS_KEY_JSON, status);
        Map<String, Object> floatingipJsonKey = new HashMap<String, Object>();
        floatingipJsonKey.put(OpenstackRestApiConstants.PublicIp.FLOATING_IP_KEY_JSON, idJsonKey);
        
        Gson gson = new Gson();
        return gson.toJson(floatingipJsonKey);
    }
	
	private String createFloatingIpRequestBody(String floatingNetworkId, String projectId, String portId) {
		CreateFloatingIpRequest createBody = new CreateFloatingIpRequest.Builder()
				.floatingNetworkId(floatingNetworkId)
				.projectId(projectId)
				.portId(portId)
				.build();
		
		return createBody.toJson();
	}
	
}
