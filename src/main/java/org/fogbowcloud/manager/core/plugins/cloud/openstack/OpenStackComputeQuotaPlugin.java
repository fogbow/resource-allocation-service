package org.fogbowcloud.manager.core.plugins.cloud.openstack;

import java.io.File;
import java.util.Properties;

import org.fogbowcloud.manager.core.HomeDir;
import org.fogbowcloud.manager.core.exceptions.QuotaException;
import org.fogbowcloud.manager.core.exceptions.RequestException;
import org.fogbowcloud.manager.core.models.quotas.ComputeQuota;
import org.fogbowcloud.manager.core.models.quotas.allocation.ComputeAllocation;
import org.fogbowcloud.manager.core.models.token.Token;
import org.fogbowcloud.manager.core.plugins.cloud.ComputeQuotaPlugin;
import org.fogbowcloud.manager.core.plugins.cloud.utils.HttpRequestClientUtil;
import org.fogbowcloud.manager.utils.JSONUtil;
import org.fogbowcloud.manager.utils.PropertiesUtil;
import org.json.JSONException;
import org.json.JSONObject;

public class OpenStackComputeQuotaPlugin implements ComputeQuotaPlugin {

	private static final String NOVAV2_PLUGIN_CONF_FILE = "openstack-nova-quota-plugin.conf";
	private static final String COMPUTE_NOVAV2_URL_KEY = "openstack_nova_v2_url";

	private static final String SUFFIX = "limits";
	private static final String COMPUTE_V2_API_ENDPOINT = "/v2/";
	
	private static final String MAX_TOTAL_CORES_JSON = "maxTotalCores";
	private static final String TOTAL_CORES_USED_JSON = "totalCoresUsed";
	private static final String MAX_TOTAL_RAM_SIZE_JSON = "maxTotalRAMSize";
	private static final String TOTAL_RAM_USED_JSON = "totalRAMUsed";
	private static final String MAX_TOTAL_INSTANCES_JSON = "maxTotalInstances";
	private static final String TOTAL_INSTANCES_USED_JSON = "totalInstancesUsed";
	
	private Properties properties;
	private HttpRequestClientUtil client;
	

	public OpenStackComputeQuotaPlugin() {
		HomeDir homeDir = HomeDir.getInstance();
		this.properties = PropertiesUtil.
				readProperties(homeDir.getPath() + File.separator + NOVAV2_PLUGIN_CONF_FILE);

		this.client = new HttpRequestClientUtil();
	}
	
	@Override
	public ComputeQuota getUserQuota(Token localToken) throws QuotaException {
		String jsonResponse = getJson(localToken);
		return processJson(jsonResponse);
	}

	private String getJson(Token localToken) throws QuotaException {
		String endpoint = 
				this.properties.getProperty(COMPUTE_NOVAV2_URL_KEY)
                + COMPUTE_V2_API_ENDPOINT	
                + SUFFIX;
		try {
			String jsonResponse = this.client.doGetRequest(endpoint, localToken);
			return jsonResponse;
		} catch (RequestException e) {
			throw new QuotaException("Could not make GET request.", e);
		}
	}
	
	private ComputeQuota processJson(String jsonStr) throws QuotaException {
		try {
			JSONObject jsonObject = (JSONObject) JSONUtil.getValue(jsonStr, "limits", "absolute");
			ComputeAllocation totalQuota = new ComputeAllocation(
					jsonObject.getInt(MAX_TOTAL_CORES_JSON), 
					jsonObject.getInt(MAX_TOTAL_RAM_SIZE_JSON),
					jsonObject.getInt(MAX_TOTAL_INSTANCES_JSON));
			ComputeAllocation usedQuota = new ComputeAllocation(
					jsonObject.getInt(TOTAL_CORES_USED_JSON), 
					jsonObject.getInt(TOTAL_RAM_USED_JSON),
					jsonObject.getInt(TOTAL_INSTANCES_USED_JSON));
			ComputeQuota computeQuota =	new ComputeQuota(
					totalQuota, 
					usedQuota);
			
			return computeQuota;
		} catch (JSONException e) {
			throw new QuotaException(e.getMessage(), e);
		}
	}
	
}
