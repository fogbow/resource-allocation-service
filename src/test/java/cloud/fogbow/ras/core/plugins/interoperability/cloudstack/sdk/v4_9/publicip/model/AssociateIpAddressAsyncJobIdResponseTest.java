package cloud.fogbow.ras.core.plugins.interoperability.cloudstack.sdk.v4_9.publicip.model;

import cloud.fogbow.ras.core.plugins.interoperability.cloudstack.CloudstackTestUtils;
import cloud.fogbow.ras.core.plugins.interoperability.cloudstack.sdk.v4_9.publicip.model.AssociateIpAddressAsyncJobIdResponse;
import org.junit.Assert;
import org.junit.Test;

import java.io.IOException;

public class AssociateIpAddressAsyncJobIdResponseTest {

    // test case: When calling the fromJson method, it must verify
    // if It returns the right AssociateIpAddressAsyncJobIdResponse.
    @Test
    public void testFromJsonSuccessfully() throws IOException {
        // set up
        String jobId = "1";
        String associateIpAddressAsyncResponseJson =
                CloudstackTestUtils.createAssociateIpAddressAsyncResponseJson(jobId);

        // execute
        AssociateIpAddressAsyncJobIdResponse associateIpAddressAsyncJobIdResponse =
                AssociateIpAddressAsyncJobIdResponse.fromJson(associateIpAddressAsyncResponseJson);

        // verify
        Assert.assertEquals(jobId, associateIpAddressAsyncJobIdResponse.getJobId());
    }

}
