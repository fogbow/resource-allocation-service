package org.fogbowcloud.ras.core.plugins.interoperability.cloudstack.attachment.v4_9;

import static org.fogbowcloud.ras.core.plugins.interoperability.cloudstack.CloudStackRestApiConstants.Attachment.*;
import org.fogbowcloud.ras.util.GsonHolder;
import com.google.gson.annotations.SerializedName;

/**
 * Documentation: https://cloudstack.apache.org/api/apidocs-4.9/apis/attachVolume.html
 * <p>
 * Response example:
 * {
 *  "detachvolumeresponse":{
 *      "jobid":"2d8de965-947b-4681-8e20-965b86da5fa1"
 *      }
 * }
 * <p>
 * We use the @SerializedName annotation to specify that the request parameter is not equal to the class field.
 */
public class DetachVolumeResponse {

    @SerializedName(DETACH_VOLUME_KEY_JSON)
    private AttachResponse response;
    
    public class AttachResponse {
        
        @SerializedName(JOB_ID_KEY_JSON)
        private String jobId;
        
    }

    public static DetachVolumeResponse fromJson(String json) {
        return GsonHolder.getInstance().fromJson(json, DetachVolumeResponse.class);
    }

    public String getJobId() {
        return response.jobId;
    }

}
