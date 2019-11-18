package cloud.fogbow.ras.core.plugins.interoperability.cloudstack.publicip.v4_9;

import cloud.fogbow.common.constants.CloudStackConstants;
import cloud.fogbow.common.exceptions.InvalidParameterException;
import cloud.fogbow.common.util.connectivity.cloud.cloudstack.CloudStackUrlUtil;
import cloud.fogbow.ras.core.plugins.interoperability.cloudstack.CloudstackTestUtils;
import org.apache.http.client.utils.URIBuilder;
import org.junit.Assert;
import org.junit.Test;

public class DisassociateIpAddressRequestTest {

    // test case: When calling the build method, it must verify if It generates the right URL.
    @Test
    public void testBuildSuccessfully() throws InvalidParameterException {
        // set up
        URIBuilder uriBuilder = CloudStackUrlUtil.createURIBuilder(
                CloudstackTestUtils.CLOUDSTACK_URL_DEFAULT,
                CloudStackConstants.PublicIp.DISASSOCIATE_IP_ADDRESS_COMMAND);
        String urlBaseExpected = uriBuilder.toString();
        String id = "id";

        String idStructureUrl = String.format(
                "%s=%s", CloudStackConstants.PublicIp.ID_KEY_JSON, id);

        String[] urlStructure = new String[] {
                urlBaseExpected,
                idStructureUrl
        };
        String urlExpectedStr = String.join(
                CloudstackTestUtils.AND_OPERATION_URL_PARAMETER, urlStructure);

        // exercise
        DisassociateIpAddressRequest disassociateIpAddressRequest = new DisassociateIpAddressRequest.Builder()
                .id(id)
                .build(CloudstackTestUtils.CLOUDSTACK_URL_DEFAULT);
        String disassociateIpAddressRequestUrl = disassociateIpAddressRequest.getUriBuilder().toString();

        // verify
        Assert.assertEquals(urlExpectedStr, disassociateIpAddressRequestUrl);
    }

    // test case: When calling the build method with a null parameter,
    // it must verify if It throws an InvalidParameterException.
    @Test(expected = InvalidParameterException.class)
    public void testBuildFail() throws InvalidParameterException {
        // exercise and verify
        new DisassociateIpAddressRequest.Builder().build(null);
    }

}
