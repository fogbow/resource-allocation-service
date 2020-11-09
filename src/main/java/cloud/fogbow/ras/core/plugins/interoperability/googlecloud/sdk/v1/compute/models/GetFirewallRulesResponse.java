package cloud.fogbow.ras.core.plugins.interoperability.googlecloud.sdk.v1.compute.models;

import cloud.fogbow.common.constants.GoogleCloudConstants;
import cloud.fogbow.common.util.GsonHolder;
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

        public String getId() {
            return this.id;
        }
        public String getName() {
            return this.name;
        }
        public String getNetwork() {
            return this.network;
        }
    }

    public List<FirewallRule> getSecurityGroupRules() {
        return this.firewallRules;
    }
}
