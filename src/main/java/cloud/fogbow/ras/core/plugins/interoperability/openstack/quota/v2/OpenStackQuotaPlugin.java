package cloud.fogbow.ras.core.plugins.interoperability.openstack.quota.v2;

import java.util.Properties;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;

import org.apache.http.client.HttpResponseException;
import org.apache.log4j.Logger;

import com.google.common.annotations.VisibleForTesting;

import cloud.fogbow.common.exceptions.FogbowException;
import cloud.fogbow.common.models.OpenStackV3User;
import cloud.fogbow.common.util.PropertiesUtil;
import cloud.fogbow.common.util.connectivity.cloud.openstack.OpenStackHttpClient;
import cloud.fogbow.common.util.connectivity.cloud.openstack.OpenStackHttpToFogbowExceptionMapper;
import cloud.fogbow.ras.api.http.response.quotas.ResourceQuota;
import cloud.fogbow.ras.api.http.response.quotas.allocation.ResourceAllocation;
import cloud.fogbow.ras.constants.Messages;
import cloud.fogbow.ras.core.plugins.interoperability.QuotaPlugin;
import cloud.fogbow.ras.core.plugins.interoperability.openstack.OpenStackCloudUtils;

public class OpenStackQuotaPlugin implements QuotaPlugin<OpenStackV3User> {

    private static final Logger LOGGER = Logger.getLogger(OpenStackQuotaPlugin.class);

    protected static final String CINDER_V3_API_ENDPOINT = "/v3";
    protected static final String VOLUME_CINDER_URL_KEY = "openstack_cinder_url";
    protected static final String LIMITS_ENDPOINT = "/limits";
    protected static final String NEUTRON_V2_API_ENDPOINT = "/v2.0";
    protected static final String NETWORK_NEUTRON_URL_KEY = "openstack_neutron_url";
    protected static final String NOVA_V2_API_ENDPOINT = "/v2";
    protected static final String COMPUTE_NOVA_URL_KEY = "openstack_nova_url";
    protected static final String QUOTAS_ENDPOINT = "/quotas";
    protected static final String SUFFIX_ENDPOINT = "/details.json";
    protected static final String URL_SEPARATOR = "/";
    
    private Properties properties;
    private OpenStackHttpClient client;
    
    public OpenStackQuotaPlugin(@NotBlank String confFilePath) {
        this.properties = PropertiesUtil.readProperties(confFilePath);
        this.client = new OpenStackHttpClient();
    }
    
    @Override
    public ResourceQuota getUserQuota(@NotNull OpenStackV3User cloudUser) throws FogbowException {
        GetComputeQuotasResponse computeQuotas = getComputeQuotas(cloudUser);
        GetNetworkQuotasResponse networkQuotas = getNetworkQuotas(cloudUser);
        GetVolumeQuotasResponse volumeQuotas = getVolumeQuotas(cloudUser);
        return buildResourceQuota(computeQuotas, networkQuotas, volumeQuotas);
    }

    @VisibleForTesting
    ResourceQuota buildResourceQuota(
            @NotNull GetComputeQuotasResponse computeQuotas, 
            @NotNull GetNetworkQuotasResponse networkQuotas,
            @NotNull GetVolumeQuotasResponse volumeQuotas) {
        
        ResourceAllocation totalQuota = getTotalQuota(computeQuotas, networkQuotas, volumeQuotas);
        ResourceAllocation usedQuota = getUsedQuota(computeQuotas, networkQuotas, volumeQuotas);
        return new ResourceQuota(totalQuota, usedQuota);
    }
    
    @VisibleForTesting
    ResourceAllocation getUsedQuota(
            @NotNull GetComputeQuotasResponse computeQuotas, 
            @NotNull GetNetworkQuotasResponse networkQuotas,
            @NotNull GetVolumeQuotasResponse volumeQuotas) {
        
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
            @NotNull GetComputeQuotasResponse computeQuotas, 
            @NotNull GetNetworkQuotasResponse networkQuotas,
            @NotNull GetVolumeQuotasResponse volumeQuotas) {
        
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
    GetVolumeQuotasResponse getVolumeQuotas(@NotNull OpenStackV3User cloudUser) throws FogbowException {
        String tenantId = getTenantId(cloudUser);
        String endpoint = getVolumeQuotaEndpoint(tenantId);
        String response = doGetQuota(endpoint, cloudUser);
        return GetVolumeQuotasResponse.fromJson(response);
    }

    @VisibleForTesting
    String getVolumeQuotaEndpoint(@NotBlank String tenantId) {
        return this.properties.getProperty(VOLUME_CINDER_URL_KEY)
                .concat(CINDER_V3_API_ENDPOINT)
                .concat(URL_SEPARATOR)
                .concat(tenantId)
                .concat(LIMITS_ENDPOINT);
    }

    @VisibleForTesting
    GetNetworkQuotasResponse getNetworkQuotas(@NotNull OpenStackV3User cloudUser) throws FogbowException {
        String tenantId = getTenantId(cloudUser);
        String endpoint = getNetworkQuotaEndpoint(tenantId);
        String response = doGetQuota(endpoint, cloudUser);
        return GetNetworkQuotasResponse.fromJson(response);
    }

    @VisibleForTesting
    String getNetworkQuotaEndpoint(@NotBlank String tenantId) {
        return this.properties.getProperty(NETWORK_NEUTRON_URL_KEY)
                .concat(NEUTRON_V2_API_ENDPOINT)
                .concat(QUOTAS_ENDPOINT)
                .concat(URL_SEPARATOR)
                .concat(tenantId)
                .concat(SUFFIX_ENDPOINT);
    }
    
    @VisibleForTesting
    String getTenantId(@NotNull OpenStackV3User cloudUser) throws FogbowException {
        return OpenStackCloudUtils.getProjectIdFrom(cloudUser);
    }

    @VisibleForTesting
    GetComputeQuotasResponse getComputeQuotas(@NotNull OpenStackV3User cloudUser) throws FogbowException {
        String endpoint = getComputeQuotaEndpoint();
        String response = doGetQuota(endpoint, cloudUser);
        return GetComputeQuotasResponse.fromJson(response);
    }
    
    @VisibleForTesting
    String getComputeQuotaEndpoint() {
        return this.properties.getProperty(COMPUTE_NOVA_URL_KEY)
                .concat(NOVA_V2_API_ENDPOINT)
                .concat(LIMITS_ENDPOINT);
    }

    @VisibleForTesting
    String doGetQuota(@NotBlank String endpoint, @NotNull OpenStackV3User cloudUser) throws FogbowException {
        String response = null;
        try {
            LOGGER.debug(Messages.Info.GETTING_QUOTA);
            response = this.client.doGetRequest(endpoint, cloudUser);
        } catch (HttpResponseException e) {
            LOGGER.debug(Messages.Exception.FAILED_TO_GET_QUOTA);
            OpenStackHttpToFogbowExceptionMapper.map(e);
        }
        return response;
    }
    
    @VisibleForTesting
    void setClient(@NotNull OpenStackHttpClient client) {
        this.client = client;
    }

}
