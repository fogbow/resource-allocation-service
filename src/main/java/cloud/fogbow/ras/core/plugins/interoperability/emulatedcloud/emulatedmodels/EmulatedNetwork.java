package cloud.fogbow.ras.core.plugins.interoperability.emulatedcloud.emulatedmodels;

import cloud.fogbow.common.util.GsonHolder;
import cloud.fogbow.common.util.JsonSerializable;
import com.google.gson.annotations.SerializedName;

import static cloud.fogbow.ras.core.plugins.interoperability.emulatedcloud.EmulatedCloudConstants.Json.*;

public class EmulatedNetwork implements JsonSerializable {
    @SerializedName(INSTANCE_ID_KEY_JSON)
    private String instanceId;

    @SerializedName(PROVIDER_KEY_JSON)
    private String provider;

    @SerializedName(CLOUD_STATE_KEY_JSON)
    private String cloudState;

    @SerializedName(CLOUD_NAME_KEY_JSON)
    private String cloudName;

    @SerializedName(NAME_KEY_JSON)
    private String name;

    @SerializedName(CIDR_KEY_JSON)
    private String cidr;

    @SerializedName(ALLOCATION_MODE_KEY_JSON)
    private String allocationMode;

    @SerializedName(GATEWAY_KEY_JSON)
    private String gateway;

    @SerializedName(VLAN_KEY_JSON)
    private String vLAN;

    @SerializedName(NETWORK_INTERFACE_KEY_JSON)
    private String networkInterface;

    @SerializedName(INTERFACE_STATE_KEY_JSON)
    private String interfaceState;

    @SerializedName(MAC_INTERFACE_KEY_JSON)
    private String macInterface;

    private EmulatedNetwork(String instanceId, String provider, String cloudState, String cloudName, String name, String cidr, String allocationMode, String gateway, String vLAN, String networkInterface, String interfaceState, String macInterface) {
        this.instanceId = instanceId;
        this.provider = provider;
        this.cloudState = cloudState;
        this.cloudName = cloudName;
        this.name = name;
        this.cidr = cidr;
        this.allocationMode = allocationMode;
        this.gateway = gateway;
        this.vLAN = vLAN;
        this.networkInterface = networkInterface;
        this.interfaceState = interfaceState;
        this.macInterface = macInterface;
    }

    public static EmulatedNetwork fromJson(String jsonContent) {
        return GsonHolder.getInstance().fromJson(jsonContent, EmulatedNetwork.class);
    }

    @Override
    public String toJson() {
        return GsonHolder.getInstance().toJson(this);
    }

    public String getInstanceId() {
        return instanceId;
    }

    public String getProvider() {
        return provider;
    }

    public String getCloudState() {
        return cloudState;
    }

    public String getCloudName() {
        return cloudName;
    }

    public String getName() {
        return name;
    }

    public String getCidr() {
        return cidr;
    }

    public String getAllocationMode() {
        return allocationMode;
    }

    public String getGateway() {
        return gateway;
    }

    public String getvLAN() {
        return vLAN;
    }

    public String getNetworkInterface() {
        return networkInterface;
    }

    public String getInterfaceState() {
        return interfaceState;
    }

    public String getMacInterface() {
        return macInterface;
    }

    public static class Builder {
        private String instanceId;
        private String provider;
        private String cloudState;
        private String cloudName;
        private String name;
        private String cidr;
        private String allocationMode;
        private String gateway;
        private String vLAN;
        private String networkInterface;
        private String interfaceState;
        private String macInterface;

        public Builder instanceId(String instanceId) {
            this.instanceId = instanceId;
            return this;
        }

        public Builder provider(String provider) {
            this.provider = provider;
            return this;
        }

        public Builder cloudState(String cloudState) {
            this.cloudState = cloudState;
            return this;
        }

        public Builder cloudName(String cloudName) {
            this.cloudName = cloudName;
            return this;
        }

        public Builder name(String name) {
            this.name = name;
            return this;
        }

        public Builder cidr(String cidr) {
            this.cidr = cidr;
            return this;
        }

        public Builder allocationMode(String allocationMode) {
            this.allocationMode = allocationMode;
            return this;
        }

        public Builder gateway(String gateway) {
            this.gateway = gateway;
            return this;
        }

        public Builder vLAN(String vLAN) {
            this.vLAN = vLAN;
            return this;
        }

        public Builder networkInterface(String networkInterface) {
            this.networkInterface = networkInterface;
            return this;
        }

        public Builder interfaceState(String interfaceState) {
            this.interfaceState = interfaceState;
            return this;
        }

        public Builder macInterface(String macInterface) {
            this.macInterface = macInterface;
            return this;
        }

        public EmulatedNetwork build() {
            return new EmulatedNetwork(instanceId, provider, cloudState, cloudName, name, cidr,
                    allocationMode, gateway, vLAN, networkInterface, interfaceState, macInterface);
        }
    }
}
