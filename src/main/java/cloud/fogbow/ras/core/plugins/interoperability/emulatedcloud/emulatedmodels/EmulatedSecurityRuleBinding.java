package cloud.fogbow.ras.core.plugins.interoperability.emulatedcloud.emulatedmodels;

import cloud.fogbow.common.util.GsonHolder;
import cloud.fogbow.common.util.JsonSerializable;
import com.google.gson.annotations.SerializedName;

import static cloud.fogbow.ras.core.plugins.interoperability.emulatedcloud.EmulatedCloudConstants.Json.MAJOR_ORDER_KEY_JSON;

public class EmulatedSecurityRuleBinding implements JsonSerializable {

    @SerializedName(MAJOR_ORDER_KEY_JSON)
    String majorOrderId;

    private EmulatedSecurityRuleBinding(String majorOrderId) {
        this.majorOrderId = majorOrderId;
    }

    @Override
    public String toJson() {
        return GsonHolder.getInstance().toJson(this);
    }

    public static EmulatedSecurityRuleBinding fromJson(String content) {
        return GsonHolder.getInstance().fromJson(content, EmulatedSecurityRuleBinding.class);
    }

    public static class Builder {
        String majorOrderId;

        public Builder majorOrderId(String majorOrderId) {
            this.majorOrderId = majorOrderId;
            return this;
        }

        public EmulatedSecurityRuleBinding build() {
            return new EmulatedSecurityRuleBinding(majorOrderId);
        }
    }

    public String getMajorOrderId() {
        return majorOrderId;
    }
}
