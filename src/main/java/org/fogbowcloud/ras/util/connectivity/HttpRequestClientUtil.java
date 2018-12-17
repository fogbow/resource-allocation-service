package org.fogbowcloud.ras.util.connectivity;

import org.apache.http.Header;
import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.HttpResponseException;
import org.apache.http.client.methods.HttpDelete;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.methods.HttpPut;
import org.apache.http.entity.StringEntity;
import org.apache.http.util.EntityUtils;
import org.apache.log4j.Logger;
import org.fogbowcloud.ras.core.constants.Messages;
import org.fogbowcloud.ras.core.exceptions.FatalErrorException;
import org.fogbowcloud.ras.core.exceptions.FogbowRasException;
import org.fogbowcloud.ras.core.exceptions.UnavailableProviderException;
import org.fogbowcloud.ras.core.models.tokens.Token;
import org.fogbowcloud.ras.core.plugins.interoperability.genericrequest.GenericRequestHttpResponse;
import org.fogbowcloud.ras.util.GsonHolder;
import org.json.JSONObject;
import org.springframework.http.HttpStatus;

import javax.servlet.http.HttpServletRequest;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.ProtocolException;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Map;

public class HttpRequestClientUtil {
    private static final Logger LOGGER = Logger.getLogger(HttpRequestClientUtil.class);

    private HttpClient client;

    public HttpRequestClientUtil() throws FatalErrorException {
        this.client = HttpRequestUtil.createHttpClient();
    }

    public HttpRequestClientUtil(HttpClient httpClient) {
        this.client = httpClient;
    }

    public String doGetRequest(String endpoint, Token token)
            throws UnavailableProviderException, HttpResponseException {
        HttpGet request = new HttpGet(endpoint);
        request.addHeader(HttpRequestUtil.CONTENT_TYPE_KEY, HttpRequestUtil.JSON_CONTENT_TYPE_KEY);
        request.addHeader(HttpRequestUtil.ACCEPT_KEY, HttpRequestUtil.JSON_CONTENT_TYPE_KEY);
        request.addHeader(HttpRequestUtil.X_AUTH_TOKEN_KEY, token.getTokenValue());

        String response = null;
        HttpResponse httpResponse = null;

        try {
            LOGGER.debug(String.format("making GET request on <%s> with token <%s>", endpoint, token));
            httpResponse = this.client.execute(request);
            if (httpResponse.getStatusLine().getStatusCode() > HttpStatus.NO_CONTENT.value()) {
                String message = httpResponse.getStatusLine().getReasonPhrase();
                throw new HttpResponseException(httpResponse.getStatusLine().getStatusCode(), message);
            }
            response = EntityUtils.toString(httpResponse.getEntity(), StandardCharsets.UTF_8);
        } catch (HttpResponseException e) {
            LOGGER.debug(String.format("error was <%s>", e.toString()));
            throw e;
        } catch (IOException e) {
            throw new UnavailableProviderException(e.getMessage(), e);
        } finally {
            try {
                EntityUtils.consume(httpResponse.getEntity());
                LOGGER.debug(String.format("response was: <%s>", response));
            } catch (Throwable t) {
                LOGGER.error(String.format(Messages.Error.ERROR_WHILE_CONSUMING_RESPONSE, t));
            }
        }
        return response;
    }

    public String doPostRequest(String endpoint, Token token, String body)
            throws UnavailableProviderException, HttpResponseException {
        HttpPost request = new HttpPost(endpoint);
        request.addHeader(HttpRequestUtil.CONTENT_TYPE_KEY, HttpRequestUtil.JSON_CONTENT_TYPE_KEY);
        request.addHeader(HttpRequestUtil.ACCEPT_KEY, HttpRequestUtil.JSON_CONTENT_TYPE_KEY);
        request.addHeader(HttpRequestUtil.X_AUTH_TOKEN_KEY, token.getTokenValue());
        request.setEntity(new StringEntity(body, StandardCharsets.UTF_8));

        String responseStr;
        HttpResponse response = null;

        try {
            LOGGER.debug(String.format("making GET request on <%s> with token <%s>", endpoint, token));
            LOGGER.debug(String.format("the body of the request is <%s>", body));
            response = this.client.execute(request);
            if (response.getStatusLine().getStatusCode() > HttpStatus.NO_CONTENT.value()) {
                String message = response.getStatusLine().getReasonPhrase();
                throw new HttpResponseException(response.getStatusLine().getStatusCode(), message);
            }
            responseStr = EntityUtils.toString(response.getEntity(), StandardCharsets.UTF_8);
        } catch (HttpResponseException e) {
            LOGGER.debug(String.format("error was: <%s>", e.toString()));
            throw e;
        } catch (IOException e) {
            throw new UnavailableProviderException(e.getMessage(), e);
        } finally {
            try {
                EntityUtils.consume(response.getEntity());
                LOGGER.debug(String.format("response was: <%s>", response));
            } catch (Throwable t) {
                LOGGER.error(String.format(Messages.Error.ERROR_WHILE_CONSUMING_RESPONSE, t));
            }
        }
        return responseStr;
    }

    public void doDeleteRequest(String endpoint, Token token)
            throws UnavailableProviderException, HttpResponseException {
        HttpDelete request = new HttpDelete(endpoint);
        request.addHeader(HttpRequestUtil.X_AUTH_TOKEN_KEY, token.getTokenValue());

        HttpResponse response = null;

        try {
            LOGGER.debug(String.format("making DELETE request on <%s> with token <%s>", endpoint, token));
            response = this.client.execute(request);
            if (response.getStatusLine().getStatusCode() > HttpStatus.NO_CONTENT.value()) {
                String message = response.getStatusLine().getReasonPhrase();
                throw new HttpResponseException(response.getStatusLine().getStatusCode(), message);
            }
        } catch (HttpResponseException e) {
            LOGGER.debug(String.format("error was: <%s>", e.toString()));
            throw e;
        } catch (IOException e) {
            throw new UnavailableProviderException(e.getMessage(), e);
        } finally {
            try {
                EntityUtils.consume(response.getEntity());
                LOGGER.debug(String.format("response was: <%s>", response));
            } catch (Throwable t) {
                LOGGER.error(String.format(Messages.Error.ERROR_WHILE_CONSUMING_RESPONSE, t));
            }
        }
    }

    public Response doPostRequest(String endpoint, String body)
            throws HttpResponseException, UnavailableProviderException {
        HttpPost request = new HttpPost(endpoint);
        request.addHeader(HttpRequestUtil.CONTENT_TYPE_KEY, HttpRequestUtil.JSON_CONTENT_TYPE_KEY);
        request.addHeader(HttpRequestUtil.ACCEPT_KEY, HttpRequestUtil.JSON_CONTENT_TYPE_KEY);
        request.setEntity(new StringEntity(body.toString(), StandardCharsets.UTF_8));

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
                LOGGER.error(String.format(Messages.Error.ERROR_WHILE_CONSUMING_RESPONSE, t));
            }
        }
        return new Response(responseStr, response.getAllHeaders());
    }

    public String doPutRequest(String endpoint, Token token, JSONObject json)
            throws HttpResponseException, UnavailableProviderException {
        HttpPut request = new HttpPut(endpoint);
        request.addHeader(HttpRequestUtil.CONTENT_TYPE_KEY, HttpRequestUtil.JSON_CONTENT_TYPE_KEY);
        request.addHeader(HttpRequestUtil.ACCEPT_KEY, HttpRequestUtil.JSON_CONTENT_TYPE_KEY);
        request.addHeader(HttpRequestUtil.X_AUTH_TOKEN_KEY, token.getTokenValue());
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
                LOGGER.error(String.format(Messages.Error.ERROR_WHILE_CONSUMING_RESPONSE, t));
            }
        }
        return responseStr;
    }

    public GenericRequestHttpResponse doGenericRequest(String method, String urlString, Map<String, String> headers, Map<String, String> body) throws FogbowRasException {
        try {
            URL url = new URL(urlString);
            HttpURLConnection connection = (HttpURLConnection) url.openConnection();
            connection.setRequestMethod(method.toUpperCase());

            addHeadersIntoConnection(connection, headers);

            if (!body.isEmpty()) {
                connection.setDoOutput(true);
                OutputStream os = connection.getOutputStream();
                os.write(toByteArray(body));
                os.flush();
                os.close();
            }

            int responseCode = connection.getResponseCode();

            BufferedReader in = new BufferedReader(new InputStreamReader(
                    connection.getInputStream()));

            StringBuffer responseBuffer = new StringBuffer();
            String inputLine;
            while ((inputLine = in.readLine()) != null) {
                responseBuffer.append(inputLine);
            }
            in.close();

            return new GenericRequestHttpResponse(responseBuffer.toString(), responseCode);
        } catch (ProtocolException e) {
            throw new FogbowRasException("", e);
        } catch (MalformedURLException e) {
            throw new FogbowRasException("", e);
        } catch (IOException e) {
            throw new FogbowRasException("", e);
        }
    }

    private void addHeadersIntoConnection(HttpURLConnection connection, Map<String, String> headers) {
        for (String key : headers.keySet()) {
            connection.setRequestProperty(key, headers.get(key));
        }
    }

    public static Map<String, String> getHeaders(HttpServletRequest request) {
        Enumeration<String> headerNames = request.getHeaderNames();

        Map<String, String> headers = new HashMap<>();
        while (headerNames.hasMoreElements()) {
            String headerName = headerNames.nextElement();
            headers.put(headerName, request.getHeader(headerName));
        }

        return headers;
    }

    private byte[] toByteArray(Map<String, String> body) {
        String json = GsonHolder.getInstance().toJson(body, Map.class);
        return json.getBytes();
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
