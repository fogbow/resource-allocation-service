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

    // maps orderId (order may be publicIp or networkId) to security group id (from orchestrator)
    private Map<String, String> securityGroupsMap;
    // maps securityGroupId to security group rule object
    private Map<String, SecurityGroupRule> securityGroupRulesMap;

    public SecurityGroupController() {
        this.securityGroupsMap = new ConcurrentHashMap<>();
        this.securityGroupRulesMap = new ConcurrentHashMap<>();
    }

    public String createSecurityGroupRules(Order majorOrder, SecurityGroupRule securityGroupRule, String providerId,
            FederationUserToken federationUserToken) throws FogbowRasException, UnexpectedException {
        CloudConnector cloudConnector = CloudConnectorFactory.getInstance().getCloudConnector(providerId);
        String securityGroupName = getSecurityGroupName(majorOrder);
        return cloudConnector.requestSecurityGroupRule(securityGroupName, securityGroupRule, federationUserToken);
    }

    public List<SecurityGroupRule> getAllSecurityGroupRules(Order majorOrder, FederationUserToken federationUserToken)
            throws FogbowRasException, UnexpectedException {
        CloudConnector cloudConnector = CloudConnectorFactory.getInstance().getCloudConnector(majorOrder.getProvider());
        String securityGroupName = getSecurityGroupName(majorOrder);
        return cloudConnector.getAllSecurityGroupRules(securityGroupName, federationUserToken);
    }

    public void deleteSecurityGroupRules(String securityGroupRuleId, String providerId,
            FederationUserToken federationUserToken) throws UnexpectedException, FogbowRasException {
        CloudConnector cloudConnector = CloudConnectorFactory.getInstance().getCloudConnector(providerId);
        cloudConnector.deleteSecurityGroupRule(securityGroupRuleId, federationUserToken);
    }

    private String getSecurityGroupName(Order majorOrder) throws InvalidParameterException {
        String securityGroupName;
        switch (majorOrder.getType()) {
            case NETWORK:
                securityGroupName = NetworkPlugin.SECURITY_GROUP_PREFIX + majorOrder.getId();
                break;
            case PUBLIC_IP:
                securityGroupName = PublicIpPlugin.SECURITY_GROUP_PREFIX + majorOrder.getId();
                break;
            default:
                throw new InvalidParameterException();
        }
        return securityGroupName;
    }

}
