package cloud.fogbow.ras.core.plugins.interoperability.cloudstack.attachment.v4_9;

import cloud.fogbow.common.exceptions.InternalServerErrorException;
import cloud.fogbow.common.util.GsonHolder;
import cloud.fogbow.ras.constants.Messages;
import cloud.fogbow.ras.core.plugins.interoperability.cloudstack.CloudStackCloudUtils;
import cloud.fogbow.ras.core.plugins.interoperability.cloudstack.CloudStackErrorResponse;
import com.google.gson.annotations.SerializedName;
import org.apache.log4j.Logger;

import javax.validation.constraints.NotNull;

import static cloud.fogbow.common.constants.CloudStackConstants.Attachment.*;

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
    private static final Logger LOGGER = Logger.getLogger(AttachmentJobStatusResponse.class);
    protected static final String NO_FAILURE_EXCEPTION_MESSAGE = "There isn't failure";

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

    /**
     * It returns a CloudStackErrorResponse when the job status response is a failure and it throws
     * an InternalServerErrorException when the job status response is not a failure due to the fact that
     * either complete or failure response comes by the same parameters, the jobResult.
     */
    @NotNull
    public CloudStackErrorResponse getErrorResponse() throws InternalServerErrorException {
        if (response.jobStatus == CloudStackCloudUtils.JOB_STATUS_FAILURE) {
            return response.jobResult;
        }
        LOGGER.debug(String.format(
                "Error code: %s, jobResult: %s", response.jobStatus, response.jobResult));
        throw new InternalServerErrorException(
                String.format(Messages.Exception.UNEXPECTED_OPERATION_S, NO_FAILURE_EXCEPTION_MESSAGE));
    }

    @NotNull
    public Volume getVolume() {
        return this.response.jobResult.volume;
    }

    /**
     * It returns an AttachmentJobStatusResponse.
     * It doesn't check the error existence because is in async operation context. In this case,
     * who will handle the error is the method that will call it.
     */
    @NotNull
    public static AttachmentJobStatusResponse fromJson(String json) {
        return GsonHolder.getInstance().fromJson(json, AttachmentJobStatusResponse.class);
    }
    
    public class JobResult extends CloudStackErrorResponse {
        
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
