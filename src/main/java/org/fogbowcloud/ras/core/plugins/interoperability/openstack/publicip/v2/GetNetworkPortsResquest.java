package org.fogbowcloud.ras.core.plugins.interoperability.openstack.publicip.v2;

import java.net.URISyntaxException;

import org.apache.http.client.utils.URIBuilder;

/**
 * Documentation : https://developer.openstack.org/api-ref/network/v2/#list-ports
 * 
 * Request Example : 
 * http://{url_neutro}:{port_neutro}/v2.0/ports?device_id={vm_id}&network_id={network_id}
 *
 */
public class GetNetworkPortsResquest {

	private URIBuilder uriBuilder;
	
	public static final String DEVICE_ID_KEY = "device_id";
	public static final String NETWORK_ID_KEY = "network_id";
	
	public GetNetworkPortsResquest(Builder builder) throws URISyntaxException  {
		this.uriBuilder = new URIBuilder(builder.url);
		this.uriBuilder.addParameter(DEVICE_ID_KEY, builder.deviceId);
		this.uriBuilder.addParameter(NETWORK_ID_KEY, builder.networkId);
	}
	
	public String getUrl() {
		return this.uriBuilder.toString();
	}
	
    public static class Builder {
        
    	private String url;
    	private String deviceId;
    	private String networkId;

        public Builder url(String url) {
            this.url = url;
            return this;
        }
    	
        public Builder deviceId(String deviceId) {
            this.deviceId = deviceId;
            return this;
        }
        
        public Builder networkId(String networkId) {
            this.networkId = networkId;
            return this;
        }        

        public GetNetworkPortsResquest build() throws URISyntaxException  {
            return new GetNetworkPortsResquest(this);
        }
    }	
	
}
