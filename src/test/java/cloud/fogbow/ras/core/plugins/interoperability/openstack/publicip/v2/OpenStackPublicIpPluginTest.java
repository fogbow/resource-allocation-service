package cloud.fogbow.ras.core.plugins.interoperability.openstack.publicip.v2;

import cloud.fogbow.common.constants.OpenStackConstants;
import cloud.fogbow.common.exceptions.FatalErrorException;
import cloud.fogbow.common.exceptions.FogbowException;
import cloud.fogbow.common.exceptions.InstanceNotFoundException;
import cloud.fogbow.common.exceptions.InvalidParameterException;
import cloud.fogbow.common.models.CloudToken;
import cloud.fogbow.common.util.HomeDir;
import cloud.fogbow.ras.constants.SystemConstants;
import cloud.fogbow.ras.core.models.instances.InstanceState;
import cloud.fogbow.ras.core.models.instances.PublicIpInstance;
import cloud.fogbow.ras.core.models.orders.PublicIpOrder;
import cloud.fogbow.ras.core.plugins.interoperability.openstack.OpenStackStateMapper;
import cloud.fogbow.ras.core.plugins.interoperability.openstack.OpenStackV3Token;
import cloud.fogbow.ras.core.plugins.interoperability.openstack.network.v2.CreateSecurityGroupResponse;
import cloud.fogbow.ras.util.connectivity.AuditableHttpRequestClient;
import com.google.gson.Gson;
import org.apache.http.HttpStatus;
import org.apache.http.client.HttpResponseException;
import org.apache.http.client.utils.URIBuilder;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;

import java.io.File;
import java.net.URISyntaxException;
import java.util.*;

public class OpenStackPublicIpPluginTest {

	private OpenStackPublicIpPlugin openStackPublicIpPlugin;
	private AuditableHttpRequestClient httpClient;
	private OpenStackV3Token openStackV3Token;
	
    private static final String FAKE_TOKEN_PROVIDER = "fake-token-provider";
    private static final String FAKE_TOKEN_VALUE = "fake-token-value";
    private static final String FAKE_USER_ID = "fake-user-id";
    private static final String FAKE_NAME = "fake-name";
    private static final String FAKE_PROJECT_ID = "fake-project-id";

	@Before
	public void setUp() {
        this.openStackV3Token = new OpenStackV3Token(FAKE_TOKEN_PROVIDER, FAKE_USER_ID, FAKE_TOKEN_VALUE, FAKE_PROJECT_ID);
        this.httpClient = Mockito.mock(AuditableHttpRequestClient.class);
        
        boolean notCheckProperties = false;
		String cloudConfPath = HomeDir.getPath() + SystemConstants.CLOUDS_CONFIGURATION_DIRECTORY_NAME + File.separator
				+ "default" + File.separator + SystemConstants.CLOUD_SPECIFICITY_CONF_FILE_NAME;
		this.openStackPublicIpPlugin = Mockito.spy(new OpenStackPublicIpPlugin(cloudConfPath, notCheckProperties));
        
        this.openStackPublicIpPlugin.setClient(this.httpClient);
	}
	
	// test case: success case
	@Test
	public void testRequestInstanceTest() throws Exception {
		// set up
		String computeInstanceId = "computeInstanceId";
		PublicIpOrder publicIpOrder = new PublicIpOrder();
		String externalNetworkId = "externalNetId";
		String floatingIpEndpoint = "http://floatingIpEndpoint";
		String portId = "portId";
		String createFloatingIpRequestBody = createFloatingIpRequestBody(externalNetworkId, FAKE_PROJECT_ID, portId);
		String floatingIpId = "floatingIpId";
		String responseCreateFloatingIp = getCreateFloatingIpResponseJson(floatingIpId);
		String responseCreateSecurityGroup = getCreateSecurityGroupResponseJson("securityGroupId");

		Mockito.doReturn(portId).when(this.openStackPublicIpPlugin).getNetworkPortIp(
				Mockito.eq(computeInstanceId), Mockito.eq(this.openStackV3Token));
		Mockito.when(this.openStackPublicIpPlugin.getExternalNetworkId()).thenReturn(externalNetworkId);
		Mockito.when(this.openStackPublicIpPlugin.getFloatingIpEndpoint()).thenReturn(floatingIpEndpoint);

		Mockito.when(this.httpClient.doPostRequest(
				Mockito.endsWith("security-groups"), Mockito.any(OpenStackV3Token.class), Mockito.anyString()))
				.thenReturn(responseCreateSecurityGroup);

		Mockito.when(this.httpClient.doPostRequest(
				Mockito.eq(floatingIpEndpoint), Mockito.any(OpenStackV3Token.class), Mockito.anyString()))
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
	@Test(expected= FogbowException.class)
	public void testRequestInstanceErrorOnGetPortTest() throws HttpResponseException, URISyntaxException, FogbowException {
		// set up
		String computeInstanceId = "computeInstanceId";
		PublicIpOrder publicIpOrder = new PublicIpOrder();
		String responseCreateSecurityGroup = getCreateSecurityGroupResponseJson("securityGroupId");

		Mockito.when(this.httpClient.doPostRequest(
				Mockito.endsWith("security-groups"), Mockito.any(OpenStackV3Token.class), Mockito.anyString()))
				.thenReturn(responseCreateSecurityGroup);

		Mockito.doThrow(new FogbowException()).when(this.openStackPublicIpPlugin).getNetworkPortIp(
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
			throws HttpResponseException, URISyntaxException, FogbowException {
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
	@Test(expected= InstanceNotFoundException.class)
	public void testGetNetworkPortIpErrorWhenRequest()
			throws HttpResponseException, URISyntaxException, FogbowException {
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
			throws HttpResponseException, URISyntaxException, FogbowException {
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
				.doGetRequest(Mockito.anyString(), Mockito.any(CloudToken.class));
	}	
	
	// test case: throw FogbowException because the cloud found two or more ports. In the Fogbow scenario is not allowed
	@Test
	public void testGetNetworkPortIpPortsSizeIrregular()
			throws HttpResponseException, URISyntaxException, FogbowException {
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
		} catch (FogbowException e) {}
		
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

		ListSecurityGroups listSecurityGroups = createListSecurityGroupsResponse();

		// when retrieving the group id by name
		Mockito.when(this.httpClient.doGetRequest(Mockito.anyString(), Mockito.any(CloudToken.class)))
				.thenReturn(listSecurityGroups.toJson());

		// exercise
		this.openStackPublicIpPlugin.deleteInstance(floatingIpId, null, openStackV3Token);
		
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

		ListSecurityGroups listSecurityGroups = createListSecurityGroupsResponse();

		// when retrieving the group id by name
		Mockito.when(this.httpClient.doGetRequest(Mockito.anyString(), Mockito.any(CloudToken.class)))
				.thenReturn(listSecurityGroups.toJson());

		HttpResponseException notFoundException = new HttpResponseException(HttpStatus.SC_NOT_FOUND, "");
		Mockito.doThrow(notFoundException).when(
				this.httpClient).doDeleteRequest(Mockito.anyString(), Mockito.any(CloudToken.class));
		
		// exercise
		this.openStackPublicIpPlugin.deleteInstance(floatingIpId, null, openStackV3Token);
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
		String neutroApiEndpoint = this.openStackPublicIpPlugin.getNeutronApiEndpoint();

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
				.getNeutronApiEndpoint();
	}
	// test case: without default network in properties

	@Test
	public void testCheckPropertiesWithoutDefaultNetworkProperties() {
		try {
			// exercise
			this.openStackPublicIpPlugin.setProperties(new Properties());
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
					.getNeutronApiEndpoint();
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
					.getNeutronApiEndpoint();
		} catch (Exception e) {
			Assert.fail();
		}
	}
	// test case: success case with active status

	@Test
	public void testGetInstanceActive() throws HttpResponseException, FogbowException {
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
	public void testGetInstanceUnavailable() throws HttpResponseException, FogbowException {
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
	// test case: throws FogbowException

	@Test
	public void testGetInstanceThrowFogbowException() throws HttpResponseException, FogbowException {
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
    		idJsonKey.put(OpenStackConstants.PublicIp.ID_KEY_JSON, portsId[i]);
    		idsJsonKey.add(idJsonKey);
		}

        Map<String, Object> portsJsonKey = new HashMap<String, Object>();
        portsJsonKey.put(OpenStackConstants.PublicIp.PORTS_KEY_JSON, idsJsonKey);

        Gson gson = new Gson();
        return gson.toJson(portsJsonKey);
	}

	private String getCreateFloatingIpResponseJson(String floatingIpId) {
    	Map<String, Object> idJsonKey = new HashMap<String, Object>();
    	idJsonKey.put(OpenStackConstants.PublicIp.ID_KEY_JSON, floatingIpId);
        Map<String, Object> floatingipJsonKey = new HashMap<String, Object>();
        floatingipJsonKey.put(OpenStackConstants.PublicIp.FLOATING_IP_KEY_JSON, idJsonKey);

        Gson gson = new Gson();
        return gson.toJson(floatingipJsonKey);
    }

	private String getGetFloatingIpResponseJson(String floatingIpId,
			String floatingIpAddress, String status) {
    	Map<String, Object> idJsonKey = new HashMap<String, Object>();
    	idJsonKey.put(OpenStackConstants.PublicIp.ID_KEY_JSON, floatingIpId);
    	idJsonKey.put(OpenStackConstants.PublicIp.FLOATING_IP_ADDRESS_KEY_JSON, floatingIpAddress);
    	idJsonKey.put(OpenStackConstants.PublicIp.STATUS_KEY_JSON, status);
        Map<String, Object> floatingipJsonKey = new HashMap<String, Object>();
        floatingipJsonKey.put(OpenStackConstants.PublicIp.FLOATING_IP_KEY_JSON, idJsonKey);

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

	public String getCreateSecurityGroupResponseJson(String id) {
		CreateSecurityGroupResponse.SecurityGroup securityGroup = new CreateSecurityGroupResponse.SecurityGroup(id);
		return new CreateSecurityGroupResponse(securityGroup).toJson();
	}

	private ListSecurityGroups createListSecurityGroupsResponse() {
		ListSecurityGroups.SecurityGroup securityGroup = new ListSecurityGroups.SecurityGroup();
		securityGroup.setId("fake-sg-id");
		List<ListSecurityGroups.SecurityGroup> securityGroups = new ArrayList<>();
		securityGroups.add(securityGroup);
		ListSecurityGroups listSecurityGroups = new ListSecurityGroups();
		listSecurityGroups.setSecurityGroups(securityGroups);
		return listSecurityGroups;
	}

}
