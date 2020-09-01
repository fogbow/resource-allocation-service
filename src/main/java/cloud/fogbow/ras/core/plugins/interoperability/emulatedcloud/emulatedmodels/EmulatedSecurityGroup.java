package cloud.fogbow.ras.core.plugins.interoperability.emulatedcloud.emulatedmodels;

import cloud.fogbow.common.util.GsonHolder;
import cloud.fogbow.common.util.JsonSerializable;
import com.google.gson.annotations.SerializedName;

import java.util.List;

import static cloud.fogbow.ras.core.plugins.interoperability.emulatedcloud.EmulatedCloudConstants.Json.SECURITY_RULES_KEY_JSON;
import static cloud.fogbow.ras.core.plugins.interoperability.emulatedcloud.EmulatedCloudConstants.Json.ID_KEY_JSON;

public class EmulatedSecurityGroup implements JsonSerializable {

    @SerializedName(ID_KEY_JSON)
    private String id;

    @SerializedName(SECURITY_RULES_KEY_JSON)
    private List<EmulatedSecurityRule> securityRules;

    private EmulatedSecurityGroup(String id, List<EmulatedSecurityRule> securityRules) {
        this.id = id;
        this.securityRules = securityRules;
    }

    @Override
    public String toJson() {
        return GsonHolder.getInstance().toJson(this);
    }

    public static EmulatedSecurityGroup fromJson(String json) {
        return GsonHolder.getInstance().fromJson(json, EmulatedSecurityGroup.class);
    }

    public String getId() {
        return id;
    }

    public List<EmulatedSecurityRule> getSecurityRules() {
        return securityRules;
    }

    public static class Builder {
        private String id;
        private List<EmulatedSecurityRule> securityRules;

        public Builder id(String id) {
            this.id = id;
            return this;
        }

        public Builder securityRules(List<EmulatedSecurityRule> securityRules) {
            this.securityRules = securityRules;
            return this;
        }

        public EmulatedSecurityGroup build() {
            return new EmulatedSecurityGroup(id, securityRules);
        }
    }
}
