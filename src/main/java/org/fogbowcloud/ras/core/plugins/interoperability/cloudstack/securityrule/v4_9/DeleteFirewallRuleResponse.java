package org.fogbowcloud.ras.core.plugins.interoperability.cloudstack.securityrule.v4_9;

import com.google.gson.annotations.SerializedName;
import org.fogbowcloud.ras.core.plugins.interoperability.cloudstack.CloudStackRestApiConstants;
import org.fogbowcloud.ras.core.plugins.interoperability.cloudstack.volume.v4_9.CreateVolumeResponse;
import org.fogbowcloud.ras.util.GsonHolder;

public class DeleteFirewallRuleResponse {
    @SerializedName(CloudStackRestApiConstants.PublicIp.DELETE_FIREWALL_RULE_RESPOSNE)
    private FirewallRuleResponse response;

    public static class FirewallRuleResponse {
        @SerializedName(CloudStackRestApiConstants.PublicIp.SUCCESS_KEY_JSON)
        private boolean success;
        @SerializedName(CloudStackRestApiConstants.PublicIp.DISPLAY_TEXT_KEY_JSON)
        private String displayText;
    }

    public static DeleteFirewallRuleResponse fromJson(String json) {
        return GsonHolder.getInstance().fromJson(json, DeleteFirewallRuleResponse.class);
    }

    public boolean isSuccess(){ return response.success; }

    public String getDisplayText(){ return response.displayText; }
}
