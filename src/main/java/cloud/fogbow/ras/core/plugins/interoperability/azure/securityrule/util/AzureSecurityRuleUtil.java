package cloud.fogbow.ras.core.plugins.interoperability.azure.securityrule.util;

import cloud.fogbow.common.exceptions.FogbowException;
import cloud.fogbow.common.exceptions.UnexpectedException;
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

    static SecurityRule.Direction getDirection(SecurityRuleDirection direction) {
        if (direction.toString().equals(DIRECTION_INBOUND)) {
            return SecurityRule.Direction.IN;
        }
        return SecurityRule.Direction.OUT;
    }

    // TODO (chico) - Implement tests
    static Ports getPorts(String portRange) {
        String[] ports = portRange.split(PORTS_SEPARATOR);
        if (ports.length == SINGLE_PORT_SIZE) {
            return new Ports(ports[FROM_PORT_ARRAY_POSITION], ports[FROM_PORT_ARRAY_POSITION]);
        }
        return new Ports(ports[FROM_PORT_ARRAY_POSITION], ports[TO_PORT_ARRAY_POSITION]);
    }

    // TODO (chico) - Implement tests
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

    // TODO (chico) - Implement tests
    @Nullable
    static String getIpAddress(String cird) {
        String[] cirdChunks = cird.split(CIRD_SEPARATOR);
        String ip = cirdChunks[IP_CIRD_ARRAY_POSITION];
        return CidrUtils.isIpv4(ip) || CidrUtils.isIpv6(ip) ? ip : null;
    }

    // TODO (chico) - Implement tests
    static AzureNetworkSecurityGroupSDK.Direction getDirection(SecurityRule.Direction direction) {
        if (direction.equals(SecurityRule.Direction.IN)) {
            return AzureNetworkSecurityGroupSDK.Direction.IN_BOUND;
        }
        return AzureNetworkSecurityGroupSDK.Direction.OUT_BOUND;
    }

    // TODO (chico) - Implement tests
    static SecurityRuleProtocol getProtocol(SecurityRule.Protocol protocol) throws FogbowException {
        switch (protocol) {
            case ANY:
                return SecurityRuleProtocol.ASTERISK;
            case TCP:
                return SecurityRuleProtocol.TCP;
            case UDP:
                return SecurityRuleProtocol.UDP;
            default:
                throw new UnexpectedException();
        }
    }

    // TODO (chico) - Implement tests
    static SecurityRule.Protocol getProtocol(SecurityRuleProtocol securityRuleProtocol) {
        String securityRuleProtocolStr = securityRuleProtocol.toString();
        if (securityRuleProtocolStr.equals(TCP_VALUE)) {
            return SecurityRule.Protocol.TCP;
        } else if (securityRuleProtocolStr.equals(UDP_VALUE)) {
            return SecurityRule.Protocol.UDP;
        }
        return SecurityRule.Protocol.ANY;
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
