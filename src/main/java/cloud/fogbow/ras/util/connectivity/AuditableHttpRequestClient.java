package cloud.fogbow.ras.util.connectivity;

import cloud.fogbow.common.util.GsonHolder;
import cloud.fogbow.common.util.connectivity.HttpRequestClientUtil;
import cloud.fogbow.ras.core.datastore.DatabaseManager;
import cloud.fogbow.ras.core.models.auditing.AuditableRequest;
import org.apache.log4j.Logger;

import java.net.HttpURLConnection;
import java.sql.Timestamp;
import java.util.Map;

// FIXME Remove this when auditing is finished
public class AuditableHttpRequestClient extends HttpRequestClientUtil {
    private static final Logger LOGGER = Logger.getLogger(AuditableHttpRequestClient.class);

//    @Override
//    public String doGetRequest(String endpoint, CloudToken token) throws UnavailableProviderException, HttpResponseException {
//        int responseCode = HttpStatus.SC_OK;
//        try {
//            return super.doGetRequest(endpoint, token);
//        } catch (HttpResponseException e) {
//            responseCode = e.getStatusCode();
//            throw e;
//        } catch (UnavailableProviderException e) {
//            responseCode = HttpStatus.SC_GATEWAY_TIMEOUT;
//            throw e;
//        } finally {
//            auditRequest(endpoint, token.getUserId(), token.getTokenProviderId(), responseCode);
//        }
//    }
//
//    @Override
//    public String doPostRequest(String url, String bodyContent, CloudToken token)
//            throws UnavailableProviderException, HttpResponseException {
//        int responseCode = HttpStatus.SC_OK;
//        try {
//            return super.doPostRequest(url, bodyContent, token);
//        } catch (HttpResponseException e) {
//            responseCode = e.getStatusCode();
//            throw e;
//        } catch (UnavailableProviderException e) {
//            responseCode = HttpStatus.SC_GATEWAY_TIMEOUT;
//            throw e;
//        } finally {
//            auditRequest(url, token.getUserId(), token.getTokenProviderId(), responseCode);
//        }
//    }
//
//    @Override
//    public void doDeleteRequest(String endpoint, CloudToken token)
//            throws UnavailableProviderException, HttpResponseException {
//        int responseCode = HttpStatus.SC_OK;
//        try {
//            super.doDeleteRequest(endpoint, token);
//        } catch (HttpResponseException e) {
//            responseCode = e.getStatusCode();
//            throw e;
//        } catch (UnavailableProviderException e) {
//            responseCode = HttpStatus.SC_GATEWAY_TIMEOUT;
//            throw e;
//        } finally {
//            auditRequest(endpoint, token.getUserId(), token.getTokenProviderId(), responseCode);
//        }
//    }
//
//    @Override
//    public HttpRequestClientUtil.Response doPostRequest(String endpoint, String body)
//            throws UnavailableProviderException, HttpResponseException {
//        int responseCode = HttpStatus.SC_OK;
//        try {
//            return super.doPostRequest(endpoint, body);
//        } catch (HttpResponseException e) {
//            responseCode = e.getStatusCode();
//            throw e;
//        } catch (UnavailableProviderException e) {
//            responseCode = HttpStatus.SC_GATEWAY_TIMEOUT;
//            throw e;
//        } finally {
//            auditRequest(endpoint, null, null, responseCode);
//        }
//    }
//
//    @Override
//    public String doPutRequest(String endpoint, CloudToken token, JSONObject json)
//            throws UnavailableProviderException, HttpResponseException {
//        int responseCode = HttpStatus.SC_OK;
//        try {
//            return super.doPutRequest(endpoint, token, json);
//        } catch (HttpResponseException e) {
//            responseCode = e.getStatusCode();
//            throw e;
//        } catch (UnavailableProviderException e) {
//            responseCode = HttpStatus.SC_GATEWAY_TIMEOUT;
//            throw e;
//        } finally {
//            auditRequest(endpoint, token.getUserId(), token.getTokenProviderId(), responseCode);
//        }
//    }

//    public GenericRequestHttpResponse doGenericRequest(String method, String urlString,
//                                                       Map<String, String> headers, Map<String, String> body, CloudToken token)
//            throws FogbowException {
//        try {
//            URL url = new URL(urlString);
//            HttpURLConnection connection = (HttpURLConnection) url.openConnection();
//            connection.setRequestMethod(method.toUpperCase());
//
//            addHeadersIntoConnection(connection, headers);
//
//            if (!body.isEmpty()) {
//                connection.setDoOutput(true);
//                OutputStream os = connection.getOutputStream();
//                os.write(toByteArray(body));
//                os.flush();
//                os.close();
//            }
//
//            int responseCode = connection.getResponseCode();
//
//            BufferedReader in = new BufferedReader(new InputStreamReader(
//                    connection.getInputStream()));
//
//            StringBuffer responseBuffer = new StringBuffer();
//            String inputLine;
//            while ((inputLine = in.readLine()) != null) {
//                responseBuffer.append(inputLine);
//            }
//            in.close();
//
//            GenericRequestHttpResponse response = new GenericRequestHttpResponse(responseBuffer.toString(), responseCode, headers);
//            auditRequest(urlString, token.getUserId(), token.getTokenProviderId(), response.getHttpCode());
//            return response;
//        } catch (ProtocolException e) {
//            throw new FogbowException("", e);
//        } catch (MalformedURLException e) {
//            throw new FogbowException("", e);
//        } catch (IOException e) {
//            throw new FogbowException("", e);
//        }
//    }

    private void addHeadersIntoConnection(HttpURLConnection connection, Map<String, String> headers) {
        for (String key : headers.keySet()) {
            connection.setRequestProperty(key, headers.get(key));
        }
    }

    private byte[] toByteArray(Map<String, String> body) {
        String json = GsonHolder.getInstance().toJson(body, Map.class);
        return json.getBytes();
    }
}
