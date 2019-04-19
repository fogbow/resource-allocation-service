package cloud.fogbow.ras.core.models.orders;

import cloud.fogbow.common.models.SystemUser;
import cloud.fogbow.ras.core.SharedOrderHolders;
import cloud.fogbow.ras.core.models.ResourceType;
import org.apache.log4j.Logger;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Table;
import javax.persistence.Transient;
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

    public PublicIpOrder(String id) {
        super(id);
        this.type = ResourceType.PUBLIC_IP;
    }

    public PublicIpOrder(String providingMember, String cloudName, String computeOrderId) {
        this(null, null, providingMember, cloudName, computeOrderId);
        this.type = ResourceType.PUBLIC_IP;
    }

    public PublicIpOrder(SystemUser systemUser, String requestingMember, String providingMember, String cloudName,
                         String computeOrderId) {
        super(UUID.randomUUID().toString(), providingMember, cloudName, systemUser, requestingMember);
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
    public String getSpec() {
        return "";
    }

    @Override
    public void updateFromRemote(PublicIpOrder remoteOrder) {
    }
}
