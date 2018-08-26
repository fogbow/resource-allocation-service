package org.fogbowcloud.ras.util.connectivity;

import org.apache.http.client.config.RequestConfig;
import org.apache.http.conn.HttpClientConnectionManager;
import org.apache.http.conn.ssl.SSLConnectionSocketFactory;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.log4j.Logger;
import org.fogbowcloud.ras.core.PropertiesHolder;
import org.fogbowcloud.ras.core.constants.ConfigurationConstants;
import org.fogbowcloud.ras.core.constants.DefaultConfigurationConstants;
import org.fogbowcloud.ras.core.exceptions.FatalErrorException;

public class HttpRequestUtil {
    private static final Logger LOGGER = Logger.getLogger(HttpRequestUtil.class);

    public static final String CONTENT_TYPE_KEY = "Content-Type";
    public static final String ACCEPT_KEY = "Accept";
    public static final String JSON_CONTENT_TYPE_KEY = "application/json";
    public static final String X_AUTH_TOKEN_KEY = "X-Auth-Token";
    private static Integer timeoutHttpRequest;

    public static void init() throws FatalErrorException {
        try {
            String timeoutRequestStr =
                    PropertiesHolder.getInstance().getProperty(ConfigurationConstants.HTTP_REQUEST_TIMEOUT);
            timeoutHttpRequest = Integer.parseInt(timeoutRequestStr);
        } catch (NullPointerException | NumberFormatException e) {
            timeoutHttpRequest = Integer.valueOf(DefaultConfigurationConstants.HTTP_REQUEST_TIMEOUT);
        } catch (Exception e) {
            throw new FatalErrorException("It is not possible to initialize HttpRequestUtil.", e);
        }
    }

    public static CloseableHttpClient createHttpClient() throws FatalErrorException {
        return createHttpClient(null, null, null);
    }

    public static CloseableHttpClient createHttpClient(SSLConnectionSocketFactory sslsf) throws FatalErrorException {
        return createHttpClient(null, sslsf, null);
    }

    public static CloseableHttpClient createHttpClient(HttpClientConnectionManager connManager) throws FatalErrorException {
        return createHttpClient(null, null, connManager);
    }

    public static CloseableHttpClient createHttpClient(Integer timeout, SSLConnectionSocketFactory sslsf,
                                               HttpClientConnectionManager connManager) throws FatalErrorException {
        if (timeoutHttpRequest == null) {
            init(); // Set to default timeout.
        }
        HttpClientBuilder builder = HttpClientBuilder.create();
        setDefaultResquestConfig(timeout, builder);
        setSSLConnection(sslsf, builder);
        setConnectionManager(connManager, builder);

        return builder.build();
    }

    protected static void setDefaultResquestConfig(Integer timeout, HttpClientBuilder builder) {

        RequestConfig.Builder requestBuilder = RequestConfig.custom();

        if (timeout == null) {
            timeout = timeoutHttpRequest;
        }
        requestBuilder = requestBuilder.setSocketTimeout(timeout);
        builder.setDefaultRequestConfig(requestBuilder.build());
    }

    protected static void setConnectionManager(HttpClientConnectionManager connManager, HttpClientBuilder builder) {
        if (connManager != null) {
            builder.setConnectionManager(connManager);
        }
    }

    protected static void setSSLConnection(SSLConnectionSocketFactory sslsf, HttpClientBuilder builder) {
        if (sslsf != null) {
            builder.setSSLSocketFactory(sslsf);
        }
    }
}
