package cloud.fogbow.ras.api.http.response;

import cloud.fogbow.ras.core.models.NetworkAllocationMode;

public class NetworkInstance extends OrderInstance {
    private String name;
    private String cidr;
    private String gateway;
    private NetworkAllocationMode allocationMode;
    private String vLAN;
    private String networkInterface;
    private String MACInterface;
    private String interfaceState;

    public NetworkInstance(String id, String cloudState, String name, String cidr, String gateway,
                           String vLAN, NetworkAllocationMode networkAllocationMode, String networkInterface,
                           String MACInterface, String interfaceState) {
        super(id, cloudState);
        this.name = name;
        this.cidr = cidr;
        this.gateway = gateway;
        this.vLAN = vLAN;
        this.allocationMode = networkAllocationMode;
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

    public String getCidr() {
        return cidr;
    }

    public String getGateway() {
        return gateway;
    }

    public String getvLAN() {
        return vLAN;
    }

    public NetworkAllocationMode getAllocationMode() {
        return allocationMode;
    }

    public void setAllocationMode(NetworkAllocationMode allocationMode) {
        this.allocationMode = allocationMode;
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
