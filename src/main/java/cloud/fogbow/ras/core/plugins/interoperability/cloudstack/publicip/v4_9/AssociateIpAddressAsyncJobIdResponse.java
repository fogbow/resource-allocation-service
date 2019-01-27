package cloud.fogbow.ras.core.plugins.interoperability.cloudstack.publicip.v4_9;

import cloud.fogbow.common.util.GsonHolder;
import com.google.gson.annotations.SerializedName;

import static cloud.fogbow.ras.core.plugins.interoperability.cloudstack.CloudStackRestApiConstants.PublicIp.ASSOCIATE_IP_ADDRESS_RESPONSE_KEY_JSON;
import static cloud.fogbow.ras.core.plugins.interoperability.cloudstack.CloudStackRestApiConstants.PublicIp.JOB_ID_KEY_JSON;

/**
 * Documentation:
 *
 * Response Example:
 * {
 *   "associateipaddressresponse":{
 *     "jobid":"7568bb4f-d925-437e-80b0-b2d984d225d4"
 *   }
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
