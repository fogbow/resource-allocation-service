package cloud.fogbow.ras.core.plugins.interoperability.cloudstack.compute.v4_9;

import cloud.fogbow.common.exceptions.InvalidParameterException;
import cloud.fogbow.common.util.connectivity.cloud.cloudstack.CloudStackUrlUtil;
import org.apache.http.client.utils.URIBuilder;
import org.junit.Assert;
import org.junit.Ignore;
import org.junit.Test;

import static cloud.fogbow.ras.core.plugins.interoperability.cloudstack.compute.v4_9.GetVirtualMachineRequest.VIRTUAL_MACHINE_ID_KEY;

public class GetVirtualMachineRequestTest {

    // test case: create GetVirtualMachineRequestUrl successfully
    @Test
    public void testCreateGetVirtualMachineRequestUrl() throws InvalidParameterException {
        // set up
        URIBuilder uriBuilder = CloudStackUrlUtil.createURIBuilder(
                CloudstackTestUtils.CLOUDSTACK_URL_DEFAULT,
                GetVirtualMachineRequest.LIST_VMS_COMMAND);
        String urlExpected = uriBuilder.toString();
        String idExpected = "idExpexted";
        String idStructureUrl = String.format("%s=%s", VIRTUAL_MACHINE_ID_KEY, idExpected);

        // exercise
        GetVirtualMachineRequest getVirtualMachineRequest = new GetVirtualMachineRequest.Builder()
                .id(idExpected)
                .build(CloudstackTestUtils.CLOUDSTACK_URL_DEFAULT);
        String getVirtualMachineRequesttUrl = getVirtualMachineRequest.getUriBuilder().toString();

        // verify
        Assert.assertTrue(getVirtualMachineRequesttUrl.contains(urlExpected));
        Assert.assertTrue(getVirtualMachineRequesttUrl.contains(idStructureUrl));
    }

    /**
     TODO(Chico) fix the Fogbow Commom code. The CloudStackRequest throws a
     InvalidParameterException from the package java.security instead the FogbowException
     **/
    @Ignore
    // test case: trying create GetVirtualMachineRequestUrl but it occur an error
    @Test(expected = InvalidParameterException.class)
    public void testCreateGetVirtualMachineRequestWithError() throws InvalidParameterException {
        // exercise and verify
        new GetVirtualMachineRequest.Builder().build(null);
    }

}
