package cloud.fogbow.ras.core.plugins.interoperability.aws.securityrule.v2;

import cloud.fogbow.common.constants.AwsConstants;
import cloud.fogbow.common.exceptions.FogbowException;
import cloud.fogbow.common.exceptions.InvalidParameterException;
import cloud.fogbow.common.models.AwsV2User;
import cloud.fogbow.common.util.Ipv4AddressValidator;
import cloud.fogbow.common.util.PropertiesUtil;
import cloud.fogbow.ras.api.http.response.SecurityRuleInstance;
import cloud.fogbow.ras.api.parameters.SecurityRule;
import cloud.fogbow.ras.constants.SystemConstants;
import cloud.fogbow.ras.core.models.ResourceType;
import cloud.fogbow.ras.core.models.orders.Order;
import cloud.fogbow.ras.core.plugins.interoperability.SecurityRulePlugin;
import cloud.fogbow.ras.core.plugins.interoperability.aws.AwsV2ClientUtil;
import cloud.fogbow.ras.core.plugins.interoperability.aws.AwsV2ConfigurationPropertyKeys;
import software.amazon.awssdk.core.exception.SdkException;
import software.amazon.awssdk.services.ec2.Ec2Client;
import software.amazon.awssdk.services.ec2.model.*;

import java.util.*;

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
        SecurityGroup group = getSecurityGroupByName(retrieveSecurityGroupName(majorOrder.getType(), majorOrder.getInstanceId()), client);

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
        Ec2Client client = AwsV2ClientUtil.createEc2Client(cloudUser.getToken(), this.region);
        String groupName = retrieveSecurityGroupName(majorOrder.getType(), majorOrder.getInstanceId());
        SecurityGroup group = getSecurityGroupByName(groupName, client);

        List<SecurityRuleInstance> inboundInstances = getRules(majorOrder, group.ipPermissions());
        List<SecurityRuleInstance> outboundInstances = getRules(majorOrder, group.ipPermissionsEgress());

        List<SecurityRuleInstance> result = new ArrayList<>();
        result.addAll(inboundInstances);
        result.addAll(outboundInstances);

        return result;
    }

    @Override
    public void deleteSecurityRule(String securityRuleId, AwsV2User cloudUser) throws FogbowException {
        Ec2Client client = AwsV2ClientUtil.createEc2Client(cloudUser.getToken(), this.region);
        Map<String, Object> ruleRepresentation = getRuleFromId(securityRuleId);
        SecurityRule rule = (SecurityRule) ruleRepresentation.get("rule");
        ResourceType type = ResourceType.valueOf((String) ruleRepresentation.get("type"));
        String instanceId = (String) ruleRepresentation.get("instanceId");
        SecurityGroup group = getSecurityGroupByName(retrieveSecurityGroupName(type, instanceId), client);

        switch (rule.getDirection()) {
            case IN:
                revokeIngressRule(rule, group, client);
                break;
            case OUT:
                revokeEgressRule(rule, group, client);
        }

    }

    private SecurityGroup getSecurityGroupByName(String name, Ec2Client client) throws FogbowException {
        DescribeSecurityGroupsRequest request = DescribeSecurityGroupsRequest.builder().groupNames(name).build();
        List<SecurityGroup> groups = client.describeSecurityGroups(request).securityGroups();

        if (groups.isEmpty()) {
            throw new FogbowException("There is no security group with the name " + name);
        }

        return groups.get(0);
    }

    private String retrieveSecurityGroupName(ResourceType type, String instanceId) throws InvalidParameterException {
        String securityGroupName;
        switch (type) {
            case NETWORK:
                securityGroupName = SystemConstants.PN_SECURITY_GROUP_PREFIX + instanceId;
                break;
            case PUBLIC_IP:
                securityGroupName = SystemConstants.PIP_SECURITY_GROUP_PREFIX + instanceId;
                break;
            default:
                throw new InvalidParameterException();
        }
        return securityGroupName;
    }

    private void addIngressRule(SecurityGroup group, SecurityRule rule, Ec2Client client) throws FogbowException {
        IpPermission ipPermission = getIpPermission(rule);

        AuthorizeSecurityGroupIngressRequest request = AuthorizeSecurityGroupIngressRequest.builder()
                .groupId(group.groupId())
                .groupName(group.groupName())
                .ipPermissions(ipPermission)
                .build();

        try {
            client.authorizeSecurityGroupIngress(request);
        } catch (SdkException ex) {
            throw new FogbowException(ex.getMessage());
        }
    }

    private void addEgressRule(SecurityGroup group, SecurityRule rule, Ec2Client client) throws FogbowException {
        IpPermission ipPermission = getIpPermission(rule);

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
        String ruleKeySeparator = AwsConstants.SECURITY_RULE_ID_SEPARATOR;
        String id = order.getInstanceId() + ruleKeySeparator + securityRule.getCidr() + ruleKeySeparator + securityRule.getPortFrom() +
                ruleKeySeparator + securityRule.getPortTo() + ruleKeySeparator + securityRule.getProtocol();
        return id;
    }

    private String getId(SecurityRuleInstance securityRule, Order order) {
        String ruleKeySeparator = AwsConstants.SECURITY_RULE_ID_SEPARATOR;

        String id = order.getInstanceId() + ruleKeySeparator + securityRule.getCidr() + ruleKeySeparator + securityRule.getPortFrom() +
                ruleKeySeparator + securityRule.getPortTo() + ruleKeySeparator + securityRule.getProtocol() + ruleKeySeparator + securityRule.getDirection().toString();
        return id;
    }

    private List<SecurityRuleInstance> getRules(Order majorOrder, List<IpPermission> ipPermissions) {
        List<SecurityRuleInstance> ruleInstances = new ArrayList<>();

        for(IpPermission ipPermission : ipPermissions) {
            int portFrom = ipPermission.fromPort();
            int portTo = ipPermission.toPort();
            String cidr = ipPermission.ipRanges().iterator().next().cidrIp();
            String protocol = ipPermission.ipProtocol();

            SecurityRuleInstance instance = new SecurityRuleInstance("",
                    SecurityRule.Direction.IN,
                    portFrom, portTo, cidr,
                    SecurityRule.EtherType.IPv4,
                    SecurityRule.Protocol.valueOf(protocol)
            );

            String id = getId(instance, majorOrder);
            instance.setId(id);

            ruleInstances.add(instance);
        }

        return ruleInstances;
    }

    private Map<String, Object> getRuleFromId(String ruleId) throws InvalidParameterException{
        String fields[] = ruleId.split(AwsConstants.SECURITY_RULE_ID_SEPARATOR);

        if (fields.length != 7) {
            throw new InvalidParameterException("");
        }

        SecurityRule rule = new SecurityRule(
            SecurityRule.Direction.valueOf(fields[5]),
            Integer.valueOf(fields[2]),
            Integer.valueOf(fields[3]),
            fields[1],
            SecurityRule.EtherType.IPv4,
            SecurityRule.Protocol.valueOf(fields[4])
        );

        Map<String, Object> result = new HashMap<>();
        result.put("rule", rule);
        result.put("type", fields[6]);
        result.put("instanceId", fields[0]);
        return result;
    }

    private void revokeIngressRule(SecurityRule rule, SecurityGroup group, Ec2Client client) throws FogbowException {
        IpPermission ipPermission = getIpPermission(rule);

        RevokeSecurityGroupIngressRequest request = RevokeSecurityGroupIngressRequest.builder()
                .ipPermissions(ipPermission)
                .groupName(group.groupName())
                .groupId(group.groupId())
                .build();

        try {
            client.revokeSecurityGroupIngress(request);
        } catch (SdkException ex) {
            throw new FogbowException(ex.getMessage());
        }
    }

    private void revokeEgressRule(SecurityRule rule, SecurityGroup group, Ec2Client client) throws FogbowException{
        IpPermission ipPermission = getIpPermission(rule);

        RevokeSecurityGroupEgressRequest request = RevokeSecurityGroupEgressRequest.builder()
                .ipPermissions(ipPermission)
                .groupId(group.groupId())
                .build();

        try {
            client.revokeSecurityGroupEgress(request);
        } catch (SdkException ex) {
            throw new FogbowException(ex.getMessage());
        }
    }

    private IpPermission getIpPermission(SecurityRule rule) {
        IpRange ipRange = IpRange.builder()
                .cidrIp(rule.getCidr())
                .build();

        IpPermission ipPermission = IpPermission.builder()
                .fromPort(rule.getPortFrom())
                .toPort(rule.getPortTo())
                .ipProtocol(rule.getProtocol().toString())
                .ipRanges(ipRange)
                .build();

        return ipPermission;
    }

}
