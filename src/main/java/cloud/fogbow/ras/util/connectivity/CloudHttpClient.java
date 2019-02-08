package cloud.fogbow.ras.util.connectivity;

import cloud.fogbow.common.exceptions.FatalErrorException;
import cloud.fogbow.common.exceptions.FogbowException;
import cloud.fogbow.common.models.CloudToken;
import cloud.fogbow.common.util.connectivity.GenericRequestHttpResponse;
import cloud.fogbow.common.util.connectivity.HttpRequestClientUtil;
import cloud.fogbow.ras.core.plugins.interoperability.genericrequest.GenericRequest;
import org.apache.http.client.HttpClient;

import java.util.Map;

public abstract class CloudHttpClient extends HttpRequestClientUtil {
    public CloudHttpClient(Integer timeout) throws FatalErrorException {
        super(timeout);
    }

    public CloudHttpClient(HttpClient httpClient) {
        super(httpClient);
    }

    @Override
    public GenericRequestHttpResponse doGenericRequest(String method, String url, Map<String, String> headers,
                                                       Map<String, String> body, CloudToken token) throws FogbowException {
        GenericRequest request = new GenericRequest(method, url, headers, body);
        includeTokenInRequest(request, token);

        return super.doGenericRequest(method, url, headers, body, token);
    }

    public abstract GenericRequest includeTokenInRequest(GenericRequest genericRequest, CloudToken token);
}
