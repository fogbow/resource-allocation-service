package cloud.fogbow.ras.core.plugins.interoperability.openstack.quota.v2;

import cloud.fogbow.common.exceptions.FatalErrorException;
import cloud.fogbow.common.exceptions.FogbowException;
import cloud.fogbow.common.models.OpenStackV3User;
import cloud.fogbow.common.util.PropertiesUtil;
import cloud.fogbow.common.util.connectivity.cloud.openstack.OpenStackHttpClient;
import cloud.fogbow.ras.api.http.response.quotas.ComputeQuota;
import cloud.fogbow.ras.api.http.response.quotas.allocation.ComputeAllocation;
import cloud.fogbow.ras.constants.Messages;
import cloud.fogbow.ras.core.plugins.interoperability.ComputeQuotaPlugin;
import cloud.fogbow.common.util.connectivity.cloud.openstack.OpenStackHttpToFogbowExceptionMapper;
import org.apache.http.client.HttpResponseException;
import org.apache.log4j.Logger;

import java.util.Properties;

public class OpenStackComputeQuotaPlugin implements ComputeQuotaPlugin<OpenStackV3User> {
    private static final Logger LOGGER = Logger.getLogger(OpenStackComputeQuotaPlugin.class);

    private static final String COMPUTE_NOVAV2_URL_KEY = "openstack_nova_v2_url";
    private static final String SUFFIX = "limits";
    private static final String COMPUTE_V2_API_ENDPOINT = "/v2/";
    private Properties properties;
    private OpenStackHttpClient client;

    public OpenStackComputeQuotaPlugin(String confFilePath) throws FatalErrorException {
        this.properties = PropertiesUtil.readProperties(confFilePath);
        this.client = new OpenStackHttpClient();
    }

    @Override
    public ComputeQuota getUserQuota(OpenStackV3User cloudUser) throws FogbowException {
        String endpoint = this.properties.getProperty(COMPUTE_NOVAV2_URL_KEY) + COMPUTE_V2_API_ENDPOINT + SUFFIX;

        String jsonResponse = null;

        jsonResponse = doGetQuota(endpoint, cloudUser);

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

    protected String doGetQuota(String endpoint, OpenStackV3User cloudUser) throws FogbowException {
        String jsonResponse = null;
        try {
            LOGGER.debug(Messages.Info.GETTING_QUOTA);
            jsonResponse = this.client.doGetRequest(endpoint, cloudUser);
        } catch (HttpResponseException e) {
            LOGGER.debug(Messages.Exception.FAILED_TO_GET_QUOTA);
            OpenStackHttpToFogbowExceptionMapper.map(e);
        }

        return jsonResponse;
    }

    // For testing
    protected void setClient(OpenStackHttpClient client) {
        this.client = client;
    }
}
