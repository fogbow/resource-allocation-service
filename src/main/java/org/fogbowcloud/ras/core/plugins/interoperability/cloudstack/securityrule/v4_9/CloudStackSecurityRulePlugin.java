package org.fogbowcloud.ras.core.plugins.interoperability.cloudstack.securityrule.v4_9;

import org.fogbowcloud.ras.core.exceptions.FogbowRasException;
import org.fogbowcloud.ras.core.exceptions.UnexpectedException;
import org.fogbowcloud.ras.core.models.orders.Order;
import org.fogbowcloud.ras.core.models.securitygroups.SecurityGroupRule;
import org.fogbowcloud.ras.core.models.tokens.Token;
import org.fogbowcloud.ras.core.plugins.interoperability.SecurityGroupPlugin;

import java.util.List;

public class CloudStackSecurityRulePlugin implements SecurityGroupPlugin {
    @Override
    public String requestSecurityGroupRule(SecurityGroupRule securityGroupRule, Order majorOrder, Token localUserAttributes) throws FogbowRasException, UnexpectedException {
        throw new UnsupportedOperationException();
    }

    @Override
    public List<SecurityGroupRule> getSecurityGroupRules(Order majorOrder, Token localUserAttributes) throws FogbowRasException, UnexpectedException {
        throw new UnsupportedOperationException();
    }

    @Override
    public void deleteSecurityGroupRule(String securityGroupRuleId, Token localUserAttributes) throws FogbowRasException, UnexpectedException {
        throw new UnsupportedOperationException();
    }
}
