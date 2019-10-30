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

public class ListFirewallRulesRequestTest {

    // test case: When calling the build method, it must verify if It generates the right URL.
    @Test
    public void testBuildSuccessfully() throws InvalidParameterException {
        // set up
        URIBuilder uriBuilder = CloudStackUrlUtil.createURIBuilder(
                CloudstackTestUtils.CLOUDSTACK_URL_DEFAULT,
                CloudStackConstants.SecurityGroupPlugin.LIST_FIREWALL_RULES_COMMAND);
        String urlBaseExpected = uriBuilder.toString();
        String ipAddressId = "ipAddressId";

        String ipAddressIdStructureUrl = String.format(
                "%s=%s", IP_ADDRESS_ID_KEY_JSON, ipAddressId);

        String[] urlStructure = new String[] {
                urlBaseExpected,
                ipAddressIdStructureUrl,
        };
        String urlExpectedStr = String.join(
                CloudstackTestUtils.AND_OPERATION_URL_PARAMETER, urlStructure);

        // exercise
        ListFirewallRulesRequest listFirewallRulesRequest = new ListFirewallRulesRequest.Builder()
                .ipAddressId(ipAddressId)
                .build(CloudstackTestUtils.CLOUDSTACK_URL_DEFAULT);
        String listFireWallRequestUrl = listFirewallRulesRequest.getUriBuilder().toString();

        // verify
        Assert.assertEquals(urlExpectedStr, listFireWallRequestUrl);
    }

    // test case: When calling the build method with a null parameter,
    // it must verify if It throws an InvalidParameterException.
    @Test(expected = InvalidParameterException.class)
    public void testBuildFail() throws InvalidParameterException {
        // exercise and verify
        new ListFirewallRulesRequest.Builder().build(null);
    }

}
