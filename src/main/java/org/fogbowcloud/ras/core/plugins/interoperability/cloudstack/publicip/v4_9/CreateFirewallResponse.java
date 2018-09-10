package org.fogbowcloud.ras.core.plugins.interoperability.cloudstack.publicip.v4_9;

import org.fogbowcloud.ras.util.GsonHolder;

/**
 * Documentation : https://cloudstack.apache.org/api/apidocs-4.9/apis/createFirewallRule.html
 * 
 * Response Example: 
 *
 */	
public class CreateFirewallResponse {

    public static CreateFirewallResponse fromJson(String jsonResponse) {
        return GsonHolder.getInstance().fromJson(jsonResponse, CreateFirewallResponse.class);
    }

    public String getJobId() {
        return null;
    }
}
