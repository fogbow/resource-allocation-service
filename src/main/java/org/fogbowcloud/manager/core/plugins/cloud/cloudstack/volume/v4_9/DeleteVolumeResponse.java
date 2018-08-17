package org.fogbowcloud.manager.core.plugins.cloud.cloudstack.volume.v4_9;

import static org.fogbowcloud.manager.core.plugins.cloud.cloudstack.CloudStackRestApiConstants.Volume.*;

import org.fogbowcloud.manager.util.GsonHolder;
import com.google.gson.annotations.SerializedName;

/**
 * Documentation: https://cloudstack.apache.org/api/apidocs-4.9/apis/deleteVolume.html
 * 
 * {
 *      "deletevolumeresponse": {
 *          "displaytext": "error description"
 *          "success": "false"
 *      }
 * }
 * 
 * We use the @SerializedName annotation to specify that the request parameter is not equal to the class field.
 */
public class DeleteVolumeResponse {

    @SerializedName(DELETE_VOLUME_RESPONSE_KEY_JSON)
    private VolumeResponse response;
    
    public class VolumeResponse {
        
        @SerializedName(DISPLAY_TEXT_KEY_JSON)
        private String displayText;
        
        @SerializedName(SUCCESS_KEY_JSON)
        private boolean success;
        
    }
    
    public static DeleteVolumeResponse fromJson(String json) {
        return GsonHolder.getInstance().fromJson(json, DeleteVolumeResponse.class);
    }
    
    public String getDisplayText() {
        return response.displayText;
    }
    
    public boolean isSuccess() {
        return response.success;
    }

}
