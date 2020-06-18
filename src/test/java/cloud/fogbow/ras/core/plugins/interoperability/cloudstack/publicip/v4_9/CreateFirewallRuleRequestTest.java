package cloud.fogbow.ras.core.plugins.interoperability.cloudstack.publicip.v4_9;

import cloud.fogbow.common.constants.CloudStackConstants;
import cloud.fogbow.common.exceptions.InternalServerErrorException;
import cloud.fogbow.common.util.connectivity.cloud.cloudstack.CloudStackUrlUtil;
import cloud.fogbow.ras.core.plugins.interoperability.cloudstack.CloudstackTestUtils;
import org.apache.http.client.utils.URIBuilder;
import org.junit.Assert;
import org.junit.Test;

public class CreateFirewallRuleRequestTest {

    // test case: When calling the build method, it must verify if It generates the right URL.
    @Test
    public void testBuildSuccessfully() throws InternalServerErrorException {
        // set up
        URIBuilder uriBuilder = CloudStackUrlUtil.createURIBuilder(
                CloudstackTestUtils.CLOUDSTACK_URL_DEFAULT,
                CloudStackConstants.PublicIp.CREATE_FIREWALL_RULE_COMMAND);
        String urlBaseExpected = uriBuilder.toString();
        String cirdList = "cirdList";
        String protocol = "protocol";
        String startPort = "startPort";
        String endPort = "endPort";
        String ipAddressId = "ipAdressId";

        String protocolStructureUrl = String.format(
                "%s=%s", CloudStackConstants.PublicIp.PROTOCOL_KEY_JSON, protocol);
        String cirdListStructureUrl = String.format(
                "%s=%s", CloudStackConstants.PublicIp.CIDR_LIST_KEY_JSON, cirdList);
        String startPortStructureUrl = String.format(
                "%s=%s", CloudStackConstants.PublicIp.STARTPORT_KEY_JSON, startPort);
        String endPortStructureUrl = String.format(
                "%s=%s", CloudStackConstants.PublicIp.ENDPORT_KEY_JSON, endPort);
        String ipAddressIdStructureUrl = String.format(
                "%s=%s", CloudStackConstants.PublicIp.IP_ADDRESS_ID_KEY_JSON, ipAddressId);

        String[] urlStructure = new String[] {
                urlBaseExpected,
                protocolStructureUrl,
                startPortStructureUrl,
                endPortStructureUrl,
                ipAddressIdStructureUrl,
                cirdListStructureUrl,
        };
        String urlExpectedStr = String.join(
                CloudstackTestUtils.AND_OPERATION_URL_PARAMETER, urlStructure);

        // exercise
        CreateFirewallRuleRequest createFirewallRuleRequest = new CreateFirewallRuleRequest.Builder()
                .cidrList(cirdList)
                .protocol(protocol)
                .startPort(startPort)
                .endPort(endPort)
                .ipAddressId(ipAddressId)
                .build(CloudstackTestUtils.CLOUDSTACK_URL_DEFAULT);
        String associateIpAddressRequestUrl = createFirewallRuleRequest.getUriBuilder().toString();

        // verify
        Assert.assertEquals(urlExpectedStr, associateIpAddressRequestUrl);
    }

    // test case: When calling the build method with a null parameter,
    // it must verify if It throws an InternalServerErrorException.
    @Test(expected = InternalServerErrorException.class)
    public void testBuildFail() throws InternalServerErrorException {
        // exercise and verify
        new CreateFirewallRuleRequest.Builder().build(null);
    }

}
