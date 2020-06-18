package cloud.fogbow.ras.core.plugins.interoperability.cloudstack.securityrule.v4_9;

import cloud.fogbow.common.exceptions.InternalServerErrorException;
import cloud.fogbow.common.util.connectivity.cloud.cloudstack.CloudStackRequest;

import static cloud.fogbow.common.constants.CloudStackConstants.SecurityGroup.IP_ADDRESS_ID_KEY_JSON;
import static cloud.fogbow.common.constants.CloudStackConstants.SecurityGroup.LIST_FIREWALL_RULES_COMMAND;

/**
 * Documentation : https://cloudstack.apache.org/api/apidocs-4.9/apis/listFirewallRules.html
 * 
 * Request Example: {url_cloudstack}?command=listFirewallRules&ipaddressid={id}&apikey={apiKey}&secret_key={secretKey}
 *
 */	
public class ListFirewallRulesRequest extends CloudStackRequest {

	protected ListFirewallRulesRequest(Builder builder) throws InternalServerErrorException {
        super(builder.cloudStackUrl);
		addParameter(IP_ADDRESS_ID_KEY_JSON, builder.ipAddressId);
	}

	@Override
	public String getCommand() {
		return LIST_FIREWALL_RULES_COMMAND;
	}

    public static class Builder {
        private String cloudStackUrl;
        private String ipAddressId;

        public Builder ipAddressId(String ipAddressId) {
            this.ipAddressId = ipAddressId;
            return this;
        }

        public ListFirewallRulesRequest build(String cloudStackUrl) throws InternalServerErrorException {
            this.cloudStackUrl = cloudStackUrl;
            return new ListFirewallRulesRequest(this);
        }
    }	
	
}
