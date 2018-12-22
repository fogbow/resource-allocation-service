package org.fogbowcloud.ras.core;

import org.fogbowcloud.ras.core.cloudconnector.CloudConnector;
import org.fogbowcloud.ras.core.cloudconnector.CloudConnectorFactory;
import org.fogbowcloud.ras.core.models.orders.Order;
import org.fogbowcloud.ras.core.models.securityrules.SecurityRule;
import org.fogbowcloud.ras.core.models.tokens.FederationUserToken;

import java.util.List;

public class SecurityRuleController {

    public SecurityRuleController() {
    }

    public String createSecurityRule(Order order, SecurityRule securityRule,
                                     FederationUserToken federationUserToken) throws Exception {
        CloudConnector cloudConnector = CloudConnectorFactory.getInstance().getCloudConnector(order.getProvider(),
                order.getCloudName());
        return cloudConnector.requestSecurityRule(order, securityRule, federationUserToken);
    }

    public List<SecurityRule> getAllSecurityRules(Order order, FederationUserToken federationUserToken)
            throws Exception {
        CloudConnector cloudConnector = CloudConnectorFactory.getInstance().getCloudConnector(order.getProvider(),
                order.getCloudName());
        return cloudConnector.getAllSecurityRules(order, federationUserToken);
    }

    public void deleteSecurityRule(String providerId, String cloudName, String securityRuleId,
                                   FederationUserToken federationUserToken) throws Exception {
        CloudConnector cloudConnector = CloudConnectorFactory.getInstance().getCloudConnector(providerId, cloudName);
        cloudConnector.deleteSecurityRule(securityRuleId, federationUserToken);
    }
}
