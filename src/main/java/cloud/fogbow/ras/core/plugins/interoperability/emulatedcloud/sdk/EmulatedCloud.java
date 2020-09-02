package cloud.fogbow.ras.core.plugins.interoperability.emulatedcloud.sdk;

import cloud.fogbow.ras.core.plugins.interoperability.emulatedcloud.sdk.compute.EmulatedCloudComputeManager;
import cloud.fogbow.ras.core.plugins.interoperability.emulatedcloud.sdk.volume.EmulatedCloudVolumeManager;

public class EmulatedCloud {

    private static EmulatedCloud instance;

    private EmulatedCloudComputeManager computeManager;
    private EmulatedCloudVolumeManager volumeManager;

    private EmulatedCloud() {
        this.computeManager = new EmulatedCloudComputeManager();
        this.volumeManager = new EmulatedCloudVolumeManager();
    }

    public static EmulatedCloud getInstance() {
        if (instance == null) {
            instance = new EmulatedCloud();
        }
        return instance;
    }

    public EmulatedCloudComputeManager computes() { return this.computeManager; }
    public EmulatedCloudVolumeManager volumes() { return this.volumeManager; }
}
