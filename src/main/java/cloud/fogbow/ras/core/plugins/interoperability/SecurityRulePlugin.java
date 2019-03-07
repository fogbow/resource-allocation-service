package cloud.fogbow.ras.core.plugins.interoperability;

import cloud.fogbow.common.exceptions.FogbowException;
import cloud.fogbow.common.models.CloudUser;
import cloud.fogbow.ras.core.models.orders.Order;
import cloud.fogbow.ras.api.http.response.securityrules.SecurityRule;

import java.util.List;

public interface SecurityRulePlugin<T extends CloudUser> {

    public String requestSecurityRule(SecurityRule securityRule, Order majorOrder, T cloudUser) throws FogbowException;

    public List<SecurityRule> getSecurityRules(Order majorOrder, T cloudUser) throws FogbowException;

    public void deleteSecurityRule(String securityRuleId, T cloudUser) throws FogbowException;
}
