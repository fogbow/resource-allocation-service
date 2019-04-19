package cloud.fogbow.ras.core.plugins.interoperability.opennebula.genericrequest.v5_4;

import static cloud.fogbow.common.constants.OpenNebulaConstants.ONE_GENERIC_REQUEST;
import static cloud.fogbow.common.constants.OpenNebulaConstants.ONE_METHOD;
import static cloud.fogbow.common.constants.OpenNebulaConstants.ONE_PARAMETERS;
import static cloud.fogbow.common.constants.OpenNebulaConstants.ONE_RESOURCE;
import static cloud.fogbow.common.constants.OpenNebulaConstants.RESOURCE_ID;
import static cloud.fogbow.common.constants.OpenNebulaConstants.URL;

import java.util.Map;

import com.google.gson.annotations.SerializedName;

import cloud.fogbow.common.util.GsonHolder;

/**
 * Request Example:
 * 	{
 * 		"oneGenericRequest":{
 *     		"url":"http://10.11.5.20:2633/RPC2",
 *     		"oneResource":"VirtualMachine",
 *     		"oneMethod":"poweroff",
 *     		"resourceId":"125",
 *     		"oneParameters":{
 *     			"hard":"true"
 *     		}
 *  	}
 *	}
 */
public class CreateOneGenericRequest {
	
	@SerializedName(ONE_GENERIC_REQUEST)
    private OneGenericRequest request;
	
	public OneGenericRequest getRequest() {
		return request;
	}

	public String getUrl() {
        return request.url;
    }
	
	public String getResource() {
        return request.resource;
    }
	
	public String getMethod() {
        return request.method;
    }
	
	public String getResourceId() {
        return request.resourceId;
    }
	
	public Map<String, String> getParameters() {
        return request.parameters;
    }
	
	public static CreateOneGenericRequest fromJson(String json) {
        return GsonHolder.getInstance().fromJson(json, CreateOneGenericRequest.class);
    }
	
	public class OneGenericRequest {
		
		@SerializedName(URL)
		private String url;
		
		@SerializedName(ONE_RESOURCE)
		private String resource;
		
		@SerializedName(ONE_METHOD)
		private String method;
		
		@SerializedName(RESOURCE_ID)
		private String resourceId;
		
		@SerializedName(ONE_PARAMETERS)
		private Map<String, String> parameters;
		
	}
}
