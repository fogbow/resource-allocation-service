package org.fogbowcloud.manager.core.utils;

import java.io.IOException;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Properties;

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
import org.fogbowcloud.manager.core.threads.AttendSpawningOrdersThread;
import org.json.JSONObject;

public class TunnelingServiceUtil {

	private static final Logger LOGGER = Logger.getLogger(AttendSpawningOrdersThread.class);

	private Properties properties; // TODO create class Properties, currently using java.util.Properties...
	private HttpClient reverseTunnelHttpClient = createReverseTunnelHttpClient();
	private PoolingHttpClientConnectionManager connectionManager;	

	public Map<String, String> getExternalServiceAddresses(String orderId) {
		if (orderId == null || orderId.isEmpty()) {
			return null;
		}
		String hostAddr = properties.getProperty(ConfigurationConstants.TOKEN_HOST_PRIVATE_ADDRESS_KEY);
		if (hostAddr == null) {
			return null;
		}
		HttpResponse response = null;
		try {
			String httpHostPort = properties.getProperty(ConfigurationConstants.TOKEN_HOST_HTTP_PORT_KEY);
			HttpGet httpGet = new HttpGet("http://" + hostAddr + ":" + httpHostPort + "/token/" + orderId + "/all");
			response = reverseTunnelHttpClient.execute(httpGet);
			if (response.getStatusLine().getStatusCode() == HttpStatus.SC_OK) {
				JSONObject jsonPorts = new JSONObject(EntityUtils.toString(response.getEntity()));
				if (jsonPorts.isNull(CommonConfigurationConstants.SSH_SERVICE_NAME)) {
					return null;
				}
				@SuppressWarnings("unchecked")
				Iterator<String> serviceIterator = jsonPorts.keys();
				Map<String, String> servicePerAddress = new HashMap<String, String>();
				String sshPublicHostIP = properties.getProperty(ConfigurationConstants.TOKEN_HOST_PUBLIC_ADDRESS_KEY);
				while (serviceIterator.hasNext()) {
					String service = (String) serviceIterator.next();
					String port = jsonPorts.optString(service);
					servicePerAddress.put(service, sshPublicHostIP + ":" + port);
				}
				return servicePerAddress;
			}
		} catch (Throwable e) {
			LOGGER.error("Error trying to communicate reverse tunnel and set map of addresses (IP and Port) for order [" + orderId + "]", e);
		} finally {
			if (response != null) {
				try {
					response.getEntity().getContent().close();
				} catch (IOException e) {
					LOGGER.warn("failed to close the content was already closed.", e);
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
