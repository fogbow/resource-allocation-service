package cloud.fogbow.ras.core.plugins.interoperability.openstack;

import cloud.fogbow.common.constants.HttpConstants;
import cloud.fogbow.common.constants.HttpMethod;
import cloud.fogbow.common.constants.OpenStackConstants;
import cloud.fogbow.common.models.OpenStackV3User;
import cloud.fogbow.ras.core.plugins.interoperability.genericrequest.HttpFogbowGenericRequest;
import cloud.fogbow.ras.util.connectivity.CloudHttpClient;

import java.util.Map;

public class OpenStackHttpClient extends CloudHttpClient<OpenStackV3User> {

    public OpenStackHttpClient() {
    }

    @Override
    public HttpFogbowGenericRequest prepareRequest(HttpFogbowGenericRequest genericRequest, OpenStackV3User cloudUser) {
        HttpFogbowGenericRequest clonedRequest = (HttpFogbowGenericRequest) genericRequest.clone();
        Map<String, String> headers = clonedRequest.getHeaders();

        headers.put(OpenStackConstants.X_AUTH_TOKEN_KEY, cloudUser.getToken());

        if (genericRequest.getMethod().equals(HttpMethod.GET)
                || genericRequest.getMethod().equals(HttpMethod.POST)) {
            headers.put(HttpConstants.CONTENT_TYPE_KEY, HttpConstants.JSON_CONTENT_TYPE_KEY);
            headers.put(HttpConstants.ACCEPT_KEY, HttpConstants.JSON_CONTENT_TYPE_KEY);
        }
        return clonedRequest;
    }
}
