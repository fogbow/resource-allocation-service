package cloud.fogbow.ras.core.plugins.interoperability.emulatedcloud.sdk.quota;

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

public class EmulatedCloudQuotaManager {
    private static EmulatedCloudQuotaManager instance;

    private final int TOTAL_INSTANCES = 100;
    private final int TOTAL_VCPU= 8;
    private final int TOTAL_RAM = 16384;
    private final int TOTAL_VOLUMES = 200;
    private final int TOTAL_STORAGE = 30;
    private final int TOTAL_NETWORKS = 15;
    private final int TOTAL_PUBLIC_IPS = 5;

    public static EmulatedCloudQuotaManager getInstance() {
        if (instance == null) {
            instance = new EmulatedCloudQuotaManager();
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
