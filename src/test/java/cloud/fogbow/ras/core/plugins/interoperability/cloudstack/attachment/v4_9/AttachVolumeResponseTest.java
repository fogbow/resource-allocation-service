package cloud.fogbow.ras.core.plugins.interoperability.cloudstack.attachment.v4_9;

import cloud.fogbow.ras.core.plugins.interoperability.cloudstack.CloudstackTestUtils;
import org.apache.http.HttpStatus;
import org.apache.http.client.HttpResponseException;
import org.junit.Assert;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

import java.io.IOException;

public class AttachVolumeResponseTest {

    @Rule
    public ExpectedException expectedException = ExpectedException.none();

    // test case: When calling the fromJson method, it must verify
    // if It returns the right AttachVolumeResponse.
    @Test
    public void testFromJsonSuccessfully() throws Exception {
        // set up
        String jobId = "1";
        String getAttachVolumeResponseJson = CloudstackTestUtils.createAttachVolumeResponseJson(jobId);

        // execute
        AttachVolumeResponse attachVolumeResponse =
                AttachVolumeResponse.fromJson(getAttachVolumeResponseJson);

        // verify
        Assert.assertEquals(jobId, attachVolumeResponse.getJobId());
    }

    // test case: When calling the fromJson method with error json response,
    // it must verify if It throws a HttpResponseException.
    @Test
    public void testFromJsonFail() throws IOException {
        // set up
        String errorText = "anyString";
        int errorCode = HttpStatus.SC_BAD_REQUEST;
        String volumesErrorResponseJson = CloudstackTestUtils
                .createAttachVolumeErrorResponseJson(errorCode, errorText);

        // verify
        this.expectedException.expect(HttpResponseException.class);
        this.expectedException.expectMessage(errorText);

        // execute
        AttachVolumeResponse.fromJson(volumesErrorResponseJson);
    }

}
