package cloud.fogbow.ras.core.plugins.interoperability.opennebula.genericrequest.v5_4;

import java.util.Map;

import com.google.gson.annotations.SerializedName;

import cloud.fogbow.common.util.GsonHolder;
import cloud.fogbow.common.util.connectivity.FogbowGenericRequest;

/**
 * Request Example:
 * 	{
 * 		"oneGenericRequest":{
 *     		"url":"http://10.11.5.20:2633/RPC2",
 *     		"oneResource":"VirtualMachine",
 *     		"oneMethod":"powerOff",
 *     		"resourceId":"125",
 *     		"oneParameters":{
 *     			"hard":"true"
 *     		}
 *  	}
 *	}
 */
public class OneFogbowGenericRequest implements FogbowGenericRequest {
	
	@SerializedName("oneGenericRequest")
    private OneGenericRequest genericRequest;
	
	public String getUrl() {
        return genericRequest.url;
    }
	
	public String getResource() {
        return genericRequest.resource;
    }
	
	public String getMethod() {
        return genericRequest.method;
    }
	
	public String getResourceId() {
        return genericRequest.resourceId;
    }
	
	public Map<String, String> getParameters() {
        return genericRequest.parameters;
    }
	
	public static OneFogbowGenericRequest fromJson(String json) {
        return GsonHolder.getInstance().fromJson(json, OneFogbowGenericRequest.class);
    }
	
	public class OneGenericRequest {
		
		@SerializedName("url")
		private String url;
		
		@SerializedName("oneResource")
		private String resource;
		
		@SerializedName("oneMethod")
		private String method;
		
		@SerializedName("resourceId")
		private String resourceId;
		
		@SerializedName("oneParameters")
		private Map<String, String> parameters;
		
	}
}
