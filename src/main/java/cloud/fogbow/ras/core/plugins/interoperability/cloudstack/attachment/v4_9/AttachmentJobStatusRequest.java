package cloud.fogbow.ras.core.plugins.interoperability.cloudstack.attachment.v4_9;

import cloud.fogbow.common.exceptions.InternalServerErrorException;
import cloud.fogbow.common.util.connectivity.cloud.cloudstack.CloudStackRequest;

import static cloud.fogbow.common.constants.CloudStackConstants.Attachment.JOB_ID_KEY_JSON;
import static cloud.fogbow.common.constants.CloudStackConstants.Attachment.QUERY_ASYNC_JOB_RESULT_COMMAND;

public class AttachmentJobStatusRequest extends CloudStackRequest {

    private AttachmentJobStatusRequest(Builder builder) throws InternalServerErrorException {
        super(builder.cloudStackUrl);
        addParameter(JOB_ID_KEY_JSON, builder.jobId);
    }
    
    @Override
    public String getCommand() {
        return QUERY_ASYNC_JOB_RESULT_COMMAND;
    }
    
    @Override
    public String toString() {
        return super.toString();
    }
    
    public static class Builder {
        private String cloudStackUrl;
        private String jobId;

        public Builder jobId(String jobId) {
            this.jobId = jobId;
            return this;
        }
        
        public AttachmentJobStatusRequest build(String cloudStackUrl) throws InternalServerErrorException {
            this.cloudStackUrl = cloudStackUrl;
            return new AttachmentJobStatusRequest(this);
        }
        
    }
    
}
