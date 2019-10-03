package cloud.fogbow.ras.core.plugins.interoperability.cloudstack.network.v4_9;

import cloud.fogbow.ras.core.plugins.interoperability.cloudstack.CloudstackTestUtils;
import org.apache.http.HttpStatus;
import org.apache.http.client.HttpResponseException;
import org.junit.Assert;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

import java.io.IOException;

public class CreateNetworkResponseTest {

    @Rule
    public ExpectedException expectedException = ExpectedException.none();

    // test case: When calling the fromJson method, it must verify if It returns the right CreateNetworkResponse.
    @Test
    public void testFromJsonSuccessfully() throws Exception {
        // set up
        String id = "1";
        String getCreateNetworkResponseJson = CloudstackTestUtils.createNetworkResponseJson(id);

        // execute
        CreateNetworkResponse createNetworkResponse =
                CreateNetworkResponse.fromJson(getCreateNetworkResponseJson);

        // verify
        Assert.assertEquals(id, createNetworkResponse.getId());
    }

    // test case: When calling the fromJson method with empty json response,
    // it must verify if It returns the CreateNetworkResponse with null values.
    @Test
    public void testFromJsonWithEmptyValue() throws Exception {
        // set up
        String getVirtualMachineResponseJson = CloudstackTestUtils.createCreateNetworkEmptyResponseJson();

        // execute
        CreateNetworkResponse createNetworkResponse =
                CreateNetworkResponse.fromJson(getVirtualMachineResponseJson);

        // verify
        Assert.assertNull(createNetworkResponse.getId());
    }

    // test case: When calling the fromJson method with error json response,
    // it must verify if It throws a HttpResponseException.
    @Test
    public void testFromJsonFail() throws IOException {
        // set up
        String errorText = "anyString";
        int errorCode = HttpStatus.SC_BAD_REQUEST;
        String volumesErrorResponseJson = CloudstackTestUtils
                .createCreateNetworkErrorResponseJson(errorCode, errorText);

        // verify
        this.expectedException.expect(HttpResponseException.class);
        this.expectedException.expectMessage(errorText);

        // execute
        CreateNetworkResponse.fromJson(volumesErrorResponseJson);
    }

}
