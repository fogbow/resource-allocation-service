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
    @Size(max = Order.FIELDS_MAX_SIZE)
    @Column
    private String computeId;

    // this attribute refers to the volumeId of the volume that will be attached attachment
    @Size(max = Order.FIELDS_MAX_SIZE)
    @Column
    private String volumeId;

    // this attribute refers to the mount point of the volume device
    @Size(max = Order.FIELDS_MAX_SIZE)
    @Column(name = DEVICE_COLUMN_NAME)
    private String device;

    // this attribute refers to the order associated to the computeId
    @Size(max = Order.ID_FIXED_SIZE)
    @Column
    private String computeOrderId;

    // this attribute refers to the order associated to the volumeId
    @Size(max = Order.ID_FIXED_SIZE)
    @Column
    private String volumeOrderId;

    public AttachmentOrder() {
        this(UUID.randomUUID().toString());
        this.type = ResourceType.ATTACHMENT;
    }

    public AttachmentOrder(String id) {
        super(id);
        this.type = ResourceType.ATTACHMENT;
    }

    public AttachmentOrder(String providingMember, String cloudName, String computeOrderId, String volumeOrderId,
                           String device) {
        this(null, null, providingMember, cloudName, computeOrderId, volumeOrderId, device);
        this.type = ResourceType.ATTACHMENT;
    }

    public AttachmentOrder(SystemUser systemUser, String requestingMember, String providingMember, String cloudName,
                           String computeOrderId, String volumeOrderId, String device) {
        super(UUID.randomUUID().toString(), providingMember, cloudName, systemUser, requestingMember);
        this.computeOrderId = computeOrderId;
        this.volumeOrderId = volumeOrderId;
        this.device = device;
        this.type = ResourceType.ATTACHMENT;
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

    public String getComputeOrderId() {
        return computeOrderId;
    }

    public String getVolumeOrderId() {
        return volumeOrderId;
    }

    @Override
    public String getSpec() {
        // TODO
        return "";
    }
}
