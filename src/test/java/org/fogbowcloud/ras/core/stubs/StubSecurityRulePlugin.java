package org.fogbowcloud.ras.core.stubs;

import org.fogbowcloud.ras.core.exceptions.FogbowRasException;
import org.fogbowcloud.ras.core.exceptions.UnexpectedException;
import org.fogbowcloud.ras.core.models.orders.Order;
import org.fogbowcloud.ras.core.models.securityrules.SecurityRule;
import org.fogbowcloud.ras.core.models.tokens.Token;
import org.fogbowcloud.ras.core.plugins.interoperability.SecurityRulePlugin;

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
