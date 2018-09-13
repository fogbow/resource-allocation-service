package org.fogbowcloud.ras.core.plugins.interoperability.cloudstack.publicip.v4_9;

import com.google.gson.annotations.SerializedName;
import org.fogbowcloud.ras.util.GsonHolder;

import static org.fogbowcloud.ras.core.plugins.interoperability.cloudstack.CloudStackRestApiConstants.PublicIp.ASSOCIATE_IP_ADDRESS_RESPONSE_KEY_JSON;
import static org.fogbowcloud.ras.core.plugins.interoperability.cloudstack.CloudStackRestApiConstants.PublicIp.JOB_ID_KEY_JSON;

/**
 * Documentation:
 * <p>
 * Response Example:
 * <p>
 * {
 * "associateipaddressresponse":{
 * "jobid":"7568bb4f-d925-437e-80b0-b2d984d225d4"
 * }
 * }
 */
public class AssociateIpAddressAsyncJobIdResponse {

    @SerializedName(ASSOCIATE_IP_ADDRESS_RESPONSE_KEY_JSON)
    private AssociateIpAddressResponse associateIpAddressResponse;

    public String getJobId() {
        return this.associateIpAddressResponse.getJobId();
    }

    public static AssociateIpAddressAsyncJobIdResponse fromJson(String json) {
        return GsonHolder.getInstance().fromJson(json, AssociateIpAddressAsyncJobIdResponse.class);
    }

    public class AssociateIpAddressResponse {

        @SerializedName(JOB_ID_KEY_JSON)
        private String jobId;

        public String getJobId() {
            return jobId;
        }

    }


}
