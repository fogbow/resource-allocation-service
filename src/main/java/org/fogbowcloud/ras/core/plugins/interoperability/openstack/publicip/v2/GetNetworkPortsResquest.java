package org.fogbowcloud.ras.core.plugins.interoperability.openstack.publicip.v2;

import org.apache.http.client.utils.URIBuilder;

/**
 * Documentation : https://developer.openstack.org/api-ref/network/v2/#list-ports
 * 
 * http://url_neutro:port_neutro/v2.0/ports?device_id={vm_id}
 *
 */
public class GetNetworkPortsResquest {

	private URIBuilder uriBuilder;
	
	public static final String DEVICE_ID_KEY = "device_id";
	
	public GetNetworkPortsResquest(Builder builder) throws Exception {
		this.uriBuilder = new URIBuilder(builder.url);
		this.uriBuilder.addParameter(DEVICE_ID_KEY, builder.deviceId);
	}
	
	public String getUrl() {
		return this.uriBuilder.toString();
	}
	
    public static class Builder {
        
    	private String url;
    	private String deviceId;

        public Builder url(String url) {
            this.url = url;
            return this;
        }
    	
        public Builder deviceId(String deviceId) {
            this.deviceId = deviceId;
            return this;
        }

        public GetNetworkPortsResquest build() throws Exception {
            return new GetNetworkPortsResquest(this);
        }
    }	
	
}
