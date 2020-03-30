package cloud.fogbow.ras.core.plugins.interoperability.azure.securityrule.util;

import cloud.fogbow.ras.api.parameters.SecurityRule;
import cloud.fogbow.ras.core.plugins.interoperability.azure.securityrule.sdk.AzureNetworkSecurityGroupSDK;
import org.junit.Assert;
import org.junit.Test;

public class AzureSecurityRuleUtilTest {

    // test case: When calling the getDirection method with IN BOUND value,
    // it must verify if It returns a IN BOUND value.
    @Test
    public void testGetDirectionSuccessfullyWhenIsInBound() {
        // set up
        AzureNetworkSecurityGroupSDK.Direction directionExpected = AzureNetworkSecurityGroupSDK.Direction.IN_BOUND;

        // exercise
        AzureNetworkSecurityGroupSDK.Direction direction = AzureSecurityRuleUtil.getDirection(SecurityRule.Direction.IN);

        // verify
        Assert.assertEquals(directionExpected, direction);
    }

    // test case: When calling the getDirection method with OUT BOUND value,
    // it must verify if It returns a OUT BOUND value.
    @Test
    public void testGetDirectionSuccessfullyWhenIsOutBound() {
        // set up
        AzureNetworkSecurityGroupSDK.Direction directionExpected = AzureNetworkSecurityGroupSDK.Direction.OUT_BOUND;

        // exercise
        AzureNetworkSecurityGroupSDK.Direction direction = AzureSecurityRuleUtil.getDirection(SecurityRule.Direction.OUT);

        // verify
        Assert.assertEquals(directionExpected, direction);
    }

}
