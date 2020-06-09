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
public class NetworkOrder extends Order<NetworkOrder> {
    private static final long serialVersionUID = 1L;

    @Transient
    private transient final Logger LOGGER = Logger.getLogger(NetworkOrder.class);

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
        this.type = ResourceType.NETWORK;
    }

    public NetworkOrder(String id) {
        super(id);
        this.type = ResourceType.NETWORK;
    }

    public NetworkOrder(String providingProvider, String cloudName, String name, String gateway, String cidr,
                        NetworkAllocationMode allocationMode) {
        this(null, null, providingProvider, cloudName, name, gateway, cidr, allocationMode);
        this.type = ResourceType.NETWORK;
    }

    public NetworkOrder(SystemUser systemUser, String requestingProvider, String providingProvider,
                        String cloudName, String name, String gateway, String cidr, NetworkAllocationMode allocationMode) {
        super(UUID.randomUUID().toString(), providingProvider, cloudName, systemUser, requestingProvider);
        this.name = name;
        this.gateway = gateway;
        this.cidr = cidr;
        this.allocationMode = allocationMode;
        this.type = ResourceType.NETWORK;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
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
    public void updateFromRemote(NetworkOrder remoteOrder) { }

}
