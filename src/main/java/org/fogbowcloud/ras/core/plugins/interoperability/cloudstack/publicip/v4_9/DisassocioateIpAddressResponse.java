package org.fogbowcloud.ras.core.plugins.interoperability.cloudstack.publicip.v4_9;

import org.fogbowcloud.ras.util.GsonHolder;

/**
 * Documentation : https://cloudstack.apache.org/api/apidocs-4.9/apis/disassociateIpAddress.html
 * 
 * Response Example: 
 *
 */	
public class DisassocioateIpAddressResponse {

    public static DisassocioateIpAddressResponse fromJson(String json) {
        return GsonHolder.getInstance().fromJson(json, DisassocioateIpAddressResponse.class);
    }
	
}
