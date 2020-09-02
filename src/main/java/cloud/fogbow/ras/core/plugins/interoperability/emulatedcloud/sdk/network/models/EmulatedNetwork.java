package cloud.fogbow.ras.core.plugins.interoperability.emulatedcloud.sdk.network.models;

import cloud.fogbow.ras.core.plugins.interoperability.emulatedcloud.sdk.EmulatedResource;

public class EmulatedNetwork extends EmulatedResource {
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

    private EmulatedNetwork(String instanceId, String provider, String cloudState, String cloudName, String name, String cidr, String allocationMode, String gateway, String vLAN, String networkInterface, String interfaceState, String macInterface) {
        super(instanceId);
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
