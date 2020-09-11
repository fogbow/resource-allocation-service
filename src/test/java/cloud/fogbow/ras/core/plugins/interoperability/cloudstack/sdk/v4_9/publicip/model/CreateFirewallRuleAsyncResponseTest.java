package cloud.fogbow.ras.core.plugins.interoperability.cloudstack.sdk.v4_9.publicip.model;

import cloud.fogbow.ras.core.plugins.interoperability.cloudstack.CloudstackTestUtils;
import cloud.fogbow.ras.core.plugins.interoperability.cloudstack.sdk.v4_9.publicip.model.CreateFirewallRuleAsyncResponse;
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
