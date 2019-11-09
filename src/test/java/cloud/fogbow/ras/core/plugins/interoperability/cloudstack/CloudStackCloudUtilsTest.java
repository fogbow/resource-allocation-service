package cloud.fogbow.ras.core.plugins.interoperability.cloudstack;

import cloud.fogbow.common.exceptions.FogbowException;
import cloud.fogbow.common.models.CloudStackUser;
import cloud.fogbow.common.util.connectivity.cloud.cloudstack.CloudStackHttpClient;
import cloud.fogbow.ras.constants.SystemConstants;
import cloud.fogbow.ras.core.TestUtils;
import org.apache.http.client.HttpResponseException;
import org.junit.Assert;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.mockito.Mockito;

public class CloudStackCloudUtilsTest {

    @Rule
    public ExpectedException expectedException = ExpectedException.none();

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
        String regexUUID =  "[0-9a-fA-F]{8}-" +
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
        long amountInBytes = 1;

        // exercise
        int gb = CloudStackCloudUtils.convertToGigabyte(amountInBytes);

        // verify
        Assert.assertEquals(0, gb);
    }

}
