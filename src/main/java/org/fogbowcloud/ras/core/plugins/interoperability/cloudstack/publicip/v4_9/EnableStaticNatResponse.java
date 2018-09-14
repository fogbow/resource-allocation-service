package org.fogbowcloud.ras.core.plugins.interoperability.cloudstack.publicip.v4_9;

import org.fogbowcloud.ras.util.GsonHolder;

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
