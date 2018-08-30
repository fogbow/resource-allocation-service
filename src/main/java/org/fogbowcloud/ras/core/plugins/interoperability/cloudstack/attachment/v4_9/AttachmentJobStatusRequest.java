package org.fogbowcloud.ras.core.plugins.interoperability.cloudstack.attachment.v4_9;

import org.fogbowcloud.ras.core.exceptions.InvalidParameterException;
import org.fogbowcloud.ras.core.plugins.interoperability.cloudstack.CloudStackRequest;

public class AttachmentJobStatusRequest extends CloudStackRequest {

    protected static final String QUERY_ASYNC_JOB_RESULT_COMMAND = "queryAsyncJobResult";
    protected static final String JOB_ID = "jobid";
    
    protected AttachmentJobStatusRequest(Builder builder) throws InvalidParameterException {
        super();
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
        private String jobId;

        public Builder jobId(String jobId) {
            this.jobId = jobId;
            return this;
        }
        
        public AttachmentJobStatusRequest build() throws InvalidParameterException {
            return new AttachmentJobStatusRequest(this);
        }
        
    }
    
}
