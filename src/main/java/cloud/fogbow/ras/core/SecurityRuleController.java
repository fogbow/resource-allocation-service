package cloud.fogbow.ras.core;

import cloud.fogbow.common.exceptions.FogbowException;
import cloud.fogbow.common.models.FederationUser;
import cloud.fogbow.ras.core.cloudconnector.CloudConnector;
import cloud.fogbow.ras.core.cloudconnector.CloudConnectorFactory;
import cloud.fogbow.ras.core.models.securityrules.SecurityRule;
import cloud.fogbow.ras.core.models.orders.Order;

import java.util.List;

public class SecurityRuleController {

    public SecurityRuleController() {
    }

    public String createSecurityRule(Order order, SecurityRule securityRule, FederationUser federationUserToken)
            throws FogbowException {
        CloudConnector cloudConnector = CloudConnectorFactory.getInstance().getCloudConnector(order.getProvider(),
                order.getCloudName());
        return cloudConnector.requestSecurityRule(order, securityRule, federationUserToken);
    }

    public List<SecurityRule> getAllSecurityRules(Order order, FederationUser federationUserToken)
            throws FogbowException {
        CloudConnector cloudConnector = CloudConnectorFactory.getInstance().getCloudConnector(order.getProvider(),
                order.getCloudName());
        return cloudConnector.getAllSecurityRules(order, federationUserToken);
    }

    public void deleteSecurityRule(String providerId, String cloudName, String securityRuleId,
                           FederationUser federationUserToken) throws FogbowException {
        CloudConnector cloudConnector = CloudConnectorFactory.getInstance().getCloudConnector(providerId, cloudName);
        cloudConnector.deleteSecurityRule(securityRuleId, federationUserToken);
    }
}
