package cloud.fogbow.ras.core.plugins.interoperability.azure.securityrule;

import cloud.fogbow.common.exceptions.FogbowException;
import cloud.fogbow.common.models.AzureUser;
import cloud.fogbow.ras.api.http.response.SecurityRuleInstance;
import cloud.fogbow.ras.api.parameters.SecurityRule;
import cloud.fogbow.ras.core.models.orders.Order;
import cloud.fogbow.ras.core.plugins.interoperability.SecurityRulePlugin;

import java.util.List;

public class AzureSecurityRulePlugin implements SecurityRulePlugin<AzureUser> {

    @Override
    public String requestSecurityRule(SecurityRule securityRule, Order majorOrder, AzureUser azureUser) throws FogbowException {
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
