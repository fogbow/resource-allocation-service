package cloud.fogbow.ras.core.plugins.interoperability.emulatedcloud.sdk;

import cloud.fogbow.ras.core.plugins.interoperability.emulatedcloud.sdk.compute.EmulatedCloudComputeManager;

public class EmulatedCloud {

    private static EmulatedCloud instance;

    private EmulatedCloudComputeManager computeClient;

    private EmulatedCloud() {
        this.computeClient = new EmulatedCloudComputeManager();
    }

    public static EmulatedCloud getInstance() {
        if (instance == null) {
            instance = new EmulatedCloud();
        }
        return instance;
    }

    public EmulatedCloudComputeManager computes() { return this.computeClient; }
}
