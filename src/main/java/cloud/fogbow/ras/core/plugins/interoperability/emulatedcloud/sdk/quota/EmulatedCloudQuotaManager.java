package cloud.fogbow.ras.core.plugins.interoperability.emulatedcloud.sdk.quota;

import cloud.fogbow.ras.core.plugins.interoperability.emulatedcloud.EmulatedCloudConstants;
import cloud.fogbow.ras.core.plugins.interoperability.emulatedcloud.EmulatedCloudUtils;
import cloud.fogbow.ras.core.plugins.interoperability.emulatedcloud.sdk.compute.EmulatedCloudComputeManager;
import cloud.fogbow.ras.core.plugins.interoperability.emulatedcloud.sdk.compute.models.EmulatedCompute;
import cloud.fogbow.ras.core.plugins.interoperability.emulatedcloud.sdk.network.EmulatedCloudNetworkManager;
import cloud.fogbow.ras.core.plugins.interoperability.emulatedcloud.sdk.network.models.EmulatedNetwork;
import cloud.fogbow.ras.core.plugins.interoperability.emulatedcloud.sdk.publicip.EmulatedCloudPublicIpManager;
import cloud.fogbow.ras.core.plugins.interoperability.emulatedcloud.sdk.publicip.models.EmulatedPublicIp;
import cloud.fogbow.ras.core.plugins.interoperability.emulatedcloud.sdk.quota.models.EmulatedQuota;
import cloud.fogbow.ras.core.plugins.interoperability.emulatedcloud.sdk.volume.EmulatedCloudVolumeManager;
import cloud.fogbow.ras.core.plugins.interoperability.emulatedcloud.sdk.volume.models.EmulatedVolume;

import java.util.List;
import java.util.Properties;

public class EmulatedCloudQuotaManager {
    private static EmulatedCloudQuotaManager instance;

    private int TOTAL_INSTANCES;
    private int TOTAL_VCPU;
    private int TOTAL_RAM;
    private int TOTAL_VOLUMES;
    private int TOTAL_STORAGE;
    private int TOTAL_NETWORKS;
    private int TOTAL_PUBLIC_IPS;

    public EmulatedCloudQuotaManager(Properties properties) {
        EmulatedCloudUtils.checkQuotaProperties(properties);
        TOTAL_INSTANCES = Integer.parseInt(properties.getProperty(EmulatedCloudConstants.Conf.QUOTA_INSTANCES_KEY));
        TOTAL_RAM = Integer.parseInt(properties.getProperty(EmulatedCloudConstants.Conf.QUOTA_RAM_KEY));
        TOTAL_VCPU = Integer.parseInt(properties.getProperty(EmulatedCloudConstants.Conf.QUOTA_VCPU_KEY));
        TOTAL_VOLUMES = Integer.parseInt(properties.getProperty(EmulatedCloudConstants.Conf.QUOTA_VOLUMES_KEY));
        TOTAL_STORAGE = 30;
        TOTAL_STORAGE = Integer.parseInt(properties.getProperty(EmulatedCloudConstants.Conf.QUOTA_STORAGE_KEY));
        TOTAL_NETWORKS = Integer.parseInt(properties.getProperty(EmulatedCloudConstants.Conf.QUOTA_NETWORKS_KEY));
        TOTAL_PUBLIC_IPS = Integer.parseInt(properties.getProperty(EmulatedCloudConstants.Conf.QUOTA_PUBLIC_IP_KEY));
    }

    public static EmulatedCloudQuotaManager getInstance(Properties properties) {
        if (instance == null) {
            instance = new EmulatedCloudQuotaManager(properties);
        }
        return instance;
    }

    public EmulatedQuota usedQuota() {
        List<EmulatedCompute> computes = EmulatedCloudComputeManager.getInstance().list();
        List<EmulatedVolume> volumes = EmulatedCloudVolumeManager.getInstance().list();
        List<EmulatedNetwork> networks = EmulatedCloudNetworkManager.getInstance().list();
        List<EmulatedPublicIp> publicIps = EmulatedCloudPublicIpManager.getInstance().list();

        int instances = computes.size();
        int ram = getTotalRam(computes);
        int vCPU = getTotalVCPU(computes);

        int volumeInstances = volumes.size();
        int storage = getTotalDisk(volumes);

        int networksInstances = networks.size();
        int publicIpsInstances = publicIps.size();

        return new EmulatedQuota.Builder()
                .instances(instances)
                .ram(ram)
                .vCPU(vCPU)
                .volumes(volumeInstances)
                .storage(storage)
                .networks(networksInstances)
                .publicIps(publicIpsInstances)
                .build();
    }

    public EmulatedQuota totalQuota() {
        return new EmulatedQuota.Builder()
                .instances(TOTAL_INSTANCES)
                .ram(TOTAL_RAM)
                .vCPU(TOTAL_VCPU)
                .volumes(TOTAL_VOLUMES)
                .storage(TOTAL_STORAGE)
                .networks(TOTAL_NETWORKS)
                .publicIps(TOTAL_PUBLIC_IPS)
                .build();
    }

    private int getTotalDisk(List<EmulatedVolume> volumes) {
        return volumes.stream()
                .map(volume -> Integer.parseInt(volume.getSize()))
                .reduce(0, Integer::sum);
    }

    private int getTotalVCPU(List<EmulatedCompute> computes) {
        return computes.stream()
                .map(compute -> compute.getvCPU())
                .reduce(0, Integer::sum);
    }

    private int getTotalRam(List<EmulatedCompute> computes) {
        return computes.stream()
                .map(compute -> compute.getMemory())
                .reduce(0, Integer::sum);
    }
}
