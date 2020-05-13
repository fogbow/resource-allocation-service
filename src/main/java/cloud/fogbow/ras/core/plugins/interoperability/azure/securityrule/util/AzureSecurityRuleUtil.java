package cloud.fogbow.ras.core.plugins.interoperability.azure.securityrule.util;

import cloud.fogbow.common.exceptions.FogbowException;
import cloud.fogbow.common.exceptions.InvalidParameterException;
import cloud.fogbow.ras.api.parameters.SecurityRule;
import cloud.fogbow.ras.core.plugins.interoperability.azure.securityrule.sdk.AzureNetworkSecurityGroupSDK;
import cloud.fogbow.ras.core.plugins.interoperability.cloudstack.securityrule.v4_9.CidrUtils;
import com.microsoft.azure.management.network.SecurityRuleDirection;
import com.microsoft.azure.management.network.SecurityRuleProtocol;

import javax.annotation.Nullable;

public interface AzureSecurityRuleUtil {

    String DIRECTION_INBOUND = "Inbound";
    String TCP_VALUE = "TCP";
    String UDP_VALUE = "UDP";

    String PORTS_SEPARATOR = "-";
    String CIRD_SEPARATOR = "/";

    int IP_CIRD_ARRAY_POSITION = 0;
    int FROM_PORT_ARRAY_POSITION = 0;
    int TO_PORT_ARRAY_POSITION = 1;
    int SINGLE_PORT_SIZE = 1;

    String ASTERISK_IP_ADDRESS = "*";
    String ANY_IP_ADDRESS = "0.0.0.0";

    static SecurityRule.Direction getFogbowDirection(SecurityRuleDirection direction) {
        if (direction.toString().equals(DIRECTION_INBOUND)) {
            return SecurityRule.Direction.IN;
        }
        return SecurityRule.Direction.OUT;
    }

    static Ports getPorts(String portRange) throws NumberFormatException {
        String[] ports = portRange.split(PORTS_SEPARATOR);
        if (ports.length == SINGLE_PORT_SIZE) {
            return new Ports(ports[FROM_PORT_ARRAY_POSITION], ports[FROM_PORT_ARRAY_POSITION]);
        }
        return new Ports(ports[FROM_PORT_ARRAY_POSITION], ports[TO_PORT_ARRAY_POSITION]);
    }

    @Nullable
    static SecurityRule.EtherType inferEtherType(String ipAddress) {
        if (CidrUtils.isIpv4(ipAddress)) {
            return SecurityRule.EtherType.IPv4;
        } else if (CidrUtils.isIpv6(ipAddress)) {
            return SecurityRule.EtherType.IPv6;
        } else {
            return null;
        }
    }

    @Nullable
    static String getIpAddress(String cird) {
        String[] cirdChunks = cird.split(CIRD_SEPARATOR);
        String ip = cirdChunks[IP_CIRD_ARRAY_POSITION];
        if (ip.equals(ASTERISK_IP_ADDRESS)) {
            ip = ANY_IP_ADDRESS;
        }
        return CidrUtils.isIpv4(ip) || CidrUtils.isIpv6(ip) ? ip : null;
    }

    static AzureNetworkSecurityGroupSDK.Direction getFogbowDirection(SecurityRule.Direction direction) {
        if (direction.equals(SecurityRule.Direction.IN)) {
            return AzureNetworkSecurityGroupSDK.Direction.IN_BOUND;
        }
        return AzureNetworkSecurityGroupSDK.Direction.OUT_BOUND;
    }

    static SecurityRuleProtocol getFogbowProtocol(SecurityRule.Protocol protocol) throws FogbowException {
        switch (protocol) {
            case ANY:
                return SecurityRuleProtocol.ASTERISK;
            case TCP:
                return SecurityRuleProtocol.TCP;
            case UDP:
                return SecurityRuleProtocol.UDP;
            default:
                throw new InvalidParameterException();
        }
    }

    static SecurityRule.Protocol getFogbowProtocol(String securityRuleProtocol) {
        switch (securityRuleProtocol) {
            case TCP_VALUE:
                return SecurityRule.Protocol.TCP;
            case UDP_VALUE:
                return SecurityRule.Protocol.UDP;
            default:
                return SecurityRule.Protocol.ANY;
        }
    }

    class Ports {
        private int from;
        private int to;

        public Ports(String from, String to) {
            this.from = Integer.valueOf(from);
            this.to = Integer.valueOf(to);
        }

        public int getFrom() {
            return from;
        }

        public int getTo() {
            return to;
        }
    }

}
