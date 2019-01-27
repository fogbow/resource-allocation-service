package cloud.fogbow.ras.core.models.orders;

import cloud.fogbow.common.models.FederationUser;
import cloud.fogbow.ras.core.models.NetworkAllocationMode;
import cloud.fogbow.ras.core.models.ResourceType;

import javax.persistence.*;
import java.util.UUID;

@Entity
@Table(name = "network_order_table")
public class NetworkOrder extends Order {
    private static final long serialVersionUID = 1L;
    @Column
    private String name;
    @Column
    private String gateway;
    @Column
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

    public NetworkOrder(FederationUser federationUser, String requestingMember, String providingMember,
                        String cloudName, String name, String gateway, String cidr, NetworkAllocationMode allocationMode) {
        super(UUID.randomUUID().toString(), providingMember, cloudName, federationUser, requestingMember);
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
}
