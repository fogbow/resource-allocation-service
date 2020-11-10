package cloud.fogbow.ras.core.plugins.interoperability.googlecloud.sdk.v1.securityrule.models;

import cloud.fogbow.common.util.GsonHolder;

public class CreateFirewallRuleResponse {
    private String id;

    public String getId() {
        return this.id;
    }

    public String toJson() {
        return GsonHolder.getInstance().toJson(this);
    }

    public static CreateFirewallRuleResponse fromJson(String json) {
        return GsonHolder.getInstance().fromJson(json, CreateFirewallRuleResponse.class);
    }
}
