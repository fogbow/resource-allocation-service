package org.fogbowcloud.ras.core.plugins.interoperability;

import org.fogbowcloud.ras.core.exceptions.FogbowRasException;
import org.fogbowcloud.ras.core.exceptions.UnexpectedException;
import org.fogbowcloud.ras.core.models.orders.Order;
import org.fogbowcloud.ras.core.models.securityrules.SecurityRule;
import org.fogbowcloud.ras.core.models.tokens.Token;

import java.util.List;

public interface SecurityRulePlugin<T extends Token> {

    public String requestSecurityRule(SecurityRule securityRule, Order majorOrder, T localUserAttributes)
            throws FogbowRasException, UnexpectedException;

    public List<SecurityRule> getSecurityRules(Order majorOrder, T localUserAttributes)
            throws FogbowRasException, UnexpectedException;

    public void deleteSecurityRule(String securityRuleId, T localUserAttributes)
            throws FogbowRasException, UnexpectedException;
}
