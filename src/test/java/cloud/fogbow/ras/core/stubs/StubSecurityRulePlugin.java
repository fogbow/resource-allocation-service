package cloud.fogbow.ras.core.stubs;

import cloud.fogbow.common.models.CloudUser;
import cloud.fogbow.ras.api.parameters.SecurityRule;
import cloud.fogbow.ras.core.models.orders.Order;
import cloud.fogbow.ras.api.http.response.SecurityRuleInstance;
import cloud.fogbow.ras.core.plugins.interoperability.SecurityRulePlugin;

import java.util.List;

/**
 * This class is a stub for the SecurityRulePlugin interface used for tests only.
 * Should not have a proper implementation.
 */
public class StubSecurityRulePlugin implements SecurityRulePlugin {

    public StubSecurityRulePlugin(String confFilePath) {
    }

    @Override
    public String requestSecurityRule(SecurityRule securityRule, Order majorOrder,
                                      CloudUser cloudUser) {
        return null;
    }

    @Override
    public List<SecurityRuleInstance> getSecurityRules(Order majorOrder,
                                                       CloudUser cloudUser) {
        return null;
    }

    @Override
    public void deleteSecurityRule(String securityRuleId, CloudUser cloudUser) {

    }
}
