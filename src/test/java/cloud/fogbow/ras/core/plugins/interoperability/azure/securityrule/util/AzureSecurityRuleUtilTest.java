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

    // test case: When calling the getPorts method with wrong values,
    // it must verify if It throws an NumberFormatException.
    @Test
    public void testGetPortsFailed() {
        // set up
        String portRange = TestUtils.ANY_VALUE;

        // verify
        this.expectedException.expect(NumberFormatException.class);

        // exercise
        AzureSecurityRuleUtil.getPorts(portRange);
    }

    // test case: When calling the inferEtherType method with ipv4,
    // it must verify if It returns etherType ipv4.
    @Test
    public void testInferEtherTypeWithIpv4() {
        // set up
        String ip = "10.10.10.10";

        // verify
        SecurityRule.EtherType etherType = AzureSecurityRuleUtil.inferEtherType(ip);

        // exercise
        Assert.assertEquals(SecurityRule.EtherType.IPv4, etherType);
    }

    // test case: When calling the inferEtherType method with ipv6,
    // it must verify if It returns etherType ipv6.
    @Test
    public void testInferEtherTypeWithIpv6() {
        // set up
        String ip = "2001:cdba:0000:0000:0000:0000:3257:9652";

        // verify
        SecurityRule.EtherType etherType = AzureSecurityRuleUtil.inferEtherType(ip);

        // exercise
        Assert.assertEquals(SecurityRule.EtherType.IPv6, etherType);
    }

    // test case: When calling the inferEtherType method with unknown value,
    // it must verify if It returns null.
    @Test
    public void testInferEtherTypeWithUnknownValue() {
        // set up
        String ip = TestUtils.ANY_VALUE;

        // verify
        SecurityRule.EtherType etherType = AzureSecurityRuleUtil.inferEtherType(ip);

        // exercise
        Assert.assertNull(etherType);
    }

}
