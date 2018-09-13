package org.fogbowcloud.ras.core.plugins.interoperability.cloudstack;

import com.google.gson.annotations.SerializedName;
import org.fogbowcloud.ras.util.GsonHolder;

import static org.fogbowcloud.ras.core.plugins.interoperability.cloudstack.CloudStackRestApiConstants.PublicIp.JOB_STATUS_KEY_JSON;
import static org.fogbowcloud.ras.core.plugins.interoperability.cloudstack.CloudStackRestApiConstants.PublicIp.QUERY_ASYNC_JOB_RESULT_KEY_JSON;

/**
 * Documentation: https://cloudstack.apache.org/api/apidocs-4.9/apis/queryAsyncJobResult.html
 *
 * Response example:
 * {
 *   "queryasyncjobresultresponse": {
 *     "jobstatus": 1,
 *     "jobresult": {
 *       "volume": {
 *         "id": "0fd664ce-4acf-4b86-8e16-a43886b6996e",
 *         "deviceid": 2,
 *         "virtualmachineid": "4f9e6f31-2ee7-494a-914f-01fc1432e59a",
 *         "state": "Ready",
 *         "jobid": "a0e403db-0342-45f1-b9e0-8ff94cc1652f"
 *       }
 *     }
 *   }
 * }
 */
public class CloudStackQueryAsyncJobResponse {

    @SerializedName(QUERY_ASYNC_JOB_RESULT_KEY_JSON)
    private QueryAsyncJobResultResponse response;

    public static CloudStackQueryAsyncJobResponse fromJson(String json) {
        return GsonHolder.getInstance().fromJson(json, CloudStackQueryAsyncJobResponse.class);
    }

    private class QueryAsyncJobResultResponse {

        @SerializedName(JOB_STATUS_KEY_JSON)
        private int jobStatus;

    }

    public int getJobStatus() {
        return response.jobStatus;
    }

}
