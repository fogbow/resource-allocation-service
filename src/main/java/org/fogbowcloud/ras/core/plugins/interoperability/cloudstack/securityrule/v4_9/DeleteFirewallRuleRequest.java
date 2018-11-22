package org.fogbowcloud.ras.core.plugins.interoperability.cloudstack.securityrule.v4_9;

import org.fogbowcloud.ras.core.exceptions.InvalidParameterException;
import org.fogbowcloud.ras.core.plugins.interoperability.cloudstack.CloudStackRequest;

public class DeleteFirewallRuleRequest extends CloudStackRequest {
    public static final String DELETE_RULE_COMMAND = "deleteFirewallRule";
    public static final String RULE_ID = "id";

    protected DeleteFirewallRuleRequest(Builder builder) throws InvalidParameterException {
        super();
        addParameter(RULE_ID, builder.ruleId);
    }

    @Override
    public String getCommand() {
        return DELETE_RULE_COMMAND;
    }

    public static class Builder {
        private String ruleId;

        public Builder ruleId(String ruleId){
            this.ruleId = ruleId;
            return this;
        }

        DeleteFirewallRuleRequest build() throws InvalidParameterException {
            return new DeleteFirewallRuleRequest(this);
        }
    }
}
