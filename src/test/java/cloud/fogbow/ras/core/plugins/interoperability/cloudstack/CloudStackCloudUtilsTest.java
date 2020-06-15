package cloud.fogbow.ras.core.plugins.interoperability.cloudstack;

import cloud.fogbow.common.exceptions.FogbowException;
import cloud.fogbow.common.exceptions.UnavailableProviderException;
import cloud.fogbow.common.exceptions.UnexpectedException;
import cloud.fogbow.common.models.CloudStackUser;
import cloud.fogbow.common.util.connectivity.cloud.cloudstack.CloudStackHttpClient;
import cloud.fogbow.common.util.connectivity.cloud.cloudstack.CloudStackQueryAsyncJobResponse;
import cloud.fogbow.common.util.connectivity.cloud.cloudstack.CloudStackQueryJobResult;
import cloud.fogbow.common.util.connectivity.cloud.cloudstack.CloudStackUrlUtil;
import cloud.fogbow.ras.constants.Messages;
import cloud.fogbow.ras.constants.SystemConstants;
import cloud.fogbow.ras.core.LoggerAssert;
import cloud.fogbow.ras.core.TestUtils;
import cloud.fogbow.ras.core.plugins.interoperability.cloudstack.volume.v4_9.GetAllDiskOfferingsRequest;
import cloud.fogbow.ras.core.plugins.interoperability.cloudstack.volume.v4_9.GetAllDiskOfferingsResponse;
import org.apache.http.client.HttpResponseException;
import org.apache.log4j.Level;
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

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

@RunWith(PowerMockRunner.class)
@PrepareForTest({CloudStackQueryJobResult.class, CloudStackQueryAsyncJobResponse.class,
        CloudStackCloudUtils.class, CloudStackQueryAsyncJobResponse.class, CloudStackUrlUtil.class,
        CloudStackQueryJobResult.class, Thread.class, GetAllDiskOfferingsResponse.class})
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

    // test case: When calling the generateInstanceName method, it must verify if
    // It returns a new instance name.
    @Test
    public void testGenerateInstanceNameSuccessfully() {
        // set up
        String regexPrefix = String.format("(%s)", SystemConstants.FOGBOW_INSTANCE_NAME_PREFIX);
        String regexUUID = "[0-9a-fA-F]{8}-" +
                "[0-9a-fA-F]{4}-" +
                "[1-5][0-9a-fA-F]{3}-" +
                "[89abAB][0-9a-fA-F]{3}-[0-9a-fA-F]{12}";
        String pattern = regexPrefix + regexUUID;

        // exercise
        String instanceName = CloudStackCloudUtils.generateInstanceName();

        // verify
        Assert.assertTrue(instanceName.matches(pattern));
    }

    // test case: When calling the convertToGigabyte method, it must verify if
    // It returns the right amount in gigabytes.
    @Test
    public void testConvertToGigabyteSuccessfully() {
        // set up
        int gbExpected = 2;
        long gbInBytes = (long) (gbExpected * CloudStackCloudUtils.ONE_GB_IN_BYTES);

        // exercise
        int gb = CloudStackCloudUtils.convertToGigabyte(gbInBytes);

        // verify
        Assert.assertEquals(gbExpected, gb);
    }

    // test case: When calling the convertToGigabyte method and the number is small, it must verify if
    // It returns the right amount in gigabytes.
    @Test
    public void testConvertToGigabyteWhenTheNumberIsSmall() {
        // set up
        long tinyAmountInBytes = 1;

        // exercise
        int gb = CloudStackCloudUtils.convertToGigabyte(tinyAmountInBytes);

        // verify
        Assert.assertEquals(0, gb);
    }

    // test case: When calling the getDiskOfferings method and a HttpResponseException occurs,
    // it must verify if a FogbowException has been thrown.
    @Test
    public void testGetDiskOfferingsFail() throws FogbowException, HttpResponseException {
        // set up
        CloudstackTestUtils.ignoringCloudStackUrl();

        String cloudStackUrl = "";
        GetAllDiskOfferingsRequest request = new GetAllDiskOfferingsRequest.Builder()
                .build(cloudStackUrl);
        CloudStackUser cloudStackUser = CloudstackTestUtils.CLOUD_STACK_USER;
        CloudStackHttpClient client = Mockito.mock(CloudStackHttpClient.class);

        Mockito.when(client.doGetRequest(Mockito.eq(request.getUriBuilder().toString()),
                Mockito.eq(cloudStackUser))).
                thenThrow(CloudstackTestUtils.createBadRequestHttpResponse());

        this.expectedException.expect(FogbowException.class);
        this.expectedException.expectMessage(CloudstackTestUtils.BAD_REQUEST_MSG);

        // exercise
        CloudStackCloudUtils.getDisksOffering(client, cloudStackUser, cloudStackUrl);
    }

    // test case: When calling the getDiskOfferings method and a HttpResponseException occurs,
    // it must verify if it returns the right DiskOffering list.
    @Test
    public void testGetDisksOfferingSuccessfully() throws FogbowException, IOException {
        // set up
        CloudstackTestUtils.ignoringCloudStackUrl();

        String cloudStackUrl = "";
        GetAllDiskOfferingsRequest request = new GetAllDiskOfferingsRequest.Builder()
                .build(cloudStackUrl);
        CloudStackUser cloudStackUser = CloudstackTestUtils.CLOUD_STACK_USER;
        CloudStackHttpClient client = Mockito.mock(CloudStackHttpClient.class);

        String responseJson = "anySthing";
        Mockito.when(client.doGetRequest(Mockito.eq(request.getUriBuilder().toString()),
                Mockito.eq(cloudStackUser))).thenReturn(responseJson);

        PowerMockito.mockStatic(GetAllDiskOfferingsResponse.class);
        List<GetAllDiskOfferingsResponse.DiskOffering> disksOfferingExpected = new ArrayList<>();

        GetAllDiskOfferingsResponse responseExpected = Mockito.mock(GetAllDiskOfferingsResponse.class);
        Mockito.when(responseExpected.getDiskOfferings()).thenReturn(disksOfferingExpected);
        PowerMockito.when(GetAllDiskOfferingsResponse.fromJson(Mockito.eq(responseJson)))
                .thenReturn(responseExpected);

        // exercise
        List<GetAllDiskOfferingsResponse.DiskOffering> disksOffering =
                CloudStackCloudUtils.getDisksOffering(client, cloudStackUser, cloudStackUrl);

        // verify
        Assert.assertEquals(disksOfferingExpected, disksOffering);
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

        mockSleepThread();

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

        mockSleepThread();

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

        mockSleepThread();

        // verify
        this.expectedException.expect(UnavailableProviderException.class);

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
        CloudStackHttpClient client =  Mockito.mock(CloudStackHttpClient.class);
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
        this.loggerTestChecking.assertEqualsInOrder(Level.WARN, Messages.Warn.SLEEP_THREAD_INTERRUPTED);
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

    private void mockSleepThread() throws InterruptedException {
        PowerMockito.mockStatic(Thread.class);
        PowerMockito.doNothing().when(Thread.class);
        Thread.sleep(Mockito.anyLong());
    }

}
