package cloud.fogbow.ras.core.plugins.interoperability.cloudstack.sdk.v4_9.publicip.model;

import cloud.fogbow.common.util.GsonHolder;

/**
 * Documentation : https://cloudstack.apache.org/api/apidocs-4.9/apis/enableStaticNat.html
 * <p>
 * Request Example:
 */
public class EnableStaticNatResponse {

    public static EnableStaticNatResponse fromJson(String json) {
        return GsonHolder.getInstance().fromJson(json, EnableStaticNatResponse.class);
    }

}
