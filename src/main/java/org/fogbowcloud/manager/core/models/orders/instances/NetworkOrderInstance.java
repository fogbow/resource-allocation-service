package org.fogbowcloud.manager.core.models.orders.instances;

public class NetworkOrderInstance extends OrderInstance {

    private String label;
    private InstanceState state;
    private String address;
    private String gateway;
    private String VLAN;
    private String networkInterface;
    private String MACInterface;
    private InstanceState interfaceState;

    public NetworkOrderInstance(String id, String label, InstanceState state, String address, String gateway, String VLAN, String networkInterface, String MACInterface, InstanceState interfaceState) {
        super(id);
        this.label = label;
        this.state = state;
        this.address = address;
        this.gateway = gateway;
        this.VLAN = VLAN;
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

    public InstanceState getState() {
        return state;
    }

    public void setState(InstanceState state) {
        this.state = state;
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

    public String getVLAN() {
        return VLAN;
    }

    public void setVLAN(String VLAN) {
        this.VLAN = VLAN;
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
