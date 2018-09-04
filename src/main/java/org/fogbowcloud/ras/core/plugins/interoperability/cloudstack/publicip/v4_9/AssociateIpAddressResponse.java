package org.fogbowcloud.ras.core.plugins.interoperability.cloudstack.publicip.v4_9;

import static org.fogbowcloud.ras.core.plugins.interoperability.cloudstack.CloudStackRestApiConstants.PublicIp.ID_KEY_JSON;
import static org.fogbowcloud.ras.core.plugins.interoperability.cloudstack.CloudStackRestApiConstants.PublicIp.IP_ADDRESS_KEY_JSON;

import org.fogbowcloud.ras.util.GsonHolder;

import com.google.gson.annotations.SerializedName;

/**
 * Documentation : https://cloudstack.apache.org/api/apidocs-4.9/apis/associateIpAddress.html
 * 
 * Response Example: 
 *
 */
public class AssociateIpAddressResponse {

	@SerializedName(IP_ADDRESS_KEY_JSON)
	private IpAddress ipAddress;
	
	public IpAddress getIpAddress() {
		return ipAddress;
	}
	
    public static AssociateIpAddressResponse fromJson(String json) {
        return GsonHolder.getInstance().fromJson(json, AssociateIpAddressResponse.class);
    }

    public class IpAddress {
        @SerializedName(ID_KEY_JSON)
        private String id;

        public String getId() {
            return id;
        }
    }
	
}
