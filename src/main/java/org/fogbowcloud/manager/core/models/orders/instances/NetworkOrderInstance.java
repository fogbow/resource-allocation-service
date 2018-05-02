package org.fogbowcloud.manager.core.models.orders.instances;

public class NetworkOrderInstance extends OrderInstance {

    private String label;
    private String address;
    private String gateway;
    private String VLAN;
    private String networkInterface;
    private String MACInterface;
    private InstanceState interfaceState;

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
