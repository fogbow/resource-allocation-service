package cloud.fogbow.ras.core.plugins.interoperability.openstack.securityrule.v2;

import cloud.fogbow.common.exceptions.FogbowException;
import cloud.fogbow.common.exceptions.InvalidParameterException;
import cloud.fogbow.common.exceptions.UnexpectedException;
import cloud.fogbow.common.models.OpenStackV3User;
import cloud.fogbow.common.util.HomeDir;
import cloud.fogbow.common.util.PropertiesUtil;
import cloud.fogbow.common.util.connectivity.cloud.openstack.OpenStackHttpClient;
import cloud.fogbow.common.util.connectivity.cloud.openstack.OpenStackHttpToFogbowExceptionMapper;
import cloud.fogbow.ras.api.parameters.SecurityRule;
import cloud.fogbow.ras.constants.Messages;
import cloud.fogbow.ras.constants.SystemConstants;
import cloud.fogbow.ras.core.BaseUnitTests;
import cloud.fogbow.ras.core.TestUtils;
import cloud.fogbow.ras.core.datastore.DatabaseManager;
import cloud.fogbow.ras.core.models.orders.ComputeOrder;
import cloud.fogbow.ras.core.models.orders.NetworkOrder;
import cloud.fogbow.ras.api.http.response.SecurityRuleInstance;
import cloud.fogbow.ras.core.models.orders.PublicIpOrder;
import org.apache.http.client.HttpResponseException;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.mockito.BDDMockito;
import org.mockito.Mockito;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.core.classloader.annotations.PrepareForTest;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Properties;

@PrepareForTest({
        CreateSecurityRuleResponse.class,
        DatabaseManager.class,
        GetSecurityGroupsResponse.class,
        GetSecurityRulesResponse.class,
        OpenStackHttpToFogbowExceptionMapper.class,
        PropertiesUtil.class,
})
public class OpenStackSecurityRulesPluginTest extends BaseUnitTests {

    private static final int FAKE_PORT_FROM = 1024;
    private static final int FAKE_PORT_TO = 2048;
    
    private static final String ANY_STRING = "any-string";
    private static final String ANY_URL = "http://localhost:8007";
    private static final String NETWORK_NEUTRONV2_URL_KEY = "openstack_neutron_v2_url";
    private static final String DEFAULT_NETWORK_URL = "http://localhost:0000";
    private static final String INGRESS_TRAFFIC = "ingress";
    private static final String SECURITY_RULE_ID = "securityRuleId";
    private static final String SECURITY_GROUP_ID = "securityGroupId";
    private static final String SECURITY_GROUP_NAME = "securityGroupName";

    private static final String FAKE_ETHERTYPE = "IPv4";
    private static final String FAKE_TOKEN_VALUE = "fake-token-value";
    private static final String FAKE_USER_ID = "fake-user-id";
    private static final String FAKE_NAME = "fake-name";
    private static final String FAKE_PROJECT_ID = "fake-project-id";
    private static final String MAP_METHOD = "map";
    private static final String PROTOCOL_ANY = "ANY";
    private static final String PROTOCOL_ICMP = "ICMP";
    private static final String PROTOCOL_TCP = "TCP";
    private static final String PROTOCOL_UDP = "UDP";


    private OpenStackSecurityRulePlugin plugin;
    private OpenStackV3User cloudUser;
    private Properties properties;
    private OpenStackHttpClient clientMock;
    private NetworkOrder majorOrder;

    @Before
    public void setUp() throws InvalidParameterException, UnexpectedException {
        this.testUtils.mockReadOrdersFromDataBase();
        String confFilePath = HomeDir.getPath() + SystemConstants.CLOUDS_CONFIGURATION_DIRECTORY_NAME + File.separator
                + "default" + File.separator + SystemConstants.CLOUD_SPECIFICITY_CONF_FILE_NAME;

        this.properties = Mockito.mock(Properties.class);
        Mockito.when(this.properties.get(NETWORK_NEUTRONV2_URL_KEY)).thenReturn(DEFAULT_NETWORK_URL);

        PowerMockito.mockStatic(PropertiesUtil.class);
        BDDMockito.when(PropertiesUtil.readProperties(Mockito.eq(confFilePath))).thenReturn(properties);

        this.plugin = Mockito.spy(new OpenStackSecurityRulePlugin(confFilePath));

        this.clientMock = Mockito.mock(OpenStackHttpClient.class);
        this.plugin.setClient(this.clientMock);
        this.cloudUser = new OpenStackV3User(FAKE_USER_ID, FAKE_NAME, FAKE_TOKEN_VALUE, FAKE_PROJECT_ID);

        this.majorOrder = testUtils.createNetworkOrder(testUtils.getLocalMemberId(), testUtils.FAKE_REMOTE_MEMBER_ID);
    }

    // test case: The http client must make only 1 request
    @Test
    public void testRequestSecurityRule() throws Exception {
        // set up
        Mockito.doReturn(SECURITY_GROUP_NAME).when(this.plugin)
                .retrieveSecurityGroupName(Mockito.eq(majorOrder));

        Mockito.doReturn(SECURITY_GROUP_ID).when(this.plugin).
                retrieveSecurityGroupId(Mockito.anyString(), Mockito.eq(cloudUser));

        Mockito.doReturn(ANY_STRING).when(this.plugin)
                .doPostRequest(Mockito.anyString(), Mockito.anyString(), Mockito.eq(this.cloudUser));

        CreateSecurityRuleResponse createSecurityRuleResponseMock = Mockito.mock(CreateSecurityRuleResponse.class);
        Mockito.when(createSecurityRuleResponseMock.getId()).thenReturn(ANY_STRING);

        PowerMockito.mockStatic(CreateSecurityRuleResponse.class);
        BDDMockito.when(CreateSecurityRuleResponse.fromJson(Mockito.eq(ANY_STRING)))
                .thenReturn(createSecurityRuleResponseMock);

        SecurityRule securityRule = createEmptySecurityRule();

        // exercise
        this.plugin.requestSecurityRule(securityRule, majorOrder, this.cloudUser);

        // verify
        Mockito.verify(this.plugin, Mockito.times(TestUtils.RUN_ONCE))
                .retrieveSecurityGroupName(Mockito.eq(majorOrder));

        Mockito.verify(this.plugin, Mockito.times(TestUtils.RUN_ONCE))
                .retrieveSecurityGroupId(Mockito.anyString(), Mockito.eq(cloudUser));

        Mockito.verify(this.plugin, Mockito.times(TestUtils.RUN_ONCE))
                .doPostRequest(Mockito.anyString(), Mockito.anyString(), Mockito.eq(cloudUser));

        PowerMockito.verifyStatic(CreateSecurityRuleResponse.class);
        CreateSecurityRuleResponse.fromJson(Mockito.eq(ANY_STRING));
    }

    // test case: Check if the method makes the expected calls
    @Test
    public void testGetSecurityRules() throws FogbowException {
        // set up
        Mockito.doReturn(SECURITY_GROUP_NAME).when(this.plugin)
                .retrieveSecurityGroupName(Mockito.eq(majorOrder));

        Mockito.doReturn(SECURITY_GROUP_ID).when(this.plugin)
                .retrieveSecurityGroupId(Mockito.anyString(), Mockito.eq(cloudUser));

        Mockito.doReturn(ANY_STRING).when(this.plugin)
                .doGetRequest(Mockito.anyString(), Mockito.eq(cloudUser));

        Mockito.doReturn(new ArrayList<SecurityRuleInstance>()).when(this.plugin)
                .getSecurityRulesFromJson(Mockito.anyString());

        // exercise
        this.plugin.getSecurityRules(majorOrder, cloudUser);

        // verify
        Mockito.verify(this.plugin, Mockito.times(TestUtils.RUN_ONCE))
                .retrieveSecurityGroupName(Mockito.eq(majorOrder));

        Mockito.verify(this.plugin, Mockito.times(TestUtils.RUN_ONCE))
                .retrieveSecurityGroupId(Mockito.anyString(), Mockito.eq(cloudUser));

        Mockito.verify(this.plugin, Mockito.times(TestUtils.RUN_ONCE))
                .doGetRequest(Mockito.anyString(), Mockito.eq(cloudUser));

        Mockito.verify(this.plugin, Mockito.times(TestUtils.RUN_ONCE))
                .getSecurityRulesFromJson(Mockito.anyString());
    }

    // test case: Check if the method makes the expected calls
    @Test
    public void testDeleteSecurityRule() throws FogbowException {
        // set up
        Mockito.doNothing().when(this.plugin)
                .doDeleteRequest(Mockito.anyString(), Mockito.eq(cloudUser));

        // exercise
        this.plugin.deleteSecurityRule(SECURITY_RULE_ID, cloudUser);

        // verify
        Mockito.verify(this.plugin, Mockito.times(TestUtils.RUN_ONCE))
                .doDeleteRequest(Mockito.anyString(), Mockito.eq(cloudUser));
    }

    // test case: when calling doDeleteRequest() method, it must verify that the
    // call was successful.
    @Test
    public void testDoDeleteSuccessfully() throws Exception {
        // exercise
        this.plugin.doDeleteRequest(ANY_URL, cloudUser);

        // verify
        Mockito.verify(this.clientMock, Mockito.times(TestUtils.RUN_ONCE))
                .doDeleteRequest(Mockito.anyString(), Mockito.any());
    }

    // test case: when doDeleteRequest() request fails, it must
    // call OpenStackHttpToFogbowExceptionMapper.map(e)
    @Test
    public void testDoDeleteUnsuccessfully() throws Exception {
        // set up
        HttpResponseException exception = testUtils.getHttpInternalServerErrorResponseException();
        Mockito.doThrow(exception).when(this.clientMock)
                .doDeleteRequest(Mockito.anyString(), Mockito.eq(cloudUser));

        PowerMockito.mockStatic(OpenStackHttpToFogbowExceptionMapper.class);
        PowerMockito.doCallRealMethod().when(OpenStackHttpToFogbowExceptionMapper.class, MAP_METHOD, Mockito.any());

        // exercise
        try {
            this.plugin.doDeleteRequest(ANY_URL, cloudUser);
            Assert.fail();
        } catch (FogbowException e) {
            // verify
            Assert.assertEquals(exception.getMessage(), e.getMessage());

            PowerMockito.verifyStatic(OpenStackHttpToFogbowExceptionMapper.class, Mockito.times(TestUtils.RUN_ONCE));
            OpenStackHttpToFogbowExceptionMapper.map(Mockito.eq(exception));
        }
    }

    // test case: when calling doPostRequest() method, it must verify that the
    // call was successful.
    @Test
    public void testDoPostRequest() throws Exception {
        // exercise
        this.plugin.doPostRequest(ANY_URL, ANY_STRING, cloudUser);

        // verify
        Mockito.verify(this.clientMock, Mockito.times(TestUtils.RUN_ONCE))
                .doPostRequest(ANY_URL, ANY_STRING, cloudUser);
    }

    // test case: when doPostRequest() request fails, it must
    // call OpenStackHttpToFogbowExceptionMapper.map(e)
    @Test
    public void testDoPostUnsuccessfully() throws Exception {
        // set up
        HttpResponseException exception = testUtils.getHttpInternalServerErrorResponseException();
        Mockito.doThrow(exception).when(this.clientMock)
                .doPostRequest(Mockito.anyString(), Mockito.anyString(), Mockito.eq(cloudUser));

        PowerMockito.mockStatic(OpenStackHttpToFogbowExceptionMapper.class);
        PowerMockito.doCallRealMethod().when(OpenStackHttpToFogbowExceptionMapper.class, MAP_METHOD, Mockito.any());

        // exercise
        try {
            this.plugin.doPostRequest(ANY_URL, ANY_STRING, cloudUser);
            Assert.fail();
        } catch (FogbowException e) {
            // verify
            Assert.assertEquals(exception.getMessage(), e.getMessage());

            PowerMockito.verifyStatic(OpenStackHttpToFogbowExceptionMapper.class, Mockito.times(TestUtils.RUN_ONCE));
            OpenStackHttpToFogbowExceptionMapper.map(Mockito.eq(exception));
        }
    }

    // test case: when calling doGetRequest() method, it must verify that the
    // call was successful.
    @Test
    public void testDoGetSuccessfully() throws Exception {
        // exercise
        this.plugin.doGetRequest(ANY_URL, cloudUser);

        // verify
        Mockito.verify(this.clientMock, Mockito.times(TestUtils.RUN_ONCE))
                .doGetRequest(Mockito.anyString(), Mockito.any());
    }

    // test case: when doGetRequest() request fails, it must
    // call OpenStackHttpToFogbowExceptionMapper.map(e)
    @Test
    public void testDoGetUnsuccessfully() throws Exception {
        // set up
        HttpResponseException exception = testUtils.getHttpInternalServerErrorResponseException();
        Mockito.doThrow(exception).when(this.clientMock)
                .doGetRequest(Mockito.anyString(), Mockito.eq(cloudUser));

        PowerMockito.mockStatic(OpenStackHttpToFogbowExceptionMapper.class);
        PowerMockito.doCallRealMethod().when(OpenStackHttpToFogbowExceptionMapper.class, MAP_METHOD, Mockito.any());

        // exercise
        try {
            this.plugin.doGetRequest(ANY_URL, cloudUser);
            Assert.fail();
        } catch (FogbowException e) {
            // verify
            Assert.assertEquals(exception.getMessage(), e.getMessage());

            PowerMockito.verifyStatic(OpenStackHttpToFogbowExceptionMapper.class, Mockito.times(TestUtils.RUN_ONCE));
            OpenStackHttpToFogbowExceptionMapper.map(Mockito.eq(exception));
        }
    }

    // test case: given a NetworkOrder it should return the appropriate group name
    @Test
    public void testRetrieveSecurityGroupNameWithNetworkOrder()
            throws cloud.fogbow.common.exceptions.InvalidParameterException {
        // setup
        NetworkOrder networkOrder = testUtils.createLocalNetworkOrder();
        String expectedGroupName = SystemConstants.PN_SECURITY_GROUP_PREFIX + networkOrder.getInstanceId();
        NetworkOrder spyNetworkOrder = Mockito.spy(networkOrder);

        // exercise
        String actualGroupName = this.plugin.retrieveSecurityGroupName(spyNetworkOrder);

        // verify
        Mockito.verify(spyNetworkOrder, Mockito.times((testUtils.RUN_ONCE))).getType();
        Mockito.verify(spyNetworkOrder, Mockito.times((testUtils.RUN_ONCE))).getInstanceId();
        Assert.assertEquals(expectedGroupName, actualGroupName);
    }

    // test case: given a PublicIpOrder it should return the appropriate group name
    @Test
    public void testRetrieveSecurityGroupNameWithPublicIpOrder()
            throws cloud.fogbow.common.exceptions.InvalidParameterException {
        // setup
        PublicIpOrder publicIpOrder = testUtils.createLocalPublicIpOrder(ANY_STRING);
        String expectedGroupName = SystemConstants.PIP_SECURITY_GROUP_PREFIX + publicIpOrder.getInstanceId();
        PublicIpOrder spyPublicIpOrder = Mockito.spy(publicIpOrder);

        // exercise
        String actualGroupName = this.plugin.retrieveSecurityGroupName(spyPublicIpOrder);

        // verify
        Mockito.verify(spyPublicIpOrder, Mockito.times((testUtils.RUN_ONCE))).getType();
        Mockito.verify(spyPublicIpOrder, Mockito.times((testUtils.RUN_ONCE))).getInstanceId();
        Assert.assertEquals(expectedGroupName, actualGroupName);
    }

    // test case: given a non-supported Order it should throw InvalidParameterException
    @Test(expected = InvalidParameterException.class)
    public void testRetrieveSecurityGroupNameWithInvalidOrder()
            throws InvalidParameterException {
        // setup
        ComputeOrder computeOrder = testUtils.createLocalComputeOrder();

        // exercise
        this.plugin.retrieveSecurityGroupName(computeOrder);
        Assert.fail();
    }

    // test case: given a security rule name retrieveSecurityGroupId() should return its id
    @Test
    public void testRetrieveSecurityGroupIdSuccessful() throws FogbowException {
        // setup
        Mockito.doReturn(ANY_STRING).when(this.plugin).doGetRequest(Mockito.anyString(), Mockito.eq(cloudUser));

        String[] groupsIds = {SECURITY_GROUP_ID};
        GetSecurityGroupsResponse response =
                createMockedSecurityGroupList(Arrays.asList(groupsIds));
        PowerMockito.mockStatic(GetSecurityGroupsResponse.class);
        BDDMockito.when(GetSecurityGroupsResponse.fromJson(Mockito.anyString()))
                .thenReturn(response);

        // exercise
        this.plugin.retrieveSecurityGroupId(SECURITY_GROUP_NAME, cloudUser);

        // verify
        Mockito.verify(plugin, Mockito.times(testUtils.RUN_ONCE))
                .doGetRequest(Mockito.anyString(), Mockito.eq(cloudUser));

        PowerMockito.verifyStatic();
        GetSecurityGroupsResponse.fromJson(Mockito.anyString());
    }

    // test case: given a security rule name and the request return many ids,
    // retrieveSecurityGroupId() should throw an exception
    @Test
    public void testRetrieveSecurityGroupIdUnsuccessful() throws FogbowException {
        // setup
        Mockito.doReturn(ANY_STRING).when(this.plugin).doGetRequest(Mockito.anyString(), Mockito.eq(cloudUser));

        String[] groupsIds = {SECURITY_GROUP_ID, SECURITY_GROUP_ID};
        GetSecurityGroupsResponse response =
                createMockedSecurityGroupList(Arrays.asList(groupsIds));
        PowerMockito.mockStatic(GetSecurityGroupsResponse.class);
        BDDMockito.when(GetSecurityGroupsResponse.fromJson(Mockito.anyString()))
                .thenReturn(response);

        String expectedExceptionMessage =
                String.format(Messages.Exception.MULTIPLE_SECURITY_GROUPS_EQUALLY_NAMED, SECURITY_GROUP_NAME);

        // exercise
        try {
            this.plugin.retrieveSecurityGroupId(SECURITY_GROUP_NAME, cloudUser);
            Assert.fail();
        } catch (FogbowException e) {
            Assert.assertEquals(expectedExceptionMessage, e.getMessage());

            Mockito.verify(plugin, Mockito.times(testUtils.RUN_ONCE))
                    .doGetRequest(Mockito.anyString(), Mockito.eq(cloudUser));

            PowerMockito.verifyStatic();
            GetSecurityGroupsResponse.fromJson(Mockito.anyString());
        }
    }

    // test case: when calling testGetSecurityRulesFromJson() method, it
    // must verify that the call was successful.
    @Test
    public void testGetSecurityRulesFromJson() throws FogbowException {
        // setup
        GetSecurityRulesResponse securityRulesResponseMock = createSecurityRulesResponseMock();

        PowerMockito.mockStatic(GetSecurityRulesResponse.class);
        BDDMockito.when(GetSecurityRulesResponse.fromJson(Mockito.anyString()))
                .thenReturn(securityRulesResponseMock);

        Mockito.doReturn(SecurityRule.Protocol.ANY).when(plugin).defineRuleProtocol(Mockito.any());

        // exercise
        List<SecurityRuleInstance> rules = plugin.getSecurityRulesFromJson(ANY_STRING);

        // verify
        Assert.assertEquals(testUtils.FAKE_INSTANCE_ID, rules.get(0).getId());
        PowerMockito.verifyStatic(GetSecurityRulesResponse.class);
        GetSecurityRulesResponse.fromJson(Mockito.anyString());

        Mockito.verify(securityRulesResponseMock, Mockito.times(testUtils.RUN_ONCE)).getSecurityRules();
    }

    // test case: when given securityRules with protocols set, it should the Protocol value
    @Test
    public void testDefineRuleProtocolSuccessful() throws FogbowException {
        // setup
        GetSecurityRulesResponse.SecurityRules tcpSecurityRule = createMockedSecurityRulesWithProtocol(PROTOCOL_TCP);
        GetSecurityRulesResponse.SecurityRules udpSecurityRule = createMockedSecurityRulesWithProtocol(PROTOCOL_UDP);
        GetSecurityRulesResponse.SecurityRules icmpSecurityRule = createMockedSecurityRulesWithProtocol(PROTOCOL_ICMP);
        GetSecurityRulesResponse.SecurityRules anySecurityRule = createMockedSecurityRulesWithProtocol(null);

        // exercise
        SecurityRule.Protocol tcpProtocol = plugin.defineRuleProtocol(tcpSecurityRule);
        SecurityRule.Protocol udpProtocol = plugin.defineRuleProtocol(udpSecurityRule);
        SecurityRule.Protocol icmpProtocol = plugin.defineRuleProtocol(icmpSecurityRule);
        SecurityRule.Protocol anyProtocol = plugin.defineRuleProtocol(anySecurityRule);

        // verify
        Assert.assertEquals(SecurityRule.Protocol.TCP, tcpProtocol);
        Assert.assertEquals(SecurityRule.Protocol.UDP, udpProtocol);
        Assert.assertEquals(SecurityRule.Protocol.ICMP, icmpProtocol);
        Assert.assertEquals(SecurityRule.Protocol.ANY, anyProtocol);
    }

    // test case: when given an unsupported protocol, it should throw an exception
    @Test(expected = FogbowException.class)
    public void testDefineRuleProtocolUnsuccessful() throws FogbowException {
        // setup
        String nonExistingProtocol = "its-very-unlikelly-that-this-protocol-is-gonna-exist";
        GetSecurityRulesResponse.SecurityRules tcpSecurityRule = createMockedSecurityRulesWithProtocol(nonExistingProtocol);

        // verify
        plugin.defineRuleProtocol(tcpSecurityRule);
        Assert.fail();
    }

    private GetSecurityGroupsResponse createMockedSecurityGroupList(List<String> groupIds) {
        GetSecurityGroupsResponse response = Mockito.mock(GetSecurityGroupsResponse.class);

        List<GetSecurityGroupsResponse.SecurityGroup> securityGroups = new ArrayList();
        for (String groupId : groupIds) {
            securityGroups.add(createMockedSecurityGroup(groupId));
        }
        Mockito.when(response.getSecurityGroups()).thenReturn(securityGroups);
        return response;
    }

    private GetSecurityGroupsResponse.SecurityGroup createMockedSecurityGroup(String id) {
        GetSecurityGroupsResponse.SecurityGroup securityGroupMock =
                Mockito.mock(GetSecurityGroupsResponse.SecurityGroup.class);
        Mockito.when(securityGroupMock.getId()).thenReturn(id);
        return securityGroupMock;
    }

    private SecurityRule createEmptySecurityRule() {
        return new SecurityRule(SecurityRule.Direction.OUT, 0, 0, "0.0.0.0/0 ", SecurityRule.EtherType.IPv4, SecurityRule.Protocol.TCP);
    }

    private GetSecurityRulesResponse createSecurityRulesResponseMock() {
        GetSecurityRulesResponse.SecurityRules securityRule = createMockedSecurityRulesWithProtocol(PROTOCOL_TCP);

        List<GetSecurityRulesResponse.SecurityRules> securityRules = new ArrayList<>();
        securityRules.add(securityRule);

        GetSecurityRulesResponse response = Mockito.mock(GetSecurityRulesResponse.class);
        Mockito.when(response.getSecurityRules()).thenReturn(securityRules);

        return response;
    }

    private GetSecurityRulesResponse.SecurityRules createMockedSecurityRulesWithProtocol(String protocol) {
        return createMockedSecurityRules(
                testUtils.FAKE_INSTANCE_ID, INGRESS_TRAFFIC, FAKE_PORT_FROM, FAKE_PORT_TO,
                testUtils.FAKE_CIDR, FAKE_ETHERTYPE, protocol);
    }

    private GetSecurityRulesResponse.SecurityRules createMockedSecurityRules(String id, String direction,
            int portFrom, int portTo, String cidr, String etherType, String protocol) {
        GetSecurityRulesResponse.SecurityRules securityRules =
                Mockito.mock(GetSecurityRulesResponse.SecurityRules.class);

        Mockito.when(securityRules.getId()).thenReturn(id);
        Mockito.when(securityRules.getDirection()).thenReturn(direction);
        Mockito.when(securityRules.getPortFrom()).thenReturn(portFrom);
        Mockito.when(securityRules.getPortTo()).thenReturn(portTo);
        Mockito.when(securityRules.getCidr()).thenReturn(cidr);
        Mockito.when(securityRules.getEtherType()).thenReturn(etherType);
        Mockito.when(securityRules.getProtocol()).thenReturn(protocol);

        return securityRules;
    }
}
