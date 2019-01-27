package cloud.fogbow.ras.core.plugins.interoperability.cloudstack.publicip.v4_9;

import cloud.fogbow.ras.core.plugins.interoperability.cloudstack.CloudStackRequest;

import java.security.InvalidParameterException;

import static cloud.fogbow.ras.core.plugins.interoperability.cloudstack.CloudStackRestApiConstants.PublicIp.JOB_ID_KEY_JSON;
import static cloud.fogbow.ras.core.plugins.interoperability.cloudstack.CloudStackRestApiConstants.PublicIp.QUERY_ASYNC_JOB_RESULT;

public class QueryAsyncJobResultRequest extends CloudStackRequest {

	protected QueryAsyncJobResultRequest(Builder builder) throws InvalidParameterException {
		addParameter(JOB_ID_KEY_JSON, builder.jobId);
	}

    @Override
    public String toString() {
        return super.toString();
    }
	
	@Override
	public String getCommand() {
		return QUERY_ASYNC_JOB_RESULT;
	}

    public static class Builder {
        private String jobId;

        public Builder jobId(String jobId) {
            this.jobId = jobId;
            return this;
        }

        public QueryAsyncJobResultRequest build() throws InvalidParameterException {
            return new QueryAsyncJobResultRequest(this);
        }
    }
	
}
