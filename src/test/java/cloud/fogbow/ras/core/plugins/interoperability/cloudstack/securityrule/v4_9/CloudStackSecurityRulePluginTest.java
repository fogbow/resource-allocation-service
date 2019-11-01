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
import cloud.fogbow.ras.core.models.orders.Order;
import cloud.fogbow.ras.core.plugins.interoperability.cloudstack.CloudStackCloudUtils;
import cloud.fogbow.ras.core.plugins.interoperability.cloudstack.CloudstackTestUtils;
import cloud.fogbow.ras.core.plugins.interoperability.cloudstack.RequestMatcher;
import cloud.fogbow.ras.core.plugins.interoperability.cloudstack.publicip.v4_9.CloudStackPublicIpPlugin;
import cloud.fogbow.ras.core.plugins.interoperability.cloudstack.publicip.v4_9.CreateFirewallRuleAsyncResponse;
import cloud.fogbow.ras.core.plugins.interoperability.cloudstack.publicip.v4_9.CreateFirewallRuleRequest;
import org.apache.http.client.HttpResponseException;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.mockito.Mockito;
import org.mockito.internal.verification.VerificationModeFactory;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.core.classloader.annotations.PowerMockIgnore;
import org.powermock.core.classloader.annotations.PrepareForTest;

import java.util.ArrayList;
import java.util.List;
import java.util.Properties;

@PowerMockIgnore({"javax.net.ssl.*", "javax.crypto.*" })
@PrepareForTest({SharedOrderHolders.class, DatabaseManager.class, CloudStackUrlUtil.class,
        CloudStackHttpClient.class, CloudStackQueryJobResult.class, CloudStackSecurityRulePlugin.class,
        CloudStackPublicIpPlugin.class, CloudStackCloudUtils.class, CreateFirewallRuleAsyncResponse.class,
        ListFirewallRulesResponse.class})
public class CloudStackSecurityRulePluginTest extends BaseUnitTests {

    @Rule
    private ExpectedException expectedException = ExpectedException.none();

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

        List<SecurityRuleInstance> securityRulesExpected = new ArrayList<>();
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
    // it must verify if It return an UDP FogbowProtocol.
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
    // it must verify if It return an IMCP FogbowProtocol.
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
    // it must verify if It return an ANY FogbowProtocol.
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

    // test case: When calling the inferEtherType method with an IP Ipv4,
    // it must verify if It returns an EtherType.IPv4.
    @Test
    public void testInferEtherTypeWhenIpv4() {
        // set up
        String ipAddress = "10.10.10.10";

        // exercise
        SecurityRule.EtherType etherType = this.plugin.inferEtherType(ipAddress);

        // verify
        Assert.assertEquals(SecurityRule.EtherType.IPv4, etherType);
    }

    // test case: When calling the inferEtherType method with an IP Ipv6,
    // it must verify if It returns an EtherType.IPv6.
    @Test
    public void testInferEtherTypeWhenIpv6() {
        // set up
        String ipAddress = "2001:db8:85a3:8d3:1319:8a2e:370:7348";

        // exercise
        SecurityRule.EtherType etherType = this.plugin.inferEtherType(ipAddress);

        // verify
        Assert.assertEquals(SecurityRule.EtherType.IPv6, etherType);
    }

    // test case: When calling the inferEtherType method with an unknown value,
    // it must verify if It returns null.
    @Test
    public void testInferEtherTypeFail() {
        // set up
        String ipAddress = "wrong";

        // exercise
        SecurityRule.EtherType etherType = this.plugin.inferEtherType(ipAddress);

        // verify
        Assert.assertNull(etherType);
    }

    // test case: When calling the deleteSecurityRule method with secondary methods mocked,
    // it must verify if the doDeleteInstance is called with the right parameters;
    // this includes the checking of the Cloudstack request.
    @Test
    public void testDeleteSecurityRuleSuccessfully() throws FogbowException {
        // set up
        String securityRuleId = "securityRuleId";
        Mockito.doNothing().when(this.plugin).doDeleteInstance(
                Mockito.any(), Mockito.eq(this.cloudStackUser));

        DeleteFirewallRuleRequest request = new DeleteFirewallRuleRequest.Builder()
                .ruleId(securityRuleId)
                .build(this.cloudStackUrl);

        // exercise
        this.plugin.deleteSecurityRule(securityRuleId, this.cloudStackUser);

        // verify
        RequestMatcher<DeleteFirewallRuleRequest> matcher = new RequestMatcher.DeleteFirewallRule(request);
        Mockito.verify(this.plugin, Mockito.times(TestUtils.RUN_ONCE)).doDeleteInstance(
                Mockito.argThat(matcher), Mockito.eq(this.cloudStackUser));
    }

    // test case: When calling the deleteSecurityRule method with secondary methods mocked and
    // throws an exception in the doDeleteInstance, it must verify if It throws the same exception;
    @Test
    public void testDeleteSecurityRuleFail() throws FogbowException {
        // set up
        String securityRuleId = "securityRuleId";

        Mockito.doThrow(new FogbowException()).when(this.plugin).doDeleteInstance(
                Mockito.any(), Mockito.eq(this.cloudStackUser));

        // verify
        this.expectedException.expect(FogbowException.class);

        // exercise
        this.plugin.deleteSecurityRule(securityRuleId, this.cloudStackUser);
    }

}
