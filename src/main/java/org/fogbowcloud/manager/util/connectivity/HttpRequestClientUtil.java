package org.fogbowcloud.manager.util.connectivity;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

import org.apache.http.Header;
import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.HttpResponseException;
import org.apache.http.client.methods.*;
import org.apache.http.entity.StringEntity;
import org.apache.http.util.EntityUtils;
import org.apache.log4j.Logger;
import org.fogbowcloud.manager.core.exceptions.*;
import org.fogbowcloud.manager.core.models.tokens.Token;
import org.json.JSONObject;

public class HttpRequestClientUtil {

	private static final Logger LOGGER = Logger.getLogger(HttpRequestClientUtil.class);
	private HttpClient client;

	public HttpRequestClientUtil() throws FatalErrorException {
        HttpRequestUtil.init();
        this.client = HttpRequestUtil.createHttpClient();
	}

    public String doGetRequest(String endpoint, Token localToken)
            throws UnavailableProviderException, HttpResponseException {
        LOGGER.debug("Doing GET request to endpoint <" + endpoint + ">");
        HttpGet request = new HttpGet(endpoint);
        request.addHeader(HttpRequestUtil.CONTENT_TYPE_KEY, HttpRequestUtil.JSON_CONTENT_TYPE_KEY);
        request.addHeader(HttpRequestUtil.ACCEPT_KEY, HttpRequestUtil.JSON_CONTENT_TYPE_KEY);
        request.addHeader(HttpRequestUtil.X_AUTH_TOKEN_KEY, localToken.getAccessId());
        return doRequestToString(request);
    }

    public String doPostRequest(String endpoint, Token localToken, JSONObject json)
            throws UnavailableProviderException, HttpResponseException {
        LOGGER.debug("Doing POST request to endpoint <" + endpoint + ">");
        HttpPost request = new HttpPost(endpoint);
        request.addHeader(HttpRequestUtil.CONTENT_TYPE_KEY, HttpRequestUtil.JSON_CONTENT_TYPE_KEY);
        request.addHeader(HttpRequestUtil.ACCEPT_KEY, HttpRequestUtil.JSON_CONTENT_TYPE_KEY);
        request.addHeader(HttpRequestUtil.X_AUTH_TOKEN_KEY, localToken.getAccessId());
        request.setEntity(new StringEntity(json.toString(), StandardCharsets.UTF_8));
        return doRequestToString(request);
    }

    public void doDeleteRequest(String endpoint, Token localToken)
            throws UnavailableProviderException, HttpResponseException {
        LOGGER.debug("Doing DELETE request to endpoint <" + endpoint + ">");
        HttpDelete request = new HttpDelete(endpoint);
        request.addHeader(HttpRequestUtil.X_AUTH_TOKEN_KEY, localToken.getAccessId());
        doRequestToString(request);
   }

    public Response doPostRequest(String endpoint, JSONObject json)
            throws HttpResponseException, UnavailableProviderException {
        HttpPost request = new HttpPost(endpoint);
        request.addHeader(HttpRequestUtil.CONTENT_TYPE_KEY, HttpRequestUtil.JSON_CONTENT_TYPE_KEY);
        request.addHeader(HttpRequestUtil.ACCEPT_KEY, HttpRequestUtil.JSON_CONTENT_TYPE_KEY);
        request.setEntity(new StringEntity(json.toString(), StandardCharsets.UTF_8));
        HttpResponse response = doRequestToResponse(request);
        String responseStr = null;
        try {
            responseStr = EntityUtils.toString(response.getEntity(), StandardCharsets.UTF_8);
        } catch (IOException e) {
            throw new UnavailableProviderException(e.getMessage(), e);
        }
        return new Response(responseStr, response.getAllHeaders());
    }

    public String doPutRequest(String endpoint, Token localToken, JSONObject json)
            throws HttpResponseException, UnavailableProviderException {
        HttpPut request = new HttpPut(endpoint);
        request.addHeader(HttpRequestUtil.CONTENT_TYPE_KEY, HttpRequestUtil.JSON_CONTENT_TYPE_KEY);
        request.addHeader(HttpRequestUtil.ACCEPT_KEY, HttpRequestUtil.JSON_CONTENT_TYPE_KEY);
        request.addHeader(HttpRequestUtil.X_AUTH_TOKEN_KEY, localToken.getAccessId());
        request.setEntity(new StringEntity(json.toString(), StandardCharsets.UTF_8));
        return doRequestToString(request);
    }

    protected String doRequestToString(HttpRequestBase request)
            throws HttpResponseException, UnavailableProviderException {
        String responseStr;
        HttpResponse response = null;

        try {
            response = this.client.execute(request);
            responseStr = EntityUtils.toString(response.getEntity(), StandardCharsets.UTF_8);
        } catch (HttpResponseException e) {
            throw e;
        } catch (IOException e) {
            throw new UnavailableProviderException(e.getMessage(), e);
        } finally {
            try {
                EntityUtils.consume(response.getEntity());
            } catch (Throwable t) {
                LOGGER.error("Error while consuming the response: " + t);
            }
        }
        return responseStr;
    }

    protected HttpResponse doRequestToResponse(HttpRequestBase request)
            throws HttpResponseException, UnavailableProviderException {
        HttpResponse response = null;

	    try {
            response = this.client.execute(request);
        } catch (HttpResponseException e) {
            throw e;
        } catch (IOException e) {
            throw new UnavailableProviderException(e.getMessage(), e);
        } finally {
            try {
                EntityUtils.consume(response.getEntity());
            } catch (Throwable t) {
                LOGGER.error("Error while consuming the response: " + t);
            }
        }
        return response;
    }

    public class Response {

        private String content;
        private Header[] headers;

        public Response(String content, Header[] headers) {
            this.content = content;
            this.headers = headers;
        }

        public String getContent() {
            return content;
        }

        public Header[] getHeaders() {
            return headers;
        }
    }
}
