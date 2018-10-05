package org.fogbowcloud.ras.api.parameters;

import org.fogbowcloud.ras.core.models.orders.VolumeOrder;

public class Volume extends OrderApiParameter<VolumeOrder> {

    private int volumeSize;
    private String name;

    @Override
    public VolumeOrder getOrder() {
        VolumeOrder order = new VolumeOrder(null, null, null, volumeSize, name);
        return order;
    }

    public int getVolumeSize() {
        return volumeSize;
    }

    public String getName() {
        return name;
    }

}
