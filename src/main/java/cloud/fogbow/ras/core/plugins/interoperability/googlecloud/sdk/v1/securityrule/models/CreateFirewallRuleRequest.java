package cloud.fogbow.ras.core.plugins.interoperability.googlecloud.sdk.v1.securityrule.models;

import cloud.fogbow.common.constants.GoogleCloudConstants;
import cloud.fogbow.common.util.GsonHolder;

import cloud.fogbow.ras.api.parameters.SecurityRule.Direction;
import com.google.gson.annotations.SerializedName;

public class CreateFirewallRuleRequest {

    private FirewallRule firewallRule;

    public CreateFirewallRuleRequest(FirewallRule firewallRule) {
        this.firewallRule = firewallRule;
    }

    public String toJson() {
        return GsonHolder.getInstance().toJson(this);
    }

    private static class FirewallRule {
        @SerializedName(GoogleCloudConstants.Network.Firewall.NAME_KEY_JSON)
        private String name;
        @SerializedName(GoogleCloudConstants.Network.Firewall.ID_KEY_JSON)
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

        public FirewallRule(Builder builder) {
            this.name = builder.name;
            this.network = builder.network;
            this.ipProtocol = builder.ipProtocol;
            this.ports = builder.ports;
            this.direction = builder.direction;
            this.connection = builder.connection;
            this.incomeCidr = builder.incomeCidr;
            this.outcomeCidr = builder.outcomeCidr;
        }
    }

    private static class Connection {
        @SerializedName(GoogleCloudConstants.Network.Firewall.IP_PROTOCOL_KEY_JSON)
        private String ipProtocol;
        @SerializedName(GoogleCloudConstants.Network.Firewall.PORT_KEY_JSON)
        private String[] ports;

        private Connection(ConnectionBuilder builder){
            this.ipProtocol = builder.ipProtocol;
        }
    }

    private static class ConnectionBuilder {

        private String ipProtocol;
        private String[] ports;

        private ConnectionBuilder ipProtocol(String name) {
            this.ipProtocol = name;
            return this;
        }

        private ConnectionBuilder ports(String[] ports) {
            this.ports = ports;
            return this;
        }

        private Connection build() {
            return new Connection(this);
        }
    }

    public static class Builder {

        public String name;
        public String network;
        private String[] incomeCidr;
        private String[] outcomeCidr;
        public String ipProtocol;
        public String[] ports;
        public String direction;
        public Connection connection;

        public Builder name(String name) {
            this.name = name;
            return this;
        }

        public Builder network(String networkName) {
            this.network = networkName;
            return this;
        }

        public Builder direction(String direction) {
            this.direction = direction.toUpperCase();
            return this;
        }

        public Builder ports(String portFrom, String portTo) {
            String portRange = null;

            if(portFrom.equals(portTo)) {
                portRange = portFrom;
            } else {
                portRange = portFrom + "-" + portTo;
            }

            this.ports = new String[]{portRange};
            return this;
        }

        public Builder connection() {
            Connection connection = new ConnectionBuilder()
                    .ipProtocol(this.ipProtocol)
                    .ports(this.ports)
                    .build();

            this.connection = connection;
            return this;
        }

        public Builder incomeCidr(String cidr) {
            if(this.direction.equals(Direction.IN.toString().toUpperCase())) {
                this.incomeCidr = new String[] {cidr};
            }
            return this;
        }

        public Builder outcomeCidr(String cidr) {
            if(this.direction.equals(Direction.OUT.toString().toUpperCase())) {
                this.outcomeCidr = new String[] {cidr};
            }
            return this;
        }

        public Builder ipProtocol(String ipProtocol) {
            this.ipProtocol = ipProtocol;
            return this;
        }

        public CreateFirewallRuleRequest build() {
            FirewallRule firewallRule = new FirewallRule(this);
            return new CreateFirewallRuleRequest(firewallRule);
        }
    }
}
