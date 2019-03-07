package cloud.fogbow.ras.core.models.orders;

import cloud.fogbow.common.models.SystemUser;
import cloud.fogbow.ras.core.models.ResourceType;
import org.apache.log4j.Logger;

import javax.persistence.*;
import javax.validation.constraints.Size;
import java.util.UUID;

@Entity
@Table(name = "volume_order_table")
public class VolumeOrder extends Order {
    private static final long serialVersionUID = 1L;

    private static final String NAME_COLUMN_NAME = "name";

    @Transient
    private transient final Logger LOGGER = Logger.getLogger(VolumeOrder.class);

    @Column
    private int volumeSize;

    @Size(max = Order.FIELDS_MAX_SIZE)
    @Column(name = NAME_COLUMN_NAME)
    private String name;

    public VolumeOrder() {
        this(UUID.randomUUID().toString());
    }

    public VolumeOrder(String id) {
        super(id);
    }

    public VolumeOrder(String providingMember, String cloudName, String name, int volumeSize) {
        this(null, null, providingMember, cloudName, name, volumeSize);
    }

    public VolumeOrder(SystemUser systemUser, String requestingMember, String providingMember,
                       String cloudName, String name, int volumeSize) {
        super(UUID.randomUUID().toString(), providingMember, cloudName, systemUser, requestingMember);
        this.name = name;
        this.volumeSize = volumeSize;
    }

    public int getVolumeSize() {
        return volumeSize;
    }

    public String getName() {
        return name;
    }

    @Override
    public ResourceType getType() {
        return ResourceType.VOLUME;
    }

    @Override
    public String getSpec() {
        return String.valueOf(this.volumeSize);
    }

    @Override
    public Logger getLogger() {
        return LOGGER;
    }

    @PrePersist
    private void checkColumnsSizes() {
        this.name = treatValue(this.name, NAME_COLUMN_NAME, Order.FIELDS_MAX_SIZE);
    }
}
