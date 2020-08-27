package cloud.fogbow.ras.core.plugins.interoperability.openstack.quota.v2;

import java.util.Properties;

import cloud.fogbow.common.constants.OpenStackConstants;
import cloud.fogbow.ras.core.plugins.interoperability.openstack.sdk.v2.quota.models.GetComputeQuotasResponse;
import cloud.fogbow.ras.core.plugins.interoperability.openstack.sdk.v2.quota.models.GetNetworkQuotasResponse;
import cloud.fogbow.ras.core.plugins.interoperability.openstack.sdk.v2.quota.models.GetVolumeQuotasResponse;
import org.apache.log4j.Logger;

import com.google.common.annotations.VisibleForTesting;

import cloud.fogbow.common.exceptions.FogbowException;
import cloud.fogbow.common.models.OpenStackV3User;
import cloud.fogbow.common.util.PropertiesUtil;
import cloud.fogbow.common.util.connectivity.cloud.openstack.OpenStackHttpClient;
import cloud.fogbow.ras.api.http.response.quotas.ResourceQuota;
import cloud.fogbow.ras.api.http.response.quotas.allocation.ResourceAllocation;
import cloud.fogbow.ras.constants.Messages;
import cloud.fogbow.ras.core.plugins.interoperability.QuotaPlugin;
import cloud.fogbow.ras.core.plugins.interoperability.openstack.util.OpenStackPluginUtils;

public class OpenStackQuotaPlugin implements QuotaPlugin<OpenStackV3User> {

    private static final Logger LOGGER = Logger.getLogger(OpenStackQuotaPlugin.class);

    private Properties properties;
    private OpenStackHttpClient client;
    
    public OpenStackQuotaPlugin(String confFilePath) {
        this.properties = PropertiesUtil.readProperties(confFilePath);
        this.initClient();
    }
    
    @Override
    public ResourceQuota getUserQuota(OpenStackV3User cloudUser) throws FogbowException {
        LOGGER.info(Messages.Log.GETTING_QUOTA);
        GetComputeQuotasResponse computeQuotas = getComputeQuotas(cloudUser);
        GetNetworkQuotasResponse networkQuotas = getNetworkQuotas(cloudUser);
        GetVolumeQuotasResponse volumeQuotas = getVolumeQuotas(cloudUser);
        return buildResourceQuota(computeQuotas, networkQuotas, volumeQuotas);
    }

    @VisibleForTesting
    ResourceQuota buildResourceQuota(
            GetComputeQuotasResponse computeQuotas, 
            GetNetworkQuotasResponse networkQuotas,
            GetVolumeQuotasResponse volumeQuotas) {
        
        ResourceAllocation totalQuota = getTotalQuota(computeQuotas, networkQuotas, volumeQuotas);
        ResourceAllocation usedQuota = getUsedQuota(computeQuotas, networkQuotas, volumeQuotas);
        return new ResourceQuota(totalQuota, usedQuota);
    }
    
    @VisibleForTesting
    ResourceAllocation getUsedQuota(
            GetComputeQuotasResponse computeQuotas, 
            GetNetworkQuotasResponse networkQuotas,
            GetVolumeQuotasResponse volumeQuotas) {
        
        int totalCoresUsed = computeQuotas.getTotalCoresUsed();
        int totalRamUsed = computeQuotas.getTotalRamUsed();
        int totalInstancesUsed = computeQuotas.getTotalInstancesUsed();
        int floatingIpUsed = networkQuotas.getFloatingIpUsed();
        int networkUsed = networkQuotas.getNetworkUsed();
        int totalGigabytesUsed = volumeQuotas.getTotalGigabytesUsed();
        int volumesUsed = volumeQuotas.getTotalVolumesUsed();
        
        ResourceAllocation usedQuota = ResourceAllocation.builder()
                .instances(totalInstancesUsed)
                .vCPU(totalCoresUsed)
                .ram(totalRamUsed)
                .publicIps(floatingIpUsed)
                .networks(networkUsed)
                .volumes(volumesUsed)
                .storage(totalGigabytesUsed)
                .build();
        
        return usedQuota;
    }

    @VisibleForTesting
    ResourceAllocation getTotalQuota(
            GetComputeQuotasResponse computeQuotas, 
            GetNetworkQuotasResponse networkQuotas,
            GetVolumeQuotasResponse volumeQuotas) {
        
        int maxTotalCores = computeQuotas.getMaxTotalCores();
        int maxTotalRamSize = computeQuotas.getMaxTotalRamSize();
        int maxTotalInstances = computeQuotas.getMaxTotalInstances();
        int floatingIpLimit = networkQuotas.getFloatingIpLimit();
        int networkLimit = networkQuotas.getNetworkLimit();
        int maxTotalVolumeGigabytes = volumeQuotas.getMaxTotalVolumeGigabytes();
        int maxVolumes = volumeQuotas.getMaxTotalVolumes();
        
        ResourceAllocation totalQuota = ResourceAllocation.builder()
                .instances(maxTotalInstances)
                .vCPU(maxTotalCores)
                .ram(maxTotalRamSize)
                .publicIps(floatingIpLimit)
                .networks(networkLimit)
                .volumes(maxVolumes)
                .storage(maxTotalVolumeGigabytes)
                .build();
        
        return totalQuota;
    }

    @VisibleForTesting
    GetVolumeQuotasResponse getVolumeQuotas(OpenStackV3User cloudUser) throws FogbowException {
        String tenantId = getTenantId(cloudUser);
        String endpoint = getVolumeQuotaEndpoint(tenantId);
        String response = doGetQuota(endpoint, cloudUser);
        return GetVolumeQuotasResponse.fromJson(response);
    }

    @VisibleForTesting
    String getVolumeQuotaEndpoint(String tenantId) {
        return this.properties.getProperty(OpenStackPluginUtils.VOLUME_CINDER_URL_KEY)
                .concat(OpenStackConstants.CINDER_V3_API_ENDPOINT)
                .concat(OpenStackConstants.ENDPOINT_SEPARATOR)
                .concat(tenantId)
                .concat(OpenStackConstants.LIMITS_ENDPOINT);
    }

    @VisibleForTesting
    GetNetworkQuotasResponse getNetworkQuotas(OpenStackV3User cloudUser) throws FogbowException {
        String tenantId = getTenantId(cloudUser);
        String endpoint = getNetworkQuotaEndpoint(tenantId);
        String response = doGetQuota(endpoint, cloudUser);
        return GetNetworkQuotasResponse.fromJson(response);
    }

    @VisibleForTesting
    String getNetworkQuotaEndpoint(String tenantId) {
        return this.properties.getProperty(OpenStackPluginUtils.NETWORK_NEUTRON_URL_KEY)
                .concat(OpenStackConstants.NEUTRON_V2_API_ENDPOINT)
                .concat(OpenStackConstants.QUOTAS_ENDPOINT)
                .concat(OpenStackConstants.ENDPOINT_SEPARATOR)
                .concat(tenantId)
                .concat(OpenStackConstants.SUFFIX_ENDPOINT);
    }
    
    @VisibleForTesting
    String getTenantId(OpenStackV3User cloudUser) throws FogbowException {
        return OpenStackPluginUtils.getProjectIdFrom(cloudUser);
    }

    @VisibleForTesting
    GetComputeQuotasResponse getComputeQuotas(OpenStackV3User cloudUser) throws FogbowException {
        String endpoint = getComputeQuotaEndpoint();
        String response = doGetQuota(endpoint, cloudUser);
        return GetComputeQuotasResponse.fromJson(response);
    }
    
    @VisibleForTesting
    String getComputeQuotaEndpoint() {
        return this.properties.getProperty(OpenStackPluginUtils.COMPUTE_NOVA_URL_KEY)
                .concat(OpenStackConstants.NOVA_V2_API_ENDPOINT)
                .concat(OpenStackConstants.LIMITS_ENDPOINT);
    }

    @VisibleForTesting
    String doGetQuota(String endpoint, OpenStackV3User cloudUser) throws FogbowException {
        String response = null;
        try {
            LOGGER.debug(Messages.Log.GETTING_QUOTA);
            response = this.client.doGetRequest(endpoint, cloudUser);
        } catch (FogbowException e) {
            LOGGER.debug(Messages.Exception.FAILED_TO_GET_QUOTA);
            throw e;
        }
        return response;
    }
    
    @VisibleForTesting
    void setClient(OpenStackHttpClient client) {
        this.client = client;
    }

    private void initClient() {
        this.client = new OpenStackHttpClient();
    }
}
