package org.fogbowcloud.manager.core.models.orders;

import java.util.UUID;

import org.fogbowcloud.manager.core.models.instances.InstanceType;
import org.fogbowcloud.manager.core.models.token.FederationUser;

public class NetworkOrder extends Order {

    private String gateway;
    private String address;
    private NetworkAllocation allocation;

    public NetworkOrder() {
        super(UUID.randomUUID().toString());
    }
    
    /** Creating Order with predefined Id. */
    public NetworkOrder(
            String id,
            FederationUser federationUser,
            String requestingMember,
            String providingMember,
            String gateway,
            String address,
            NetworkAllocation allocation) {
        super(id, federationUser, requestingMember, providingMember);
        this.gateway = gateway;
        this.address = address;
        this.allocation = allocation;
    }

    public NetworkOrder(
            FederationUser federationUser,
            String requestingMember,
            String providingMember,
            String gateway,
            String address,
            NetworkAllocation allocation) {
        this(
                UUID.randomUUID().toString(),
                federationUser,
                requestingMember,
                providingMember,
                gateway,
                address,
                allocation);
    }

    public String getGateway() {
        return gateway;
    }

    public String getAddress() {
        return address;
    }

    public NetworkAllocation getAllocation() {
        return allocation;
    }

    @Override
    public InstanceType getType() {
        return InstanceType.NETWORK;
    }
}
