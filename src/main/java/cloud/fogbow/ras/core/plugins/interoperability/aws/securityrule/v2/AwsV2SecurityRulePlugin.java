package cloud.fogbow.ras.core.plugins.interoperability.aws.securityrule.v2;

import java.security.InvalidParameterException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.stream.Collectors;

import org.apache.log4j.Logger;

import cloud.fogbow.common.exceptions.FogbowException;
import cloud.fogbow.common.exceptions.InstanceNotFoundException;
import cloud.fogbow.common.exceptions.UnexpectedException;
import cloud.fogbow.common.models.AwsV2User;
import cloud.fogbow.common.util.Ipv4AddressValidator;
import cloud.fogbow.common.util.PropertiesUtil;
import cloud.fogbow.ras.api.http.response.SecurityRuleInstance;
import cloud.fogbow.ras.api.parameters.SecurityRule;
import cloud.fogbow.ras.api.parameters.SecurityRule.Direction;
import cloud.fogbow.ras.api.parameters.SecurityRule.EtherType;
import cloud.fogbow.ras.api.parameters.SecurityRule.Protocol;
import cloud.fogbow.ras.constants.Messages;
import cloud.fogbow.ras.core.models.ResourceType;
import cloud.fogbow.ras.core.models.orders.Order;
import cloud.fogbow.ras.core.plugins.interoperability.SecurityRulePlugin;
import cloud.fogbow.ras.core.plugins.interoperability.aws.AwsV2ClientUtil;
import cloud.fogbow.ras.core.plugins.interoperability.aws.AwsV2CloudUtil;
import cloud.fogbow.ras.core.plugins.interoperability.aws.AwsV2ConfigurationPropertyKeys;
import software.amazon.awssdk.core.exception.SdkException;
import software.amazon.awssdk.services.ec2.Ec2Client;
import software.amazon.awssdk.services.ec2.model.Address;
import software.amazon.awssdk.services.ec2.model.AuthorizeSecurityGroupEgressRequest;
import software.amazon.awssdk.services.ec2.model.AuthorizeSecurityGroupIngressRequest;
import software.amazon.awssdk.services.ec2.model.DescribeSecurityGroupsRequest;
import software.amazon.awssdk.services.ec2.model.DescribeSecurityGroupsResponse;
import software.amazon.awssdk.services.ec2.model.IpPermission;
import software.amazon.awssdk.services.ec2.model.IpRange;
import software.amazon.awssdk.services.ec2.model.SecurityGroup;
import software.amazon.awssdk.services.ec2.model.Subnet;

public class AwsV2SecurityRulePlugin implements SecurityRulePlugin<AwsV2User> {

    private static final Logger LOGGER = Logger.getLogger(AwsV2SecurityRulePlugin.class);
    
    private static final String ALL_PROTOCOLS = "-1";
    private static final String CIDR_SEPARATOR = "/";
    private static final String SECURITY_RULE_IDENTIFIER_FORMAT = "%s@%s@%s@%s@%s@%s@%s@%s";
    
    private String region;
    protected AwsV2SecurityRuleUtils utils;

    public AwsV2SecurityRulePlugin(String confFilePath) {
        Properties properties = PropertiesUtil.readProperties(confFilePath);
        this.region = properties.getProperty(AwsV2ConfigurationPropertyKeys.AWS_REGION_SELECTION_KEY);
        this.utils = AwsV2SecurityRuleUtils.getInstance();
    }

    @Override
    public String requestSecurityRule(SecurityRule securityRule, Order majorOrder, AwsV2User cloudUser) throws FogbowException {
        LOGGER.info(String.format(Messages.Info.REQUESTING_INSTANCE_FROM_PROVIDER));
        Ec2Client client = AwsV2ClientUtil.createEc2Client(cloudUser.getToken(), this.region);
        String securityGroupId = getSecurityGroupId(majorOrder, client);
        addRuleToSecurityGroup(securityGroupId, securityRule, client);
        return generateSecurityRuleId(securityRule, majorOrder);
    }
    
    protected String generateSecurityRuleId(SecurityRule securityRule, Order order) {
        String[] cidrSlice = securityRule.getCidr().split(CIDR_SEPARATOR);
        String securityRuleId = String.format(SECURITY_RULE_IDENTIFIER_FORMAT, 
                order.getInstanceId(), 
                cidrSlice[0], 
                cidrSlice[1],
                securityRule.getPortFrom(),
                securityRule.getPortTo(),
                securityRule.getProtocol().toString(),
                securityRule.getDirection().toString(),
                order.getType().getValue()
        );
        return securityRuleId;
    }
    
    protected void addRuleToSecurityGroup(String groupId, SecurityRule securityRule, Ec2Client client)
            throws FogbowException {
        
        IpPermission ipPermission = getIpPermission(securityRule);
        Direction direction = securityRule.getDirection();

        switch (direction) {
        case IN:
            addIngressRule(groupId, ipPermission, client);
            break;
        case OUT:
            addEgressRule(groupId, ipPermission, client);
            break;
        default:
            throw new InvalidParameterException(String.format(Messages.Exception.INVALID_PARAMETER_S, direction));
        }
    }

    protected void addEgressRule(String groupId, IpPermission ipPermission, Ec2Client client) throws FogbowException {
        AuthorizeSecurityGroupEgressRequest request = AuthorizeSecurityGroupEgressRequest.builder()
                .groupId(groupId)
                .ipPermissions(ipPermission)
                .build();
        try {
            client.authorizeSecurityGroupEgress(request);
        } catch (SdkException e) {
            throw new UnexpectedException(String.format(Messages.Exception.GENERIC_EXCEPTION, e), e);
        }
    }
    
    protected void addIngressRule(String groupId, IpPermission ipPermission, Ec2Client client)
            throws FogbowException {
        
        AuthorizeSecurityGroupIngressRequest request = AuthorizeSecurityGroupIngressRequest.builder()
                .groupId(groupId)
                .ipPermissions(ipPermission)
                .build();
        try {
            client.authorizeSecurityGroupIngress(request);
        } catch (SdkException e) {
            throw new UnexpectedException(String.format(Messages.Exception.GENERIC_EXCEPTION, e), e);
        }
    }
    
    protected IpPermission getIpPermission(SecurityRule securityRule) {
        int fromPort = securityRule.getPortFrom();
        int toPort = securityRule.getPortTo();
        String ipProtocol = securityRule.getProtocol().equals(Protocol.ANY) ? ALL_PROTOCOLS
                : securityRule.getProtocol().toString();
        
        IpRange ipRange = buildIpAddressRange(securityRule);

        IpPermission ipPermission = IpPermission.builder()
                .fromPort(fromPort)
                .toPort(toPort)
                .ipProtocol(ipProtocol)
                .ipRanges(ipRange)
                .build();

        return ipPermission;
    }

    protected IpRange buildIpAddressRange(SecurityRule securityRule) {
        String cidrIp = securityRule.getCidr();
        validateIpAddress(cidrIp);
        
        IpRange ipRange = IpRange.builder()
                .cidrIp(cidrIp)
                .build();
        
        return ipRange;
    }
    
    protected void validateIpAddress(String cidrIp) {
        Ipv4AddressValidator validator = new Ipv4AddressValidator();
        String[] cidrSlice = cidrIp.split(CIDR_SEPARATOR);
        String ipAddress = cidrSlice[0];
        if (!validator.validate(ipAddress)) {
            throw new InvalidParameterException(String.format(Messages.Exception.INVALID_CIDR_FORMAT, cidrIp));
        }
    }

    protected String getSecurityGroupId(Order majorOrder, Ec2Client client) throws FogbowException {
        String instanceId = majorOrder.getInstanceId();
        ResourceType resourceType = majorOrder.getType();
        
        switch (resourceType) {
        case PUBLIC_IP:
            Address address = AwsV2CloudUtil.getAddressById(instanceId, client);
            return AwsV2CloudUtil.getGroupIdFrom(address.tags());
        case NETWORK:
            Subnet subnet = AwsV2CloudUtil.getSubnetById(instanceId, client);
            return AwsV2CloudUtil.getGroupIdFrom(subnet.tags());
        default:
            throw new InvalidParameterException(String.format(Messages.Exception.INVALID_PARAMETER_S, resourceType));
        }
    }
    
    @Override
    public List<SecurityRuleInstance> getSecurityRules(Order majorOrder, AwsV2User cloudUser) throws FogbowException {
        LOGGER.info(String.format(Messages.Info.GETTING_INSTANCE_S, majorOrder.getInstanceId()));
        Ec2Client client = AwsV2ClientUtil.createEc2Client(cloudUser.getToken(), this.region);
        String securityGroupId = getSecurityGroupId(majorOrder, client);
        SecurityGroup securityGroup = getSecurityGroupById(securityGroupId, client);
        return doGetSecurityRules(majorOrder, securityGroup);
    }

    protected List<SecurityRuleInstance> doGetSecurityRules(Order majorOrder, SecurityGroup securityGroup) {
        List inboundInstances = loadRuleInstances(Direction.IN, majorOrder, securityGroup.ipPermissions());
        List outboundInstances = loadRuleInstances(Direction.OUT, majorOrder, securityGroup.ipPermissionsEgress());

        List<SecurityRuleInstance> resultList = new ArrayList<>();
        resultList.addAll(inboundInstances);
        resultList.addAll(outboundInstances);
        return resultList;
    }
    
    protected List<SecurityRuleInstance> loadRuleInstances(Direction direction, Order order, List<IpPermission> ipPermissions) {
        List<SecurityRuleInstance> ruleInstancesList = new ArrayList<>();
        
        ipPermissions = ipPermissions.stream()
                .filter(ipPermission -> validateIpPermission(ipPermission))
                .collect(Collectors.toList());
        
        SecurityRuleInstance ruleInstance;
        for (IpPermission ipPermission : ipPermissions) {
            ruleInstance = buildSecurityRuleInstance(direction, order, ipPermission);
            ruleInstancesList.add(ruleInstance);
        }
        return ruleInstancesList;
    }
    
    protected SecurityRuleInstance buildSecurityRuleInstance(Direction direction, Order order, IpPermission ipPermission) {
        int portFrom = ipPermission.fromPort();
        int portTo = ipPermission.toPort();
        String cidr = ipPermission.ipRanges().iterator().next().cidrIp();
        String id = order.getInstanceId();
        Protocol protocol = Protocol.valueOf(ipPermission.ipProtocol().toUpperCase());
        EtherType etherType = EtherType.IPv4;
        return new SecurityRuleInstance(id, direction, portFrom, portTo, cidr, etherType, protocol);
    }

    protected boolean validateIpPermission(IpPermission ipPermission) {
        return ipPermission.fromPort() != null 
                && ipPermission.toPort() != null 
                && ipPermission.ipRanges() != null
                && ipPermission.ipProtocol() != null;
    }
    
    protected SecurityGroup getSecurityGroupById(String groupId, Ec2Client client) throws FogbowException {
        DescribeSecurityGroupsRequest request = DescribeSecurityGroupsRequest.builder()
                .groupIds(groupId)
                .build();

        DescribeSecurityGroupsResponse response = doDescribeSecurityGroupsRequest(request, client);
        return getSecurityGroupFrom(response);
    }

    protected SecurityGroup getSecurityGroupFrom(DescribeSecurityGroupsResponse response)
            throws FogbowException {
        
        if (response != null && !response.securityGroups().isEmpty()) {
            return response.securityGroups().listIterator().next();
        }
        throw new InstanceNotFoundException(Messages.Exception.INSTANCE_NOT_FOUND);
    }

    protected DescribeSecurityGroupsResponse doDescribeSecurityGroupsRequest(DescribeSecurityGroupsRequest request,
            Ec2Client client) throws FogbowException {
        try {
            return client.describeSecurityGroups(request);
        } catch (SdkException e) {
            throw new UnexpectedException(String.format(Messages.Exception.GENERIC_EXCEPTION, e), e);
        }
    }

    @Override
    public void deleteSecurityRule(String securityRuleId, AwsV2User cloudUser) throws FogbowException {
        LOGGER.info(String.format(Messages.Info.DELETING_INSTANCE_S, securityRuleId));
        Ec2Client client = AwsV2ClientUtil.createEc2Client(cloudUser.getToken(), this.region);
        Map<String, Object> ruleRepresentation = utils.getRuleFromId(securityRuleId);
        SecurityRule rule = (SecurityRule) ruleRepresentation.get("rule");
        String instanceId = (String) ruleRepresentation.get("instanceId");

        String strType = (String) ruleRepresentation.get("type");
        ResourceType type = ResourceType.valueOf(strType);
        SecurityGroup group = utils.getSecurityGroup(instanceId, type, client);

        switch (rule.getDirection()) {
            case IN:
                utils.revokeIngressRule(rule, group, client);
                break;
            case OUT:
                utils.revokeEgressRule(rule, group, client);
        }

    }
}
