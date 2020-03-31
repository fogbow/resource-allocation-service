package cloud.fogbow.ras.core.plugins.interoperability.azure.securityrule.util;

import cloud.fogbow.common.exceptions.FogbowException;
import cloud.fogbow.common.exceptions.InvalidParameterException;
import cloud.fogbow.ras.api.parameters.SecurityRule;
import cloud.fogbow.ras.core.TestUtils;
import cloud.fogbow.ras.core.plugins.interoperability.azure.securityrule.sdk.AzureNetworkSecurityGroupSDK;
import com.microsoft.azure.management.network.SecurityRuleDirection;
import com.microsoft.azure.management.network.SecurityRuleProtocol;
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
        AzureNetworkSecurityGroupSDK.Direction direction = AzureSecurityRuleUtil.getFogbowDirection(SecurityRule.Direction.IN);

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
        AzureNetworkSecurityGroupSDK.Direction direction = AzureSecurityRuleUtil.getFogbowDirection(SecurityRule.Direction.OUT);

        // verify
        Assert.assertEquals(directionExpected, direction);
    }

    // test case: When calling the getPorts method with range port,
    // it must verify if It returns right values.
    @Test
    public void testGetPortsWhenRangePort() {
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

    // test case: When calling the getPorts method with single port,
    // it must verify if It returns same and right values.
    @Test
    public void testGetPortsWhenSinglePort() {
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

        // exercise
        SecurityRule.EtherType etherType = AzureSecurityRuleUtil.inferEtherType(ip);

        // verify
        Assert.assertEquals(SecurityRule.EtherType.IPv6, etherType);
    }

    // test case: When calling the inferEtherType method with unknown value,
    // it must verify if It returns null.
    @Test
    public void testInferEtherTypeWithUnknownValue() {
        // set up
        String ip = TestUtils.ANY_VALUE;

        // exercise
        SecurityRule.EtherType etherType = AzureSecurityRuleUtil.inferEtherType(ip);

        // verify
        Assert.assertNull(etherType);
    }

    // test case: When calling the getIpAddress method with ipv4,
    // it must verify if It returns ip ipv4.
    @Test
    public void testGetIpAddressWhenIpv4() {
        // set up
        String ip = "10.10.10.0";
        String cird = ip + "/24";

        // exercise
        String ipAddress = AzureSecurityRuleUtil.getIpAddress(cird);

        // verify
        Assert.assertEquals(ip, ipAddress);
    }

    // test case: When calling the getIpAddress method with ipv6,
    // it must verify if It returns ip ipv6.
    @Test
    public void testGetIpAddressWhenIpv6() {
        // set up
        String ip = "2001:cdba:0000:0000:0000:0000:3257:9652";
        String cird = ip + "/24";

        // exercise
        String ipAddress = AzureSecurityRuleUtil.getIpAddress(cird);

        // verify
        Assert.assertEquals(ip, ipAddress);
    }

    // test case: When calling the getIpAddress method with non ip value,
    // it must verify if It returns null.
    @Test
    public void testGetIpAddressWhenNonIpValue() {
        // set up
        String cird = TestUtils.ANY_VALUE;

        // exercise
        String ipAddress = AzureSecurityRuleUtil.getIpAddress(cird);

        // verify
        Assert.assertNull(ipAddress);
    }

    // test case: When calling the getFogbowDirection method with inbound value,
    // it must verify if It returns Direction In.
    @Test
    public void testGetFogbowDirectionWhenInboundValue() {
        // set up
        SecurityRuleDirection securityRuleDirection = SecurityRuleDirection.INBOUND;
        SecurityRule.Direction directionExpeceted = SecurityRule.Direction.IN;

        // exercise
        SecurityRule.Direction direction = AzureSecurityRuleUtil.getFogbowDirection(securityRuleDirection);

        // verify
        Assert.assertEquals(directionExpeceted, direction);
    }

    // test case: When calling the getFogbowDirection method with outbound value,
    // it must verify if It returns Direction In.
    @Test
    public void testGetFogbowDirectionWhenOutboundValue() {
        // set up
        SecurityRuleDirection securityRuleDirection = SecurityRuleDirection.OUTBOUND;
        SecurityRule.Direction directionExpeceted = SecurityRule.Direction.OUT;

        // exercise
        SecurityRule.Direction direction = AzureSecurityRuleUtil.getFogbowDirection(securityRuleDirection);

        // verify
        Assert.assertEquals(directionExpeceted, direction);
    }

    // test case: When calling the getProtocol method with ANY value,
    // it must verify if It returns protocol ASTERISK.
    @Test
    public void testGetProtocolWhenAnyValue() throws FogbowException {
        // set up
        SecurityRule.Protocol securityRuleFogbow = SecurityRule.Protocol.ANY;
        SecurityRuleProtocol securityRuleProtocolExpected = SecurityRuleProtocol.ASTERISK;

        // exercise
        SecurityRuleProtocol protocol = AzureSecurityRuleUtil.getFogbowProtocol(securityRuleFogbow);

        // verify
        Assert.assertEquals(securityRuleProtocolExpected, protocol);
    }

    // test case: When calling the getProtocol method with TCP value,
    // it must verify if It returns protocol TCP.
    @Test
    public void testGetProtocolWhenTCPValue() throws FogbowException {
        // set up
        SecurityRule.Protocol securityRuleFogbow = SecurityRule.Protocol.TCP;
        SecurityRuleProtocol securityRuleProtocolExpected = SecurityRuleProtocol.TCP;

        // exercise
        SecurityRuleProtocol protocol = AzureSecurityRuleUtil.getFogbowProtocol(securityRuleFogbow);

        // verify
        Assert.assertEquals(securityRuleProtocolExpected, protocol);
    }

    // test case: When calling the getProtocol method with UDP value,
    // it must verify if It returns protocol UDP.
    @Test
    public void testGetProtocolWhenUDPValue() throws FogbowException {
        // set up
        SecurityRule.Protocol securityRuleFogbow = SecurityRule.Protocol.UDP;
        SecurityRuleProtocol securityRuleProtocolExpected = SecurityRuleProtocol.UDP;

        // exercise
        SecurityRuleProtocol protocol = AzureSecurityRuleUtil.getFogbowProtocol(securityRuleFogbow);

        // verify
        Assert.assertEquals(securityRuleProtocolExpected, protocol);
    }

    // test case: When calling the getProtocol method with ICMP value,
    // it must verify if It throws an InvalidParameterException.
    @Test
    public void testGetProtocolWhenICMPValue() throws FogbowException {
        // set up
        SecurityRule.Protocol securityRuleFogbow = SecurityRule.Protocol.ICMP;

        // verify
        this.expectedException.expect(InvalidParameterException.class);

        // exercise
        AzureSecurityRuleUtil.getFogbowProtocol(securityRuleFogbow);
    }

    // test case: When calling the getFogbowProtocol method with TCP value,
    // it must verify if It returns protocol TCP.
    @Test
    public void testGetFogbowProtocolWhenTCPValue() {
        // set up
        String securityRuleProtocol = AzureSecurityRuleUtil.TCP_VALUE;
        SecurityRule.Protocol protocolExpected = SecurityRule.Protocol.TCP;

        // exercise
        SecurityRule.Protocol protocol = AzureSecurityRuleUtil.getFogbowProtocol(securityRuleProtocol);

        // verify
        Assert.assertEquals(protocolExpected, protocol);
    }

    // test case: When calling the getFogbowProtocol method with UDP value,
    // it must verify if It returns protocol UDP.
    @Test
    public void testGetFogbowProtocolWhenUDPValue() {
        // set up
        String securityRuleProtocol = AzureSecurityRuleUtil.UDP_VALUE;
        SecurityRule.Protocol protocolExpected = SecurityRule.Protocol.UDP;

        // exercise
        SecurityRule.Protocol protocol = AzureSecurityRuleUtil.getFogbowProtocol(securityRuleProtocol);

        // verify
        Assert.assertEquals(protocolExpected, protocol);
    }

    // test case: When calling the getFogbowProtocol method with any other value,
    // it must verify if It returns protocol ANY.
    @Test
    public void testGetFogbowProtocolWhenAnyValue() {
        // set up
        String securityRuleProtocol = TestUtils.ANY_VALUE;
        SecurityRule.Protocol protocolExpected = SecurityRule.Protocol.ANY;

        // exercise
        SecurityRule.Protocol protocol = AzureSecurityRuleUtil.getFogbowProtocol(securityRuleProtocol);

        // verify
        Assert.assertEquals(protocolExpected, protocol);
    }

}
