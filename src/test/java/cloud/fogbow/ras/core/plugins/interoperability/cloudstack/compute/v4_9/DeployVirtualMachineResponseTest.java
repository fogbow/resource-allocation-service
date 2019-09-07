package cloud.fogbow.ras.core.plugins.interoperability.cloudstack.compute.v4_9;

import cloud.fogbow.common.exceptions.FogbowException;
import org.junit.Assert;
import org.junit.Test;

import java.io.IOException;

public class DeployVirtualMachineResponseTest {

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

}
