package org.fogbowcloud.ras.core.plugins.interoperability.cloudstack.securityrule.v4_9;


import com.google.gson.Gson;
import org.apache.http.HttpStatus;
import org.apache.http.client.HttpResponseException;
import org.fogbowcloud.ras.core.exceptions.FogbowRasException;
import org.fogbowcloud.ras.core.exceptions.InvalidParameterException;
import org.fogbowcloud.ras.core.exceptions.UnexpectedException;
import org.fogbowcloud.ras.core.models.orders.ComputeOrder;
import org.fogbowcloud.ras.core.models.orders.NetworkOrder;
import org.fogbowcloud.ras.core.models.orders.Order;
import org.fogbowcloud.ras.core.models.orders.PublicIpOrder;
import org.fogbowcloud.ras.core.models.securityrules.Direction;
import org.fogbowcloud.ras.core.models.securityrules.EtherType;
import org.fogbowcloud.ras.core.models.securityrules.Protocol;
import org.fogbowcloud.ras.core.models.securityrules.SecurityRule;
import org.fogbowcloud.ras.core.models.tokens.CloudStackToken;
import org.fogbowcloud.ras.core.models.tokens.OpenStackV3Token;
import org.fogbowcloud.ras.core.models.tokens.Token;
import org.fogbowcloud.ras.core.plugins.aaa.tokengenerator.cloudstack.CloudStackTokenGeneratorPlugin;
import org.fogbowcloud.ras.core.plugins.interoperability.cloudstack.CloudStackQueryJobResult;
import org.fogbowcloud.ras.core.plugins.interoperability.cloudstack.CloudStackRestApiConstants;
import org.fogbowcloud.ras.core.plugins.interoperability.cloudstack.CloudStackUrlUtil;
import org.fogbowcloud.ras.core.plugins.interoperability.cloudstack.publicip.v4_9.CloudStackPublicIpPlugin;
import org.fogbowcloud.ras.util.connectivity.HttpRequestClientUtil;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.BDDMockito;
import org.mockito.Mockito;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.core.classloader.annotations.PowerMockIgnore;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;

import java.util.*;

@RunWith(PowerMockRunner.class)
@PowerMockIgnore({"javax.net.ssl.*", "javax.crypto.*" })
@PrepareForTest({CloudStackUrlUtil.class, HttpRequestClientUtil.class, CloudStackQueryJobResult.class,
        CloudStackSecurityRulePlugin.class})
public class CloudStackSecurityRulePluginTest {

    private static String FAKE_JOB_ID = "fake-job-id";
    private static String FAKE_SECURITY_RULE_ID = "fake-rule-id";

    private CloudStackSecurityRulePlugin plugin;
    private HttpRequestClientUtil client;

    private CloudStackQueryJobResult queryJobResult;

    @Before
    public void setUp() {
        // we dont want HttpRequestUtil code to be executed in this test
        PowerMockito.mockStatic(HttpRequestClientUtil.class);
        PowerMockito.mockStatic(CloudStackQueryJobResult.class);

        this.queryJobResult = Mockito.mock(CloudStackQueryJobResult.class);
        this.client = Mockito.mock(HttpRequestClientUtil.class);
        this.plugin = Mockito.spy(new CloudStackSecurityRulePlugin());
        this.plugin.setClient(this.client);
    }

    // test case: success case
    @Test
    public void testGetFirewallRules() throws FogbowRasException, UnexpectedException, HttpResponseException {
        // setup
        Order publicIpOrder = new PublicIpOrder();
        String instanceId = "instanceId";
        publicIpOrder.setInstanceId(instanceId);
        String tokenValue = "x" + CloudStackTokenGeneratorPlugin.CLOUDSTACK_TOKEN_VALUE_SEPARATOR + "y";
        CloudStackToken localUserAttributes = new CloudStackToken("", tokenValue, "", "", "");

        // create firewall rules response
        int portFrom = 20;
        int portTo = 30;
        String cidr = "0.0.0.0/0";
        EtherType etherType = EtherType.IPv4;
        Protocol protocol = Protocol.TCP;
        List<SecurityRule> securityRulesExpected = new ArrayList<SecurityRule>();
        securityRulesExpected.add(new SecurityRule(Direction.IN, portFrom, portTo, cidr, etherType, protocol));
        securityRulesExpected.add(new SecurityRule(Direction.IN, portFrom, portTo, cidr, etherType, protocol));
        String listFirewallRulesResponse = getListFirewallRulesResponseJson(securityRulesExpected, etherType);

        Mockito.when(this.client.doGetRequest(
                Mockito.anyString(), Mockito.any(CloudStackToken.class)))
                .thenReturn(listFirewallRulesResponse);

        Assert.assertEquals(this.client.doGetRequest("", new CloudStackToken()), listFirewallRulesResponse);

        CloudStackPublicIpPlugin.setOrderidToInstanceIdMapping(publicIpOrder.getId(), instanceId);

        // exercise
        List<SecurityRule> securityRules = this.plugin.getSecurityRules(publicIpOrder, localUserAttributes);

        //verify
        Assert.assertEquals(securityRulesExpected.size(), securityRules.size());
        Assert.assertArrayEquals(securityRulesExpected.toArray(), securityRules.toArray());
    }

    // test case: throw exception where trying to request to the cloudstack
    @Test
    public void testGetFirewallRulesExceptionInComunication()
            throws FogbowRasException, UnexpectedException, HttpResponseException {
        // setup
        Order publicIpOrder = new PublicIpOrder();
        String tokenValue = "x" + CloudStackTokenGeneratorPlugin.CLOUDSTACK_TOKEN_VALUE_SEPARATOR + "y";
        CloudStackToken localUserAttributes = new CloudStackToken("", tokenValue, "", "", "");

        HttpResponseException badRequestException = new HttpResponseException(HttpStatus.SC_BAD_REQUEST, "");
        Mockito.doThrow(badRequestException).when(this.client).doGetRequest(
                Mockito.anyString(), Mockito.any(OpenStackV3Token.class));

        CloudStackPublicIpPlugin.setOrderidToInstanceIdMapping(publicIpOrder.getId(), publicIpOrder.getId());

        // exercise
        List<SecurityRule> securityRules = null;
        try {
            this.plugin.getSecurityRules(publicIpOrder, localUserAttributes);
            Assert.fail();
        } catch (InvalidParameterException e) {
        }


        // verify
        Assert.assertNull(securityRules);
        Mockito.verify(this.client, Mockito.times(1)).doGetRequest(
                Mockito.anyString(),
                Mockito.eq(localUserAttributes));
    }

    // test case: unsupported network order
    @Test(expected = UnsupportedOperationException.class)
    public void testGetFirewallRulesNetworkOrder() throws FogbowRasException, UnexpectedException {
        // setup
        Order networkOrder = new NetworkOrder();
        CloudStackToken localUserAttributes = new CloudStackToken();

        // exercise
        this.plugin.getSecurityRules(networkOrder, localUserAttributes);
    }

    // test case: throw exception when the order is different of the options: network, publicip
    @Test(expected = UnexpectedException.class)
    public void testGetFirewallRulesOrderIrregular() throws FogbowRasException, UnexpectedException {
        // setup
        Order irregularkOrder = new ComputeOrder();
        CloudStackToken localUserAttributes = new CloudStackToken();

        // exercise
        this.plugin.getSecurityRules(irregularkOrder, localUserAttributes);
    }

    // test case: Creating a security rule with egress direction, should raise an UnsupportedOperationException.
    @Test
    public void testCreateEgressSecurityRule() throws UnexpectedException, FogbowRasException {
        // set up
        SecurityRule securityRule = createSecurityRule();
        securityRule.setDirection(Direction.OUT);

        // exercise
        try {
            plugin.requestSecurityRule(securityRule, Mockito.mock(Order.class), Mockito.mock(CloudStackToken.class));
            Assert.fail();
        } catch (UnsupportedOperationException e) {
            // verify
        }
    }

    // test case: Test waitForJobResult method, when max tries is reached.
    @Test
    public void testWaitForJobResultUntilMaxTries() throws FogbowRasException, UnexpectedException {
        // set up
        Mockito.doNothing().when(plugin).deleteSecurityRule(Mockito.anyString(), Mockito.any(CloudStackToken.class));
        String processingJobResponse = getProcessingJobResponse(CloudStackQueryJobResult.PROCESSING);
        BDDMockito.given(this.queryJobResult.getQueryJobResult(
                Mockito.any(HttpRequestClientUtil.class), Mockito.anyString(), Mockito.any(Token.class))).
                willReturn(processingJobResponse);

        // exercise
        try {
            this.plugin.waitForJobResult(this.client, FAKE_JOB_ID, Mockito.mock(CloudStackToken.class));
            Assert.fail();
        } catch (FogbowRasException e) {
            // verify
            Mockito.verify(this.plugin, Mockito.times(1)).deleteSecurityRule(Mockito.anyString(),
                    Mockito.any(CloudStackToken.class));
        }
    }

    // test case: Test waitForJobResult method, after getting processing status for 5 seconds it will fail and test if
    // catches a FogbowRasException.
    @Test
    public void testWaitForJobResultWillReturnFailedJob() throws FogbowRasException, UnexpectedException {
        // set up
        Mockito.doNothing().when(plugin).deleteSecurityRule(Mockito.anyString(), Mockito.any(CloudStackToken.class));

        String processingJobResponse = getProcessingJobResponse(CloudStackQueryJobResult.PROCESSING);
        BDDMockito.given(this.queryJobResult.getQueryJobResult(
                Mockito.any(HttpRequestClientUtil.class), Mockito.anyString(), Mockito.any(Token.class))).
                willReturn(processingJobResponse);

        // exercise
        try {
            TimerTask timerTask = getTimerTask(CloudStackQueryJobResult.FAILURE);

            new Timer().schedule(timerTask, 5 * CloudStackSecurityRulePlugin.ONE_SECOND_IN_MILIS);
            this.plugin.waitForJobResult(Mockito.mock(HttpRequestClientUtil.class), FAKE_JOB_ID,
                    Mockito.mock(CloudStackToken.class));
            Assert.fail();
        } catch (FogbowRasException e) {
            // verify
            Mockito.verify(this.plugin, Mockito.times(0)).deleteSecurityRule(Mockito.anyString(),
                    Mockito.any(CloudStackToken.class));
        }
    }

    // test case: Test waitForJobResult method, after getting 'processing' status for 5 seconds, it will be attended
    // and test if returns the instanceId.
    @Test
    public void testWaitForJobResultWillReturnSuccessJob() throws FogbowRasException, UnexpectedException {
        // set up
        String processingJobResponse = getProcessingJobResponse(CloudStackQueryJobResult.PROCESSING);
        BDDMockito.given(this.queryJobResult.getQueryJobResult(
                Mockito.any(HttpRequestClientUtil.class), Mockito.anyString(), Mockito.any(Token.class))).
                willReturn(processingJobResponse);

        // exercise
        TimerTask timerTask = getTimerTask(CloudStackQueryJobResult.SUCCESS);

        new Timer().schedule(timerTask, 5 * CloudStackSecurityRulePlugin.ONE_SECOND_IN_MILIS);
        String instanceId = this.plugin.waitForJobResult(Mockito.mock(HttpRequestClientUtil.class), FAKE_JOB_ID,
                Mockito.mock(CloudStackToken.class));

        // verify
        Assert.assertEquals(FAKE_SECURITY_RULE_ID, instanceId);
    }

    // test case: Test waitForJobResult method receives a failed job and test if catches a FogbowRasException.
    @Test
    public void testWaitForJobResultFailedJob() throws FogbowRasException, UnexpectedException {
        // set up
        Mockito.doNothing().when(plugin).deleteSecurityRule(Mockito.anyString(), Mockito.any(CloudStackToken.class));

        String processingJobResponse = getProcessingJobResponse(CloudStackQueryJobResult.FAILURE);
        BDDMockito.given(this.queryJobResult.getQueryJobResult(
                Mockito.any(HttpRequestClientUtil.class), Mockito.anyString(), Mockito.any(Token.class))).
                willReturn(processingJobResponse);

        // exercise
        try {
            this.plugin.waitForJobResult(Mockito.mock(HttpRequestClientUtil.class), FAKE_JOB_ID,
                    Mockito.mock(CloudStackToken.class));
            Assert.fail();
        } catch (FogbowRasException e) {
            // verify
            Mockito.verify(this.plugin, Mockito.times(0)).deleteSecurityRule(Mockito.anyString(),
                    Mockito.any(CloudStackToken.class));
        }
    }

    // test case: Test waitForJobResult method receives a sucessful job and test if returns the instanceId.
    @Test
    public void testWaitForJobResultSuccessJob() throws FogbowRasException, UnexpectedException {
        // set up
        String processingJobResponse = getProcessingJobResponse(CloudStackQueryJobResult.SUCCESS);
        BDDMockito.given(this.queryJobResult.getQueryJobResult(
                Mockito.any(HttpRequestClientUtil.class), Mockito.anyString(), Mockito.any(Token.class))).
                willReturn(processingJobResponse);

        // exercise
        String instanceId = this.plugin.waitForJobResult(Mockito.mock(HttpRequestClientUtil.class), FAKE_JOB_ID,
                Mockito.mock(CloudStackToken.class));

        // verify
        Assert.assertEquals(FAKE_SECURITY_RULE_ID, instanceId);
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
                            Mockito.any(HttpRequestClientUtil.class), Mockito.anyString(), Mockito.any(Token.class))).
                            willReturn(processingJobResponse);
                } catch (FogbowRasException e) {
                    e.printStackTrace();
                }
            }
        };
    }

    private SecurityRule createSecurityRule() {
        SecurityRule securityRule = new SecurityRule(Direction.IN, 10000, 10005, "10.0.0.0/24",
                EtherType.IPv4, Protocol.TCP);
        return securityRule;
    }

    private String getListFirewallRulesResponseJson(List<SecurityRule> securityRules, EtherType etherType) {
        List<Map<String, Object>> listFirewallRule = new ArrayList<Map<String, Object>>();
        for (SecurityRule securityRule : securityRules) {
            Map<String, Object> firewallRule = new HashMap<String, Object>();
            firewallRule.put(CloudStackRestApiConstants.SecurityGroupPlugin.CIDR_LIST_KEY_JSON,
                    securityRule.getCidr());
            firewallRule.put(CloudStackRestApiConstants.SecurityGroupPlugin.ID_KEY_JSON,
                    securityRule.getInstanceId());
            firewallRule.put(CloudStackRestApiConstants.SecurityGroupPlugin.START_PORT_KEY_JSON,
                    securityRule.getPortFrom());
            firewallRule.put(CloudStackRestApiConstants.SecurityGroupPlugin.END_PORT_KEY_JSON,
                    securityRule.getPortTo());
            firewallRule.put(CloudStackRestApiConstants.SecurityGroupPlugin.PROPOCOL_KEY_JSON,
                    securityRule.getProtocol().toString());
            if (etherType.equals(EtherType.IPv4)) {
                firewallRule.put(CloudStackRestApiConstants.SecurityGroupPlugin.IP_ADDRESS_KEY_JSON, "0.0.0.0");
            } else {
                firewallRule.put(CloudStackRestApiConstants.SecurityGroupPlugin.IP_ADDRESS_KEY_JSON,
                        "FE80:0000:0000:0000:0202:B3FF:FE1E:8329");
            }

            listFirewallRule.add(firewallRule);
        }
        Map<String, List<Map<String, Object>>> firewallRules = new HashMap<String, List<Map<String, Object>>>();
        firewallRules.put(CloudStackRestApiConstants.SecurityGroupPlugin.FIREWALL_RULE_KEY_JSON, listFirewallRule);

        Map<String, Object> floatingipJsonKey = new HashMap<String, Object>();
        floatingipJsonKey.put(CloudStackRestApiConstants.SecurityGroupPlugin.LIST_FIREWALL_RULES_KEY_JSON, firewallRules);

        Gson gson = new Gson();
        return gson.toJson(floatingipJsonKey);
    }
}
