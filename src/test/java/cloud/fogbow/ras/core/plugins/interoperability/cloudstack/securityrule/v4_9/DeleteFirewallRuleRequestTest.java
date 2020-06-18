package cloud.fogbow.ras.core.plugins.interoperability.cloudstack.securityrule.v4_9;

import cloud.fogbow.common.constants.CloudStackConstants;
import cloud.fogbow.common.exceptions.InternalServerErrorException;
import cloud.fogbow.common.exceptions.InvalidParameterException;
import cloud.fogbow.common.util.connectivity.cloud.cloudstack.CloudStackUrlUtil;
import cloud.fogbow.ras.core.plugins.interoperability.cloudstack.CloudstackTestUtils;
import org.apache.http.client.utils.URIBuilder;
import org.junit.Assert;
import org.junit.Test;

import static cloud.fogbow.ras.core.plugins.interoperability.emulatedcloud.EmulatedCloudConstants.Plugins.PublicIp.ID_KEY_JSON;

public class DeleteFirewallRuleRequestTest {

    // test case: When calling the build method, it must verify if It generates the right URL.
    @Test
    public void testBuildSuccessfully() throws InternalServerErrorException {
        // set up
        URIBuilder uriBuilder = CloudStackUrlUtil.createURIBuilder(
                CloudstackTestUtils.CLOUDSTACK_URL_DEFAULT,
                CloudStackConstants.SecurityGroup.DELETE_FIREWALL_RULE_COMMAND);
        String urlBaseExpected = uriBuilder.toString();
        String ruleId = "ruleId";

        String ruleIdStructureUrl = String.format("%s=%s", ID_KEY_JSON, ruleId);

        String[] urlStructure = new String[] {
                urlBaseExpected,
                ruleIdStructureUrl,
        };
        String urlExpectedStr = String.join(
                CloudstackTestUtils.AND_OPERATION_URL_PARAMETER, urlStructure);

        // exercise
        DeleteFirewallRuleRequest deleteFirewallRuleRequest = new DeleteFirewallRuleRequest.Builder()
                .ruleId(ruleId)
                .build(CloudstackTestUtils.CLOUDSTACK_URL_DEFAULT);
        String deleteFireWallRequestUrl = deleteFirewallRuleRequest.getUriBuilder().toString();

        // verify
        Assert.assertEquals(urlExpectedStr, deleteFireWallRequestUrl);
    }

    // test case: When calling the build method with a null parameter,
    // it must verify if It throws an InternalServerErrorException.
    @Test(expected = InternalServerErrorException.class)
    public void testBuildFail() throws InternalServerErrorException {
        // exercise and verify
        new DeleteFirewallRuleRequest.Builder().build(null);
    }


}
