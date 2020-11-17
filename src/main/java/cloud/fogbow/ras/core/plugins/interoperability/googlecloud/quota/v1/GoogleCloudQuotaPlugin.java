package cloud.fogbow.ras.core.plugins.interoperability.googlecloud.quota.v1;

import cloud.fogbow.common.exceptions.FogbowException;
import cloud.fogbow.common.models.GoogleCloudUser;
import cloud.fogbow.common.util.PropertiesUtil;
import cloud.fogbow.common.util.connectivity.cloud.googlecloud.GoogleCloudHttpClient;
import cloud.fogbow.ras.api.http.response.quotas.ResourceQuota;
import cloud.fogbow.ras.api.http.response.quotas.allocation.ResourceAllocation;
import cloud.fogbow.ras.constants.Messages;
import cloud.fogbow.ras.core.plugins.interoperability.QuotaPlugin;
import com.google.common.annotations.VisibleForTesting;
import org.apache.log4j.Logger;

import java.util.Properties;

public class GoogleCloudQuotaPlugin implements QuotaPlugin<GoogleCloudUser> {
    private static final Logger LOGGER = Logger.getLogger(GoogleCloudQuotaPlugin.class);

    //TODO - remove when the plugin is implemented
    private static final int usedQuotaValue = 10;
    private static final int totalQuotaValue = 100;

    private Properties properties;
    private GoogleCloudHttpClient client;

    public GoogleCloudQuotaPlugin(String confFilePath){
        this.properties = PropertiesUtil.readProperties(confFilePath);
        this.initClient();
    }

    //TODO - implement getUserQuota
    // The non-implemented version returns a ResourceQuota filled with predefined values
    @Override
    public ResourceQuota getUserQuota(GoogleCloudUser cloudUser) throws FogbowException{
        LOGGER.info(Messages.Log.GETTING_QUOTA);
        return buildResourceQuota(totalQuotaValue, usedQuotaValue);
    }

    //TODO - implement buildResourceQuota
    // The non-implemented version creates a ResourceAllocation with predefined values
    @VisibleForTesting
    ResourceQuota buildResourceQuota(int totalQuotaValue, int usedQuotaValue) {
        ResourceAllocation totalQuota = getQuota(totalQuotaValue);
        ResourceAllocation usedQuota = getQuota(usedQuotaValue);
        return new ResourceQuota(totalQuota, usedQuota);
    }

    @VisibleForTesting
    ResourceAllocation getQuota(int defaultValue) {

        int cores = defaultValue;
        int ramSize = defaultValue;
        int instances = defaultValue;
        int ip = defaultValue;
        int network = defaultValue;
        int volumeGigabytes = defaultValue;
        int volumes = defaultValue;

        ResourceAllocation quota = ResourceAllocation.builder()
                .instances(instances)
                .vCPU(cores)
                .ram(ramSize)
                .publicIps(ip)
                .networks(network)
                .volumes(volumes)
                .storage(volumeGigabytes)
                .build();

        return quota;
    }

    private void initClient(){
        this.client = new GoogleCloudHttpClient();
    }
}
