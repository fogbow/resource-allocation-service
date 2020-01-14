package cloud.fogbow.ras.core.plugins.interoperability.cloudstack.publicip.v4_9;

import cloud.fogbow.ras.core.plugins.interoperability.cloudstack.CloudstackTestUtils;
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
