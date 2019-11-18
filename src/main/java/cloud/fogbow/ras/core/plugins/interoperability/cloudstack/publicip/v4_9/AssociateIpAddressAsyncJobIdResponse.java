package cloud.fogbow.ras.core.plugins.interoperability.cloudstack.publicip.v4_9;

import cloud.fogbow.common.util.GsonHolder;
import cloud.fogbow.ras.core.plugins.interoperability.cloudstack.CloudStackErrorResponse;
import com.google.gson.annotations.SerializedName;
import org.apache.http.client.HttpResponseException;

import static cloud.fogbow.common.constants.CloudStackConstants.PublicIp.ASSOCIATE_IP_ADDRESS_RESPONSE_KEY_JSON;
import static cloud.fogbow.common.constants.CloudStackConstants.PublicIp.JOB_ID_KEY_JSON;

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
    private AssociateIpAddressResponse response;

    public String getJobId() {
        return this.response.getJobId();
    }

    public static AssociateIpAddressAsyncJobIdResponse fromJson(String json) throws HttpResponseException {
        AssociateIpAddressAsyncJobIdResponse associateIpAddressAsyncJobIdResponse =
                GsonHolder.getInstance().fromJson(json, AssociateIpAddressAsyncJobIdResponse.class);
        associateIpAddressAsyncJobIdResponse.response.checkErrorExistence();
        return associateIpAddressAsyncJobIdResponse;
    }

    public class AssociateIpAddressResponse extends CloudStackErrorResponse {

        @SerializedName(JOB_ID_KEY_JSON)
        private String jobId;

        public String getJobId() {
            return jobId;
        }

    }


}
