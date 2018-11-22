package org.fogbowcloud.ras.core.plugins.interoperability.cloudstack.securityrule.v4_9;

import java.util.List;

import org.fogbowcloud.ras.core.exceptions.FogbowRasException;
import org.fogbowcloud.ras.core.exceptions.UnexpectedException;
import org.fogbowcloud.ras.core.models.orders.Order;
import org.fogbowcloud.ras.core.models.securityrules.SecurityRule;
import org.fogbowcloud.ras.core.models.tokens.CloudStackToken;
import org.fogbowcloud.ras.core.plugins.interoperability.SecurityRulePlugin;

public class CloudStackSecurityRulePlugin implements SecurityRulePlugin<CloudStackToken> {
    @Override
    public String requestSecurityRule(SecurityRule securityRule, Order majorOrder, CloudStackToken localUserAttributes) throws FogbowRasException, UnexpectedException {
        throw new UnsupportedOperationException();
    }

    @Override
    public List<SecurityRule> getSecurityRules(Order majorOrder, CloudStackToken localUserAttributes) throws FogbowRasException, UnexpectedException {
        throw new UnsupportedOperationException();
    }

    @Override
    public void deleteSecurityRule(String securityRuleId, CloudStackToken localUserAttributes) throws FogbowRasException, UnexpectedException {
        throw new UnsupportedOperationException();
    }
}
