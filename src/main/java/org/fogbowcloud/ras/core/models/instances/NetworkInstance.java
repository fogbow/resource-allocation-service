package org.fogbowcloud.ras.core.models.instances;

import org.fogbowcloud.ras.core.models.orders.NetworkAllocationMode;

public class NetworkInstance extends Instance {
    private String name;
    private String address;
    private String gateway;
    private String vLAN;
    private NetworkAllocationMode allocation;
    private String networkInterface;
    private String MACInterface;
    private String interfaceState;

    public NetworkInstance(String id, InstanceState instanceState, String name, String address, String gateway,
                           String vLAN, NetworkAllocationMode networkAllocationMode, String networkInterface,
                           String MACInterface, String interfaceState) {
        super(id, instanceState);
        this.name = name;
        this.address = address;
        this.gateway = gateway;
        this.vLAN = vLAN;
        this.allocation = networkAllocationMode;
        this.networkInterface = networkInterface;
        this.MACInterface = MACInterface;
        this.interfaceState = interfaceState;
    }

    public NetworkInstance(String id) {
        super(id);
    }

    public String getName() {
        return name;
    }

    public String getAddress() {
        return address;
    }

    public String getGateway() {
        return gateway;
    }

    public String getvLAN() {
        return vLAN;
    }

    public NetworkAllocationMode getAllocation() {
        return allocation;
    }

    public void setAllocation(NetworkAllocationMode allocation) {
        this.allocation = allocation;
    }

    public String getInterfaceState() {
        return this.interfaceState;
    }

    public String getMACInterface() {
        return this.MACInterface;
    }

    public String getNetworkInterface() {
        return this.networkInterface;
    }
}
