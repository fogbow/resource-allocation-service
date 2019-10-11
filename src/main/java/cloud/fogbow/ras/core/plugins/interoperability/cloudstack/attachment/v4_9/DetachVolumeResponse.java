package cloud.fogbow.ras.core.plugins.interoperability.cloudstack.attachment.v4_9;

import cloud.fogbow.common.util.GsonHolder;
import cloud.fogbow.ras.core.plugins.interoperability.cloudstack.CloudStackErrorResponse;
import com.google.gson.annotations.SerializedName;
import org.apache.http.client.HttpResponseException;

import javax.validation.constraints.NotNull;

import static cloud.fogbow.common.constants.CloudStackConstants.Attachment.DETACH_VOLUME_KEY_JSON;
import static cloud.fogbow.common.constants.CloudStackConstants.Attachment.JOB_ID_KEY_JSON;

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
    
    public class AttachResponse extends CloudStackErrorResponse {

        @NotNull
        @SerializedName(JOB_ID_KEY_JSON)
        private String jobId;
        
    }

    @NotNull
    public static DetachVolumeResponse fromJson(String json) throws HttpResponseException {
        DetachVolumeResponse detachVolumeResponse = GsonHolder.getInstance()
                .fromJson(json, DetachVolumeResponse.class);
        detachVolumeResponse.response.checkErrorExistence();
        return detachVolumeResponse;
    }

    public String getJobId() {
        return response.jobId;
    }

}
