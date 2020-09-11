package cloud.fogbow.ras.core.plugins.interoperability.emulatedcloud.sdk.securityrule;

import cloud.fogbow.ras.core.plugins.interoperability.emulatedcloud.EmulatedCloudConstants;
import cloud.fogbow.ras.core.plugins.interoperability.emulatedcloud.EmulatedCloudUtils;
import cloud.fogbow.ras.core.plugins.interoperability.emulatedcloud.sdk.ResourceManager;
import cloud.fogbow.ras.core.plugins.interoperability.emulatedcloud.sdk.securityrule.models.EmulatedSecurityRule;

import java.security.InvalidParameterException;
import java.util.*;
import java.util.stream.Collectors;

public class EmulatedCloudSecurityRuleManager implements ResourceManager<EmulatedSecurityRule> {
    private static EmulatedCloudSecurityRuleManager instance;
    private Map<String, EmulatedSecurityRule> securityRules;

    private EmulatedCloudSecurityRuleManager() {
        this.securityRules = new HashMap<>();
    }

    public static EmulatedCloudSecurityRuleManager getInstance() {
        if (instance == null) {
            instance = new EmulatedCloudSecurityRuleManager();
        }
        return instance;
    }

    @Override
    public Optional<EmulatedSecurityRule> find(String instanceId) {
        return Optional.ofNullable(this.securityRules.get(instanceId));
    }

    @Override
    public List<EmulatedSecurityRule> list() {
        return new ArrayList<>(this.securityRules.values());
    }

    public List<EmulatedSecurityRule> listBySecurityGroup(String securityGroupId) {
        return this.securityRules.values().stream()
                .filter(securityRule -> securityRule.getSecurityGroupId().equals(securityGroupId))
                .collect(Collectors.toList());
    }

    public void deleteBySecurityGroup(String securityGroupId) {
        securityRules.entrySet().removeIf(entry -> {
            EmulatedSecurityRule securityRule = entry.getValue();
            return securityRule.getSecurityGroupId().equals(securityGroupId);
        });
    }

    @Override
    public String create(EmulatedSecurityRule securityRules) {
        EmulatedCloudUtils.validateEmulatedResource(securityRules);
        this.securityRules.put(securityRules.getInstanceId(), securityRules);
        return securityRules.getInstanceId();
    }

    @Override
    public void delete(String instanceId) {
        if (this.securityRules.containsKey(instanceId)) {
            this.securityRules.remove(instanceId);
        } else {
            throw new InvalidParameterException(EmulatedCloudConstants.Exception.RESOURCE_NOT_FOUND);
        }
    }
}
