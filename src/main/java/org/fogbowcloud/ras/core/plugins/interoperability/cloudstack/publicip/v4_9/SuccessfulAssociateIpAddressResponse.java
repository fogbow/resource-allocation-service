package org.fogbowcloud.ras.core.plugins.interoperability.cloudstack.publicip.v4_9;

import org.fogbowcloud.ras.util.GsonHolder;

import com.google.gson.annotations.SerializedName;

import static org.fogbowcloud.ras.core.plugins.interoperability.cloudstack.CloudStackRestApiConstants.PublicIp.*;

/**
 * Documentation : https://cloudstack.apache.org/api/apidocs-4.9/apis/associateIpAddress.html
 * 
 * Response Example: 
 *
 *{
 *  "queryasyncjobresultresponse":{
 *     "jobresult":{
 *       "ipaddress":{
 *          "id":"a0a228c2-842a-4f19-8667-e44c356bf70f",
 *          "ipaddress":"200.133.39.26"
 *       }
 *     }
 *   }
 * }
 *
 */
public class SuccessfulAssociateIpAddressResponse {

    @SerializedName(QUERY_ASYNC_JOB_RESULT_KEY_JSON)
    private QueryAsyncJobResultResponse response;

	public IpAddress getIpAddress() {
		return this.response.jobResult.ipAddress;
	}
	
    public static SuccessfulAssociateIpAddressResponse fromJson(String json) {
        return GsonHolder.getInstance().fromJson(json, SuccessfulAssociateIpAddressResponse.class);
    }

    private class QueryAsyncJobResultResponse {

        @SerializedName(JOB_STATUS_KEY_JSON)
        private int jobStatus;

        @SerializedName(JOB_RESULT_KEY_JSON)
        private JobResult jobResult;

    }

    private class JobResult {

        @SerializedName(IP_ADDRESS_KEY_JSON)
        private IpAddress ipAddress;

    }

    public class IpAddress {

        @SerializedName(ID_KEY_JSON)
        private String id;

        @SerializedName(IP_ADDRESS_KEY_JSON)
        private String ip;

        public String getId() {
            return id;
        }
        
        public String getIpAddress() {
			return ip;
		}
    }
	
}
