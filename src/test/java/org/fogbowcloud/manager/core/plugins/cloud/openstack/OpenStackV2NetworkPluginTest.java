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
import org.fogbowcloud.manager.core.plugins.cloud.models.ErrorType;
import org.fogbowcloud.manager.core.plugins.cloud.models.ResponseConstants;
import org.fogbowcloud.manager.core.models.orders.NetworkAllocationMode;
import org.fogbowcloud.manager.core.models.orders.NetworkOrder;
import org.fogbowcloud.manager.core.models.instances.InstanceState;
import org.fogbowcloud.manager.core.models.instances.NetworkInstance;
import org.fogbowcloud.manager.core.models.tokens.FederationUser;
import org.fogbowcloud.manager.core.models.tokens.Token;
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

	private static final String NETWORK_HA_ROUTER_REPLICATED_INTERFACE = "network:ha_router_replicated_interface";
	private static final String UTF_8 = "UTF-8";
	private static final String DEFAULT_GATEWAY_INFO = "000000-gateway_info";
	private static final String DEFAULT_TENANT_ID = "tenantId";
	private static final String DEFAULT_NETWORK_URL = "http://localhost:0000";

	private OpenStackV2NetworkPlugin openStackV2NetworkPlugin;
	private Token defaultToken;
	private HttpClient client;
	private Properties properties;

	@Before
	public void setUp() {
	    HomeDir.getInstance().setPath("src/test/resources/private");
        PropertiesHolder propertiesHolder = PropertiesHolder.getInstance();
        this.properties = propertiesHolder.getProperties();
        this.properties.put(OpenStackV2NetworkPlugin.KEY_EXTERNAL_GATEWAY_INFO, DEFAULT_GATEWAY_INFO);
        this.properties.put(NETWORK_NEUTRONV2_URL_KEY, DEFAULT_NETWORK_URL);
		this.openStackV2NetworkPlugin = Mockito.spy(new OpenStackV2NetworkPlugin());

		this.client = Mockito.mock(HttpClient.class);
		this.openStackV2NetworkPlugin.setClient(this.client);

		Map<String, String> attributes = new HashMap<String, String>();
		attributes.put(OpenStackV2NetworkPlugin.TENANT_ID, DEFAULT_TENANT_ID);
		this.defaultToken = new Token("accessId", new Token.User("user", "user"), new Date(), attributes);
	}

	@After
	public void validate() {
		Mockito.validateMockitoUsage();
	}

	@Test
	public void testGenerateJsonEntityToCreateRouter() throws JSONException {
		JSONObject generateJsonEntityToCreateRouter = this.openStackV2NetworkPlugin.generateJsonEntityToCreateRouter();

		JSONObject routerJsonObject = generateJsonEntityToCreateRouter
				.optJSONObject(OpenStackV2NetworkPlugin.KEY_JSON_ROUTER);
		
		Assert.assertEquals(DEFAULT_GATEWAY_INFO,
				routerJsonObject.optJSONObject(OpenStackV2NetworkPlugin.KEY_EXTERNAL_GATEWAY_INFO)
						.optString(OpenStackV2NetworkPlugin.KEY_NETWORK_ID));
		
		Assert.assertTrue(routerJsonObject.optString(OpenStackV2NetworkPlugin.KEY_NAME)
				.contains(OpenStackV2NetworkPlugin.DEFAULT_ROUTER_NAME));
	}

	@Test
	public void testGenerateJsonEntityToCreateNetwork() throws JSONException {
		JSONObject generateJsonEntityToCreateNetwork = this.openStackV2NetworkPlugin
				.generateJsonEntityToCreateNetwork(DEFAULT_TENANT_ID);

		JSONObject networkJsonObject = generateJsonEntityToCreateNetwork
				.optJSONObject(OpenStackV2NetworkPlugin.KEY_JSON_NETWORK);
		Assert.assertEquals(DEFAULT_TENANT_ID, networkJsonObject.optString(OpenStackV2NetworkPlugin.KEY_TENANT_ID));
		Assert.assertTrue(networkJsonObject.optString(OpenStackV2NetworkPlugin.KEY_NAME)
				.contains(OpenStackV2NetworkPlugin.DEFAULT_NETWORK_NAME));
	}

	@Test
	public void testSetDnsList() {
		String dnsOne = "one";
		String dnsTwo = "Two";

		this.properties.put(OpenStackV2NetworkPlugin.KEY_DNS_NAMESERVERS, dnsOne + "," + dnsTwo);
		this.openStackV2NetworkPlugin.setDNSList(this.properties);
		
		Assert.assertEquals(2, this.openStackV2NetworkPlugin.getDnsList().length);
		Assert.assertEquals(dnsOne, this.openStackV2NetworkPlugin.getDnsList()[0]);
		Assert.assertEquals(dnsTwo, this.openStackV2NetworkPlugin.getDnsList()[1]);
	}

	@Test
	public void testGenerateJsonEntityToCreateSubnet() throws JSONException {
		String dnsOne = "one";
		String dnsTwo = "Two";
		this.properties.put(OpenStackV2NetworkPlugin.KEY_DNS_NAMESERVERS, dnsOne + "," + dnsTwo);
		this.openStackV2NetworkPlugin.setDNSList(this.properties);

		String networkId = "networkId";
		String address = "10.10.10.10/24";
		String gateway = "10.10.10.11";
		NetworkOrder order = createNetworkOrder(networkId, address, gateway, NetworkAllocationMode.DYNAMIC);

		JSONObject generateJsonEntityToCreateSubnet = this.openStackV2NetworkPlugin
				.generateJsonEntityToCreateSubnet(order.getId(), DEFAULT_TENANT_ID, order);

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
				subnetJsonObject.optString(OpenStackV2NetworkPlugin.KEY_IP_VERSION));
		Assert.assertEquals(dnsOne, subnetJsonObject.optJSONArray(OpenStackV2NetworkPlugin.KEY_DNS_NAMESERVERS).get(0));
		Assert.assertEquals(dnsTwo, subnetJsonObject.optJSONArray(OpenStackV2NetworkPlugin.KEY_DNS_NAMESERVERS).get(1));
	}

	@Test
	public void testGenerateJsonEntityToCreateSubnetDefaultAddress() throws JSONException {
		String networkId = "networkId";
		NetworkOrder order = createNetworkOrder(networkId, null, null, null);
		JSONObject generateJsonEntityToCreateSubnet = this.openStackV2NetworkPlugin
				.generateJsonEntityToCreateSubnet(order.getId(), DEFAULT_TENANT_ID, order);

		JSONObject subnetJsonObject = generateJsonEntityToCreateSubnet
				.optJSONObject(OpenStackV2NetworkPlugin.KEY_JSON_SUBNET);
		Assert.assertEquals(OpenStackV2NetworkPlugin.DEFAULT_NETWORK_ADDRESS,
				subnetJsonObject.optString(OpenStackV2NetworkPlugin.KEY_CIDR));
	}

	@Test
	public void testGenerateJsonEntityToCreateSubnetDefaultDns() throws JSONException {
		String networkId = "networkId";
		NetworkOrder order = createNetworkOrder(networkId, null, null, null);
		JSONObject generateJsonEntityToCreateSubnet = this.openStackV2NetworkPlugin
				.generateJsonEntityToCreateSubnet(order.getId(), DEFAULT_TENANT_ID, order);

		JSONObject subnetJsonObject = generateJsonEntityToCreateSubnet
				.optJSONObject(OpenStackV2NetworkPlugin.KEY_JSON_SUBNET);
		Assert.assertEquals(OpenStackV2NetworkPlugin.DEFAULT_DNS_NAME_SERVERS[0],
				subnetJsonObject.optJSONArray(OpenStackV2NetworkPlugin.KEY_DNS_NAMESERVERS).get(0));
		Assert.assertEquals(OpenStackV2NetworkPlugin.DEFAULT_DNS_NAME_SERVERS[1],
				subnetJsonObject.optJSONArray(OpenStackV2NetworkPlugin.KEY_DNS_NAMESERVERS).get(1));
	}

	@Test
	public void testGenerateJsonEntityToCreateSubnetStaticAllocation() throws JSONException {
		String networkId = "networkId";
		NetworkOrder order = createNetworkOrder(networkId, null, null, NetworkAllocationMode.STATIC);
		JSONObject generateJsonEntityToCreateSubnet = this.openStackV2NetworkPlugin
				.generateJsonEntityToCreateSubnet(order.getId(), DEFAULT_TENANT_ID, order);

		JSONObject subnetJsonObject = generateJsonEntityToCreateSubnet
				.optJSONObject(OpenStackV2NetworkPlugin.KEY_JSON_SUBNET);
		Assert.assertEquals(false, subnetJsonObject.optBoolean(OpenStackV2NetworkPlugin.KEY_ENABLE_DHCP));
	}

	@Test
	public void testGenerateJsonEntityToCreateSubnetWithoutGateway() throws JSONException {
		String networkId = "networkId";
		NetworkOrder order = createNetworkOrder(networkId, null, null, null);
		JSONObject generateJsonEntityToCreateSubnet = this.openStackV2NetworkPlugin
				.generateJsonEntityToCreateSubnet(order.getId(), DEFAULT_TENANT_ID, order);

		JSONObject subnetJsonObject = generateJsonEntityToCreateSubnet
				.optJSONObject(OpenStackV2NetworkPlugin.KEY_JSON_SUBNET);
		Assert.assertTrue(subnetJsonObject.optString(OpenStackV2NetworkPlugin.KEY_GATEWAY_IP).isEmpty());
	}

	@Test
	public void testGenerateJsonEntitySubnetId() throws JSONException {
		String subnetId = "subnet";
		JSONObject generateJsonEntitySubnetId = this.openStackV2NetworkPlugin.generateJsonEntitySubnetId(subnetId);

		Assert.assertEquals(subnetId,
				generateJsonEntitySubnetId.optString(OpenStackV2NetworkPlugin.KEY_JSON_SUBNET_ID));
	}

	@Test
	public void testGetRouterIdFromJson() throws JSONException {
		String routerId = "routerId00";
		JSONObject routerContentJsonObject = new JSONObject();
		routerContentJsonObject.put(OpenStackV2NetworkPlugin.KEY_ID, routerId);

		JSONObject routerJsonObject = new JSONObject();
		routerJsonObject.put(OpenStackV2NetworkPlugin.KEY_JSON_ROUTER, routerContentJsonObject);
		Assert.assertEquals(routerId, this.openStackV2NetworkPlugin.getRouterIdFromJson(routerJsonObject.toString()));
	}

	@Test
	public void testGetNetworkIdFromJson() throws JSONException {
		String networkId = "networkId00";
		JSONObject networkContentJsonObject = new JSONObject();
		networkContentJsonObject.put(OpenStackV2NetworkPlugin.KEY_ID, networkId);

		JSONObject networkJsonObject = new JSONObject();
		networkJsonObject.put(OpenStackV2NetworkPlugin.KEY_JSON_NETWORK, networkContentJsonObject);
		Assert.assertEquals(networkId,
				this.openStackV2NetworkPlugin.getNetworkIdFromJson(networkJsonObject.toString()));
	}

	@Test
	public void testGetSubnetIdFromJson() throws JSONException {
		String subnetId = "subnetId00";
		JSONObject subnetContentJsonObject = new JSONObject();
		subnetContentJsonObject.put(OpenStackV2NetworkPlugin.KEY_ID, subnetId);

		JSONObject subnetJsonObject = new JSONObject();
		subnetJsonObject.put(OpenStackV2NetworkPlugin.KEY_JSON_SUBNET, subnetContentJsonObject);
		Assert.assertEquals(subnetId, this.openStackV2NetworkPlugin.getSubnetIdFromJson(subnetJsonObject.toString()));
	}

	@Test
	public void testGetInstanceFromJson() throws JSONException, IOException, FogbowManagerException {
		// Generating network response string
		JSONObject networkContentJsonObject = new JSONObject();
		String networkId = "networkId00";
		String networkName = "netName";
		String subnetId = "subnetId00";
		String vlan = "vlan00";
		networkContentJsonObject.put(OpenStackV2NetworkPlugin.KEY_ID, networkId);
		networkContentJsonObject.put(OpenStackV2NetworkPlugin.KEY_PROVIDER_SEGMENTATION_ID, vlan);
		networkContentJsonObject.put(OpenStackV2NetworkPlugin.KEY_STATUS,
				OpenStackNetworkInstanceStateMapper.ACTIVE_STATUS);
		networkContentJsonObject.put(OpenStackV2NetworkPlugin.KEY_NAME, networkName);
		JSONArray subnetJsonArray = new JSONArray(Arrays.asList(new String[] { subnetId }));
		networkContentJsonObject.put(OpenStackV2NetworkPlugin.KEY_SUBNETS, subnetJsonArray);
		JSONObject networkJsonObject = new JSONObject();
		networkJsonObject.put(OpenStackV2NetworkPlugin.KEY_JSON_NETWORK, networkContentJsonObject);

		// Generating subnet response string
		JSONObject subnetContentJsonObject = new JSONObject();
		String gatewayIp = "10.10.10.10";
		String cidr = "10.10.10.0/24";
		subnetContentJsonObject.put(OpenStackV2NetworkPlugin.KEY_GATEWAY_IP, gatewayIp);
		subnetContentJsonObject.put(OpenStackV2NetworkPlugin.KEY_ENABLE_DHCP, true);
		subnetContentJsonObject.put(OpenStackV2NetworkPlugin.KEY_CIDR, cidr);

		JSONObject subnetJsonObject = new JSONObject();
		subnetJsonObject.put(OpenStackV2NetworkPlugin.KEY_JSON_SUBNET, subnetContentJsonObject);

		HttpResponse httpResponseGetNetwork = createHttpResponse(networkJsonObject.toString(), HttpStatus.SC_OK);
		HttpResponse httpResponseGetSubnet = createHttpResponse(subnetJsonObject.toString(), HttpStatus.SC_OK);
		Mockito.when(this.client.execute(Mockito.any(HttpUriRequest.class))).thenReturn(httpResponseGetNetwork,
				httpResponseGetSubnet);

		NetworkInstance instance = this.openStackV2NetworkPlugin.getInstance("instanceId00", this.defaultToken);

		Assert.assertEquals(networkId, instance.getId());
		Assert.assertEquals(networkName, instance.getLabel());
		Assert.assertEquals(vlan, instance.getvLAN());
		Assert.assertEquals(InstanceState.READY, instance.getState());
		Assert.assertEquals(gatewayIp, instance.getGateway());
		Assert.assertEquals(cidr, instance.getAddress());
		Assert.assertEquals(NetworkAllocationMode.DYNAMIC, instance.getAllocation());
	}

	@Test
	public void testRemoveInstance() throws IOException, JSONException, FogbowManagerException {
		JSONObject portOneJsonObject = new JSONObject();
		String networkId = "networkId";
		portOneJsonObject.put(OpenStackV2NetworkPlugin.KEY_NETWORK_ID, networkId);
		portOneJsonObject.put(OpenStackV2NetworkPlugin.KEY_DEVICE_OWNER, NETWORK_HA_ROUTER_REPLICATED_INTERFACE);

		String routerId = "routerId";
		portOneJsonObject.put(OpenStackV2NetworkPlugin.KEY_DEVICE_ID, routerId);
		JSONArray subnetsjsonArray = new JSONArray();
		JSONObject subnetObject = new JSONObject();

		String subnetId = "subnetId";
		subnetObject.put(OpenStackV2NetworkPlugin.KEY_JSON_SUBNET_ID, subnetId);
		subnetsjsonArray.put(0, subnetObject);
		portOneJsonObject.put(OpenStackV2NetworkPlugin.KEY_FIXES_IPS, subnetsjsonArray);

		JSONArray portsArrayJsonObject = new JSONArray();
		portsArrayJsonObject.put(0, portOneJsonObject);

		JSONObject portsJsonObject = new JSONObject();
		portsJsonObject.put(OpenStackV2NetworkPlugin.KEY_JSON_PORTS, portsArrayJsonObject);

		HttpResponse httpResponseGetPorts = createHttpResponse(portsJsonObject.toString(), HttpStatus.SC_OK);
		HttpResponse httpResponsePutRemoveInterface = createHttpResponse("", HttpStatus.SC_OK);
		HttpResponse httpResponseDeleteRouter = createHttpResponse("", HttpStatus.SC_OK);
		HttpResponse httpResponseDeleteNetwork = createHttpResponse("", HttpStatus.SC_OK);
		Mockito.when(this.client.execute(Mockito.any(HttpUriRequest.class))).thenReturn(httpResponseGetPorts,
				httpResponsePutRemoveInterface, httpResponseDeleteRouter, httpResponseDeleteNetwork);

		this.openStackV2NetworkPlugin.deleteInstance(networkId, this.defaultToken);

		Mockito.verify(this.client, Mockito.times(4)).execute(Mockito.any(HttpUriRequest.class));
	}

	@Test
	public void testRemoveInstanceNullpointException() throws JSONException, IOException {
		JSONObject portOneJsonObject = new JSONObject();
		String networkId = "networkId";
		portOneJsonObject.put(OpenStackV2NetworkPlugin.KEY_NETWORK_ID, networkId);
		portOneJsonObject.put(OpenStackV2NetworkPlugin.KEY_DEVICE_OWNER, NETWORK_HA_ROUTER_REPLICATED_INTERFACE);

		String routerId = "routerId";
		portOneJsonObject.put(OpenStackV2NetworkPlugin.KEY_DEVICE_ID, routerId);

		JSONArray portsArrayJsonObject = new JSONArray();
		portsArrayJsonObject.put(0, portOneJsonObject);

		JSONObject portsJsonObject = new JSONObject();
		portsJsonObject.put(OpenStackV2NetworkPlugin.KEY_JSON_PORTS, portsArrayJsonObject);

		HttpResponse httpResponseGetPorts = createHttpResponse(portsJsonObject.toString(), HttpStatus.SC_OK);
		Mockito.when(this.client.execute(Mockito.any(HttpUriRequest.class))).thenReturn(httpResponseGetPorts);

		try {
			this.openStackV2NetworkPlugin.deleteInstance(networkId, this.defaultToken);
			Assert.fail();
		} catch (FogbowManagerException e) {
			String errorMessage = e.getMessage();
			Assert.assertTrue(errorMessage.contains(ErrorType.BAD_REQUEST.toString()));
			Assert.assertTrue(errorMessage.contains(ResponseConstants.IRREGULAR_SYNTAX.toString()));
		} catch (Exception e) {
			Assert.fail();
		}

		Mockito.verify(this.client, Mockito.times(1)).execute(Mockito.any(HttpUriRequest.class));
	}

	@Test
	public void testRemoveInstanceRemoveRouterBadRequestException() throws JSONException, IOException {
		JSONObject portOneJsonObject = new JSONObject();
		String networkId = "networkId";
		portOneJsonObject.put(OpenStackV2NetworkPlugin.KEY_NETWORK_ID, networkId);
		portOneJsonObject.put(OpenStackV2NetworkPlugin.KEY_DEVICE_OWNER, NETWORK_HA_ROUTER_REPLICATED_INTERFACE);

		String routerId = "routerId";
		portOneJsonObject.put(OpenStackV2NetworkPlugin.KEY_DEVICE_ID, routerId);

		JSONArray subnetsjsonArray = new JSONArray();
		JSONObject subnetObject = new JSONObject();
		String subnetId = "subnetId";
		subnetObject.put(OpenStackV2NetworkPlugin.KEY_JSON_SUBNET_ID, subnetId);
		subnetsjsonArray.put(0, subnetObject);
		portOneJsonObject.put(OpenStackV2NetworkPlugin.KEY_FIXES_IPS, subnetsjsonArray);

		JSONArray portsArrayJsonObject = new JSONArray();
		portsArrayJsonObject.put(0, portOneJsonObject);

		JSONObject portsJsonObject = new JSONObject();
		portsJsonObject.put(OpenStackV2NetworkPlugin.KEY_JSON_PORTS, portsArrayJsonObject);

		HttpResponse httpResponseGetPorts = createHttpResponse(portsJsonObject.toString(), HttpStatus.SC_OK);
		HttpResponse httpResponseDeleteRouter = createHttpResponse("", HttpStatus.SC_BAD_REQUEST);
		HttpResponse httpResponseDeleteNetwork = createHttpResponse("", HttpStatus.SC_OK);
		Mockito.when(this.client.execute(Mockito.any(HttpUriRequest.class))).thenReturn(httpResponseGetPorts,
				httpResponseDeleteRouter, httpResponseDeleteNetwork);

		try {
			this.openStackV2NetworkPlugin.deleteInstance(networkId, this.defaultToken);
			Assert.fail();
		} catch (FogbowManagerException e) {
			String errorMessage = e.getMessage();
			Assert.assertTrue(errorMessage.contains(ErrorType.BAD_REQUEST.toString()));
		} catch (Exception e) {
			Assert.fail();
		}

		Mockito.verify(this.client, Mockito.times(2)).execute(Mockito.any(HttpUriRequest.class));
	}

	@Test
	public void testRemoveInstanceRemoveNetworkBadRequestException() throws JSONException, IOException {
		JSONObject portOneJsonObject = new JSONObject();
		String networkId = "networkId";
		portOneJsonObject.put(OpenStackV2NetworkPlugin.KEY_NETWORK_ID, networkId);
		portOneJsonObject.put(OpenStackV2NetworkPlugin.KEY_DEVICE_OWNER, NETWORK_HA_ROUTER_REPLICATED_INTERFACE);

		String routerId = "routerId";
		portOneJsonObject.put(OpenStackV2NetworkPlugin.KEY_DEVICE_ID, routerId);

		JSONArray subnetsjsonArray = new JSONArray();
		JSONObject subnetObject = new JSONObject();
		String subnetId = "subnetId";
		subnetObject.put(OpenStackV2NetworkPlugin.KEY_JSON_SUBNET_ID, subnetId);
		subnetsjsonArray.put(0, subnetObject);
		portOneJsonObject.put(OpenStackV2NetworkPlugin.KEY_FIXES_IPS, subnetsjsonArray);

		JSONArray portsArrayJsonObject = new JSONArray();
		portsArrayJsonObject.put(0, portOneJsonObject);

		JSONObject portsJsonObject = new JSONObject();
		portsJsonObject.put(OpenStackV2NetworkPlugin.KEY_JSON_PORTS, portsArrayJsonObject);

		HttpResponse httpResponseGetPorts = createHttpResponse(portsJsonObject.toString(), HttpStatus.SC_OK);
		HttpResponse httpResponseDeleteRouter = createHttpResponse("", HttpStatus.SC_OK);
		HttpResponse httpResponseDeleteNetwork = createHttpResponse("", HttpStatus.SC_BAD_REQUEST);
		Mockito.when(this.client.execute(Mockito.any(HttpUriRequest.class))).thenReturn(httpResponseGetPorts,
				httpResponseDeleteRouter, httpResponseDeleteNetwork);

		try {
			this.openStackV2NetworkPlugin.deleteInstance(networkId, this.defaultToken);
			Assert.fail();
		} catch (FogbowManagerException e) {
			String errorMessage = e.getMessage();
			Assert.assertTrue(errorMessage.contains(ErrorType.BAD_REQUEST.toString()));
		} catch (Exception e) {
			Assert.fail();
		}

		Mockito.verify(client, Mockito.times(4)).execute(Mockito.any(HttpUriRequest.class));
	}

	@Test
	public void testRemoveNetworkWithInstanceAssociatedException() throws JSONException, IOException, FogbowManagerException {
		String networkId = "networkId";
		String routerId = "routerId";

		// generate ports json response
		JSONObject portOneJsonObject = new JSONObject();
		portOneJsonObject.put(OpenStackV2NetworkPlugin.KEY_NETWORK_ID, networkId);
		portOneJsonObject.put(OpenStackV2NetworkPlugin.KEY_DEVICE_OWNER, NETWORK_HA_ROUTER_REPLICATED_INTERFACE);
		portOneJsonObject.put(OpenStackV2NetworkPlugin.KEY_DEVICE_ID, routerId);

		JSONArray subnetsjsonArray = new JSONArray();
		JSONObject subnetObject = new JSONObject();
		String subnetId = "subnetId";
		subnetObject.put(OpenStackV2NetworkPlugin.KEY_JSON_SUBNET_ID, subnetId);
		subnetsjsonArray.put(0, subnetObject);
		portOneJsonObject.put(OpenStackV2NetworkPlugin.KEY_FIXES_IPS, subnetsjsonArray);

		JSONArray portsArrayJsonObject = new JSONArray();
		portsArrayJsonObject.put(0, portOneJsonObject);

		JSONObject portsJsonObject = new JSONObject();
		portsJsonObject.put(OpenStackV2NetworkPlugin.KEY_JSON_PORTS, portsArrayJsonObject);

		// mock
		HttpResponse httpResponseGetPorts = createHttpResponse(portsJsonObject.toString(), HttpStatus.SC_OK);
		HttpResponse httpResponseDeleteRouter = createHttpResponse("", HttpStatus.SC_BAD_REQUEST);
		Mockito.when(this.client.execute(Mockito.any(HttpUriRequest.class))).thenReturn(httpResponseGetPorts,
				httpResponseDeleteRouter);

		try {
			this.openStackV2NetworkPlugin.deleteInstance(networkId, this.defaultToken);
			Assert.fail();
		} catch (FogbowManagerException e) {
			String errorMessage = e.getMessage();
			Assert.assertTrue(errorMessage.contains(ErrorType.BAD_REQUEST.toString()));
		} catch (Exception e) {
			Assert.fail();
		}

		// check
		Mockito.verify(this.client, Mockito.times(2)).execute(Mockito.any(HttpUriRequest.class));
		Mockito.verify(this.openStackV2NetworkPlugin, Mockito.never()).removeNetwork(Mockito.any(Token.class),
				Mockito.anyString());

		// mock
		Mockito.reset(this.client);
		httpResponseGetPorts = createHttpResponse(portsJsonObject.toString(), HttpStatus.SC_OK);
		HttpResponse httpResponseDeleteNetwork = createHttpResponse("", HttpStatus.SC_OK);
		httpResponseDeleteRouter = createHttpResponse("", HttpStatus.SC_OK);
		HttpResponse httpResponsePutRequest = createHttpResponse("", HttpStatus.SC_OK);
		Mockito.when(this.client.execute(Mockito.any(HttpUriRequest.class))).thenReturn(httpResponseGetPorts,
				httpResponsePutRequest, httpResponseDeleteRouter, httpResponseDeleteNetwork);

		this.openStackV2NetworkPlugin.deleteInstance(networkId, this.defaultToken);

		// check
		Mockito.verify(this.openStackV2NetworkPlugin, Mockito.times(1)).removeRouter(Mockito.any(Token.class),
				Mockito.eq(routerId));
		Mockito.verify(this.openStackV2NetworkPlugin, Mockito.times(1)).removeNetwork(Mockito.any(Token.class),
				Mockito.eq(networkId));
	}

	@Test
	public void testRequestInstance() throws IOException, FogbowManagerException {
		HttpResponse httpResponsePostRouter = createHttpResponse("", HttpStatus.SC_OK);
		HttpResponse httpResponsePostNetwork = createHttpResponse("", HttpStatus.SC_OK);
		HttpResponse httpResponsePostSubnet = createHttpResponse("", HttpStatus.SC_OK);
		HttpResponse httpResponsePutInterface = createHttpResponse("", HttpStatus.SC_OK);
		Mockito.when(this.client.execute(Mockito.any(HttpUriRequest.class))).thenReturn(httpResponsePostRouter,
				httpResponsePostNetwork, httpResponsePostSubnet, httpResponsePutInterface);

		NetworkOrder order = createEmptyOrder();
		this.openStackV2NetworkPlugin.requestInstance(order, this.defaultToken);

		Mockito.verify(this.client, Mockito.times(4)).execute(Mockito.any(HttpUriRequest.class));
	}

	@Test(expected = FogbowManagerException.class)
	public void testRequestInstancePostRouterError() throws IOException, FogbowManagerException {
		HttpResponse httpResponsePostRouter = createHttpResponse("", HttpStatus.SC_BAD_REQUEST);
		Mockito.when(this.client.execute(Mockito.any(HttpUriRequest.class))).thenReturn(httpResponsePostRouter);

		NetworkOrder order = createEmptyOrder();
		this.openStackV2NetworkPlugin.requestInstance(order, this.defaultToken);

		Mockito.verify(this.client, Mockito.times(1)).execute(Mockito.any(HttpUriRequest.class));
	}

	@Test
	public void testRequestInstancePostNetworkError() throws IOException {
		HttpResponse httpResponsePostRouter = createHttpResponse("", HttpStatus.SC_OK);
		HttpResponse httpResponsePostNetwork = createHttpResponse("", HttpStatus.SC_BAD_REQUEST);
		HttpResponse httpResponseRemoveRouter = createHttpResponse("", HttpStatus.SC_OK);
		Mockito.when(this.client.execute(Mockito.any(HttpUriRequest.class))).thenReturn(httpResponsePostRouter,
				httpResponsePostNetwork, httpResponseRemoveRouter);

		NetworkOrder order = createEmptyOrder();
		try {
			this.openStackV2NetworkPlugin.requestInstance(order, this.defaultToken);
			Assert.fail();
		} catch (Exception e) {
		}

		Mockito.verify(this.client, Mockito.times(3)).execute(Mockito.any(HttpUriRequest.class));
	}

	@Test(expected = FogbowManagerException.class)
	public void testRequestInstancePostSubnetError() throws IOException, FogbowManagerException {
		HttpResponse httpResponsePostRouter = createHttpResponse("", HttpStatus.SC_OK);
		HttpResponse httpResponsePostNetwork = createHttpResponse("", HttpStatus.SC_OK);
		HttpResponse httpResponsePostSubnet = createHttpResponse("", HttpStatus.SC_BAD_REQUEST);
		HttpResponse httpResponseRemoveRouter = createHttpResponse("", HttpStatus.SC_OK);
		HttpResponse httpResponseRemoveNetwork = createHttpResponse("", HttpStatus.SC_OK);
		Mockito.when(this.client.execute(Mockito.any(HttpUriRequest.class))).thenReturn(httpResponsePostRouter,
				httpResponsePostNetwork, httpResponsePostSubnet, httpResponseRemoveRouter, httpResponseRemoveNetwork);

		NetworkOrder order = createEmptyOrder();
		this.openStackV2NetworkPlugin.requestInstance(order, this.defaultToken);

		Mockito.verify(this.client, Mockito.times(5)).execute(Mockito.any(HttpUriRequest.class));
	}

	@Test
	public void testRequestInstancePutInterfaceError() throws IOException {
		HttpResponse httpResponsePostRouter = createHttpResponse("", HttpStatus.SC_OK);
		HttpResponse httpResponsePostNetwork = createHttpResponse("", HttpStatus.SC_OK);
		HttpResponse httpResponsePostSubnet = createHttpResponse("", HttpStatus.SC_OK);
		HttpResponse httpResponsePutInterface = createHttpResponse("", HttpStatus.SC_BAD_REQUEST);
		HttpResponse httpResponseRemoveRouter = createHttpResponse("", HttpStatus.SC_OK);
		HttpResponse httpResponseRemoveNetwork = createHttpResponse("", HttpStatus.SC_OK);
		Mockito.when(this.client.execute(Mockito.any(HttpUriRequest.class))).thenReturn(httpResponsePostRouter,
				httpResponsePostNetwork, httpResponsePostSubnet, httpResponsePutInterface, httpResponseRemoveRouter,
				httpResponseRemoveNetwork);

		NetworkOrder order = createEmptyOrder();
		try {
			this.openStackV2NetworkPlugin.requestInstance(order, this.defaultToken);
			Assert.fail();
		} catch (Exception e) {
		}

		Mockito.verify(this.client, Mockito.times(6)).execute(Mockito.any(HttpUriRequest.class));
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
}
