package cloud.fogbow.ras.core.models.orders;

import cloud.fogbow.common.models.SystemUser;
import cloud.fogbow.ras.core.models.ResourceType;
import org.apache.log4j.Logger;

import javax.persistence.*;
import javax.validation.constraints.Size;
import java.util.UUID;

@Entity
@Table(name = "volume_order_table")
public class VolumeOrder extends Order<VolumeOrder> {
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
        this.type = ResourceType.VOLUME;
    }

    public VolumeOrder(String id) {
        super(id);
        this.type = ResourceType.VOLUME;
    }

    public VolumeOrder(String providingMember, String cloudName, String name, int volumeSize) {
        this(null, null, providingMember, cloudName, name, volumeSize);
        this.type = ResourceType.VOLUME;
    }

    public VolumeOrder(SystemUser systemUser, String requestingMember, String providingMember,
                       String cloudName, String name, int volumeSize) {
        super(UUID.randomUUID().toString(), providingMember, cloudName, systemUser, requestingMember);
        this.name = name;
        this.volumeSize = volumeSize;
        this.type = ResourceType.VOLUME;
    }

    public int getVolumeSize() {
        return volumeSize;
    }

    public String getName() {
        return name;
    }

    @Override
    public String getSpec() {
        return String.valueOf(this.volumeSize);
    }

    @Override
    public void updateFromRemote(VolumeOrder remoteOrder) {
    }
}
