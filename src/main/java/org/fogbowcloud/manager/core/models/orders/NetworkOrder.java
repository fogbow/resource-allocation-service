package org.fogbowcloud.manager.core.models.orders;

import org.fogbowcloud.manager.core.models.orders.instances.OrderInstance;
import org.fogbowcloud.manager.core.models.token.Token;

public class NetworkOrder extends Order {

    private String gateway;
    private String address;
    private String allocation;

    public NetworkOrder(String id, OrderState orderState, Token localToken, Token federationToken,
                        String requestingMember, String providingMember, OrderInstance orderInstance, long fulfilledTime) {
        super(id, orderState, localToken, federationToken, requestingMember, providingMember, orderInstance, fulfilledTime);
    }

    public String getGateway() {
        return gateway;
    }

    public void setGateway(String gateway) {
        this.gateway = gateway;
    }

    public String getAddress() {
        return address;
    }

    public void setAddress(String address) {
        this.address = address;
    }

    public String getAllocation() {
        return allocation;
    }

    public void setAllocation(String allocation) {
        this.allocation = allocation;
    }
}
