package cloud.fogbow.ras.core.plugins.interoperability.cloudstack.network.v4_9;

import cloud.fogbow.ras.core.plugins.interoperability.cloudstack.CloudstackTestUtils;
import org.apache.http.HttpStatus;
import org.apache.http.client.HttpResponseException;
import org.junit.Assert;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

import java.io.IOException;

public class GetNetworkResponseTest {

    @Rule
    public ExpectedException expectedException = ExpectedException.none();

    // test case: When calling the fromJson method, it must verify if It returns the right GetNetworkResponse.
    @Test
    public void testFromJsonSuccessfully() throws Exception {
        // set up
        String id = "1";
        String name = "name";
        String gateway = "gateway";
        String cird = "cird";
        String state = "state";
        String getGetNetworkResponseJson = CloudstackTestUtils.createGetNetworkResponseJson(
                id, name, gateway, cird, state);

        // execute
        GetNetworkResponse getNetworkResponse =
                GetNetworkResponse.fromJson(getGetNetworkResponseJson);

        // verify
        Assert.assertEquals(id, getNetworkResponse.getNetworks().get(0).getId());
        Assert.assertEquals(name, getNetworkResponse.getNetworks().get(0).getName());
        Assert.assertEquals(gateway, getNetworkResponse.getNetworks().get(0).getGateway());
        Assert.assertEquals(cird, getNetworkResponse.getNetworks().get(0).getCidr());
        Assert.assertEquals(state, getNetworkResponse.getNetworks().get(0).getState());

    }

    // test case: When calling the fromJson method with empty json response,
    // it must verify if It returns the GetNetworkResponse with empty values.
    @Test
    public void testFromJsonWithEmptyValue() throws Exception {
        // set up
        String getVirtualMachineResponseJson = CloudstackTestUtils.createGetNetworkEmptyResponseJson();

        // execute
        GetNetworkResponse getNetworkResponse =
                GetNetworkResponse.fromJson(getVirtualMachineResponseJson);

        // verify
        Assert.assertTrue(getNetworkResponse.getNetworks().isEmpty());
    }

    // test case: When calling the fromJson method with error json response,
    // it must verify if It throws a HttpResponseException.
    @Test
    public void testFromJsonFail() throws IOException {
        // set up
        String errorText = "anyString";
        int errorCode = HttpStatus.SC_BAD_REQUEST;
        String volumesErrorResponseJson = CloudstackTestUtils
                .createGetNetworkErrorResponseJson(errorCode, errorText);

        // verify
        this.expectedException.expect(HttpResponseException.class);
        this.expectedException.expectMessage(errorText);

        // execute
        GetNetworkResponse.fromJson(volumesErrorResponseJson);
    }

}
