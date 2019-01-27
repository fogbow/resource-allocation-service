package cloud.fogbow.ras.core.plugins.interoperability.cloudstack.attachment.v4_9;

import cloud.fogbow.common.exceptions.InvalidParameterException;
import cloud.fogbow.ras.core.plugins.interoperability.cloudstack.CloudStackRequest;

public class AttachmentJobStatusRequest extends CloudStackRequest {

    protected static final String QUERY_ASYNC_JOB_RESULT_COMMAND = "queryAsyncJobResult";
    protected static final String JOB_ID = "jobid";
    
    protected AttachmentJobStatusRequest(Builder builder) throws InvalidParameterException {
        super(builder.cloudStackUrl);
        addParameter(JOB_ID, builder.jobId);
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
        
        public AttachmentJobStatusRequest build(String cloudStackUrl) throws InvalidParameterException {
            this.cloudStackUrl = cloudStackUrl;
            return new AttachmentJobStatusRequest(this);
        }
        
    }
    
}
