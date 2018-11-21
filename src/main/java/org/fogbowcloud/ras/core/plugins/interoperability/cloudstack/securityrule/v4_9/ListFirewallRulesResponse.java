package org.fogbowcloud.ras.core.plugins.interoperability.cloudstack.securityrule.v4_9;

import static org.fogbowcloud.ras.core.plugins.interoperability.cloudstack.CloudStackRestApiConstants.SecurityGroupPlugin.LIST_FIREWALL_RULES_KEY_JSON;

import java.util.List;

import org.fogbowcloud.ras.core.models.securitygroups.SecurityGroupRule;
import org.fogbowcloud.ras.util.GsonHolder;

import com.google.gson.annotations.SerializedName;

/**
 * Documentation: https://cloudstack.apache.org/api/apidocs-4.9/apis/listFirewallRules.html
 * 
 * Response example:
 * 
 * {  
 *   "listfirewallrulesresponse":{  
 *      "firewallrule":[  
 *         {  
 *            "id":"e11ef52c-f5c3-4962-a0a0-7b545cb70a77",
 *            "protocol":"tcp",
 *            "startport":23,
 *            "endport":24,
 *            "cidrlist":"0.0.0.0/0",            
 *         },
 *         {  
 *            "id":"0bd00d5f-5550-4e06-880a-893de7a13082",
 *            "protocol":"tcp",
 *            "startport":22,
 *            "endport":22,
 *            "cidrlist":"0.0.0.0/0",
 *         }
 *       ]
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
    
    private class ListFirewallRules {

    	// TODO use constants
        @SerializedName("template")
        private List<SecurityGroupRule> images;

    }
    
}
