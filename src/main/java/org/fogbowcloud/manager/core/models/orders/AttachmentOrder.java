package org.fogbowcloud.manager.core.models.orders;

import java.util.UUID;

import org.fogbowcloud.manager.core.models.ResourceType;
import org.fogbowcloud.manager.core.models.tokens.FederationUserToken;

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

    public AttachmentOrder(String id, FederationUserToken federationUserToken, String requestingMember,
                           String providingMember, String source, String target, String device) {
        super(id, federationUserToken, requestingMember, providingMember);
        this.source = source;
        this.target = target;
        this.device = device;
    }

    public AttachmentOrder(FederationUserToken federationUserToken, String requestingMember,
                           String providingMember, String source, String target, String device) {
        super(UUID.randomUUID().toString(), federationUserToken, requestingMember, providingMember);
        this.source = source;
        this.target = target;
        this.device = device;
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

    @Override
    public ResourceType getType() {
        return ResourceType.ATTACHMENT;
    }
}
