package org.fogbowcloud.manager.core.models.orders.instances;

import org.fogbowcloud.manager.core.models.orders.NetworkAllocation;

public class NetworkOrderInstance extends OrderInstance {

    private String label;
    private String address;
    private String gateway;
    private String vLAN;
    private NetworkAllocation allocation;
    private String networkInterface;
    private String MACInterface;
    private InstanceState interfaceState;

    public NetworkOrderInstance(
            String id,
            String label,
            InstanceState state,
            String address,
            String gateway,
            String vLAN,
            NetworkAllocation networkAllocation,
            String networkInterface,
            String MACInterface,
            InstanceState interfaceState) {
        super(id, state);
        this.label = label;
        this.address = address;
        this.gateway = gateway;
        this.vLAN = vLAN;
        this.allocation = networkAllocation;
        this.networkInterface = networkInterface;
        this.MACInterface = MACInterface;
        this.interfaceState = interfaceState;
    }

    public String getLabel() {
        return label;
    }

    public void setLabel(String label) {
        this.label = label;
    }

    public String getAddress() {
        return address;
    }

    public void setAddress(String address) {
        this.address = address;
    }

    public String getGateway() {
        return gateway;
    }

    public void setGateway(String gateway) {
        this.gateway = gateway;
    }

    public String getvLAN() {
        return vLAN;
    }

    public void setvLAN(String vLAN) {
        this.vLAN = vLAN;
    }

    public NetworkAllocation getAllocation() {
        return allocation;
    }

    public void setAllocation(NetworkAllocation allocation) {
        this.allocation = allocation;
    }

    public String getNetworkInterface() {
        return networkInterface;
    }

    public void setNetworkInterface(String networkInterface) {
        this.networkInterface = networkInterface;
    }

    public String getMACInterface() {
        return MACInterface;
    }

    public void setMACInterface(String MACInterface) {
        this.MACInterface = MACInterface;
    }

    public InstanceState getInterfaceState() {
        return interfaceState;
    }

    public void setInterfaceState(InstanceState interfaceState) {
        this.interfaceState = interfaceState;
    }
}
