package org.fogbowcloud.ras.api.parameters;

import org.fogbowcloud.ras.core.models.orders.NetworkAllocationMode;
import org.fogbowcloud.ras.core.models.orders.NetworkOrder;

public class Network {
    public static final String allocationAllowableValues = getAllocationAllowableValues();
    private String provider;
    private String name;
    private String gateway;
    private String address;
    private NetworkAllocationMode allocation;

    public NetworkOrder getOrder() {
        NetworkOrder order = new NetworkOrder(provider, name, gateway, address, allocation);
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

    public String getAddress() {
        return address;
    }

    public NetworkAllocationMode getAllocation() {
        return allocation;
    }

    private static String getAllocationAllowableValues() {
        // TODO get these from NetworkAllocationMode.values()
        return "dynamic,static";
    }
}
