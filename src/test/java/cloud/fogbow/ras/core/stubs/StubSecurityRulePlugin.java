package cloud.fogbow.ras.core.stubs;

import cloud.fogbow.ras.core.models.securityrules.SecurityRule;
import cloud.fogbow.ras.core.plugins.interoperability.SecurityRulePlugin;
import cloud.fogbow.ras.core.models.orders.Order;
import org.fogbowcloud.ras.core.models.tokens.Token;

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
                                      Token localUserAttributes) throws FogbowRasException, UnexpectedException {
        return null;
    }

    @Override
    public List<SecurityRule> getSecurityRules(Order majorOrder,
                                               Token localUserAttributes) throws FogbowRasException, UnexpectedException {
        return null;
    }

    @Override
    public void deleteSecurityRule(String securityRuleId, Token token)
            throws FogbowRasException, UnexpectedException {

    }
}
