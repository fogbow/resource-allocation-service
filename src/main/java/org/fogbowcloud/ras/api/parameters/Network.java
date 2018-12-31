package org.fogbowcloud.ras.api.parameters;

import org.fogbowcloud.ras.core.models.NetworkAllocationMode;
import org.fogbowcloud.ras.core.models.orders.NetworkOrder;

public class Network implements OrderApiParameter {
    private String provider;
    private String cloudName;
    private String name;
    private String gateway;
    private String cidr;
    private NetworkAllocationMode allocationMode;

    @Override
    public NetworkOrder getOrder() {
        NetworkOrder order = new NetworkOrder(provider, cloudName, name, gateway, cidr, allocationMode);
        return order;
    }

    public String getProvider() {
        return provider;
    }

    public String getCloudName() {
        return cloudName;
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
