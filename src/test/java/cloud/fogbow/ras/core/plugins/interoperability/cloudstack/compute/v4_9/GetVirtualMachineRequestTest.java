package cloud.fogbow.ras.core.plugins.interoperability.cloudstack.compute.v4_9;

import cloud.fogbow.common.constants.CloudStackConstants;
import cloud.fogbow.common.exceptions.InvalidParameterException;
import cloud.fogbow.common.util.connectivity.cloud.cloudstack.CloudStackUrlUtil;
import cloud.fogbow.ras.core.plugins.interoperability.cloudstack.CloudstackTestUtils;
import org.apache.http.client.utils.URIBuilder;
import org.junit.Assert;
import org.junit.Test;

public class GetVirtualMachineRequestTest {

    // test case: create GetVirtualMachineRequestUrl successfully
    @Test
    public void testCreateGetVirtualMachineRequestUrl() throws InvalidParameterException {
        // set up
        URIBuilder uriBuilder = CloudStackUrlUtil.createURIBuilder(
                CloudstackTestUtils.CLOUDSTACK_URL_DEFAULT,
                CloudStackConstants.Compute.LIST_VIRTUAL_MACHINES_COMMAND);
        String urlBaseExpected = uriBuilder.toString();
        String idExpected = "idExpexted";
        String idStructureUrl = String.format("%s=%s", CloudStackConstants.Compute.ID_KEY_JSON, idExpected);
        String[] urlStructure = new String[] {
                urlBaseExpected,
                idStructureUrl
        };
        String urlExpectedStr = String.join(
                CloudstackTestUtils.AND_OPERATION_URL_PARAMETER, urlStructure);

        // exercise
        GetVirtualMachineRequest getVirtualMachineRequest = new GetVirtualMachineRequest.Builder()
                .id(idExpected)
                .build(CloudstackTestUtils.CLOUDSTACK_URL_DEFAULT);
        String getVirtualMachineRequesttUrl = getVirtualMachineRequest.getUriBuilder().toString();

        // verify
        Assert.assertEquals(urlExpectedStr, getVirtualMachineRequesttUrl);
    }

    // test case: trying create GetVirtualMachineRequestUrl but it occur an error
    @Test(expected = InvalidParameterException.class)
    public void testCreateGetVirtualMachineRequestWithError() throws InvalidParameterException {
        // exercise and verify
        new GetVirtualMachineRequest.Builder().build(null);
    }

}
