package cloud.fogbow.ras.core.plugins.interoperability.opennebula.securityrule.v5_4;

import cloud.fogbow.common.util.CidrUtils;
import org.apache.log4j.Logger;

import cloud.fogbow.ras.api.parameters.SecurityRule;
import cloud.fogbow.ras.api.parameters.SecurityRule.Direction;
import cloud.fogbow.ras.api.parameters.SecurityRule.EtherType;
import cloud.fogbow.ras.api.parameters.SecurityRule.Protocol;
import cloud.fogbow.ras.constants.Messages;

public class SecurityRuleUtil {

    private static final Logger LOGGER = Logger.getLogger(SecurityRuleUtil.class);

    private static final String CIDR_SEPARATOR = "/";
    private static final String RANGE_SEPARATOR = ":";

    // protocols template values
    public static final String ALL_TEMPLATE_VALUE = "ALL";
    public static final String IPSEC_TEMPLATE_VALUE = "IPSEC";
    public static final String ICMP_TEMPLATE_VALUE = "ICMP";
    public static final String ICMPV6_TEMPLATE_VALUE = "ICMPV6";
    public static final String TCP_TEMPLATE_VALUE = "TCP";
    public static final String UDP_TEMPLATE_VALUE = "UDP";

    // directions template values
    public static final String INBOUND_TEMPLATE_VALUE = "INBOUND";
    public static final String OUTBOUND_TEMPLATE_VALUE = "OUTBOUND";

    private static final int BASE_VALUE = 2;
    private static final int IPV4_AMOUNT_BITS = 32;
    private static final int IPV6_AMOUNT_BITS = 128;
    protected static final int INT_ERROR_CODE = -1;
	
    public static String getAddressCidr(Rule rule) {
        String size = rule.getSize();
        String ipAddress = rule.getIp();
        EtherType etherType = getEtherTypeFrom(rule);
        if (etherType != null && (size != null && !size.isEmpty())) {
            int range = Integer.parseInt(size);
            int cidr = calculateCidr(range, etherType);
            return ipAddress + CIDR_SEPARATOR + cidr;
        }
        return null;
    }

    public static int getPortInRange(Rule rule, int index) {
        // NOTE(pauloewerton): ONe allows to define range as intervals, but rules created by Fogbow always
        // follow the simpler pattern 'from:to'
        String range = rule.getRange();
        try {
            String[] splitPorts = range.split(RANGE_SEPARATOR);
            if (splitPorts.length == 1) {
                return Integer.parseInt(range);
            } else if (splitPorts.length == 2) {
                return Integer.parseInt(splitPorts[index]);
            } else {
                LOGGER.warn(String.format(Messages.Log.INCONSISTENT_RANGE_S, range));
            }
        } catch (Exception e) {
            LOGGER.warn(String.format(Messages.Exception.INVALID_PARAMETER_S, range), e);
        }
        return INT_ERROR_CODE;
    }

    public static Protocol getProtocolFrom(Rule rule) {
        String protocol = rule.getProtocol();
        if (protocol != null && !protocol.isEmpty()) {
            switch (protocol) {
            case TCP_TEMPLATE_VALUE:
                return Protocol.TCP;
            case UDP_TEMPLATE_VALUE:
                return Protocol.UDP;
            case ICMP_TEMPLATE_VALUE:
            case ICMPV6_TEMPLATE_VALUE:
                return Protocol.ICMP;
            case ALL_TEMPLATE_VALUE:
                return Protocol.ANY;
            // NOTE(pauloewerton): IPSEC is supported by ONe but not by Fogbow
            case IPSEC_TEMPLATE_VALUE:
            default:
                LOGGER.warn(String.format(Messages.Log.INCONSISTENT_PROTOCOL_S, protocol));
                return null;
            }
        }
        LOGGER.warn(String.format(Messages.Exception.INVALID_PARAMETER_S, protocol));
        return null;
    }

    public static EtherType getEtherTypeFrom(Rule rule) {
        String ipAddress = rule.getIp();
        if (ipAddress != null && !ipAddress.isEmpty()) {
            if (CidrUtils.isIpv4(ipAddress)) {
                return SecurityRule.EtherType.IPv4;
            } else if (CidrUtils.isIpv6(ipAddress)) {
                return SecurityRule.EtherType.IPv6;
            }
        }
        LOGGER.warn(String.format(Messages.Exception.INVALID_PARAMETER_S, ipAddress));
        return null;
    }

    public static Direction getDirectionFrom(Rule rule) {
        String type = rule.getType();
        if (type != null && !type.isEmpty()) {
            switch (type) {
            case INBOUND_TEMPLATE_VALUE:
                return Direction.IN;
            case OUTBOUND_TEMPLATE_VALUE:
                return Direction.OUT;
            default:
                LOGGER.warn(String.format(Messages.Log.INCONSISTENT_DIRECTION_S, type));
                return null;
            }
        }
        LOGGER.warn(String.format(Messages.Exception.INVALID_PARAMETER_S, type));
        return null;
    }
    
    protected static int calculateCidr(int range, EtherType etherType) {
    	int amountBits = etherType.equals(EtherType.IPv4) ? IPV4_AMOUNT_BITS : IPV6_AMOUNT_BITS;
    	int exponent = 1;
    	int value = 0;
    	for (int i = 0; i < amountBits; i++) {
    		if (exponent >= range) {
    			value = amountBits - i;
    			return value;
    		} else {
    			exponent *= BASE_VALUE;
    		}
    	}
    	return value;
    }
}
