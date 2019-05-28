package cloud.fogbow.ras.core.plugins.interoperability.emulatedcloud.emulatedmodels;

import cloud.fogbow.common.util.GsonHolder;
import cloud.fogbow.common.util.JsonSerializable;
import com.google.gson.annotations.SerializedName;

import java.util.ArrayList;
import java.util.List;

import static cloud.fogbow.ras.core.plugins.interoperability.emulatedcloud.EmulatedCloudConstants.Json.SECURITY_RULES_KEY_JSON;

public class EmulatedOrderWithSecurityRule implements JsonSerializable {
    @SerializedName(SECURITY_RULES_KEY_JSON)
    protected List<EmulatedSecurityRule> securityRules = null;

    @Override
    public String toJson() {
        return GsonHolder.getInstance().toJson(this);
    }

    public static EmulatedOrderWithSecurityRule fromJson(String json) {
        return GsonHolder.getInstance().fromJson(json, EmulatedOrderWithSecurityRule.class);
    }

    public List<EmulatedSecurityRule> getSecurityRules() {
        return securityRules;
    }

    public void addSecurityRule(EmulatedSecurityRule securityRule) {

        if (securityRules == null) {
            securityRules = new ArrayList<EmulatedSecurityRule>();
        }

        securityRules.add(securityRule);
    }
}
