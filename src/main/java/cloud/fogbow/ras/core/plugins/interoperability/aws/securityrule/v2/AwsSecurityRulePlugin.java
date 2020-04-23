package cloud.fogbow.ras.core.plugins.interoperability.aws.securityrule.v2;

import java.security.InvalidParameterException;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;
import java.util.stream.Collectors;

import org.apache.log4j.Logger;

import cloud.fogbow.common.constants.AwsConstants;
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
import software.amazon.awssdk.services.ec2.model.RevokeSecurityGroupEgressRequest;
import software.amazon.awssdk.services.ec2.model.RevokeSecurityGroupIngressRequest;
import software.amazon.awssdk.services.ec2.model.SecurityGroup;
import software.amazon.awssdk.services.ec2.model.Subnet;

public class AwsSecurityRulePlugin implements SecurityRulePlugin<AwsV2User> {

    private static final Logger LOGGER = Logger.getLogger(AwsSecurityRulePlugin.class);
    
    private static final String EGRESS_DIRECTION = "OUT";
    private static final String INGRESS_DIRECTION = "IN";
    private static final String INGRESS_VALUE = "ingress";
    private static final String PUBLIC_IP_RESOURCE_TYPE = "publicIp";

    private static final int CIDR_FIELD_POSITION = 1;
    private static final int DIRECTION_FIELD_POSITION = 6;
    private static final int PORT_FROM_FIELD_POSITION = 3;
    private static final int PORT_TO_FIELD_POSITION = 4;
    private static final int PROTOCOL_FIELD_POSITION = 5;
    private static final int SECURITY_RULE_ID_FIELDS_NUMBER = 8;
    private static final int TYPE_FIELD_POSITION = 7;
    
    protected static final String ALL_PROTOCOLS = "-1";
    protected static final String CIDR_SEPARATOR = "/";
    protected static final String SECURITY_RULE_IDENTIFIER_FORMAT = "%s@%s@%s@%s@%s@%s@%s@%s";
    
    protected static final int FIRST_POSITION = 0;
    
    private String region;

    public AwsSecurityRulePlugin(String confFilePath) {
        Properties properties = PropertiesUtil.readProperties(confFilePath);
        this.region = properties.getProperty(AwsV2ConfigurationPropertyKeys.AWS_REGION_SELECTION_KEY);
    }

    @Override
    public String requestSecurityRule(SecurityRule securityRule, Order majorOrder, AwsV2User cloudUser) throws FogbowException {
        LOGGER.info(String.format(Messages.Info.REQUESTING_INSTANCE_FROM_PROVIDER));
        Ec2Client client = AwsV2ClientUtil.createEc2Client(cloudUser.getToken(), this.region);
        ResourceType resourceType = majorOrder.getType();
        String instanceId = majorOrder.getInstanceId();
        String securityGroupId = getSecurityGroupId(instanceId, resourceType, client);                
        addRuleToSecurityGroup(securityGroupId, securityRule, client);
        return doPackingSecurityRuleId(instanceId, securityRule, resourceType);
    }
    
    @Override
    public List<SecurityRuleInstance> getSecurityRules(Order majorOrder, AwsV2User cloudUser) throws FogbowException {
        LOGGER.info(String.format(Messages.Info.GETTING_INSTANCE_S, majorOrder.getInstanceId()));
        Ec2Client client = AwsV2ClientUtil.createEc2Client(cloudUser.getToken(), this.region);
        ResourceType resourceType = majorOrder.getType();
        String instanceId = majorOrder.getInstanceId();
        return doGetSecurityRules(instanceId, resourceType, client);
    }

    @Override
    public void deleteSecurityRule(String securityRuleId, AwsV2User cloudUser) throws FogbowException {
        LOGGER.info(String.format(Messages.Info.DELETING_INSTANCE_S, securityRuleId));
        Ec2Client client = AwsV2ClientUtil.createEc2Client(cloudUser.getToken(), this.region);
        doDeleteSecurityRule(securityRuleId, client);
    }

    protected void doDeleteSecurityRule(String securityRuleId, Ec2Client client) throws FogbowException {
        String instanceId = extractFieldFrom(securityRuleId, FIRST_POSITION);
        String type = extractFieldFrom(securityRuleId, TYPE_FIELD_POSITION);
        ResourceType resourceType = type.equals(PUBLIC_IP_RESOURCE_TYPE) 
                ? ResourceType.PUBLIC_IP
                : ResourceType.valueOf(type.toUpperCase());

        String groupId = getSecurityGroupId(instanceId, resourceType, client);

        SecurityRule rule = doUnpackingSecurityRuleId(securityRuleId);
        switch (rule.getDirection()) {
        case IN:
            revokeIngressRule(groupId, rule, client);
            break;
        case OUT:
            revokeEgressRule(groupId, rule, client);
        }
    }

    protected void revokeEgressRule(String groupId, SecurityRule rule, Ec2Client client) throws FogbowException {
        IpPermission ipPermission = buildIpPermission(rule);

        RevokeSecurityGroupEgressRequest request = RevokeSecurityGroupEgressRequest.builder()
                .ipPermissions(ipPermission)
                .groupId(groupId)
                .build();
        try {
            client.revokeSecurityGroupEgress(request);
        } catch (SdkException e) {
            throw new UnexpectedException(String.format(Messages.Exception.GENERIC_EXCEPTION, e), e);
        }
    }

    protected void revokeIngressRule(String groupId, SecurityRule rule, Ec2Client client) throws FogbowException {
        IpPermission ipPermission = buildIpPermission(rule);

        RevokeSecurityGroupIngressRequest request = RevokeSecurityGroupIngressRequest.builder()
                .ipPermissions(ipPermission)
                .groupId(groupId)
                .build();
        try {
            client.revokeSecurityGroupIngress(request);
        } catch (SdkException e) {
            throw new UnexpectedException(String.format(Messages.Exception.GENERIC_EXCEPTION, e), e);
        }
    }

    protected SecurityRule doUnpackingSecurityRuleId(String securityRuleId) {
        String[] fields = securityRuleId.split(AwsConstants.SECURITY_RULE_ID_SEPARATOR);
        if (fields.length == SECURITY_RULE_ID_FIELDS_NUMBER) {
            int portFrom = Integer.valueOf(fields[PORT_FROM_FIELD_POSITION]);
            int portTo = Integer.valueOf(fields[PORT_TO_FIELD_POSITION]);
            String cidr = fields[CIDR_FIELD_POSITION] + CIDR_SEPARATOR + fields[CIDR_FIELD_POSITION + 1];
            Direction direction = getDirectionValueFrom(fields[DIRECTION_FIELD_POSITION]);
            EtherType etherType = EtherType.IPv4;
            Protocol protocol = Protocol.valueOf(fields[PROTOCOL_FIELD_POSITION].toUpperCase());
            return new SecurityRule(direction, portFrom, portTo, cidr, etherType, protocol);
        }
        throw new InvalidParameterException(String.format(Messages.Exception.INVALID_PARAMETER_S, securityRuleId));
    }

    protected Direction getDirectionValueFrom(String fieldValue) {
        return Direction.valueOf(fieldValue.equals(INGRESS_VALUE) 
                ? INGRESS_DIRECTION 
                : EGRESS_DIRECTION);
    }
    
    protected String extractFieldFrom(String securityRuleId, int position) {
        String[] fields = securityRuleId.split(AwsConstants.SECURITY_RULE_ID_SEPARATOR);
        if (fields.length == SECURITY_RULE_ID_FIELDS_NUMBER) {
            return fields[position];
        }
        throw new InvalidParameterException(String.format(Messages.Exception.INVALID_PARAMETER_S, securityRuleId));
    }
    
    protected List<SecurityRuleInstance> doGetSecurityRules(String instanceId, ResourceType resourceType,
            Ec2Client client) throws FogbowException {

        String securityGroupId = getSecurityGroupId(instanceId, resourceType, client);
        SecurityGroup securityGroup = getSecurityGroupById(securityGroupId, client);

        List inboundInstances = loadSecurityRuleInstances(instanceId, Direction.IN, securityGroup.ipPermissions());
        List outboundInstances = loadSecurityRuleInstances(instanceId, Direction.OUT, securityGroup.ipPermissionsEgress());

        List<SecurityRuleInstance> resultList = new ArrayList<>();
        resultList.addAll(inboundInstances);
        resultList.addAll(outboundInstances);
        return resultList;
    }
    
    protected List<SecurityRuleInstance> loadSecurityRuleInstances(String instanceId, Direction direction,
            List<IpPermission> ipPermissions) {
        
        List<SecurityRuleInstance> instancesList = new ArrayList<>();

        ipPermissions = ipPermissions.stream()
                .filter(ipPermission -> validateIpPermission(ipPermission))
                .collect(Collectors.toList());

        SecurityRuleInstance instance;
        for (IpPermission ipPermission : ipPermissions) {
            instance = buildSecurityRuleInstance(instanceId, direction, ipPermission);
            instancesList.add(instance);
        }
        return instancesList;
    }
    
    protected SecurityRuleInstance buildSecurityRuleInstance(String instanceId, Direction direction,
            IpPermission ipPermission) {
        
        int portFrom = ipPermission.fromPort();
        int portTo = ipPermission.toPort();
        String cidr = ipPermission.ipRanges().iterator().next().cidrIp();
        Protocol protocol = getProtocolFrom(ipPermission.ipProtocol());
        EtherType etherType = EtherType.IPv4;
        return new SecurityRuleInstance(instanceId, direction, portFrom, portTo, cidr, etherType, protocol);
    }
    
    protected Protocol getProtocolFrom(String ipProtocol) {
        return ipProtocol == ALL_PROTOCOLS 
                ? Protocol.ANY 
                : Protocol.valueOf(ipProtocol.toUpperCase());
    }

    protected boolean validateIpPermission(IpPermission ipPermission) {
        return ipPermission.fromPort() != null 
                && ipPermission.toPort() != null 
                && ipPermission.ipProtocol() != null
                && !ipPermission.ipRanges().isEmpty();
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
    
    protected String doPackingSecurityRuleId(String instanceId, SecurityRule securityRule, ResourceType resourceType) {
        String[] cidrSlice = securityRule.getCidr().split(CIDR_SEPARATOR);
        String securityRuleId = String.format(SECURITY_RULE_IDENTIFIER_FORMAT, 
                instanceId, 
                cidrSlice[FIRST_POSITION], 
                cidrSlice[CIDR_FIELD_POSITION],
                securityRule.getPortFrom(),
                securityRule.getPortTo(),
                securityRule.getProtocol().toString(),
                securityRule.getDirection().toString(),
                resourceType.getValue()
        );
        return securityRuleId;
    }
    
    protected void addRuleToSecurityGroup(String groupId, SecurityRule securityRule, Ec2Client client)
            throws FogbowException {
        
        IpPermission ipPermission = buildIpPermission(securityRule);
        Direction direction = securityRule.getDirection();

        switch (direction) {
        case IN:
            addIngressRule(groupId, ipPermission, client);
            break;
        case OUT:
            addEgressRule(groupId, ipPermission, client);
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
    
    protected IpPermission buildIpPermission(SecurityRule securityRule) {
        int fromPort = securityRule.getPortFrom();
        int toPort = securityRule.getPortTo();
        String ipProtocol = defineIpProtocolFrom(securityRule.getProtocol());
        
        IpRange ipRange = buildIpAddressRange(securityRule);

        IpPermission ipPermission = IpPermission.builder()
                .fromPort(fromPort)
                .toPort(toPort)
                .ipProtocol(ipProtocol)
                .ipRanges(ipRange)
                .build();

        return ipPermission;
    }

    protected String defineIpProtocolFrom(Protocol protocol) {
        return protocol.equals(Protocol.ANY) 
                ? ALL_PROTOCOLS
                : protocol.toString();
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
    
    protected String getSecurityGroupId(String instanceId, ResourceType resourceType, Ec2Client client)
            throws FogbowException {

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
    
}
