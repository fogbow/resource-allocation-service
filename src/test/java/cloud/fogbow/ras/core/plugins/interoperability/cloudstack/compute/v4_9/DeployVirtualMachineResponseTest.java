package cloud.fogbow.ras.core.plugins.interoperability.cloudstack.compute.v4_9;

import cloud.fogbow.common.exceptions.FogbowException;
import cloud.fogbow.ras.core.plugins.interoperability.cloudstack.CloudstackTestUtils;
import org.apache.http.HttpStatus;
import org.apache.http.client.HttpResponseException;
import org.junit.Assert;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

import java.io.IOException;

public class DeployVirtualMachineResponseTest {

    @Rule
    public ExpectedException expectedException = ExpectedException.none();

    // test case: create DeployVirtualMachineResponse from Cloudstack Json Response
    @Test
    public void testDeployVirtualMachineResponse() throws IOException, FogbowException {
        // set up
        String id = "id";
        String json = CloudstackTestUtils.createDeployVirtualMachineResponseJson(id);

        // exercise
        DeployVirtualMachineResponse deployVirtualMachineResponse =
                DeployVirtualMachineResponse.fromJson(json);

        //verify
        Assert.assertEquals(id, deployVirtualMachineResponse.getId());
    }

    // test case: create DeployVirtualMachineResponse from Cloudstack Json Response, but it happen an error
    @Test
    public void testDeployVirtualMachineErrorResponse() throws IOException, FogbowException {
        // set up
        int errorCode = HttpStatus.SC_BAD_REQUEST;
        String errorText = "anyMessage";
        String json = CloudstackTestUtils.createDeployVirtualMachineErrorResponseJson(
                errorCode, errorText);

        // verify
        this.expectedException.expect(FogbowException.class);
        this.expectedException.expectMessage(errorText);

        // exercise
        DeployVirtualMachineResponse.fromJson(json);
    }

}
