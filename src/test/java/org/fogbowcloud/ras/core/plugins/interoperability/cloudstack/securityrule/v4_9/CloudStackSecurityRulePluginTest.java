package org.fogbowcloud.ras.core.plugins.interoperability.cloudstack.securityrule.v4_9;

import org.fogbowcloud.ras.core.exceptions.FogbowRasException;
import org.fogbowcloud.ras.core.exceptions.UnexpectedException;
import org.fogbowcloud.ras.core.models.orders.Order;
import org.fogbowcloud.ras.core.models.securityrules.Direction;
import org.fogbowcloud.ras.core.models.securityrules.EtherType;
import org.fogbowcloud.ras.core.models.securityrules.Protocol;
import org.fogbowcloud.ras.core.models.securityrules.SecurityRule;
import org.fogbowcloud.ras.core.models.tokens.CloudStackToken;
import org.fogbowcloud.ras.core.models.tokens.Token;
import org.fogbowcloud.ras.core.plugins.interoperability.cloudstack.CloudStackQueryJobResult;
import org.fogbowcloud.ras.core.plugins.interoperability.cloudstack.CloudStackUrlUtil;
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

import java.util.Timer;
import java.util.TimerTask;

@RunWith(PowerMockRunner.class)
@PowerMockIgnore({"javax.net.ssl.*"})
@PrepareForTest({CloudStackUrlUtil.class, HttpRequestClientUtil.class, CloudStackQueryJobResult.class,
        CloudStackSecurityRulePlugin.class})
public class CloudStackSecurityRulePluginTest {

    private static String FAKE_JOB_ID = "fake-job-id";
    private static String FAKE_SECURITY_RULE_ID = "fake-rule-id";

    private CloudStackSecurityRulePlugin plugin;
    private HttpRequestClientUtil client;
    private CloudStackQueryJobResult queryJobResult;

    @Before
    public void setUp(){
        // we dont want HttpRequestUtil code to be executed in this test
        PowerMockito.mockStatic(HttpRequestClientUtil.class);
        PowerMockito.mockStatic(CloudStackQueryJobResult.class);

        this.client = Mockito.mock(HttpRequestClientUtil.class);
        this.queryJobResult = Mockito.mock(CloudStackQueryJobResult.class);
        this.plugin = Mockito.spy(new CloudStackSecurityRulePlugin());
        this.plugin.setClient(this.client);
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

    // test case: Creating a security rule for an IPv6, should raise an UnsupportedOperationException.
    @Test
    public void testCreateIPv6SecurityRule() throws UnexpectedException, FogbowRasException {
        // set up
        SecurityRule securityRule = createSecurityRule();
        securityRule.setEtherType(EtherType.IPv6);

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
                Mockito.any(HttpRequestClientUtil.class),Mockito.anyString(), Mockito.any(Token.class))).
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
                Mockito.any(HttpRequestClientUtil.class),Mockito.anyString(), Mockito.any(Token.class))).
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
                Mockito.any(HttpRequestClientUtil.class),Mockito.anyString(), Mockito.any(Token.class))).
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
                Mockito.any(HttpRequestClientUtil.class),Mockito.anyString(), Mockito.any(Token.class))).
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
                Mockito.any(HttpRequestClientUtil.class),Mockito.anyString(), Mockito.any(Token.class))).
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
                "    \"jobinstanceid\": \""+ FAKE_SECURITY_RULE_ID +"\"\n" +
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
                            Mockito.any(HttpRequestClientUtil.class),Mockito.anyString(), Mockito.any(Token.class))).
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
}
