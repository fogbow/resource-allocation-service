package cloud.fogbow.ras.core.plugins.interoperability.openstack.quota.v2;

import cloud.fogbow.common.exceptions.FatalErrorException;
import cloud.fogbow.common.exceptions.FogbowException;
import cloud.fogbow.common.models.CloudToken;
import cloud.fogbow.common.util.PropertiesUtil;
import cloud.fogbow.ras.constants.ConfigurationPropertyDefaults;
import cloud.fogbow.ras.constants.ConfigurationPropertyKeys;
import cloud.fogbow.ras.core.PropertiesHolder;
import cloud.fogbow.ras.core.models.quotas.ComputeQuota;
import cloud.fogbow.ras.core.models.quotas.allocation.ComputeAllocation;
import cloud.fogbow.ras.core.plugins.interoperability.ComputeQuotaPlugin;
import cloud.fogbow.ras.core.plugins.interoperability.openstack.OpenStackHttpClient;
import cloud.fogbow.ras.core.plugins.interoperability.openstack.OpenStackHttpToFogbowExceptionMapper;
import org.apache.http.client.HttpResponseException;
import org.apache.log4j.Logger;

import java.util.Properties;

public class OpenStackComputeQuotaPlugin implements ComputeQuotaPlugin {
    private static final Logger LOGGER = Logger.getLogger(OpenStackComputeQuotaPlugin.class);

    private static final String COMPUTE_NOVAV2_URL_KEY = "openstack_nova_v2_url";
    private static final String SUFFIX = "limits";
    private static final String COMPUTE_V2_API_ENDPOINT = "/v2/";
    private Properties properties;
    private OpenStackHttpClient client;

    public OpenStackComputeQuotaPlugin(String confFilePath) throws FatalErrorException {
        this.properties = PropertiesUtil.readProperties(confFilePath);

        this.client = new OpenStackHttpClient(
                new Integer(PropertiesHolder.getInstance().getProperty(ConfigurationPropertyKeys.HTTP_REQUEST_TIMEOUT_KEY,
                        ConfigurationPropertyDefaults.XMPP_TIMEOUT)));
    }

    @Override
    public ComputeQuota getUserQuota(CloudToken token) throws FogbowException {
        String endpoint = this.properties.getProperty(COMPUTE_NOVAV2_URL_KEY) + COMPUTE_V2_API_ENDPOINT + SUFFIX;

        String jsonResponse = null;

        try {
            LOGGER.debug("Calling quota plugin");
            jsonResponse = this.client.doGetRequest(endpoint, token);
        } catch (HttpResponseException e) {
            LOGGER.debug("Exception raised: " + e.getMessage(), e);
            OpenStackHttpToFogbowExceptionMapper.map(e);
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
