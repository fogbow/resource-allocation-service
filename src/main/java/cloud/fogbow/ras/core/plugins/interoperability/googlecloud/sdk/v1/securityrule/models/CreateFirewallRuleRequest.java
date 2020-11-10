package cloud.fogbow.ras.core.plugins.interoperability.googlecloud.sdk.v1.securityrule.models;

import cloud.fogbow.common.constants.GoogleCloudConstants;
import cloud.fogbow.common.util.GsonHolder;

import cloud.fogbow.ras.api.parameters.SecurityRule.Direction;
import com.google.gson.annotations.SerializedName;

public class CreateFirewallRuleRequest {

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
    private CreateFirewallRuleConnection[] allowedConnection;
    @SerializedName(GoogleCloudConstants.Network.Firewall.DENIED_KEY_JSON)
    private CreateFirewallRuleConnection[] deniedConnection;
    @SerializedName(GoogleCloudConstants.Network.Firewall.PRIORITY_KEY_JSON)
    private int priority;

    public CreateFirewallRuleRequest(FirewallRule firewallRule) {
        this.name = firewallRule.name;
        this.network = firewallRule.network;
        this.direction = firewallRule. direction;
        this.incomeCidr = firewallRule.incomeCidr;
        this.outcomeCidr = firewallRule.outcomeCidr;
        this.allowedConnection = firewallRule.allowedConnection;
        this.deniedConnection = firewallRule.deniedConnection;
        this.priority = firewallRule.priority;
    }

    public String toJson() {
        return GsonHolder.getInstance().toJson(this);
    }

    private static class FirewallRule {
        private String name;
        private String network;
        private String direction;
        private String[] incomeCidr;
        private String[] outcomeCidr;
        private CreateFirewallRuleConnection[] allowedConnection;
        private CreateFirewallRuleConnection[] deniedConnection;
        private int priority;

        public FirewallRule(Builder builder) {
            this.name = builder.name;
            this.network = builder.network;
            this.priority = builder.priority;
            this.direction = builder.direction;
            this.allowedConnection = builder.allowedConnection;
            this.deniedConnection = builder.deniedConnection;
            this.incomeCidr = builder.incomeCidr;
            this.outcomeCidr = builder.outcomeCidr;
            this.priority = builder.priority;
        }
    }

    public static class Builder {

        public String name;
        public String network;
        public int priority;
        private String[] incomeCidr;
        private String[] outcomeCidr;
        public String direction;
        private CreateFirewallRuleConnection[] allowedConnection;
        private CreateFirewallRuleConnection[] deniedConnection;

        public Builder name(String name) {
            this.name = name;
            return this;
        }

        public Builder network(String networkName) {
            this.network = networkName;
            return this;
        }
        public Builder priority(int priority) {
            this.priority = priority;
            return this;
        }

        public Builder direction(String direction) {
            this.direction = direction.toUpperCase();
            return this;
        }

        public Builder connection(String portFrom, String portTo, String ipProtocol) {
            String portRange = null;

            if(portFrom.equals(portTo)) {
                portRange = portFrom;
            } else {
                portRange = portFrom + "-" + portTo;
            }

            CreateFirewallRuleConnection connection = new CreateFirewallRuleConnection.ConnectionBuilder()
                    .ipProtocol(ipProtocol)
                    .ports(new String[]{portRange})
                    .build();
            if(this.direction.equalsIgnoreCase(Direction.IN.toString())) {
                this.allowedConnection = new CreateFirewallRuleConnection[]{connection};
            } else if(this.direction.equalsIgnoreCase(Direction.OUT.toString())) {
                this.deniedConnection = new CreateFirewallRuleConnection[]{connection};
            }
            return this;
        }

        public Builder incomeCidr(String cidr) {
            if(this.direction.equalsIgnoreCase(Direction.IN.toString())) {
                this.incomeCidr = new String[] {cidr};
            }
            return this;
        }

        public Builder outcomeCidr(String cidr) {
            if(this.direction.equalsIgnoreCase(Direction.OUT.toString())) {
                this.outcomeCidr = new String[] {cidr};
            }
            return this;
        }

        public CreateFirewallRuleRequest build() {
            FirewallRule firewallRule = new FirewallRule(this);
            return new CreateFirewallRuleRequest(firewallRule);
        }
    }
}
