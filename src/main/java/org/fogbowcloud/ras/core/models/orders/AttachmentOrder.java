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
    private String computeId;
    /**
     * this attribute refers to the volumeId of the volume that will be attached attachment
     */
    @Column
    private String volumeId;
    /**
     * this attribute refers to the mount point of the volume device
     */
    @Column
    private String device;

    public AttachmentOrder() {
        this(UUID.randomUUID().toString());
    }

    public AttachmentOrder(String id) {
        super(id);
    }

    public AttachmentOrder(String providingMember, String computeId, String volumeId, String device) {
        this(null, null, providingMember, computeId, volumeId, device);
    }

    public AttachmentOrder(FederationUserToken federationUserToken, String requestingMember,
                           String providingMember, String computeId, String volumeId, String device) {
        super(UUID.randomUUID().toString(), providingMember, federationUserToken, requestingMember);
        this.computeId = computeId;
        this.volumeId = volumeId;
        this.device = device;
    }

    public String getComputeId() {
        return this.computeId;
    }

    public void setComputeId(String computeId) {
        this.computeId = computeId;
    }

    public String getVolumeId() {
        return this.volumeId;
    }

    public void setVolumeId(String volumeId) {
        this.volumeId = volumeId;
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
