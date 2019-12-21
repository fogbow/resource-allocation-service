package cloud.fogbow.ras.core.plugins.interoperability.cloudstack.publicip.v4_9;

import cloud.fogbow.common.util.GsonHolder;
import com.google.gson.annotations.SerializedName;

import static cloud.fogbow.common.constants.CloudStackConstants.PublicIp.CREATE_FIREWALL_RULE_RESPONSE;
import static cloud.fogbow.common.constants.CloudStackConstants.PublicIp.JOB_ID_KEY_JSON;

/**
 * Documentation: https://cloudstack.apache.org/api/apidocs-4.9/apis/createFirewallRule.html
 *
 * Response Example:
 * {
 *   "createfirewallruleresponse":{
 *     "jobid":"7568bb4f-d925-437e-80b0-b2d984d225d4"
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
