package org.fogbowcloud.ras.core.plugins.interoperability.cloudstack.publicip.v4_9;

import static org.fogbowcloud.ras.core.plugins.interoperability.cloudstack.CloudStackRestApiConstants.PublicIp.JOB_STATUS_KEY_JSON;
import static org.fogbowcloud.ras.core.plugins.interoperability.cloudstack.CloudStackRestApiConstants.PublicIp.QUERY_ASYNC_JOB_RESULT_KEY_JSON;

import org.fogbowcloud.ras.util.GsonHolder;

import com.google.gson.annotations.SerializedName;

/**
 * 
 * Documentation: 
 * 
 * Response Example: 
 * {
 *  "queryasyncjobresultresponse":{
 *    "jobstatus":1,
 *  }
 * }
 *
 */
public class QueryAsyncJobResultResponse {

	@SerializedName(QUERY_ASYNC_JOB_RESULT_KEY_JSON)
	private QueryAsyncJobResult queryAsyncJobResult;
	
	public QueryAsyncJobResult getQueryAsyncJobResult() {
		return queryAsyncJobResult;
	}
	
    public static QueryAsyncJobResult fromJson(String json) {
        return GsonHolder.getInstance().fromJson(json, QueryAsyncJobResult.class);
    }

    public class QueryAsyncJobResult {
        @SerializedName(JOB_STATUS_KEY_JSON)
        private String jobStatus;

        public String getJobStatus() {
			return jobStatus;
		}
    }
	
}
