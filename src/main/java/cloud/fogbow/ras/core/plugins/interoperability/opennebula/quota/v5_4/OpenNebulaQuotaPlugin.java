package cloud.fogbow.ras.core.plugins.interoperability.opennebula.quota.v5_4;

import java.util.Properties;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;

import org.apache.log4j.Logger;
import org.opennebula.client.Client;
import org.opennebula.client.user.User;
import org.opennebula.client.user.UserPool;
import org.opennebula.client.vnet.VirtualNetworkPool;

import com.google.common.annotations.VisibleForTesting;

import cloud.fogbow.common.exceptions.FogbowException;
import cloud.fogbow.common.models.CloudUser;
import cloud.fogbow.common.util.PropertiesUtil;
import cloud.fogbow.ras.api.http.response.quotas.ResourceQuota;
import cloud.fogbow.ras.api.http.response.quotas.allocation.ResourceAllocation;
import cloud.fogbow.ras.constants.Messages;
import cloud.fogbow.ras.core.plugins.interoperability.QuotaPlugin;
import cloud.fogbow.ras.core.plugins.interoperability.opennebula.OpenNebulaClientUtil;
import cloud.fogbow.ras.core.plugins.interoperability.opennebula.OpenNebulaConfigurationPropertyKeys;

public class OpenNebulaQuotaPlugin implements QuotaPlugin<CloudUser> {

    private static final Logger LOGGER = Logger.getLogger(OpenNebulaQuotaPlugin.class);
    
    protected static final String FORMAT_QUOTA_NETWORK_S_PATH = "NETWORK_QUOTA/NETWORK[ID=%s]/LEASES";
    protected static final String FORMAT_QUOTA_NETWORK_S_USED_PATH = "NETWORK_QUOTA/NETWORK[ID=%s]/LEASES_USED";
    protected static final String FORMAT_QUOTA_DATASTORE_S_SIZE_PATH = "DATASTORE_QUOTA/DATASTORE[ID=%s]/SIZE";
    protected static final String FORMAT_QUOTA_DATASTORE_S_IMAGES_PATH = "DATASTORE_QUOTA/DATASTORE[ID=%s]/IMAGES";
    protected static final String FORMAT_QUOTA_DATASTORE_S_SIZE_USED_PATH = "DATASTORE_QUOTA/DATASTORE[ID=%s]/SIZE_USED";
    protected static final String FORMAT_QUOTA_DATASTORE_S_IMAGES_USED_PATH = "DATASTORE_QUOTA/DATASTORE[ID=%s]/IMAGES_USED";
    protected static final String QUOTA_CPU_PATH = "VM_QUOTA/VM/CPU";
    protected static final String QUOTA_MEMORY_PATH = "VM_QUOTA/VM/MEMORY";
    protected static final String QUOTA_VMS_PATH = "VM_QUOTA/VM/VMS";
    protected static final String QUOTA_CPU_USED_PATH = "VM_QUOTA/VM/CPU_USED";
    protected static final String QUOTA_MEMORY_USED_PATH = "VM_QUOTA/VM/MEMORY_USED";
    protected static final String QUOTA_VMS_USED_PATH = "VM_QUOTA/VM/VMS_USED";
    protected static final int UNLIMITED_NETWORK_QUOTA_VALUE = -1;

    private static final int ONE_GIGABYTE_IN_MEGABYTES = 1024;

    private String defaultDatastore;
    private String defaultPublicNetwork;
    private String endpoint;

    public OpenNebulaQuotaPlugin(@NotBlank String confFilePath) {
        Properties properties = PropertiesUtil.readProperties(confFilePath);
        this.defaultDatastore = properties.getProperty(OpenNebulaConfigurationPropertyKeys.DEFAULT_DATASTORE_ID_KEY);
        this.defaultPublicNetwork = properties.getProperty(OpenNebulaConfigurationPropertyKeys.DEFAULT_PUBLIC_NETWORK_ID_KEY);
        this.endpoint = properties.getProperty(OpenNebulaConfigurationPropertyKeys.OPENNEBULA_RPC_ENDPOINT_KEY);
    }

    @Override
    public ResourceQuota getUserQuota(@NotNull CloudUser cloudUser) throws FogbowException {
        LOGGER.info(Messages.Info.GETTING_QUOTA);
        Client client = OpenNebulaClientUtil.createClient(this.endpoint, cloudUser.getToken());
        UserPool userPool = OpenNebulaClientUtil.getUserPool(client);
        User user = OpenNebulaClientUtil.getUser(userPool, cloudUser.getId());

        ResourceAllocation totalAllocation = getTotalAllocation(user);
        ResourceAllocation usedAllocation = getUsedAllocation(user, client);
        
        return new ResourceQuota(totalAllocation, usedAllocation);
    }

    @VisibleForTesting
    ResourceAllocation getUsedAllocation(@NotNull User user, @NotNull Client client) throws FogbowException {
        String diskSizeQuotaUsedPath = String.format(FORMAT_QUOTA_DATASTORE_S_SIZE_USED_PATH, this.defaultDatastore);
        String volumeQuotaUsedPath = String.format(FORMAT_QUOTA_DATASTORE_S_IMAGES_USED_PATH, this.defaultDatastore);
        String publicIpQuotaUsedPath = String.format(FORMAT_QUOTA_NETWORK_S_USED_PATH, this.defaultPublicNetwork);
        VirtualNetworkPool networkPool = OpenNebulaClientUtil.getNetworkPoolByUser(client);

        int cpuInUse = convertToInteger(user.xpath(QUOTA_CPU_USED_PATH));
        int diskInUse = convertToInteger(user.xpath(diskSizeQuotaUsedPath));
        int diskInUseGB = convertMegabytesIntoGigabytes(diskInUse);
        int instancesInUse = convertToInteger(user.xpath(QUOTA_VMS_USED_PATH));
        int memoryInUse = convertToInteger(user.xpath(QUOTA_MEMORY_USED_PATH));
        int networksInUse = networkPool.getLength();
        int publicIpsInUse = convertToInteger(user.xpath(publicIpQuotaUsedPath));
        int volumesInUse = convertToInteger(user.xpath(volumeQuotaUsedPath));
        
        ResourceAllocation usedAllocation = ResourceAllocation.builder()
                .instances(instancesInUse)
                .vCPU(cpuInUse)
                .ram(memoryInUse)
                .storage(diskInUseGB)
                .networks(networksInUse)
                .volumes(volumesInUse)
                .publicIps(publicIpsInUse)
                .build();
        
        return usedAllocation;
    }

    @VisibleForTesting
    int convertMegabytesIntoGigabytes(int sizeInMb) {
        return sizeInMb / ONE_GIGABYTE_IN_MEGABYTES;
    }

    @VisibleForTesting
    ResourceAllocation getTotalAllocation(@NotNull User user) {
        String diskSizeQuotaPath = String.format(FORMAT_QUOTA_DATASTORE_S_SIZE_PATH, this.defaultDatastore);
        String volumeQuotaPath = String.format(FORMAT_QUOTA_DATASTORE_S_IMAGES_PATH, this.defaultDatastore);
        String publicIpQuotaPath = String.format(FORMAT_QUOTA_NETWORK_S_PATH, this.defaultPublicNetwork);

        int maxCpu = convertToInteger(user.xpath(QUOTA_CPU_PATH));
        int maxDisk = convertToInteger(user.xpath(diskSizeQuotaPath));
        int maxInstances = convertToInteger(user.xpath(QUOTA_VMS_PATH));
        int maxMemory = convertToInteger(user.xpath(QUOTA_MEMORY_PATH));
        int maxPublicIps = convertToInteger(user.xpath(publicIpQuotaPath));
        int maxVolumes = convertToInteger(user.xpath(volumeQuotaPath));
        
        /**
         * OpenNebula defines user quotas for the number of addresses generated from a
         * network, but it does not allow estimating how many network reservations can
         * be created, so the value -1 will be adopted to inform this resource as
         * unlimited.
         */
        int maxNetworks = UNLIMITED_NETWORK_QUOTA_VALUE;
        
        ResourceAllocation totalAllocation = ResourceAllocation.builder()
                .instances(maxInstances)
                .vCPU(maxCpu)
                .ram(maxMemory)
                .storage(maxDisk)
                .networks(maxNetworks)
                .volumes(maxVolumes)
                .publicIps(maxPublicIps)
                .build();
        
        return totalAllocation;
    }
    
    @VisibleForTesting
    int convertToInteger(String number) {
        int converted = 0;
        try {
            converted = (int) Math.round(Double.parseDouble(number));
        } catch (NumberFormatException e) {
            LOGGER.error(String.format(Messages.Error.ERROR_MESSAGE, e));
        }
        return converted;
    }

}
