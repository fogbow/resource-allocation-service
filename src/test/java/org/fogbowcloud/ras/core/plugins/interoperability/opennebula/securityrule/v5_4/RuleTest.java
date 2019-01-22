package org.fogbowcloud.ras.core.plugins.interoperability.opennebula.securityrule.v5_4;

import org.fogbowcloud.ras.core.models.securityrules.Direction;
import org.fogbowcloud.ras.core.models.securityrules.EtherType;
import org.fogbowcloud.ras.core.models.securityrules.Protocol;
import org.junit.Assert;
import org.junit.Test;

public class RuleTest {

    // test case: success case
    @Test
    public void testGetEtherType() {
        // type ipv4
        // setup
        Rule rule = new Rule();
        rule.setIp("10.10.0.0");
        // exercise
        EtherType etherType = rule.getEtherType();
        //verify
        Assert.assertEquals(EtherType.IPv4, etherType);

        // type ipv6
        // setup
        Rule ruleTwo = new Rule();
        ruleTwo.setIp("2001:0db8:85a3:0000:0000:8a2e:0370:7334");
        // exercise
        EtherType etherTypeTwo = ruleTwo.getEtherType();
        //verify
        Assert.assertEquals(EtherType.IPv6, etherTypeTwo);
    }

    // test case: ip not recognized
    @Test
    public void testGetEtherTypeIpNotRecognized() {
        // type ipv4
        // setup
        Rule rule = new Rule();
        rule.setIp("10.10");
        // exercise
        EtherType etherType = rule.getEtherType();
        //verify
        Assert.assertNull(etherType);
    }

    // test case: success case
    @Test
    public void testGetDirection() {
        // setup
        Rule rule = new Rule();
        rule.setType(Rule.INBOUND_XML_TEMPLATE_VALUE);
        // exercise
        Direction direction = rule.getDirection();
        // verify
        Assert.assertEquals(Direction.IN, direction);


        // setup
        Rule ruleTwo = new Rule();
        ruleTwo.setType(Rule.OUTBOUND_XML_TEMPLATE_VALUE);
        // exercise
        Direction directionTwo = ruleTwo.getDirection();
        // verify
        Assert.assertEquals(Direction.OUT, directionTwo);
    }

    // test case: unknown rule type
    @Test
    public void testGetDirectionUnknown() {
        // setup
        Rule rule = new Rule();
        rule.setType("unknown");
        // exercise
        Direction direction = rule.getDirection();
        // verify
        Assert.assertNull(direction);
    }

    // test case: success case
    @Test
    public void testGetPortInRange() {
        // setup
        Rule rule = new Rule();
        int portToExpected = 3000;
        int portFromExpected = 22;
        String separator = Rule.OPENNEBULA_RANGE_SEPARATOR;
        String range = String.format("%s%s%s", portFromExpected, separator, portToExpected);
        rule.setRange(range);

        // verify
        int portFrom = rule.getPortInRange(Rule.POSITION_PORT_FROM_IN_RANGE);
        Assert.assertEquals(portFromExpected, portFrom);

        // verify
        int portTo = rule.getPortInRange(Rule.POSITION_PORT_TO_IN_RANGE);
        Assert.assertEquals(portToExpected, portTo);
    }

    // test case: inconsistent format of the range
    @Test
    public void testGetPortInRangeWithInconsistentFormat() {
        // setup
        Rule rule = new Rule();
        rule.setRange("20");
        // verify
        int portFrom = rule.getPortInRange(Rule.POSITION_PORT_FROM_IN_RANGE);
        Assert.assertEquals(Rule.INT_ERROR_CODE, portFrom);
    }

    // test case: range is empty
    @Test
    public void testGetPortInRangeEmpty() {
        // setup
        Rule rule = new Rule();
        // verify
        int portFrom = rule.getPortInRange(Rule.POSITION_PORT_FROM_IN_RANGE);
        Assert.assertEquals(Rule.MINIMUM_RANGE_PORT_NETWORK, portFrom);
    }

    // test case: success case with ipv4
    @Test
    public void testGetCIDRIPV4() {
        // setup
        String ipUnique = "10.10.0.0";

        Rule rule = new Rule();
        String size = "256";
        int subnetBySize = 24;
        rule.setIp(ipUnique);
        rule.setSize(size);

        // exercise and verify
        String cirdExpected = String.format("%s%s%s", ipUnique, Rule.CIRD_SEPARATOR, subnetBySize);
        Assert.assertEquals(cirdExpected, rule.getCIDR());

        // setup two
        Rule ruleTwo = new Rule();
        String sizeTwo = "65536";
        int subnetBySizeTwo = 16;
        ruleTwo.setIp(ipUnique);
        ruleTwo.setSize(sizeTwo);

        // exercise and verify two
        String cirdExpectedTwo = String.format("%s%s%s", ipUnique, Rule.CIRD_SEPARATOR, subnetBySizeTwo);
        Assert.assertEquals(cirdExpectedTwo, ruleTwo.getCIDR());
    }

    // test case: without ip
    @Test
    public void testGetCIDRWithoutIp() {
        // setup
        Rule rule = new Rule();
        // exercise and verify
        Assert.assertEquals(Rule.ALL_XML_TEMPLATE_VALUE, rule.getCIDR());
    }

    @Test
    public void testGetCIDRIPV6() {
        // setup
        String ipUnique = "2001:0db8:85a3:0000:0000:8a2e:0370:7334";

        Rule rule = new Rule();
        String size = "256";
        int subnetBySize = 120;
        rule.setIp(ipUnique);
        rule.setSize(size);

        // exercise and verify
        String cirdExpected = String.format("%s%s%s", ipUnique, Rule.CIRD_SEPARATOR, subnetBySize);
        Assert.assertEquals(cirdExpected, rule.getCIDR());

        // setup two
        Rule ruleTwo = new Rule();
        String sizeTwo = "268435456";
        int subnetBySizeTwo = 100;
        ruleTwo.setIp(ipUnique);
        ruleTwo.setSize(sizeTwo);

        // exercise and verify two
        String cirdExpectedTwo = String.format("%s%s%s", ipUnique, Rule.CIRD_SEPARATOR, subnetBySizeTwo);
        Assert.assertEquals(cirdExpectedTwo, ruleTwo.getCIDR());
    }

    // test case: get TCP protocol
    @Test
    public void testGetSRProtocolTCP() {
        // setup
        Rule rule = new Rule();
        rule.setProtocol(Rule.TCP_XML_TEMPLATE_VALUE);

        // exercise and verify
        Assert.assertEquals(Protocol.TCP, rule.getSRProtocol());
    }

    // test case: get UDP protocol
    @Test
    public void testGetSRProtocolUDP() {
        // setup
        Rule rule = new Rule();
        rule.setProtocol(Rule.UDP_XML_TEMPLATE_VALUE);

        // exercise and verify
        Assert.assertEquals(Protocol.UDP, rule.getSRProtocol());
    }

    // test case: get ICMP protocol
    @Test
    public void testGetSRProtocolICMP() {
        // setup
        Rule rule = new Rule();
        rule.setProtocol(Rule.ICMP_XML_TEMPLATE_VALUE);

        // exercise and verify
        Assert.assertEquals(Protocol.ICMP, rule.getSRProtocol());

        // setup
        new Rule();
        rule.setProtocol(Rule.ICMPV6_XML_TEMPLATE_VALUE);

        // exercise and verify
        Assert.assertEquals(Protocol.ICMP, rule.getSRProtocol());
    }

    // test case: get ALL protocol
    @Test
    public void testGetSRProtocolALL() {
        // setup
        Rule rule = new Rule();
        rule.setProtocol(Rule.ALL_XML_TEMPLATE_VALUE);

        // exercise and verify
        Assert.assertEquals(Protocol.ANY, rule.getSRProtocol());
    }

    // test case: inconsitent protocol
    @Test
    public void testGetSRProtocol() {
        // setup
        Rule rule = new Rule();
        rule.setProtocol("isconsistent");

        // exercise and verify
        Assert.assertNull(rule.getSRProtocol());
    }

}
