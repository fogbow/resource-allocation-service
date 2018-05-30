package org.fogbowcloud.manager.core.models.orders;

import java.util.UUID;
import org.fogbowcloud.manager.core.models.token.FederationUser;

public class AttachmentOrder extends Order {

    /** this attribute refers to the instance of the computer where the volume will be attached */
    private String source;

    /** this attribute refers to the instanceId of the target volume of the attachment */
    private String target;

    /** this attribute refers to the mount point of the volume device */
    private String device;

    public AttachmentOrder() {
        super(UUID.randomUUID().toString());
    }

    public AttachmentOrder(String id, FederationUser federationUser, String requestingMember,
            String providingMember, String source, String target, String device) {
        super(id, federationUser, requestingMember, providingMember);
        this.source = source;
        this.target = target;
        this.device = device;
    }

    public AttachmentOrder(FederationUser federationUser, String requestingMember,
            String providingMember, String source, String target, String device) {
        super(UUID.randomUUID().toString(), federationUser, requestingMember, providingMember);
        this.source = source;
        this.target = target;
        this.device = device;
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
