package cloud.fogbow.ras.core.models.orders;

import cloud.fogbow.common.models.FederationUser;
import cloud.fogbow.ras.core.models.ResourceType;
import org.apache.log4j.Logger;

import javax.persistence.*;
import java.util.UUID;

@Entity
@Table(name = "public_ip_order_table")
public class PublicIpOrder extends Order {
    private static final long serialVersionUID = 1L;

    @Transient
    private transient static final Logger LOGGER = Logger.getLogger(PublicIpOrder.class);

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

    public PublicIpOrder(FederationUser federationUser, String requestingMember,
                         String providingMember, String cloudName, String computeOrderId) {
        super(UUID.randomUUID().toString(), providingMember, cloudName, federationUser, requestingMember);
        this.computeOrderId = computeOrderId;
    }

    @Override
    public ResourceType getType() {
        return ResourceType.PUBLIC_IP;
    }

    public String getComputeOrderId() {
        return computeOrderId;
    }

    @Override
    public String getSpec() {
        return "";
    }

    @Override
    public Logger getLogger() {
        return LOGGER;
    }

}
