package org.fogbowcloud.ras.core.plugins.interoperability.cloudstack.volume.v4_9;

import com.google.gson.annotations.SerializedName;
import org.fogbowcloud.ras.util.GsonHolder;

import static org.fogbowcloud.ras.core.plugins.interoperability.cloudstack.CloudStackRestApiConstants.Volume.*;

/**
 * Documentation: https://cloudstack.apache.org/api/apidocs-4.9/apis/deleteVolume.html
 * <p>
 * {
 * "deletevolumeresponse": {
 * "success": "true"
 * }
 * }
 * 
 * or
 * 
 * {
 * "deletevolumeresponse": {
 * "displaytext": "error description",
 * "success": "false"
 * }
 * }
 * <p>
 * We use the @SerializedName annotation to specify that the request parameter is not equal to the class field.
 */
public class DeleteVolumeResponse {
    @SerializedName(DELETE_VOLUME_KEY_JSON)
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
