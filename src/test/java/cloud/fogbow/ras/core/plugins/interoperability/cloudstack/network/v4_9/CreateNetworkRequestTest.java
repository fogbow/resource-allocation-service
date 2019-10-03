package cloud.fogbow.ras.core.plugins.interoperability.cloudstack.network.v4_9;

import cloud.fogbow.common.exceptions.InvalidParameterException;
import cloud.fogbow.common.util.connectivity.cloud.cloudstack.CloudStackUrlUtil;
import cloud.fogbow.ras.core.plugins.interoperability.cloudstack.CloudstackTestUtils;
import org.apache.http.client.utils.URIBuilder;
import org.junit.Assert;
import org.junit.Test;

public class CreateNetworkRequestTest {

    // test case: When calling the build method, it must verify if It generates the right URL.
    @Test
    public void testBuildSuccessfully() throws InvalidParameterException {
        // set up
        URIBuilder uriBuilder = CloudStackUrlUtil.createURIBuilder(
                CloudstackTestUtils.CLOUDSTACK_URL_DEFAULT,
                CreateNetworkRequest.CREATE_NETWORK_COMMAND);
        String urlBaseExpected = uriBuilder.toString();

        String nameExpexted = "nameExpexted";
        String displayTextExpected = "displayTextExpected";
        String networkOfferingIdExpected = "1";
        String zoneIdExpected = "2";
        String startingIpExpected = "...10";
        String endingIpExpected = "...11";
        String gatewayExpected = "...1";
        String netmaskException = "...255";

        String virtualMachineIdStructureUrl =
                String.format("%s=%s", CreateNetworkRequest.NAME_KEY, nameExpexted);
        String displayNameStructureUrl =
                String.format("%s=%s", CreateNetworkRequest.DISPLAY_TEXT_KEY, displayTextExpected);
        String networkOfferingStructureUrl =
                String.format("%s=%s", CreateNetworkRequest.NETWORK_OFFERING_ID_KEY, networkOfferingIdExpected);
        String zoneIdStructureUrl =
                String.format("%s=%s", CreateNetworkRequest.ZONE_ID_KEY, zoneIdExpected);
        String startingIpStructureUrl =
                String.format("%s=%s", CreateNetworkRequest.STARTING_IP_KEY, startingIpExpected);
        String endingIpStructureUrl =
                String.format("%s=%s", CreateNetworkRequest.ENDING_IP_KEY, endingIpExpected);
        String gatewayStructureUrl =
                String.format("%s=%s", CreateNetworkRequest.GATEWAY_KEY, gatewayExpected);
        String netmaskStructureUrl =
                String.format("%s=%s", CreateNetworkRequest.NETMASK_KEY, netmaskException);

        String[] urlStructure = new String[] {
                urlBaseExpected,
                virtualMachineIdStructureUrl,
                displayNameStructureUrl,
                networkOfferingStructureUrl,
                zoneIdStructureUrl,
                startingIpStructureUrl,
                endingIpStructureUrl,
                gatewayStructureUrl,
                netmaskStructureUrl
        };
        String urlExpectedStr = String.join(
                CloudstackTestUtils.AND_OPERATION_URL_PARAMETER, urlStructure);

        // exercise
        CreateNetworkRequest createNetworkRequest = new CreateNetworkRequest.Builder()
                .name(nameExpexted)
                .displayText(displayTextExpected)
                .endingIp(endingIpExpected)
                .gateway(gatewayExpected)
                .netmask(netmaskException)
                .networkOfferingId(networkOfferingIdExpected)
                .startIp(startingIpExpected)
                .zoneId(zoneIdExpected)
                .build(CloudstackTestUtils.CLOUDSTACK_URL_DEFAULT);
        String createNetworkRequestUrl = createNetworkRequest.getUriBuilder().toString();

        // verify
        Assert.assertEquals(urlExpectedStr, createNetworkRequestUrl);
    }

    // test case: When calling the build method with a null parameter,
    // it must verify if It throws an InvalidParameterException.
    @Test(expected = InvalidParameterException.class)
    public void testCreateNetworkRequestWithError() throws InvalidParameterException {
        // exercise and verify
        new CreateNetworkRequest.Builder().build(null);
    }

}
