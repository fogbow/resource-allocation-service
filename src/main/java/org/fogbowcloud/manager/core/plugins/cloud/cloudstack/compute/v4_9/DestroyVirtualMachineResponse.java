package org.fogbowcloud.manager.core.plugins.cloud.cloudstack.compute.v4_9;

import com.google.gson.annotations.SerializedName;
import org.fogbowcloud.manager.util.GsonHolder;

import static org.fogbowcloud.manager.core.plugins.cloud.cloudstack.CloudStackRestApiConstants.Compute.DESTROY_VIRTUAL_MACHINE_KEY_JSON;
import static org.fogbowcloud.manager.core.plugins.cloud.cloudstack.CloudStackRestApiConstants.Volume.*;

/**
 * Documentation: https://cloudstack.apache.org/api/apidocs-4.9/apis/destroyVirtualMachine.html
 * 
 * {
 *      "destroyvirtualmachineresponse": {
 *          "displaytext": "error description",
 *          "success": "false"
 *      }
 * }
 * 
 * We use the @SerializedName annotation to specify that the request parameter is not equal to the class field.
 */
public class DestroyVirtualMachineResponse {

    @SerializedName(DESTROY_VIRTUAL_MACHINE_KEY_JSON)
    private DestroyResponse response;
    
    public class DestroyResponse {
        
        @SerializedName(DISPLAY_TEXT_KEY_JSON)
        private String displayText;
        
        @SerializedName(SUCCESS_KEY_JSON)
        private boolean success;
        
    }
    
    public static DestroyVirtualMachineResponse fromJson(String json) {
        return GsonHolder.getInstance().fromJson(json, DestroyVirtualMachineResponse.class);
    }
    
    public String getDisplayText() {
        return response.displayText;
    }
    
    public boolean isSuccess() {
        return response.success;
    }

}
