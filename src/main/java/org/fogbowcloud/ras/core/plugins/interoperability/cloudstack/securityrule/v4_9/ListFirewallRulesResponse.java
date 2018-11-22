package org.fogbowcloud.ras.core.plugins.interoperability.cloudstack.securityrule.v4_9;

import static org.fogbowcloud.ras.core.plugins.interoperability.cloudstack.CloudStackRestApiConstants.SecurityGroupPlugin.ALL_VALUE_PROTOCOL;
import static org.fogbowcloud.ras.core.plugins.interoperability.cloudstack.CloudStackRestApiConstants.SecurityGroupPlugin.CIDR_LIST_KEY_JSON;
import static org.fogbowcloud.ras.core.plugins.interoperability.cloudstack.CloudStackRestApiConstants.SecurityGroupPlugin.END_PORT_KEY_JSON;
import static org.fogbowcloud.ras.core.plugins.interoperability.cloudstack.CloudStackRestApiConstants.SecurityGroupPlugin.FIREWALL_RULE_KEY_JSON;
import static org.fogbowcloud.ras.core.plugins.interoperability.cloudstack.CloudStackRestApiConstants.SecurityGroupPlugin.ICMP_VALUE_PROTOCOL;
import static org.fogbowcloud.ras.core.plugins.interoperability.cloudstack.CloudStackRestApiConstants.SecurityGroupPlugin.ID_KEY_JSON;
import static org.fogbowcloud.ras.core.plugins.interoperability.cloudstack.CloudStackRestApiConstants.SecurityGroupPlugin.LIST_FIREWALL_RULES_KEY_JSON;
import static org.fogbowcloud.ras.core.plugins.interoperability.cloudstack.CloudStackRestApiConstants.SecurityGroupPlugin.PROPOCOL_KEY_JSON;
import static org.fogbowcloud.ras.core.plugins.interoperability.cloudstack.CloudStackRestApiConstants.SecurityGroupPlugin.START_PORT_KEY_JSON;
import static org.fogbowcloud.ras.core.plugins.interoperability.cloudstack.CloudStackRestApiConstants.SecurityGroupPlugin.TCP_VALUE_PROTOCOL;
import static org.fogbowcloud.ras.core.plugins.interoperability.cloudstack.CloudStackRestApiConstants.SecurityGroupPlugin.UDP_VALUE_PROTOCOL;
import static org.fogbowcloud.ras.core.plugins.interoperability.cloudstack.CloudStackRestApiConstants.SecurityGroupPlugin.IP_ADDRESS_KEY_JSON;

import java.util.List;

import org.fogbowcloud.ras.core.exceptions.UnexpectedException;
import org.fogbowcloud.ras.core.models.securityrules.Direction;
import org.fogbowcloud.ras.core.models.securityrules.EtherType;
import org.fogbowcloud.ras.core.models.securityrules.Protocol;
import org.fogbowcloud.ras.util.GsonHolder;

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
 *        "cidrlist":"0.0.0.0/0",            
 *      },
 *      {  
 *        "id":"0bd00d5f-5550-4e06-880a-893de7a13082",
 *        "protocol":"tcp",
 *        "startport":22,
 *        "endport":22,
 *        "ipaddress" : "0.0.0.2",
 *        "cidrlist":"0.0.0.0/0",
 *      }
 *    ]
 *   }
 * }
 * 
 */
public class ListFirewallRulesResponse {

	@SerializedName(LIST_FIREWALL_RULES_KEY_JSON)
	private ListFirewallRules response;

	public static ListFirewallRulesResponse fromJson(String jsonResponse) {
		return GsonHolder.getInstance().fromJson(jsonResponse, ListFirewallRulesResponse.class);
	}

	public List<SecurityRuleResponse> getSecurityRulesResponse() {
		return this.response.securityRulesResponse;
	}
	
	private class ListFirewallRules {

		@SerializedName(FIREWALL_RULE_KEY_JSON)
		private List<SecurityRuleResponse> securityRulesResponse;

	}

    public class SecurityRuleResponse {

    	@SerializedName(ID_KEY_JSON)    	
        private String instanceId;
    	@SerializedName(CIDR_LIST_KEY_JSON)
        private String cidr;
    	@SerializedName(START_PORT_KEY_JSON)
        private int portFrom;
    	@SerializedName(END_PORT_KEY_JSON)
        private int portTo;
        @SerializedName(PROPOCOL_KEY_JSON)
		private String protocol;
        @SerializedName(IP_ADDRESS_KEY_JSON)
        private String ipaddress;
        
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
		
		public EtherType getFogbowEtherType() throws UnexpectedException {
			if (CIRDUtils.isIpv4(this.ipaddress)) {
				return EtherType.IPv4;
			} else if (CIRDUtils.isIpv6(this.ipaddress)) {
				return EtherType.IPv6;
			}
			// TODO check if is necessary use the class Message
			// TODO Is the UnexpectedException correct in this case ?
			throw new UnexpectedException("Is not a ipv4 neither ipv6"); 
		}
		
		public Protocol getFogbowProtocol() throws UnexpectedException {
			switch (this.protocol) {
			case TCP_VALUE_PROTOCOL:
				return Protocol.TCP;
			case UDP_VALUE_PROTOCOL:
				return Protocol.UDP;
			case ICMP_VALUE_PROTOCOL:
				return Protocol.ICMP;
			case ALL_VALUE_PROTOCOL:
				return Protocol.ANY;
			default:
				// TODO check if is necessary use the class Message
				// TODO Is the UnexpectedException correct in this case ?				
				throw new UnexpectedException("Protocol not determined in the documentation");
			}
		}

		public void setProtocol(String protocol) {
			this.protocol = protocol;
		}        
        
    }
    
}
