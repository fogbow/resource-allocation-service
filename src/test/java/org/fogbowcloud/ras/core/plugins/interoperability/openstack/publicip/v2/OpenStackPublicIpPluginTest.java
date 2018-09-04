package org.fogbowcloud.ras.core.plugins.interoperability.openstack.publicip.v2;

import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.http.HttpStatus;
import org.apache.http.client.HttpResponseException;
import org.apache.http.client.utils.URIBuilder;
import org.fogbowcloud.ras.core.exceptions.FogbowRasException;
import org.fogbowcloud.ras.core.exceptions.InstanceNotFoundException;
import org.fogbowcloud.ras.core.exceptions.UnexpectedException;
import org.fogbowcloud.ras.core.models.tokens.OpenStackV3Token;
import org.fogbowcloud.ras.core.models.tokens.Token;
import org.fogbowcloud.ras.core.plugins.interoperability.openstack.OpenstackRestApiConstants;
import org.fogbowcloud.ras.util.connectivity.HttpRequestClientUtil;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import org.mockito.Mockito;

import com.google.gson.Gson;

public class OpenStackPublicIpPluginTest {

	private OpenStackPublicIpPlugin openStackPublicIpPlugin;
	private HttpRequestClientUtil httpClient;
	private OpenStackV3Token openStackV3Token;
	
    private static final String FAKE_TOKEN_PROVIDER = "fake-token-provider";
    private static final String FAKE_TOKEN_VALUE = "fake-token-value";
    private static final String FAKE_USER_ID = "fake-user-id";
    private static final String FAKE_NAME = "fake-name";
    private static final String FAKE_PROJECT_ID = "fake-project-id";
    private static final String FAKE_PROJECT_NAME = "fake-project-name";

	@Before
	public void setUp() {
        this.openStackV3Token = new OpenStackV3Token(FAKE_TOKEN_PROVIDER, FAKE_TOKEN_VALUE, FAKE_USER_ID, FAKE_NAME, FAKE_PROJECT_ID, FAKE_PROJECT_NAME);
        this.httpClient = Mockito.mock(HttpRequestClientUtil.class);
        
        boolean notCheckProperties = false;
		this.openStackPublicIpPlugin = Mockito.spy(new OpenStackPublicIpPlugin(notCheckProperties));
        
        this.openStackPublicIpPlugin.setClient(this.httpClient);
	}
	
	// test case: success case
	@Test
	public void allocatePublicIpTest() throws Exception {
		// set up
		String computeInstanceId = "computeInstanceId";
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
		String publicIpId = this.openStackPublicIpPlugin.allocatePublicIp(computeInstanceId, this.openStackV3Token);

		// verify
		Mockito.verify(this.httpClient, Mockito.times(1)).doPostRequest(
				Mockito.eq(floatingIpEndpoint), 
				Mockito.eq(this.openStackV3Token), 
				Mockito.eq(createFloatingIpRequestBody));
		Assert.assertEquals(floatingIpId, publicIpId);
	}
	
	// test case: throw exception when trying to get the network port
	@Test(expected=FogbowRasException.class)
	public void allocatePublicIpErrorOnGetPortTest() throws HttpResponseException, URISyntaxException, FogbowRasException, UnexpectedException {
		// set up
		String computeInstanceId = "computeInstanceId";
		Mockito.doThrow(new FogbowRasException()).when(this.openStackPublicIpPlugin).getNetworkPortIp(
				Mockito.eq(computeInstanceId), Mockito.eq(this.openStackV3Token));
		
		// exercise
		this.openStackPublicIpPlugin.allocatePublicIp(computeInstanceId, this.openStackV3Token);
		
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
	public void testReleasePublicIp()
			throws Exception {
		// set up
		String floatingIpId = "floatingIpId";
		String floatingIpEndpoint = "http://endpoint";
		Mockito.doReturn(floatingIpEndpoint).when(this.openStackPublicIpPlugin).getFloatingIpEndpoint();
		String endpointExcepted = String.format("%s/%s", floatingIpEndpoint, floatingIpId);		
		
		// exercise
		this.openStackPublicIpPlugin.releasePublicIp(floatingIpId, openStackV3Token);
		
		// verify
		Mockito.verify(this.httpClient, Mockito.times(1))
				.doDeleteRequest(Mockito.eq(endpointExcepted), Mockito.eq(this.openStackV3Token));
	}
	
	// test case: the cloud throws http exception
	@Test(expected=InstanceNotFoundException.class)
	public void testReleasePublicIpa()
			throws Exception {
		// set up
		String floatingIpId = "floatingIpId";
		String floatingIpEndpoint = "http://endpoint";
		Mockito.doReturn(floatingIpEndpoint).when(this.openStackPublicIpPlugin).getFloatingIpEndpoint();

		HttpResponseException notFoundException = new HttpResponseException(HttpStatus.SC_NOT_FOUND, "");
		Mockito.doThrow(notFoundException).when(
				this.httpClient).doDeleteRequest(Mockito.anyString(), Mockito.any(Token.class));
		
		// exercise
		this.openStackPublicIpPlugin.releasePublicIp(floatingIpId, openStackV3Token);
	}	
	
	@Ignore
	@Test
	public void testCheckProperties() {
		// TODO implement
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
	
	private String createFloatingIpRequestBody(String floatingNetworkId, String projectId, String portId) {
		CreateFloatingIpRequest createBody = new CreateFloatingIpRequest.Builder()
				.floatingNetworkId(floatingNetworkId)
				.projectId(projectId)
				.portId(portId)
				.build();
		
		return createBody.toJson();
	}
	
}
