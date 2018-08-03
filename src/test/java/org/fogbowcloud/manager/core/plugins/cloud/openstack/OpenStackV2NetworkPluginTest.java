package org.fogbowcloud.manager.core.plugins.cloud.openstack;

import java.io.ByteArrayInputStream;
import java.io.File;
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
import org.apache.http.client.HttpResponseException;
import org.apache.http.client.methods.HttpUriRequest;
import org.apache.http.message.BasicStatusLine;
import org.fogbowcloud.manager.core.HomeDir;
import org.fogbowcloud.manager.core.PropertiesHolder;
import org.fogbowcloud.manager.core.exceptions.FogbowManagerException;
import org.fogbowcloud.manager.core.exceptions.UnexpectedException;
import org.fogbowcloud.manager.core.models.instances.InstanceState;
import org.fogbowcloud.manager.core.models.instances.NetworkInstance;
import org.fogbowcloud.manager.core.models.orders.NetworkAllocationMode;
import org.fogbowcloud.manager.core.models.orders.NetworkOrder;
import org.fogbowcloud.manager.core.models.tokens.FederationUser;
import org.fogbowcloud.manager.core.models.tokens.Token;
import org.fogbowcloud.manager.core.plugins.serialization.openstack.network.v2.CreateResponse;
import org.fogbowcloud.manager.core.plugins.serialization.openstack.network.v2.CreateSecurityGroupResponse;
import org.fogbowcloud.manager.util.connectivity.HttpRequestClientUtil;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;

public class OpenStackV2NetworkPluginTest {

	private static final String NETWORK_NEUTRONV2_URL_KEY = "openstack_neutron_v2_url";

	private static final String UTF_8 = "UTF-8";
	private static final String DEFAULT_GATEWAY_INFO = "000000-gateway_info";
	private static final String DEFAULT_TENANT_ID = "tenantId";
	private static final String DEFAULT_NETWORK_URL = "http://localhost:0000";
	private static final String SECURITY_GROUP_ID = "fake-sg-id";
	private static final String NETWORK_ID = "networkId";

	private static final String SUFFIX_ENDPOINT_DELETE_NETWORK = OpenStackV2NetworkPlugin.SUFFIX_ENDPOINT_NETWORK +
			File.separator + NETWORK_ID;

	private static final String SUFFIX_ENDPOINT_DELETE_SECURITY_GROUP = OpenStackV2NetworkPlugin.SUFFIX_ENDPOINT_SECURITY_GROUP +
			File.separator + SECURITY_GROUP_ID;
	private static final String SUFFIX_ENDPOINT_GET_SECURITY_GROUP = OpenStackV2NetworkPlugin.SUFFIX_ENDPOINT_SECURITY_GROUP + "?" +
			OpenStackV2NetworkPlugin.QUERY_NAME + "=" + OpenStackV2NetworkPlugin.SECURITY_GROUP_PREFIX + "-"
			+ NETWORK_ID;

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
		this.openStackV2NetworkPlugin = Mockito.spy(new OpenStackV2NetworkPlugin());

		this.client = Mockito.mock(HttpClient.class);
		this.httpRequestClientUtil = Mockito.spy(new HttpRequestClientUtil(this.client));
		this.openStackV2NetworkPlugin.setClient(this.httpRequestClientUtil);

		Map<String, String> attributes = new HashMap<String, String>();
		attributes.put(OpenStackV2NetworkPlugin.TENANT_ID, DEFAULT_TENANT_ID);
		this.defaultToken = new Token("accessId", new Token.User("user", "user"), new Date(), attributes);
	}

	@After
	public void validate() {
		Mockito.validateMockitoUsage();
	}

	//requestInstance tests

	//test case: The http client must make only 5 requests
	@Test
	public void testNumberOfRequestsInSucceededRequestInstance() throws IOException, FogbowManagerException, UnexpectedException {
		//set up
		// post network
		String createNetworkResponse = new CreateResponse(new CreateResponse.Network(NETWORK_ID)).toJson();
		Mockito.doReturn(createNetworkResponse).when(this.httpRequestClientUtil)
				.doPostRequest(Mockito.endsWith(OpenStackV2NetworkPlugin.SUFFIX_ENDPOINT_NETWORK),
						Mockito.eq(this.defaultToken), Mockito.any(JSONObject.class));
		//post subnet
		Mockito.doReturn("").when(this.httpRequestClientUtil)
				.doPostRequest(Mockito.endsWith(OpenStackV2NetworkPlugin.SUFFIX_ENDPOINT_SUBNET),
						Mockito.eq(this.defaultToken), Mockito.any(JSONObject.class));

		//post security group
		CreateSecurityGroupResponse.SecurityGroup securityGroup = new CreateSecurityGroupResponse.SecurityGroup(SECURITY_GROUP_ID);
		CreateSecurityGroupResponse createSecurityGroupResponse = new CreateSecurityGroupResponse(securityGroup);
		Mockito.doReturn(createSecurityGroupResponse.toJson()).when(this.httpRequestClientUtil)
				.doPostRequest(Mockito.endsWith(OpenStackV2NetworkPlugin.SUFFIX_ENDPOINT_SECURITY_GROUP),
						Mockito.eq(this.defaultToken), Mockito.any(JSONObject.class));
		//post ssh and icmp rule
		Mockito.doReturn("").when(this.httpRequestClientUtil)
				.doPostRequest(Mockito.endsWith(OpenStackV2NetworkPlugin.SUFFIX_ENDPOINT_SECURITY_GROUP_RULES),
						Mockito.eq(this.defaultToken), Mockito.any(JSONObject.class));

		Mockito.doReturn(null).when(this.openStackV2NetworkPlugin).getNetworkIdFromJson(Mockito.anyString());
		NetworkOrder order = createEmptyOrder();

		//exercise
		this.openStackV2NetworkPlugin.requestInstance(order, this.defaultToken);

		//verify
		Mockito.verify(this.httpRequestClientUtil, Mockito.times(1)).doPostRequest(
				Mockito.endsWith(OpenStackV2NetworkPlugin.SUFFIX_ENDPOINT_NETWORK), Mockito.eq(this.defaultToken),
				Mockito.any(JSONObject.class));
		Mockito.verify(this.httpRequestClientUtil, Mockito.times(1)).doPostRequest(
				Mockito.endsWith(OpenStackV2NetworkPlugin.SUFFIX_ENDPOINT_SUBNET), Mockito.eq(this.defaultToken),
				Mockito.any(JSONObject.class));
		Mockito.verify(this.httpRequestClientUtil, Mockito.times(1)).doPostRequest(
				Mockito.endsWith(OpenStackV2NetworkPlugin.SUFFIX_ENDPOINT_SECURITY_GROUP), Mockito.eq(this.defaultToken),
				Mockito.any(JSONObject.class));
		Mockito.verify(this.httpRequestClientUtil, Mockito.times(2)).doPostRequest(
				Mockito.endsWith(OpenStackV2NetworkPlugin.SUFFIX_ENDPOINT_SECURITY_GROUP_RULES), Mockito.eq(this.defaultToken),
				Mockito.any(JSONObject.class));
	}

	//test case: Tests if an exception will be thrown in case that openstack raise an error in network request.
	@Test
	public void testRequestInstancePostNetworkError() throws IOException, UnexpectedException {
		//set up
		HttpResponse httpResponsePostNetwork = createHttpResponse("", HttpStatus.SC_BAD_REQUEST);
		Mockito.when(this.client.execute(Mockito.any(HttpUriRequest.class))).thenReturn(httpResponsePostNetwork);
		NetworkOrder order = createEmptyOrder();

		//exercise
		try {
			this.openStackV2NetworkPlugin.requestInstance(order, this.defaultToken);
			Assert.fail();
		} catch (FogbowManagerException e) {
			// Throws an exception, as expected
		} catch (Exception e) {
			Assert.fail();
		}

		//verify
		Mockito.verify(this.client, Mockito.times(1)).execute(Mockito.any(HttpUriRequest.class));
	}

	//test case: Tests if an exception will be thrown in case that openstack raise an error when requesting for a new subnet.
	@Test
	public void testRequestInstancePostSubnetError() throws IOException, FogbowManagerException, UnexpectedException {
		//set up
		String createNetworkResponse = new CreateResponse(new CreateResponse.Network(NETWORK_ID)).toJson();
		HttpResponse httpResponsePostNetwork = createHttpResponse(createNetworkResponse, HttpStatus.SC_OK);
		HttpResponse httpResponsePostSubnet = createHttpResponse("", HttpStatus.SC_BAD_REQUEST);
		HttpResponse httpResponseRemoveNetwork = createHttpResponse("", HttpStatus.SC_OK);
		Mockito.when(this.client.execute(Mockito.any(HttpUriRequest.class))).thenReturn(httpResponsePostNetwork,
				httpResponsePostSubnet, httpResponseRemoveNetwork);
		NetworkOrder order = createEmptyOrder();

		try {
			//exercise
			this.openStackV2NetworkPlugin.requestInstance(order, this.defaultToken);
			Assert.fail();
		} catch (FogbowManagerException e) {

		}

		//verify
		Mockito.verify(this.client, Mockito.times(3)).execute(Mockito.any(HttpUriRequest.class));
	}

	//test case: Tests the case that security group raise an exception. This implies that network will be removed.
	@Test
	public void testErrorInPostSecurityGroup() throws IOException, FogbowManagerException, UnexpectedException {
		//set up
		//post network
		CreateResponse createResponse = new CreateResponse(new CreateResponse.Network(NETWORK_ID));
		Mockito.doReturn(createResponse.toJson()).when(this.httpRequestClientUtil)
				.doPostRequest(Mockito.endsWith(OpenStackV2NetworkPlugin.SUFFIX_ENDPOINT_NETWORK),
						Mockito.eq(this.defaultToken), Mockito.any(JSONObject.class));
		//post subnet
		Mockito.doReturn("").when(this.httpRequestClientUtil)
				.doPostRequest(Mockito.endsWith(OpenStackV2NetworkPlugin.SUFFIX_ENDPOINT_SUBNET),
						Mockito.eq(this.defaultToken), Mockito.any(JSONObject.class));
		//post security group
		Mockito.doThrow(new HttpResponseException(HttpStatus.SC_FORBIDDEN, "")).when(this.httpRequestClientUtil)
				.doPostRequest(Mockito.endsWith(OpenStackV2NetworkPlugin.SUFFIX_ENDPOINT_SECURITY_GROUP),
						Mockito.eq(this.defaultToken), Mockito.any(JSONObject.class));
		//remove network
		Mockito.doNothing().when(this.httpRequestClientUtil).doDeleteRequest(
				Mockito.endsWith(SUFFIX_ENDPOINT_DELETE_NETWORK), Mockito.eq(this.defaultToken));

		Mockito.doReturn(NETWORK_ID).when(this.openStackV2NetworkPlugin).getNetworkIdFromJson(Mockito.anyString());
		NetworkOrder order = createEmptyOrder();

		//exercise
		try {
			this.openStackV2NetworkPlugin.requestInstance(order, this.defaultToken);
		} catch (FogbowManagerException | UnexpectedException e) {
			//doNothing
		}

		//verify
		//request checks
		Mockito.verify(this.httpRequestClientUtil, Mockito.times(1)).doPostRequest(
				Mockito.endsWith(OpenStackV2NetworkPlugin.SUFFIX_ENDPOINT_NETWORK), Mockito.eq(this.defaultToken),
				Mockito.any(JSONObject.class));
		Mockito.verify(this.httpRequestClientUtil, Mockito.times(1)).doPostRequest(
				Mockito.endsWith(OpenStackV2NetworkPlugin.SUFFIX_ENDPOINT_SUBNET), Mockito.eq(this.defaultToken),
				Mockito.any(JSONObject.class));
		Mockito.verify(this.httpRequestClientUtil, Mockito.times(1)).doPostRequest(
				Mockito.endsWith(OpenStackV2NetworkPlugin.SUFFIX_ENDPOINT_SECURITY_GROUP), Mockito.eq(this.defaultToken),
				Mockito.any(JSONObject.class));
		Mockito.verify(this.httpRequestClientUtil, Mockito.never()).doPostRequest(
				Mockito.endsWith(OpenStackV2NetworkPlugin.SUFFIX_ENDPOINT_SECURITY_GROUP_RULES), Mockito.eq(this.defaultToken),
				Mockito.any(JSONObject.class));

		//remove checks
		Mockito.verify(this.httpRequestClientUtil, Mockito.times(1)).doDeleteRequest(
				Mockito.endsWith(SUFFIX_ENDPOINT_DELETE_NETWORK), Mockito.eq(this.defaultToken));
	}

	//test case: Tests the case that security group rules raise an exception. This implies that network
	// and security group will be removed.
	@Test
	public void testErrorInPostSecurityGroupRules() throws IOException, FogbowManagerException, UnexpectedException {
		//set up
		//post network
		CreateResponse createResponse = new CreateResponse(new CreateResponse.Network(NETWORK_ID));
		Mockito.doReturn(createResponse.toJson()).when(this.httpRequestClientUtil)
				.doPostRequest(Mockito.endsWith(OpenStackV2NetworkPlugin.SUFFIX_ENDPOINT_NETWORK),
						Mockito.eq(this.defaultToken), Mockito.any(JSONObject.class));

		//post subnet
		Mockito.doReturn("").when(this.httpRequestClientUtil)
				.doPostRequest(Mockito.endsWith(OpenStackV2NetworkPlugin.SUFFIX_ENDPOINT_SUBNET),
						Mockito.eq(this.defaultToken), Mockito.any(JSONObject.class));

		//post security group
		CreateSecurityGroupResponse.SecurityGroup securityGroup = new CreateSecurityGroupResponse.SecurityGroup(SECURITY_GROUP_ID);
		CreateSecurityGroupResponse createSecurityGroupResponse = new CreateSecurityGroupResponse(securityGroup);
		Mockito.doReturn(createSecurityGroupResponse.toJson()).when(this.httpRequestClientUtil)
				.doPostRequest(Mockito.endsWith(OpenStackV2NetworkPlugin.SUFFIX_ENDPOINT_SECURITY_GROUP),
						Mockito.eq(this.defaultToken), Mockito.any(JSONObject.class));

		//error in post security group rules
		Mockito.doThrow(new HttpResponseException(HttpStatus.SC_FORBIDDEN, "")).when(this.httpRequestClientUtil)
				.doPostRequest(Mockito.endsWith(OpenStackV2NetworkPlugin.SUFFIX_ENDPOINT_SECURITY_GROUP_RULES),
						Mockito.eq(this.defaultToken), Mockito.any(JSONObject.class));

		//remove network
		Mockito.doNothing().when(this.httpRequestClientUtil).doDeleteRequest(
				Mockito.endsWith(SUFFIX_ENDPOINT_DELETE_NETWORK), Mockito.eq(this.defaultToken));
		Mockito.doNothing().when(this.httpRequestClientUtil).doDeleteRequest(
				Mockito.endsWith(SUFFIX_ENDPOINT_DELETE_SECURITY_GROUP), Mockito.eq(this.defaultToken));

		Mockito.doReturn(NETWORK_ID).when(this.openStackV2NetworkPlugin).getNetworkIdFromJson(Mockito.anyString());
		NetworkOrder order = createEmptyOrder();

		//exercise
		try {
			this.openStackV2NetworkPlugin.requestInstance(order, this.defaultToken);
		} catch (FogbowManagerException | UnexpectedException e) {
			//doNothing
		}

		//verify
		//request checks
		Mockito.verify(this.httpRequestClientUtil, Mockito.times(1)).doPostRequest(
				Mockito.endsWith(OpenStackV2NetworkPlugin.SUFFIX_ENDPOINT_NETWORK), Mockito.eq(this.defaultToken),
				Mockito.any(JSONObject.class));
		Mockito.verify(this.httpRequestClientUtil, Mockito.times(1)).doPostRequest(
				Mockito.endsWith(OpenStackV2NetworkPlugin.SUFFIX_ENDPOINT_SUBNET), Mockito.eq(this.defaultToken),
				Mockito.any(JSONObject.class));
		Mockito.verify(this.httpRequestClientUtil, Mockito.times(1)).doPostRequest(
				Mockito.endsWith(OpenStackV2NetworkPlugin.SUFFIX_ENDPOINT_SECURITY_GROUP), Mockito.eq(this.defaultToken),
				Mockito.any(JSONObject.class));
		Mockito.verify(this.httpRequestClientUtil, Mockito.times(1)).doPostRequest(
				Mockito.endsWith(OpenStackV2NetworkPlugin.SUFFIX_ENDPOINT_SECURITY_GROUP_RULES), Mockito.eq(this.defaultToken),
				Mockito.any(JSONObject.class));

		//remove checks
		Mockito.verify(this.httpRequestClientUtil, Mockito.times(1)).doDeleteRequest(
				Mockito.endsWith(SUFFIX_ENDPOINT_DELETE_NETWORK), Mockito.eq(this.defaultToken));
		Mockito.verify(this.httpRequestClientUtil, Mockito.never()).doGetRequest(
				Mockito.endsWith(SUFFIX_ENDPOINT_GET_SECURITY_GROUP), Mockito.eq(this.defaultToken));
		Mockito.verify(this.httpRequestClientUtil, Mockito.times(1)).doDeleteRequest(
				Mockito.endsWith(SUFFIX_ENDPOINT_DELETE_SECURITY_GROUP) , Mockito.eq(this.defaultToken));
	}

	//requestInstance collaborators tests

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
		Assert.assertEquals(2, this.openStackV2NetworkPlugin.getDnsList().length);
		Assert.assertEquals(dnsOne, this.openStackV2NetworkPlugin.getDnsList()[0]);
		Assert.assertEquals(dnsTwo, this.openStackV2NetworkPlugin.getDnsList()[1]);
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
		Assert.assertEquals(DEFAULT_TENANT_ID, subnetJsonObject.optString(OpenStackV2NetworkPlugin.KEY_TENANT_ID));
		Assert.assertTrue(subnetJsonObject.optString(OpenStackV2NetworkPlugin.KEY_NAME)
				.contains(OpenStackV2NetworkPlugin.DEFAULT_SUBNET_NAME));
		Assert.assertEquals(order.getId(), subnetJsonObject.optString(OpenStackV2NetworkPlugin.KEY_NETWORK_ID));
		Assert.assertEquals(order.getAddress(), subnetJsonObject.optString(OpenStackV2NetworkPlugin.KEY_CIDR));
		Assert.assertEquals(order.getGateway(), subnetJsonObject.optString(OpenStackV2NetworkPlugin.KEY_GATEWAY_IP));
		Assert.assertEquals(true, subnetJsonObject.optBoolean(OpenStackV2NetworkPlugin.KEY_ENABLE_DHCP));
		Assert.assertEquals(OpenStackV2NetworkPlugin.DEFAULT_IP_VERSION,
				subnetJsonObject.optInt(OpenStackV2NetworkPlugin.KEY_IP_VERSION));
		Assert.assertEquals(dnsOne, subnetJsonObject.optJSONArray(OpenStackV2NetworkPlugin.KEY_DNS_NAMESERVERS).get(0));
		Assert.assertEquals(dnsTwo, subnetJsonObject.optJSONArray(OpenStackV2NetworkPlugin.KEY_DNS_NAMESERVERS).get(1));
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
		Assert.assertEquals(OpenStackV2NetworkPlugin.DEFAULT_NETWORK_ADDRESS,
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
		Assert.assertEquals(OpenStackV2NetworkPlugin.DEFAULT_DNS_NAME_SERVERS[0],
				subnetJsonObject.optJSONArray(OpenStackV2NetworkPlugin.KEY_DNS_NAMESERVERS).get(0));
		Assert.assertEquals(OpenStackV2NetworkPlugin.DEFAULT_DNS_NAME_SERVERS[1],
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
		Assert.assertEquals(false, subnetJsonObject.optBoolean(OpenStackV2NetworkPlugin.KEY_ENABLE_DHCP));
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
		Assert.assertTrue(subnetJsonObject.optString(OpenStackV2NetworkPlugin.KEY_GATEWAY_IP).isEmpty());
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
		Assert.assertEquals(networkId,
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
		Mockito.when(this.client.execute(Mockito.any(HttpUriRequest.class))).thenReturn(httpResponseGetNetwork,
				httpResponseGetSubnet);

		//exercise
		NetworkInstance instance = this.openStackV2NetworkPlugin.getInstance("instanceId00", this.defaultToken);

		//verify
		Assert.assertEquals(networkId, instance.getId());
		Assert.assertEquals(networkName, instance.getLabel());
		Assert.assertEquals(vlan, instance.getvLAN());
		Assert.assertEquals(InstanceState.READY, instance.getState());
		Assert.assertEquals(gatewayIp, instance.getGateway());
		Assert.assertEquals(cidr, instance.getAddress());
		Assert.assertEquals(NetworkAllocationMode.DYNAMIC, instance.getAllocation());
	}


	//deleteInstance tests

	//test case: Tests remove instance, it must execute a http client exactly 3 times: 1 GetRequest, 2 DeleteRequests
	@Test
	public void testRemoveInstance() throws IOException, JSONException, FogbowManagerException, UnexpectedException {
		//set up
		JSONObject securityGroupResponse = createSecurityGroupGetResponse(SECURITY_GROUP_ID);
		String suffixEndpointNetwork = OpenStackV2NetworkPlugin.SUFFIX_ENDPOINT_NETWORK + "/" + NETWORK_ID;
		String suffixEndpointGetSG = OpenStackV2NetworkPlugin.SUFFIX_ENDPOINT_SECURITY_GROUP + "?" +
				OpenStackV2NetworkPlugin.QUERY_NAME + "=" + OpenStackV2NetworkPlugin.SECURITY_GROUP_PREFIX + "-"
				+ NETWORK_ID;
		String suffixEndpointDeleteSG = OpenStackV2NetworkPlugin.SUFFIX_ENDPOINT_SECURITY_GROUP + "/" + SECURITY_GROUP_ID;

		Mockito.doNothing().when(this.httpRequestClientUtil).doDeleteRequest(
				Mockito.endsWith(suffixEndpointNetwork), Mockito.eq(this.defaultToken));
		Mockito.doReturn(securityGroupResponse.toString()).when(this.httpRequestClientUtil).doGetRequest(
				Mockito.endsWith(suffixEndpointGetSG), Mockito.eq(this.defaultToken));
		Mockito.doNothing().when(this.httpRequestClientUtil).doDeleteRequest(
				Mockito.endsWith(suffixEndpointDeleteSG), Mockito.eq(this.defaultToken));

		//exercise
		this.openStackV2NetworkPlugin.deleteInstance(NETWORK_ID, this.defaultToken);

		//verify
		Mockito.verify(this.httpRequestClientUtil, Mockito.times(1)).doDeleteRequest(
				Mockito.endsWith(suffixEndpointNetwork), Mockito.eq(this.defaultToken));
		Mockito.verify(this.httpRequestClientUtil, Mockito.times(1)).doGetRequest(
				Mockito.endsWith(suffixEndpointGetSG), Mockito.eq(this.defaultToken));
		Mockito.verify(this.httpRequestClientUtil, Mockito.times(1)).doDeleteRequest(
				Mockito.endsWith(suffixEndpointDeleteSG) , Mockito.eq(this.defaultToken));
	}

	//test: Tests a delete in a network which has compute attached to it
	@Test
	public void testRemoveNetworkWithInstanceAssociated() throws JSONException, IOException, FogbowManagerException, UnexpectedException {
		//set up
		String suffixEndpointNetwork = OpenStackV2NetworkPlugin.SUFFIX_ENDPOINT_NETWORK + "/" + NETWORK_ID;

		Mockito.doThrow(new HttpResponseException(HttpStatus.SC_CONFLICT, "conflict")).when(this.httpRequestClientUtil)
				.doDeleteRequest(Mockito.endsWith(suffixEndpointNetwork), Mockito.eq(this.defaultToken));

		//exercise
		try {
			this.openStackV2NetworkPlugin.deleteInstance(NETWORK_ID, this.defaultToken);
			Assert.fail();
		} catch (FogbowManagerException e) {
			// TODO: check error message
		} catch (Exception e) {
			Assert.fail();
		}

		// verify
		Mockito.verify(this.httpRequestClientUtil, Mockito.times(1)).doDeleteRequest(
				Mockito.anyString() , Mockito.eq(this.defaultToken));
		Mockito.verify(this.httpRequestClientUtil, Mockito.never()).doGetRequest(
				Mockito.anyString(), Mockito.eq(this.defaultToken));
	}

	// test case: throws an exception when try to delete the security group
	@Test(expected=FogbowManagerException.class)
	public void testDeleteInstanceExceptionSecurityGroupDeletion() throws FogbowManagerException, UnexpectedException, IOException {
		// set up
		JSONObject securityGroupResponse = createSecurityGroupGetResponse(SECURITY_GROUP_ID);
		// network deletion ok
		Mockito.doNothing().when(this.httpRequestClientUtil)
				.doDeleteRequest(Mockito.endsWith(SUFFIX_ENDPOINT_DELETE_NETWORK), Mockito.eq(this.defaultToken));
		// retrieving securityGroupId ok
		Mockito.doReturn(securityGroupResponse.toString()).when(this.httpRequestClientUtil).doGetRequest(
				Mockito.endsWith(SUFFIX_ENDPOINT_GET_SECURITY_GROUP), Mockito.eq(this.defaultToken));
		// security group deletion not ok
		Mockito.doThrow(new HttpResponseException(org.apache.commons.httpclient.HttpStatus.SC_BAD_REQUEST, "")).when(this.httpRequestClientUtil)
				.doDeleteRequest(Mockito.endsWith(SUFFIX_ENDPOINT_DELETE_SECURITY_GROUP), Mockito.eq(this.defaultToken));

		// exercise
		this.openStackV2NetworkPlugin.deleteInstance(NETWORK_ID, this.defaultToken);

		// verify
		Mockito.verify(this.httpRequestClientUtil, Mockito.times(1)).doDeleteRequest(
				Mockito.endsWith(SUFFIX_ENDPOINT_DELETE_NETWORK), Mockito.eq(this.defaultToken));
		Mockito.verify(this.httpRequestClientUtil, Mockito.times(1)).doGetRequest(
				Mockito.endsWith(SUFFIX_ENDPOINT_GET_SECURITY_GROUP), Mockito.eq(this.defaultToken));
		Mockito.verify(this.httpRequestClientUtil, Mockito.times(1)).doDeleteRequest(
				Mockito.endsWith(SUFFIX_ENDPOINT_DELETE_SECURITY_GROUP) , Mockito.eq(this.defaultToken));
	}

	// test case: throws a "notFoundInstance" exception and continue try to delete the security group
	@Test
	public void testDeleteInstanceNotFoundNetworkException() throws FogbowManagerException, UnexpectedException, IOException {
		// set up
		JSONObject securityGroupResponse = createSecurityGroupGetResponse(SECURITY_GROUP_ID);
		// network deletion not ok and return nof found
		Mockito.doThrow(new HttpResponseException(org.apache.commons.httpclient.HttpStatus.SC_NOT_FOUND, "")).when(this.httpRequestClientUtil)
				.doDeleteRequest(Mockito.endsWith(SUFFIX_ENDPOINT_DELETE_NETWORK), Mockito.eq(this.defaultToken));
		// retrieved securityGroupId ok
		Mockito.doReturn(securityGroupResponse.toString()).when(this.httpRequestClientUtil).doGetRequest(
				Mockito.endsWith(SUFFIX_ENDPOINT_GET_SECURITY_GROUP), Mockito.eq(this.defaultToken));
		// security group deletion ok
		Mockito.doNothing().when(this.httpRequestClientUtil).doDeleteRequest(Mockito.endsWith(SUFFIX_ENDPOINT_DELETE_SECURITY_GROUP), Mockito.eq(this.defaultToken));

		// exercise
		this.openStackV2NetworkPlugin.deleteInstance(NETWORK_ID, this.defaultToken);

		// verify
		Mockito.verify(this.httpRequestClientUtil, Mockito.times(1)).doDeleteRequest(
				Mockito.endsWith(SUFFIX_ENDPOINT_DELETE_NETWORK), Mockito.eq(this.defaultToken));
		Mockito.verify(this.httpRequestClientUtil, Mockito.times(1)).doGetRequest(
				Mockito.endsWith(SUFFIX_ENDPOINT_GET_SECURITY_GROUP), Mockito.eq(this.defaultToken));
		Mockito.verify(this.httpRequestClientUtil, Mockito.times(1)).doDeleteRequest(
				Mockito.endsWith(SUFFIX_ENDPOINT_DELETE_SECURITY_GROUP) , Mockito.eq(this.defaultToken));
	}


	//test case: Tests if getSecurityGroupIdFromGetResponse can retrieve the respective id from a valid json
	@Test
	public void testRetrieveSecurityGroupIdFromGetResponse() throws UnexpectedException {
		//set up
		JSONObject securityGroup = new JSONObject();
		securityGroup.put(OpenStackV2NetworkPlugin.KEY_TENANT_ID, "fake-tenant-id");
		securityGroup.put(OpenStackV2NetworkPlugin.KEY_NAME, "fake-name");
		securityGroup.put(OpenStackV2NetworkPlugin.KEY_ID, SECURITY_GROUP_ID);

		JSONArray securityGroups = new JSONArray();
		securityGroups.put(securityGroup);
		JSONObject response = new JSONObject();
		response.put(OpenStackV2NetworkPlugin.KEY_SECURITY_GROUPS, securityGroups);

		//exercise
		String id = this.openStackV2NetworkPlugin.getSecurityGroupIdFromGetResponse(response.toString());

		//verify
		Assert.assertEquals(SECURITY_GROUP_ID, id);
	}

	//test case: Tests if getSecurityGroupIdFromGetResponse throws exception when cannot get id from json
	@Test(expected = UnexpectedException.class)
	public void testErrorToRetrieveSecurityGroupIdFromGetResponse() throws UnexpectedException {
		//set up
		JSONObject securityGroup = new JSONObject();
		securityGroup.put(OpenStackV2NetworkPlugin.KEY_TENANT_ID, "fake-tenant-id");
		securityGroup.put(OpenStackV2NetworkPlugin.KEY_NAME, "fake-name");

		JSONArray securityGroups = new JSONArray();
		securityGroups.put(securityGroup);
		JSONObject response = new JSONObject();
		response.put(OpenStackV2NetworkPlugin.KEY_SECURITY_GROUPS, securityGroups);

		//exercise
		this.openStackV2NetworkPlugin.getSecurityGroupIdFromGetResponse(response.toString());
	}

	private NetworkOrder createNetworkOrder(String networkId, String address, String gateway,
			NetworkAllocationMode allocation) {
		String requestingMember = "fake-requesting-member";
		String providingMember = "fake-providing-member";
		NetworkOrder order = new NetworkOrder(networkId, Mockito.mock(FederationUser.class), requestingMember, providingMember,
				gateway, address, allocation);
		return order;
	}

	private NetworkOrder createEmptyOrder() {
		return new NetworkOrder(null, null, null, null, null, null);
	}

	private HttpResponse createHttpResponse(String content, int httpStatus) throws IOException {
		HttpResponse httpResponse = Mockito.mock(HttpResponse.class);
		HttpEntity httpEntity = Mockito.mock(HttpEntity.class);
		InputStream inputStrem = new ByteArrayInputStream(content.getBytes(UTF_8));
		Mockito.when(httpEntity.getContent()).thenReturn(inputStrem);
		Mockito.when(httpResponse.getEntity()).thenReturn(httpEntity);
		StatusLine statusLine = new BasicStatusLine(new ProtocolVersion("", 0, 0), httpStatus, "");
		Mockito.when(httpResponse.getStatusLine()).thenReturn(statusLine);
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
