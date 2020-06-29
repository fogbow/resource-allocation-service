package cloud.fogbow.ras.core.plugins.interoperability.openstack.network.v2;

import cloud.fogbow.common.constants.OpenStackConstants;
import cloud.fogbow.common.exceptions.FogbowException;
import cloud.fogbow.common.exceptions.InstanceNotFoundException;
import cloud.fogbow.common.exceptions.InternalServerErrorException;
import cloud.fogbow.common.util.HomeDir;
import cloud.fogbow.common.util.connectivity.HttpErrorConditionToFogbowExceptionMapper;
import cloud.fogbow.common.util.connectivity.cloud.openstack.OpenStackHttpClient;
import cloud.fogbow.ras.constants.SystemConstants;
import cloud.fogbow.ras.core.BaseUnitTests;
import cloud.fogbow.ras.core.PropertiesHolder;
import cloud.fogbow.ras.core.TestUtils;
import cloud.fogbow.ras.core.datastore.DatabaseManager;
import cloud.fogbow.ras.core.datastore.services.AuditableOrderStateChangeService;
import cloud.fogbow.ras.core.models.NetworkAllocationMode;
import cloud.fogbow.ras.api.http.response.NetworkInstance;
import cloud.fogbow.ras.core.models.orders.NetworkOrder;
import cloud.fogbow.ras.core.plugins.interoperability.openstack.util.OpenStackPluginUtils;
import cloud.fogbow.ras.core.plugins.interoperability.openstack.util.OpenStackStateMapper;
import cloud.fogbow.common.models.OpenStackV3User;
import cloud.fogbow.ras.core.plugins.interoperability.openstack.util.v2.OpenStackCloudUtils;
import com.google.gson.JsonSyntaxException;
import org.apache.http.client.HttpResponseException;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.core.classloader.annotations.PrepareForTest;

import java.io.File;
import java.io.IOException;
import java.util.Arrays;
import java.util.Properties;

@PrepareForTest({DatabaseManager.class, OpenStackPluginUtils.class, OpenStackCloudUtils.class,
        HttpErrorConditionToFogbowExceptionMapper.class, CreateNetworkResponse.class,
        CreateSecurityGroupResponse.class, GetSubnetResponse.class})
public class OpenStackNetworkPluginTest extends BaseUnitTests {

    private static final String NETWORK_NEUTRONV2_URL_KEY = "openstack_neutron_v2_url";

    private static final String DEFAULT_GATEWAY_INFO = "000000-gateway_info";
    private static final String DEFAULT_PROJECT_ID = "PROJECT_ID";
    private static final String DEFAULT_NETWORK_URL = "http://localhost:0000";
    private static final String SECURITY_GROUP_ID = "fake-sg-id";
    private static final String NETWORK_ID = "networkId";

    private static final String FAKE_TOKEN_VALUE = "fake-token-value";
    private static final String FAKE_USER_ID = "fake-user-id";
    private static final String FAKE_NAME = "fake-name";
    private static final String FAKE_PROJECT_ID = "fake-project-id";
    private static final String FAKE_TENANT_ID = "tenant-id";
    private static final String FAKE_SUBNET_ID = "subnet-id";
    private static final String FAKE_REQUESTING_MEMBER = "fake-requesting-member";
    private static final String FAKE_PROVIDING_MEMBER = "fake_providing-member";
    private OpenStackNetworkPlugin openStackNetworkPlugin;
    private OpenStackV3User openStackV3User;
    private Properties properties;
    private OpenStackHttpClient openStackHttpClient;
    private static int INTERNAL_SERVER_ERROR_HTTP_CODE = 500;

    @Before
    public void setUp() throws Exception {
        testUtils.mockReadOrdersFromDataBase();
        PropertiesHolder propertiesHolder = PropertiesHolder.getInstance();
        this.properties = propertiesHolder.getProperties();
        this.properties.put(OpenStackPluginUtils.EXTERNAL_NETWORK_ID_KEY, DEFAULT_GATEWAY_INFO);
        this.properties.put(NETWORK_NEUTRONV2_URL_KEY, DEFAULT_NETWORK_URL);
        String cloudConfPath = HomeDir.getPath() + SystemConstants.CLOUDS_CONFIGURATION_DIRECTORY_NAME + File.separator
                + "default" + File.separator + SystemConstants.CLOUD_SPECIFICITY_CONF_FILE_NAME;
        this.openStackNetworkPlugin = Mockito.spy(new OpenStackNetworkPlugin(cloudConfPath));

        this.openStackHttpClient = Mockito.spy(new OpenStackHttpClient());
        this.openStackNetworkPlugin.setClient(this.openStackHttpClient);
        this.openStackV3User = new OpenStackV3User(FAKE_USER_ID, FAKE_NAME, FAKE_TOKEN_VALUE, FAKE_PROJECT_ID);

        PowerMockito.mockStatic(HttpErrorConditionToFogbowExceptionMapper.class);
        PowerMockito.doCallRealMethod().when(HttpErrorConditionToFogbowExceptionMapper.class, "map", Mockito.any());
    }

    //test case: Check if the method makes the expected calls.
    @Test
    public void testRequestInstance() throws FogbowException {
        //setup
        PowerMockito.mockStatic(OpenStackPluginUtils.class);
        NetworkOrder order = testUtils.createNetworkOrder(FAKE_REQUESTING_MEMBER, FAKE_PROVIDING_MEMBER);
        CreateNetworkResponse createNetworkResponse = new CreateNetworkResponse(new CreateNetworkResponse.Network(NETWORK_ID));
        Mockito.doReturn(createNetworkResponse).when(openStackNetworkPlugin).createNetwork(Mockito.any(), Mockito.any(), Mockito.any());
        Mockito.doReturn(new CreateSecurityGroupResponse(new CreateSecurityGroupResponse.SecurityGroup(SECURITY_GROUP_ID))).when(openStackNetworkPlugin)
                .createSecurityGroup(Mockito.any(), Mockito.any(), Mockito.any(), Mockito.any());
        Mockito.doNothing().when(openStackNetworkPlugin).createSubNet(Mockito.any(), Mockito.any(), Mockito.any(), Mockito.any());
        Mockito.doNothing().when(openStackNetworkPlugin).createSecurityGroupRules(Mockito.any(), Mockito.any(), Mockito.any(), Mockito.any());

        //exercise
        openStackNetworkPlugin.requestInstance(order, openStackV3User);

        //verify
        PowerMockito.verifyStatic(OpenStackPluginUtils.class, Mockito.times(TestUtils.RUN_ONCE));
        OpenStackPluginUtils.getProjectIdFrom(openStackV3User);
        PowerMockito.verifyStatic(OpenStackPluginUtils.class, Mockito.times(TestUtils.RUN_ONCE));
        OpenStackPluginUtils.getNetworkSecurityGroupName(NETWORK_ID);
        Mockito.verify(openStackNetworkPlugin, Mockito.times(TestUtils.RUN_ONCE)).createNetwork(Mockito.any(), Mockito.any(), Mockito.any());
        Mockito.verify(openStackNetworkPlugin, Mockito.times(TestUtils.RUN_ONCE)).createSubNet(Mockito.any(), Mockito.any(), Mockito.any(), Mockito.any());
        Mockito.verify(openStackNetworkPlugin, Mockito.times(TestUtils.RUN_ONCE)).createSecurityGroup(Mockito.any(), Mockito.any(), Mockito.any(), Mockito.any());
        Mockito.verify(openStackNetworkPlugin, Mockito.times(TestUtils.RUN_ONCE)).createSecurityGroupRules(Mockito.any(), Mockito.any(), Mockito.any(), Mockito.any());
    }

    //test case: Check if the method makes the expected calls
    @Test
    public void testGetInstance() throws FogbowException {
        //setup
        Mockito.doReturn(TestUtils.EMPTY_STRING).when(openStackNetworkPlugin).doGetInstance(Mockito.any(), Mockito.any());
        Mockito.doReturn(new NetworkInstance(NETWORK_ID)).when(openStackNetworkPlugin).buildInstance(Mockito.any(), Mockito.any());
        NetworkOrder order = createNetworkOrder(NETWORK_ID, TestUtils.DEFAULT_CIDR, DEFAULT_GATEWAY_INFO, NetworkAllocationMode.DYNAMIC);

        //exercise
        openStackNetworkPlugin.getInstance(order, openStackV3User);

        //verify
        Mockito.verify(openStackNetworkPlugin, Mockito.times(TestUtils.RUN_ONCE)).doGetInstance(Mockito.any(), Mockito.any());
        Mockito.verify(openStackNetworkPlugin, Mockito.times(TestUtils.RUN_ONCE)).buildInstance(Mockito.any(), Mockito.any());
    }

    //test case: Check if the method makes the expected calls
    @Test
    public void testDeleteInstance() throws FogbowException {
        //setup
        PowerMockito.mockStatic(OpenStackPluginUtils.class);
        Mockito.doReturn(SECURITY_GROUP_ID).when(openStackNetworkPlugin).retrieveSecurityGroupId(Mockito.any(), Mockito.any());
        Mockito.doNothing().when(openStackNetworkPlugin).doDeleteInstance(Mockito.any(), Mockito.any(), Mockito.any());
        NetworkOrder order = createNetworkOrder(NETWORK_ID, TestUtils.DEFAULT_CIDR, DEFAULT_GATEWAY_INFO, NetworkAllocationMode.DYNAMIC);
        //exercise
        openStackNetworkPlugin.deleteInstance(order, openStackV3User);
        //verify
        PowerMockito.verifyStatic(OpenStackPluginUtils.class, Mockito.times(TestUtils.RUN_ONCE));
        OpenStackPluginUtils.getNetworkSecurityGroupName(order.getInstanceId());
        Mockito.verify(openStackNetworkPlugin, Mockito.times(TestUtils.RUN_ONCE)).retrieveSecurityGroupId(Mockito.any(), Mockito.any());
        Mockito.verify(openStackNetworkPlugin, Mockito.times(TestUtils.RUN_ONCE)).doDeleteInstance(Mockito.any(), Mockito.any(), Mockito.any());
    }

    //test case: test if the method makes the expected call.
    @Test
    public void testDoGetInstance() throws FogbowException {
        //setup
        NetworkOrder order = createNetworkOrder(NETWORK_ID, TestUtils.DEFAULT_CIDR, DEFAULT_GATEWAY_INFO, NetworkAllocationMode.DYNAMIC);
        Mockito.doReturn(order.getId()).when(openStackNetworkPlugin).doGetRequest(Mockito.any(), Mockito.any());
        //exercise
        openStackNetworkPlugin.doGetInstance(NETWORK_NEUTRONV2_URL_KEY, openStackV3User);
        //verify
        Mockito.verify(openStackNetworkPlugin, Mockito.times(TestUtils.RUN_ONCE)).doGetRequest(Mockito.any(), Mockito.any());
    }

    //test case: test if the method make the expected call
    @Test
    public void testDoGetRequest() throws FogbowException, HttpResponseException {
        //setup
        Mockito.doReturn(TestUtils.EMPTY_STRING).when(openStackHttpClient).doGetRequest(Mockito.any(), Mockito.any());
        //exercise
        openStackNetworkPlugin.doGetRequest(openStackV3User, NETWORK_NEUTRONV2_URL_KEY);
        //verify
        Mockito.verify(openStackHttpClient, Mockito.times(TestUtils.RUN_ONCE)).doGetRequest(Mockito.any(), Mockito.any());
    }

    //test case: test if the method calls the ExceptionMapper when doGetRequest throws an Http exception.
    @Test
    public void testDoGetRequestInExceptionCase() throws Exception {
        //setup
        FogbowException exception = new FogbowException(TestUtils.EMPTY_STRING);
        Mockito.doThrow(exception).when(openStackHttpClient).doGetRequest(Mockito.any(), Mockito.any());

        try {
            //exercise
            openStackNetworkPlugin.doGetRequest(openStackV3User, NETWORK_NEUTRONV2_URL_KEY);
            Assert.fail();
        } catch (FogbowException ex) {
            //verify
            Mockito.verify(openStackHttpClient, Mockito.times(TestUtils.RUN_ONCE)).doGetRequest(Mockito.any(), Mockito.any());
        }
    }

    //test case: Check if the method makes the expected calls
    @Test
    public void testDoDeleteInstance() throws FogbowException {
        //setup
        Mockito.doNothing().when(openStackNetworkPlugin).removeNetwork(Mockito.any(), Mockito.any());
        Mockito.doNothing().when(openStackNetworkPlugin).doDeleteSecurityGroup(Mockito.any(), Mockito.any());
        //exercise
        openStackNetworkPlugin.doDeleteInstance(NETWORK_ID, SECURITY_GROUP_ID, openStackV3User);
        //verify
        Mockito.verify(openStackNetworkPlugin, Mockito.times(TestUtils.RUN_ONCE)).removeNetwork(Mockito.any(), Mockito.any());
        Mockito.verify(openStackNetworkPlugin, Mockito.times(TestUtils.RUN_ONCE)).doDeleteSecurityGroup(Mockito.any(), Mockito.any());
    }

    //test case: Check if the method makes the expected call
    @Test
    public void testDoDeleteInstanceWhenInstanceNotFound() throws FogbowException {
        //setup
        Mockito.doThrow(new InstanceNotFoundException()).when(openStackNetworkPlugin).removeNetwork(Mockito.any(), Mockito.any());
        Mockito.doNothing().when(openStackNetworkPlugin).doDeleteSecurityGroup(Mockito.any(), Mockito.any());
        //exercise
        openStackNetworkPlugin.doDeleteInstance(NETWORK_ID, SECURITY_GROUP_ID, openStackV3User);
        //verify
        Mockito.verify(openStackNetworkPlugin, Mockito.times(TestUtils.RUN_ONCE)).doDeleteSecurityGroup(Mockito.any(), Mockito.any());
    }

    //test case: check if the method throws a FogbowException when the removeNetwork do the same.
    @Test(expected = FogbowException.class)//verify
    public void testDoDeleteInstanceWhenFogbowException() throws FogbowException {
        //setup
        Mockito.doThrow(new FogbowException("")).when(openStackNetworkPlugin).removeNetwork(Mockito.any(), Mockito.any());
        Mockito.doNothing().when(openStackNetworkPlugin).doDeleteSecurityGroup(Mockito.any(), Mockito.any());

        //exercise
        openStackNetworkPlugin.doDeleteInstance(NETWORK_ID, SECURITY_GROUP_ID, openStackV3User);
    }

    //test case: Check if the method makes the expected call.
    @Test
    public void doDeleteSecurityGroup() throws FogbowException {
        //setup
        Mockito.doNothing().when(openStackNetworkPlugin).removeSecurityGroup(Mockito.any(), Mockito.any());
        //exercise
        openStackNetworkPlugin.doDeleteSecurityGroup(SECURITY_GROUP_ID, openStackV3User);
        //verify
        Mockito.verify(openStackNetworkPlugin, Mockito.times(TestUtils.RUN_ONCE)).removeSecurityGroup(Mockito.any(), Mockito.any());
    }

    //test case: check if the method makes the expected call when an exception is thrown.
    @Test
    public void doDeleteSecurityGroupWhenFogbowException() throws FogbowException {
        //setup
        Mockito.doThrow(new FogbowException("")).when(openStackNetworkPlugin).removeSecurityGroup(Mockito.any(), Mockito.any());

        try {
            //exercise
            openStackNetworkPlugin.doDeleteSecurityGroup(SECURITY_GROUP_ID, openStackV3User);
            Assert.fail();
        } catch (FogbowException ex) {
            //verify
            Mockito.verify(openStackNetworkPlugin, Mockito.times(TestUtils.RUN_ONCE)).removeSecurityGroup(Mockito.any(), Mockito.any());
        }
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

    //test case: check if the method makes the expected calls
    @Test
    public void testCreateNetwork() throws FogbowException, HttpResponseException {
        //setup
        PowerMockito.mockStatic(CreateNetworkResponse.class);
        Mockito.doReturn(TestUtils.EMPTY_STRING).when(openStackHttpClient).doPostRequest(Mockito.any(), Mockito.any(), Mockito.any());
        //exercise
        openStackNetworkPlugin.createNetwork(FAKE_NAME, openStackV3User, FAKE_TENANT_ID);
        //verify
        PowerMockito.verifyStatic(CreateNetworkResponse.class, Mockito.times(TestUtils.RUN_ONCE));
        CreateNetworkResponse.fromJson(TestUtils.EMPTY_STRING);
        Mockito.verify(openStackHttpClient, Mockito.times(TestUtils.RUN_ONCE)).doPostRequest(Mockito.any(), Mockito.any(), Mockito.any());
    }

    //test case: check if the method throws an InternalServerErrorException when a JsonSyntaxException occurs.
    @Test(expected = InternalServerErrorException.class)//verify
    public void testCreateNetworkWhenJsonException() throws FogbowException, HttpResponseException {
        //setup
        PowerMockito.mockStatic(CreateNetworkResponse.class);
        PowerMockito.when(CreateNetworkResponse.fromJson(Mockito.any())).thenThrow(new JsonSyntaxException(TestUtils.EMPTY_STRING));
        Mockito.doReturn(TestUtils.EMPTY_STRING).when(openStackHttpClient).doPostRequest(Mockito.any(), Mockito.any(), Mockito.any());
        //exercise
        openStackNetworkPlugin.createNetwork(FAKE_NAME, openStackV3User, FAKE_TENANT_ID);
    }

    //test case: check if the method makes the expected calls when a HttpException is thrown.
    @Test
    public void testCreateNetworkWhenHttpException() throws FogbowException, HttpResponseException {
        //setup
        PowerMockito.mockStatic(CreateNetworkResponse.class);
        FogbowException fogbowException = new FogbowException(TestUtils.EMPTY_STRING);
        Mockito.doThrow(fogbowException).when(openStackHttpClient).doPostRequest(Mockito.any(), Mockito.any(), Mockito.any());

        try {
            //exercise
            openStackNetworkPlugin.createNetwork(FAKE_NAME, openStackV3User, FAKE_TENANT_ID);
            Assert.fail();
        } catch (FogbowException ex) {
            //verify
            PowerMockito.verifyStatic(CreateNetworkResponse.class, Mockito.times(0));
            CreateNetworkResponse.fromJson(TestUtils.EMPTY_STRING);
            PowerMockito.verifyStatic(HttpErrorConditionToFogbowExceptionMapper.class, Mockito.times(TestUtils.RUN_ONCE));
        }
    }

    //test case: check if the method makes the expected calls.
    @Test
    public void testCreateSubnet() throws FogbowException, HttpResponseException {
        //setup
        Mockito.doReturn(TestUtils.EMPTY_STRING).when(openStackNetworkPlugin).generateJsonEntityToCreateSubnet(Mockito.any(), Mockito.any(), Mockito.any());
        Mockito.doReturn(TestUtils.EMPTY_STRING).when(openStackHttpClient).doPostRequest(Mockito.any(), Mockito.any(), Mockito.any());
        NetworkOrder order = createNetworkOrder(NETWORK_ID, TestUtils.DEFAULT_CIDR, DEFAULT_GATEWAY_INFO, NetworkAllocationMode.DYNAMIC);
        //exercise
        openStackNetworkPlugin.createSubNet(openStackV3User, order, NETWORK_ID, FAKE_TENANT_ID);
        //verify
        Mockito.verify(openStackNetworkPlugin, Mockito.times(TestUtils.RUN_ONCE)).generateJsonEntityToCreateSubnet(Mockito.any(), Mockito.any(), Mockito.any());
        Mockito.verify(openStackHttpClient, Mockito.times(TestUtils.RUN_ONCE)).doPostRequest(Mockito.any(), Mockito.any(), Mockito.any());
    }

    //test case: check if the method makes the expected calls when a HttpException is thrown
    @Test
    public void testCreateSubnetWhenHttpException() throws FogbowException, HttpResponseException {
        //setup
        Mockito.doReturn(TestUtils.EMPTY_STRING).when(openStackNetworkPlugin).generateJsonEntityToCreateSubnet(Mockito.any(), Mockito.any(), Mockito.any());
        FogbowException fogbowException = new FogbowException(TestUtils.EMPTY_STRING);
        Mockito.doThrow(fogbowException).when(openStackHttpClient).doPostRequest(Mockito.any(), Mockito.any(), Mockito.any());
        Mockito.doNothing().when(openStackNetworkPlugin).removeNetwork(Mockito.any(), Mockito.any());
        NetworkOrder order = createNetworkOrder(NETWORK_ID, TestUtils.DEFAULT_CIDR, DEFAULT_GATEWAY_INFO, NetworkAllocationMode.DYNAMIC);

        try {
            //exercise
            openStackNetworkPlugin.createSubNet(openStackV3User, order, NETWORK_ID, FAKE_TENANT_ID);
        } catch (FogbowException ex) {
            //verify
            Mockito.verify(openStackHttpClient, Mockito.times(TestUtils.RUN_ONCE)).doPostRequest(Mockito.any(), Mockito.any(), Mockito.any());
            Mockito.verify(openStackNetworkPlugin, Mockito.times(TestUtils.RUN_ONCE)).generateJsonEntityToCreateSubnet(Mockito.any(), Mockito.any(), Mockito.any());
            Mockito.verify(openStackNetworkPlugin, Mockito.times(TestUtils.RUN_ONCE)).removeNetwork(Mockito.any(), Mockito.any());
            PowerMockito.verifyStatic(HttpErrorConditionToFogbowExceptionMapper.class, Mockito.times(TestUtils.RUN_ONCE));
        }
    }

    //test case: check if the method makes the expected calls.
    @Test
    public void testCreateSecurityGroup() throws FogbowException, HttpResponseException {
        //setup
        PowerMockito.mockStatic(CreateSecurityGroupResponse.class);
        Mockito.doReturn(TestUtils.EMPTY_STRING).when(openStackHttpClient).doPostRequest(Mockito.any(), Mockito.any(), Mockito.any());
        //exercise
        openStackNetworkPlugin.createSecurityGroup(openStackV3User, FAKE_NAME, FAKE_TENANT_ID, NETWORK_ID);
        //verify
        PowerMockito.verifyStatic(CreateSecurityGroupResponse.class, Mockito.times(TestUtils.RUN_ONCE));
        CreateSecurityGroupResponse.fromJson(TestUtils.EMPTY_STRING);
        Mockito.verify(openStackHttpClient, Mockito.times(TestUtils.RUN_ONCE)).doPostRequest(Mockito.any(), Mockito.any(), Mockito.any());
    }

    //test case: check if the method makes the expected calls when a httpException is thrown
    @Test
    public void testCreateSecurityGroupWhenHttpException() throws FogbowException, HttpResponseException {
        //setup
        PowerMockito.mockStatic(CreateSecurityGroupResponse.class);
        FogbowException fogbowException = new FogbowException(TestUtils.EMPTY_STRING);
        Mockito.doThrow(fogbowException).when(openStackHttpClient).doPostRequest(Mockito.any(), Mockito.any(), Mockito.any());
        Mockito.doNothing().when(openStackNetworkPlugin).removeNetwork(Mockito.any(), Mockito.any());

        try {
            //exercise
            openStackNetworkPlugin.createSecurityGroup(openStackV3User, FAKE_NAME, FAKE_TENANT_ID, NETWORK_ID);
        } catch (FogbowException ex) {
            //verify
            PowerMockito.verifyStatic(CreateSecurityGroupResponse.class, Mockito.times(0));
            CreateSecurityGroupResponse.fromJson(TestUtils.EMPTY_STRING);
            Mockito.verify(openStackHttpClient, Mockito.times(TestUtils.RUN_ONCE)).doPostRequest(Mockito.any(), Mockito.any(), Mockito.any());
            Mockito.verify(openStackNetworkPlugin, Mockito.times(TestUtils.RUN_ONCE)).removeNetwork(Mockito.any(), Mockito.any());
            PowerMockito.verifyStatic(HttpErrorConditionToFogbowExceptionMapper.class, Mockito.times(TestUtils.RUN_ONCE));
        }
    }

    //test case: check if the method makes the expected calls.
    @Test
    public void testCreateSecurityGroupRules() throws FogbowException, HttpResponseException {
        //setup
        CreateSecurityGroupRuleRequest allTcp = new CreateSecurityGroupRuleRequest.Builder()
                .protocol(TestUtils.TCP_PROTOCOL).build();
        CreateSecurityGroupRuleRequest allUdp = new CreateSecurityGroupRuleRequest.Builder()
                .protocol(TestUtils.UDP_PROTOCOL).build();
        CreateSecurityGroupRuleRequest icmp = new CreateSecurityGroupRuleRequest.Builder()
                .protocol(TestUtils.UDP_PROTOCOL).build();
        Mockito.doReturn(allTcp).when(openStackNetworkPlugin).createAllTcpRuleRequest(Mockito.any(), Mockito.any(), Mockito.eq(TestUtils.TCP_PROTOCOL));
        Mockito.doReturn(allUdp).when(openStackNetworkPlugin).createAllTcpRuleRequest(Mockito.any(), Mockito.any(), Mockito.eq(TestUtils.UDP_PROTOCOL));
        Mockito.doReturn(icmp).when(openStackNetworkPlugin).createIcmpRuleRequest(Mockito.any(), Mockito.any());
        Mockito.doReturn(TestUtils.EMPTY_STRING).when(openStackHttpClient).doPostRequest(Mockito.any(), Mockito.any(), Mockito.any());
        NetworkOrder order = createNetworkOrder(NETWORK_ID, TestUtils.DEFAULT_CIDR, DEFAULT_GATEWAY_INFO, NetworkAllocationMode.DYNAMIC);
        //exercise
        openStackNetworkPlugin.createSecurityGroupRules(order, openStackV3User, NETWORK_ID, SECURITY_GROUP_ID);
        //verify
        Mockito.verify(openStackNetworkPlugin, Mockito.times(2)).createAllTcpRuleRequest(Mockito.any(), Mockito.any(), Mockito.any());
        Mockito.verify(openStackNetworkPlugin, Mockito.times(TestUtils.RUN_ONCE)).createIcmpRuleRequest(Mockito.any(), Mockito.any());
        Mockito.verify(openStackHttpClient, Mockito.times(3)).doPostRequest(Mockito.any(), Mockito.any(), Mockito.any());
    }

    //test case: check if the method makes the expected calls when a HttpException is thrown.
    @Test
    public void testCreateSecurityGroupRulesWhenHttpException() throws FogbowException, HttpResponseException {
        //setup
        CreateSecurityGroupRuleRequest allTcp = new CreateSecurityGroupRuleRequest.Builder()
                .protocol(TestUtils.TCP_PROTOCOL).build();
        CreateSecurityGroupRuleRequest allUdp = new CreateSecurityGroupRuleRequest.Builder()
                .protocol(TestUtils.UDP_PROTOCOL).build();
        CreateSecurityGroupRuleRequest icmp = new CreateSecurityGroupRuleRequest.Builder()
                .protocol(TestUtils.UDP_PROTOCOL).build();
        Mockito.doReturn(allTcp).when(openStackNetworkPlugin).createAllTcpRuleRequest(Mockito.any(), Mockito.any(), Mockito.eq(TestUtils.TCP_PROTOCOL));
        Mockito.doReturn(allUdp).when(openStackNetworkPlugin).createAllTcpRuleRequest(Mockito.any(), Mockito.any(), Mockito.eq(TestUtils.UDP_PROTOCOL));
        Mockito.doReturn(icmp).when(openStackNetworkPlugin).createIcmpRuleRequest(Mockito.any(), Mockito.any());
        FogbowException fogbowException = new FogbowException(TestUtils.EMPTY_STRING);
        Mockito.doThrow(fogbowException).when(openStackHttpClient).doPostRequest(Mockito.any(), Mockito.any(), Mockito.any());
        Mockito.doNothing().when(openStackNetworkPlugin).removeNetwork(Mockito.any(), Mockito.any());
        Mockito.doNothing().when(openStackNetworkPlugin).removeSecurityGroup(Mockito.any(), Mockito.any());
        NetworkOrder order = createNetworkOrder(NETWORK_ID, TestUtils.DEFAULT_CIDR, DEFAULT_GATEWAY_INFO, NetworkAllocationMode.DYNAMIC);

        try {
            //exercise
            openStackNetworkPlugin.createSecurityGroupRules(order, openStackV3User, NETWORK_ID, SECURITY_GROUP_ID);
        } catch (FogbowException ex) {
            //verify
            Mockito.verify(openStackNetworkPlugin, Mockito.times(2)).createAllTcpRuleRequest(Mockito.any(), Mockito.any(), Mockito.any());
            Mockito.verify(openStackNetworkPlugin, Mockito.times(TestUtils.RUN_ONCE)).createIcmpRuleRequest(Mockito.any(), Mockito.any());
            Mockito.verify(openStackHttpClient, Mockito.times(1)).doPostRequest(Mockito.any(), Mockito.any(), Mockito.any());
            Mockito.verify(openStackNetworkPlugin, Mockito.times(TestUtils.RUN_ONCE)).removeNetwork(Mockito.any(), Mockito.any());
            Mockito.verify(openStackNetworkPlugin, Mockito.times(TestUtils.RUN_ONCE)).removeSecurityGroup(Mockito.any(), Mockito.any());
            PowerMockito.verifyStatic(HttpErrorConditionToFogbowExceptionMapper.class, Mockito.times(TestUtils.RUN_ONCE));
        }
    }

    //test case: check if the method makes the expected calls.
    @Test
    public void testRetrieveSecurityGroupId() throws FogbowException {
        //setup
        PowerMockito.mockStatic(OpenStackCloudUtils.class);
        Mockito.doReturn(TestUtils.EMPTY_STRING).when(openStackNetworkPlugin).doGetRequest(Mockito.any(), Mockito.any());
        //exercise
        openStackNetworkPlugin.retrieveSecurityGroupId(SECURITY_GROUP_ID, openStackV3User);
        //verify
        Mockito.verify(openStackNetworkPlugin, Mockito.times(TestUtils.RUN_ONCE)).doGetRequest(Mockito.any(), Mockito.any());
        PowerMockito.verifyStatic(OpenStackCloudUtils.class, Mockito.times(TestUtils.RUN_ONCE));
        OpenStackCloudUtils.getSecurityGroupIdFromGetResponse(TestUtils.EMPTY_STRING);
    }

    //test case: check if the method makes the expected call.
    @Test
    public void testRemoveSecurityGroup() throws FogbowException, HttpResponseException {
        //setup
        Mockito.doNothing().when(openStackHttpClient).doDeleteRequest(Mockito.any(), Mockito.any());
        //exercise
        openStackNetworkPlugin.removeSecurityGroup(openStackV3User, SECURITY_GROUP_ID);
        //verify
        Mockito.verify(openStackHttpClient, Mockito.times(TestUtils.RUN_ONCE)).doDeleteRequest(Mockito.any(), Mockito.any());
    }

    //test case: check if the method makes the expected calls when a HttpException is thrown
    @Test
    public void testRemoveSecurityGroupWhenHttpException() throws FogbowException, HttpResponseException {
        //setup
        PowerMockito.mockStatic(HttpErrorConditionToFogbowExceptionMapper.class);
        FogbowException fogbowException = new FogbowException(TestUtils.EMPTY_STRING);
        Mockito.doThrow(fogbowException).when(openStackHttpClient).doDeleteRequest(Mockito.any(), Mockito.any());

        try {
            //exercise
            openStackNetworkPlugin.removeSecurityGroup(openStackV3User, SECURITY_GROUP_ID);
        } catch (FogbowException ex) {
            //verify
            Mockito.verify(openStackHttpClient, Mockito.times(TestUtils.RUN_ONCE)).doDeleteRequest(Mockito.any(), Mockito.any());
            PowerMockito.verifyStatic(HttpErrorConditionToFogbowExceptionMapper.class, Mockito.times(TestUtils.RUN_ONCE));
        }

    }

    //test case: check if the method makes the expected calls
    @Test
    public void testGetSubnetInformation() throws FogbowException, HttpResponseException {
        //setup
        PowerMockito.mockStatic(GetSubnetResponse.class);
        Mockito.doReturn(TestUtils.EMPTY_STRING).when(openStackHttpClient).doGetRequest(Mockito.any(), Mockito.any());
        //exercise
        openStackNetworkPlugin.getSubnetInformation(openStackV3User, FAKE_SUBNET_ID);
        //verify
        Mockito.verify(openStackHttpClient, Mockito.times(TestUtils.RUN_ONCE)).doGetRequest(Mockito.any(), Mockito.any());
        PowerMockito.verifyStatic(GetSubnetResponse.class, Mockito.times(TestUtils.RUN_ONCE));
        GetSubnetResponse.fromJson(TestUtils.EMPTY_STRING);
    }

    //test case: check if the method makes the expected calls when a HttpException is thrown
    @Test
    public void testGetSubnetInformationWhenHttpException() throws FogbowException, HttpResponseException {
        //setup
        PowerMockito.mockStatic(HttpErrorConditionToFogbowExceptionMapper.class);
        FogbowException fogbowException = new FogbowException(TestUtils.EMPTY_STRING);
        Mockito.doThrow(fogbowException).when(openStackHttpClient).doGetRequest(Mockito.any(), Mockito.any());

        try {
            //exercise
            openStackNetworkPlugin.getSubnetInformation(openStackV3User, FAKE_SUBNET_ID);
        } catch (FogbowException ex) {
            //verify
            Mockito.verify(openStackHttpClient, Mockito.times(TestUtils.RUN_ONCE)).doGetRequest(Mockito.any(), Mockito.any());
            PowerMockito.verifyStatic(HttpErrorConditionToFogbowExceptionMapper.class, Mockito.times(TestUtils.RUN_ONCE));
        }

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
        NetworkOrder order = testUtils.createNetworkOrder(FAKE_REQUESTING_MEMBER, FAKE_PROVIDING_MEMBER);

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
    public void testGetNetworkIdFromJson() throws JSONException, InternalServerErrorException {
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

    //test case: check if the method makes the expected call.
    @Test
    public void testRemoveNetwork() throws FogbowException, HttpResponseException {
        //setup
        Mockito.doNothing().when(openStackHttpClient).doDeleteRequest(Mockito.any(), Mockito.any());
        //exercise
        openStackNetworkPlugin.removeNetwork(openStackV3User, NETWORK_ID);
        //verify
        Mockito.verify(openStackHttpClient, Mockito.times(TestUtils.RUN_ONCE)).doDeleteRequest(Mockito.any(), Mockito.any());
    }

    //test case: check if the method makes the expected calls when a HttpException is thrown.
    @Test
    public void testRemoveNetworkWhenHttpException() throws FogbowException, HttpResponseException {
        //setup
        PowerMockito.mockStatic(HttpErrorConditionToFogbowExceptionMapper.class);
        FogbowException fogbowException = new FogbowException(TestUtils.EMPTY_STRING);
        Mockito.doThrow(fogbowException).when(openStackHttpClient).doDeleteRequest(Mockito.any(), Mockito.any());

        try {
            //exercise
            openStackNetworkPlugin.removeNetwork(openStackV3User, NETWORK_ID);
        } catch (FogbowException ex) {
            //verify
            Mockito.verify(openStackHttpClient, Mockito.times(TestUtils.RUN_ONCE)).doDeleteRequest(Mockito.any(), Mockito.any());
            PowerMockito.verifyStatic(HttpErrorConditionToFogbowExceptionMapper.class, Mockito.times(TestUtils.RUN_ONCE));
        }

    }

    private NetworkOrder createNetworkOrder(String networkId, String address, String gateway,
                                            NetworkAllocationMode allocation) {
        String providingMember = "fake-providing-member";
        String name = "name";
        NetworkOrder order = new NetworkOrder(providingMember,
                "default", name, gateway, address, allocation);
        order.setInstanceId(networkId);
        return order;
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
}
