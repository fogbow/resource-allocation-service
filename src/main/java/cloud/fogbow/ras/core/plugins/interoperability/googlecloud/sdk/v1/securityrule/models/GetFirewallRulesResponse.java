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
        @SerializedName(GoogleCloudConstants.Network.Firewall.NETWORK_KEY_JSON)
        private String network;
        @SerializedName(GoogleCloudConstants.Network.Firewall.DIRECTION_KEY_JSON)
        private String direction;
        @SerializedName(GoogleCloudConstants.Network.Firewall.CIDR_INCOME_JSON)
        private String[] incomeCidr;
        @SerializedName(GoogleCloudConstants.Network.Firewall.CIDR_OUTCOME_KEY_JSON)
        private String[] outcomeCidr;
        @SerializedName(GoogleCloudConstants.Network.Firewall.ALLOWED_KEY_JSON)
        private Connection[] allowedConnection;
        @SerializedName(GoogleCloudConstants.Network.Firewall.DENIED_KEY_JSON)
        private Connection[] deniedConnection;
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
            return this.getDirection().equalsIgnoreCase(SecurityRule.Direction.IN.toString()) ? this.incomeCidr[0] : this.outcomeCidr[0];
        }
        public Connection[] getAllowedConnection() {
            return this.allowedConnection;
        }
        public Connection[] getDeniedConnection() {
            return this.deniedConnection;
        }
        public String getIpProtocol() {
            return this.getDirection().equalsIgnoreCase(SecurityRule.Direction.IN.toString()) ? this.allowedConnection[0].getIpProtocol() :
                                                                                                this.deniedConnection[0].getIpProtocol();
        }
        public String getPort() {
            return this.getDirection().equalsIgnoreCase(SecurityRule.Direction.IN.toString()) ? this.allowedConnection[0].getPort() :
                                                                                                this.deniedConnection[0].getPort();
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
