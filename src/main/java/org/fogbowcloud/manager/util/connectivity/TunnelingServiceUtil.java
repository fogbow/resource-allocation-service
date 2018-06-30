package org.fogbowcloud.manager.util.connectivity;

import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.HttpStatus;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.conn.PoolingHttpClientConnectionManager;
import org.apache.http.util.EntityUtils;
import org.apache.log4j.Logger;
import org.fogbowcloud.manager.core.PropertiesHolder;
import org.fogbowcloud.manager.core.constants.ConfigurationConstants;
import org.fogbowcloud.manager.core.constants.DefaultConfigurationConstants;
import org.fogbowcloud.manager.core.exceptions.FatalErrorException;
import org.json.JSONObject;

import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

public class TunnelingServiceUtil {

    private static final Logger LOGGER = Logger.getLogger(TunnelingServiceUtil.class);

    public static final String HTTP_PROTOCOL = "http://";
    public static final String TOKEN_POINT = "/tokens/";
    public static final String END_POINT_ALL = "/all";

    private PoolingHttpClientConnectionManager connectionManager;

    private HttpClient reverseTunnelHttpClient;

    private static TunnelingServiceUtil instance;

    private TunnelingServiceUtil() throws FatalErrorException {
        this.reverseTunnelHttpClient = createReverseTunnelHttpClient();
    }

    public static TunnelingServiceUtil getInstance() throws FatalErrorException {
        if (instance == null) {
            instance = new TunnelingServiceUtil();
        }
        return instance;
    }

    public Map<String, String> getExternalServiceAddresses(String orderId) throws FatalErrorException {
        if (orderId == null || orderId.isEmpty()) {
            return null;
        }
        String hostAddr = PropertiesHolder.getInstance().getProperty(
                        ConfigurationConstants.REVERSE_TUNNEL_PRIVATE_ADDRESS_KEY);
        if (hostAddr == null) {
            return null;
        }
        HttpResponse response = null;
        try {
            String httpHostPort = PropertiesHolder.getInstance().getProperty(
                    ConfigurationConstants.REVERSE_TUNNEL_HTTP_PORT_KEY);
            HttpGet httpGet =
                    new HttpGet(HTTP_PROTOCOL + hostAddr + ":" + httpHostPort
                                    + TOKEN_POINT + orderId + END_POINT_ALL);
            response = this.reverseTunnelHttpClient.execute(httpGet);
            boolean isOkHttpStatusResponse = response.getStatusLine().getStatusCode() == HttpStatus.SC_OK;
            if (response != null && isOkHttpStatusResponse) {
                LOGGER.debug("Getting addresses (IP and Port) for order [" + orderId + "]");
                JSONObject jsonPorts = new JSONObject(EntityUtils.toString(response.getEntity()));
                if (jsonPorts.isNull(DefaultConfigurationConstants.SSH_SERVICE_NAME)) {
                    return null;
                }
                @SuppressWarnings("unchecked")
                Iterator<String> serviceIterator = jsonPorts.keys();
                Map<String, String> servicePerAddress = new HashMap<String, String>();
                String sshPublicHostIP = PropertiesHolder.getInstance().getProperty(
                                ConfigurationConstants.REVERSE_TUNNEL_PUBLIC_ADDRESS_KEY);
                while (serviceIterator.hasNext()) {
                    String service = serviceIterator.next();
                    String port = jsonPorts.optString(service);
                    servicePerAddress.put(service, sshPublicHostIP + ":" + port);
                }
                return servicePerAddress;
            }
        } catch (Throwable e) {
            LOGGER.error(
                    "Error while to communicate reverse tunnel or set map of addresses (IP and Port) for order ["
                            + orderId + "]", e);
        } finally {
            if (response != null) {
                HttpEntity responseEntity = null;
                InputStream entityContent = null;
                try {
                    responseEntity = response.getEntity();
                    entityContent = responseEntity.getContent();
                    entityContent.close();
                } catch (IOException e) {
                    LOGGER.warn("Failed to close the content was already closed.", e);
                }
            }
        }
        return null;
    }

    private CloseableHttpClient createReverseTunnelHttpClient() throws FatalErrorException {
        this.connectionManager = new PoolingHttpClientConnectionManager();
        this.connectionManager.setMaxTotal(DefaultConfigurationConstants.DEFAULT_MAX_POOL);
        this.connectionManager.setDefaultMaxPerRoute(DefaultConfigurationConstants.DEFAULT_MAX_POOL);
        return HttpRequestUtil.createHttpClient(this.connectionManager);
    }
}
