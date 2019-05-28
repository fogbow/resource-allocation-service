package cloud.fogbow.ras.core.plugins.interoperability.emulatedcloud.emulatedmodels;

import cloud.fogbow.common.util.GsonHolder;
import com.google.gson.annotations.SerializedName;

import static cloud.fogbow.ras.core.plugins.interoperability.emulatedcloud.EmulatedCloudConstants.Json.*;

public class EmulatedPublicIp {
    @SerializedName(CLOUD_STATE_KEY_JSON)
    private String cloudState;

    @SerializedName(COMPUTE_ID_KEY_JSON)
    private String computeId;

    @SerializedName(CLOUD_NAME_KEY_JSON)
    private String cloudName;

    @SerializedName(INSTANCE_KEY_JSON)
    private String id;

    @SerializedName(FLOATING_IP_KEY_JSON)
    private String ip;

    @SerializedName(PROVIDER_KEY_JSON)
    private String provider;

    @SerializedName(STATE_KEY_JSON)
    private String state;

    private EmulatedPublicIp(String cloudState, String computeId, String cloudName,
                            String id, String ip, String provider, String state) {
        this.cloudState = cloudState;
        this.computeId = computeId;
        this.cloudName = cloudName;
        this.id = id;
        this.ip = ip;
        this.provider = provider;
        this.state = state;
    }

    @Override
    public String toJson() {
        return GsonHolder.getInstance().toJson(this);
    }

    public static EmulatedPublicIp fromJson(String json) {
        return GsonHolder.getInstance().fromJson(json, EmulatedPublicIp.class);
    }

    public static class Builder {
        private String cloudState;
        private String computeId;
        private String cloudName;
        private String id;
        private String ip;
        private String provider;
        private String state;


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

        public Builder id(String id) {
            this.id = id;
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

        public Builder state(String state) {
            this.state = state;
            return this;
        }

        public EmulatedPublicIp build() {
            return new EmulatedPublicIp(cloudState, computeId, cloudName, id, ip, provider, state);
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

    public String getId() {
        return id;
    }

    public String getIp() {
        return ip;
    }

    public String getProvider() {
        return provider;
    }

    public String getState() {
        return state;
    }


}