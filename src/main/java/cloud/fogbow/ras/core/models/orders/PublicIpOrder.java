package cloud.fogbow.ras.core.models.orders;

import cloud.fogbow.common.models.SystemUser;
import cloud.fogbow.ras.core.SharedOrderHolders;
import cloud.fogbow.ras.core.models.ResourceType;
import org.apache.log4j.Logger;

import javax.persistence.*;
import javax.validation.constraints.Size;
import java.util.UUID;

@Entity
@Table(name = "public_ip_order_table")
public class PublicIpOrder extends Order<PublicIpOrder> {
    private static final long serialVersionUID = 1L;

    @Transient
    private transient static final Logger LOGGER = Logger.getLogger(PublicIpOrder.class);

    @Size(max = Order.ID_FIXED_SIZE)
    @Column
    private String computeOrderId;

    public PublicIpOrder() {
        this(UUID.randomUUID().toString());
        this.type = ResourceType.PUBLIC_IP;
    }

    public PublicIpOrder(String computeOrderId) {
        this(null, null, null, null, computeOrderId);
        this.type = ResourceType.PUBLIC_IP;
        ComputeOrder computeOrder = (ComputeOrder) SharedOrderHolders.getInstance().getActiveOrdersMap().get(computeOrderId);
        setProvider(computeOrder == null ? null : computeOrder.getProvider());
        setCloudName(computeOrder == null ? null : computeOrder.getCloudName());

    }

    public PublicIpOrder(SystemUser systemUser, String requestingProvider, String providingProvider, String cloudName,
                         String computeOrderId) {
        super(UUID.randomUUID().toString(), providingProvider, cloudName, systemUser, requestingProvider);
        this.computeOrderId = computeOrderId;
        this.type = ResourceType.PUBLIC_IP;
    }

    public String getComputeId() {
        ComputeOrder computeOrder = (ComputeOrder) SharedOrderHolders.getInstance().getActiveOrdersMap().get(this.computeOrderId);
        if (computeOrder == null) {
            return null;
        } else {
            return computeOrder.getInstanceId();
        }
    }

    public void setComputeOrderId(String computeOrderId) {
        this.computeOrderId = computeOrderId;
    }

    public String getComputeOrderId() {
        return computeOrderId;
    }

    @Override
    public void updateFromRemote(PublicIpOrder remoteOrder) {
        this.setFaultMessage(remoteOrder.getFaultMessage());
    }

}
