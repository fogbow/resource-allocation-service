package cloud.fogbow.ras.core.plugins.interoperability.azure.securityrule;

import cloud.fogbow.common.exceptions.FogbowException;
import cloud.fogbow.common.models.AzureUser;
import cloud.fogbow.ras.api.http.response.SecurityRuleInstance;
import cloud.fogbow.ras.api.parameters.SecurityRule;
import cloud.fogbow.ras.constants.Messages;
import cloud.fogbow.ras.core.models.orders.Order;
import cloud.fogbow.ras.core.plugins.interoperability.SecurityRulePlugin;
import org.apache.log4j.Logger;

import java.util.List;

public class AzureSecurityRulePlugin implements SecurityRulePlugin<AzureUser> {

    private static final Logger LOGGER = Logger.getLogger(AzureSecurityRulePlugin.class);

    @Override
    public String requestSecurityRule(SecurityRule securityRule, Order majorOrder, AzureUser azureUser)
            throws FogbowException {

        LOGGER.info(Messages.Info.REQUESTING_INSTANCE_FROM_PROVIDER);

        String cidr = securityRule.getCidr();
        int portFrom = securityRule.getPortFrom();
        int portTo = securityRule.getPortTo();
        String direction = securityRule.getDirection().toString();
        String etherType = securityRule.getEtherType().toString();
        String protocol = securityRule.getProtocol().toString();

        return null;
    }

    @Override
    public List<SecurityRuleInstance> getSecurityRules(Order majorOrder, AzureUser azureUser) throws FogbowException {
        return null;
    }

    @Override
    public void deleteSecurityRule(String securityRuleId, AzureUser azureUser) throws FogbowException {

    }

}
