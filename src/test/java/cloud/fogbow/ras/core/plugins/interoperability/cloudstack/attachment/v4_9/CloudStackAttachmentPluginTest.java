package cloud.fogbow.ras.core.plugins.interoperability.cloudstack.attachment.v4_9;

import ch.qos.logback.classic.Level;
import cloud.fogbow.common.exceptions.FogbowException;
import cloud.fogbow.common.exceptions.InvalidParameterException;
import cloud.fogbow.common.exceptions.UnexpectedException;
import cloud.fogbow.common.models.CloudStackUser;
import cloud.fogbow.common.util.PropertiesUtil;
import cloud.fogbow.common.util.connectivity.cloud.cloudstack.CloudStackHttpClient;
import cloud.fogbow.common.util.connectivity.cloud.cloudstack.CloudStackUrlUtil;
import cloud.fogbow.ras.api.http.response.AttachmentInstance;
import cloud.fogbow.ras.constants.Messages;
import cloud.fogbow.ras.core.BaseUnitTests;
import cloud.fogbow.ras.core.LoggerAssert;
import cloud.fogbow.ras.core.SharedOrderHolders;
import cloud.fogbow.ras.core.TestUtils;
import cloud.fogbow.ras.core.datastore.DatabaseManager;
import cloud.fogbow.ras.core.models.orders.AttachmentOrder;
import cloud.fogbow.ras.core.models.orders.ComputeOrder;
import cloud.fogbow.ras.core.models.orders.VolumeOrder;
import cloud.fogbow.ras.core.plugins.interoperability.cloudstack.CloudStackCloudUtils;
import cloud.fogbow.ras.core.plugins.interoperability.cloudstack.CloudstackTestUtils;
import cloud.fogbow.ras.core.plugins.interoperability.cloudstack.RequestMatcher;
import org.apache.http.client.HttpResponseException;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.junit.runner.RunWith;
import org.mockito.Mockito;
import org.mockito.internal.verification.VerificationModeFactory;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;

import java.io.IOException;
import java.util.Properties;

@RunWith(PowerMockRunner.class)
@PrepareForTest({SharedOrderHolders.class, CloudStackUrlUtil.class, DetachVolumeResponse.class,
        DatabaseManager.class, AttachVolumeResponse.class, CloudStackCloudUtils.class,
        DetachVolumeResponse.class, AttachmentJobStatusResponse.class})
public class CloudStackAttachmentPluginTest extends BaseUnitTests {

    @Rule
    private ExpectedException expectedException = ExpectedException.none();
    private LoggerAssert loggerTestChecking = new LoggerAssert(CloudStackAttachmentPlugin.class);

    private CloudStackAttachmentPlugin plugin;
    private CloudStackHttpClient client;
    private CloudStackUser cloudStackUser;
    private AttachmentOrder attachmentOrder;
    private String cloudStackUrl;

    @Before
    public void setUp() throws InvalidParameterException, UnexpectedException {
        String cloudStackConfFilePath = CloudstackTestUtils.CLOUDSTACK_CONF_FILE_PATH;
        Properties properties = PropertiesUtil.readProperties(cloudStackConfFilePath);

        this.client = Mockito.mock(CloudStackHttpClient.class);
        this.plugin = Mockito.spy(new CloudStackAttachmentPlugin(cloudStackConfFilePath));
        this.plugin.setClient(this.client);
        this.cloudStackUser = CloudstackTestUtils.CLOUD_STACK_USER;
        this.cloudStackUrl = properties.getProperty(CloudStackCloudUtils.CLOUDSTACK_URL_CONFIG);

        this.attachmentOrder = Mockito.spy(
                this.testUtils.createLocalAttachmentOrder(new ComputeOrder(), new VolumeOrder()));
        Mockito.doReturn("1").when(this.attachmentOrder).getVolumeId();
        Mockito.doReturn("2").when(this.attachmentOrder).getComputeId();
        Mockito.doReturn("3").when(this.attachmentOrder).getInstanceId();

        this.testUtils.mockReadOrdersFromDataBase();
        CloudstackTestUtils.ignoringCloudStackUrl();
    }

    // test case: When calling the requestInstance method with secondary methods mocked,
    // it must verify if the doRequestInstance is called with the right parameters;
    // this includes the checking of the Cloudstack request.
    @Test
    public void testRequestInstanceSuccessfully() throws FogbowException {
        // set up
        String volumeIdExpected = this.attachmentOrder.getVolumeId();
        String virtualMachineIdExpected = this.attachmentOrder.getComputeId();

        String attachmentIdExpeted = "attachmentIdExpected";
        Mockito.doReturn(attachmentIdExpeted).when(this.plugin).doRequestInstance(
                Mockito.any(), Mockito.eq(this.cloudStackUser));

        AttachVolumeRequest request = new AttachVolumeRequest.Builder()
                .id(volumeIdExpected)
                .virtualMachineId(virtualMachineIdExpected)
                .build(this.cloudStackUrl);

        // exercise
        String attachmentId = this.plugin.requestInstance(
                this.attachmentOrder, this.cloudStackUser);

        // verify
        Assert.assertEquals(attachmentIdExpeted, attachmentId);
        RequestMatcher<AttachVolumeRequest> matcher = new RequestMatcher.AttachVolume(request);
        Mockito.verify(this.plugin, Mockito.times(TestUtils.RUN_ONCE))
                .doRequestInstance(Mockito.argThat(matcher), Mockito.eq(this.cloudStackUser));
    }

    // test case: When calling the requestInstance method and occurs an FogbowException,
    // it must verify if It returns an FogbowException.
    @Test(expected = FogbowException.class)
    public void testRequestInstanceFail() throws FogbowException {
        // set up
        Mockito.doThrow(new FogbowException()).when(this.plugin)
                .doRequestInstance(Mockito.any(), Mockito.eq(this.cloudStackUser));

        this.plugin.requestInstance(this.attachmentOrder, this.cloudStackUser);
    }

    // test case: When calling the doRequestInstance method with secondary methods mocked,
    // it must verify if It returns the instanceId(jobId).
    @Test
    public void testDoRequestInstanceSuccessfully() throws FogbowException, HttpResponseException {
        // set up
        AttachVolumeRequest request = new AttachVolumeRequest.Builder().build("");

        PowerMockito.mockStatic(CloudStackCloudUtils.class);
        String resposeStr = "anything";
        PowerMockito.when(CloudStackCloudUtils.doGet(
                Mockito.eq(this.client),
                Mockito.eq(request.getUriBuilder().toString()), Mockito.eq(cloudStackUser))).
                thenReturn(resposeStr);

        PowerMockito.mockStatic(AttachVolumeResponse.class);
        String jobIdExpexted = "jobIdExpected";
        AttachVolumeResponse response = Mockito.mock(AttachVolumeResponse.class);
        Mockito.when(response.getJobId()).thenReturn(jobIdExpexted);
        PowerMockito.when(AttachVolumeResponse.fromJson(Mockito.eq(resposeStr))).thenReturn(response);

        // exercise
        String jobId = this.plugin.doRequestInstance(request, this.cloudStackUser);

        // verify
        Assert.assertEquals(jobIdExpexted, jobId);
    }

    // test case: When calling the doRequestInstance method with secondary methods mocked and
    // it occurs an HttpResponseException, it must verify if It returns a FogbowException.
    @Test
    public void testDoRequestInstanceFail() throws FogbowException, HttpResponseException {
        // set up
        AttachVolumeRequest request = new AttachVolumeRequest.Builder().build("");

        PowerMockito.mockStatic(CloudStackCloudUtils.class);
        PowerMockito.when(CloudStackCloudUtils.doGet(
                Mockito.eq(this.client),
                Mockito.eq(request.getUriBuilder().toString()), Mockito.eq(cloudStackUser))).
                thenThrow(CloudstackTestUtils.createBadRequestHttpResponse());

        // verify
        this.expectedException.expect(FogbowException.class);
        this.expectedException.expectMessage(CloudstackTestUtils.BAD_REQUEST_MSG);

        // exercise
        this.plugin.doRequestInstance(request, this.cloudStackUser);
    }

    // test case: When calling the getInstance method with secondary methods mocked,
    // it must verify if the doGetInstance is called with the right parameters;
    // this includes the checking of the Cloudstack request.
    @Test
    public void testGetInstanceSuccessfully() throws  FogbowException {
        // set up
        String jobId = this.attachmentOrder.getInstanceId();

        AttachmentInstance attachmentInstanceExpected = Mockito.mock(AttachmentInstance.class);
        Mockito.doReturn(attachmentInstanceExpected).when(this.plugin).doGetInstance(
                Mockito.eq(this.attachmentOrder), Mockito.any(AttachmentJobStatusRequest.class),
                Mockito.eq(this.cloudStackUser));

        AttachmentJobStatusRequest request = new AttachmentJobStatusRequest.Builder()
                .jobId(jobId)
                .build(this.cloudStackUrl);
        
        // exercise
        AttachmentInstance attachmentInstance = this.plugin.getInstance(
                this.attachmentOrder, this.cloudStackUser);

        // verify
        Assert.assertEquals(attachmentInstanceExpected, attachmentInstance);
        RequestMatcher<AttachmentJobStatusRequest> matcher = new RequestMatcher.AttachmentJobStatus(request);
        Mockito.verify(this.plugin, Mockito.times(TestUtils.RUN_ONCE)).doGetInstance(
                Mockito.eq(this.attachmentOrder), Mockito.argThat(matcher), Mockito.eq(this.cloudStackUser));
    }

    // test case: When calling the getInstance method and occurs an FogbowException,
    // it must verify if It returns an FogbowException.
    @Test(expected = FogbowException.class)
    public void testGetInstanceFail() throws FogbowException {
        // set up
        Mockito.doThrow(new FogbowException()).when(this.plugin).doGetInstance(
                Mockito.eq(this.attachmentOrder), Mockito.any(), Mockito.eq(this.cloudStackUser));

        this.plugin.getInstance(this.attachmentOrder, this.cloudStackUser);
    }

    // test case: When calling the doGetInstance method with secondary methods mocked,
    // it must verify if It returns the right AttachmentInstance.
    @Test
    public void testDoGetInstanceSuccessfully() throws FogbowException, HttpResponseException {
        // set up
        AttachmentJobStatusRequest request = new AttachmentJobStatusRequest.Builder().build("");

        PowerMockito.mockStatic(CloudStackCloudUtils.class);
        String resposeStr = "anything";
        PowerMockito.when(CloudStackCloudUtils.doGet(
                Mockito.eq(this.client),
                Mockito.eq(request.getUriBuilder().toString()), Mockito.eq(cloudStackUser))).
                thenReturn(resposeStr);

        PowerMockito.mockStatic(AttachmentJobStatusResponse.class);
        AttachmentJobStatusResponse response = Mockito.mock(AttachmentJobStatusResponse.class);
        PowerMockito.when(AttachmentJobStatusResponse.fromJson(Mockito.eq(resposeStr))).thenReturn(response);

        AttachmentInstance attachmentInstanceExpected = Mockito.mock(AttachmentInstance.class);
        String instanceId = this.attachmentOrder.getInstanceId();
        Mockito.doReturn(attachmentInstanceExpected).when(this.plugin).loadInstanceByJobStatus(
                Mockito.eq(instanceId), Mockito.eq(response));

        // exercise
        AttachmentInstance attachmentInstance = this.plugin.doGetInstance(
                this.attachmentOrder, request, this.cloudStackUser);

        // verify
        Assert.assertEquals(attachmentInstanceExpected, attachmentInstance);
    }

    // test case: When calling the doGetInstance method with secondary methods mocked and
    // it occurs an HttpResponseException, it must verify if It returns a FogbowException.
    @Test
    public void testDoGetInstanceFail() throws FogbowException, HttpResponseException {
        // set up
        AttachmentJobStatusRequest request = new AttachmentJobStatusRequest.Builder().build("");

        PowerMockito.mockStatic(CloudStackCloudUtils.class);
        PowerMockito.when(CloudStackCloudUtils.doGet(
                Mockito.eq(this.client),
                Mockito.eq(request.getUriBuilder().toString()), Mockito.eq(cloudStackUser))).
                thenThrow(CloudstackTestUtils.createBadRequestHttpResponse());

        // verify
        this.expectedException.expect(FogbowException.class);
        this.expectedException.expectMessage(CloudstackTestUtils.BAD_REQUEST_MSG);

        // exercise
        try {
            this.plugin.doGetInstance(this.attachmentOrder, request, this.cloudStackUser);
        } catch (Exception e) {
            throw e;
        } finally {
            // verify
            Mockito.verify(this.plugin, Mockito.times(TestUtils.NEVER_RUN)).loadInstanceByJobStatus(
                    Mockito.anyString(), Mockito.any());
        }
    }

    // test case: When calling the loadInstanceByJobStatus method with status complete,
    // it must verify if It returns a complete AttachmentInstance.
    @Test
    public void testLoadInstanceByJobStatusWithCompleteInstance() throws UnexpectedException {
        // set up
        String attachmentInstanceId = "id";
        Integer deviceIdExpected = 1;
        String jobIdExpected = "jobId";
        String stateExpected= "state";
        String virtualMachineIdExpected = "vmId";
        String volumeIdExpected = "volumeId";

        AttachmentJobStatusResponse response = Mockito.mock(AttachmentJobStatusResponse.class);
        Integer jobStatus = CloudStackCloudUtils.JOB_STATUS_COMPLETE;
        Mockito.when(response.getJobStatus()).thenReturn(jobStatus);
        AttachmentJobStatusResponse.Volume volume = Mockito.mock(AttachmentJobStatusResponse.Volume.class);
        Mockito.when(volume.getId()).thenReturn(volumeIdExpected);
        Mockito.when(volume.getDeviceId()).thenReturn(deviceIdExpected);
        Mockito.when(volume.getJobId()).thenReturn(jobIdExpected);
        Mockito.when(volume.getState()).thenReturn(stateExpected);
        Mockito.when(volume.getVirtualMachineId()).thenReturn(virtualMachineIdExpected);
        Mockito.when(response.getVolume()).thenReturn(volume);

        // exercise
        AttachmentInstance attachmentInstance = this.plugin.loadInstanceByJobStatus(
                attachmentInstanceId, response);

        // verify
        Assert.assertEquals(jobIdExpected, attachmentInstance.getId());
        Assert.assertEquals(virtualMachineIdExpected, attachmentInstance.getComputeId());
        Assert.assertEquals(String.valueOf(deviceIdExpected), attachmentInstance.getDevice());
        Assert.assertEquals(volumeIdExpected, attachmentInstance.getVolumeId());
        Assert.assertEquals(stateExpected, attachmentInstance.getCloudState());
    }

    // test case: When calling the loadInstanceByJobStatus method with status pending,
    // it must verify if It returns a pending AttachmentInstance.
    @Test
    public void testLoadInstanceByJobStatusWithPendingInstance() throws UnexpectedException {
        // set up
        String attachmentInstanceId = "id";

        AttachmentJobStatusResponse response = Mockito.mock(AttachmentJobStatusResponse.class);
        Integer jobStatus = CloudStackCloudUtils.JOB_STATUS_PENDING;
        Mockito.when(response.getJobStatus()).thenReturn(jobStatus);

        // exercise
        AttachmentInstance attachmentInstance = this.plugin.loadInstanceByJobStatus(
                attachmentInstanceId, response);

        // verify
        Assert.assertEquals(attachmentInstanceId, attachmentInstance.getId());
        Assert.assertEquals(CloudStackCloudUtils.PENDING_STATE, attachmentInstance.getCloudState());
    }

    // test case: When calling the loadInstanceByJobStatus method with status failed,
    // it must verify if It returns a failed AttachmentInstance.
    @Test
    public void testLoadInstanceByJobStatusWithFailedInstance() throws UnexpectedException {
        // set up
        String attachmentInstanceId = "id";

        AttachmentJobStatusResponse response = Mockito.mock(AttachmentJobStatusResponse.class);
        Integer jobStatus = CloudStackCloudUtils.JOB_STATUS_FAILURE;
        Mockito.when(response.getJobStatus()).thenReturn(jobStatus);

        Mockito.doNothing().when(this.plugin).logFailure(Mockito.eq(response));

        // exercise
        AttachmentInstance attachmentInstance = this.plugin.loadInstanceByJobStatus(
                attachmentInstanceId, response);

        // verify
        Assert.assertEquals(attachmentInstanceId, attachmentInstance.getId());
        Assert.assertEquals(CloudStackCloudUtils.FAILURE_STATE, attachmentInstance.getCloudState());
    }

    // test case: When calling the loadInstanceByJobStatus method with unknown status code,
    // it must verify if It returns an
    @Test
    public void testLoadInstanceByJobStatusFail() throws UnexpectedException {
        // set up
        String attachmentInstanceId = "id";

        AttachmentJobStatusResponse response = Mockito.mock(AttachmentJobStatusResponse.class);
        Integer jobStatusUnknown = -1;
        Mockito.when(response.getJobStatus()).thenReturn(jobStatusUnknown);

        // verify
        this.expectedException.expect(UnexpectedException.class);
        this.expectedException.expectMessage(Messages.Error.UNEXPECTED_JOB_STATUS);

        // exercise
        this.plugin.loadInstanceByJobStatus(attachmentInstanceId, response);
    }

    // test case: When calling the logFailure method, it must verify if It shows the error log expected.
    @Test
    public void testLogFailureSuccessfully() throws IOException {
        // set up
        int errorCode = 0;
        String errorText = "anything";
        String responseStr = CloudstackTestUtils.createAsyncErrorResponseJson(
                CloudStackCloudUtils.JOB_STATUS_FAILURE, errorCode, errorText);
        AttachmentJobStatusResponse response = AttachmentJobStatusResponse.fromJson(responseStr);

        String msgFailed = String.format(CloudStackAttachmentPlugin.FAILED_ATTACH_ERROR_MESSAGE,
                errorCode, errorText);
        String msgError = String.format(Messages.Error.ERROR_WHILE_ATTACHING_VOLUME_GENERAL, msgFailed);

        // verify
        this.plugin.logFailure(response);

        // exercise
        this.loggerTestChecking.assertEquals(1, Level.ERROR, msgError);
    }

    // test case: When calling the logFailure method and occurs an UnexpectedException,
    // it must verify if It shows the warn log expected;
    @Test
    public void testLogFailureFail() throws UnexpectedException {
        // set up
        AttachmentJobStatusResponse response = Mockito.mock(AttachmentJobStatusResponse.class);
        Mockito.when(response.getErrorResponse()).thenThrow(new UnexpectedException());

        // verify
        this.plugin.logFailure(response);

        // exercise
        this.loggerTestChecking.assertEquals(1, Level.WARN, "");
    }

    // test case: When calling the deleteInstance method with secondary methods mocked,
    // it must verify if the doDeleteInstance is called with the right parameters;
    // this includes the checking of the Cloudstack request.
    @Test
    public void testDeleteInstanceSuccessfully() throws FogbowException {
        // set up
        String volumeId = this.attachmentOrder.getVolumeId();

        Mockito.doNothing().when(this.plugin).doDeleteInstance(
                Mockito.any(), Mockito.eq(this.cloudStackUser));

        DetachVolumeRequest request = new DetachVolumeRequest.Builder()
                .id(volumeId)
                .build(this.cloudStackUrl);

        // exercise
        this.plugin.deleteInstance(this.attachmentOrder, this.cloudStackUser);

        // verify
        RequestMatcher<DetachVolumeRequest> matcher = new RequestMatcher.DetachVolume(request);
        Mockito.verify(this.plugin, Mockito.times(TestUtils.RUN_ONCE))
                .doDeleteInstance(Mockito.argThat(matcher), Mockito.eq(this.cloudStackUser));
    }

    // test case: When calling the deleteInstance method and occurs an FogbowException,
    // it must verify if It returns an FogbowException.

    @Test(expected = FogbowException.class)
    public void testDeleteInstanceFail() throws FogbowException {
        // set up
        Mockito.doThrow(new FogbowException()).when(this.plugin)
                .doDeleteInstance(Mockito.any(), Mockito.eq(this.cloudStackUser));

        this.plugin.deleteInstance(this.attachmentOrder, this.cloudStackUser);
    }
    // test case: When calling the doDeleteInstance method with secondary methods mocked,
    // it must verify if It returns the instanceId(jobId).
    @Test
    public void testDoDeleteInstanceSuccessfully() throws FogbowException, HttpResponseException {
        // set up
        DetachVolumeRequest request = new DetachVolumeRequest.Builder().build("");

        PowerMockito.mockStatic(CloudStackCloudUtils.class);
        String resposenStr = "anyString";
        PowerMockito.when(CloudStackCloudUtils.doGet(Mockito.any(
                CloudStackHttpClient.class), Mockito.anyString(), Mockito.eq(this.cloudStackUser)))
                .thenReturn(resposenStr);

        PowerMockito.mockStatic(DetachVolumeResponse.class);
        DetachVolumeResponse detachVolumeResponse = Mockito.mock(DetachVolumeResponse.class);
        PowerMockito.when(DetachVolumeResponse.fromJson(Mockito.eq(resposenStr)))
                .thenReturn(detachVolumeResponse);


        // exercise
        this.plugin.doDeleteInstance(request, this.cloudStackUser);

        // verify
        PowerMockito.verifyStatic(DetachVolumeResponse.class, VerificationModeFactory.times(TestUtils.RUN_ONCE));
        DetachVolumeResponse.fromJson(Mockito.eq(resposenStr));
    }

    // test case: When calling the doDeleteInstance method with secondary methods mocked and
    // it occurs an HttpResponseException, it must verify if It returns a FogbowException.
    @Test
    public void testDoDeleteInstanceFail() throws FogbowException, HttpResponseException {
        // set up
        DetachVolumeRequest request = new DetachVolumeRequest.Builder().build("");

        PowerMockito.mockStatic(CloudStackCloudUtils.class);
        PowerMockito.when(CloudStackCloudUtils.doGet(Mockito.any(
                CloudStackHttpClient.class), Mockito.anyString(), Mockito.eq(this.cloudStackUser)))
                .thenThrow(CloudstackTestUtils.createBadRequestHttpResponse());

        // verify
        this.expectedException.expect(FogbowException.class);
        this.expectedException.expectMessage(CloudstackTestUtils.BAD_REQUEST_MSG);

        // exercise
        this.plugin.doDeleteInstance(request, this.cloudStackUser);
    }

    // test case: When calling the doDeleteInstance method with secondary methods mocked and
    // it occurs an HttpResponseException in the DetachVolumeResponse.fromJson,
    // it must verify if It returns a FogbowException.
    @Test
    public void testDoDeleteInstanceFailOnFromJson() throws FogbowException, HttpResponseException {
        // set up
        DetachVolumeRequest request = new DetachVolumeRequest.Builder().build("");

        PowerMockito.mockStatic(CloudStackCloudUtils.class);
        String responseStr = "anyString";
        PowerMockito.when(CloudStackCloudUtils.doGet(Mockito.any(
                CloudStackHttpClient.class), Mockito.anyString(), Mockito.eq(this.cloudStackUser)))
                .thenReturn(responseStr);

        PowerMockito.mockStatic(DetachVolumeResponse.class);
        PowerMockito.when(DetachVolumeResponse.fromJson(Mockito.eq(responseStr))).
                thenThrow(CloudstackTestUtils.createBadRequestHttpResponse());

        // verify
        this.expectedException.expect(FogbowException.class);
        this.expectedException.expectMessage(CloudstackTestUtils.BAD_REQUEST_MSG);

        // exercise
        this.plugin.doDeleteInstance(request, this.cloudStackUser);
    }

}
