package cloud.fogbow.ras.core.plugins.interoperability.cloudstack.sdk.v4_9.securityrule.model;

import cloud.fogbow.ras.core.plugins.interoperability.cloudstack.CloudstackTestUtils;
import cloud.fogbow.ras.core.plugins.interoperability.cloudstack.sdk.v4_9.publicip.model.CreateFirewallRuleAsyncResponse;
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
        String jobId = "2";
        String createFirewallRuleResponseJson =
                CloudstackTestUtils.createFirewallRuleAsyncResponseJson(jobId);

        // execute
        CreateFirewallRuleAsyncResponse response =
                CreateFirewallRuleAsyncResponse.fromJson(createFirewallRuleResponseJson);

        // verify
        Assert.assertEquals(jobId, response.getJobId());
    }

}
