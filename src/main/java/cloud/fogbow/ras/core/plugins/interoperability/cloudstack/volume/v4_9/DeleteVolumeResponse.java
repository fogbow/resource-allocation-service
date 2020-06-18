package cloud.fogbow.ras.core.plugins.interoperability.cloudstack.volume.v4_9;

import cloud.fogbow.common.exceptions.FogbowException;
import cloud.fogbow.common.util.GsonHolder;
import cloud.fogbow.ras.core.plugins.interoperability.cloudstack.CloudStackErrorResponse;
import com.google.gson.annotations.SerializedName;

import static cloud.fogbow.common.constants.CloudStackConstants.Volume.*;

/**
 * Documentation: https://cloudstack.apache.org/api/apidocs-4.9/apis/deleteVolume.html
 * <p>
 * {
 *   "deletevolumeresponse": {
 *     "success": "true"
 *   }
 * }
 * <p>
 * or
 * <p>
 * {
 *   "deletevolumeresponse": {
 *     "displaytext": "error description",
 *     "success": "false"
 *   }
 * }
 * <p>
 * We use the @SerializedName annotation to specify that the request parameter is not equal to the class field.
 */
public class DeleteVolumeResponse {
    @SerializedName(DELETE_VOLUME_KEY_JSON)
    private VolumeResponse response;

    public class VolumeResponse extends CloudStackErrorResponse {
        @SerializedName(DISPLAY_TEXT_KEY_JSON)
        private String displayText;
        @SerializedName(SUCCESS_KEY_JSON)
        private boolean success;
    }

    public static DeleteVolumeResponse fromJson(String json) throws FogbowException {
        DeleteVolumeResponse deleteVolumeResponse =
                GsonHolder.getInstance().fromJson(json, DeleteVolumeResponse.class);
        deleteVolumeResponse.response.checkErrorExistence();
        return deleteVolumeResponse;
    }

    public String getDisplayText() {
        return response.displayText;
    }

    public boolean isSuccess() {
        return response.success;
    }
}
