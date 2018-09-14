package org.fogbowcloud.ras.core.plugins.interoperability.cloudstack.compute.v4_9;

import com.google.gson.annotations.SerializedName;
import org.fogbowcloud.ras.util.GsonHolder;

import static org.fogbowcloud.ras.core.plugins.interoperability.cloudstack.CloudStackRestApiConstants.Compute.KEYPAIR_KEY_JSON;
import static org.fogbowcloud.ras.core.plugins.interoperability.cloudstack.CloudStackRestApiConstants.Compute.NAME_KEY_JSON;
import static org.fogbowcloud.ras.core.plugins.interoperability.cloudstack.CloudStackRestApiConstants.Compute.REGISTER_KEYPAIR_KEY_JSON;

/**
 * Documentation: https://cloudstack.apache.org/api/apidocs-4.9/apis/registerSSHKeyPair.html
 * <p>
 * {
 *   "registersshkeypairresponse": {
 *     "keypair": {
 *       "name":"akeypair",
 *       "account":"account@accounts.com",
 *       "domainid":"adomainid",
 *       "domain":"FOGBOW",
 *       "fingerprint":"f7:e4:f5:1e:01:55:ca:0d:4f:2a:49:01:d5:b7:1b:07"
 *     }
 *   }
 * }
 * </p>
 */
public class RegisterSSHKeypairResponse {
    @SerializedName(REGISTER_KEYPAIR_KEY_JSON)
    private RegisterKeypairResponse registerSSHKeypairResponse;

    public class RegisterKeypairResponse {
        @SerializedName(KEYPAIR_KEY_JSON)
        private Keypair keypair;
    }

    public String getKeypairName() {
        return registerSSHKeypairResponse.keypair.name;
    }

    public class Keypair {
        @SerializedName(NAME_KEY_JSON)
        private String name;
    }

    public static RegisterSSHKeypairResponse fromJson(String json) {
        return GsonHolder.getInstance().fromJson(json, RegisterSSHKeypairResponse.class);
    }
}
