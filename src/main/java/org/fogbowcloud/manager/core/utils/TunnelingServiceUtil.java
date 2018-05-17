package org.fogbowcloud.manager.core.utils;

import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Properties;

import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.HttpStatus;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.conn.PoolingHttpClientConnectionManager;
import org.apache.http.util.EntityUtils;
import org.apache.log4j.Logger;
import org.fogbowcloud.manager.core.constants.CommonConfigurationConstants;
import org.fogbowcloud.manager.core.constants.ConfigurationConstants;
import org.fogbowcloud.manager.core.threads.SpawningProcessor;
import org.json.JSONObject;

public class TunnelingServiceUtil {

	private static final Logger LOGGER = Logger.getLogger(SpawningProcessor.class);
	
	public static final String HTTP_PROTOCOL = "http://";
	public static final String TOKEN_POINT = "/token/";
	public static final String END_POINT_ALL = "/all";

	private Properties properties;
	private PoolingHttpClientConnectionManager connectionManager;

	private HttpClient reverseTunnelHttpClient = createReverseTunnelHttpClient();

	private static TunnelingServiceUtil instance;
	
	private TunnelingServiceUtil() {}
	
	public static TunnelingServiceUtil getInstance() {
		if (instance == null) {
			instance = new TunnelingServiceUtil();
		}
		return instance;
	}

	public Map<String, String> getExternalServiceAddresses(String orderId) {
		if (orderId == null || orderId.isEmpty()) {
			return null;
		}
		String hostAddr = this.properties.getProperty(ConfigurationConstants.REVERSE_TUNNEL_PRIVATE_ADDRESS_KEY);
		if (hostAddr == null) {
			return null;
		}
		HttpResponse response = null;
		try {
			String httpHostPort = this.properties.getProperty(ConfigurationConstants.REVERSE_TUNNEL_HTTP_PORT_KEY);
			HttpGet httpGet = new HttpGet(HTTP_PROTOCOL + hostAddr + ":" + httpHostPort + TOKEN_POINT + orderId + END_POINT_ALL);
			response = this.reverseTunnelHttpClient.execute(httpGet);
			boolean isOkHttpStatusResponse  = response.getStatusLine().getStatusCode() == HttpStatus.SC_OK;
			if (response != null && isOkHttpStatusResponse) {
				LOGGER.info("Start process to get addresses (IP and Port) for order [" + orderId + "]");
				JSONObject jsonPorts = new JSONObject(EntityUtils.toString(response.getEntity()));
				if (jsonPorts.isNull(CommonConfigurationConstants.SSH_SERVICE_NAME)) {
					return null;
				}
				@SuppressWarnings("unchecked")
				Iterator<String> serviceIterator = jsonPorts.keys();
				Map<String, String> servicePerAddress = new HashMap<String, String>();
				String sshPublicHostIP = this.properties.getProperty(ConfigurationConstants.REVERSE_TUNNEL_PUBLIC_ADDRESS_KEY);
				while (serviceIterator.hasNext()) {
					String service = serviceIterator.next();
					String port = jsonPorts.optString(service);
					servicePerAddress.put(service, sshPublicHostIP + ":" + port);
				}
				return servicePerAddress;
			}
		} catch (Throwable e) {
			LOGGER.error("Error while to communicate reverse tunnel or set map of addresses (IP and Port) for order [" + orderId + "]", e);
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

	private CloseableHttpClient createReverseTunnelHttpClient() {
		this.connectionManager = new PoolingHttpClientConnectionManager();
		this.connectionManager.setMaxTotal(CommonConfigurationConstants.DEFAULT_MAX_POOL);
		this.connectionManager.setDefaultMaxPerRoute(CommonConfigurationConstants.DEFAULT_MAX_POOL);
		return HttpRequestUtil.createHttpClient(this.connectionManager);
	}
	
}
