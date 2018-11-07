package org.fogbowcloud.ras.core.plugins.interoperability;

import org.fogbowcloud.ras.core.exceptions.FogbowRasException;
import org.fogbowcloud.ras.core.exceptions.UnexpectedException;
import org.fogbowcloud.ras.core.models.instances.NetworkInstance;
import org.fogbowcloud.ras.core.models.orders.NetworkOrder;
import org.fogbowcloud.ras.core.models.orders.Order;
import org.fogbowcloud.ras.core.models.securitygroups.SecurityGroup;
import org.fogbowcloud.ras.core.models.securitygroups.SecurityGroupRule;
import org.fogbowcloud.ras.core.models.tokens.Token;

public interface SecurityGroupPlugin<T extends Token> {

    public String requestSecurityGroupRule(SecurityGroupRule securityGroupRule, T localUserAttributes)
            throws FogbowRasException, UnexpectedException;

    public SecurityGroup getSecurityGroup(String securityGroupId, T localUserAttributes)
            throws FogbowRasException, UnexpectedException;

    public SecurityGroupRule getSecurityGroupRule(String orderId, T localUserAttributes)
            throws FogbowRasException, UnexpectedException;

    public void deleteSecurityGroupRule(String securityGroupRuleId, T localUserAttributes)
            throws FogbowRasException, UnexpectedException;
}
