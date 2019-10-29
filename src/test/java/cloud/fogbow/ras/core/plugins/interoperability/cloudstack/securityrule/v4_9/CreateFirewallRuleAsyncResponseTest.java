package cloud.fogbow.ras.core.plugins.interoperability.cloudstack.securityrule.v4_9;

import cloud.fogbow.ras.core.plugins.interoperability.cloudstack.CloudstackTestUtils;
import cloud.fogbow.ras.core.plugins.interoperability.cloudstack.publicip.v4_9.CreateFirewallRuleAsyncResponse;
import org.junit.Assert;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

import java.io.IOException;

public class CreateFirewallRuleAsyncResponseTest {

    @Rule
    public ExpectedException expectedException = ExpectedException.none();

    // test case: When calling the fromJson method, it must verify
    // if It returns the right CreateFirewallRuleAsyncResponse.
    @Test
    public void testFromJsonSuccessfully() throws IOException {
        // set up
        String jobId = "1";
        String createFirewallRuleResponseJson =
                CloudstackTestUtils.createFirewallRuleAsyncResponseJson(jobId);

        // execute
        CreateFirewallRuleAsyncResponse response =
                CreateFirewallRuleAsyncResponse.fromJson(createFirewallRuleResponseJson);

        // verify
        Assert.assertEquals(jobId, response.getJobId());
    }

}
