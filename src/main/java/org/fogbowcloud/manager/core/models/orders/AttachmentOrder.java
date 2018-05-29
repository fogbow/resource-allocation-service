package org.fogbowcloud.manager.core.models.orders;

import java.util.UUID;

public class AttachmentOrder extends Order {
    
    /** this attribute refers to the instance of the computer where the volume will be attached */
    private String source; //computeSource
    
    /** this attribute refers to the instanceId of the target volume of the attachment */
    private String target; //volumeTarget
    
    /** this attribute refers to the mount point of the volume device */
    private String device; //mountPointDevice
    
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

    public String getDevice() {
        return this.device;
    }

    @Override
    public OrderType getType() {
        return OrderType.ATTACHMENT;
    }
    
}