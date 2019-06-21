package cloud.fogbow.ras.core.plugins.interoperability.aws.securityrule.v2;

import cloud.fogbow.common.exceptions.FogbowException;
import cloud.fogbow.common.models.AwsV2User;
import cloud.fogbow.common.util.PropertiesUtil;
import cloud.fogbow.ras.api.http.response.SecurityRuleInstance;
import cloud.fogbow.ras.api.parameters.SecurityRule;
import cloud.fogbow.ras.core.models.ResourceType;
import cloud.fogbow.ras.core.models.orders.Order;
import cloud.fogbow.ras.core.plugins.interoperability.SecurityRulePlugin;
import cloud.fogbow.ras.core.plugins.interoperability.aws.AwsV2ClientUtil;
import cloud.fogbow.ras.core.plugins.interoperability.aws.AwsV2ConfigurationPropertyKeys;
import software.amazon.awssdk.services.ec2.Ec2Client;
import software.amazon.awssdk.services.ec2.model.*;

import java.util.*;

public class AwsV2SecurityRulePlugin implements SecurityRulePlugin<AwsV2User> {

    private String region;
    protected AwsV2SecurityRuleUtils awsV2SecurityRuleUtils;

    public AwsV2SecurityRulePlugin(String confFilePath) {
        Properties properties = PropertiesUtil.readProperties(confFilePath);
        this.region = properties.getProperty(AwsV2ConfigurationPropertyKeys.AWS_REGION_SELECTION_KEY);
        this.awsV2SecurityRuleUtils = AwsV2SecurityRuleUtils.getInstance();
    }

    @Override
    public String requestSecurityRule(SecurityRule securityRule, Order majorOrder, AwsV2User cloudUser) throws FogbowException {
        awsV2SecurityRuleUtils.validateRule(securityRule);

        Ec2Client client = AwsV2ClientUtil.createEc2Client(cloudUser.getToken(), this.region);
        SecurityGroup group = awsV2SecurityRuleUtils.getSecurityGroup(majorOrder.getInstanceId(), majorOrder.getType(), client);

        switch (securityRule.getDirection()) {
            case IN:
                awsV2SecurityRuleUtils.addIngressRule(group, securityRule, client);
                break;
            case OUT:
                awsV2SecurityRuleUtils.addEgressRule(group, securityRule, client);
                break;
            default:
                throw new FogbowException();
        }

        return awsV2SecurityRuleUtils.getId(securityRule, majorOrder);
    }

    @Override
    public List<SecurityRuleInstance> getSecurityRules(Order majorOrder, AwsV2User cloudUser) throws FogbowException {
        Ec2Client client = AwsV2ClientUtil.createEc2Client(cloudUser.getToken(), this.region);

        SecurityGroup group = awsV2SecurityRuleUtils.getSecurityGroup(majorOrder.getInstanceId(), majorOrder.getType(), client);

        List<SecurityRuleInstance> inboundInstances = awsV2SecurityRuleUtils.getRules(majorOrder, group.ipPermissions(), SecurityRule.Direction.IN);
        List<SecurityRuleInstance> outboundInstances = awsV2SecurityRuleUtils.getRules(majorOrder, group.ipPermissionsEgress(), SecurityRule.Direction.OUT);

        List<SecurityRuleInstance> result = new ArrayList<>();
        result.addAll(inboundInstances);
        result.addAll(outboundInstances);

        return result;
    }

    @Override
    public void deleteSecurityRule(String securityRuleId, AwsV2User cloudUser) throws FogbowException {
        Ec2Client client = AwsV2ClientUtil.createEc2Client(cloudUser.getToken(), this.region);
        Map<String, Object> ruleRepresentation = awsV2SecurityRuleUtils.getRuleFromId(securityRuleId);
        SecurityRule rule = (SecurityRule) ruleRepresentation.get("rule");
        String instanceId = (String) ruleRepresentation.get("instanceId");

        String strType = (String) ruleRepresentation.get("type");
        ResourceType type = ResourceType.valueOf(strType);
        SecurityGroup group = awsV2SecurityRuleUtils.getSecurityGroup(instanceId, type, client);

        switch (rule.getDirection()) {
            case IN:
                awsV2SecurityRuleUtils.revokeIngressRule(rule, group, client);
                break;
            case OUT:
                awsV2SecurityRuleUtils.revokeEgressRule(rule, group, client);
        }

    }
}
