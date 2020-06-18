package cloud.fogbow.ras.core.plugins.interoperability.cloudstack.publicip.v4_9;

import cloud.fogbow.common.exceptions.FogbowException;
import cloud.fogbow.ras.core.plugins.interoperability.cloudstack.CloudstackTestUtils;
import org.apache.http.HttpStatus;
import org.apache.http.client.HttpResponseException;
import org.junit.Assert;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

import java.io.IOException;

public class SuccessfulAssociateIpAddressResponseTest {

    @Rule
    public ExpectedException expectedException = ExpectedException.none();

    // test case: When calling the fromJson method, it must verify
    // if It returns the right SuccessfulAssociateIpAddressResponse.
    @Test
    public void testFromJsonSuccessfully() throws IOException, FogbowException {
        // set up
        String id = "id";
        String ipAddress = "ipAddress";
        String asyncAssociateIpAddressResponseJson =
                CloudstackTestUtils.createAsyncAssociateIpAddressResponseJson(id, ipAddress);

        // execute
        SuccessfulAssociateIpAddressResponse successfulAssociateIpAddressResponse =
                SuccessfulAssociateIpAddressResponse.fromJson(asyncAssociateIpAddressResponseJson);

        // verify
        Assert.assertEquals(id, successfulAssociateIpAddressResponse.getIpAddress().getId());
        Assert.assertEquals(ipAddress, successfulAssociateIpAddressResponse.getIpAddress().getIpAddress());
    }

    // test case: When calling the fromJson method with error json response,
    // it must verify if It returns the rigth FogbowException.
    @Test
    public void testFromJsonFail() throws IOException, FogbowException {
        // set up
        String errorText = "anyString";
        Integer errorCode = HttpStatus.SC_BAD_REQUEST;
        String asyncAssociateIpAddressErrorResponseJson = CloudstackTestUtils
                .createAsyncAssociateIpAddressErrorResponseJson(errorCode, errorText);

        this.expectedException.expect(FogbowException.class);
        this.expectedException.expectMessage(errorText);

        // execute
        SuccessfulAssociateIpAddressResponse.fromJson(asyncAssociateIpAddressErrorResponseJson);
    }

}
