package org.fogbowcloud.ras.core.plugins.interoperability.cloudstack.compute.v4_9;

import com.google.gson.annotations.SerializedName;
import org.fogbowcloud.ras.util.GsonHolder;

import static org.fogbowcloud.ras.core.plugins.interoperability.cloudstack.CloudStackRestApiConstants.Compute.DELETE_KEYPAIR_KEY_JSON;
import static org.fogbowcloud.ras.core.plugins.interoperability.cloudstack.CloudStackRestApiConstants.Compute.SUCCESS_KEY_JSON;

/**
 * Documentation:
 * <p>
 * {
 *   "deletesshkeypairresponse": {
 *     "success":"true"
 *   }
 * }
 * </p>
 */
public class DeleteSSHKeypairResponse {
    @SerializedName(DELETE_KEYPAIR_KEY_JSON)
    private DeleteKeypairResponse deleteKeypairResponse;

    public class DeleteKeypairResponse {
        @SerializedName(SUCCESS_KEY_JSON)
        public String success;
    }

    public String getSuccess() {
        return deleteKeypairResponse.success;
    }

    public static DeleteSSHKeypairResponse fromJson(String json) {
        return GsonHolder.getInstance().fromJson(json, DeleteSSHKeypairResponse.class);
    }
}
