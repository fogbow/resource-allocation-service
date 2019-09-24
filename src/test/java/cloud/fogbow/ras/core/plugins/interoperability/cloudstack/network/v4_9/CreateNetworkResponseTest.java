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

    // test case: create CreateNetworkResponse from Cloudstack Json Response
    @Test
    public void testCreateNetworkResponseFromJson() throws Exception {
        // set up
        String id = "1";
        String getCreateNetworkResponseJson = CloudstackTestUtils.createNetworkResponseJson(id);

        // execute
        CreateNetworkResponse createNetworkResponse =
                CreateNetworkResponse.fromJson(getCreateNetworkResponseJson);

        // verify
        Assert.assertEquals(id, createNetworkResponse.getId());
    }

    // test case: create CreateNetworkResponse from Cloudstack Json Response with empty values
    @Test
    public void testCreateNetworkResponseFromJsonEmptyValue() throws Exception {
        // set up
        String getVirtualMachineResponseJson = CloudstackTestUtils.createEmptyCreateNetworkResponseJson();

        // execute
        CreateNetworkResponse createNetworkResponse =
                CreateNetworkResponse.fromJson(getVirtualMachineResponseJson);

        // verify
        Assert.assertNull(createNetworkResponse.getId());
    }

    // test case: create CreateNetworkResponse from error Cloudstack Json Response
    @Test
    public void testCreateNetworkResponseErrorJson() throws IOException {
        // set up
        String errorText = "anyString";
        int errorCode = HttpStatus.SC_BAD_REQUEST;
        String volumesErrorResponseJson = CloudstackTestUtils
                .createCreateNetworkResponseJson(errorCode, errorText);

        // verify
        this.expectedException.expect(HttpResponseException.class);
        this.expectedException.expectMessage(errorText);

        // execute
        CreateNetworkResponse.fromJson(volumesErrorResponseJson);
    }

}
