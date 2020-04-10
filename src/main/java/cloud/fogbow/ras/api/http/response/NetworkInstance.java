package cloud.fogbow.ras.api.http.response;

import cloud.fogbow.ras.constants.ApiDocumentation;
import cloud.fogbow.ras.core.models.NetworkAllocationMode;
import io.swagger.annotations.ApiModelProperty;

public class NetworkInstance extends OrderInstance {
    @ApiModelProperty(position = 7, example = ApiDocumentation.Model.NETWORK_NAME)
    private String name;
    @ApiModelProperty(position = 8, example = "10.10.0.0/16")
    private String cidr;
    @ApiModelProperty(position = 9, example = "10.10.0.1")
    private String gateway;
    @ApiModelProperty(position = 10, example = "dynamic")
    private NetworkAllocationMode allocationMode;
    @ApiModelProperty(position = 11, example = "1200")
    private String vLAN;
    @ApiModelProperty(position = 11, example = "8415ca5b-6f11-4225-884e-543ed9ab9eed")
    private String networkInterface;
    @ApiModelProperty(position = 11, example = "fa:16:3e:42:4b:19")
    private String MACInterface;
    @ApiModelProperty(position = 11, example = "Up")
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

    public NetworkInstance(String id, String cloudState) {
        super(id, cloudState);
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
