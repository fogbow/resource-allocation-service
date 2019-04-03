package cloud.fogbow.ras.core.models.orders;

import cloud.fogbow.common.models.SystemUser;
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
public class PublicIpOrder extends Order {
    private static final long serialVersionUID = 1L;

    @Transient
    private transient static final Logger LOGGER = Logger.getLogger(PublicIpOrder.class);

    @Size(max = Order.ID_FIXED_SIZE)
    @Column
    private String computeOrderId;

    public PublicIpOrder() {
        this(UUID.randomUUID().toString());
    }

    public PublicIpOrder(String id) {
        super(id);
    }

    public PublicIpOrder(String providingMember, String cloudName, String computeOrderId) {
        this(null, null, providingMember, cloudName, computeOrderId);
    }

    public PublicIpOrder(SystemUser systemUser, String requestingMember,
                         String providingMember, String cloudName, String computeOrderId) {
        super(UUID.randomUUID().toString(), providingMember, cloudName, systemUser, requestingMember);
        this.computeOrderId = computeOrderId;
    }

    public String getComputeOrderId() {
        return computeOrderId;
    }

    @Override
    public String getSpec() {
        return "";
    }
}
