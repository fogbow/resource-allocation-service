package cloud.fogbow.ras.core.plugins.interoperability.emulatedcloud.emulatedmodels;

import cloud.fogbow.common.util.GsonHolder;
import cloud.fogbow.common.util.JsonSerializable;
import cloud.fogbow.ras.api.http.response.SecurityRuleInstance;
import cloud.fogbow.ras.api.parameters.SecurityRule;
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

    public void removeSecurityRule(String securityRuleId) {
        this.securityRules.remove(securityRuleId);
    }

    public static List<SecurityRuleInstance> getFogbowSecurityRules(List<EmulatedSecurityRule> securityRules){
        List<SecurityRuleInstance> securityRuleInstances = new ArrayList<>();

        for (EmulatedSecurityRule emulatedSecurityRule: securityRules) {
            String id = emulatedSecurityRule.getId();
            SecurityRule.Direction direction = SecurityRule.Direction.valueOf(emulatedSecurityRule.getDirection());
            int portFrom = emulatedSecurityRule.getPortFrom();
            int portTo = emulatedSecurityRule.getPortTo();
            String cidr = emulatedSecurityRule.getCidr();
            SecurityRule.EtherType etherType = SecurityRule.EtherType.valueOf(emulatedSecurityRule.getEtherType());
            SecurityRule.Protocol protocol = SecurityRule.Protocol.valueOf(emulatedSecurityRule.getProtocol());

            SecurityRuleInstance securityRuleInstance = new SecurityRuleInstance(id, direction, portFrom, portTo, cidr, etherType, protocol);
            securityRuleInstances.add(securityRuleInstance);
        }

        return securityRuleInstances;
    }
}
