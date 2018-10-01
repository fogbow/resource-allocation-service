package org.fogbowcloud.ras.api.parameters;

import org.fogbowcloud.ras.core.models.orders.AttachmentOrder;

public class Attachment implements OrderApiParameter<AttachmentOrder> {

    private String source;
    private String target;
    private String device;

    public String getSource() {
        return source;
    }

    public String getTarget() {
        return target;
    }

    public String getDevice() {
        return device;
    }

    @Override
    public AttachmentOrder getOrder() {
        AttachmentOrder order = new AttachmentOrder(null, null,
            null, source, target, device);
        return order;
    }

}
