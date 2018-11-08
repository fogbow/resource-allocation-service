package org.fogbowcloud.ras.core;

import org.fogbowcloud.ras.core.cloudconnector.CloudConnector;
import org.fogbowcloud.ras.core.cloudconnector.CloudConnectorFactory;
import org.fogbowcloud.ras.core.models.orders.Order;
import org.fogbowcloud.ras.core.models.securitygroups.SecurityGroupRule;

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

    public String createSecurityGroupRules(Order majorOrder, SecurityGroupRule securityGroupRule) {
        String memberId = majorOrder.getProvider();
        CloudConnector cloudConnector = CloudConnectorFactory.getInstance().getCloudConnector(memberId);
        return cloudConnector.requestSecurityGroupRules(majorOrder.getId(), securityGroupRule, majorOrder.getFederationUserToken());
    }

    public List<SecurityGroupRule> getAllSecurityGroupRules(Order majorOrder) {
        String memberId = majorOrder.getProvider();
        CloudConnector cloudConnector = CloudConnectorFactory.getInstance().getCloudConnector(memberId);
        return cloudConnector.getAllSecurityGroupRules(majorOrder.getId(), majorOrder.getFederationUserToken());
    }

    public void deleteSecurityGroupRules(Order majorOrder, String securityGroupRuleId) {
        String memberId = majorOrder.getProvider();
        CloudConnector cloudConnector = CloudConnectorFactory.getInstance().getCloudConnector(memberId);
        cloudConnector.deleteSecurityGroupRules(securityGroupRuleId, majorOrder.getFederationUserToken());
    }

}
