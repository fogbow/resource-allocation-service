package cloud.fogbow.ras.core.plugins.interoperability.googlecloud.sdk.v1.securityrule.models;

import cloud.fogbow.common.constants.GoogleCloudConstants;
import cloud.fogbow.common.util.GsonHolder;

import cloud.fogbow.ras.api.parameters.SecurityRule;
import cloud.fogbow.ras.api.parameters.SecurityRule.EtherType;
import com.google.gson.annotations.SerializedName;

import java.util.ArrayList;
import java.util.List;

public class GetFirewallRulesResponse {

    @SerializedName(GoogleCloudConstants.Network.Firewall.FIREWALLS_JSON)
    ArrayList<FirewallRule> firewallRules;

    public static GetFirewallRulesResponse fromJson(String json) {
        return GsonHolder.getInstance().fromJson(json, GetFirewallRulesResponse.class);
    }

    public static class FirewallRule {
        @SerializedName(GoogleCloudConstants.Network.Firewall.ID_KEY_JSON)
        private String id;
        @SerializedName(GoogleCloudConstants.Network.Firewall.NAME_KEY_JSON)
        private String name;
        @SerializedName(GoogleCloudConstants.Network.NETWORK_KEY_JSON)
        private String network;
        @SerializedName(GoogleCloudConstants.Network.Firewall.DIRECTION_KEY_JSON)
        private String direction;
        @SerializedName(GoogleCloudConstants.Network.Firewall.CIDR_INCOME_JSON)
        private String[] incomeCidr;
        @SerializedName(GoogleCloudConstants.Network.Firewall.CIDR_OUTCOME_KEY_JSON)
        private String[] outcomeCidr;
        @SerializedName(GoogleCloudConstants.Network.Firewall.ALLOWED_KEY_JSON)
        private Connection connection;
        private String ipProtocol;
        private String[] ports;
        @SerializedName(GoogleCloudConstants.Network.Firewall.ETHERTYPE_KEY_JSON)
        private final String ETHERTYPE = EtherType.IPv4.toString();

        public String getId() {
            return this.id;
        }
        public String getName() {
            return this.name;
        }
        public String getNetwork() {
            return this.network;
        }
        public String getCidr() {
            String[] cidr = null;
            if(this.getDirection().equals(SecurityRule.Direction.IN)) {
                cidr = this.incomeCidr;
            } else {
                cidr = this.outcomeCidr;
            }
            return cidr[0];
        }
        public Connection[] getConnection() {
            return new Connection[] {this.connection};
        }
        public String getIpProtocol() {
            return this.connection.getIpProtocol();
        }
        public String getPort() {
            return this.connection.getPort();
        }
        public String getDirection() {
            return this.direction;
        }
        public String getEtherType() {
            return ETHERTYPE;
        }

    }

    private static class Connection {
        @SerializedName(GoogleCloudConstants.Network.Firewall.IP_PROTOCOL_KEY_JSON)
        private String ipProtocol;
        @SerializedName(GoogleCloudConstants.Network.Firewall.PORT_KEY_JSON)
        private String[] port;

        public String getIpProtocol() {
            return this.ipProtocol;
        }
        public String getPort() {
            return this.port[0];
        }
    }

    public List<FirewallRule> getSecurityGroupRules() {
        return this.firewallRules;
    }
}
