package cloud.fogbow.ras.core.models.orders;

import cloud.fogbow.common.models.SystemUser;
import cloud.fogbow.ras.core.models.ResourceType;
import org.apache.log4j.Logger;

import javax.persistence.*;
import javax.validation.constraints.Size;
import java.util.UUID;

@Entity
@Table(name = "attachment_order_table")
public class AttachmentOrder extends Order {
    private static final long serialVersionUID = 1L;

    private static final String DEVICE_COLUMN_NAME = "device";

    @Transient
    private transient final Logger LOGGER = Logger.getLogger(AttachmentOrder.class);

    // this attribute refers to the instance of the computer where the volume will be attached
    @Column
    @Size(max = Order.ID_FIXED_SIZE)
    private String computeId;

    // this attribute refers to the volumeId of the volume that will be attached attachment
    @Size(max = Order.ID_FIXED_SIZE)
    @Column
    private String volumeId;

    // this attribute refers to the mount point of the volume device
    @Size(max = Order.FIELDS_MAX_SIZE)
    @Column(name = DEVICE_COLUMN_NAME)
    private String device;

    public AttachmentOrder() {
        this(UUID.randomUUID().toString());
    }

    public AttachmentOrder(String id) {
        super(id);
    }

    public AttachmentOrder(String providingMember, String cloudName, String computeId, String volumeId, String device) {
        this(null, null, providingMember, cloudName, computeId, volumeId, device);
    }

    public AttachmentOrder(SystemUser systemUser, String requestingMember,
                           String providingMember, String cloudName, String computeId, String volumeId, String device) {
        super(UUID.randomUUID().toString(), providingMember, cloudName, systemUser, requestingMember);
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
