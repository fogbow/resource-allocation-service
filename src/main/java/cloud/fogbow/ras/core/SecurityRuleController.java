package cloud.fogbow.ras.core;

import cloud.fogbow.common.exceptions.FogbowException;
import cloud.fogbow.common.models.SystemUser;
import cloud.fogbow.ras.core.cloudconnector.CloudConnector;
import cloud.fogbow.ras.core.cloudconnector.CloudConnectorFactory;
import cloud.fogbow.ras.core.models.orders.Order;
import cloud.fogbow.ras.api.http.response.securityrules.SecurityRule;

import java.util.List;

public class SecurityRuleController {

    public SecurityRuleController() {
    }

    public String createSecurityRule(Order order, SecurityRule securityRule, SystemUser systemUser)
            throws FogbowException {
        CloudConnector cloudConnector = CloudConnectorFactory.getInstance().getCloudConnector(order.getProvider(),
                order.getCloudName());
        return cloudConnector.requestSecurityRule(order, securityRule, systemUser);
    }

    public List<SecurityRule> getAllSecurityRules(Order order, SystemUser systemUser) throws FogbowException {
        CloudConnector cloudConnector = CloudConnectorFactory.getInstance().getCloudConnector(order.getProvider(),
                order.getCloudName());
        return cloudConnector.getAllSecurityRules(order, systemUser);
    }

    public void deleteSecurityRule(String providerId, String cloudName, String securityRuleId, SystemUser systemUser)
            throws FogbowException {
        CloudConnector cloudConnector = CloudConnectorFactory.getInstance().getCloudConnector(providerId, cloudName);
        cloudConnector.deleteSecurityRule(securityRuleId, systemUser);
    }
}
