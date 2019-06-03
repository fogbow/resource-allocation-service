package cloud.fogbow.ras.core.plugins.interoperability.aws.securityrule.v2;

import cloud.fogbow.common.exceptions.FogbowException;
import cloud.fogbow.common.exceptions.InvalidParameterException;
import cloud.fogbow.common.models.AwsV2User;
import cloud.fogbow.common.util.Ipv4AddressValidator;
import cloud.fogbow.common.util.PropertiesUtil;
import cloud.fogbow.ras.api.http.response.SecurityRuleInstance;
import cloud.fogbow.ras.api.parameters.SecurityRule;
import cloud.fogbow.ras.constants.SystemConstants;
import cloud.fogbow.ras.core.models.orders.Order;
import cloud.fogbow.ras.core.plugins.interoperability.SecurityRulePlugin;
import cloud.fogbow.ras.core.plugins.interoperability.aws.AwsV2ClientUtil;
import cloud.fogbow.ras.core.plugins.interoperability.aws.AwsV2ConfigurationPropertyKeys;
import software.amazon.awssdk.core.exception.SdkException;
import software.amazon.awssdk.services.ec2.Ec2Client;
import software.amazon.awssdk.services.ec2.model.*;

import java.util.List;
import java.util.Properties;

public class AwsV2SecurityRulePlugin implements SecurityRulePlugin<AwsV2User> {

    private String region;

    public AwsV2SecurityRulePlugin(String confFilePath) {
        Properties properties = PropertiesUtil.readProperties(confFilePath);
        this.region = properties.getProperty(AwsV2ConfigurationPropertyKeys.AWS_REGION_SELECTION_KEY);
    }

    @Override
    public String requestSecurityRule(SecurityRule securityRule, Order majorOrder, AwsV2User cloudUser) throws FogbowException {
        validateRule(securityRule);

        Ec2Client client = AwsV2ClientUtil.createEc2Client(cloudUser.getToken(), this.region);
        SecurityGroup group = getSecurityGroupByName(retrieveSecurityGroupName(majorOrder), client);

        switch (securityRule.getDirection()) {
            case IN:
                addIngressRule(group, securityRule, client);
                break;
            case OUT:
                addEgressRule(group, securityRule, client);
                break;
            default:
                throw new FogbowException();
        }

        return getId(securityRule, majorOrder);
    }

    @Override
    public List<SecurityRuleInstance> getSecurityRules(Order majorOrder, AwsV2User cloudUser) throws FogbowException {
        throw new UnsupportedOperationException("This feature has not been implemented for aws cloud, yet.");
    }

    @Override
    public void deleteSecurityRule(String securityRuleId, AwsV2User cloudUser) throws FogbowException {
        throw new UnsupportedOperationException("This feature has not been implemented for aws cloud, yet.");
    }

    private SecurityGroup getSecurityGroupByName(String name, Ec2Client client) throws FogbowException {
        DescribeSecurityGroupsRequest request = DescribeSecurityGroupsRequest.builder().groupNames(name).build();
        List<SecurityGroup> groups = client.describeSecurityGroups(request).securityGroups();

        if (groups.isEmpty()) {
            throw new FogbowException("There is no security group with the name " + name);
        }

        return groups.get(0);
    }

    private String retrieveSecurityGroupName(Order majorOrder) throws InvalidParameterException {
        String securityGroupName;
        switch (majorOrder.getType()) {
            case NETWORK:
                securityGroupName = SystemConstants.PN_SECURITY_GROUP_PREFIX + majorOrder.getInstanceId();
                break;
            case PUBLIC_IP:
                securityGroupName = SystemConstants.PIP_SECURITY_GROUP_PREFIX + majorOrder.getInstanceId();
                break;
            default:
                throw new InvalidParameterException();
        }
        return securityGroupName;
    }

    private void addIngressRule(SecurityGroup group, SecurityRule rule, Ec2Client client) throws FogbowException {
        AuthorizeSecurityGroupIngressRequest request = AuthorizeSecurityGroupIngressRequest.builder()
                .cidrIp(rule.getCidr())
                .fromPort(rule.getPortFrom())
                .toPort(rule.getPortTo())
                .groupId(group.groupId())
                .ipProtocol(rule.getProtocol().toString())
                .groupName(group.groupName())
                .build();

        try {
            client.authorizeSecurityGroupIngress(request);
        } catch (SdkException ex) {
            throw new FogbowException(ex.getMessage());
        }
    }

    private void addEgressRule(SecurityGroup group, SecurityRule rule, Ec2Client client) throws FogbowException {
        IpRange ipRange = IpRange.builder()
                .cidrIp(rule.getCidr())
                .build();

        IpPermission ipPermission = IpPermission.builder()
                .fromPort(rule.getPortFrom())
                .toPort(rule.getPortTo())
                .ipProtocol(rule.getProtocol().toString())
                .ipRanges(ipRange)
                .build();

        AuthorizeSecurityGroupEgressRequest request = AuthorizeSecurityGroupEgressRequest.builder()
                .groupId(group.groupId())
                .ipPermissions(ipPermission)
                .build();

        try {
            client.authorizeSecurityGroupEgress(request);
        } catch (SdkException ex) {
            throw new FogbowException(ex.getMessage());
        }
    }

    private void validateRule(SecurityRule rule) throws InvalidParameterException {
        if(rule.getProtocol().equals(SecurityRule.Protocol.ANY)) {
            throw new InvalidParameterException("The protocol must be specified");
        }

        Ipv4AddressValidator ipv4AddressValidator = new Ipv4AddressValidator();
        if(!ipv4AddressValidator.validate(rule.getCidr())) {
            throw new InvalidParameterException("The cidr must follow ipv4 pattern");
        }
    }

    private String getId(SecurityRule securityRule, Order order) {
        String ruleKeySeparator = "";
        String id = order.getInstanceId() + ruleKeySeparator + securityRule.getCidr() + ruleKeySeparator + securityRule.getPortFrom() +
                ruleKeySeparator + securityRule.getPortTo() + ruleKeySeparator + securityRule.getProtocol();
        return id;
    }
}
