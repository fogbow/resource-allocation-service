package cloud.fogbow.ras.core.plugins.interoperability.cloudstack.securityrule.v4_9;

import java.util.List;

import cloud.fogbow.common.util.GsonHolder;
import cloud.fogbow.ras.core.plugins.interoperability.cloudstack.CloudStackRestApiConstants;
import cloud.fogbow.ras.core.models.securityrules.Direction;

import com.google.gson.annotations.SerializedName;

/**
 * Documentation: https://cloudstack.apache.org/api/apidocs-4.9/apis/listFirewallRules.html
 * 
 * Response example:
 * 
 * {  
 *  "listfirewallrulesresponse":{  
 *    "firewallrule":[  
 *      {  
 *        "id":"e11ef52c-f5c3-4962-a0a0-7b545cb70a77",
 *        "protocol":"tcp",
 *        "startport":23,
 *        "endport":24,
 *        "ipaddress" : "0.0.0.1",
 *        "cidrList":"0.0.0.0/0",
 *      },
 *      {  
 *        "id":"0bd00d5f-5550-4e06-880a-893de7a13082",
 *        "protocol":"tcp",
 *        "startport":22,
 *        "endport":22,
 *        "ipaddress" : "0.0.0.2",
 *        "cidrList":"0.0.0.0/0",
 *      }
 *    ]
 *   }
 * }
 * 
 */
public class ListFirewallRulesResponse {

	@SerializedName(CloudStackRestApiConstants.SecurityGroupPlugin.LIST_FIREWALL_RULES_KEY_JSON)
	private ListFirewallRules response;

	public static ListFirewallRulesResponse fromJson(String jsonResponse) {
		return GsonHolder.getInstance().fromJson(jsonResponse, ListFirewallRulesResponse.class);
	}

	public List<SecurityRuleResponse> getSecurityRulesResponse() {
		return this.response.securityRulesResponse;
	}
	
	private class ListFirewallRules {

		@SerializedName(CloudStackRestApiConstants.SecurityGroupPlugin.FIREWALL_RULE_KEY_JSON)
		private List<SecurityRuleResponse> securityRulesResponse;

	}

    public class SecurityRuleResponse {

    	@SerializedName(CloudStackRestApiConstants.SecurityGroupPlugin.ID_KEY_JSON)
        private String instanceId;
    	@SerializedName(CloudStackRestApiConstants.SecurityGroupPlugin.CIDR_LIST_KEY_JSON)
        private String cidr;
    	@SerializedName(CloudStackRestApiConstants.SecurityGroupPlugin.START_PORT_KEY_JSON)
        private int portFrom;
    	@SerializedName(CloudStackRestApiConstants.SecurityGroupPlugin.END_PORT_KEY_JSON)
        private int portTo;
        @SerializedName(CloudStackRestApiConstants.SecurityGroupPlugin.PROPOCOL_KEY_JSON)
		private String protocol;
        @SerializedName(CloudStackRestApiConstants.SecurityGroupPlugin.IP_ADDRESS_KEY_JSON)
        private String ipAddress;
        
		public String getInstanceId() {
			return instanceId;
		}

		public String getCidr() {
			return cidr;
		}

		public int getPortFrom() {
			return portFrom;
		}

		public int getPortTo() {
			return portTo;
		}

		public Direction getDirection() {
			return Direction.IN;
		}

		public String getProtocol() {
			return protocol;
		}

		public String getIpAddress() {
			return ipAddress;
		}

		public void setProtocol(String protocol) {
			this.protocol = protocol;
		}        
        
    }

}
