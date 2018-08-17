package org.fogbowcloud.manager.core.plugins.cloud.cloudstack.volume.v4_9;

import static org.fogbowcloud.manager.core.plugins.cloud.cloudstack.CloudStackRestApiConstants.Volume.*;

import org.fogbowcloud.manager.util.GsonHolder;
import com.google.gson.annotations.SerializedName;

/**
 * Documentation: https://cloudstack.apache.org/api/apidocs-4.9/apis/createVolume.html
 * 
 * Response example:
 * {
 *      "createvolumeresponse": {
 *          "id": "a97fdd39-31d8-4f6f-bae6-28837b51c5af",
 *          "jobid": "b56c510d-3476-495e-a1ef-e8fe3f9d2923"
 *      }
 * }
 * 
 * We use the @SerializedName annotation to specify that the request parameter is not equal to the class field.
 */
public class CreateVolumeResponse {

    @SerializedName(CREATE_VOLUME_RESPONSE_KEY_JSON)
    private VolumeResponse response;
    
    public class VolumeResponse {
        
        @SerializedName(ID_KEY_JSON)
        private String id;
        
        @SerializedName(JOB_ID_KEY_JSON)
        private String jobId;
        
    }
    
    public static CreateVolumeResponse fromJson(String json) {
        return GsonHolder.getInstance().fromJson(json, CreateVolumeResponse.class);
    }
    
    public String getId() {
        return response.id;
    }
    
    public String getJobId() {
        return response.jobId;
    }
    
}
