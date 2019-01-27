package cloud.fogbow.ras.core.plugins.interoperability;

import cloud.fogbow.common.exceptions.FogbowException;
import cloud.fogbow.common.models.CloudToken;
import cloud.fogbow.ras.core.models.securityrules.SecurityRule;
import cloud.fogbow.ras.core.models.orders.Order;

import java.util.List;

public interface SecurityRulePlugin {

    public String requestSecurityRule(SecurityRule securityRule, Order majorOrder, CloudToken localUserAttributes)
            throws FogbowException;

    public List<SecurityRule> getSecurityRules(Order majorOrder, CloudToken localUserAttributes)
            throws FogbowException;

    public void deleteSecurityRule(String securityRuleId, CloudToken localUserAttributes)
            throws FogbowException;
}
