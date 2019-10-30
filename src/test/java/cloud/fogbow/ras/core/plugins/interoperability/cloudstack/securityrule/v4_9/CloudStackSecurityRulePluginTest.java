package cloud.fogbow.ras.core.plugins.interoperability.cloudstack.securityrule.v4_9;

import cloud.fogbow.common.constants.CloudStackConstants;
import cloud.fogbow.common.exceptions.FogbowException;
import cloud.fogbow.common.exceptions.InvalidParameterException;
import cloud.fogbow.common.exceptions.UnexpectedException;
import cloud.fogbow.common.models.CloudStackUser;
import cloud.fogbow.common.util.PropertiesUtil;
import cloud.fogbow.common.util.connectivity.cloud.cloudstack.CloudStackHttpClient;
import cloud.fogbow.common.util.connectivity.cloud.cloudstack.CloudStackQueryAsyncJobResponse;
import cloud.fogbow.common.util.connectivity.cloud.cloudstack.CloudStackQueryJobResult;
import cloud.fogbow.common.util.connectivity.cloud.cloudstack.CloudStackUrlUtil;
import cloud.fogbow.ras.api.http.response.SecurityRuleInstance;
import cloud.fogbow.ras.api.parameters.SecurityRule;
import cloud.fogbow.ras.constants.Messages;
import cloud.fogbow.ras.core.BaseUnitTests;
import cloud.fogbow.ras.core.SharedOrderHolders;
import cloud.fogbow.ras.core.TestUtils;
import cloud.fogbow.ras.core.datastore.DatabaseManager;
import cloud.fogbow.ras.core.models.ResourceType;
import cloud.fogbow.ras.core.models.orders.ComputeOrder;
import cloud.fogbow.ras.core.models.orders.NetworkOrder;
import cloud.fogbow.ras.core.models.orders.Order;
import cloud.fogbow.ras.core.models.orders.PublicIpOrder;
import cloud.fogbow.ras.core.plugins.interoperability.cloudstack.CloudStackCloudUtils;
import cloud.fogbow.ras.core.plugins.interoperability.cloudstack.CloudstackTestUtils;
import cloud.fogbow.ras.core.plugins.interoperability.cloudstack.RequestMatcher;
import cloud.fogbow.ras.core.plugins.interoperability.cloudstack.publicip.v4_9.CloudStackPublicIpPlugin;
import cloud.fogbow.ras.core.plugins.interoperability.cloudstack.publicip.v4_9.CreateFirewallRuleAsyncResponse;
import cloud.fogbow.ras.core.plugins.interoperability.cloudstack.publicip.v4_9.CreateFirewallRuleRequest;
import com.google.gson.Gson;
import org.apache.http.HttpStatus;
import org.apache.http.client.HttpResponseException;
import org.junit.*;
import org.junit.rules.ExpectedException;
import org.mockito.BDDMockito;
import org.mockito.Mockito;
import org.mockito.internal.verification.VerificationModeFactory;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.core.classloader.annotations.PowerMockIgnore;
import org.powermock.core.classloader.annotations.PrepareForTest;

import java.util.*;

@PowerMockIgnore({"javax.net.ssl.*", "javax.crypto.*" })
@PrepareForTest({SharedOrderHolders.class, DatabaseManager.class, CloudStackUrlUtil.class,
        CloudStackHttpClient.class, CloudStackQueryJobResult.class, CloudStackSecurityRulePlugin.class,
        CloudStackPublicIpPlugin.class, CloudStackCloudUtils.class, CreateFirewallRuleAsyncResponse.class,
        ListFirewallRulesResponse.class})
public class CloudStackSecurityRulePluginTest extends BaseUnitTests {

    private static String FAKE_JOB_ID = "fake-job-id";
    private static String FAKE_SECURITY_RULE_ID = "fake-rule-id";
    private static final String JSON = "json";
    private static final String RESPONSE_KEY = "response";
    private static final String ID_KEY = "id";
    private static final HashMap<String, String> FAKE_COOKIE_HEADER = new HashMap<>();

    private static final String FAKE_USER_ID = "fake-user-id";
    private static final String FAKE_USERNAME = "fake-username";
    private static final String FAKE_DOMAIN = "fake-domain";
    private static final String FAKE_TOKEN_VALUE = "x" + CloudStackConstants.KEY_VALUE_SEPARATOR + "y";

    private static final CloudStackUser FAKE_CLOUD_USER = new CloudStackUser(FAKE_USER_ID, FAKE_USERNAME, FAKE_TOKEN_VALUE, FAKE_DOMAIN, FAKE_COOKIE_HEADER);

    @Rule
    private ExpectedException expectedException = ExpectedException.none();

    private CloudStackQueryJobResult queryJobResult;
    private CloudStackSecurityRulePlugin plugin;
    private CloudStackHttpClient client;
    private CloudStackUser cloudStackUser;
    private String cloudStackUrl;

    @Before
    public void setUp() throws UnexpectedException, InvalidParameterException {
        String cloudStackConfFilePath = CloudstackTestUtils.CLOUDSTACK_CONF_FILE_PATH;
        Properties properties = PropertiesUtil.readProperties(cloudStackConfFilePath);
        this.cloudStackUrl = properties.getProperty(CloudStackCloudUtils.CLOUDSTACK_URL_CONFIG);
        this.cloudStackUser = CloudstackTestUtils.CLOUD_STACK_USER;

        // we dont want HttpRequestUtil code to be executed in this test
        PowerMockito.mockStatic(CloudStackHttpClient.class);
        PowerMockito.mockStatic(CloudStackQueryJobResult.class);

        this.queryJobResult = Mockito.mock(CloudStackQueryJobResult.class);
        this.client = Mockito.mock(CloudStackHttpClient.class);
        this.plugin = Mockito.spy(new CloudStackSecurityRulePlugin(cloudStackConfFilePath));
        this.plugin.setClient(this.client);

        this.testUtils.mockReadOrdersFromDataBase();
        CloudstackTestUtils.ignoringCloudStackUrl();
    }

    // test case: When calling the requestSecurityRule method with secondary methods mocked,
    // it must verify if the doRequestInstance is called with the right parameters;
    // this includes the checking of the Cloudstack request.
    @Test
    public void testRequestSecurityRuleSuccessfully() throws FogbowException {
        // set up
        String cidr = "10.10.10.10/20";
        int portFromExpected = 22;
        int portToExpected = 22;
        SecurityRule.Protocol protocolExpected = SecurityRule.Protocol.TCP;
        SecurityRule securityRule = new SecurityRule(
                null, portFromExpected, portToExpected, cidr, null, protocolExpected);

        String orderIdExpected = "orderId";
        Order order = Mockito.mock(Order.class);
        Mockito.when(order.getId()).thenReturn(orderIdExpected);

        String publicIpExpected = "publicIp";
        PowerMockito.mockStatic(CloudStackPublicIpPlugin.class);
        PowerMockito.when(CloudStackPublicIpPlugin.getPublicIpId(Mockito.eq(orderIdExpected)))
                .thenReturn(publicIpExpected);

        String instanceIdExpected = "InstanceId";
        Mockito.doReturn(instanceIdExpected).when(this.plugin).doRequestInstance(
                Mockito.any(), Mockito.eq(this.cloudStackUser));

        Mockito.doNothing().when(this.plugin).checkRequestSecurityParameters(
                Mockito.eq(securityRule), Mockito.eq(order));

        CreateFirewallRuleRequest request = new CreateFirewallRuleRequest.Builder()
                .protocol(protocolExpected.toString())
                .startPort(String.valueOf(portFromExpected))
                .endPort(String.valueOf(portToExpected))
                .ipAddressId(publicIpExpected)
                .cidrList(cidr)
                .build(this.cloudStackUrl);

        // exercise
        String instanceId = this.plugin.requestSecurityRule(securityRule, order, cloudStackUser);

        // verify
        Assert.assertEquals(instanceId, instanceIdExpected);
        RequestMatcher<CreateFirewallRuleRequest> matcher = new RequestMatcher.CreateFirewallRule(request);
        Mockito.verify(this.plugin, Mockito.times(TestUtils.RUN_ONCE)).doRequestInstance(
                Mockito.argThat(matcher), Mockito.eq(this.cloudStackUser));
    }

    // test case: When calling the requestSecurityRule method with secondary methods mocked and
    // throws an exception in the doRequestInstance, it must verify if It throws the same exception;
    @Test
    public void testRequestSecurityRuleFail() throws FogbowException {
        // set up
        String cidr = "10.10.10.10/20";
        int portFromExpected = 22;
        int portToExpected = 22;
        SecurityRule.Protocol protocolExpected = SecurityRule.Protocol.TCP;
        SecurityRule securityRule = new SecurityRule(
                null, portFromExpected, portToExpected, cidr, null, protocolExpected);

        String orderIdExpected = "orderId";
        Order order = Mockito.mock(Order.class);
        Mockito.when(order.getId()).thenReturn(orderIdExpected);

        Mockito.doNothing().when(this.plugin).checkRequestSecurityParameters(
                Mockito.eq(securityRule), Mockito.eq(order));

        String publicIpExpected = "publicIp";
        PowerMockito.mockStatic(CloudStackPublicIpPlugin.class);
        PowerMockito.when(CloudStackPublicIpPlugin.getPublicIpId(Mockito.eq(orderIdExpected)))
                .thenReturn(publicIpExpected);

        Mockito.doThrow(new FogbowException()).when(this.plugin)
                .doRequestInstance(Mockito.any(), Mockito.eq(this.cloudStackUser));

        // verify
        this.expectedException.expect(FogbowException.class);

        // exercise
        try {
            this.plugin.requestSecurityRule(securityRule, order, cloudStackUser);
            Assert.fail();
        } finally {
            Mockito.verify(this.plugin, Mockito.times(TestUtils.RUN_ONCE)).doRequestInstance(
                    Mockito.any(), Mockito.any());
        }
    }

    // test case: When calling the requestSecurityRule method and throws an InvalidParameterException
    // when is being checked the parameters, it must verify if It throws the same exception
    // and stop the method.
    @Test
    public void testRequestSecurityRuleFailOnCheckParameters() throws FogbowException {
        // set up
        SecurityRule securityRule = Mockito.mock(SecurityRule.class);
        Order order = Mockito.mock(Order.class);

        Mockito.doThrow(new InvalidParameterException()).when(this.plugin)
                .checkRequestSecurityParameters(Mockito.eq(securityRule), Mockito.eq(order));

        this.expectedException.expect(InvalidParameterException.class);

        // exercise
        try {
            this.plugin.requestSecurityRule(securityRule, order, cloudStackUser);
        } finally {
            // verify
            Mockito.verify(securityRule, Mockito.times(TestUtils.NEVER_RUN)).getCidr();
        }
    }

    // test case: When calling the doRequestInstance method with secondary methods mocked,
    // it must verify if It returns the right instance id (job id).
    @Test
    public void testDoRequestInstanceSuccessfully() throws FogbowException, HttpResponseException {
        // set up
        CreateFirewallRuleRequest request = new CreateFirewallRuleRequest.Builder().build("");
        String responseStr = "anything";
        String url = request.getUriBuilder().toString();
        PowerMockito.mockStatic(CloudStackCloudUtils.class);
        PowerMockito.when(CloudStackCloudUtils.doRequest(
                Mockito.eq(this.client), Mockito.eq(url), Mockito.eq(this.cloudStackUser)))
                .thenReturn(responseStr);

        String jobId = "jobId";
        CreateFirewallRuleAsyncResponse response = Mockito.mock(CreateFirewallRuleAsyncResponse.class);
        Mockito.when(response.getJobId()).thenReturn(jobId);

        PowerMockito.mockStatic(CreateFirewallRuleAsyncResponse.class);
        PowerMockito.when(CreateFirewallRuleAsyncResponse.fromJson(responseStr))
                .thenReturn(response);

        String instanceIdExpected = "instanceId";
        PowerMockito.when(CloudStackCloudUtils.waitForResult(Mockito.eq(this.client),
                Mockito.eq(this.cloudStackUrl), Mockito.eq(jobId), Mockito.eq(this.cloudStackUser)))
                .thenReturn(instanceIdExpected);

        // exercise
        String instanceId = this.plugin.doRequestInstance(request, this.cloudStackUser);

        // verify
        Assert.assertEquals(instanceIdExpected, instanceId);
    }

    // test case: When calling the doRequestInstance method with secondary methods mocked and occurs
    // an exception in the doRequest, it must verify if It throws the same exception.
    @Test
    public void testDoRequestInstanceFailOnDoRequest() throws FogbowException, HttpResponseException {
        // set up
        CreateFirewallRuleRequest request = new CreateFirewallRuleRequest.Builder().build("");
        String url = request.getUriBuilder().toString();
        PowerMockito.mockStatic(CloudStackCloudUtils.class);
        PowerMockito.when(CloudStackCloudUtils.doRequest(
                Mockito.eq(this.client), Mockito.eq(url), Mockito.eq(this.cloudStackUser)))
                .thenThrow(CloudstackTestUtils.createBadRequestHttpResponse());

        // verify
        this.expectedException.expect(FogbowException.class);
        this.expectedException.expectMessage(CloudstackTestUtils.BAD_REQUEST_MSG);

        // exercise
        try {
            this.plugin.doRequestInstance(request, this.cloudStackUser);
            Assert.fail();
        } finally {
            // verify
            PowerMockito.verifyStatic(CloudStackCloudUtils.class,
                    VerificationModeFactory.times(TestUtils.NEVER_RUN));
            CloudStackCloudUtils.waitForResult(
                    Mockito.any(), Mockito.any(), Mockito.any(), Mockito.any());
        }
    }

    // test case: When calling the doRequestInstance method with secondary methods mocked and occurs
    // a TimeoutCloudstackAsync, it must verify if It will delete the instance and rethorws the
    // exception.
    @Test
    public void testDoRequestInstanceFailWhenTimeout() throws FogbowException, HttpResponseException {
        // set up
        CreateFirewallRuleRequest request = new CreateFirewallRuleRequest.Builder().build("");
        String responseStr = "anything";
        String url = request.getUriBuilder().toString();
        PowerMockito.mockStatic(CloudStackCloudUtils.class);
        PowerMockito.when(CloudStackCloudUtils.doRequest(
                Mockito.eq(this.client), Mockito.eq(url), Mockito.eq(this.cloudStackUser)))
                .thenReturn(responseStr);

        String jobId = "jobId";
        CreateFirewallRuleAsyncResponse response = Mockito.mock(CreateFirewallRuleAsyncResponse.class);
        Mockito.when(response.getJobId()).thenReturn(jobId);

        PowerMockito.mockStatic(CreateFirewallRuleAsyncResponse.class);
        PowerMockito.when(CreateFirewallRuleAsyncResponse.fromJson(responseStr))
                .thenReturn(response);

        String errorMessage = "error";
        PowerMockito.when(CloudStackCloudUtils.waitForResult(Mockito.eq(this.client),
                Mockito.eq(this.cloudStackUrl), Mockito.eq(jobId), Mockito.eq(this.cloudStackUser)))
                .thenThrow(new CloudStackCloudUtils.TimeoutCloudstackAsync(errorMessage));

        CloudStackQueryAsyncJobResponse responseAsync = Mockito.mock(CloudStackQueryAsyncJobResponse.class);
        String securityGroupId = "securityId";
        Mockito.when(responseAsync.getJobInstanceId()).thenReturn(securityGroupId);
        PowerMockito.when(CloudStackCloudUtils.getAsyncJobResponse(
                Mockito.eq(this.client), Mockito.eq(this.cloudStackUrl), Mockito.eq(jobId),
                Mockito.eq(this.cloudStackUser))).thenReturn(responseAsync);

        Mockito.doNothing().when(this.plugin).deleteSecurityRule(
                Mockito.eq(securityGroupId), Mockito.eq(this.cloudStackUser));

        // verify
        this.expectedException.expect(CloudStackCloudUtils.TimeoutCloudstackAsync.class);
        this.expectedException.expectMessage(errorMessage);

        // exercise
        try {
            this.plugin.doRequestInstance(request, this.cloudStackUser);
            Assert.fail();
        } finally {
            // verify
            PowerMockito.verifyStatic(CloudStackCloudUtils.class,
                    VerificationModeFactory.times(TestUtils.RUN_ONCE));
            CloudStackCloudUtils.waitForResult(
                    Mockito.any(), Mockito.any(), Mockito.any(), Mockito.any());

            Mockito.verify(this.plugin, Mockito.times(TestUtils.RUN_ONCE)).deleteSecurityRule(
                    Mockito.eq(securityGroupId), Mockito.eq(this.cloudStackUser));
        }
    }

    // test case: When calling the checkRequestSecurityParameters method, it must verify if
    // It does not throws an InvalidParameterException.
    @Test
    public void testCheckRequestSecurityParametersSuccessfully() {

        // set up
        SecurityRule securityRule = Mockito.mock(SecurityRule.class);
        Mockito.when(securityRule.getDirection()).thenReturn(SecurityRule.Direction.IN);

        Order order = Mockito.mock(Order.class);
        Mockito.when(order.getType()).thenReturn(ResourceType.PUBLIC_IP);

        // exercise and verify
        try {
            this.plugin.checkRequestSecurityParameters(securityRule, order);
        } catch (InvalidParameterException e) {
            Assert.fail();
        }
    }

    // test case: When calling the checkRequestSecurityParameters method with wrong direction,
    // It must verify if It throws the right InvalidParameterException.
    @Test
    public void testCheckRequestSecurityParametersWhenDirectionIsWrong()
            throws InvalidParameterException {

        // set up
        SecurityRule securityRule = Mockito.mock(SecurityRule.class);
        Mockito.when(securityRule.getDirection()).thenReturn(SecurityRule.Direction.OUT);
        Order order = Mockito.mock(Order.class);

        this.expectedException.expect(InvalidParameterException.class);
        this.expectedException.expectMessage(Messages.Exception.INVALID_PARAMETER);

        // exercise
        this.plugin.checkRequestSecurityParameters(securityRule, order);
    }

    // test case: When calling the checkRequestSecurityParameters method with wrong order type,
    // It must verify if It throws the right InvalidParameterException.
    @Test
    public void testCheckRequestSecurityParametersWhenOrderTypeWrong()
            throws InvalidParameterException {

        // set up
        SecurityRule securityRule = Mockito.mock(SecurityRule.class);
        Mockito.when(securityRule.getDirection()).thenReturn(SecurityRule.Direction.IN);

        Order order = Mockito.mock(Order.class);
        Mockito.when(order.getType()).thenReturn(ResourceType.COMPUTE);

        this.expectedException.expect(InvalidParameterException.class);
        this.expectedException.expectMessage(Messages.Exception.INVALID_RESOURCE);

        // exercise
        this.plugin.checkRequestSecurityParameters(securityRule, order);
    }

    // test case: When calling the doGetSecurityRules method with public ip order,
    // it must verify if It returns the list of security rules.
    @Test
    public void testDoGetSecurityRulesWhenResourceTypePublicIp() throws FogbowException {
        // set up
        String orderId = "orderId";
        Order order = Mockito.mock(Order.class);
        Mockito.when(order.getType()).thenReturn(ResourceType.PUBLIC_IP);
        Mockito.when(order.getId()).thenReturn(orderId);

        PowerMockito.mockStatic(CloudStackPublicIpPlugin.class);
        String publicIpId = "publicIpId";
        PowerMockito.when(CloudStackPublicIpPlugin.getPublicIpId(Mockito.eq(orderId)))
                .thenReturn(publicIpId);

        List<SecurityRuleInstance> securityRulesExpected = new ArrayList();
        Mockito.doReturn(securityRulesExpected).when(this.plugin).getFirewallRules(
                Mockito.eq(publicIpId), Mockito.eq(this.cloudStackUser));

        // exercise
        List<SecurityRuleInstance> securityRules =
                this.plugin.doGetSecurityRules(order, this.cloudStackUser);

        // verify
        Assert.assertEquals(securityRulesExpected, securityRules);
    }

    // test case: When calling the doGetSecurityRules method with network order,
    // it must verify if It returns an empty list.
    @Test
    public void testDoGetSecurityRulesWhenResourceTypeNetwork() throws FogbowException {
        // set up
        Order order = Mockito.mock(Order.class);
        Mockito.when(order.getType()).thenReturn(ResourceType.NETWORK);

        // exercise
        List<SecurityRuleInstance> securityRules =
                this.plugin.doGetSecurityRules(order, this.cloudStackUser);

        // verify
        Assert.assertTrue(securityRules.isEmpty());
    }

    // test case: When calling the doGetSecurityRules method with not recognized order,
    // it must verify if It throws an UnexpectedException.
    @Test
    public void testDoGetSecurityRulesFail() throws FogbowException {
        // set up
        Order order = Mockito.mock(Order.class);
        Mockito.when(order.getType()).thenReturn(ResourceType.VOLUME);

        this.expectedException.expect(UnexpectedException.class);
        String errorMsg = String.format(Messages.Error.INVALID_LIST_SECURITY_RULE_TYPE, order.getType());
        this.expectedException.expectMessage(errorMsg);

        // exercise
        this.plugin.doGetSecurityRules(order, this.cloudStackUser);
    }

    // test case: When calling the getFirewallRules method with secondary methods mocked and occurs,
    // it must verify if It returns right firewall rules.
    @Test
    public void testGetFirewallRulesSuccessfully() throws FogbowException, HttpResponseException {
        // set up
        String ipAddessId = "ipAddressId";

        ListFirewallRulesRequest request = new ListFirewallRulesRequest.Builder()
                .ipAddressId(ipAddessId)
                .build(this.cloudStackUrl);

        String responseStr = "anything";
        PowerMockito.mockStatic(CloudStackCloudUtils.class);
        PowerMockito.when(CloudStackCloudUtils.doRequest(Mockito.eq(this.client),
                Mockito.eq(request.getUriBuilder().toString()), Mockito.eq(this.cloudStackUser)))
                .thenReturn(responseStr);

        List<ListFirewallRulesResponse.SecurityRuleResponse> securityRulesExpected = new ArrayList<>();
        ListFirewallRulesResponse response = Mockito.mock(ListFirewallRulesResponse.class);
        Mockito.when(response.getSecurityRulesResponse()).thenReturn(securityRulesExpected);
        PowerMockito.mockStatic(ListFirewallRulesResponse.class);
        PowerMockito.when(ListFirewallRulesResponse.fromJson(Mockito.eq(responseStr)))
                .thenReturn(response);

        List<SecurityRuleInstance> firewallRulesExpected = new ArrayList<>();
        Mockito.doReturn(firewallRulesExpected).when(this.plugin).convertToFogbowSecurityRules(
                Mockito.eq(securityRulesExpected));

        // exercise
        List<SecurityRuleInstance> firewallRules = this.plugin.getFirewallRules(
                ipAddessId, this.cloudStackUser);

        // verify
        Assert.assertEquals(firewallRulesExpected, firewallRules);
    }

    // test case: When calling the getFirewallRules method with secondary methods mocked and occurs
    // an exception in the doRequest, it must verify if It throws a FogbowException.
    @Test
    public void testGetFirewallRulesFailDoRequest() throws FogbowException, HttpResponseException {
        // set up
        String ipAddessId = "ipAddressId";

        ListFirewallRulesRequest request = new ListFirewallRulesRequest.Builder()
                .ipAddressId(ipAddessId)
                .build(this.cloudStackUrl);

        PowerMockito.mockStatic(CloudStackCloudUtils.class);
        PowerMockito.when(CloudStackCloudUtils.doRequest(Mockito.eq(this.client),
                Mockito.eq(request.getUriBuilder().toString()), Mockito.eq(this.cloudStackUser)))
                .thenThrow(CloudstackTestUtils.createBadRequestHttpResponse());

        this.expectedException.expect(FogbowException.class);
        this.expectedException.expectMessage(CloudstackTestUtils.BAD_REQUEST_MSG);

        // exercise
        this.plugin.getFirewallRules(ipAddessId, this.cloudStackUser);
    }

    // test case: When calling the convertToFogbowSecurityRules method,
    // it must verify if It return right security rules.
    @Test
    public void testConvertToFogbowSecurityRulesSuccessfully() {
        // set up
        String instanceId = "1";
        String protocol = SecurityRule.Protocol.TCP.toString();
        int startPort = 1;
        int endPort = 2;
        String ipaddress = "10.10.10.1";
        SecurityRule.EtherType etherType = this.plugin.inferEtherType(ipaddress);
        String cird = "10.10.10.0/20";
        SecurityRule.Direction direction = SecurityRule.Direction.IN;

        ListFirewallRulesResponse.SecurityRuleResponse securityRuleOne =
                Mockito.mock(ListFirewallRulesResponse.SecurityRuleResponse.class);
        Mockito.when(securityRuleOne.getCidr()).thenReturn(cird);
        Mockito.when(securityRuleOne.getProtocol()).thenReturn(protocol);
        Mockito.when(securityRuleOne.getInstanceId()).thenReturn(instanceId);
        Mockito.when(securityRuleOne.getPortFrom()).thenReturn(startPort);
        Mockito.when(securityRuleOne.getPortTo()).thenReturn(endPort);
        Mockito.when(securityRuleOne.getIpAddress()).thenReturn(ipaddress);
        Mockito.when(securityRuleOne.getDirection()).thenReturn(direction);

        List<ListFirewallRulesResponse.SecurityRuleResponse> securityRulesResponse = new ArrayList<>();
        securityRulesResponse.add(securityRuleOne);

        // exercise
        List<SecurityRuleInstance> securityRuleInstances =
                this.plugin.convertToFogbowSecurityRules(securityRulesResponse);

        // verify
        Assert.assertEquals(securityRulesResponse.size(), securityRuleInstances.size());
        SecurityRuleInstance firstSecurityRule = securityRuleInstances.listIterator().next();
        Assert.assertEquals(startPort, firstSecurityRule.getPortFrom());
        Assert.assertEquals(endPort, firstSecurityRule.getPortTo());
        Assert.assertEquals(protocol, firstSecurityRule.getProtocol().toString());
        Assert.assertEquals(direction, firstSecurityRule.getDirection());
        Assert.assertEquals(instanceId, firstSecurityRule.getId());
        Assert.assertEquals(etherType, firstSecurityRule.getEtherType());
    }

    // test case: When calling the convertToFogbowSecurityRules method and there is no firewall rules,
    // it must verify if It return an empty list.
    @Test
    public void testConvertToFogbowSecurityRulesWhenEmptyList() {
        // set up
        List<ListFirewallRulesResponse.SecurityRuleResponse> securityRulesResponse = new ArrayList<>();

        // exercise
        List<SecurityRuleInstance> securityRuleInstances =
                this.plugin.convertToFogbowSecurityRules(securityRulesResponse);

        // verify
        Assert.assertTrue(securityRuleInstances.isEmpty());
    }

    // test case: When calling the getFogbowProtocol method and the protocol is TCP,
    // it must verify if It return a TPC FogbowProtocol.
    @Test
    public void testGetFogbowProtocolWheIsTcp() {
        // set up
        String protocol = CloudStackConstants.SecurityGroupPlugin.TCP_VALUE_PROTOCOL;

        // exercise
        SecurityRule.Protocol fogbowProtocol = this.plugin.getFogbowProtocol(protocol);

        // verify
        Assert.assertEquals(SecurityRule.Protocol.TCP, fogbowProtocol);
    }

    // test case: When calling the getFogbowProtocol method and the protocol is UDP,
    // it must verify if It return a UDP FogbowProtocol.
    @Test
    public void testGetFogbowProtocolWhenIsUdp() {
        // set up
        String protocol = CloudStackConstants.SecurityGroupPlugin.UDP_VALUE_PROTOCOL;

        // exercise
        SecurityRule.Protocol fogbowProtocol = this.plugin.getFogbowProtocol(protocol);

        // verify
        Assert.assertEquals(SecurityRule.Protocol.UDP, fogbowProtocol);
    }

    // test case: When calling the getFogbowProtocol method and the protocol is IMCP,
    // it must verify if It return a IMCP FogbowProtocol.
    @Test
    public void testGetFogbowProtocolWhenIsImcp() {
        // set up
        String protocol = CloudStackConstants.SecurityGroupPlugin.ICMP_VALUE_PROTOCOL;

        // exercise
        SecurityRule.Protocol fogbowProtocol = this.plugin.getFogbowProtocol(protocol);

        // verify
        Assert.assertEquals(SecurityRule.Protocol.ICMP, fogbowProtocol);
    }

    // test case: When calling the getFogbowProtocol method and the protocol is any,
    // it must verify if It return a ANY FogbowProtocol.
    @Test
    public void testGetFogbowProtocolWhenIsAny() {
        // set up
        String protocol = CloudStackConstants.SecurityGroupPlugin.ALL_VALUE_PROTOCOL;

        // exercise
        SecurityRule.Protocol fogbowProtocol = this.plugin.getFogbowProtocol(protocol);

        // verify
        Assert.assertEquals(SecurityRule.Protocol.ANY, fogbowProtocol);
    }

    // test case: When calling the getFogbowProtocol method and the protocol is unknown,
    // it must verify if It return null.
    @Test
    public void testGetFogbowProtocolFail() {
        // set up
        String protocol = "unknown";

        // exercise
        SecurityRule.Protocol fogbowProtocol = this.plugin.getFogbowProtocol(protocol);

        // verify
        Assert.assertNull(fogbowProtocol);
    }

    // # ------- old code --------- @

    // test case: success case
    @Test
    public void testGetFirewallRules() throws FogbowException, HttpResponseException {
        // setup
        Order publicIpOrder = new PublicIpOrder();
        String instanceId = "instanceId";
        publicIpOrder.setInstanceId(instanceId);

        // create firewall rules response
        int portFrom = 20;
        int portTo = 30;
        String cidr = "0.0.0.0/0";
        SecurityRule.EtherType etherType = SecurityRule.EtherType.IPv4;
        SecurityRule.Protocol protocol = SecurityRule.Protocol.TCP;
        List<SecurityRuleInstance> securityRulesExpected = new ArrayList<SecurityRuleInstance>();
        securityRulesExpected.add(new SecurityRuleInstance("instance1", SecurityRule.Direction.IN, portFrom, portTo, cidr, etherType, protocol));
        securityRulesExpected.add(new SecurityRuleInstance("instance2", SecurityRule.Direction.IN, portFrom, portTo, cidr, etherType, protocol));
        String listFirewallRulesResponse = getListFirewallRulesResponseJson(securityRulesExpected, etherType);

        Mockito.when(this.client.doGetRequest(
                Mockito.anyString(), Mockito.any(CloudStackUser.class)))
                .thenReturn(listFirewallRulesResponse);

        CloudStackPublicIpPlugin.setOrderidToInstanceIdMapping(publicIpOrder.getId(), instanceId);

        // exercise
        List<SecurityRuleInstance> securityRules = this.plugin.getSecurityRules(publicIpOrder, FAKE_CLOUD_USER);

        //verify
        Assert.assertEquals(securityRulesExpected.size(), securityRules.size());
        Assert.assertEquals(securityRulesExpected.get(0).toString(), securityRules.get(0).toString());
        Assert.assertEquals(securityRulesExpected.get(1).toString(), securityRules.get(1).toString());
    }

    // test case: throw exception when trying to request to the cloudstack
    @Test
    public void testGetFirewallRulesExceptionInComunication()
            throws FogbowException, HttpResponseException {
        // setup
        Order publicIpOrder = new PublicIpOrder();

        HttpResponseException badRequestException = new HttpResponseException(HttpStatus.SC_BAD_REQUEST, "");
        Mockito.doThrow(badRequestException).when(this.client).doGetRequest(
                Mockito.anyString(), Mockito.any(CloudStackUser.class));

        CloudStackPublicIpPlugin.setOrderidToInstanceIdMapping(publicIpOrder.getId(), publicIpOrder.getId());

        // exercise
        List<SecurityRuleInstance> securityRuleInstances = null;
        try {
            this.plugin.getSecurityRules(publicIpOrder, FAKE_CLOUD_USER);
            Assert.fail();
        } catch (FogbowException e) {
        }


        // verify
        Assert.assertNull(securityRuleInstances);
        Mockito.verify(this.client, Mockito.times(1)).doGetRequest(
                Mockito.anyString(),
                Mockito.eq(FAKE_CLOUD_USER));
    }

    // test case: unsupported network order
    @Ignore
    @Test(expected = UnsupportedOperationException.class)
    public void testGetFirewallRulesNetworkOrder() throws FogbowException {
        // setup
        Order networkOrder = new NetworkOrder();

        // exercise
        this.plugin.getSecurityRules(networkOrder, FAKE_CLOUD_USER);
    }

    // test case: throw exception when the order is different of the options: network, publicip
    @Test(expected = UnexpectedException.class)
    public void testGetFirewallRulesOrderIrregular() throws FogbowException {
        // setup
        Order irregularOrder = new ComputeOrder();

        // exercise
        this.plugin.getSecurityRules(irregularOrder, FAKE_CLOUD_USER);
    }

    @Ignore // TODO(chico) - It make no more sense
    // test case: Test waitForJobResult method, when max tries is reached.
    @Test
    public void testWaitForJobResultUntilMaxTries() throws FogbowException {
        // set up
        Mockito.doNothing().when(plugin).deleteSecurityRule(Mockito.anyString(), Mockito.any(CloudStackUser.class));
        String processingJobResponse = getProcessingJobResponse(CloudStackQueryJobResult.PROCESSING);
        BDDMockito.given(this.queryJobResult.getQueryJobResult(
                Mockito.any(CloudStackHttpClient.class), Mockito.anyString(), Mockito.anyString(), Mockito.any(CloudStackUser.class))).
                willReturn(processingJobResponse);

        // exercise
        try {
            CloudStackCloudUtils.waitForResult(this.client, this.cloudStackUrl, FAKE_JOB_ID, Mockito.mock(CloudStackUser.class));
            Assert.fail();
        } catch (FogbowException e) {
            // verify
            Mockito.verify(this.plugin, Mockito.times(1)).deleteSecurityRule(Mockito.anyString(),
                    Mockito.any(CloudStackUser.class));
        }
    }

    // test case: Test waitForJobResult method, after getting processing status for 5 seconds it will fail and test if
    // catches a FogbowException.
    @Test
    public void testWaitForJobResultWillReturnFailedJob() throws FogbowException {
        // set up
        Mockito.doNothing().when(plugin).deleteSecurityRule(Mockito.anyString(), Mockito.any(CloudStackUser.class));

        String processingJobResponse = getProcessingJobResponse(CloudStackQueryJobResult.PROCESSING);
        BDDMockito.given(this.queryJobResult.getQueryJobResult(
                Mockito.any(CloudStackHttpClient.class), Mockito.anyString(), Mockito.anyString(), Mockito.any(CloudStackUser.class))).
                willReturn(processingJobResponse);

        // exercise
        try {
            TimerTask timerTask = getTimerTask(CloudStackQueryJobResult.FAILURE);
            new Timer().schedule(timerTask, 5 * CloudStackCloudUtils.ONE_SECOND_IN_MILIS);
            CloudStackCloudUtils.waitForResult(Mockito.mock(CloudStackHttpClient.class), this.cloudStackUrl,
                    FAKE_JOB_ID, Mockito.mock(CloudStackUser.class));
            Assert.fail();
        } catch (FogbowException e) {
            // verify
            Mockito.verify(this.plugin, Mockito.times(0)).deleteSecurityRule(Mockito.anyString(),
                    Mockito.any(CloudStackUser.class));
        }
    }

    // test case: Test waitForJobResult method, after getting 'processing' status for 5 seconds, it will be attended
    // and test if returns the instanceId.
    @Test
    public void testWaitForJobResultWillReturnSuccessJob() throws FogbowException {
        // set up
        String processingJobResponse = getProcessingJobResponse(CloudStackQueryJobResult.PROCESSING);
        BDDMockito.given(this.queryJobResult.getQueryJobResult(
                Mockito.any(CloudStackHttpClient.class), Mockito.anyString(), Mockito.anyString(), Mockito.any(CloudStackUser.class))).
                willReturn(processingJobResponse);

        // exercise
        TimerTask timerTask = getTimerTask(CloudStackQueryJobResult.SUCCESS);
        new Timer().schedule(timerTask, 5 * CloudStackCloudUtils.ONE_SECOND_IN_MILIS);
        String instanceId = CloudStackCloudUtils.waitForResult(Mockito.mock(CloudStackHttpClient.class),
                this.cloudStackUrl, FAKE_JOB_ID, Mockito.mock(CloudStackUser.class));

        // verify
        Assert.assertEquals(FAKE_SECURITY_RULE_ID, instanceId);
    }

    // test case: Test waitForJobResult method receives a failed job and test if catches a FogbowException.
    @Test
    public void testWaitForJobResultFailedJob() throws FogbowException {
        // set up
        Mockito.doNothing().when(plugin).deleteSecurityRule(Mockito.anyString(), Mockito.any(CloudStackUser.class));

        String processingJobResponse = getProcessingJobResponse(CloudStackQueryJobResult.FAILURE);
        BDDMockito.given(this.queryJobResult.getQueryJobResult(
                Mockito.any(CloudStackHttpClient.class), Mockito.anyString(), Mockito.anyString(), Mockito.any(CloudStackUser.class))).
                willReturn(processingJobResponse);

        // exercise
        try {
            CloudStackCloudUtils.waitForResult(Mockito.mock(CloudStackHttpClient.class),
                    this.cloudStackUrl, FAKE_JOB_ID, Mockito.mock(CloudStackUser.class));
            Assert.fail();
        } catch (FogbowException e) {
            // verify
            Mockito.verify(this.plugin, Mockito.times(0)).deleteSecurityRule(Mockito.anyString(),
                    Mockito.any(CloudStackUser.class));
        }
    }

    // test case: Test waitForJobResult method receives a sucessful job and test if returns the instanceId.
    @Test
    public void testWaitForJobResultSuccessJob() throws FogbowException {
        // set up
        String processingJobResponse = getProcessingJobResponse(CloudStackQueryJobResult.PROCESSING);
        String successJobResponse = getProcessingJobResponse(CloudStackQueryJobResult.SUCCESS);
        BDDMockito.given(this.queryJobResult.getQueryJobResult(
                Mockito.any(CloudStackHttpClient.class), Mockito.anyString(), Mockito.anyString(), Mockito.any(CloudStackUser.class))).
                willReturn(processingJobResponse, successJobResponse);

        // exercise
        String instanceId = CloudStackCloudUtils.waitForResult(Mockito.mock(CloudStackHttpClient.class),
                this.cloudStackUrl, FAKE_JOB_ID, Mockito.mock(CloudStackUser.class));

        // verify
        Assert.assertEquals(FAKE_SECURITY_RULE_ID, instanceId);
    }

    // test case: Test delete security rule success operation should make at least three http get request: one for the
    // delete operation itself; one to get the processing job status and the last one to get the success job status.
    @Test
    public void testDeleteSecurityRule() throws FogbowException, HttpResponseException {
        // set up
        String processingJobResponse = getProcessingJobResponse(CloudStackQueryJobResult.PROCESSING);
        String successJobResponse = getProcessingJobResponse(CloudStackQueryJobResult.SUCCESS);
        BDDMockito.given(this.queryJobResult.getQueryJobResult(
                Mockito.any(CloudStackHttpClient.class), Mockito.anyString(), Mockito.anyString(), Mockito.any(CloudStackUser.class)))
                .willReturn(processingJobResponse)
                .willReturn(successJobResponse);

        String deleteRuleCommand = DeleteFirewallRuleRequest.DELETE_RULE_COMMAND;
        String expectedDeleteRuleRequestUrl = generateExpectedUrl(this.cloudStackUrl, deleteRuleCommand,
                RESPONSE_KEY, JSON,
                ID_KEY, FAKE_SECURITY_RULE_ID);
        String fakeResponse = getDeleteFirewallRuleResponse(FAKE_JOB_ID);

        PowerMockito.mockStatic(CloudStackUrlUtil.class);
        PowerMockito.when(CloudStackUrlUtil.createURIBuilder(Mockito.anyString(), Mockito.anyString())).thenCallRealMethod();
        Mockito.when(this.client.doGetRequest(Mockito.eq(expectedDeleteRuleRequestUrl), Mockito.eq(FAKE_CLOUD_USER)))
                .thenReturn(fakeResponse);

        // exercise
        this.plugin.deleteSecurityRule(FAKE_SECURITY_RULE_ID, FAKE_CLOUD_USER);

        // verify
        Mockito.verify(this.client, Mockito.times(1))
                .doGetRequest(expectedDeleteRuleRequestUrl, FAKE_CLOUD_USER);
    }

    @Ignore
    @Test(expected = FogbowException.class)
    public void testDeleteSecurityRuleTimeoutFail() throws FogbowException, HttpResponseException {
        // set up
        String processingJobResponse = getProcessingJobResponse(CloudStackQueryJobResult.PROCESSING);
        BDDMockito.given(this.queryJobResult.getQueryJobResult(
                Mockito.any(CloudStackHttpClient.class), Mockito.anyString(), Mockito.anyString(), Mockito.any(CloudStackUser.class)))
                .willReturn(processingJobResponse);

        String deleteRuleCommand = DeleteFirewallRuleRequest.DELETE_RULE_COMMAND;
        String expectedDeleteRuleRequestUrl = generateExpectedUrl(this.cloudStackUrl, deleteRuleCommand,
                RESPONSE_KEY, JSON,
                ID_KEY, FAKE_SECURITY_RULE_ID);
        String fakeResponse = getDeleteFirewallRuleResponse(FAKE_JOB_ID);

        PowerMockito.mockStatic(CloudStackUrlUtil.class);
        PowerMockito.when(CloudStackUrlUtil.createURIBuilder(Mockito.anyString(), Mockito.anyString())).thenCallRealMethod();
        Mockito.when(this.client.doGetRequest(Mockito.eq(expectedDeleteRuleRequestUrl), Mockito.eq(FAKE_CLOUD_USER)))
                .thenReturn(fakeResponse);

        // exercise
        this.plugin.deleteSecurityRule(FAKE_SECURITY_RULE_ID, FAKE_CLOUD_USER);

        // verify
        Mockito.verify(this.client, Mockito.times(1))
                .doGetRequest(expectedDeleteRuleRequestUrl, FAKE_CLOUD_USER);
    }

    // Test case: http request fail on deleting security rule
    @Test(expected = FogbowException.class)
    public void testDeleteSecurityRuleHttpFail() throws FogbowException, HttpResponseException {
        // set up
        String deleteRuleCommand = DeleteFirewallRuleRequest.DELETE_RULE_COMMAND;
        String expectedDeleteRuleRequestUrl = generateExpectedUrl(this.cloudStackUrl, deleteRuleCommand,
                RESPONSE_KEY, JSON,
                ID_KEY, FAKE_SECURITY_RULE_ID);

        PowerMockito.mockStatic(CloudStackUrlUtil.class);
        PowerMockito.when(CloudStackUrlUtil.createURIBuilder(Mockito.anyString(), Mockito.anyString())).thenCallRealMethod();

        // Delete response is unused
        Mockito.when(this.client.doGetRequest(expectedDeleteRuleRequestUrl, FAKE_CLOUD_USER)).thenThrow(
                new HttpResponseException(503, "service unavailable"));

        // exercise
        this.plugin.deleteSecurityRule(FAKE_SECURITY_RULE_ID, FAKE_CLOUD_USER);

        Mockito.verify(this.client, Mockito.times(1))
                .doGetRequest(expectedDeleteRuleRequestUrl, FAKE_CLOUD_USER);
    }

    private String getProcessingJobResponse(int processing) {
        return "{\n" +
                "  \"queryasyncjobresultresponse\": {\n" +
                "    \"jobstatus\": " + processing + ", \n" +
                "    \"jobinstanceid\": \"" + FAKE_SECURITY_RULE_ID + "\"\n" +
                "  }\n" +
                "}";
    }

    private TimerTask getTimerTask(int status) {
        return new TimerTask() {
            @Override
            public void run() {
                String processingJobResponse = getProcessingJobResponse(status);
                try {
                    BDDMockito.given(CloudStackSecurityRulePluginTest.this.queryJobResult.getQueryJobResult(
                            Mockito.any(CloudStackHttpClient.class), Mockito.anyString(), Mockito.anyString(), Mockito.any(CloudStackUser.class))).
                            willReturn(processingJobResponse);
                } catch (FogbowException e) {
                    e.printStackTrace();
                }
            }
        };
    }

    private SecurityRule createSecurityRule() {
        SecurityRule securityRule = new SecurityRule(SecurityRule.Direction.IN, 10000, 10005, "10.0.0.0/24",
                SecurityRule.EtherType.IPv4, SecurityRule.Protocol.TCP);
        return securityRule;
    }

    private String getListFirewallRulesResponseJson(List<SecurityRuleInstance> securityRuleInstances, SecurityRule.EtherType etherType) {
        List<Map<String, Object>> listFirewallRule = new ArrayList<Map<String, Object>>();
        for (SecurityRuleInstance securityRuleInstance : securityRuleInstances) {
            Map<String, Object> firewallRule = new HashMap<String, Object>();
            firewallRule.put(CloudStackConstants.SecurityGroupPlugin.CIDR_LIST_KEY_JSON,
                    securityRuleInstance.getCidr());
            firewallRule.put(CloudStackConstants.SecurityGroupPlugin.ID_KEY_JSON,
                    securityRuleInstance.getId());
            firewallRule.put(CloudStackConstants.SecurityGroupPlugin.START_PORT_KEY_JSON,
                    securityRuleInstance.getPortFrom());
            firewallRule.put(CloudStackConstants.SecurityGroupPlugin.END_PORT_KEY_JSON,
                    securityRuleInstance.getPortTo());
            firewallRule.put(CloudStackConstants.SecurityGroupPlugin.PROPOCOL_KEY_JSON,
                    securityRuleInstance.getProtocol().toString());
            if (etherType.equals(SecurityRule.EtherType.IPv4)) {
                firewallRule.put(CloudStackConstants.SecurityGroupPlugin.IP_ADDRESS_KEY_JSON, "0.0.0.0");
            } else {
                firewallRule.put(CloudStackConstants.SecurityGroupPlugin.IP_ADDRESS_KEY_JSON,
                        "FE80:0000:0000:0000:0202:B3FF:FE1E:8329");
            }

            listFirewallRule.add(firewallRule);
        }
        Map<String, List<Map<String, Object>>> firewallRules = new HashMap<String, List<Map<String, Object>>>();
        firewallRules.put(CloudStackConstants.SecurityGroupPlugin.FIREWALL_RULE_KEY_JSON, listFirewallRule);

        Map<String, Object> floatingipJsonKey = new HashMap<String, Object>();
        floatingipJsonKey.put(CloudStackConstants.SecurityGroupPlugin.LIST_FIREWALL_RULES_KEY_JSON, firewallRules);

        Gson gson = new Gson();
        return gson.toJson(floatingipJsonKey);
    }

    private String generateExpectedUrl(String endpoint, String command, String... keysAndValues) {
        if (keysAndValues.length % 2 != 0) {
            // there should be one value for each key
            return null;
        }

        String url = String.format("%s?command=%s", endpoint, command);
        for (int i = 0; i < keysAndValues.length; i += 2) {
            String key = keysAndValues[i];
            String value = keysAndValues[i + 1];
            url += String.format("&%s=%s", key, value);
        }

        return url;
    }

    private String getDeleteFirewallRuleResponse(String id) {
        String response = "{\"deletefirewallruleresponse\":{\"jobid\":\"%s\"}}";

        return String.format(response, id);
    }
}
