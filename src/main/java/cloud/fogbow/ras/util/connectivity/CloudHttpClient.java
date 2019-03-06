package cloud.fogbow.ras.util.connectivity;

import cloud.fogbow.common.constants.HttpMethod;
import cloud.fogbow.common.exceptions.FogbowException;
import cloud.fogbow.common.models.CloudUser;
import cloud.fogbow.common.util.GsonHolder;
import cloud.fogbow.common.util.connectivity.HttpResponse;
import cloud.fogbow.common.util.connectivity.HttpRequestClientUtil;
import cloud.fogbow.ras.core.plugins.interoperability.genericrequest.HttpFogbowGenericRequest;
import org.apache.http.HttpStatus;
import org.apache.http.client.HttpResponseException;

import java.util.HashMap;

public abstract class CloudHttpClient<T extends CloudUser> {
    public static final String EMPTY_BODY = "{}";

    public CloudHttpClient() {
    }

    public String doGetRequest(String url, T cloudUser) throws FogbowException, HttpResponseException {
        return callDoGenericRequest(HttpMethod.GET, url, EMPTY_BODY, cloudUser);
    }

    public void doDeleteRequest(String url, T cloudUser) throws FogbowException, HttpResponseException {
        callDoGenericRequest(HttpMethod.DELETE, url, EMPTY_BODY, cloudUser);
    }

    public String doPostRequest(String url, String bodyContent, T cloudUser)
            throws FogbowException, HttpResponseException {
        return callDoGenericRequest(HttpMethod.POST, url, bodyContent, cloudUser);
    }

    private String callDoGenericRequest(HttpMethod method, String url, String bodyContent, T cloudUser)
            throws FogbowException, HttpResponseException {
        HashMap<String, String> body = GsonHolder.getInstance().fromJson(bodyContent, HashMap.class);

        HashMap<String, String> headers = new HashMap<>();
        HttpResponse response = doGenericRequest(method, url, headers, body, cloudUser);

        if (response.getHttpCode() > HttpStatus.SC_NO_CONTENT) {
            throw new HttpResponseException(response.getHttpCode(), response.getContent());
        }

        return response.getContent();
    }

    public HttpResponse doGenericRequest(HttpMethod method, String url, HashMap<String, String> headers,
                                         HashMap<String, String> body , T cloudUser) throws FogbowException {
        HttpFogbowGenericRequest request = new HttpFogbowGenericRequest(method, url, body, headers);
        HttpFogbowGenericRequest preparedRequest = prepareRequest(request, cloudUser);

        return HttpRequestClientUtil.doGenericRequest(preparedRequest.getMethod(),
                preparedRequest.getUrl(), preparedRequest.getHeaders(), preparedRequest.getBody());
    }

    public abstract HttpFogbowGenericRequest prepareRequest(HttpFogbowGenericRequest genericRequest, T cloudUser);
}
