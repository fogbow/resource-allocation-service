package cloud.fogbow.ras.util.connectivity;

import cloud.fogbow.common.exceptions.FogbowException;
import cloud.fogbow.common.models.CloudToken;
import cloud.fogbow.common.util.GsonHolder;
import cloud.fogbow.common.util.connectivity.GenericRequestHttpResponse;
import cloud.fogbow.common.util.connectivity.HttpRequestClientUtil;
import cloud.fogbow.ras.core.plugins.interoperability.genericrequest.GenericRequest;
import org.apache.http.client.HttpResponseException;
import org.springframework.http.HttpStatus;

import java.util.HashMap;

public abstract class CloudHttpClient {

    protected HttpRequestClientUtil client;

    public static final String EMPTY_BODY = "{}";

    public CloudHttpClient(HttpRequestClientUtil client) {
        this.client = client;
    }

    public String doGetRequest(String url, CloudToken token) throws FogbowException, HttpResponseException {
        return callDoGenericRequest("GET", url, EMPTY_BODY, token);
    }

    public void doDeleteRequest(String url, CloudToken token) throws FogbowException, HttpResponseException {
        callDoGenericRequest("DELETE", url, EMPTY_BODY, token);
    }

    public String doPostRequest(String url, String bodyContent, CloudToken token)
            throws FogbowException, HttpResponseException {
        return callDoGenericRequest("POST", url, bodyContent, token);
    }

    public String doPutRequest(String url, String bodyContent, CloudToken token) throws FogbowException, HttpResponseException {
        return callDoGenericRequest("PUT", url, bodyContent, token);
    }

    private String callDoGenericRequest(String method, String url, String bodyContent, CloudToken token) throws FogbowException, HttpResponseException {
        HashMap<String, String> body = GsonHolder.getInstance().fromJson(bodyContent, HashMap.class);

        HashMap<String, String> headers = new HashMap<>();
        GenericRequestHttpResponse response = doGenericRequest(method, url, headers, body, token);

        if (response.getHttpCode() > HttpStatus.NO_CONTENT.value()) {
            throw new HttpResponseException(response.getHttpCode(), response.getContent());
        }

        return response.getContent();
    }

    public GenericRequestHttpResponse doGenericRequest(String method, String url, HashMap<String, String> headers,
                                                       HashMap<String, String> body , CloudToken token) throws FogbowException {
        GenericRequest request = new GenericRequest(method, url, body, headers);
        GenericRequest preparedRequest = prepareRequest(request, token);

        return client.doGenericRequest(preparedRequest.getMethod(),
                preparedRequest.getUrl(), preparedRequest.getHeaders(), preparedRequest.getBody());
    }

    public abstract GenericRequest prepareRequest(GenericRequest genericRequest, CloudToken token);
}
