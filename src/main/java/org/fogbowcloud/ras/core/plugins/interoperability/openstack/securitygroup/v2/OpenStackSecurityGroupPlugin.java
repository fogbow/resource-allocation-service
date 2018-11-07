package org.fogbowcloud.ras.core.plugins.interoperability.openstack.securitygroup.v2;

import org.apache.log4j.Logger;
import org.fogbowcloud.ras.core.exceptions.FogbowRasException;
import org.fogbowcloud.ras.core.exceptions.UnexpectedException;
import org.fogbowcloud.ras.core.models.securitygroups.SecurityGroup;
import org.fogbowcloud.ras.core.models.securitygroups.SecurityGroupRule;
import org.fogbowcloud.ras.core.models.tokens.OpenStackV3Token;
import org.fogbowcloud.ras.core.plugins.interoperability.SecurityGroupPlugin;

public class OpenStackSecurityGroupPlugin implements SecurityGroupPlugin<OpenStackV3Token> {
    private static final Logger LOGGER = Logger.getLogger(OpenStackSecurityGroupPlugin.class);

    @Override
    public String requestSecurityGroupRule(SecurityGroupRule securityGroupRule, OpenStackV3Token openStackV3Token)
            throws FogbowRasException, UnexpectedException {
        return null;
    }

    @Override
    public SecurityGroup getSecurityGroup(String securityGroupId, OpenStackV3Token openStackV3Token)
            throws FogbowRasException, UnexpectedException {
       return null;
    }

    @Override
    public SecurityGroupRule getSecurityGroupRule(String securityGroupRuleId, OpenStackV3Token openStackV3Token)
            throws FogbowRasException, UnexpectedException {
        return null;
    }

    @Override
    public void deleteSecurityGroupRule(String securityGroupRuleId, OpenStackV3Token openStackV3Token)
            throws FogbowRasException, UnexpectedException {
    }
}
