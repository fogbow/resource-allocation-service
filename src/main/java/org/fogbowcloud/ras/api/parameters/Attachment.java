package org.fogbowcloud.ras.api.parameters;

import org.fogbowcloud.ras.core.models.orders.AttachmentOrder;

public class Attachment extends OrderApiParameter<AttachmentOrder> {

    private String computeId;
    private String volumeId;
    private String device;

    public String getComputeId() {
        return computeId;
    }

    public String getVolumeId() {
        return volumeId;
    }

    public String getDevice() {
        return device;
    }

    @Override
    public AttachmentOrder getOrder() {
        AttachmentOrder order = new AttachmentOrder(null, null,
            null, computeId, volumeId, device);
        return order;
    }

}
