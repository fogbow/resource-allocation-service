package cloud.fogbow.ras.core.plugins.interoperability.aws.securityrule.v2;

import cloud.fogbow.common.constants.AwsConstants;
import cloud.fogbow.common.exceptions.*;
import cloud.fogbow.common.util.Ipv4AddressValidator;
import cloud.fogbow.ras.api.http.response.SecurityRuleInstance;
import cloud.fogbow.ras.api.parameters.SecurityRule;
import cloud.fogbow.ras.constants.Messages;
import cloud.fogbow.ras.core.models.ResourceType;
import cloud.fogbow.ras.core.models.orders.Order;
import software.amazon.awssdk.core.exception.SdkException;
import software.amazon.awssdk.services.ec2.Ec2Client;
import software.amazon.awssdk.services.ec2.model.*;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class AwsV2SecurityRuleUtils {
    private static AwsV2SecurityRuleUtils instance;

    private final int FIRST_POSITION = 0;
    private final int INSTANCE_ID_POSITION = 0;
    private final int CIDR_ID_POSITION = 1;
    private final int PORT_FROM_ID_POSITION = 3;
    private final int PORT_TO_ID_POSITION = 4;
    private final int PROTOCOL_ID_POSITION = 5;
    private final int DIRECTION_ID_POSITION = 6;
    private final int TYPE_ID_POSITION = 7;
    private final int ID_SIZE = 8;

    private static final String AWS_TAG_GROUP_ID = "groupId";

    private static final String INGRESS_DIRECTION = "IN";
    private static final String EGRESS_DIRECTION = "OUT";
    private static final String INGRESS_DIRECTION_VALUE = "ingress";

    private static final String CIDR_NETWORK_BITS_SEPARATOR = "/";

    private static final int DIRECTION_ENUM_POSITION = 0;
    private static final int PROTOCOL_ENUM_POSITION = 1;
    private static final int RESOURCE_TYPE_ENUM_POSITION = 2;

    private static final String PUBLIC_IP = "publicip";

    private AwsV2SecurityRuleUtils() { }

    public static synchronized AwsV2SecurityRuleUtils getInstance() throws FatalErrorException {
        if (instance == null) {
            instance = new AwsV2SecurityRuleUtils();
        }
        return instance;
    }

    protected SecurityGroup getSecurityGroupBySubnetId(String instanceId, Ec2Client client) throws FogbowException {
        String groupId = getGroupIdBySubnet(instanceId, client);

        DescribeSecurityGroupsRequest request = DescribeSecurityGroupsRequest.builder()
                .groupIds(groupId)
                .build();

        List<SecurityGroup> groups = client.describeSecurityGroups(request).securityGroups();

        if (groups.isEmpty()) {
            throw new FogbowException(String.format(Messages.Exception.NO_SECURITY_GROUP_FOUND, groupId));
        }

        return groups.get(FIRST_POSITION);
    }

    protected SecurityGroup getSecurityGroupByAllocationId(String allocation, Ec2Client client) throws FogbowException {
        Address address = getAddress(allocation, client);

        SecurityGroup group = null;

        String groupId = "";
        for (Tag tag: address.tags()) {
            if (tag.key().equals(AWS_TAG_GROUP_ID)) {
                groupId = tag.value();
            }
        }

        return getGroupById(groupId, client);
    }

    protected Address getAddress(String allocationId, Ec2Client client) throws FogbowException{
        DescribeAddressesRequest addressesRequest = DescribeAddressesRequest.builder()
                .allocationIds(allocationId).build();

        Address address = null;

        try {
            address = client.describeAddresses(addressesRequest).addresses().get(FIRST_POSITION);
        } catch(SdkException ex) {
            throw new InstanceNotFoundException(Messages.Exception.INSTANCE_NOT_FOUND);
        }

        return address;
    }

    protected SecurityGroup getGroupById(String groupId, Ec2Client client) throws FogbowException{
        DescribeSecurityGroupsRequest request = DescribeSecurityGroupsRequest.builder().groupIds(groupId).build();
        SecurityGroup group = null;

        try {
            group = client.describeSecurityGroups(request).securityGroups().iterator().next();
        } catch (SdkException ex) {
            throw new InstanceNotFoundException(Messages.Exception.INSTANCE_NOT_FOUND);
        }

        return group;
    }

    public SecurityGroup getSecurityGroup(String instanceId, ResourceType type, Ec2Client client) throws FogbowException{
        SecurityGroup group = null;

        switch (type) {
            case PUBLIC_IP:
                group = getSecurityGroupByAllocationId(instanceId, client);
                break;
            case NETWORK:
                group = getSecurityGroupBySubnetId(instanceId, client);
                break;
        }

        return group;
    }

    protected String getGroupIdBySubnet(String subnetId, Ec2Client client)
            throws UnexpectedException, InstanceNotFoundException {

        Subnet subnet = getSubnetById(subnetId, client);
        for (Tag tag : subnet.tags()) {
            if (tag.key().equals(AWS_TAG_GROUP_ID)) {
                return tag.value();
            }
        }
        throw new InstanceNotFoundException(Messages.Exception.INSTANCE_NOT_FOUND);
    }

    protected Subnet getSubnetById(String subnetId, Ec2Client client)
            throws UnexpectedException, InstanceNotFoundException {

        DescribeSubnetsResponse response = doDescribeSubnetsRequests(subnetId, client);
        if (response != null && !response.subnets().isEmpty()) {
            return response.subnets().listIterator().next();
        }
        throw new InstanceNotFoundException(Messages.Exception.INSTANCE_NOT_FOUND);
    }

    protected DescribeSubnetsResponse doDescribeSubnetsRequests(String subnetId, Ec2Client client)
            throws UnexpectedException {

        DescribeSubnetsRequest request = DescribeSubnetsRequest.builder()
                .subnetIds(subnetId)
                .build();
        try {
            return client.describeSubnets(request);
        } catch (Exception e) {
            throw new UnexpectedException(String.format(Messages.Exception.GENERIC_EXCEPTION, e), e);
        }
    }

    public void addIngressRule(SecurityGroup group, SecurityRule rule, Ec2Client client) throws FogbowException {
        IpPermission ipPermission = getIpPermission(rule);

        AuthorizeSecurityGroupIngressRequest request = AuthorizeSecurityGroupIngressRequest.builder()
                .groupId(group.groupId())
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
        if (rule.getProtocol().equals(SecurityRule.Protocol.ANY)) {
            throw new InvalidParameterException(Messages.Exception.NO_PROTOCOL_SPECIFIED);
        }

        Ipv4AddressValidator ipv4AddressValidator = new Ipv4AddressValidator();
        if (!ipv4AddressValidator.validate(rule.getCidr().split(CIDR_NETWORK_BITS_SEPARATOR)[0])) {
            throw new InvalidParameterException(String.format(Messages.Exception.INVALID_CIDR_FORMAT, rule.getCidr()));
        }
    }

    public String getId(SecurityRule securityRule, Order order) {
        String ruleKeySeparator = AwsConstants.SECURITY_RULE_ID_SEPARATOR;
        String requiredEnums[] = getRequiredEnums(securityRule.getDirection().toString(), securityRule.getProtocol().toString(), order.getType().getValue());

        String id = order.getInstanceId() + ruleKeySeparator + securityRule.getCidr().split(CIDR_NETWORK_BITS_SEPARATOR)[0] + ruleKeySeparator +
                securityRule.getCidr().split(CIDR_NETWORK_BITS_SEPARATOR)[1] + ruleKeySeparator + securityRule.getPortFrom() +
                ruleKeySeparator + securityRule.getPortTo() + ruleKeySeparator + requiredEnums[PROTOCOL_ENUM_POSITION] + ruleKeySeparator +
                requiredEnums[DIRECTION_ENUM_POSITION] + ruleKeySeparator + requiredEnums[RESOURCE_TYPE_ENUM_POSITION];
        return id;
    }

    public String getId(SecurityRuleInstance securityRule, Order order) {
        String ruleKeySeparator = AwsConstants.SECURITY_RULE_ID_SEPARATOR;
        String requiredEnums[] = getRequiredEnums(securityRule.getDirection().toString(), securityRule.getProtocol().toString(), order.getType().getValue());

        String id = order.getInstanceId() + ruleKeySeparator + securityRule.getCidr().split(CIDR_NETWORK_BITS_SEPARATOR)[0] + ruleKeySeparator +
                securityRule.getCidr().split(CIDR_NETWORK_BITS_SEPARATOR)[1] + ruleKeySeparator + securityRule.getPortFrom() +
                ruleKeySeparator + securityRule.getPortTo() + ruleKeySeparator + requiredEnums[PROTOCOL_ENUM_POSITION] + ruleKeySeparator +
                requiredEnums[DIRECTION_ENUM_POSITION] + ruleKeySeparator + requiredEnums[RESOURCE_TYPE_ENUM_POSITION];
        return id;
    }

    public List<SecurityRuleInstance> getRules(Order majorOrder, List<IpPermission> ipPermissions, SecurityRule.Direction direction) {
        List<SecurityRuleInstance> ruleInstances = new ArrayList<>();

        ipPermissions = ipPermissions.stream().filter(ipPermission -> validateIpPermission(ipPermission)).collect(Collectors.toList());

        for (IpPermission ipPermission : ipPermissions) {
            int portFrom = ipPermission.fromPort();
            int portTo = ipPermission.toPort();
            String cidr = ipPermission.ipRanges().iterator().next().cidrIp();
            String protocol = ipPermission.ipProtocol();

            SecurityRuleInstance instance = new SecurityRuleInstance("",
                    direction,
                    portFrom, portTo, cidr,
                    SecurityRule.EtherType.IPv4,
                    SecurityRule.Protocol.valueOf(protocol.toUpperCase())
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
            throw new InvalidParameterException(String.format(Messages.Exception.INVALID_PARAMETER_S, ruleId));
        }

        String cidr = fields[CIDR_ID_POSITION] + CIDR_NETWORK_BITS_SEPARATOR + fields[CIDR_ID_POSITION+1];

        SecurityRule rule = new SecurityRule(
                SecurityRule.Direction.valueOf(fields[DIRECTION_ID_POSITION]),
                Integer.valueOf(fields[PORT_FROM_ID_POSITION]),
                Integer.valueOf(fields[PORT_TO_ID_POSITION]),
                cidr,
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

    protected boolean validateIpPermission(IpPermission ipPermission) {
        return ipPermission.fromPort() != null && ipPermission.toPort() != null && ipPermission.ipRanges() != null && ipPermission.ipProtocol() != null;
    }

    private String[] getRequiredEnums(String direction, String protocol, String type) {
        String requiredEnums[] = new String[3];

        if (direction.equals(INGRESS_DIRECTION_VALUE)) {
            requiredEnums[DIRECTION_ENUM_POSITION] = INGRESS_DIRECTION;
        } else {
            requiredEnums[DIRECTION_ENUM_POSITION] = EGRESS_DIRECTION;
        }

        requiredEnums[PROTOCOL_ENUM_POSITION] = protocol.toUpperCase();

        if (type.toLowerCase().equals(PUBLIC_IP)) {
            type = "public_ip";
        }

        requiredEnums[RESOURCE_TYPE_ENUM_POSITION] = type.toUpperCase();

        return requiredEnums;
    }
}
