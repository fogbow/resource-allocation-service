package org.fogbowcloud.ras.core;

import org.fogbowcloud.ras.core.cloudconnector.CloudConnector;
import org.fogbowcloud.ras.core.cloudconnector.CloudConnectorFactory;
import org.fogbowcloud.ras.core.models.orders.Order;
import org.fogbowcloud.ras.core.models.securitygroups.SecurityGroupRule;
import org.fogbowcloud.ras.core.models.tokens.FederationUserToken;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class SecurityGroupController {

    // maps orderId (order may be publicIp or networkId) to security group id (from orchestrator)
    private Map<String, String> securityGroupsMap;
    // maps securityGroupId to security group rule object
    private Map<String, SecurityGroupRule> securityGroupRulesMap;

    public SecurityGroupController() {
        this.securityGroupsMap = new ConcurrentHashMap<>();
        this.securityGroupRulesMap = new ConcurrentHashMap<>();
    }

    public String createSecurityGroupRules(String majorOrderId, SecurityGroupRule securityGroupRule, String providerId,
                                           FederationUserToken federationUserToken) {
        CloudConnector cloudConnector = CloudConnectorFactory.getInstance().getCloudConnector(providerId);
        return cloudConnector.requestSecurityGroupRules(majorOrderId, securityGroupRule, federationUserToken);
    }

    public List<SecurityGroupRule> getAllSecurityGroupRules(String majorOrderId, String providerId,
                                                            FederationUserToken federationUserToken) {
        CloudConnector cloudConnector = CloudConnectorFactory.getInstance().getCloudConnector(providerId);
        return cloudConnector.getAllSecurityGroupRules(majorOrderId, federationUserToken);
    }

    public void deleteSecurityGroupRules(String securityGroupRuleId, String providerId,
                                         FederationUserToken federationUserToken) {
        CloudConnector cloudConnector = CloudConnectorFactory.getInstance().getCloudConnector(providerId);
        cloudConnector.deleteSecurityGroupRules(securityGroupRuleId, federationUserToken);
    }

}
