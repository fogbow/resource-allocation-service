package cloud.fogbow.ras.core.models.orders;

import cloud.fogbow.common.models.SystemUser;
import cloud.fogbow.ras.core.models.NetworkAllocationMode;
import cloud.fogbow.ras.core.models.ResourceType;
import org.apache.log4j.Logger;

import javax.persistence.*;
import javax.validation.constraints.Size;
import java.util.UUID;

@Entity
@Table(name = "network_order_table")
public class NetworkOrder extends Order {
    private static final long serialVersionUID = 1L;

    private transient static final Logger LOGGER = Logger.getLogger(NetworkOrder.class);

    private static final String NAME_COLUMN_NAME = "name";
    private static final String GATEWAY_COLUMN_NAME = "gateway";
    private static final String CIDR_COLUMN_NAME = "cidr";

    @Size(max = Order.FIELDS_MAX_SIZE)
    @Column(name = NAME_COLUMN_NAME)
    private String name;

    @Size(max = Order.FIELDS_MAX_SIZE)
    @Column(name = GATEWAY_COLUMN_NAME)
    private String gateway;

    @Size(max = Order.FIELDS_MAX_SIZE)
    @Column(name = CIDR_COLUMN_NAME)
    private String cidr;

    @Column
    @Enumerated(EnumType.STRING)
    private NetworkAllocationMode allocationMode;

    public NetworkOrder() {
        this(UUID.randomUUID().toString());
    }

    public NetworkOrder(String id) {
        super(id);
    }

    public NetworkOrder(String providingMember, String cloudName, String name, String gateway, String cidr,
                        NetworkAllocationMode allocationMode) {
        this(null, null, providingMember, cloudName, name, gateway, cidr, allocationMode);
    }

    public NetworkOrder(SystemUser systemUser, String requestingMember, String providingMember,
                        String cloudName, String name, String gateway, String cidr, NetworkAllocationMode allocationMode) {
        super(UUID.randomUUID().toString(), providingMember, cloudName, systemUser, requestingMember);
        this.name = name;
        this.gateway = gateway;
        this.cidr = cidr;
        this.allocationMode = allocationMode;
    }

    public String getName() {
        return name;
    }

    public String getGateway() {
        return gateway;
    }

    public String getCidr() {
        return cidr;
    }

    public NetworkAllocationMode getAllocationMode() {
        return allocationMode;
    }

    @Override
    public ResourceType getType() {
        return ResourceType.NETWORK;
    }

    @Override
    public String getSpec() {
        return "";
    }

    @Override
    public Logger getLogger() {
        return LOGGER;
    }

    @PrePersist
    protected void checkColumnsSizes() {
        this.name = treatValue(this.name, NAME_COLUMN_NAME, Order.FIELDS_MAX_SIZE);
        this.gateway = treatValue(this.gateway, GATEWAY_COLUMN_NAME, Order.FIELDS_MAX_SIZE);
        this.cidr = treatValue(this.cidr, CIDR_COLUMN_NAME, Order.FIELDS_MAX_SIZE);
    }
}
