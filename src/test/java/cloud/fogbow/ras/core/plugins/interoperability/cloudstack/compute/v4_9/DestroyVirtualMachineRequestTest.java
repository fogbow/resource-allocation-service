package cloud.fogbow.ras.core.plugins.interoperability.cloudstack.compute.v4_9;

import cloud.fogbow.common.exceptions.InvalidParameterException;
import cloud.fogbow.common.util.connectivity.cloud.cloudstack.CloudStackUrlUtil;
import org.apache.http.client.utils.URIBuilder;
import org.junit.Assert;
import org.junit.Test;

import static cloud.fogbow.ras.core.plugins.interoperability.cloudstack.compute.v4_9.DestroyVirtualMachineRequest.EXPUNGE_KEY;
import static cloud.fogbow.ras.core.plugins.interoperability.cloudstack.compute.v4_9.GetVirtualMachineRequest.VIRTUAL_MACHINE_ID_KEY;

public class DestroyVirtualMachineRequestTest {

    // test case: create DestroyVirtualMachineRequestUrl successfully
    @Test
    public void testDestroyVirtualMachineRequestUrl() throws InvalidParameterException {
        // set up
        URIBuilder uriBuilder = CloudStackUrlUtil.createURIBuilder(
                CloudstackTestUtils.CLOUDSTACK_URL_DEFAULT,
                DestroyVirtualMachineRequest.DESTROY_VIRTUAL_MACHINE_COMMAND);
        String urlBaseExpected = uriBuilder.toString();
        String virtualMachineIdExpected = "idExpexted";
        String virtualMachineIdStructureUrl = String.format("%s=%s", VIRTUAL_MACHINE_ID_KEY, virtualMachineIdExpected);
        String expungeExpected = "true";
        String expungeStructureUrl = String.format("%s=%s", EXPUNGE_KEY, expungeExpected);
        String[] urlStructure = new String[] {
                urlBaseExpected,
                virtualMachineIdStructureUrl,
                expungeStructureUrl
        };
        String urlExpectedStr = String.join(
                CloudstackTestUtils.AND_OPERATION_URL_PARAMETER, urlStructure);

        // exercise
        DestroyVirtualMachineRequest destroyVirtualMachineRequest = new DestroyVirtualMachineRequest.Builder()
                .id(virtualMachineIdExpected)
                .expunge(expungeExpected)
                .build(CloudstackTestUtils.CLOUDSTACK_URL_DEFAULT);
        String destroyVirtualMachineRequestUrl = destroyVirtualMachineRequest.getUriBuilder().toString();

        // verify
        Assert.assertEquals(urlExpectedStr, destroyVirtualMachineRequestUrl);
    }
}
