package org.fogbowcloud.ras.util.connectivity;

import org.apache.http.client.HttpClient;
import org.apache.http.client.HttpResponseException;
import org.apache.log4j.Logger;
import org.fogbowcloud.ras.core.exceptions.FatalErrorException;
import org.fogbowcloud.ras.core.exceptions.FogbowRasException;
import org.fogbowcloud.ras.core.exceptions.UnavailableProviderException;
import org.fogbowcloud.ras.core.models.tokens.Token;
import org.fogbowcloud.ras.core.plugins.interoperability.genericrequest.GenericRequestHttpResponse;
import org.json.JSONObject;

import java.util.Map;

public class AuditableHttpRequestClient extends HttpRequestClientUtil {
    private static final Logger LOGGER = Logger.getLogger(AuditableHttpRequestClient.class);

    public AuditableHttpRequestClient() throws FatalErrorException {
        super();
    }

    public AuditableHttpRequestClient(HttpClient httpClient) {
        super(httpClient);
    }

    @Override
    public String doGetRequest(String endpoint, Token token)
            throws UnavailableProviderException, HttpResponseException {
        return super.doGetRequest(endpoint, token);
    }

    @Override
    public String doPostRequest(String endpoint, Token token, String body)
            throws UnavailableProviderException, HttpResponseException {
        return super.doPostRequest(endpoint, token, body);
    }

    @Override
    public void doDeleteRequest(String endpoint, Token token)
            throws UnavailableProviderException, HttpResponseException {
        super.doDeleteRequest(endpoint, token);
    }

    @Override
    public HttpRequestClientUtil.Response doPostRequest(String endpoint, String body)
            throws HttpResponseException, UnavailableProviderException {
        return super.doPostRequest(endpoint, body);
    }

    @Override
    public String doPutRequest(String endpoint, Token token, JSONObject json)
            throws HttpResponseException, UnavailableProviderException {
        return super.doPutRequest(endpoint, token, json);
    }

    @Override
    public GenericRequestHttpResponse doGenericRequest(String method, String urlString, Map<String, String> headers, Map<String, String> body) throws FogbowRasException {
        return super.doGenericRequest(method, urlString, headers, body);
    }
}
