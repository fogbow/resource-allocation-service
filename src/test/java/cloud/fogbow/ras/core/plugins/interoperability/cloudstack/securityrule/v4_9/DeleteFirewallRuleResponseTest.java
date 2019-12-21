package cloud.fogbow.ras.core.plugins.interoperability.cloudstack.securityrule.v4_9;

import cloud.fogbow.ras.core.plugins.interoperability.cloudstack.CloudstackTestUtils;
import org.junit.Assert;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

import java.io.IOException;

public class DeleteFirewallRuleResponseTest {

    @Rule
    public ExpectedException expectedException = ExpectedException.none();

    // test case: When calling the fromJson method, it must verify
    // if It returns the right DeleteFirewallRuleAsyncResponse.
    @Test
    public void testFromJsonSuccessfully() throws IOException {
        // set up
        String jobId = "2";
        String deleteFirewallRuleAsyncResponseJson =
                CloudstackTestUtils.deleteFirewallRuleAsyncResponseJson(jobId);

        // execute
        DeleteFirewallRuleResponse response =
                DeleteFirewallRuleResponse.fromJson(deleteFirewallRuleAsyncResponseJson);

        // verify
        Assert.assertEquals(jobId, response.getJobId());
    }

}
