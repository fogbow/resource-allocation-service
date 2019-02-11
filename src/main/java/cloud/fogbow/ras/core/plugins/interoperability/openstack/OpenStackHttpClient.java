package cloud.fogbow.ras.core.plugins.interoperability.openstack;

import cloud.fogbow.common.exceptions.FatalErrorException;
import cloud.fogbow.common.models.CloudToken;
import cloud.fogbow.common.util.connectivity.HttpRequestUtil;
import cloud.fogbow.ras.constants.Messages;
import cloud.fogbow.ras.core.plugins.interoperability.genericrequest.GenericRequest;
import cloud.fogbow.ras.util.connectivity.CloudHttpClient;
import org.apache.http.client.HttpClient;

import java.util.Map;

public class OpenStackHttpClient extends CloudHttpClient {
    public OpenStackHttpClient(Integer timeout) throws FatalErrorException {
        super(timeout);
    }

    public OpenStackHttpClient(HttpClient httpClient) {
        super(httpClient);
    }

    @Override
    public GenericRequest includeTokenInRequest(GenericRequest genericRequest, CloudToken token) {
        GenericRequest clonedRequest = (GenericRequest) genericRequest.clone();
        Map<String, String> headers = clonedRequest.getHeaders();

        if (headers.containsKey(HttpRequestUtil.X_AUTH_TOKEN_KEY)) {
            throw new IllegalArgumentException(Messages.Exception.TOKEN_ALREADY_SPECIFIED);
        }
        headers.put(HttpRequestUtil.X_AUTH_TOKEN_KEY, token.getTokenValue());

        return clonedRequest;
    }
}
