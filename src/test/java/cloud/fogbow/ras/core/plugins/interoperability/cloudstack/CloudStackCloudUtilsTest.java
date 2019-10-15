package cloud.fogbow.ras.core.plugins.interoperability.cloudstack;

import cloud.fogbow.common.exceptions.FogbowException;
import cloud.fogbow.common.models.CloudStackUser;
import cloud.fogbow.common.util.connectivity.cloud.cloudstack.CloudStackHttpClient;
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

}
