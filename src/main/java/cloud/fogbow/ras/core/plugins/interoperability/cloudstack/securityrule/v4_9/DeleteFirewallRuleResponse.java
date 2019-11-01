package cloud.fogbow.ras.core.plugins.interoperability.cloudstack.securityrule.v4_9;

import cloud.fogbow.common.constants.CloudStackConstants;
import cloud.fogbow.common.util.GsonHolder;
import com.google.gson.annotations.SerializedName;

/**
 * Documentation: https://cloudstack.apache.org/api/apidocs-4.9/apis/deleteFirewallRule.html
 *
 * Example response:
 * {
 * "deletefirewallruleresponse": {
 *     "jobid":"bcf3e901-a4a0-4b1c-bcb2-5d8972e30a88"
 *   }
 * }
 *
 * Considering this as a async call, will be needed
 * more requests to check the result of this operation
 */

public class DeleteFirewallRuleResponse {
    @SerializedName(CloudStackConstants.SecurityGroupPlugin.DELETE_FIREWALL_RULE_RESPONSE)
    private FirewallRuleResponse response;

    public static class FirewallRuleResponse {
        @SerializedName(CloudStackConstants.PublicIp.JOB_ID_KEY_JSON)
        private String jobId;
    }

    public static DeleteFirewallRuleResponse fromJson(String json) {
        return GsonHolder.getInstance().fromJson(json, DeleteFirewallRuleResponse.class);
    }

    public String getJobId(){return response.jobId; }
}
