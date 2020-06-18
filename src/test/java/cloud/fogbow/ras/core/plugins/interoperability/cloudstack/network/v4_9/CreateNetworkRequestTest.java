package cloud.fogbow.ras.core.plugins.interoperability.cloudstack.network.v4_9;

import cloud.fogbow.common.constants.CloudStackConstants;
import cloud.fogbow.common.exceptions.InternalServerErrorException;
import cloud.fogbow.common.util.connectivity.cloud.cloudstack.CloudStackUrlUtil;
import cloud.fogbow.ras.core.plugins.interoperability.cloudstack.CloudstackTestUtils;
import org.apache.http.client.utils.URIBuilder;
import org.junit.Assert;
import org.junit.Test;

public class CreateNetworkRequestTest {

    // test case: When calling the build method, it must verify if It generates the right URL.
    @Test
    public void testBuildSuccessfully() throws InternalServerErrorException {
        // set up
        URIBuilder uriBuilder = CloudStackUrlUtil.createURIBuilder(
                CloudstackTestUtils.CLOUDSTACK_URL_DEFAULT,
                CloudStackConstants.Network.CREATE_NETWORK_COMMAND);
        String urlBaseExpected = uriBuilder.toString();

        String nameExpexted = "nameExpexted";
        String displayTextExpected = "displayTextExpected";
        String networkOfferingIdExpected = "1";
        String zoneIdExpected = "2";
        String startingIpExpected = "...10";
        String endingIpExpected = "...11";
        String gatewayExpected = "...1";
        String netmaskException = "...255";

        String virtualMachineIdStructureUrl = String.format(
                "%s=%s", CloudStackConstants.Network.NAME_KEY_JSON, nameExpexted);
        String displayNameStructureUrl = String.format(
                "%s=%s", CloudStackConstants.Network.DISPLAY_TEXT_KEY_JSON, displayTextExpected);
        String networkOfferingStructureUrl = String.format("%s=%s",
                CloudStackConstants.Network.NETWORK_OFFERING_ID_KEY_JSON, networkOfferingIdExpected);
        String zoneIdStructureUrl = String.format(
                "%s=%s", CloudStackConstants.Network.ZONE_ID_KEY_JSON, zoneIdExpected);
        String startingIpStructureUrl = String.format(
                "%s=%s", CloudStackConstants.Network.STARTING_IP_KEY_JSON, startingIpExpected);
        String endingIpStructureUrl = String.format(
                "%s=%s", CloudStackConstants.Network.ENDING_IP_KEY_JSON, endingIpExpected);
        String gatewayStructureUrl = String.format(
                "%s=%s", CloudStackConstants.Network.GATEWAY_KEY_JSON, gatewayExpected);
        String netmaskStructureUrl = String.format(
                "%s=%s", CloudStackConstants.Network.NETMASK_KEY_JSON, netmaskException);

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
    // it must verify if It throws an InternalServerErrorException.
    @Test(expected = InternalServerErrorException.class)
    public void testCreateNetworkRequestWithError() throws InternalServerErrorException {
        // exercise and verify
        new CreateNetworkRequest.Builder().build(null);
    }

}
