package cloud.fogbow.ras.core.plugins.interoperability;

import cloud.fogbow.common.exceptions.FogbowException;
import cloud.fogbow.common.models.CloudToken;
import cloud.fogbow.ras.core.models.orders.Order;
import cloud.fogbow.ras.api.http.response.securityrules.SecurityRule;

import java.util.List;

public interface SecurityRulePlugin<T extends CloudToken> {

    public String requestSecurityRule(SecurityRule securityRule, Order majorOrder, T localUserAttributes)
            throws FogbowException;

    public List<SecurityRule> getSecurityRules(Order majorOrder, T localUserAttributes)
            throws FogbowException;

    public void deleteSecurityRule(String securityRuleId, T localUserAttributes)
            throws FogbowException;
}
