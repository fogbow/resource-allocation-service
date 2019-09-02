package cloud.fogbow.ras.core.plugins.interoperability.openstack.network.v2;

import cloud.fogbow.common.constants.HttpMethod;
import cloud.fogbow.common.constants.OpenStackConstants;
import cloud.fogbow.common.exceptions.FogbowException;
import cloud.fogbow.common.exceptions.InvalidParameterException;
import cloud.fogbow.common.exceptions.UnexpectedException;
import cloud.fogbow.common.util.HomeDir;
import cloud.fogbow.common.util.connectivity.cloud.openstack.OpenStackHttpClient;
import cloud.fogbow.common.util.connectivity.HttpResponse;
import cloud.fogbow.common.util.connectivity.cloud.openstack.OpenStackHttpToFogbowExceptionMapper;
import cloud.fogbow.ras.constants.SystemConstants;
import cloud.fogbow.ras.core.BaseUnitTests;
import cloud.fogbow.ras.core.PropertiesHolder;
import cloud.fogbow.ras.core.TestUtils;
import cloud.fogbow.ras.core.datastore.DatabaseManager;
import cloud.fogbow.ras.core.datastore.services.AuditableOrderStateChangeService;
import cloud.fogbow.ras.core.models.NetworkAllocationMode;
import cloud.fogbow.ras.api.http.response.InstanceState;
import cloud.fogbow.ras.api.http.response.NetworkInstance;
import cloud.fogbow.ras.core.models.orders.NetworkOrder;
import cloud.fogbow.ras.core.plugins.interoperability.openstack.OpenStackCloudUtils;
import cloud.fogbow.ras.core.plugins.interoperability.openstack.OpenStackStateMapper;
import cloud.fogbow.common.models.OpenStackV3User;
import org.apache.http.HttpStatus;
import org.apache.http.client.HttpResponseException;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.core.classloader.annotations.PrepareForTest;

import java.io.File;
import java.io.IOException;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Properties;

@PrepareForTest({DatabaseManager.class, OpenStackCloudUtils.class, OpenStackHttpToFogbowExceptionMapper.class})
public class OpenStackNetworkPluginTest extends BaseUnitTests {

    private static final String NETWORK_NEUTRONV2_URL_KEY = "openstack_neutron_v2_url";

    private static final String UTF_8 = "UTF-8";
    private static final String DEFAULT_GATEWAY_INFO = "000000-gateway_info";
    private static final String DEFAULT_PROJECT_ID = "PROJECT_ID";
    private static final String DEFAULT_NETWORK_URL = "http://localhost:0000";
    private static final String SECURITY_GROUP_ID = "fake-sg-id";
    private static final String NETWORK_ID = "networkId";

    private static final String FAKE_TOKEN_VALUE = "fake-token-value";
    private static final String FAKE_USER_ID = "fake-user-id";
    private static final String FAKE_NAME = "fake-name";
    private static final String FAKE_PROJECT_ID = "fake-project-id";

    private static final String SUFFIX_ENDPOINT_DELETE_NETWORK = OpenStackNetworkPlugin.SUFFIX_ENDPOINT_NETWORK +
            File.separator + NETWORK_ID;

    private static final String SUFFIX_ENDPOINT_DELETE_SECURITY_GROUP = OpenStackNetworkPlugin.SUFFIX_ENDPOINT_SECURITY_GROUP +
            File.separator + SECURITY_GROUP_ID;
    private static final String SUFFIX_ENDPOINT_GET_SECURITY_GROUP = OpenStackNetworkPlugin.SUFFIX_ENDPOINT_SECURITY_GROUP + "?" +
            OpenStackNetworkPlugin.QUERY_NAME + "=" + SystemConstants.PN_SECURITY_GROUP_PREFIX + NETWORK_ID;

    private OpenStackNetworkPlugin openStackNetworkPlugin;
    private OpenStackV3User openStackV3User;
    private Properties properties;
    private OpenStackHttpClient openStackHttpClient;

    @Before
    public void setUp() throws InvalidParameterException, UnexpectedException {
        testUtils.mockReadOrdersFromDataBase();
        PropertiesHolder propertiesHolder = PropertiesHolder.getInstance();
        this.properties = propertiesHolder.getProperties();
        this.properties.put(OpenStackNetworkPlugin.KEY_EXTERNAL_GATEWAY_INFO, DEFAULT_GATEWAY_INFO);
        this.properties.put(NETWORK_NEUTRONV2_URL_KEY, DEFAULT_NETWORK_URL);
        String cloudConfPath = HomeDir.getPath() + SystemConstants.CLOUDS_CONFIGURATION_DIRECTORY_NAME + File.separator
                + "default" + File.separator + SystemConstants.CLOUD_SPECIFICITY_CONF_FILE_NAME;
        this.openStackNetworkPlugin = Mockito.spy(new OpenStackNetworkPlugin(cloudConfPath));

        this.openStackHttpClient = Mockito.spy(new OpenStackHttpClient());
        this.openStackNetworkPlugin.setClient(this.openStackHttpClient);
        this.openStackV3User = new OpenStackV3User(FAKE_USER_ID, FAKE_NAME, FAKE_TOKEN_VALUE, FAKE_PROJECT_ID);
    }

    @Test
    public void testRequestInstance() throws FogbowException{
        PowerMockito.mockStatic(OpenStackCloudUtils.class);
        NetworkOrder order = createNetworkOrder(NETWORK_ID, TestUtils.DEFAULT_CIDR, DEFAULT_GATEWAY_INFO, NetworkAllocationMode.DYNAMIC);
        CreateNetworkResponse createNetworkResponse = new CreateNetworkResponse(new CreateNetworkResponse.Network(NETWORK_ID));
        Mockito.doReturn(createNetworkResponse).when(openStackNetworkPlugin).createNetwork(Mockito.any(), Mockito.any(), Mockito.any());
        Mockito.doReturn(new CreateSecurityGroupResponse(new CreateSecurityGroupResponse.SecurityGroup(SECURITY_GROUP_ID))).when(openStackNetworkPlugin)
            .createSecurityGroup(Mockito.any(), Mockito.any(), Mockito.any(), Mockito.any());
        Mockito.doNothing().when(openStackNetworkPlugin).createSubNet(Mockito.any(), Mockito.any(), Mockito.any(), Mockito.any());
        Mockito.doNothing().when(openStackNetworkPlugin).createSecurityGroupRules(Mockito.any(), Mockito.any(), Mockito.any(), Mockito.any());

        openStackNetworkPlugin.requestInstance(order, openStackV3User);

        PowerMockito.verifyStatic(OpenStackCloudUtils.class, Mockito.times(TestUtils.RUN_ONCE));
        OpenStackCloudUtils.getProjectIdFrom(openStackV3User);
        PowerMockito.verifyStatic(OpenStackCloudUtils.class, Mockito.times(TestUtils.RUN_ONCE));
        OpenStackCloudUtils.getSGNameForPrivateNetwork(NETWORK_ID);
        Mockito.verify(openStackNetworkPlugin, Mockito.times(TestUtils.RUN_ONCE)).createNetwork(Mockito.any(), Mockito.any(), Mockito.any());
        Mockito.verify(openStackNetworkPlugin, Mockito.times(TestUtils.RUN_ONCE)).createSubNet(Mockito.any(), Mockito.any(), Mockito.any(), Mockito.any());
        Mockito.verify(openStackNetworkPlugin, Mockito.times(TestUtils.RUN_ONCE)).createSecurityGroup(Mockito.any(), Mockito.any(), Mockito.any(), Mockito.any());
        Mockito.verify(openStackNetworkPlugin, Mockito.times(TestUtils.RUN_ONCE)).createSecurityGroupRules(Mockito.any(), Mockito.any(), Mockito.any(), Mockito.any());
    }

    @Test
    public void testGetInstance() throws FogbowException{
        Mockito.doReturn(TestUtils.EMPTY_STRING).when(openStackNetworkPlugin).doGetInstance(Mockito.any(), Mockito.any());
        Mockito.doReturn(new NetworkInstance(NETWORK_ID)).when(openStackNetworkPlugin).buildInstance(Mockito.any(), Mockito.any());
        NetworkOrder order = createNetworkOrder(NETWORK_ID, TestUtils.DEFAULT_CIDR, DEFAULT_GATEWAY_INFO, NetworkAllocationMode.DYNAMIC);

        openStackNetworkPlugin.getInstance(order, openStackV3User);

        Mockito.verify(openStackNetworkPlugin, Mockito.times(TestUtils.RUN_ONCE)).doGetInstance(Mockito.any(), Mockito.any());
        Mockito.verify(openStackNetworkPlugin, Mockito.times(TestUtils.RUN_ONCE)).buildInstance(Mockito.any(), Mockito.any());
    }

    @Test
    public void testDeleteInstance() throws FogbowException{
        PowerMockito.mockStatic(OpenStackCloudUtils.class);
        Mockito.doReturn(SECURITY_GROUP_ID).when(openStackNetworkPlugin).retrieveSecurityGroupId(Mockito.any(), Mockito.any());
        Mockito.doNothing().when(openStackNetworkPlugin).doDeleteInstance(Mockito.any(), Mockito.any(), Mockito.any());
        NetworkOrder order = createNetworkOrder(NETWORK_ID, TestUtils.DEFAULT_CIDR, DEFAULT_GATEWAY_INFO, NetworkAllocationMode.DYNAMIC);

        openStackNetworkPlugin.deleteInstance(order, openStackV3User);

        PowerMockito.verifyStatic(OpenStackCloudUtils.class, Mockito.times(TestUtils.RUN_ONCE));
        OpenStackCloudUtils.getSGNameForPrivateNetwork(order.getInstanceId());
        Mockito.verify(openStackNetworkPlugin, Mockito.times(TestUtils.RUN_ONCE)).retrieveSecurityGroupId(Mockito.any(), Mockito.any());
        Mockito.verify(openStackNetworkPlugin, Mockito.times(TestUtils.RUN_ONCE)).doDeleteInstance(Mockito.any(), Mockito.any(), Mockito.any());
    }

    @Test
    public void testDoGetInstance() throws FogbowException{
        NetworkOrder order = createNetworkOrder(NETWORK_ID, TestUtils.DEFAULT_CIDR, DEFAULT_GATEWAY_INFO, NetworkAllocationMode.DYNAMIC);
        Mockito.doReturn(order.getId()).when(openStackNetworkPlugin).doGetRequest(Mockito.any(), Mockito.any(), Mockito.any());

        openStackNetworkPlugin.doGetInstance(order, openStackV3User);

        Mockito.verify(openStackNetworkPlugin, Mockito.times(TestUtils.RUN_ONCE)).doGetRequest(Mockito.any(), Mockito.any(), Mockito.any());
    }

    @Test
    public void testDoGetRequest() throws FogbowException, HttpResponseException{
        Mockito.doReturn(TestUtils.EMPTY_STRING).when(openStackHttpClient).doGetRequest(Mockito.any(), Mockito.any());

        openStackNetworkPlugin.doGetRequest(openStackV3User, NETWORK_NEUTRONV2_URL_KEY, TestUtils.EMPTY_STRING);

        Mockito.verify(openStackHttpClient, Mockito.times(TestUtils.RUN_ONCE)).doGetRequest(Mockito.any(), Mockito.any());
    }

    @Test
    public void testDoGetRequestInExceptionCase() throws FogbowException, HttpResponseException{
        PowerMockito.mockStatic(OpenStackHttpToFogbowExceptionMapper.class);
        HttpResponseException exception = new HttpResponseException(500, TestUtils.EMPTY_STRING);
        Mockito.doThrow(exception).when(openStackHttpClient).doGetRequest(Mockito.any(), Mockito.any());

        try {
            openStackNetworkPlugin.doGetRequest(openStackV3User, NETWORK_NEUTRONV2_URL_KEY, TestUtils.EMPTY_STRING);
        } catch (FogbowException ex) {
            PowerMockito.verifyStatic(OpenStackHttpToFogbowExceptionMapper.class);
            OpenStackHttpToFogbowExceptionMapper.map(exception);
        }

        Mockito.verify(openStackHttpClient, Mockito.times(TestUtils.RUN_ONCE)).doGetRequest(Mockito.any(), Mockito.any());
    }

    //test case: Tests if the dns list will be returned as expected
    @Test
    public void testSetDnsList() {
        //set up
        String dnsOne = "one";
        String dnsTwo = "Two";
        this.properties.put(OpenStackNetworkPlugin.KEY_DNS_NAMESERVERS, dnsOne + "," + dnsTwo);

        //exercise
        this.openStackNetworkPlugin.setDNSList(this.properties);

        //verify
        Assert.assertEquals(2, this.openStackNetworkPlugin.getDnsList().length);
        Assert.assertEquals(dnsOne, this.openStackNetworkPlugin.getDnsList()[0]);
        Assert.assertEquals(dnsTwo, this.openStackNetworkPlugin.getDnsList()[1]);
    }

    //test case: Tests if the json to request subnet was generated as expected
    @Test
    public void testGenerateJsonEntityToCreateSubnet() throws JSONException {
        //set up
        String dnsOne = "one";
        String dnsTwo = "Two";
        this.properties.put(OpenStackNetworkPlugin.KEY_DNS_NAMESERVERS, dnsOne + "," + dnsTwo);
        this.openStackNetworkPlugin.setDNSList(this.properties);
        String networkId = "networkId";
        String address = "10.10.10.0/24";
        String gateway = "10.10.10.11";
        NetworkOrder order = createNetworkOrder(networkId, address, gateway, NetworkAllocationMode.DYNAMIC);

        //exercise
        String generateJsonEntityToCreateSubnet = this.openStackNetworkPlugin
                .generateJsonEntityToCreateSubnet(order.getId(), DEFAULT_PROJECT_ID, order);

        //verify
        String subnetJson = generateJsonEntityToCreateSubnet;
        JSONObject subnetJsonObject = new JSONObject(subnetJson).optJSONObject(OpenStackConstants.Network.SUBNET_KEY_JSON);
        Assert.assertEquals(DEFAULT_PROJECT_ID, subnetJsonObject.optString(OpenStackConstants.Network.PROJECT_ID_KEY_JSON));
        // ToDo: check if this assertion is needed
        //Assert.assertTrue(subnetJsonObject.optString(OpenStackConstants.Network.NAME_KEY_JSON).contains(SystemConstants.DEFAULT_NETWORK_NAME));
        Assert.assertEquals(order.getId(), subnetJsonObject.optString(OpenStackConstants.Network.NETWORK_ID_KEY_JSON));
        Assert.assertEquals(order.getCidr(), subnetJsonObject.optString(OpenStackConstants.Network.CIDR_KEY_JSON));
        Assert.assertEquals(order.getGateway(), subnetJsonObject.optString(OpenStackConstants.Network.GATEWAY_IP_KEY_JSON));
        Assert.assertEquals(true, subnetJsonObject.optBoolean(OpenStackConstants.Network.ENABLE_DHCP_KEY_JSON));
        Assert.assertEquals(OpenStackNetworkPlugin.DEFAULT_IP_VERSION,
                subnetJsonObject.optInt(OpenStackConstants.Network.IP_VERSION_KEY_JSON));
        Assert.assertEquals(dnsOne, subnetJsonObject.optJSONArray(OpenStackNetworkPlugin.KEY_DNS_NAMESERVERS).get(0));
        Assert.assertEquals(dnsTwo, subnetJsonObject.optJSONArray(OpenStackNetworkPlugin.KEY_DNS_NAMESERVERS).get(1));
    }

    //test case: Tests if the json to request subnet was generated as expected, when address is not provided.
    @Test
    public void testGenerateJsonEntityToCreateSubnetDefaultAddress() throws JSONException {
        //set up
        String networkId = "networkId";
        NetworkOrder order = createNetworkOrder(networkId, null, null, null);

        //exercise
        String entityToCreateSubnet = this.openStackNetworkPlugin
                .generateJsonEntityToCreateSubnet(order.getId(), DEFAULT_PROJECT_ID, order);
        JSONObject generateJsonEntityToCreateSubnet = new JSONObject(entityToCreateSubnet);

        //verify
        JSONObject subnetJsonObject = generateJsonEntityToCreateSubnet
                .optJSONObject(OpenStackConstants.Network.SUBNET_KEY_JSON);
        Assert.assertEquals(OpenStackNetworkPlugin.DEFAULT_NETWORK_CIDR,
                subnetJsonObject.optString(OpenStackConstants.Network.CIDR_KEY_JSON));
    }

    //test case: Tests if the json to request subnet was generated as expected, when dns is not provided.
    @Test
    public void testGenerateJsonEntityToCreateSubnetDefaultDns() throws JSONException {
        //set up
        String networkId = "networkId";
        NetworkOrder order = createNetworkOrder(networkId, null, null, null);

        //exercise
        String entityToCreateSubnet = this.openStackNetworkPlugin
                .generateJsonEntityToCreateSubnet(order.getId(), DEFAULT_PROJECT_ID, order);
        JSONObject generateJsonEntityToCreateSubnet = new JSONObject(entityToCreateSubnet);

        //verify
        JSONObject subnetJsonObject = generateJsonEntityToCreateSubnet
                .optJSONObject(OpenStackConstants.Network.SUBNET_KEY_JSON);
        Assert.assertEquals(OpenStackNetworkPlugin.DEFAULT_DNS_NAME_SERVERS[0],
                subnetJsonObject.optJSONArray(OpenStackNetworkPlugin.KEY_DNS_NAMESERVERS).get(0));
        Assert.assertEquals(OpenStackNetworkPlugin.DEFAULT_DNS_NAME_SERVERS[1],
                subnetJsonObject.optJSONArray(OpenStackNetworkPlugin.KEY_DNS_NAMESERVERS).get(1));
    }

    //test case: Tests if the json to request subnet was generated as expected, when a static allocation is required.
    @Test
    public void testGenerateJsonEntityToCreateSubnetStaticAllocation() throws JSONException {
        //set up
        String networkId = "networkId";
        NetworkOrder order = createNetworkOrder(networkId, null, null, NetworkAllocationMode.STATIC);

        //exercise
        String entityToCreateSubnet = this.openStackNetworkPlugin
                .generateJsonEntityToCreateSubnet(order.getId(), DEFAULT_PROJECT_ID, order);
        JSONObject generateJsonEntityToCreateSubnet = new JSONObject(entityToCreateSubnet);

        //verify
        JSONObject subnetJsonObject = generateJsonEntityToCreateSubnet
                .optJSONObject(OpenStackConstants.Network.SUBNET_KEY_JSON);
        Assert.assertEquals(false, subnetJsonObject.optBoolean(OpenStackConstants.Network.ENABLE_DHCP_KEY_JSON));
    }

    //test case: Tests if the json to request subnet was generated as expected, when gateway is not provided.
    @Test
    public void testGenerateJsonEntityToCreateSubnetWithoutGateway() throws JSONException {
        //set up
        String networkId = "networkId";
        NetworkOrder order = createNetworkOrder(networkId, null, null, null);

        //exercise
        String entityToCreateSubnet = this.openStackNetworkPlugin
                .generateJsonEntityToCreateSubnet(order.getId(), DEFAULT_PROJECT_ID, order);
        JSONObject generateJsonEntityToCreateSubnet = new JSONObject(entityToCreateSubnet);

        //verify
        JSONObject subnetJsonObject = generateJsonEntityToCreateSubnet
                .optJSONObject(OpenStackConstants.Network.SUBNET_KEY_JSON);
        Assert.assertTrue(subnetJsonObject.optString(OpenStackConstants.Network.GATEWAY_IP_KEY_JSON).isEmpty());
    }

    //getInstance tests

    //test case: Tests get networkId from json response
    @Test
    public void testGetNetworkIdFromJson() throws JSONException, UnexpectedException {
        //set up
        String networkId = "networkId00";
        JSONObject networkContentJsonObject = new JSONObject();
        networkContentJsonObject.put(OpenStackConstants.Network.ID_KEY_JSON, networkId);
        JSONObject networkJsonObject = new JSONObject();

        //exercise
        networkJsonObject.put(OpenStackConstants.Network.NETWORK_KEY_JSON, networkContentJsonObject);

        //verify
        Assert.assertEquals(networkId,
                this.openStackNetworkPlugin.getNetworkIdFromJson(networkJsonObject.toString()));
    }

    //test case: Tests get instance from json response
    @Test
    public void testGetInstanceFromJson() throws JSONException, IOException, FogbowException {
        //set up
        DatabaseManager.getInstance().setAuditableOrderStateChangeService(Mockito.mock(AuditableOrderStateChangeService.class));

        String networkId = "networkId00";
        String networkName = "netName";
        String subnetId = "subnetId00";
        String vlan = "vlan00";
        String gatewayIp = "10.10.10.10";
        String cidr = "10.10.10.0/24";
        // Generating network response string
        JSONObject networkContentJsonObject = generateJsonResponseForNetwork(networkId, networkName, subnetId, vlan);
        JSONObject networkJsonObject = new JSONObject();
        networkJsonObject.put(OpenStackConstants.Network.NETWORK_KEY_JSON, networkContentJsonObject);

        // Generating subnet response string
        JSONObject subnetContentJsonObject = generateJsonResponseForSubnet(gatewayIp, cidr);
        JSONObject subnetJsonObject = new JSONObject();
        subnetJsonObject.put(OpenStackConstants.Network.SUBNET_KEY_JSON, subnetContentJsonObject);

        Mockito.doReturn(networkJsonObject.toString()).doReturn(subnetJsonObject.toString()).when(this.openStackHttpClient).doGetRequest(Mockito.anyString(), Mockito.any(OpenStackV3User.class));

        NetworkOrder networkOrder = new NetworkOrder();
        networkOrder.setInstanceId("instanceId00");

        //exercise
        NetworkInstance instance = this.openStackNetworkPlugin.getInstance(networkOrder, this.openStackV3User);

        //verify
        Assert.assertEquals(networkId, instance.getId());
        Assert.assertEquals(networkName, instance.getName());
        Assert.assertEquals(vlan, instance.getvLAN());
        Assert.assertEquals(OpenStackStateMapper.ACTIVE_STATUS, instance.getCloudState());
        Assert.assertEquals(gatewayIp, instance.getGateway());
        Assert.assertEquals(cidr, instance.getCidr());
        Assert.assertEquals(NetworkAllocationMode.DYNAMIC, instance.getAllocationMode());
    }

    private NetworkOrder createNetworkOrder(String networkId, String address, String gateway,
                                            NetworkAllocationMode allocation) {
        String requestingMember = "fake-requesting-member";
        String providingMember = "fake-providing-member";
        String name = "name";
        NetworkOrder order = new NetworkOrder(providingMember,
                "default", name, gateway, address, allocation);
        order.setInstanceId(networkId);
        return order;
    }

    private NetworkOrder createEmptyOrder() {
        return new NetworkOrder(null, null, null, "default", null, null, null, null);
    }

    private JSONObject generateJsonResponseForSubnet(String gatewayIp, String cidr) {
        JSONObject subnetContentJsonObject = new JSONObject();

        subnetContentJsonObject.put(OpenStackConstants.Network.GATEWAY_IP_KEY_JSON, gatewayIp);
        subnetContentJsonObject.put(OpenStackConstants.Network.ENABLE_DHCP_KEY_JSON, true);
        subnetContentJsonObject.put(OpenStackConstants.Network.CIDR_KEY_JSON, cidr);

        return subnetContentJsonObject;
    }

    private JSONObject generateJsonResponseForNetwork(String networkId, String networkName, String subnetId, String vlan) {
        JSONObject networkContentJsonObject = new JSONObject();

        networkContentJsonObject.put(OpenStackConstants.Network.ID_KEY_JSON, networkId);
        networkContentJsonObject.put(OpenStackConstants.Network.PROVIDER_SEGMENTATION_ID_KEY_JSON, vlan);
        networkContentJsonObject.put(OpenStackConstants.Network.STATUS_KEY_JSON,
                OpenStackStateMapper.ACTIVE_STATUS);
        networkContentJsonObject.put(OpenStackConstants.Network.NAME_KEY_JSON, networkName);
        JSONArray subnetJsonArray = new JSONArray(Arrays.asList(new String[]{subnetId}));
        networkContentJsonObject.put(OpenStackConstants.Network.SUBNETS_KEY_JSON, subnetJsonArray);

        return networkContentJsonObject;
    }

    private JSONObject createSecurityGroupGetResponse(String id) {
        JSONObject jsonObject = new JSONObject();
        JSONArray securityGroups = new JSONArray();
        JSONObject securityGroupInfo = new JSONObject();
        securityGroupInfo.put(OpenStackConstants.Network.PROJECT_ID_KEY_JSON, "fake-project-id");
        securityGroupInfo.put(OpenStackNetworkPlugin.QUERY_NAME, "fake-name");
        securityGroupInfo.put(OpenStackConstants.Network.ID_KEY_JSON, id);
        securityGroups.put(securityGroupInfo);
        jsonObject.put(OpenStackConstants.Network.SECURITY_GROUPS_KEY_JSON, securityGroups);
        return jsonObject;
    }
}
