package cloud.fogbow.ras.core.plugins.interoperability.cloudstack;

import ch.qos.logback.classic.Level;
import cloud.fogbow.common.exceptions.FogbowException;
import cloud.fogbow.common.exceptions.UnexpectedException;
import cloud.fogbow.common.models.CloudStackUser;
import cloud.fogbow.common.util.connectivity.cloud.cloudstack.CloudStackHttpClient;
import cloud.fogbow.common.util.connectivity.cloud.cloudstack.CloudStackQueryAsyncJobResponse;
import cloud.fogbow.common.util.connectivity.cloud.cloudstack.CloudStackQueryJobResult;
import cloud.fogbow.common.util.connectivity.cloud.cloudstack.CloudStackUrlUtil;
import cloud.fogbow.ras.constants.Messages;
import cloud.fogbow.ras.core.LoggerAssert;
import cloud.fogbow.ras.core.TestUtils;
import org.apache.http.client.HttpResponseException;
import org.junit.Assert;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.junit.runner.RunWith;
import org.mockito.Mockito;
import org.mockito.internal.verification.VerificationModeFactory;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;

@RunWith(PowerMockRunner.class)
@PrepareForTest({CloudStackQueryJobResult.class , CloudStackQueryAsyncJobResponse.class,
        CloudStackCloudUtils.class, CloudStackQueryAsyncJobResponse.class, CloudStackUrlUtil.class,
        CloudStackQueryJobResult.class, Thread.class})
public class CloudStackCloudUtilsTest {

    @Rule
    private ExpectedException expectedException = ExpectedException.none();
    private LoggerAssert loggerTestChecking = new LoggerAssert(CloudStackCloudUtils.class);

    // test case: When calling the doRequest method with a right parameter,
    // it must verify if It returns the subnetInfo expected.
    @Test
    public void testDoRequestSuccessfully() throws FogbowException, HttpResponseException {
        // set up
        String responseExpected = "responseExpected";
        String url = "";

        CloudStackUser cloudStackUser = CloudstackTestUtils.CLOUD_STACK_USER;
        CloudStackHttpClient client = Mockito.mock(CloudStackHttpClient.class);
        Mockito.when(client.doGetRequest(Mockito.eq(url), Mockito.eq(cloudStackUser))).
                thenReturn(responseExpected);

        // exercise
        String response = CloudStackCloudUtils.doRequest(client, url, cloudStackUser);

        // verify
        Assert.assertEquals(responseExpected, response);
        Mockito.verify(client, Mockito.times(TestUtils.RUN_ONCE)).
                doGetRequest(Mockito.eq(url), Mockito.eq(cloudStackUser));
    }

    // test case: When calling the doRequest method and
    // it occurs an HttpResponseException, it must verify if It returns a HttpResponseException.
    @Test
    public void testDoRequestFail() throws FogbowException, HttpResponseException {
        // set up
        String url = "";
        CloudStackUser cloudStackUser = CloudstackTestUtils.CLOUD_STACK_USER;
        CloudStackHttpClient client = Mockito.mock(CloudStackHttpClient.class);
        Mockito.when(client.doGetRequest(Mockito.eq(url), Mockito.eq(cloudStackUser))).
                thenThrow(CloudstackTestUtils.createBadRequestHttpResponse());

        // verify
        this.expectedException.expect(HttpResponseException.class);
        this.expectedException.expectMessage(CloudstackTestUtils.BAD_REQUEST_MSG);

        // exercise
        CloudStackCloudUtils.doRequest(client, url, cloudStackUser);
    }

    // test case: When calling the doRequest method and
    // it occurs an FogbowException, it must verify if It returns a HttpResponseException.
    @Test
    public void testDoRequestFailWhenThrowFogbowException() throws FogbowException, HttpResponseException {
        // set up
        String url = "";
        String errorMessage = "error";
        CloudStackUser cloudStackUser = CloudstackTestUtils.CLOUD_STACK_USER;
        CloudStackHttpClient client = Mockito.mock(CloudStackHttpClient.class);
        Mockito.when(client.doGetRequest(Mockito.eq(url), Mockito.eq(cloudStackUser))).
                thenThrow(new FogbowException(errorMessage));

        // verify
        this.expectedException.expect(HttpResponseException.class);
        this.expectedException.expectMessage(errorMessage);

        // exercise
        CloudStackCloudUtils.doRequest(client, url, cloudStackUser);
    }

    // test case: When calling the waitForResult method and receives two processing status and one
    // success status, it must verify if It returns the right jobInstanceId.
    @Test
    public void testWaitForResultWhenWaitTwiceAndReceiveSuccess() throws Exception {
        // set up
        String url = "";
        String jobId = "jobId";
        CloudStackHttpClient client = Mockito.mock(CloudStackHttpClient.class);
        CloudStackUser cloudStackUser = CloudstackTestUtils.CLOUD_STACK_USER;

        CloudStackQueryAsyncJobResponse response = mockGetAsyncJobResponse();
        String jobInstanceIdExpected = "instanceId";
        Mockito.when(response.getJobInstanceId()).thenReturn(jobInstanceIdExpected);
        Mockito.when(response.getJobStatus())
                .thenReturn(CloudStackQueryJobResult.PROCESSING)
                .thenReturn(CloudStackQueryJobResult.PROCESSING)
                .thenReturn(CloudStackQueryJobResult.SUCCESS);

        mockGetTimeSleepThread();

        // exercise
        String jobInstanceId = CloudStackCloudUtils.waitForResult(client, url, jobId, cloudStackUser);

        // verify
        Assert.assertEquals(jobInstanceIdExpected, jobInstanceId);
        PowerMockito.verifyStatic(CloudStackCloudUtils.class, VerificationModeFactory.times(TestUtils.RUN_THRICE));
        CloudStackQueryJobResult.getQueryJobResult(
                Mockito.eq(client), Mockito.eq(url), Mockito.eq(jobId), Mockito.eq(cloudStackUser));
    }

    // test case: When calling the waitForResult method and receives only one
    // success status, it must verify if It returns the right jobInstanceId.
    @Test
    public void testWaitForResultWhenReceiveOnlySuccess() throws Exception {
        // set up
        String url = "";
        String jobId = "jobId";
        CloudStackHttpClient client = Mockito.mock(CloudStackHttpClient.class);
        CloudStackUser cloudStackUser = CloudstackTestUtils.CLOUD_STACK_USER;

        CloudStackQueryAsyncJobResponse response = mockGetAsyncJobResponse();
        String jobInstanceIdExpected = "instanceId";
        Mockito.when(response.getJobInstanceId()).thenReturn(jobInstanceIdExpected);
        Mockito.when(response.getJobStatus())
                .thenReturn(CloudStackQueryJobResult.SUCCESS);

        mockGetTimeSleepThread();

        // exercise
        String jobInstanceId = CloudStackCloudUtils.waitForResult(client, url, jobId, cloudStackUser);

        // verify
        Assert.assertEquals(jobInstanceIdExpected, jobInstanceId);
        PowerMockito.verifyStatic(CloudStackCloudUtils.class, VerificationModeFactory.times(TestUtils.RUN_ONCE));
        CloudStackQueryJobResult.getQueryJobResult(
                Mockito.eq(client), Mockito.eq(url), Mockito.eq(jobId), Mockito.eq(cloudStackUser));
    }

    // test case: When calling the waitForResult method and receives only processing status result,
    // it must verify if It throws a TimeoutCloudstackAsync because it's exceeded the try's limit.
    @Test
    public void testWaitForResultFail() throws Exception {
        // set up
        int extraGetQueryJobResult = 1;
        int totalTriesExpetected = CloudStackCloudUtils.MAX_TRIES + extraGetQueryJobResult;

        String url = "";
        String jobId = "jobId";
        CloudStackHttpClient client = Mockito.mock(CloudStackHttpClient.class);
        CloudStackUser cloudStackUser = CloudstackTestUtils.CLOUD_STACK_USER;

        CloudStackQueryAsyncJobResponse response = mockGetAsyncJobResponse();
        String jobInstanceIdExpected = "instanceId";
        Mockito.when(response.getJobInstanceId()).thenReturn(jobInstanceIdExpected);
        Mockito.when(response.getJobStatus()).thenReturn(CloudStackQueryJobResult.PROCESSING);

        mockGetTimeSleepThread();

        // verify
        this.expectedException.expect(CloudStackCloudUtils.TimeoutCloudstackAsync.class);

        // exercise
        try {
            CloudStackCloudUtils.waitForResult(client, url, jobId, cloudStackUser);
        } finally {
            // verify
            PowerMockito.verifyStatic(CloudStackCloudUtils.class,
                    VerificationModeFactory.times(totalTriesExpetected));
            CloudStackQueryJobResult.getQueryJobResult(
                    Mockito.eq(client), Mockito.eq(url), Mockito.eq(jobId), Mockito.eq(cloudStackUser));
        }
    }

    // test case: When calling the processJobResult method and job status is success,
    // it must verify if It returns the right instance Id.
    @Test
    public void testProcessJobResultWhenSuccessStatus() throws FogbowException {
        // set up
        String jobId = "jobId";
        String instanceIdExpected = "instanceId";
        CloudStackQueryAsyncJobResponse response = Mockito.mock(CloudStackQueryAsyncJobResponse.class);
        Mockito.when(response.getJobStatus()).thenReturn(CloudStackQueryJobResult.SUCCESS);
        Mockito.when(response.getJobInstanceId()).thenReturn(instanceIdExpected);

        // exercise
        String instanceId = CloudStackCloudUtils.processJobResult(response, jobId);
        // verify
        Assert.assertEquals(instanceIdExpected, instanceId);
    }

    // test case: When calling the processJobResult method and job status is failed,
    // it must verify if It throws a FogbowException.
    @Test
    public void testProcessJobResultWhenFailedStatus() throws FogbowException {
        // set up
        String jobId = "jobId";
        CloudStackQueryAsyncJobResponse response = Mockito.mock(CloudStackQueryAsyncJobResponse.class);
        Mockito.when(response.getJobStatus()).thenReturn(CloudStackQueryJobResult.FAILURE);

        this.expectedException.expect(FogbowException.class);
        this.expectedException.expectMessage(String.format(Messages.Exception.JOB_HAS_FAILED, jobId));

        // exercise
        CloudStackCloudUtils.processJobResult(response, jobId);
    }

    // test case: When calling the processJobResult method and job status is unknown,
    // it must verify if It throws an UnexpectedException.
    @Test
    public void testProcessJobResultFail() throws FogbowException {
        // set up
        String jobId = "jobId";
        int statusUnkown = -1;
        CloudStackQueryAsyncJobResponse response = Mockito.mock(CloudStackQueryAsyncJobResponse.class);
        Mockito.when(response.getJobStatus()).thenReturn(statusUnkown);

        this.expectedException.expect(UnexpectedException.class);
        this.expectedException.expectMessage(Messages.Error.UNEXPECTED_JOB_STATUS);

        // exercise
        CloudStackCloudUtils.processJobResult(response, jobId);
    }

    // test case: When calling the getAsyncJobResponse method, it must verify if
    // It returns the right CloudStackQueryAsyncJobResponse.
    @Test
    public void testGetAsyncJobResponse() throws FogbowException {
        // set up
        CloudStackHttpClient client =  Mockito.mock(CloudStackHttpClient.class);;
        String cloudstackUrl = "cloudstackUrl";
        String jobId = "jobId";
        CloudStackUser cloudstackUser = CloudstackTestUtils.CLOUD_STACK_USER;

        String responseStr = "response";
        PowerMockito.mockStatic(CloudStackQueryJobResult.class);
        PowerMockito.when(CloudStackQueryJobResult.getQueryJobResult(Mockito.eq(client),
                Mockito.eq(cloudstackUrl), Mockito.eq(jobId), Mockito.eq(cloudstackUser)))
                .thenReturn(responseStr);

        PowerMockito.mockStatic(CloudStackQueryAsyncJobResponse.class);
        CloudStackQueryAsyncJobResponse responseExpected =
                Mockito.mock(CloudStackQueryAsyncJobResponse.class);
        PowerMockito.when(CloudStackQueryAsyncJobResponse.fromJson(Mockito.eq(responseStr)))
                .thenReturn(responseExpected);

        CloudstackTestUtils.ignoringCloudStackUrl();

        // exercise
        CloudStackQueryAsyncJobResponse response = CloudStackCloudUtils.getAsyncJobResponse(
                client, cloudstackUrl, jobId, cloudstackUser);

        // verify
        Assert.assertEquals(responseExpected, response);
    }

    // test case: When calling the sleepThread method and occurs an InterruptedException,
    // it must verify if It shows the log related.
    @Test
    public void testSleepThreadFail() throws Exception {
        // set up
        PowerMockito.mockStatic(Thread.class);
        PowerMockito.doThrow(new InterruptedException()).when(Thread.class);
        Thread.sleep(Mockito.anyLong());

        // exercise
        CloudStackCloudUtils.sleepThread();

        // verify
        this.loggerTestChecking.assertEquals(1, Level.WARN, Messages.Warn.THREAD_INTERRUPTED);
    }

    private CloudStackQueryAsyncJobResponse mockGetAsyncJobResponse() throws FogbowException {
        CloudStackQueryAsyncJobResponse response = Mockito.mock(CloudStackQueryAsyncJobResponse.class);

        String responseStr = "anyString";
        PowerMockito.mockStatic(CloudStackQueryJobResult.class);
        PowerMockito.when(CloudStackQueryJobResult.getQueryJobResult(
                Mockito.any(), Mockito.any(), Mockito.any(), Mockito.any())).
                thenReturn(responseStr);

        PowerMockito.mockStatic(CloudStackQueryAsyncJobResponse.class);
        PowerMockito.when(CloudStackQueryAsyncJobResponse.fromJson(Mockito.eq(responseStr)))
                .thenReturn(response);

        return response;
    }

    private void mockGetTimeSleepThread() {
        long TINY_VALUE = 1L;
        PowerMockito.spy(CloudStackCloudUtils.class);
        PowerMockito.when(CloudStackCloudUtils.getTimeSleepThread()).thenReturn(TINY_VALUE);
    }

}
