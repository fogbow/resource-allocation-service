package cloud.fogbow.ras.core.plugins.interoperability.aws.securityrule.v2;

import cloud.fogbow.common.constants.AwsConstants;
import cloud.fogbow.common.exceptions.FatalErrorException;
import cloud.fogbow.common.exceptions.FogbowException;
import cloud.fogbow.common.exceptions.InvalidParameterException;
import cloud.fogbow.common.util.Ipv4AddressValidator;
import cloud.fogbow.ras.api.http.response.SecurityRuleInstance;
import cloud.fogbow.ras.api.parameters.SecurityRule;
import cloud.fogbow.ras.constants.SystemConstants;
import cloud.fogbow.ras.core.models.ResourceType;
import cloud.fogbow.ras.core.models.orders.Order;
import software.amazon.awssdk.core.exception.SdkException;
import software.amazon.awssdk.services.ec2.Ec2Client;
import software.amazon.awssdk.services.ec2.model.*;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class AwsV2SecurityRuleUtils {
    private static AwsV2SecurityRuleUtils instance;

    private final int FIRST_POSITION = 0;
    private final int INSTANCE_ID_POSITION = 0;
    private final int CIDR_ID_POSITION = 1;
    private final int PORT_FROM_ID_POSITION = 2;
    private final int PORT_TO_ID_POSITION = 3;
    private final int PROTOCOL_ID_POSITION = 4;
    private final int DIRECTION_ID_POSITION = 5;
    private final int TYPE_ID_POSITION = 6;
    private final int ID_SIZE = 7;

    private AwsV2SecurityRuleUtils() { }

    public static synchronized AwsV2SecurityRuleUtils getInstance() throws FatalErrorException {
        if (instance == null) {
            instance = new AwsV2SecurityRuleUtils();
        }
        return instance;
    }

    public SecurityGroup getSecurityGroupByName(String name, Ec2Client client) throws FogbowException {
        DescribeSecurityGroupsRequest request = DescribeSecurityGroupsRequest.builder().groupNames(name).build();
        List<SecurityGroup> groups = client.describeSecurityGroups(request).securityGroups();

        if (groups.isEmpty()) {
            throw new FogbowException("There is no security group with the name " + name);
        }

        return groups.get(FIRST_POSITION);
    }

    public String retrieveSecurityGroupName(ResourceType type, String instanceId) throws InvalidParameterException {
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

    public void addIngressRule(SecurityGroup group, SecurityRule rule, Ec2Client client) throws FogbowException {
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

    public void addEgressRule(SecurityGroup group, SecurityRule rule, Ec2Client client) throws FogbowException {
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

    public void validateRule(SecurityRule rule) throws InvalidParameterException {
        if(rule.getProtocol().equals(SecurityRule.Protocol.ANY)) {
            throw new InvalidParameterException("The protocol must be specified");
        }

        Ipv4AddressValidator ipv4AddressValidator = new Ipv4AddressValidator();
        if(!ipv4AddressValidator.validate(rule.getCidr())) {
            throw new InvalidParameterException("The cidr must follow ipv4 pattern");
        }
    }

    public String getId(SecurityRule securityRule, Order order) {
        String ruleKeySeparator = AwsConstants.SECURITY_RULE_ID_SEPARATOR;
        String id = order.getInstanceId() + ruleKeySeparator + securityRule.getCidr() + ruleKeySeparator + securityRule.getPortFrom() +
                ruleKeySeparator + securityRule.getPortTo() + ruleKeySeparator + securityRule.getProtocol() + ruleKeySeparator + securityRule.getDirection().toString() +
                ruleKeySeparator + order.getType().getValue();
        return id;
    }

    public String getId(SecurityRuleInstance securityRule, Order order) {
        String ruleKeySeparator = AwsConstants.SECURITY_RULE_ID_SEPARATOR;
        String id = order.getInstanceId() + ruleKeySeparator + securityRule.getCidr() + ruleKeySeparator + securityRule.getPortFrom() +
                ruleKeySeparator + securityRule.getPortTo() + ruleKeySeparator + securityRule.getProtocol() + ruleKeySeparator + securityRule.getDirection().toString() +
                ruleKeySeparator + order.getType().getValue();
        return id;
    }

    public List<SecurityRuleInstance> getRules(Order majorOrder, List<IpPermission> ipPermissions) {
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

    public Map<String, Object> getRuleFromId(String ruleId) throws InvalidParameterException{
        String fields[] = ruleId.split(AwsConstants.SECURITY_RULE_ID_SEPARATOR);

        if (fields.length != ID_SIZE) {
            throw new InvalidParameterException("");
        }

        SecurityRule rule = new SecurityRule(
                SecurityRule.Direction.valueOf(fields[DIRECTION_ID_POSITION]),
                Integer.valueOf(fields[PORT_FROM_ID_POSITION]),
                Integer.valueOf(fields[PORT_TO_ID_POSITION]),
                fields[CIDR_ID_POSITION],
                SecurityRule.EtherType.IPv4,
                SecurityRule.Protocol.valueOf(fields[PROTOCOL_ID_POSITION])
        );

        Map<String, Object> result = new HashMap<>();
        result.put("rule", rule);
        result.put("type", fields[TYPE_ID_POSITION]);
        result.put("instanceId", fields[INSTANCE_ID_POSITION]);

        return result;
    }

    public void revokeIngressRule(SecurityRule rule, SecurityGroup group, Ec2Client client) throws FogbowException {
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

    public void revokeEgressRule(SecurityRule rule, SecurityGroup group, Ec2Client client) throws FogbowException{
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

    public IpPermission getIpPermission(SecurityRule rule) {
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
