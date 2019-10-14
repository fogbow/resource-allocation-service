package cloud.fogbow.ras.core.plugins.interoperability.opennebula.securityrule.v5_4;

import cloud.fogbow.common.exceptions.FogbowException;
import cloud.fogbow.ras.api.parameters.SecurityRule;
import cloud.fogbow.ras.core.TestUtils;
import cloud.fogbow.ras.core.datastore.DatabaseManager;
import cloud.fogbow.ras.core.plugins.interoperability.opennebula.OpenNebulaBaseTests;
import cloud.fogbow.ras.core.plugins.interoperability.opennebula.OpenNebulaClientUtil;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.core.classloader.annotations.PrepareForTest;

import static cloud.fogbow.ras.core.plugins.interoperability.opennebula.securityrule.v5_4.SecurityRuleUtil.*;

@PrepareForTest({OpenNebulaClientUtil.class, DatabaseManager.class})
public class SecurityRuleUtilTest extends OpenNebulaBaseTests {

    private static final int PORT_INDEX = 0;

    private Rule rule;

    @Before
    public void setUp() throws FogbowException {
        super.setUp();

        this.rule = this.buildRule();
        PowerMockito.spy(SecurityRuleUtil.class);
    }

    // test case: when invoking getAddressCidr with a valid rule, the utility
    // should return its cidr
    @Test
    public void testGetAddressCidr() {
        // set up
        String expected = "192.168.100.1/25";

        // exercise
        String cidr = SecurityRuleUtil.getAddressCidr(this.rule);

        // verify
        Assert.assertEquals(expected, cidr);
    }

    // test case: when invoking getAddressCidr with an invalid size, the utility
    // should return null
    @Test
    public void testGetAddressCidrNull() {
        // set up
        this.rule.setSize(null);

        // exercise
        String cidr = SecurityRuleUtil.getAddressCidr(this.rule);

        // verify
        Assert.assertEquals(null, cidr);
    }

    // test cases: when calling getPortInRange
    // 1) with the default range 'from:to', the utility should return the port indicated by index
    // 2) with the simpler range 'port', the utility should return it after validation
    // 3) otherwise, return error code
    @Test
    public void testGetPortInRange() {
        // set up
        String simpleRange = "1";
        String invalidRange = "1:22:34";
        String emptyRange = "";

        int expectedPort = 1;
        int expectedErrorPort = -1;

        // exercise
        int port1 = SecurityRuleUtil.getPortInRange(this.rule, PORT_INDEX);
        this.rule.setRange(simpleRange);
        int port2 = SecurityRuleUtil.getPortInRange(this.rule, PORT_INDEX);
        this.rule.setRange(invalidRange);
        int port3 = SecurityRuleUtil.getPortInRange(this.rule, PORT_INDEX);
        this.rule.setRange(emptyRange);
        int port4 = SecurityRuleUtil.getPortInRange(this.rule, PORT_INDEX);

        // verify
        Assert.assertEquals(expectedPort, port1);
        Assert.assertEquals(expectedPort, port2);
        Assert.assertEquals(expectedErrorPort, port3);
        Assert.assertEquals(expectedErrorPort, port4);
    }

    // test cases: when calling getProtocolFrom with a Fogbow-compatible ONe secgroup protocol,
    // the utility should return its Fogbow counterpart or null otherwise
    @Test
    public void testGetProtocolFrom() {
        // exercise
        SecurityRule.Protocol anyExpected = SecurityRuleUtil.getProtocolFrom(this.rule);
        this.rule.setProtocol(TCP_TEMPLATE_VALUE);
        SecurityRule.Protocol tcpExpected = SecurityRuleUtil.getProtocolFrom(this.rule);
        this.rule.setProtocol(UDP_TEMPLATE_VALUE);
        SecurityRule.Protocol udpExpected = SecurityRuleUtil.getProtocolFrom(this.rule);
        this.rule.setProtocol(ICMP_TEMPLATE_VALUE);
        SecurityRule.Protocol icmpExpected = SecurityRuleUtil.getProtocolFrom(this.rule);
        this.rule.setProtocol(IPSEC_TEMPLATE_VALUE);
        SecurityRule.Protocol nullExpected1 = SecurityRuleUtil.getProtocolFrom(this.rule);
        this.rule.setProtocol(null);
        SecurityRule.Protocol nullExpected2 = SecurityRuleUtil.getProtocolFrom(this.rule);

        // exercise
        Assert.assertEquals(SecurityRule.Protocol.TCP, tcpExpected);
        Assert.assertEquals(SecurityRule.Protocol.UDP, udpExpected);
        Assert.assertEquals(SecurityRule.Protocol.ICMP, icmpExpected);
        Assert.assertEquals(SecurityRule.Protocol.ANY, anyExpected);
        Assert.assertEquals(null, nullExpected1);
        Assert.assertEquals(null, nullExpected2);
    }

    // test case: when calling getEtherTypeFrom, the utility should return ipv4 or ipv6 based
    // on the network addresses; return null otherwise
    @Test
    public void testGetEtherTypeFrom() {
        // set up
        String ipv6 = "fd5d:12c9:2201:1:db30:4db:785c:3037";

        // exercise
        SecurityRule.EtherType ipv4Expected = SecurityRuleUtil.getEtherTypeFrom(this.rule);
        this.rule.setIp(ipv6);
        SecurityRule.EtherType ipv6Expected = SecurityRuleUtil.getEtherTypeFrom(this.rule);
        this.rule.setIp(null);
        SecurityRule.EtherType nullExpected = SecurityRuleUtil.getEtherTypeFrom(this.rule);

        // verify
        Assert.assertEquals(SecurityRule.EtherType.IPv4, ipv4Expected);
        Assert.assertEquals(SecurityRule.EtherType.IPv6, ipv6Expected);
        Assert.assertEquals(null, nullExpected);
    }

    // test case: when calling getDirectionFrom with a ONe secgroup valid direction,
    // the utility should return its fogbow counterpart; otherwise, return null
    @Test
    public void testGetDirectionFrom() {
        // set up
        String invalidDirection = "INVALID";

        // exercise
        SecurityRule.Direction inboundExpected = SecurityRuleUtil.getDirectionFrom(this.rule);
        this.rule.setType(OUTBOUND_TEMPLATE_VALUE);
        SecurityRule.Direction outboundExpected = SecurityRuleUtil.getDirectionFrom(this.rule);
        this.rule.setType(invalidDirection);
        SecurityRule.Direction nullExpected1 = SecurityRuleUtil.getDirectionFrom(this.rule);
        this.rule.setType(null);
        SecurityRule.Direction nullExpected2 = SecurityRuleUtil.getDirectionFrom(this.rule);

        // verify
        Assert.assertEquals(SecurityRule.Direction.IN, inboundExpected);
        Assert.assertEquals(SecurityRule.Direction.OUT, outboundExpected);
        Assert.assertEquals(null, nullExpected1);
        Assert.assertEquals(null, nullExpected2);
    }

    // test case: when calling calculateCidr with valid address range size and ether type,
    // the utility should return the respective cidr notation suffix
    @Test
    public void testCalculateCidr() {
        // set up
        int ipv4ExpectedCidr = 25;
        int ipv6ExpectedCidr = 100;
        String ipv6RangeSize = "268435456";

        // exercise
        int ipv4Cidr = SecurityRuleUtil.calculateCidr(Integer.parseInt(this.rule.getSize()), SecurityRule.EtherType.IPv4);
        this.rule.setSize(ipv6RangeSize);
        int ipv6Cidr = SecurityRuleUtil.calculateCidr(Integer.parseInt(this.rule.getSize()), SecurityRule.EtherType.IPv6);

        // verify
        Assert.assertEquals(ipv4ExpectedCidr, ipv4Cidr);
        Assert.assertEquals(ipv6ExpectedCidr, ipv6Cidr);
    }

    private Rule buildRule() {
        Rule rule = Rule.builder()
                .protocol("ALL")
                .ip("192.168.100.1")
                .size("100")
                .range("1:65536")
                .type("INBOUND")
                .groupId(TestUtils.FAKE_SECURITY_GROUP_ID)
                .build();

        return rule;
    }
}
