package cloud.fogbow.ras.core.plugins.interoperability.cloudstack.volume.v4_9;

import cloud.fogbow.common.util.GsonHolder;
import cloud.fogbow.ras.core.plugins.interoperability.cloudstack.CloudStackErrorResponse;
import com.google.gson.annotations.SerializedName;
import org.apache.http.client.HttpResponseException;

import javax.validation.constraints.NotNull;

import static cloud.fogbow.common.constants.CloudStackConstants.Volume.*;

/**
 * Documentation: https://cloudstack.apache.org/api/apidocs-4.9/apis/createVolume.html
 * <p>
 * Response example:
 * {
 *   "createvolumeresponse": {
 *     "id": "a97fdd39-31d8-4f6f-bae6-28837b51c5af"
 *   }
 * }
 * <p>
 * We use the @SerializedName annotation to specify that the request parameter is not equal to the class field.
 */
public class CreateVolumeResponse {
    @SerializedName(CREATE_VOLUME_KEY_JSON)
    private VolumeResponse response;

    public class VolumeResponse extends CloudStackErrorResponse {
        @SerializedName(ID_KEY_JSON)
        private String id;
    }

    public static CreateVolumeResponse fromJson(String json) throws HttpResponseException {
        CreateVolumeResponse createVolumeResponse =
                GsonHolder.getInstance().fromJson(json, CreateVolumeResponse.class);
        createVolumeResponse.response.checkErrorExistence();
        return createVolumeResponse;
    }

    public String getId() {
        return response.id;
    }
}
