package cloud.fogbow.ras.core.plugins.interoperability.cloudstack.securityrule.v4_9;

import cloud.fogbow.common.constants.CloudStackConstants;
import cloud.fogbow.common.exceptions.InvalidParameterException;
import cloud.fogbow.common.util.connectivity.cloud.cloudstack.CloudStackUrlUtil;
import cloud.fogbow.ras.core.plugins.interoperability.cloudstack.CloudstackTestUtils;
import cloud.fogbow.ras.core.plugins.interoperability.cloudstack.publicip.v4_9.CreateFirewallRuleRequest;
import org.apache.http.client.utils.URIBuilder;
import org.junit.Assert;
import org.junit.Test;

import static cloud.fogbow.common.constants.CloudStackConstants.PublicIp.*;

public class CreateFirewallRuleRequestTest {

    // test case: When calling the build method, it must verify if It generates the right URL.
    @Test
    public void testBuildSuccessfully() throws InvalidParameterException {
        // set up
        URIBuilder uriBuilder = CloudStackUrlUtil.createURIBuilder(
                CloudstackTestUtils.CLOUDSTACK_URL_DEFAULT,
                CloudStackConstants.SecurityGroup.CREATE_FIREWALL_RULE_COMMAND);
        String urlBaseExpected = uriBuilder.toString();
        String protocol = "protocol";
        String startPort = "startPort";
        String endPort = "endPort";
        String ipAddressId = "ipAddressId";
        String cird = "cird";

        String protocolIdStructureUrl = String.format(
                "%s=%s", PROTOCOL_KEY_JSON, protocol);
        String startPortStructureUrl = String.format(
                "%s=%s", STARTPORT_KEY_JSON, startPort);
        String endPortStructureUrl = String.format(
                "%s=%s", ENDPORT_KEY_JSON, endPort);
        String ipAddressIdStructureUrl = String.format(
                "%s=%s", IP_ADDRESS_ID_KEY_JSON, ipAddressId);
        String cirdStructureUrl = String.format(
                "%s=%s", CIDR_LIST_KEY_JSON, cird);

        String[] urlStructure = new String[] {
                urlBaseExpected,
                protocolIdStructureUrl,
                startPortStructureUrl,
                endPortStructureUrl,
                ipAddressIdStructureUrl,
                cirdStructureUrl
        };
        String urlExpectedStr = String.join(
                CloudstackTestUtils.AND_OPERATION_URL_PARAMETER, urlStructure);

        // exercise
        CreateFirewallRuleRequest createFirewallRuleRequest = new CreateFirewallRuleRequest.Builder()
                .startPort(startPort)
                .endPort(endPort)
                .protocol(protocol)
                .ipAddressId(ipAddressId)
                .cidrList(cird)
                .build(CloudstackTestUtils.CLOUDSTACK_URL_DEFAULT);
        String createFireWallRequestUrl = createFirewallRuleRequest.getUriBuilder().toString();

        // verify
        Assert.assertEquals(urlExpectedStr, createFireWallRequestUrl);
    }

    // test case: When calling the build method with a null parameter,
    // it must verify if It throws an InvalidParameterException.
    @Test(expected = InvalidParameterException.class)
    public void testBuildFail() throws InvalidParameterException {
        // exercise and verify
        new CreateFirewallRuleRequest.Builder().build(null);
    }

}
