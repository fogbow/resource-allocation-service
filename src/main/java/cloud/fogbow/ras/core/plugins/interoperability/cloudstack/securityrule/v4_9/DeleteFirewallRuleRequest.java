package cloud.fogbow.ras.core.plugins.interoperability.cloudstack.securityrule.v4_9;

import cloud.fogbow.common.exceptions.InvalidParameterException;
import cloud.fogbow.common.util.cloud.cloudstack.CloudStackRequest;

/**
 * Documentation https://cloudstack.apache.org/api/apidocs-4.9/apis/deleteFirewallRule.html
 *
 * <p> Example URL: <clodstack_endpoint>?apikey=<you_api_key>&command=deleteFirewallRule&id=<id_of_rule>&response=json&signature=<signature> </p>
 */
public class DeleteFirewallRuleRequest extends CloudStackRequest {
    public static final String DELETE_RULE_COMMAND = "deleteFirewallRule";
    public static final String RULE_ID = "id";

    protected DeleteFirewallRuleRequest(Builder builder) throws InvalidParameterException {
        super(builder.cloudStackUrl);
        addParameter(RULE_ID, builder.ruleId);
    }

    @Override
    public String getCommand() {
        return DELETE_RULE_COMMAND;
    }

    public static class Builder {
        private String cloudStackUrl;
        private String ruleId;

        public Builder ruleId(String ruleId){
            this.ruleId = ruleId;
            return this;
        }

        DeleteFirewallRuleRequest build(String cloudStackUrl) throws InvalidParameterException {
            this.cloudStackUrl = cloudStackUrl;
            return new DeleteFirewallRuleRequest(this);
        }
    }
}
