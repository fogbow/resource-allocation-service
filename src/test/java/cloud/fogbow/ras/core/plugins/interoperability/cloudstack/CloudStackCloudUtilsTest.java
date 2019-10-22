package cloud.fogbow.ras.core.plugins.interoperability.cloudstack;

import cloud.fogbow.common.exceptions.FogbowException;
import cloud.fogbow.common.exceptions.InvalidParameterException;
import cloud.fogbow.common.models.CloudStackUser;
import cloud.fogbow.common.util.connectivity.cloud.cloudstack.CloudStackHttpClient;
import cloud.fogbow.common.util.connectivity.cloud.cloudstack.CloudStackUrlUtil;
import cloud.fogbow.ras.core.TestUtils;
import cloud.fogbow.ras.core.plugins.interoperability.cloudstack.compute.v4_9.GetVirtualMachineRequest;
import cloud.fogbow.ras.core.plugins.interoperability.cloudstack.compute.v4_9.GetVirtualMachineResponse;
import org.apache.http.client.HttpResponseException;
import org.apache.http.client.utils.URIBuilder;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.junit.runner.RunWith;
import org.mockito.Mockito;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;

@RunWith(PowerMockRunner.class)
@PrepareForTest({GetVirtualMachineResponse.class, CloudStackUrlUtil.class})
public class CloudStackCloudUtilsTest {

    @Rule
    public ExpectedException expectedException = ExpectedException.none();
    private CloudStackHttpClient client;

    @Before
    public void setUp() throws InvalidParameterException {
        this.client = Mockito.mock(CloudStackHttpClient.class);
        CloudstackTestUtils.ignoringCloudStackUrl();
    }
    
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

    // test case: When calling the requestGetVirtualMachine method with secondary methods mocked,
    // it must verify if it returns the GetVirtualMachineResponse correct.
    @Test
    public void testRequestGetVirtualMachineSuccessfully() throws FogbowException, HttpResponseException {
        // set up
        CloudStackUser cloudStackUser = CloudstackTestUtils.CLOUD_STACK_USER;
        GetVirtualMachineRequest request = new GetVirtualMachineRequest.Builder()
                .build("anything");
        URIBuilder uriRequest = request.getUriBuilder();

        String responseStr = "anyResponseStr";
        Mockito.when(this.client.doGetRequest(Mockito.eq(uriRequest.toString()),
                Mockito.eq(cloudStackUser))).thenReturn(responseStr);

        PowerMockito.mockStatic(GetVirtualMachineResponse.class);
        GetVirtualMachineResponse responseExpected =
                Mockito.mock(GetVirtualMachineResponse.class);
        PowerMockito.when(GetVirtualMachineResponse.fromJson(Mockito.eq(responseStr)))
                .thenReturn(responseExpected);

        // exercise
        GetVirtualMachineResponse response = CloudStackCloudUtils.requestGetVirtualMachine(
                this.client, request, cloudStackUser);

        // verify
        Assert.assertEquals(responseExpected, response);
    }

    // test case: calling the requestGetVirtualMachineFail method and it occurs a Exception,
    // it must verify if it was threw a FogbowException
    @Test
    public void testRequestGetVirtualMachineFail() throws FogbowException, HttpResponseException {

        // set up
        CloudStackUser cloudStackUser = CloudstackTestUtils.CLOUD_STACK_USER;
        GetVirtualMachineRequest getVirtualMachineRequest = new GetVirtualMachineRequest.Builder()
                .build("anything");
        URIBuilder uriRequest = getVirtualMachineRequest.getUriBuilder();

        Mockito.when(this.client.doGetRequest(Mockito.eq(uriRequest.toString()),
                Mockito.eq(cloudStackUser))).thenThrow(CloudstackTestUtils.createBadRequestHttpResponse());

        // verify
        this.expectedException.expect(FogbowException.class);
        this.expectedException.expectMessage(CloudstackTestUtils.BAD_REQUEST_MSG);

        // exercise
        CloudStackCloudUtils.requestGetVirtualMachine(this.client, getVirtualMachineRequest, cloudStackUser);
    }

}
