package cloud.fogbow.ras.core.plugins.interoperability.cloudstack.publicip.v4_9;

import cloud.fogbow.common.constants.CloudStackConstants;
import cloud.fogbow.common.exceptions.InternalServerErrorException;
import cloud.fogbow.common.util.connectivity.cloud.cloudstack.CloudStackUrlUtil;
import cloud.fogbow.ras.core.plugins.interoperability.cloudstack.CloudstackTestUtils;
import org.apache.http.client.utils.URIBuilder;
import org.junit.Assert;
import org.junit.Test;

public class AssociateIpAddressRequestTest {

    // test case: When calling the build method, it must verify if It generates the right URL.
    @Test
    public void testBuildSuccessfully() throws InternalServerErrorException {
        // set up
        URIBuilder uriBuilder = CloudStackUrlUtil.createURIBuilder(
                CloudstackTestUtils.CLOUDSTACK_URL_DEFAULT,
                CloudStackConstants.PublicIp.ASSOCIATE_IP_ADDRESS_COMMAND);
        String urlBaseExpected = uriBuilder.toString();
        String networkId = "networkId";

        String networkIdStructureUrl = String.format(
                "%s=%s", CloudStackConstants.PublicIp.NETWORK_ID_KEY_JSON, networkId);

        String[] urlStructure = new String[] {
                urlBaseExpected,
                networkIdStructureUrl
        };
        String urlExpectedStr = String.join(
                CloudstackTestUtils.AND_OPERATION_URL_PARAMETER, urlStructure);

        // exercise
        AssociateIpAddressRequest associateIpAddressRequest = new AssociateIpAddressRequest.Builder()
                .networkId(networkId)
                .build(CloudstackTestUtils.CLOUDSTACK_URL_DEFAULT);
        String associateIpAddressRequestUrl = associateIpAddressRequest.getUriBuilder().toString();

        // verify
        Assert.assertEquals(urlExpectedStr, associateIpAddressRequestUrl);
    }

    // test case: When calling the build method with a null parameter,
    // it must verify if It throws an InternalServerErrorException.
    @Test(expected = InternalServerErrorException.class)
    public void testBuildFail() throws InternalServerErrorException {
        // exercise and verify
        new AssociateIpAddressRequest.Builder().build(null);
    }

}
