package org.fogbowcloud.ras.api.parameters;

import io.swagger.annotations.ApiParam;
import io.swagger.annotations.Example;
import org.fogbowcloud.ras.core.models.orders.NetworkAllocationMode;

public class Network {

    public static final String allocationAllowableValues = getAllocationAllowableValues();

    private String name;
    private String gateway;
    private String address;
    private NetworkAllocationMode allocation;

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
