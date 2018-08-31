package org.fogbowcloud.ras.core.models.orders;

import org.fogbowcloud.ras.core.models.ResourceType;
import org.fogbowcloud.ras.core.models.tokens.FederationUserToken;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Table;
import java.util.UUID;

@Entity
@Table(name = "attachment_order_table")
public class AttachmentOrder extends Order {
    private static final long serialVersionUID = 1L;
    /**
     * this attribute refers to the instance of the computer where the volume will be attached
     */
    @Column
    private String source;
    /**
     * this attribute refers to the instanceId of the target volume of the attachment
     */
    @Column
    private String target;
    /**
     * this attribute refers to the mount point of the volume device
     */
    @Column
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

	@Override
	public String getSpec() {
		return "";
	}
}
