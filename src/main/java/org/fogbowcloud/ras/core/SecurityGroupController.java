package org.fogbowcloud.ras.core;

import org.fogbowcloud.ras.core.cloudconnector.CloudConnector;
import org.fogbowcloud.ras.core.cloudconnector.CloudConnectorFactory;
import org.fogbowcloud.ras.core.exceptions.FogbowRasException;
import org.fogbowcloud.ras.core.exceptions.InvalidParameterException;
import org.fogbowcloud.ras.core.exceptions.UnexpectedException;
import org.fogbowcloud.ras.core.models.orders.Order;
import org.fogbowcloud.ras.core.models.securitygroups.SecurityGroupRule;
import org.fogbowcloud.ras.core.models.tokens.FederationUserToken;
import org.fogbowcloud.ras.core.plugins.interoperability.NetworkPlugin;
import org.fogbowcloud.ras.core.plugins.interoperability.PublicIpPlugin;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class SecurityGroupController {

    public SecurityGroupController() {
    }

    public String createSecurityGroupRules(Order majorOrder, SecurityGroupRule securityGroupRule,
            FederationUserToken federationUserToken) throws Exception {
        CloudConnector cloudConnector = CloudConnectorFactory.getInstance().getCloudConnector(majorOrder.getProvider());
        return cloudConnector.requestSecurityGroupRule(majorOrder, securityGroupRule, federationUserToken);
    }

    public List<SecurityGroupRule> getAllSecurityGroupRules(Order majorOrder, FederationUserToken federationUserToken)
            throws Exception {
        CloudConnector cloudConnector = CloudConnectorFactory.getInstance().getCloudConnector(majorOrder.getProvider());
        return cloudConnector.getAllSecurityGroupRules(majorOrder, federationUserToken);
    }

    public void deleteSecurityGroupRules(String securityGroupRuleId, String providerId,
            FederationUserToken federationUserToken) throws Exception {
        CloudConnector cloudConnector = CloudConnectorFactory.getInstance().getCloudConnector(providerId);
        cloudConnector.deleteSecurityGroupRule(securityGroupRuleId, federationUserToken);
    }
}
