package org.fogbowcloud.manager.core.plugins.cloud.openstack;

import java.io.File;
import java.util.Properties;

import org.apache.http.client.HttpResponseException;
import org.fogbowcloud.manager.core.HomeDir;
import org.fogbowcloud.manager.core.constants.DefaultConfigurationConstants;
import org.fogbowcloud.manager.core.exceptions.*;
import org.fogbowcloud.manager.core.models.quotas.ComputeQuota;
import org.fogbowcloud.manager.core.models.quotas.allocation.ComputeAllocation;
import org.fogbowcloud.manager.core.models.tokens.LocalUserAttributes;
import org.fogbowcloud.manager.core.plugins.cloud.ComputeQuotaPlugin;
import org.fogbowcloud.manager.core.plugins.serialization.openstack.quota.v2.GetQuotaResponse;
import org.fogbowcloud.manager.util.connectivity.HttpRequestClientUtil;
import org.fogbowcloud.manager.util.PropertiesUtil;

public class OpenStackComputeQuotaPlugin implements ComputeQuotaPlugin {

	private static final String COMPUTE_NOVAV2_URL_KEY = "openstack_nova_v2_url";

	private static final String SUFFIX = "limits";
	private static final String COMPUTE_V2_API_ENDPOINT = "/v2/";

	private Properties properties;
	private HttpRequestClientUtil client;

	public OpenStackComputeQuotaPlugin() throws FatalErrorException {
		HomeDir homeDir = HomeDir.getInstance();
        this.properties = PropertiesUtil.readProperties(homeDir.getPath() + File.separator
                + DefaultConfigurationConstants.OPENSTACK_CONF_FILE_NAME);

		this.client = new HttpRequestClientUtil();
	}

	@Override
	public ComputeQuota getUserQuota(LocalUserAttributes localUserAttributes) throws FogbowManagerException, UnexpectedException {
		String endpoint = this.properties.getProperty(COMPUTE_NOVAV2_URL_KEY)
				+ COMPUTE_V2_API_ENDPOINT + SUFFIX;

		String jsonResponse = null;

		try {
			jsonResponse = this.client.doGetRequest(endpoint, localUserAttributes);
		} catch (HttpResponseException e) {
			OpenStackHttpToFogbowManagerExceptionMapper.map(e);
		}

		GetQuotaResponse getQuotaResponse = GetQuotaResponse.fromJson(jsonResponse);

		int maxTotalCores = getQuotaResponse.getMaxTotalCores();
		int maxTotalRamSize = getQuotaResponse.getMaxTotalRamSize();
		int maxTotalInstances = getQuotaResponse.getMaxTotalInstances();
		ComputeAllocation totalQuota = new ComputeAllocation(maxTotalCores,	maxTotalRamSize, maxTotalInstances);

		int totalCoresUsed = getQuotaResponse.getTotalCoresUsed();
		int totalRamUsed = getQuotaResponse.getTotalRamUsed();
		int totalInstancesUsed = getQuotaResponse.getTotalInstancesUsed();
		ComputeAllocation usedQuota = new ComputeAllocation(totalCoresUsed, totalRamUsed, totalInstancesUsed);

		return new ComputeQuota(totalQuota, usedQuota);
	}

}
