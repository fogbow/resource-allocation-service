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

    public String createSecurityRule(Order majorOrder, SecurityRule securityRule,
                                     FederationUserToken federationUserToken) throws Exception {
        CloudConnector cloudConnector = CloudConnectorFactory.getInstance().getCloudConnector(majorOrder.getProvider(),
                majorOrder.getCloudName());
        return cloudConnector.requestSecurityRule(majorOrder, securityRule, federationUserToken);
    }

    public List<SecurityRule> getAllSecurityRules(Order majorOrder, FederationUserToken federationUserToken)
            throws Exception {
        CloudConnector cloudConnector = CloudConnectorFactory.getInstance().getCloudConnector(majorOrder.getProvider(),
                majorOrder.getCloudName());
        return cloudConnector.getAllSecurityRules(majorOrder, federationUserToken);
    }

    public void deleteSecurityRule(String securityRuleId, String providerId,
                                   FederationUserToken federationUserToken) throws Exception {
        // FIXMECloudname
        CloudConnector cloudConnector = CloudConnectorFactory.getInstance().getCloudConnector(providerId, "");
        cloudConnector.deleteSecurityRule(securityRuleId, federationUserToken);
    }
}
