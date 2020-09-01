package cloud.fogbow.ras.core.plugins.interoperability.emulatedcloud.emulatedmodels;

import cloud.fogbow.common.util.GsonHolder;
import cloud.fogbow.common.util.JsonSerializable;
import com.google.gson.annotations.SerializedName;

import java.util.ArrayList;
import java.util.List;

import static cloud.fogbow.ras.core.plugins.interoperability.emulatedcloud.EmulatedCloudConstants.Json.SECURITY_RULES_KEY_JSON;
import static cloud.fogbow.ras.core.plugins.interoperability.emulatedcloud.EmulatedCloudConstants.Json.ID_KEY_JSON;

public class EmulatedSecurityGroup implements JsonSerializable {

    @SerializedName(ID_KEY_JSON)
    private String id;

    @SerializedName(SECURITY_RULES_KEY_JSON)
    private List<String> securityRules;

    private EmulatedSecurityGroup(String id, List<String> securityRules) {
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

    public List<String> getSecurityRules() {
        return securityRules;
    }

    public void addSecurityRule(String securityRuleId) {
        if (securityRules == null) {
            securityRules = new ArrayList<>();
        }

        securityRules.add(securityRuleId);
    }

    public void removeSecurityRule(String securityRuleId) {
        this.securityRules.remove(securityRuleId);
    }

    public static class Builder {
        private String id;
        private List<String> securityRules;

        public Builder id(String id) {
            this.id = id;
            return this;
        }

        public Builder securityRules(List<String> securityRules) {
            this.securityRules = securityRules;
            return this;
        }

        public EmulatedSecurityGroup build() {
            return new EmulatedSecurityGroup(id, securityRules);
        }
    }
}
