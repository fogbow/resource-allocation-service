package org.fogbowcloud.ras.api.parameters;

import org.fogbowcloud.ras.core.models.orders.VolumeOrder;

public class Volume {
    private String provider;
    private String cloudName;
    private String name;
    private int volumeSize;

    public VolumeOrder getOrder() {
        VolumeOrder order = new VolumeOrder(provider, cloudName, name, volumeSize);
        return order;
    }

    public String getProvider() {
        return provider;
    }

    public String getCloudName() {
        return cloudName;
    }

    public String getName() {
        return name;
    }

    public int getVolumeSize() {
        return volumeSize;
    }
}
