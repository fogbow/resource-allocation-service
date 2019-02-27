package cloud.fogbow.ras.core.plugins.interoperability.cloudstack.securityrule.v4_9;

import cloud.fogbow.common.constants.CloudStackConstants;
import cloud.fogbow.common.exceptions.FogbowException;
import cloud.fogbow.common.exceptions.UnexpectedException;
import cloud.fogbow.common.models.CloudToken;
import cloud.fogbow.common.models.FederationUser;
import cloud.fogbow.common.util.HomeDir;
import cloud.fogbow.common.util.PropertiesUtil;
import cloud.fogbow.ras.constants.SystemConstants;
import cloud.fogbow.ras.core.models.orders.ComputeOrder;
import cloud.fogbow.ras.core.models.orders.NetworkOrder;
import cloud.fogbow.ras.core.models.orders.Order;
import cloud.fogbow.ras.core.models.orders.PublicIpOrder;
import cloud.fogbow.ras.core.models.securityrules.Direction;
import cloud.fogbow.ras.core.models.securityrules.EtherType;
import cloud.fogbow.ras.core.models.securityrules.Protocol;
import cloud.fogbow.ras.core.models.securityrules.SecurityRule;
import cloud.fogbow.ras.core.plugins.interoperability.cloudstack.CloudStackHttpClient;
import cloud.fogbow.ras.core.plugins.interoperability.cloudstack.CloudStackQueryJobResult;
import cloud.fogbow.ras.core.plugins.interoperability.cloudstack.CloudStackUrlUtil;
import cloud.fogbow.ras.core.plugins.interoperability.cloudstack.publicip.v4_9.CloudStackPublicIpPlugin;
import com.google.gson.Gson;
import org.apache.http.HttpStatus;
import org.apache.http.client.HttpResponseException;
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

import java.io.File;
import java.util.*;

@RunWith(PowerMockRunner.class)
@PowerMockIgnore({"javax.net.ssl.*", "javax.crypto.*" })
@PrepareForTest({CloudStackUrlUtil.class, CloudStackHttpClient.class, CloudStackQueryJobResult.class, CloudStackSecurityRulePlugin.class})
public class CloudStackSecurityRulePluginTest {

    private static String FAKE_JOB_ID = "fake-job-id";
    private static String FAKE_SECURITY_RULE_ID = "fake-rule-id";
    private static final String JSON = "json";
    private static final String JOB_ID_KEY = "json";
    private static final String RESPONSE_KEY = "response";
    private static final String ID_KEY = "id";
    private static final String CLOUD_NAME = "cloudstack";
    private static final String CLOUDSTACK_URL = "cloudstack_api_url";

    private static final String FAKE_TOKEN_PROVIDER = "fake-token-provider";
    private static final String FAKE_USER_ID = "fake-user-id";
    private static final String FAKE_USERNAME = "fake-username";
    private static final String FAKE_TOKEN_VALUE = "fake-api-key:fake-secret-key";
    private static final String FAKE_SIGNATURE = "fake-signature";

    private static final FederationUser FAKE_USER = new FederationUser(FAKE_TOKEN_PROVIDER, FAKE_USER_ID, FAKE_USERNAME, FAKE_TOKEN_VALUE, new HashMap<>());
    private static final CloudToken FAKE_TOKEN = new CloudToken(FAKE_USER);

    private CloudStackSecurityRulePlugin plugin;
    private CloudStackHttpClient client;
    private Properties properties;

    private CloudStackQueryJobResult queryJobResult;

    @Before
    public void setUp() {
        // we dont want HttpRequestUtil code to be executed in this test
        PowerMockito.mockStatic(CloudStackHttpClient.class);
        PowerMockito.mockStatic(CloudStackQueryJobResult.class);

        String cloudStackConfFilePath = HomeDir.getPath() + SystemConstants.CLOUDS_CONFIGURATION_DIRECTORY_NAME +
                File.separator + CLOUD_NAME + File.separator + SystemConstants.CLOUD_SPECIFICITY_CONF_FILE_NAME;
        this.properties = PropertiesUtil.readProperties(cloudStackConfFilePath);

        this.queryJobResult = Mockito.mock(CloudStackQueryJobResult.class);
        this.client = Mockito.mock(CloudStackHttpClient.class);
        this.plugin = Mockito.spy(new CloudStackSecurityRulePlugin(cloudStackConfFilePath));
        this.plugin.setClient(this.client);
    }

    // test case: success case
    @Test
    public void testGetFirewallRules() throws FogbowException, HttpResponseException {
        // setup
        Order publicIpOrder = new PublicIpOrder();
        String instanceId = "instanceId";
        publicIpOrder.setInstanceId(instanceId);
        String tokenValue = "x" + CloudStackConstants.KEY_VALUE_SEPARATOR + "y";
        CloudToken localUserAttributes  = new CloudToken(new FederationUser("", "", "", tokenValue, new HashMap<>()));

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
                Mockito.anyString(), Mockito.any(CloudToken.class)))
                .thenReturn(listFirewallRulesResponse);

        CloudToken token  = new CloudToken(new FederationUser("provider", "user", "name", "token", new HashMap<>()));
        Assert.assertEquals(this.client.doGetRequest("", token), listFirewallRulesResponse);

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
            throws FogbowException, HttpResponseException {
        // setup
        Order publicIpOrder = new PublicIpOrder();
        String tokenValue = "x" + CloudStackConstants.KEY_VALUE_SEPARATOR + "y";
        CloudToken localUserAttributes  = new CloudToken(new FederationUser("", "", "", tokenValue, new HashMap<>()));

        HttpResponseException badRequestException = new HttpResponseException(HttpStatus.SC_BAD_REQUEST, "");
        Mockito.doThrow(badRequestException).when(this.client).doGetRequest(
                Mockito.anyString(), Mockito.any(CloudToken.class));

        CloudStackPublicIpPlugin.setOrderidToInstanceIdMapping(publicIpOrder.getId(), publicIpOrder.getId());

        // exercise
        List<SecurityRule> securityRules = null;
        try {
            this.plugin.getSecurityRules(publicIpOrder, localUserAttributes);
            Assert.fail();
        } catch (FogbowException e) {
        }


        // verify
        Assert.assertNull(securityRules);
        Mockito.verify(this.client, Mockito.times(1)).doGetRequest(
                Mockito.anyString(),
                Mockito.eq(localUserAttributes));
    }

    // test case: unsupported network order
    @Test(expected = UnsupportedOperationException.class)
    public void testGetFirewallRulesNetworkOrder() throws FogbowException {
        // setup
        Order networkOrder = new NetworkOrder();
        CloudToken localUserAttributes  = new CloudToken(new FederationUser("provider", "user", "name", "token", new HashMap<>()));

        // exercise
        this.plugin.getSecurityRules(networkOrder, localUserAttributes);
    }

    // test case: throw exception when the order is different of the options: network, publicip
    @Test(expected = UnexpectedException.class)
    public void testGetFirewallRulesOrderIrregular() throws FogbowException {
        // setup
        Order irregularOrder = new ComputeOrder();
        CloudToken localUserAttributes  = new CloudToken(new FederationUser("provider", "user", "name", "token", new HashMap<>()));

        // exercise
        this.plugin.getSecurityRules(irregularOrder, localUserAttributes);
    }

    // test case: Creating a security rule with egress direction, should raise an UnsupportedOperationException.
    @Test
    public void testCreateEgressSecurityRule() throws FogbowException {
        // set up
        SecurityRule securityRule = createSecurityRule();
        securityRule.setDirection(Direction.OUT);

        // exercise
        try {
            plugin.requestSecurityRule(securityRule, Mockito.mock(Order.class), Mockito.mock(CloudToken.class));
            Assert.fail();
        } catch (UnsupportedOperationException e) {
            // verify
        }
    }

    // test case: Test waitForJobResult method, when max tries is reached.
    @Test
    public void testWaitForJobResultUntilMaxTries() throws FogbowException {
        // set up
        Mockito.doNothing().when(plugin).deleteSecurityRule(Mockito.anyString(), Mockito.any(CloudToken.class));
        String processingJobResponse = getProcessingJobResponse(CloudStackQueryJobResult.PROCESSING);
        BDDMockito.given(this.queryJobResult.getQueryJobResult(
                Mockito.any(CloudStackHttpClient.class), Mockito.anyString(), Mockito.any(CloudToken.class))).
                willReturn(processingJobResponse);

        // exercise
        try {
            this.plugin.waitForJobResult(this.client, FAKE_JOB_ID, Mockito.mock(CloudToken.class));
            Assert.fail();
        } catch (FogbowException e) {
            // verify
            Mockito.verify(this.plugin, Mockito.times(1)).deleteSecurityRule(Mockito.anyString(),
                    Mockito.any(CloudToken.class));
        }
    }

    // test case: Test waitForJobResult method, after getting processing status for 5 seconds it will fail and test if
    // catches a FogbowException.
    @Test
    public void testWaitForJobResultWillReturnFailedJob() throws FogbowException {
        // set up
        Mockito.doNothing().when(plugin).deleteSecurityRule(Mockito.anyString(), Mockito.any(CloudToken.class));

        String processingJobResponse = getProcessingJobResponse(CloudStackQueryJobResult.PROCESSING);
        BDDMockito.given(this.queryJobResult.getQueryJobResult(
                Mockito.any(CloudStackHttpClient.class), Mockito.anyString(), Mockito.any(CloudToken.class))).
                willReturn(processingJobResponse);

        // exercise
        try {
            TimerTask timerTask = getTimerTask(CloudStackQueryJobResult.FAILURE);

            new Timer().schedule(timerTask, 5 * CloudStackSecurityRulePlugin.ONE_SECOND_IN_MILIS);
            this.plugin.waitForJobResult(Mockito.mock(CloudStackHttpClient.class), FAKE_JOB_ID,
                    Mockito.mock(CloudToken.class));
            Assert.fail();
        } catch (FogbowException e) {
            // verify
            Mockito.verify(this.plugin, Mockito.times(0)).deleteSecurityRule(Mockito.anyString(),
                    Mockito.any(CloudToken.class));
        }
    }

    // test case: Test waitForJobResult method, after getting 'processing' status for 5 seconds, it will be attended
    // and test if returns the instanceId.
    @Test
    public void testWaitForJobResultWillReturnSuccessJob() throws FogbowException {
        // set up
        String processingJobResponse = getProcessingJobResponse(CloudStackQueryJobResult.PROCESSING);
        BDDMockito.given(this.queryJobResult.getQueryJobResult(
                Mockito.any(CloudStackHttpClient.class), Mockito.anyString(), Mockito.any(CloudToken.class))).
                willReturn(processingJobResponse);

        // exercise
        TimerTask timerTask = getTimerTask(CloudStackQueryJobResult.SUCCESS);

        new Timer().schedule(timerTask, 5 * CloudStackSecurityRulePlugin.ONE_SECOND_IN_MILIS);
        String instanceId = this.plugin.waitForJobResult(Mockito.mock(CloudStackHttpClient.class), FAKE_JOB_ID,
                Mockito.mock(CloudToken.class));

        // verify
        Assert.assertEquals(FAKE_SECURITY_RULE_ID, instanceId);
    }

    // test case: Test waitForJobResult method receives a failed job and test if catches a FogbowException.
    @Test
    public void testWaitForJobResultFailedJob() throws FogbowException {
        // set up
        Mockito.doNothing().when(plugin).deleteSecurityRule(Mockito.anyString(), Mockito.any(CloudToken.class));

        String processingJobResponse = getProcessingJobResponse(CloudStackQueryJobResult.FAILURE);
        BDDMockito.given(this.queryJobResult.getQueryJobResult(
                Mockito.any(CloudStackHttpClient.class), Mockito.anyString(), Mockito.any(CloudToken.class))).
                willReturn(processingJobResponse);

        // exercise
        try {
            this.plugin.waitForJobResult(Mockito.mock(CloudStackHttpClient.class), FAKE_JOB_ID,
                    Mockito.mock(CloudToken.class));
            Assert.fail();
        } catch (FogbowException e) {
            // verify
            Mockito.verify(this.plugin, Mockito.times(0)).deleteSecurityRule(Mockito.anyString(),
                    Mockito.any(CloudToken.class));
        }
    }

    // test case: Test waitForJobResult method receives a sucessful job and test if returns the instanceId.
    @Test
    public void testWaitForJobResultSuccessJob() throws FogbowException {
        // set up
        String processingJobResponse = getProcessingJobResponse(CloudStackQueryJobResult.SUCCESS);
        BDDMockito.given(this.queryJobResult.getQueryJobResult(
                Mockito.any(CloudStackHttpClient.class), Mockito.anyString(), Mockito.any(CloudToken.class))).
                willReturn(processingJobResponse);

        // exercise
        String instanceId = this.plugin.waitForJobResult(Mockito.mock(CloudStackHttpClient.class), FAKE_JOB_ID,
                Mockito.mock(CloudToken.class));

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
                Mockito.any(CloudStackHttpClient.class), Mockito.anyString(), Mockito.any(CloudToken.class)))
                .willReturn(processingJobResponse)
                .willReturn(successJobResponse);

        String endpoint = getBaseEndpointFromCloudStackConf();
        String deleteRuleCommand = DeleteFirewallRuleRequest.DELETE_RULE_COMMAND;
        String expectedDeleteRuleRequestUrl = generateExpectedUrl(endpoint, deleteRuleCommand,
                RESPONSE_KEY, JSON,
                ID_KEY, FAKE_SECURITY_RULE_ID);
        String fakeResponse = getDeleteFirewallRuleResponse(FAKE_JOB_ID);

        PowerMockito.mockStatic(CloudStackUrlUtil.class);
        PowerMockito.when(CloudStackUrlUtil.createURIBuilder(Mockito.anyString(), Mockito.anyString())).thenCallRealMethod();
        Mockito.when(this.client.doGetRequest(Mockito.eq(expectedDeleteRuleRequestUrl), Mockito.eq(FAKE_TOKEN)))
                .thenReturn(fakeResponse);

        // exercise
        this.plugin.deleteSecurityRule(FAKE_SECURITY_RULE_ID, FAKE_TOKEN);

        // verify
        Mockito.verify(this.client, Mockito.times(1))
                .doGetRequest(expectedDeleteRuleRequestUrl, FAKE_TOKEN);
    }

    @Test(expected = FogbowException.class)
    public void testDeleteSecurityRuleTimeoutFail() throws FogbowException, HttpResponseException {
        // set up
        String processingJobResponse = getProcessingJobResponse(CloudStackQueryJobResult.PROCESSING);
        BDDMockito.given(this.queryJobResult.getQueryJobResult(
                Mockito.any(CloudStackHttpClient.class), Mockito.anyString(), Mockito.any(CloudToken.class)))
                .willReturn(processingJobResponse);

        String endpoint = getBaseEndpointFromCloudStackConf();
        String deleteRuleCommand = DeleteFirewallRuleRequest.DELETE_RULE_COMMAND;
        String expectedDeleteRuleRequestUrl = generateExpectedUrl(endpoint, deleteRuleCommand,
                RESPONSE_KEY, JSON,
                ID_KEY, FAKE_SECURITY_RULE_ID);
        String fakeResponse = getDeleteFirewallRuleResponse(FAKE_JOB_ID);

        PowerMockito.mockStatic(CloudStackUrlUtil.class);
        PowerMockito.when(CloudStackUrlUtil.createURIBuilder(Mockito.anyString(), Mockito.anyString())).thenCallRealMethod();
        Mockito.when(this.client.doGetRequest(Mockito.eq(expectedDeleteRuleRequestUrl), Mockito.eq(FAKE_TOKEN)))
                .thenReturn(fakeResponse);

        // exercise
        this.plugin.deleteSecurityRule(FAKE_SECURITY_RULE_ID, FAKE_TOKEN);

        // verify
        Mockito.verify(this.client, Mockito.times(1))
                .doGetRequest(expectedDeleteRuleRequestUrl, FAKE_TOKEN);
    }

    // Test case: http request fail on deleting security rule
    @Test(expected = FogbowException.class)
    public void testDeleteSecurityRuleHttpFail() throws FogbowException, HttpResponseException {
        // set up
        String endpoint = getBaseEndpointFromCloudStackConf();
        String deleteRuleCommand = DeleteFirewallRuleRequest.DELETE_RULE_COMMAND;
        String expectedDeleteRuleRequestUrl = generateExpectedUrl(endpoint, deleteRuleCommand,
                RESPONSE_KEY, JSON,
                ID_KEY, FAKE_SECURITY_RULE_ID);

        PowerMockito.mockStatic(CloudStackUrlUtil.class);
        PowerMockito.when(CloudStackUrlUtil.createURIBuilder(Mockito.anyString(), Mockito.anyString())).thenCallRealMethod();

        // Delete response is unused
        Mockito.when(this.client.doGetRequest(expectedDeleteRuleRequestUrl, FAKE_TOKEN)).thenThrow(
                new HttpResponseException(503, "service unavailable"));

        // exercise
        this.plugin.deleteSecurityRule(FAKE_SECURITY_RULE_ID, FAKE_TOKEN);

        Mockito.verify(this.client, Mockito.times(1))
                .doGetRequest(expectedDeleteRuleRequestUrl, FAKE_TOKEN);
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
                            Mockito.any(CloudStackHttpClient.class), Mockito.anyString(), Mockito.any(CloudToken.class))).
                            willReturn(processingJobResponse);
                } catch (FogbowException e) {
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
            firewallRule.put(CloudStackConstants.SecurityGroupPlugin.CIDR_LIST_KEY_JSON,
                    securityRule.getCidr());
            firewallRule.put(CloudStackConstants.SecurityGroupPlugin.ID_KEY_JSON,
                    securityRule.getInstanceId());
            firewallRule.put(CloudStackConstants.SecurityGroupPlugin.START_PORT_KEY_JSON,
                    securityRule.getPortFrom());
            firewallRule.put(CloudStackConstants.SecurityGroupPlugin.END_PORT_KEY_JSON,
                    securityRule.getPortTo());
            firewallRule.put(CloudStackConstants.SecurityGroupPlugin.PROPOCOL_KEY_JSON,
                    securityRule.getProtocol().toString());
            if (etherType.equals(EtherType.IPv4)) {
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

    private String getBaseEndpointFromCloudStackConf() {
        return this.properties.getProperty(CLOUDSTACK_URL);
    }

    private String getDeleteFirewallRuleResponse(String id) {
        String response = "{\"deletefirewallruleresponse\":{\"jobid\":\"%s\"}}";

        return String.format(response, id);
    }
}
