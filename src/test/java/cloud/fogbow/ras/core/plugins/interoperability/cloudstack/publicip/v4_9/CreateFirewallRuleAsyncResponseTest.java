package cloud.fogbow.ras.core.plugins.interoperability.cloudstack.publicip.v4_9;

import cloud.fogbow.ras.core.plugins.interoperability.cloudstack.CloudstackTestUtils;
import org.junit.Assert;
import org.junit.Test;

import java.io.IOException;

public class CreateFirewallRuleAsyncResponseTest {

    // test case: When calling the fromJson method, it must verify
    // if It returns the right CreateFirewallRuleAsyncResponse.
    @Test
    public void testFromJsonSuccessfully() throws IOException {
        // set up
        String jobId = "1";
        String firewallRuleAsyncResponseJson =
                CloudstackTestUtils.createFirewallRuleAsyncResponseJson(jobId);

        // execute
        CreateFirewallRuleAsyncResponse createFirewallRuleAsyncResponse =
                CreateFirewallRuleAsyncResponse.fromJson(firewallRuleAsyncResponseJson);

        // verify
        Assert.assertEquals(jobId, createFirewallRuleAsyncResponse.getJobId());
    }

}
