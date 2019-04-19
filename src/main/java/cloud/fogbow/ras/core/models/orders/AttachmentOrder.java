package cloud.fogbow.ras.core.models.orders;

import cloud.fogbow.common.models.SystemUser;
import cloud.fogbow.ras.core.SharedOrderHolders;
import cloud.fogbow.ras.core.models.ResourceType;
import org.apache.log4j.Logger;

import javax.persistence.*;
import javax.validation.constraints.Size;
import java.util.UUID;

@Entity
@Table(name = "attachment_order_table")
public class AttachmentOrder extends Order<AttachmentOrder> {
    private static final long serialVersionUID = 1L;

    private static final String DEVICE_COLUMN_NAME = "device";

    @Transient
    private transient final Logger LOGGER = Logger.getLogger(AttachmentOrder.class);

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
        ComputeOrder computeOrder = (ComputeOrder) SharedOrderHolders.getInstance().getActiveOrdersMap().get(this.computeOrderId);
        if (computeOrder == null) {
            return null;
        } else {
            return computeOrder.getInstanceId();
        }
    }

    public String getVolumeId() {
        VolumeOrder volumeOrder = (VolumeOrder) SharedOrderHolders.getInstance().getActiveOrdersMap().get(this.volumeOrderId);
        if (volumeOrder == null) {
            return null;
        } else {
            return volumeOrder.getInstanceId();
        }
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
        return "";
    }

    @Override
    public void updateFromRemote(AttachmentOrder remoteOrder) {
    }
}
