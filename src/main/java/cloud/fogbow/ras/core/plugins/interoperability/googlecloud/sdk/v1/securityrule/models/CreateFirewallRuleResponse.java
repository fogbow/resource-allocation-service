package cloud.fogbow.ras.core.plugins.interoperability.googlecloud.sdk.v1.securityrule.models;

import cloud.fogbow.common.constants.OpenStackConstants;
import cloud.fogbow.common.util.GsonHolder;
import com.google.gson.annotations.SerializedName;

public class CreateFirewallRuleResponse {
    @SerializedName(OpenStackConstants.Network.SECURITY_GROUP_RULE_KEY_JSON)
    public FirewallRule securityRule;

    private String id;

    public String getId() {
        return securityRule.id;
    }

    public String toJson() {
        return GsonHolder.getInstance().toJson(this);
    }

    public static CreateFirewallRuleResponse fromJson(String json) {
        return GsonHolder.getInstance().fromJson(json, CreateFirewallRuleResponse.class);
    }

    public static class FirewallRule {

        private String id;

        public FirewallRule(String id) {
            this.id = id;
        }
    }
}
