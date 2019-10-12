package cloud.fogbow.ras.core.plugins.interoperability.cloudstack.attachment.v4_9;

import cloud.fogbow.common.exceptions.UnexpectedException;
import cloud.fogbow.ras.constants.Messages;
import cloud.fogbow.ras.core.plugins.interoperability.cloudstack.CloudStackCloudUtils;
import cloud.fogbow.ras.core.plugins.interoperability.cloudstack.CloudStackErrorResponse;
import cloud.fogbow.ras.core.plugins.interoperability.cloudstack.CloudstackTestUtils;
import org.apache.http.HttpStatus;
import org.junit.Assert;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

import java.io.IOException;

import static cloud.fogbow.ras.core.plugins.interoperability.cloudstack.attachment.v4_9.AttachmentJobStatusResponse.NO_FAILURE_EXCEPTION_MESSAGE;

public class AttachmentJobStatusResponseTest {

    @Rule
    public ExpectedException expectedException = ExpectedException.none();

    // test case: When calling the fromJson method, it must verify
    // if It returns the right AttachmentJobStatusResponse.
    @Test
    public void testFromJsonSuccessfully() throws IOException {
        // set up
        String jobId = "jobId";
        int jobStatus = CloudStackCloudUtils.JOB_STATUS_COMPLETE;
        String volumeId = "volumeId";
        int deviceId = 2;
        String state = "state";
        String virtualMachineId = "virtualMachineId";
        String attachmentJobStatusResponseJson = CloudstackTestUtils.createAttachmentJobStatusResponseJson(
                jobStatus, volumeId, deviceId, virtualMachineId, state, jobId);

        // execute
        AttachmentJobStatusResponse attachmentJobStatusResponse =
                AttachmentJobStatusResponse.fromJson(attachmentJobStatusResponseJson);

        // verify
        Assert.assertEquals(jobStatus, attachmentJobStatusResponse.getJobStatus());
        AttachmentJobStatusResponse.Volume volume = attachmentJobStatusResponse.getVolume();
        Assert.assertEquals(volumeId, volume.getId());
        Assert.assertEquals(jobId, volume.getJobId());
        Assert.assertEquals(virtualMachineId, volume.getVirtualMachineId());
        Assert.assertEquals(state, volume.getState());
        Assert.assertEquals(deviceId, volume.getDeviceId());
    }

    // test case: When calling the fromJson method with error json response,
    // it must verify if It returns the rigth CloudStackErrorResponse.
    @Test
    public void testFromJsonFail() throws IOException, UnexpectedException {
        // set up
        String errorText = "anyString";
        int jobStatus = CloudStackCloudUtils.JOB_STATUS_FAILURE;
        Integer errorCode = HttpStatus.SC_BAD_REQUEST;
        String volumesErrorResponseJson = CloudstackTestUtils
                .createAsyncErrorResponseJson(jobStatus, errorCode, errorText);

        // execute
        AttachmentJobStatusResponse attachmentJobStatusResponse =
                AttachmentJobStatusResponse.fromJson(volumesErrorResponseJson);

        // verify
        CloudStackErrorResponse errorResponse = attachmentJobStatusResponse.getErrorResponse();
        Assert.assertEquals(errorCode, errorResponse.getErrorCode());
        Assert.assertEquals(errorText, errorResponse.getErrorText());
    }

    // test case: When calling the getErrorResponse method with no error json response,
    // it must verify if It throws an UnexpectedException.
    @Test
    public void testGetErrorResponseFail() throws IOException, UnexpectedException {
        // set up
        int jobStatus = CloudStackCloudUtils.JOB_STATUS_COMPLETE;
        String attachmentJobStatusResponseJson = CloudstackTestUtils.createAttachmentJobStatusResponseJson(
                jobStatus, "", 0, "", "", "");

        AttachmentJobStatusResponse attachmentJobStatusResponse =
                AttachmentJobStatusResponse.fromJson(attachmentJobStatusResponseJson);

        // verify
        this.expectedException.expect(UnexpectedException.class);
        this.expectedException.expectMessage(
                String.format(Messages.Exception.UNEXPECTED_OPERATION_S, NO_FAILURE_EXCEPTION_MESSAGE));

        // execute
        attachmentJobStatusResponse.getErrorResponse();
    }

}
