package org.fogbowcloud.ras.api.parameters;

import org.fogbowcloud.ras.core.models.orders.NetworkAllocationMode;
import org.fogbowcloud.ras.core.models.orders.NetworkOrder;

public class Network {
    private String provider;
    private String name;
    private String gateway;
    private String cidr;
    private NetworkAllocationMode allocationMode;

    public NetworkOrder getOrder() {
        NetworkOrder order = new NetworkOrder(provider, name, gateway, cidr, allocationMode);
        return order;
    }

    public String getProvider() {
        return provider;
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
}
