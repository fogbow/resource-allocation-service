package org.fogbowcloud.ras.core.stubs;

import org.fogbowcloud.ras.core.exceptions.FogbowRasException;
import org.fogbowcloud.ras.core.exceptions.UnexpectedException;
import org.fogbowcloud.ras.core.models.securitygroups.SecurityGroupRule;
import org.fogbowcloud.ras.core.models.tokens.Token;
import org.fogbowcloud.ras.core.plugins.interoperability.SecurityGroupPlugin;

import java.util.List;

/**
 * This class is a stub for the SecurityGroupPlugin interface used for tests only.
 * Should not have a proper implementation.
 */
public class StubSecurityGroupPlugin implements SecurityGroupPlugin {
    @Override
    public String requestSecurityGroupRule(SecurityGroupRule securityGroupRule, String securityGroupId, Token token)
            throws FogbowRasException, UnexpectedException {
        return null;
    }

    @Override
    public String retrieveSecurityGroupId(String securityGroupName, Token token)
            throws UnexpectedException, FogbowRasException {
        return null;
    }

    @Override
    public List<SecurityGroupRule> getSecurityGroupRules(String securityGroupId, Token token)
            throws FogbowRasException, UnexpectedException {
        return null;
    }

    @Override
    public void deleteSecurityGroupRule(String securityGroupRuleId, Token token)
            throws FogbowRasException, UnexpectedException {

    }
}
