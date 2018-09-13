package org.fogbowcloud.ras.core.plugins.interoperability.openstack.publicip.v2;

import static org.fogbowcloud.ras.core.plugins.interoperability.openstack.OpenstackRestApiConstants.PublicIp.FLOATING_IP_ADDRESS_KEY_JSON;
import static org.fogbowcloud.ras.core.plugins.interoperability.openstack.OpenstackRestApiConstants.PublicIp.FLOATING_IP_KEY_JSON;
import static org.fogbowcloud.ras.core.plugins.interoperability.openstack.OpenstackRestApiConstants.PublicIp.ID_KEY_JSON;
import static org.fogbowcloud.ras.core.plugins.interoperability.openstack.OpenstackRestApiConstants.PublicIp.STATUS_KEY_JSON;

import org.fogbowcloud.ras.util.GsonHolder;

import com.google.gson.annotations.SerializedName;

/**
 * Documentation : https://developer.openstack.org/api-ref/network/v2/#show-floating-ip-details
 * 
 * Response Example:
 * {
 *  "floatingip": {
 *    "id": "2f245a7b-796b-4f26-9cf9-9e82d248fda7",
 *    "floating_ip_address": "172.24.4.228",
 *    "status": "ACTIVE"
 *  }
 * }
 * 
 */
public class GetFloatingIpResponse {

	@SerializedName(FLOATING_IP_KEY_JSON)
	private FloatingIp floatingIp;
	
	
	public FloatingIp getFloatingIp() {
		return this.floatingIp;
	}
	
    public static GetFloatingIpResponse fromJson(String json) {
        return GsonHolder.getInstance().fromJson(json, GetFloatingIpResponse.class);
    }
    
    public static class FloatingIp {
    	@SerializedName(ID_KEY_JSON)
    	private String id;
    	@SerializedName(FLOATING_IP_ADDRESS_KEY_JSON)        
    	private String floatingIpAddress;
    	@SerializedName(STATUS_KEY_JSON)        
    	private String status;    	
    	
    	public String getId() {
    		return this.id;
    	}
    	
    	public String getFloatingIpAddress() {
    		return this.floatingIpAddress;
    	}
    	
    	public String getStatus() {
			return status;
		}
    }
	
}
