package org.fogbowcloud.ras.core.plugins.interoperability.cloudstack.attachment.v4_9;

import com.google.gson.annotations.SerializedName;
import org.fogbowcloud.ras.util.GsonHolder;

import static org.fogbowcloud.ras.core.plugins.interoperability.cloudstack.CloudStackRestApiConstants.Attachment.*;

/**
 * Documentation: https://cloudstack.apache.org/api/apidocs-4.9/apis/queryAsyncJobResult.html
 * <p>
 * {
 *  "queryasyncjobresultresponse": {
 *      "jobstatus": 1,
 *      "jobresult": {
 *          "volume": {
 *              "id": "0fd664ce-4acf-4b86-8e16-a43886b6996e",
 *              "deviceid": 2,
 *              "virtualmachineid": "4f9e6f31-2ee7-494a-914f-01fc1432e59a",
 *              "state": "Ready",
 *              "jobid": "a0e403db-0342-45f1-b9e0-8ff94cc1652f"
 *          }
 *      }
 *   }
 * } 
 * <p>
 * We use the @SerializedName annotation to specify that the request parameter is not equal to the class field.
 */
public class AttachmentJobStatusResponse {

    @SerializedName(QUERY_ASYNC_JOB_RESULT_KEY_JSON)
    private JobResultResponse response;
    
    public class JobResultResponse {
        
        // jobStatus: 0 PENDING, 1 COMPLETE, 2 FAILURE
        @SerializedName(JOB_STATUS_KEY_JSON)
        private int jobStatus;
        @SerializedName(JOB_RESULT_KEY_JSON)
        private JobResult jobResult;
    }
    
    public int getJobStatus() {
        return response.jobStatus;
    }
    
    public Volume getVolume() {
        return this.response.jobResult.volume;
    }
    
    public static AttachmentJobStatusResponse fromJson(String json) {
        return GsonHolder.getInstance().fromJson(json, AttachmentJobStatusResponse.class);
    }
    
    public class JobResult {
        
        @SerializedName(VOLUME_KEY_JSON)
        private Volume volume;
    }
    
    public class Volume {
        
        @SerializedName(ID_KEY_JSON)
        private String id;
        @SerializedName(DEVICE_ID_KEY_JSON)
        private int deviceId;
        @SerializedName(VIRTUAL_MACHINE_ID_KEY_JSON)
        private String virtualMachineId;
        @SerializedName(STATE_KEY_JSON)
        private String state;
        @SerializedName(JOB_ID_KEY_JSON)
        private String jobId;
        
        public String getId() {
            return this.id;
        }
        
        public int getDeviceId() {
            return this.deviceId;
        }
        
        public String getVirtualMachineId() {
            return this.virtualMachineId;
        }
        
        public String getState() {
            return this.state;
        }
        
        public String getJobId() {
            return this.jobId;
        }
    }
    
}
