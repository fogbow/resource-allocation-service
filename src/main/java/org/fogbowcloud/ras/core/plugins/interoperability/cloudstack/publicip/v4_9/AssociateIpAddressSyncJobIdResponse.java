package org.fogbowcloud.ras.core.plugins.interoperability.cloudstack.publicip.v4_9;

import static org.fogbowcloud.ras.core.plugins.interoperability.cloudstack.CloudStackRestApiConstants.PublicIp.ASSOCIATE_IP_ADDRESS_RESPONSE_KEY_JSON;
import static org.fogbowcloud.ras.core.plugins.interoperability.cloudstack.CloudStackRestApiConstants.PublicIp.JOB_ID_KEY_JSON;

import org.fogbowcloud.ras.util.GsonHolder;

import com.google.gson.annotations.SerializedName;

/**
 * 
 * Documentation: 
 * 
 * Response Example: 
 * 
 * {
 *  "associateipaddressresponse":{
 *    "jobid":"7568bb4f-d925-437e-80b0-b2d984d225d4"
 *  }
 * }
 *
 */
public class AssociateIpAddressSyncJobIdResponse implements SyncJobIdResponse {

	@SerializedName(ASSOCIATE_IP_ADDRESS_RESPONSE_KEY_JSON)
	private Associateipaddressresponse associateipaddressresponse;
	
	@Override
	public String getJobId() {
		return this.associateipaddressresponse.getJobId();
	}
		
    public static AssociateIpAddressSyncJobIdResponse fromJson(String json) {
        return GsonHolder.getInstance().fromJson(json, AssociateIpAddressSyncJobIdResponse.class);
    }
    
    public class Associateipaddressresponse {
    	@SerializedName(JOB_ID_KEY_JSON)
    	private String jobId;
    	
    	public String getJobId() {
			return jobId;
		}
    }

	
}
