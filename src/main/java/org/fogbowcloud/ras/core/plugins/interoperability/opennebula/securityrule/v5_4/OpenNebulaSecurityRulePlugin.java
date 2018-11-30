package org.fogbowcloud.ras.core.plugins.interoperability.opennebula.securityrule.v5_4;

import org.apache.log4j.Logger;
import org.fogbowcloud.ras.core.constants.Messages;
import org.fogbowcloud.ras.core.exceptions.FogbowRasException;
import org.fogbowcloud.ras.core.exceptions.InvalidParameterException;
import org.fogbowcloud.ras.core.exceptions.UnexpectedException;
import org.fogbowcloud.ras.core.models.orders.Order;
import org.fogbowcloud.ras.core.models.securityrules.SecurityRule;
import org.fogbowcloud.ras.core.models.tokens.OpenNebulaToken;
import org.fogbowcloud.ras.core.plugins.interoperability.SecurityRulePlugin;
import org.fogbowcloud.ras.core.plugins.interoperability.opennebula.OpenNebulaClientFactory;
import org.fogbowcloud.ras.util.connectivity.HttpRequestClientUtil;
import org.opennebula.client.Client;
import org.opennebula.client.secgroup.SecurityGroup;

import java.util.List;

public class OpenNebulaSecurityRulePlugin implements SecurityRulePlugin<OpenNebulaToken> {

    public static final Logger LOGGER = Logger.getLogger(OpenNebulaSecurityRulePlugin.class);

    private OpenNebulaClientFactory factory;
    
    public OpenNebulaSecurityRulePlugin() {
        this.factory = new OpenNebulaClientFactory();
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
        LOGGER.info(
                String.format(Messages.Info.DELETING_INSTANCE, securityRuleId, localUserAttributes.getTokenValue()));
        Client client = this.factory.createClient(localUserAttributes.getTokenValue());
        int id;
        try {
            id = Integer.parseInt(securityRuleId);
        } catch (Exception e) {
            LOGGER.error(String.format(Messages.Error.ERROR_WHILE_CONVERTING_INSTANCE_ID, securityRuleId));
            throw new InvalidParameterException(Messages.Exception.INVALID_PARAMETER);
        }
        SecurityGroup.delete(client, id);
    }
}
