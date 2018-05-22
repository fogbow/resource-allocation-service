package org.fogbowcloud.manager.utils;

import java.util.Properties;
import java.util.concurrent.TimeUnit;
import org.apache.http.client.config.RequestConfig;
import org.apache.http.conn.HttpClientConnectionManager;
import org.apache.http.conn.ssl.SSLConnectionSocketFactory;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.log4j.Logger;

public class HttpRequestUtil {

    public static final String CONTENT_TYPE_KEY = "Content-Type";
    public static final String ACCEPT_KEY = "Accept";
    public static final String JSON_CONTENT_TYPE_KEY = "application/json";

    private static final String TIMEOUT_HTTP_REQUEST = "timeout_http_request";
    private static final Logger LOGGER = Logger.getLogger(HttpRequestUtil.class);
    private static final int DEFAULT_TIMEOUT_REQUEST = (int) TimeUnit.MINUTES.toMillis(1);
    private static Integer timeoutHttpRequest;

    public static void init(Properties properties) {
        try {
            if (properties == null) {
                timeoutHttpRequest = DEFAULT_TIMEOUT_REQUEST;
            }

            String timeoutRequestStr = properties.getProperty(TIMEOUT_HTTP_REQUEST);
            timeoutHttpRequest = Integer.parseInt(timeoutRequestStr);
        } catch (NullPointerException | NumberFormatException e) {
            LOGGER.info(
                    "Setting HttpRequestUtil timeout with default: "
                            + DEFAULT_TIMEOUT_REQUEST
                            + " ms.");
            timeoutHttpRequest = DEFAULT_TIMEOUT_REQUEST;
        } catch (Exception e) {
            LOGGER.error("It is not possible to initialize HttpRequestUtil.", e);
            throw e;
        }
        LOGGER.info("The default HttpRequestUtil timeout is: " + timeoutHttpRequest + " ms.");
    }

    public static CloseableHttpClient createHttpClient() {
        return createHttpClient(null, null, null);
    }

    public static CloseableHttpClient createHttpClient(SSLConnectionSocketFactory sslsf) {
        return createHttpClient(null, sslsf, null);
    }

    public static CloseableHttpClient createHttpClient(HttpClientConnectionManager connManager) {
        return createHttpClient(null, null, connManager);
    }

    public static CloseableHttpClient createHttpClient(
            Integer timeout,
            SSLConnectionSocketFactory sslsf,
            HttpClientConnectionManager connManager) {
        if (timeoutHttpRequest == null) {
            init(null); // Set to default timeout.
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
        LOGGER.debug("Creating httpclient with timeout: " + timeout);
        requestBuilder = requestBuilder.setSocketTimeout(timeout);
        builder.setDefaultRequestConfig(requestBuilder.build());
    }

    protected static void setConnectionManager(
            HttpClientConnectionManager connManager, HttpClientBuilder builder) {
        if (connManager != null) {
            builder.setConnectionManager(connManager);
        }
    }

    protected static void setSSLConnection(
            SSLConnectionSocketFactory sslsf, HttpClientBuilder builder) {
        if (sslsf != null) {
            builder.setSSLSocketFactory(sslsf);
        }
    }

    protected static int getTimeoutHttpRequest() {
        return timeoutHttpRequest;
    }

    protected static void setTimeoutHttpRequest(Integer timeoutHttpRequest) {
        HttpRequestUtil.timeoutHttpRequest = timeoutHttpRequest;
    }
}
