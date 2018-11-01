package org.fogbowcloud.ras.api.parameters;

import org.fogbowcloud.ras.core.models.orders.AttachmentOrder;

public class Attachment {
    private String provider;
    private String computeId;
    private String volumeId;
    private String device;

    public String getProvider() {
        return provider;
    }

    public String getComputeId() {
        return computeId;
    }

    public String getVolumeId() {
        return volumeId;
    }

    public String getDevice() {
        return device;
    }

    public AttachmentOrder getOrder() {
        AttachmentOrder order = new AttachmentOrder(provider, computeId, volumeId, device);
        return order;
    }
}
