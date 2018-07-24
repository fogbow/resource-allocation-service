package org.fogbowcloud.manager.core.plugins.cloud.openstack;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Arrays;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;

import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.HttpStatus;
import org.apache.http.ProtocolVersion;
import org.apache.http.StatusLine;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpUriRequest;
import org.apache.http.message.BasicStatusLine;
import org.fogbowcloud.manager.core.HomeDir;
import org.fogbowcloud.manager.core.PropertiesHolder;
import org.fogbowcloud.manager.core.exceptions.FogbowManagerException;
import org.fogbowcloud.manager.core.exceptions.UnauthorizedRequestException;
import org.fogbowcloud.manager.core.exceptions.UnexpectedException;
import org.fogbowcloud.manager.core.models.instances.InstanceState;
import org.fogbowcloud.manager.core.models.instances.NetworkInstance;
import org.fogbowcloud.manager.core.models.orders.NetworkAllocationMode;
import org.fogbowcloud.manager.core.models.orders.NetworkOrder;
import org.fogbowcloud.manager.core.models.tokens.FederationUser;
import org.fogbowcloud.manager.core.models.tokens.Token;
import org.fogbowcloud.manager.util.connectivity.HttpRequestClientUtil;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.junit.After;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;

import static org.junit.Assert.*;
import static org.mockito.Mockito.*;

public class OpenStackV2NetworkPluginTest {

	private static final String NETWORK_NEUTRONV2_URL_KEY = "openstack_neutron_v2_url";

	private static final String UTF_8 = "UTF-8";
	private static final String DEFAULT_GATEWAY_INFO = "000000-gateway_info";
	private static final String DEFAULT_TENANT_ID = "tenantId";
	private static final String DEFAULT_NETWORK_URL = "http://localhost:0000";

	private OpenStackV2NetworkPlugin openStackV2NetworkPlugin;
	private Token defaultToken;
	private HttpClient client;
	private Properties properties;
	private HttpRequestClientUtil httpRequestClientUtil;
	
	@Before
	public void setUp() {
	    HomeDir.getInstance().setPath("src/test/resources/private");
        PropertiesHolder propertiesHolder = PropertiesHolder.getInstance();
        this.properties = propertiesHolder.getProperties();
        this.properties.put(OpenStackV2NetworkPlugin.KEY_EXTERNAL_GATEWAY_INFO, DEFAULT_GATEWAY_INFO);
        this.properties.put(NETWORK_NEUTRONV2_URL_KEY, DEFAULT_NETWORK_URL);
		this.openStackV2NetworkPlugin = spy(new OpenStackV2NetworkPlugin());

		this.client = mock(HttpClient.class);
		this.httpRequestClientUtil = new HttpRequestClientUtil(this.client);
		this.openStackV2NetworkPlugin.setClient(this.httpRequestClientUtil);

		Map<String, String> attributes = new HashMap<String, String>();
		attributes.put(OpenStackV2NetworkPlugin.TENANT_ID, DEFAULT_TENANT_ID);
		this.defaultToken = new Token("accessId", new Token.User("user", "user"), new Date(), attributes);
	}

	@After
	public void validate() {
		validateMockitoUsage();
	}

	//requestInstance tests

	//test case: The http client must make only 5 requests
	@Test
	public void testNumberOfRequests() throws IOException, FogbowManagerException, UnexpectedException {
		//set up
		HttpResponse httpResponsePostNetwork = createHttpResponse("", HttpStatus.SC_OK);
		HttpResponse httpResponsePostSubnet = createHttpResponse("", HttpStatus.SC_OK);
		HttpResponse httpResponsePostSecurityGroup = createHttpResponse("", HttpStatus.SC_OK);
		HttpResponse httpResponsePostSshRule = createHttpResponse("", HttpStatus.SC_OK);
		HttpResponse httpResponsePostIcmpRule = createHttpResponse("", HttpStatus.SC_OK);
		when(this.client.execute(any(HttpUriRequest.class))).thenReturn(httpResponsePostNetwork,
				httpResponsePostSubnet, httpResponsePostSecurityGroup, httpResponsePostSshRule, httpResponsePostIcmpRule);
		doReturn("fake-sg-id").when(this.openStackV2NetworkPlugin).getSecurityGroupIdFromJson(anyString());
		doReturn(null).when(this.openStackV2NetworkPlugin).getNetworkIdFromJson(anyString());
		NetworkOrder order = createEmptyOrder();

		//exercise
		this.openStackV2NetworkPlugin.requestInstance(order, this.defaultToken);

		//verify
		verify(this.client, times(5)).execute(any(HttpUriRequest.class));
	}


	//test case: Tests if an exception will be thrown in case that openstack raise an error in network request.
	@Test
	public void testRequestInstancePostNetworkError() throws IOException, UnexpectedException {
		//set up
		HttpResponse httpResponsePostNetwork = createHttpResponse("", HttpStatus.SC_BAD_REQUEST);
		when(this.client.execute(any(HttpUriRequest.class))).thenReturn(httpResponsePostNetwork);
		NetworkOrder order = createEmptyOrder();

		//exercise
		try {
			this.openStackV2NetworkPlugin.requestInstance(order, this.defaultToken);
			fail();
		} catch (FogbowManagerException e) {
			// Throws an exception, as expected
		} catch (Exception e) {
			fail();
		}

		//verify
		verify(this.client, times(1)).execute(any(HttpUriRequest.class));
	}

	//test case: Tests if an exception will be thrown in case that openstack raise an error when requesting for a new subnet.
	@Test(expected = UnexpectedException.class)
	public void testRequestInstancePostSubnetError() throws IOException, FogbowManagerException, UnexpectedException {
		//set up
		HttpResponse httpResponsePostNetwork = createHttpResponse("", HttpStatus.SC_OK);
		HttpResponse httpResponsePostSubnet = createHttpResponse("", HttpStatus.SC_BAD_REQUEST);
		HttpResponse httpResponseRemoveNetwork = createHttpResponse("", HttpStatus.SC_OK);
		when(this.client.execute(any(HttpUriRequest.class))).thenReturn(httpResponsePostNetwork,
				httpResponsePostSubnet, httpResponseRemoveNetwork);
		NetworkOrder order = createEmptyOrder();

		//exercise
		this.openStackV2NetworkPlugin.requestInstance(order, this.defaultToken);

		//verify
		verify(this.client, times(3)).execute(any(HttpUriRequest.class));
	}

	//test case: Tests if the json to request network was generated as expected
	@Test
	public void testGenerateJsonEntityToCreateNetwork() throws JSONException {
		//exercise
		JSONObject generateJsonEntityToCreateNetwork = this.openStackV2NetworkPlugin
				.generateJsonEntityToCreateNetwork(DEFAULT_TENANT_ID);

		//verify
		JSONObject networkJsonObject = generateJsonEntityToCreateNetwork
				.optJSONObject(OpenStackV2NetworkPlugin.KEY_JSON_NETWORK);
		assertEquals(DEFAULT_TENANT_ID, networkJsonObject.optString(OpenStackV2NetworkPlugin.KEY_TENANT_ID));
		assertTrue(networkJsonObject.optString(OpenStackV2NetworkPlugin.KEY_NAME)
				.contains(OpenStackV2NetworkPlugin.DEFAULT_NETWORK_NAME));
	}

	//test case: Tests if the dns list will be returned as expected
	@Test
	public void testSetDnsList() {
		//set up
		String dnsOne = "one";
		String dnsTwo = "Two";
		this.properties.put(OpenStackV2NetworkPlugin.KEY_DNS_NAMESERVERS, dnsOne + "," + dnsTwo);

		//exercise
		this.openStackV2NetworkPlugin.setDNSList(this.properties);

		//verify
		assertEquals(2, this.openStackV2NetworkPlugin.getDnsList().length);
		assertEquals(dnsOne, this.openStackV2NetworkPlugin.getDnsList()[0]);
		assertEquals(dnsTwo, this.openStackV2NetworkPlugin.getDnsList()[1]);
	}

	//test case: Tests if the json to request subnet was generated as expected
	@Test
	public void testGenerateJsonEntityToCreateSubnet() throws JSONException {
		//set up
		String dnsOne = "one";
		String dnsTwo = "Two";
		this.properties.put(OpenStackV2NetworkPlugin.KEY_DNS_NAMESERVERS, dnsOne + "," + dnsTwo);
		this.openStackV2NetworkPlugin.setDNSList(this.properties);
		String networkId = "networkId";
		String address = "10.10.10.0/24";
		String gateway = "10.10.10.11";
		NetworkOrder order = createNetworkOrder(networkId, address, gateway, NetworkAllocationMode.DYNAMIC);

		//exercise
		JSONObject generateJsonEntityToCreateSubnet = this.openStackV2NetworkPlugin
				.generateJsonEntityToCreateSubnet(order.getId(), DEFAULT_TENANT_ID, order);

		//verify
		JSONObject subnetJsonObject = generateJsonEntityToCreateSubnet
				.optJSONObject(OpenStackV2NetworkPlugin.KEY_JSON_SUBNET);
		assertEquals(DEFAULT_TENANT_ID, subnetJsonObject.optString(OpenStackV2NetworkPlugin.KEY_TENANT_ID));
		assertTrue(subnetJsonObject.optString(OpenStackV2NetworkPlugin.KEY_NAME)
				.contains(OpenStackV2NetworkPlugin.DEFAULT_SUBNET_NAME));
		assertEquals(order.getId(), subnetJsonObject.optString(OpenStackV2NetworkPlugin.KEY_NETWORK_ID));
		assertEquals(order.getAddress(), subnetJsonObject.optString(OpenStackV2NetworkPlugin.KEY_CIDR));
		assertEquals(order.getGateway(), subnetJsonObject.optString(OpenStackV2NetworkPlugin.KEY_GATEWAY_IP));
		assertEquals(true, subnetJsonObject.optBoolean(OpenStackV2NetworkPlugin.KEY_ENABLE_DHCP));
		assertEquals(OpenStackV2NetworkPlugin.DEFAULT_IP_VERSION,
				subnetJsonObject.optString(OpenStackV2NetworkPlugin.KEY_IP_VERSION));
		assertEquals(dnsOne, subnetJsonObject.optJSONArray(OpenStackV2NetworkPlugin.KEY_DNS_NAMESERVERS).get(0));
		assertEquals(dnsTwo, subnetJsonObject.optJSONArray(OpenStackV2NetworkPlugin.KEY_DNS_NAMESERVERS).get(1));
	}

	//test case: Tests if the json to request subnet was generated as expected, when address is not provided.
	@Test
	public void testGenerateJsonEntityToCreateSubnetDefaultAddress() throws JSONException {
		//set up
		String networkId = "networkId";
		NetworkOrder order = createNetworkOrder(networkId, null, null, null);

		//exercise
		JSONObject generateJsonEntityToCreateSubnet = this.openStackV2NetworkPlugin
				.generateJsonEntityToCreateSubnet(order.getId(), DEFAULT_TENANT_ID, order);

		//verify
		JSONObject subnetJsonObject = generateJsonEntityToCreateSubnet
				.optJSONObject(OpenStackV2NetworkPlugin.KEY_JSON_SUBNET);
		assertEquals(OpenStackV2NetworkPlugin.DEFAULT_NETWORK_ADDRESS,
				subnetJsonObject.optString(OpenStackV2NetworkPlugin.KEY_CIDR));
	}

	//test case: Tests if the json to request subnet was generated as expected, when dns is not provided.
	@Test
	public void testGenerateJsonEntityToCreateSubnetDefaultDns() throws JSONException {
		//set up
		String networkId = "networkId";
		NetworkOrder order = createNetworkOrder(networkId, null, null, null);

		//exercise
		JSONObject generateJsonEntityToCreateSubnet = this.openStackV2NetworkPlugin
				.generateJsonEntityToCreateSubnet(order.getId(), DEFAULT_TENANT_ID, order);

		//verify
		JSONObject subnetJsonObject = generateJsonEntityToCreateSubnet
				.optJSONObject(OpenStackV2NetworkPlugin.KEY_JSON_SUBNET);
		assertEquals(OpenStackV2NetworkPlugin.DEFAULT_DNS_NAME_SERVERS[0],
				subnetJsonObject.optJSONArray(OpenStackV2NetworkPlugin.KEY_DNS_NAMESERVERS).get(0));
		assertEquals(OpenStackV2NetworkPlugin.DEFAULT_DNS_NAME_SERVERS[1],
				subnetJsonObject.optJSONArray(OpenStackV2NetworkPlugin.KEY_DNS_NAMESERVERS).get(1));
	}

	//test case: Tests if the json to request subnet was generated as expected, when a static allocation is required.
	@Test
	public void testGenerateJsonEntityToCreateSubnetStaticAllocation() throws JSONException {
		//set up
		String networkId = "networkId";
		NetworkOrder order = createNetworkOrder(networkId, null, null, NetworkAllocationMode.STATIC);

		//exercise
		JSONObject generateJsonEntityToCreateSubnet = this.openStackV2NetworkPlugin
				.generateJsonEntityToCreateSubnet(order.getId(), DEFAULT_TENANT_ID, order);

		//verify
		JSONObject subnetJsonObject = generateJsonEntityToCreateSubnet
				.optJSONObject(OpenStackV2NetworkPlugin.KEY_JSON_SUBNET);
		assertEquals(false, subnetJsonObject.optBoolean(OpenStackV2NetworkPlugin.KEY_ENABLE_DHCP));
	}

	//test case: Tests if the json to request subnet was generated as expected, when gateway is not provided.
	@Test
	public void testGenerateJsonEntityToCreateSubnetWithoutGateway() throws JSONException {
		//set up
		String networkId = "networkId";
		NetworkOrder order = createNetworkOrder(networkId, null, null, null);

		//exercise
		JSONObject generateJsonEntityToCreateSubnet = this.openStackV2NetworkPlugin
				.generateJsonEntityToCreateSubnet(order.getId(), DEFAULT_TENANT_ID, order);

		//verify
		JSONObject subnetJsonObject = generateJsonEntityToCreateSubnet
				.optJSONObject(OpenStackV2NetworkPlugin.KEY_JSON_SUBNET);
		assertTrue(subnetJsonObject.optString(OpenStackV2NetworkPlugin.KEY_GATEWAY_IP).isEmpty());
	}

	//getInstance tests

	//test case: Tests get networkId from json response
	@Test
	public void testGetNetworkIdFromJson() throws JSONException, UnexpectedException {
		//set up
		String networkId = "networkId00";
		JSONObject networkContentJsonObject = new JSONObject();
		networkContentJsonObject.put(OpenStackV2NetworkPlugin.KEY_ID, networkId);
		JSONObject networkJsonObject = new JSONObject();

		//exercise
		networkJsonObject.put(OpenStackV2NetworkPlugin.KEY_JSON_NETWORK, networkContentJsonObject);

		//verify
		assertEquals(networkId,
				this.openStackV2NetworkPlugin.getNetworkIdFromJson(networkJsonObject.toString()));
	}

	//test case: Tests get instance from json response
	@Test
	public void testGetInstanceFromJson() throws JSONException, IOException, FogbowManagerException, UnexpectedException {
		//set up
		String networkId = "networkId00";
		String networkName = "netName";
		String subnetId = "subnetId00";
		String vlan = "vlan00";
		String gatewayIp = "10.10.10.10";
		String cidr = "10.10.10.0/24";
		// Generating network response string
		JSONObject networkContentJsonObject = generateJsonResponseForNetwork(networkId, networkName, subnetId, vlan);
		JSONObject networkJsonObject = new JSONObject();
		networkJsonObject.put(OpenStackV2NetworkPlugin.KEY_JSON_NETWORK, networkContentJsonObject);

		// Generating subnet response string
		JSONObject subnetContentJsonObject = generateJsonResponseForSubnet(gatewayIp, cidr);
		JSONObject subnetJsonObject = new JSONObject();
		subnetJsonObject.put(OpenStackV2NetworkPlugin.KEY_JSON_SUBNET, subnetContentJsonObject);

		HttpResponse httpResponseGetNetwork = createHttpResponse(networkJsonObject.toString(), HttpStatus.SC_OK);
		HttpResponse httpResponseGetSubnet = createHttpResponse(subnetJsonObject.toString(), HttpStatus.SC_OK);
		when(this.client.execute(any(HttpUriRequest.class))).thenReturn(httpResponseGetNetwork,
				httpResponseGetSubnet);

		//exercise
		NetworkInstance instance = this.openStackV2NetworkPlugin.getInstance("instanceId00", this.defaultToken);

		//verify
		assertEquals(networkId, instance.getId());
		assertEquals(networkName, instance.getLabel());
		assertEquals(vlan, instance.getvLAN());
		assertEquals(InstanceState.READY, instance.getState());
		assertEquals(gatewayIp, instance.getGateway());
		assertEquals(cidr, instance.getAddress());
		assertEquals(NetworkAllocationMode.DYNAMIC, instance.getAllocation());
	}


	//deleteInstance tests


	//test case: Tests remove instance, it must execute a http client exactly 3 times
	@Test
	public void testRemoveInstance() throws IOException, JSONException, FogbowManagerException, UnexpectedException {
		//set up
		String networkId = "networkId";
		String securityGroupId = "fake-sg-id";
		JSONObject securityGroupResponse = createSecurityGroupGetResponse(securityGroupId);

		HttpResponse httpResponseDeleteNetwork = createHttpResponse("", HttpStatus.SC_OK);
		HttpResponse httpResponseGetSecurityGroupId = createHttpResponse(securityGroupResponse.toString(), HttpStatus.SC_OK);
		HttpResponse httpResponseSecurityGroup = createHttpResponse("", HttpStatus.SC_OK);
		when(this.client.execute(any(HttpUriRequest.class))).thenReturn(httpResponseDeleteNetwork,
				httpResponseGetSecurityGroupId, httpResponseSecurityGroup);

		//exercise
		this.openStackV2NetworkPlugin.deleteInstance(networkId, this.defaultToken);

		//verify
		verify(this.client, times(3)).execute(any(HttpUriRequest.class));
	}

	//test: Tests a delete in a network which has compute attached to it
	@Test
	public void testRemoveNetworkWithInstanceAssociated() throws JSONException, IOException, FogbowManagerException, UnexpectedException {
		//set up
		String networkId = "networkId";

		HttpResponse httpResponseDeleteRouter = createHttpResponse("", HttpStatus.SC_FORBIDDEN);
		when(this.client.execute(any(HttpUriRequest.class))).thenReturn(httpResponseDeleteRouter);

		//exercise
		try {
			this.openStackV2NetworkPlugin.deleteInstance(networkId, this.defaultToken);
			fail();
		} catch (UnauthorizedRequestException e) {
			// TODO: check error message
		} catch (Exception e) {
			fail();
		}

		// verify
		verify(this.client, times(1)).execute(any(HttpUriRequest.class));
		verify(this.openStackV2NetworkPlugin, never()).retrieveSecurityGroupId(anyString(), any(Token.class));
		verify(this.openStackV2NetworkPlugin, never()).removeSecurityGroup(any(Token.class), anyString());
	}

	//test case:
	@Ignore
	@Test
	public void testRemoveInstanceNullPointerException() throws JSONException, IOException {
		//set up
		JSONObject portOneJsonObject = new JSONObject();
		String networkId = "networkId";
		portOneJsonObject.put(OpenStackV2NetworkPlugin.KEY_NETWORK_ID, networkId);

		JSONObject portsJsonObject = new JSONObject();

		HttpResponse httpResponseGetPorts = createHttpResponse(portsJsonObject.toString(), HttpStatus.SC_OK);
		when(this.client.execute(any(HttpUriRequest.class))).thenReturn(httpResponseGetPorts);

		//exercise
		try {
			this.openStackV2NetworkPlugin.deleteInstance(networkId, this.defaultToken);
			fail();
		} catch (UnexpectedException e) {
			// TODO: check error message
		} catch (Exception e) {
			fail();
		}

		//verify
		verify(this.client, times(1)).execute(any(HttpUriRequest.class));
	}

	//test case:
	@Ignore
	@Test
	public void testRemoveInstanceRemoveNetworkBadRequestException() throws JSONException, IOException, UnexpectedException {
		JSONObject portOneJsonObject = new JSONObject();
		String networkId = "networkId";
		String securityGroupId = "fake-sg-id";
		JSONObject securityGroupResponse = createSecurityGroupGetResponse(securityGroupId);
		portOneJsonObject.put(OpenStackV2NetworkPlugin.KEY_NETWORK_ID, networkId);

		JSONArray subnetsjsonArray = new JSONArray();
		JSONObject subnetObject = new JSONObject();
		String subnetId = "subnetId";
		subnetsjsonArray.put(0, subnetObject);

		JSONArray portsArrayJsonObject = new JSONArray();
		portsArrayJsonObject.put(0, portOneJsonObject);

		JSONObject portsJsonObject = new JSONObject();

		HttpResponse httpResponseDeleteNetwork = createHttpResponse("", HttpStatus.SC_BAD_REQUEST);
		HttpResponse httpResponseGetSecurityGroup = createHttpResponse(securityGroupResponse.toString(), HttpStatus.SC_BAD_REQUEST);
		HttpResponse httpResponseDeleteSecurityGroup = createHttpResponse("", HttpStatus.SC_BAD_REQUEST);
		when(this.client.execute(any(HttpUriRequest.class))).thenReturn(httpResponseDeleteNetwork,
				httpResponseGetSecurityGroup, httpResponseDeleteSecurityGroup);

		try {
			this.openStackV2NetworkPlugin.deleteInstance(networkId, this.defaultToken);
			fail();
		} catch (FogbowManagerException e) {
			// TODO: check error message
		} catch (Exception e) {
			fail();
		}

		verify(client, times(4)).execute(any(HttpUriRequest.class));
	}

	private NetworkOrder createNetworkOrder(String networkId, String address, String gateway,
			NetworkAllocationMode allocation) {
		String requestingMember = "fake-requesting-member";
		String providingMember = "fake-providing-member";
		NetworkOrder order = new NetworkOrder(networkId, mock(FederationUser.class), requestingMember, providingMember,
				gateway, address, allocation);
		return order;
	}

	private NetworkOrder createEmptyOrder() {
		return new NetworkOrder(null, null, null, null, null, null);
	}

	private HttpResponse createHttpResponse(String content, int httpStatus) throws IOException {
		HttpResponse httpResponse = mock(HttpResponse.class);
		HttpEntity httpEntity = mock(HttpEntity.class);
		InputStream inputStrem = new ByteArrayInputStream(content.getBytes(UTF_8));
		when(httpEntity.getContent()).thenReturn(inputStrem);
		when(httpResponse.getEntity()).thenReturn(httpEntity);
		StatusLine statusLine = new BasicStatusLine(new ProtocolVersion("", 0, 0), httpStatus, "");
		when(httpResponse.getStatusLine()).thenReturn(statusLine);
		return httpResponse;
	}

	private JSONObject generateJsonResponseForSubnet(String gatewayIp, String cidr) {
		JSONObject subnetContentJsonObject = new JSONObject();

		subnetContentJsonObject.put(OpenStackV2NetworkPlugin.KEY_GATEWAY_IP, gatewayIp);
		subnetContentJsonObject.put(OpenStackV2NetworkPlugin.KEY_ENABLE_DHCP, true);
		subnetContentJsonObject.put(OpenStackV2NetworkPlugin.KEY_CIDR, cidr);

		return subnetContentJsonObject;
	}

	private JSONObject generateJsonResponseForNetwork(String networkId, String networkName, String subnetId, String vlan) {
		JSONObject networkContentJsonObject = new JSONObject();

		networkContentJsonObject.put(OpenStackV2NetworkPlugin.KEY_ID, networkId);
		networkContentJsonObject.put(OpenStackV2NetworkPlugin.KEY_PROVIDER_SEGMENTATION_ID, vlan);
		networkContentJsonObject.put(OpenStackV2NetworkPlugin.KEY_STATUS,
				OpenStackStateMapper.ACTIVE_STATUS);
		networkContentJsonObject.put(OpenStackV2NetworkPlugin.KEY_NAME, networkName);
		JSONArray subnetJsonArray = new JSONArray(Arrays.asList(new String[] { subnetId }));
		networkContentJsonObject.put(OpenStackV2NetworkPlugin.KEY_SUBNETS, subnetJsonArray);

		return networkContentJsonObject;
	}

	private JSONObject createSecurityGroupGetResponse(String id) {
		JSONObject jsonObject = new JSONObject();
		JSONArray securityGroups = new JSONArray();
		JSONObject securityGroupInfo = new JSONObject();
		securityGroupInfo.put(OpenStackV2NetworkPlugin.KEY_TENANT_ID, "fake-project-id");
		securityGroupInfo.put(OpenStackV2NetworkPlugin.QUERY_NAME, "fake-name");
		securityGroupInfo.put(OpenStackV2NetworkPlugin.KEY_ID, id);
		securityGroups.put(securityGroupInfo);
		jsonObject.put(OpenStackV2NetworkPlugin.KEY_SECURITY_GROUPS, securityGroups);
		return jsonObject;
	}
}
