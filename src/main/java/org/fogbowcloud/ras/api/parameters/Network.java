package org.fogbowcloud.ras.api.parameters;

import org.fogbowcloud.ras.core.models.orders.NetworkAllocationMode;
import org.fogbowcloud.ras.core.models.orders.NetworkOrder;

public class Network implements OrderApiParameter<NetworkOrder> {

    public static final String allocationAllowableValues = getAllocationAllowableValues();

    private String name;
    private String gateway;
    private String address;
    private NetworkAllocationMode allocation;

    @Override
    public NetworkOrder getOrder() {
        return new NetworkOrder(null, null, null,
            name, gateway, address, allocation);
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
