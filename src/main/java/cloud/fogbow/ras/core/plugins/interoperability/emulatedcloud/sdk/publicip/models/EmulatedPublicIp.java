package cloud.fogbow.ras.core.plugins.interoperability.emulatedcloud.sdk.publicip.models;

import cloud.fogbow.ras.core.plugins.interoperability.emulatedcloud.sdk.EmulatedResource;

public class EmulatedPublicIp extends EmulatedResource {
    private String cloudState;
    private String computeId;
    private String cloudName;
    private String ip;
    private String provider;

    private EmulatedPublicIp(String instanceId, String cloudState, String computeId,
                             String ip, String provider, String cloudName) {
        super(instanceId);
        this.cloudState = cloudState;
        this.computeId = computeId;
        this.cloudName = cloudName;
        this.ip = ip;
        this.provider = provider;
    }

    public static class Builder {
        private String cloudState;
        private String computeId;
        private String cloudName;
        private String instanceId;
        private String ip;
        private String provider;

        public Builder cloudState(String cloudState) {
            this.cloudState = cloudState;
            return this;
        }

        public Builder computeId(String computeId) {
            this.computeId = computeId;
            return this;
        }

        public Builder cloudName(String cloudName) {
            this.cloudName = cloudName;
            return this;
        }

        public Builder instanceId(String instanceId) {
            this.instanceId = instanceId;
            return this;
        }

        public Builder ip(String ip) {
            this.ip = ip;
            return this;
        }

        public Builder provider(String provider) {
            this.provider = provider;
            return this;
        }

        public EmulatedPublicIp build() {
            return new EmulatedPublicIp(instanceId, cloudState, computeId, ip, provider, cloudName);
        }
    }

    public String getCloudState() {
        return cloudState;
    }

    public String getComputeId() {
        return computeId;
    }

    public String getCloudName() {
        return cloudName;
    }

    public String getIp() {
        return ip;
    }

    public String getProvider() {
        return provider;
    }
}