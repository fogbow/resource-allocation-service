package cloud.fogbow.ras.core.plugins.interoperability.cloudstack.network.v4_9;

import cloud.fogbow.common.exceptions.InvalidParameterException;
import cloud.fogbow.common.util.connectivity.cloud.cloudstack.CloudStackUrlUtil;
import cloud.fogbow.ras.core.plugins.interoperability.cloudstack.CloudstackTestUtils;
import org.apache.http.client.utils.URIBuilder;
import org.junit.Assert;
import org.junit.Test;

public class GetNetworkRequestTest {

    // test case: When calling the build method, it must verify if It generates the right URL.
    @Test
    public void testBuildSuccessfully() throws InvalidParameterException {
        // set up
        URIBuilder uriBuilder = CloudStackUrlUtil.createURIBuilder(
                CloudstackTestUtils.CLOUDSTACK_URL_DEFAULT,
                GetNetworkRequest.LIST_NETWORKS_COMMAND);
        String urlBaseExpected = uriBuilder.toString();
        String networkId = "networkId";

        String networkIdStructureUrl =
                String.format("%s=%s", GetNetworkRequest.NETWORK_ID_KEY, networkId);

        String[] urlStructure = new String[] {
                urlBaseExpected,
                networkIdStructureUrl,
        };
        String urlExpectedStr = String.join(
                CloudstackTestUtils.AND_OPERATION_URL_PARAMETER, urlStructure);

        // exercise
        GetNetworkRequest getNetworkRequest = new GetNetworkRequest.Builder()
                .id(networkId)
                .build(CloudstackTestUtils.CLOUDSTACK_URL_DEFAULT);
        String getNetworkRequestUrl = getNetworkRequest.getUriBuilder().toString();

        // verify
        Assert.assertEquals(urlExpectedStr, getNetworkRequestUrl);
    }

    // test case: When calling the build method with a null parameter,
    // it must verify if It throws an InvalidParameterException.
    @Test(expected = InvalidParameterException.class)
    public void testBuildFail() throws InvalidParameterException {
        // exercise and verify
        new GetNetworkRequest.Builder().build(null);
    }

}
