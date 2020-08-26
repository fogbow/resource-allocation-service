package cloud.fogbow.ras.core.plugins.interoperability.cloudstack.sdk.v4_9.publicip.model;

import cloud.fogbow.common.util.GsonHolder;
import com.google.gson.annotations.SerializedName;

import static cloud.fogbow.common.constants.CloudStackConstants.JOB_ID_KEY_JSON;
import static cloud.fogbow.common.constants.CloudStackConstants.SecurityGroup.CREATE_FIREWALL_RULE_RESPONSE;

/**
 * Documentation: https://cloudstack.apache.org/api/apidocs-4.9/apis/createFirewallRule.html
 * <p>
 * {
 *  "createfirewallruleresponse": {
 *      "jobstatus": 1,
 *   }
 * }
 */
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
