package cloud.fogbow.ras.core.plugins.interoperability.googlecloud.sdk.v1.securityrule.models;

import cloud.fogbow.common.constants.GoogleCloudConstants;
import cloud.fogbow.common.util.GsonHolder;

import cloud.fogbow.ras.api.parameters.SecurityRule;
import cloud.fogbow.ras.api.parameters.SecurityRule.EtherType;
import com.google.gson.annotations.SerializedName;

public class GetFirewallRuleResponse {

    public static GetFirewallRuleResponse fromJson(String json) {
        return GsonHolder.getInstance().fromJson(json, GetFirewallRuleResponse.class);
    }
    @SerializedName(GoogleCloudConstants.Network.FIREWALL_ID_JSON)
    private String id;
    @SerializedName(GoogleCloudConstants.Network.FIREWALL_NAME_JSON)
    private String name;
    @SerializedName(GoogleCloudConstants.Network.FIREWALL_NETWORK_JSON)
    private String network;
    @SerializedName(GoogleCloudConstants.Network.FIREWALL_CONNECTION_DIRECTION_JSON)
    private String direction;
    @SerializedName(GoogleCloudConstants.Network.FIREWALL_CIDR_INCOME_JSON)
    private String[] incomeCidr;
    @SerializedName(GoogleCloudConstants.Network.FIREWALL_CIDR_OUTCOME_JSON)
    private String[] outcomeCidr;
    @SerializedName(GoogleCloudConstants.Network.FIREWALL_ALLOWED_JSON)
    private Connection[] connection;
    private String ipProtocol;
    private String[] ports;
    @SerializedName(GoogleCloudConstants.Network.FIREWALL_ETHER_TYPE_JSON)
    private String etherType;

    public String getId() {
        return this.id;
    }
    public String getName() {
        return this.name;
    }
    public String getNetwork() {
        return this.network;
    }
    public String getDirection() {
        return this.direction;
    }
    public String getCidr() {
        String cidr = null;
        if(this.getDirection().equals(SecurityRule.Direction.IN)) {
            cidr = this.incomeCidr[0];
        } else {
            cidr = this.outcomeCidr[0];
        }
        return cidr;
    }
    public Connection[] getConnection() {
        return new Connection[] { this.connection[0] };
    }
    public String getIpProtocol() {
        return this.connection[0].getIpProtocol();
    }
    public String getPort() {
        return this.connection[0].getPort();
    }
    public String getEtherType() {
        return EtherType.IPv4.toString();
    }

    private static class Connection {
        @SerializedName(GoogleCloudConstants.Network.FIREWALL_ALLOWED_IP_PROTOCOL_JSON)
        private String ipProtocol;
        @SerializedName(GoogleCloudConstants.Network.FIREWALL_ALLOWED_PORT_JSON)
        private String[] port;

        public String getIpProtocol() {
            return this.ipProtocol;
        }
        public String getPort() {
            return this.port[0];
        }
    }
}
