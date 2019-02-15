package cloud.fogbow.ras.core.plugins.interoperability.openstack;

import cloud.fogbow.common.models.CloudToken;
import cloud.fogbow.common.util.connectivity.HttpRequestClientUtil;
import cloud.fogbow.common.util.connectivity.HttpRequestUtil;
import cloud.fogbow.ras.core.plugins.interoperability.genericrequest.GenericRequest;
import cloud.fogbow.ras.util.connectivity.CloudHttpClient;
import org.springframework.http.HttpMethod;

import java.util.Map;

public class OpenStackHttpClient extends CloudHttpClient {

    public OpenStackHttpClient(HttpRequestClientUtil client) {
        super(client);
    }

    @Override
    public GenericRequest prepareRequest(GenericRequest genericRequest, CloudToken token) {
        GenericRequest clonedRequest = (GenericRequest) genericRequest.clone();
        Map<String, String> headers = clonedRequest.getHeaders();

        headers.put(HttpRequestUtil.X_AUTH_TOKEN_KEY, token.getTokenValue());

        if (genericRequest.getMethod().equals(HttpMethod.GET)
                || genericRequest.getMethod().equals(HttpMethod.POST)) {
            headers.put(HttpRequestUtil.CONTENT_TYPE_KEY, HttpRequestUtil.JSON_CONTENT_TYPE_KEY);
            headers.put(HttpRequestUtil.ACCEPT_KEY, HttpRequestUtil.JSON_CONTENT_TYPE_KEY);
        }
        return clonedRequest;
    }
}
