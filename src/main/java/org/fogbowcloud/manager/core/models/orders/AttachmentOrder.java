package org.fogbowcloud.manager.core.models.orders;

import java.util.UUID;

import org.fogbowcloud.manager.core.models.instances.InstanceType;
import org.fogbowcloud.manager.core.models.token.FederationUser;

public class AttachmentOrder extends Order {

    private static final String SEPARATOR_ID = " ";

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

    public void setSource(String source) {
        this.source = source;
    }

    public String getTarget() {
        return this.target;
    }

    public void setTarget(String target) {
        this.target = target;
    }

    public String getDevice() {
        return this.device;
    }

    /**
     * The attachment-instance-id is formed by the source-id and target-id.
     *
     * If order-instance-id is null, it means the attachment has
     * already been deleted, then it also must return null so that
     * the closed processor will no longer try to delete this one.
     *
     * @return instance-id
     */
    @Override
    public String getInstanceId() {
        if (super.getInstanceId() == null) {
            return null;
        }

        return getSource() + SEPARATOR_ID + getTarget();
    }

    @Override
    public InstanceType getType() {
        return InstanceType.ATTACHMENT;
    }
}
