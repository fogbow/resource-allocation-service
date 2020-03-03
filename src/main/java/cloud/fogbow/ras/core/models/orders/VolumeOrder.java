package cloud.fogbow.ras.core.models.orders;

import cloud.fogbow.common.models.SystemUser;
import cloud.fogbow.ras.api.http.response.quotas.allocation.VolumeAllocation;
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

    @Embedded
    private VolumeAllocation actualAllocation;

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

    public VolumeOrder(String providingProvider, String cloudName, String name, int volumeSize) {
        this(null, null, providingProvider, cloudName, name, volumeSize);
        this.type = ResourceType.VOLUME;
    }

    public VolumeOrder(SystemUser systemUser, String requestingProvider, String providingProvider,
                       String cloudName, String name, int volumeSize) {
        super(UUID.randomUUID().toString(), providingProvider, cloudName, systemUser, requestingProvider);
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

    public void setName(String name) {
        this.name = name;
    }

    public VolumeAllocation getActualAllocation() {
        return this.actualAllocation;
    }
    
    public void setActualAllocation(VolumeAllocation actualAllocation) {
        this.actualAllocation = actualAllocation;
    }
    
    @Override
    public void updateFromRemote(VolumeOrder remoteOrder) {
        this.setActualAllocation(remoteOrder.getActualAllocation());
    }

}
