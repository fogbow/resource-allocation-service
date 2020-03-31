package cloud.fogbow.ras.core.plugins.interoperability.azure.securityrule.util;

import cloud.fogbow.ras.api.parameters.SecurityRule;
import cloud.fogbow.ras.core.TestUtils;
import cloud.fogbow.ras.core.plugins.interoperability.azure.securityrule.sdk.AzureNetworkSecurityGroupSDK;
import org.junit.Assert;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

public class AzureSecurityRuleUtilTest {

    @Rule
    public ExpectedException expectedException = ExpectedException.none();


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

    // test case: When calling the getPorts method and with range port,
    // it must verify if It returns right values.
    @Test
    public void testGetPortsSuccessfullyWhenRangePort() {
        // set up
        int fromExpected = 22;
        int toExpected = 30;
        String portRange = fromExpected + AzureSecurityRuleUtil.PORTS_SEPARATOR + toExpected;

        // exercise
        AzureSecurityRuleUtil.Ports ports = AzureSecurityRuleUtil.getPorts(portRange);

        // verify
        Assert.assertEquals(fromExpected, ports.getFrom());
        Assert.assertEquals(toExpected, ports.getTo());
    }

    // test case: When calling the getPorts method and with single port,
    // it must verify if It returns same and right values.
    @Test
    public void testGetPortsSuccessfullyWhenSinglePort() {
        // set up
        int fromExpected = 22;
        int toExpected = fromExpected;
        String portRange = String.valueOf(fromExpected);

        // exercise
        AzureSecurityRuleUtil.Ports ports = AzureSecurityRuleUtil.getPorts(portRange);

        // verify
        Assert.assertEquals(fromExpected, ports.getFrom());
        Assert.assertEquals(toExpected, ports.getTo());
    }

    // test case: When calling the getPorts method and with single port,
    // it must verify if It returns same and right values.
    @Test
    public void testGetPortsFailed() {
        // set up
        String portRange = TestUtils.ANY_VALUE;

        // verify
        this.expectedException.expect(NumberFormatException.class);

        // exercise
        AzureSecurityRuleUtil.getPorts(portRange);
    }

}
