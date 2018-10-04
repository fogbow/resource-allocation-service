package org.fogbowcloud.ras.core.plugins.interoperability.cloudstack.publicip.v4_9;

import com.google.gson.annotations.SerializedName;
import org.fogbowcloud.ras.util.GsonHolder;

import static org.fogbowcloud.ras.core.plugins.interoperability.cloudstack.CloudStackRestApiConstants.PublicIp.CREATE_FIREWALL_RULE_RESPONSE;
import static org.fogbowcloud.ras.core.plugins.interoperability.cloudstack.CloudStackRestApiConstants.PublicIp.JOB_ID_KEY_JSON;

public class CreateFirewallRuleAsyncResponse {

    @SerializedName(CREATE_FIREWALL_RULE_RESPONSE)
    private CreateFirewallRuleResponse response;

    public String getJobId() {
        return response.jobId;
    }

    public static CreateFirewallRuleAsyncResponse fromJson(String json) {
        return GsonHolder.getInstance().fromJson(json, CreateFirewallRuleAsyncResponse.class);
    }

    private class CreateFirewallRuleResponse {

        @SerializedName(JOB_ID_KEY_JSON)
        private String jobId;

    }
}
