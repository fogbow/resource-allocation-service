package org.fogbowcloud.ras.core.models.orders;

import org.fogbowcloud.ras.core.models.ResourceType;
import org.fogbowcloud.ras.core.models.tokens.FederationUserToken;

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
    private String address;
    @Column
    @Enumerated(EnumType.STRING)
    private NetworkAllocationMode allocation;

    public NetworkOrder() {
        this(UUID.randomUUID().toString());
    }

    public NetworkOrder(String id) {
        super(id);
    }

    public NetworkOrder(String providingMember, String name, String gateway, String address,
                        NetworkAllocationMode allocation) {
        this(null, null, providingMember, name, gateway, address, allocation);
    }

    public NetworkOrder(FederationUserToken federationUserToken, String requestingMember, String providingMember,
                        String name, String gateway, String address, NetworkAllocationMode allocation) {
        super(UUID.randomUUID().toString(), providingMember, federationUserToken, requestingMember);
        this.name = name;
        this.gateway = gateway;
        this.address = address;
        this.allocation = allocation;
    }

    public String getName() {
        return name;
    }

    public String getGateway() {
        return gateway;
    }

    public String getAddress() {
        return address;
    }

    public NetworkAllocationMode getAllocation() {
        return allocation;
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
