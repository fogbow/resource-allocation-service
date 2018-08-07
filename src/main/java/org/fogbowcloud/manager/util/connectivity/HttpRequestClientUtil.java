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
import org.fogbowcloud.manager.core.models.tokens.LocalUserAttributes;
import org.json.JSONObject;
import org.springframework.http.HttpStatus;

public class HttpRequestClientUtil {

	private static final Logger LOGGER = Logger.getLogger(HttpRequestClientUtil.class);
	private HttpClient client;

	public HttpRequestClientUtil() throws FatalErrorException {
        HttpRequestUtil.init();
        this.client = HttpRequestUtil.createHttpClient();
	}
	
	public HttpRequestClientUtil(HttpClient httpClient) {
		this.client = httpClient;
	}

    public String doGetRequest(String endpoint, LocalUserAttributes localUserAttributes)
            throws UnavailableProviderException, HttpResponseException {
        LOGGER.debug("Doing GET request to endpoint <" + endpoint + ">");
        HttpGet request = new HttpGet(endpoint);
        request.addHeader(HttpRequestUtil.CONTENT_TYPE_KEY, HttpRequestUtil.JSON_CONTENT_TYPE_KEY);
        request.addHeader(HttpRequestUtil.ACCEPT_KEY, HttpRequestUtil.JSON_CONTENT_TYPE_KEY);
        request.addHeader(HttpRequestUtil.X_AUTH_TOKEN_KEY, localUserAttributes.getTokenValue());

        String response;
        HttpResponse httpResponse = null;

        try {
            httpResponse = this.client.execute(request);
            if (httpResponse.getStatusLine().getStatusCode() > HttpStatus.NO_CONTENT.value()) {
                String message = httpResponse.getStatusLine().getReasonPhrase();
                throw new HttpResponseException(httpResponse.getStatusLine().getStatusCode(), message);
            }
            response = EntityUtils.toString(httpResponse.getEntity(), StandardCharsets.UTF_8);
        } catch (HttpResponseException e) {
            throw e;
        } catch (IOException e) {
            throw new UnavailableProviderException(e.getMessage(), e);
        } finally {
            try {
                EntityUtils.consume(httpResponse.getEntity());
            } catch (Throwable t) {
                LOGGER.error("Error while consuming the response: " + t);
            }
        }
        return response;
    }
    
    public String doPostRequest(String endpoint, LocalUserAttributes localUserAttributes, String body)
            throws UnavailableProviderException, HttpResponseException {
        LOGGER.debug("Doing POST request to endpoint <" + endpoint + ">");
        HttpPost request = new HttpPost(endpoint);
        request.addHeader(HttpRequestUtil.CONTENT_TYPE_KEY, HttpRequestUtil.JSON_CONTENT_TYPE_KEY);
        request.addHeader(HttpRequestUtil.ACCEPT_KEY, HttpRequestUtil.JSON_CONTENT_TYPE_KEY);
        request.addHeader(HttpRequestUtil.X_AUTH_TOKEN_KEY, localUserAttributes.getTokenValue());
        request.setEntity(new StringEntity(body, StandardCharsets.UTF_8));

        String responseStr;
        HttpResponse response = null;

        try {
            response = this.client.execute(request);            
            if (response.getStatusLine().getStatusCode() > HttpStatus.NO_CONTENT.value()) {
                String message = response.getStatusLine().getReasonPhrase();
                throw new HttpResponseException(response.getStatusLine().getStatusCode(), message); 
            }            
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

    public void doDeleteRequest(String endpoint, LocalUserAttributes localUserAttributes)
            throws UnavailableProviderException, HttpResponseException {
        LOGGER.debug("Doing DELETE request to endpoint <" + endpoint + ">");
        HttpDelete request = new HttpDelete(endpoint);
        request.addHeader(HttpRequestUtil.X_AUTH_TOKEN_KEY, localUserAttributes.getTokenValue());

        HttpResponse response = null;

        try {
            response = this.client.execute(request);
            if (response.getStatusLine().getStatusCode() > HttpStatus.NO_CONTENT.value()) {
                String message = response.getStatusLine().getReasonPhrase();
                throw new HttpResponseException(response.getStatusLine().getStatusCode(), message); 
            }            
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
	}

    public Response doPostRequest(String endpoint, JSONObject json)
            throws HttpResponseException, UnavailableProviderException {
        HttpPost request = new HttpPost(endpoint);
        request.addHeader(HttpRequestUtil.CONTENT_TYPE_KEY, HttpRequestUtil.JSON_CONTENT_TYPE_KEY);
        request.addHeader(HttpRequestUtil.ACCEPT_KEY, HttpRequestUtil.JSON_CONTENT_TYPE_KEY);
        request.setEntity(new StringEntity(json.toString(), StandardCharsets.UTF_8));

        HttpResponse response = null;
        String responseStr = null;

        try {
            response = this.client.execute(request);
            if (response.getStatusLine().getStatusCode() > HttpStatus.NO_CONTENT.value()) {
                String message = response.getStatusLine().getReasonPhrase();
                throw new HttpResponseException(response.getStatusLine().getStatusCode(), message); 
            }    
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
        return new Response(responseStr, response.getAllHeaders());
    }

    @SuppressWarnings("unused")
    public String doPutRequest(String endpoint, LocalUserAttributes localUserAttributes, JSONObject json)
            throws HttpResponseException, UnavailableProviderException {
        HttpPut request = new HttpPut(endpoint);
        request.addHeader(HttpRequestUtil.CONTENT_TYPE_KEY, HttpRequestUtil.JSON_CONTENT_TYPE_KEY);
        request.addHeader(HttpRequestUtil.ACCEPT_KEY, HttpRequestUtil.JSON_CONTENT_TYPE_KEY);
        request.addHeader(HttpRequestUtil.X_AUTH_TOKEN_KEY, localUserAttributes.getTokenValue());
        request.setEntity(new StringEntity(json.toString(), StandardCharsets.UTF_8));

        String responseStr;
        HttpResponse response = null;

        try {
            response = this.client.execute(request);
            if (response.getStatusLine().getStatusCode() > HttpStatus.NO_CONTENT.value()) {
                String message = response.getStatusLine().getReasonPhrase();
                throw new HttpResponseException(response.getStatusLine().getStatusCode(), message); 
            }         
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
