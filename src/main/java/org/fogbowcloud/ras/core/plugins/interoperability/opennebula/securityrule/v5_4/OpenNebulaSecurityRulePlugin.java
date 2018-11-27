package org.fogbowcloud.ras.core.plugins.interoperability.opennebula.securityrule.v5_4;

import org.apache.log4j.Logger;
import org.fogbowcloud.ras.core.exceptions.FogbowRasException;
import org.fogbowcloud.ras.core.exceptions.UnexpectedException;
import org.fogbowcloud.ras.core.models.orders.Order;
import org.fogbowcloud.ras.core.models.securityrules.SecurityRule;
import org.fogbowcloud.ras.core.models.tokens.OpenNebulaToken;
import org.fogbowcloud.ras.core.plugins.interoperability.SecurityRulePlugin;
import org.fogbowcloud.ras.util.connectivity.HttpRequestClientUtil;

import java.util.List;

public class OpenNebulaSecurityRulePlugin implements SecurityRulePlugin<OpenNebulaToken> {

    public static final Logger LOGGER = Logger.getLogger(OpenNebulaSecurityRulePlugin.class);

    private HttpRequestClientUtil client;
    
    public OpenNebulaSecurityRulePlugin() {
        this.client = new HttpRequestClientUtil();
    }

    @Override
    public String requestSecurityRule(SecurityRule securityRule, Order majorOrder, OpenNebulaToken localUserAttributes)
            throws FogbowRasException, UnexpectedException {
        throw new UnsupportedOperationException();
    }

    @Override
    public List<SecurityRule> getSecurityRules(Order majorOrder, OpenNebulaToken localUserAttributes)
            throws FogbowRasException, UnexpectedException {
        throw new UnsupportedOperationException();
    }

    @Override
    public void deleteSecurityRule(String securityRuleId, OpenNebulaToken localUserAttributes)
            throws FogbowRasException, UnexpectedException {
        throw new UnsupportedOperationException();
    }
}
