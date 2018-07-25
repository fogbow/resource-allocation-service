package org.fogbowcloud.manager.core.plugins.cloud.openstack;

import java.io.File;
import java.util.Properties;

import org.apache.http.client.HttpResponseException;
import org.fogbowcloud.manager.core.HomeDir;
import org.fogbowcloud.manager.core.constants.DefaultConfigurationConstants;
import org.fogbowcloud.manager.core.exceptions.*;
import org.fogbowcloud.manager.core.models.quotas.ComputeQuota;
import org.fogbowcloud.manager.core.models.quotas.allocation.ComputeAllocation;
import org.fogbowcloud.manager.core.models.tokens.Token;
import org.fogbowcloud.manager.core.plugins.cloud.ComputeQuotaPlugin;
import org.fogbowcloud.manager.util.connectivity.HttpRequestClientUtil;
import org.fogbowcloud.manager.util.JSONUtil;
import org.fogbowcloud.manager.util.PropertiesUtil;
import org.json.JSONException;
import org.json.JSONObject;

public class OpenStackComputeQuotaPlugin implements ComputeQuotaPlugin {

	private static final String COMPUTE_NOVAV2_URL_KEY = "openstack_nova_v2_url";

	private static final String SUFFIX = "limits";
	private static final String COMPUTE_V2_API_ENDPOINT = "/v2/";
	
	protected static final String MAX_TOTAL_CORES_JSON = "maxTotalCores";
	protected static final String TOTAL_CORES_USED_JSON = "totalCoresUsed";
	protected static final String MAX_TOTAL_RAM_SIZE_JSON = "maxTotalRAMSize";
	protected static final String TOTAL_RAM_USED_JSON = "totalRAMUsed";
	protected static final String MAX_TOTAL_INSTANCES_JSON = "maxTotalInstances";
	protected static final String TOTAL_INSTANCES_USED_JSON = "totalInstancesUsed";
	
	private Properties properties;
	private HttpRequestClientUtil client;

	public OpenStackComputeQuotaPlugin() throws FatalErrorException {
		HomeDir homeDir = HomeDir.getInstance();
        this.properties = PropertiesUtil.readProperties(homeDir.getPath() + File.separator
                + DefaultConfigurationConstants.OPENSTACK_CONF_FILE_NAME);

		this.client = new HttpRequestClientUtil();
	}
	
	@Override
	public ComputeQuota getUserQuota(Token localToken) throws FogbowManagerException, UnexpectedException {
		String jsonResponse = getQuotaJson(localToken);
		return processJson(jsonResponse);
	}

	protected String getQuotaJson(Token localToken) throws FogbowManagerException, UnexpectedException {
		String endpoint = this.properties.getProperty(COMPUTE_NOVAV2_URL_KEY)
                + COMPUTE_V2_API_ENDPOINT + SUFFIX;

		String jsonResponse = null;

		try {
			jsonResponse = this.client.doGetRequest(endpoint, localToken);
		} catch (HttpResponseException e) {
			OpenStackHttpToFogbowManagerExceptionMapper.map(e);
		}

		return jsonResponse;
	}
	
	private ComputeQuota processJson(String jsonStr) throws UnexpectedException {
		try {
			JSONObject jsonObject = (JSONObject) JSONUtil.getValue(jsonStr, "limits", "absolute");

			ComputeAllocation totalQuota = new ComputeAllocation(jsonObject.getInt(MAX_TOTAL_CORES_JSON),
					jsonObject.getInt(MAX_TOTAL_RAM_SIZE_JSON), jsonObject.getInt(MAX_TOTAL_INSTANCES_JSON));

			ComputeAllocation usedQuota = new ComputeAllocation(jsonObject.getInt(TOTAL_CORES_USED_JSON),
					jsonObject.getInt(TOTAL_RAM_USED_JSON),	jsonObject.getInt(TOTAL_INSTANCES_USED_JSON));

			ComputeQuota computeQuota =	new ComputeQuota(totalQuota, usedQuota);
			
			return computeQuota;
		} catch (JSONException e) {
			throw new UnexpectedException(e.getMessage(), e);
		}
	}
}
