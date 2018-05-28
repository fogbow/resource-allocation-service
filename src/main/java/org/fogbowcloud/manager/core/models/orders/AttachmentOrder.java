package org.fogbowcloud.manager.core.models.orders;

import java.util.UUID;

public class AttachmentOrder extends Order {
    
    private String source;
    private String target;
    private String deviceId;
    
    public AttachmentOrder() {
        super(UUID.randomUUID().toString());
    }
    
    public AttachmentOrder(String id) {
        super(id);
    }
    
    public String getSource() {
        return this.source;
    }

    public String getTarget() {
        return this.target;
    }

    public String getDeviceId() {
        return this.deviceId;
    }

    @Override
    public OrderType getType() {
        return OrderType.ATTACHMENT;
    }
    
}