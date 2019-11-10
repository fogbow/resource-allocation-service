package cloud.fogbow.ras.core.plugins.interoperability.cloudstack.securityrule.v4_9;

import cloud.fogbow.common.exceptions.InvalidParameterException;
import cloud.fogbow.common.util.connectivity.cloud.cloudstack.CloudStackRequest;

import static cloud.fogbow.common.constants.CloudStackConstants.SecurityGroupPlugin.DELETE_FIREWALL_RULE_COMMAND;
import static cloud.fogbow.ras.core.plugins.interoperability.emulatedcloud.EmulatedCloudConstants.Plugins.PublicIp.ID_KEY_JSON;

/**
 * Documentation https://cloudstack.apache.org/api/apidocs-4.9/apis/deleteFirewallRule.html
 *
 * <p> Example URL: <clodstack_endpoint>?apikey=<you_api_key>&command=deleteFirewallRule&id=<id_of_rule>&response=json&signature=<signature> </p>
 */
public class DeleteFirewallRuleRequest extends CloudStackRequest {

    protected DeleteFirewallRuleRequest(Builder builder) throws InvalidParameterException {
        super(builder.cloudStackUrl);
        addParameter(ID_KEY_JSON, builder.ruleId);
    }

    @Override
    public String getCommand() {
        return DELETE_FIREWALL_RULE_COMMAND;
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
