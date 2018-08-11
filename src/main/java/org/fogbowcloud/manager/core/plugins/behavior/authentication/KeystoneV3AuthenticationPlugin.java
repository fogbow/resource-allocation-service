package org.fogbowcloud.manager.core.plugins.behavior.authentication;

import org.apache.http.client.HttpResponseException;
import org.fogbowcloud.manager.core.plugins.cloud.openstack.OpenStackHttpToFogbowManagerExceptionMapper;

public class KeystoneV3AuthenticationPlugin implements AuthenticationPlugin {

    // TODO: implement this method.
    @Override
    public boolean isAuthentic(String federationTokenValue) {
//        String endpoint = this.properties.getProperty(COMPUTE_NOVAV2_URL_KEY)
//                + COMPUTE_V2_API_ENDPOINT + SUFFIX;
//
//        String jsonResponse = null;
//
//        try {
//            jsonResponse = this.client.doGetRequest(endpoint, token);
//        } catch (HttpResponseException e) {
//            return false;
//        }
        return true;
    }
}
