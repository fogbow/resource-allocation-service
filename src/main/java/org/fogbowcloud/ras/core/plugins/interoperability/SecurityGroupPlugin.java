package org.fogbowcloud.ras.core.plugins.interoperability;

import org.fogbowcloud.ras.core.exceptions.FogbowRasException;
import org.fogbowcloud.ras.core.exceptions.UnexpectedException;
import org.fogbowcloud.ras.core.models.instances.NetworkInstance;
import org.fogbowcloud.ras.core.models.orders.NetworkOrder;
import org.fogbowcloud.ras.core.models.orders.Order;
import org.fogbowcloud.ras.core.models.securitygroups.SecurityGroup;
import org.fogbowcloud.ras.core.models.securitygroups.SecurityGroupRule;
import org.fogbowcloud.ras.core.models.tokens.Token;

import java.util.List;

public interface SecurityGroupPlugin<T extends Token> {

    public String requestSecurityGroupRule(SecurityGroupRule securityGroupRule, String securityGroupId, T localUserAttributes)
            throws FogbowRasException, UnexpectedException;

    public String retrieveSecurityGroupId(String securityGroupName, T localUserAttributes) throws UnexpectedException, FogbowRasException;

    public List<SecurityGroupRule> getSecurityGroupRules(String securityGroupId, T localUserAttributes)
            throws FogbowRasException, UnexpectedException;

    public void deleteSecurityGroupRule(String securityGroupRuleId, T localUserAttributes)
            throws FogbowRasException, UnexpectedException;
}
