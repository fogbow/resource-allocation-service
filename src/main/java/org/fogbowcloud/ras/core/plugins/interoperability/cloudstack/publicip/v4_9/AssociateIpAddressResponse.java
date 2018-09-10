package org.fogbowcloud.ras.core.plugins.interoperability.cloudstack.publicip.v4_9;

import static org.fogbowcloud.ras.core.plugins.interoperability.cloudstack.CloudStackRestApiConstants.PublicIp.ID_KEY_JSON;
import static org.fogbowcloud.ras.core.plugins.interoperability.cloudstack.CloudStackRestApiConstants.PublicIp.IP_ADDRESS_KEY_JSON;
import static org.fogbowcloud.ras.core.plugins.interoperability.cloudstack.CloudStackRestApiConstants.PublicIp.QUERY_ASYNC_JOB_RESULT_KEY_JSON;
import static org.fogbowcloud.ras.core.plugins.interoperability.cloudstack.CloudStackRestApiConstants.PublicIp.JOB_RESULT_KEY_JSON;

import org.fogbowcloud.ras.core.plugins.interoperability.cloudstack.CloudStackQueryAsyncJobResponse;
import org.fogbowcloud.ras.util.GsonHolder;

import com.google.gson.annotations.SerializedName;

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
public class AssociateIpAddressResponse extends CloudStackQueryAsyncJobResponse {

    private JobResult jobResult;

	public IpAddress getIpAddress() {
		return this.jobResult.ipAddress;
	}
	
    public static AssociateIpAddressResponse fromJson(String json) {
        return GsonHolder.getInstance().fromJson(json, AssociateIpAddressResponse.class);
    }

    public class JobResult {

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
