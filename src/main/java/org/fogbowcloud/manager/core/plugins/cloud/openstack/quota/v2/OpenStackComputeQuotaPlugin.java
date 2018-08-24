package org.fogbowcloud.manager.core.plugins.cloud.openstack.quota.v2;

import org.apache.http.client.HttpResponseException;
import org.fogbowcloud.manager.core.HomeDir;
import org.fogbowcloud.manager.core.constants.DefaultConfigurationConstants;
import org.fogbowcloud.manager.core.exceptions.FatalErrorException;
import org.fogbowcloud.manager.core.exceptions.FogbowManagerException;
import org.fogbowcloud.manager.core.exceptions.UnexpectedException;
import org.fogbowcloud.manager.core.models.quotas.ComputeQuota;
import org.fogbowcloud.manager.core.models.quotas.allocation.ComputeAllocation;
import org.fogbowcloud.manager.core.models.tokens.Token;
import org.fogbowcloud.manager.core.plugins.cloud.ComputeQuotaPlugin;
import org.fogbowcloud.manager.core.plugins.cloud.openstack.OpenStackHttpToFogbowManagerExceptionMapper;
import org.fogbowcloud.manager.util.PropertiesUtil;
import org.fogbowcloud.manager.util.connectivity.HttpRequestClientUtil;

import java.util.Properties;

public class OpenStackComputeQuotaPlugin implements ComputeQuotaPlugin {

    private static final String COMPUTE_NOVAV2_URL_KEY = "openstack_nova_v2_url";

    private static final String SUFFIX = "limits";
    private static final String COMPUTE_V2_API_ENDPOINT = "/v2/";

    private Properties properties;
    private HttpRequestClientUtil client;

    public OpenStackComputeQuotaPlugin() throws FatalErrorException {
        this.properties = PropertiesUtil.readProperties(HomeDir.getPath() +
                DefaultConfigurationConstants.OPENSTACK_CONF_FILE_NAME);

        this.client = new HttpRequestClientUtil();
    }

    @Override
    public ComputeQuota getUserQuota(Token token) throws FogbowManagerException, UnexpectedException {
        String endpoint = this.properties.getProperty(COMPUTE_NOVAV2_URL_KEY)
                + COMPUTE_V2_API_ENDPOINT + SUFFIX;

        String jsonResponse = null;

        try {
            jsonResponse = this.client.doGetRequest(endpoint, token);
        } catch (HttpResponseException e) {
            OpenStackHttpToFogbowManagerExceptionMapper.map(e);
        }

        GetQuotaResponse getQuotaResponse = GetQuotaResponse.fromJson(jsonResponse);

        int maxTotalCores = getQuotaResponse.getMaxTotalCores();
        int maxTotalRamSize = getQuotaResponse.getMaxTotalRamSize();
        int maxTotalInstances = getQuotaResponse.getMaxTotalInstances();
        ComputeAllocation totalQuota = new ComputeAllocation(maxTotalCores, maxTotalRamSize, maxTotalInstances);

        int totalCoresUsed = getQuotaResponse.getTotalCoresUsed();
        int totalRamUsed = getQuotaResponse.getTotalRamUsed();
        int totalInstancesUsed = getQuotaResponse.getTotalInstancesUsed();
        ComputeAllocation usedQuota = new ComputeAllocation(totalCoresUsed, totalRamUsed, totalInstancesUsed);

        return new ComputeQuota(totalQuota, usedQuota);
    }

}
