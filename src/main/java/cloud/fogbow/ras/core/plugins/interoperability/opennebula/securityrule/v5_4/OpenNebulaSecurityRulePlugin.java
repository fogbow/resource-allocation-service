package cloud.fogbow.ras.core.plugins.interoperability.opennebula.securityrule.v5_4;

import java.util.ArrayList;
import java.util.List;
import java.util.Properties;

import org.apache.commons.lang.StringUtils;
import org.apache.commons.net.util.SubnetUtils;
import org.apache.commons.net.util.SubnetUtils.SubnetInfo;
import org.apache.log4j.Logger;
import org.opennebula.client.Client;
import org.opennebula.client.OneResponse;
import org.opennebula.client.secgroup.SecurityGroup;
import org.opennebula.client.vnet.VirtualNetwork;

import cloud.fogbow.common.exceptions.FatalErrorException;
import cloud.fogbow.common.exceptions.FogbowException;
import cloud.fogbow.common.exceptions.InstanceNotFoundException;
import cloud.fogbow.common.exceptions.InvalidParameterException;
import cloud.fogbow.common.exceptions.UnexpectedException;
import cloud.fogbow.common.models.CloudUser;
import cloud.fogbow.common.util.PropertiesUtil;
import cloud.fogbow.ras.api.http.response.SecurityRuleInstance;
import cloud.fogbow.ras.api.parameters.SecurityRule;
import cloud.fogbow.ras.api.parameters.SecurityRule.Direction;
import cloud.fogbow.ras.api.parameters.SecurityRule.EtherType;
import cloud.fogbow.ras.api.parameters.SecurityRule.Protocol;
import cloud.fogbow.ras.constants.Messages;
import cloud.fogbow.ras.constants.SystemConstants;
import cloud.fogbow.ras.core.models.orders.Order;
import cloud.fogbow.ras.core.plugins.interoperability.SecurityRulePlugin;
import cloud.fogbow.ras.core.plugins.interoperability.cloudstack.securityrule.v4_9.CidrUtils;
import cloud.fogbow.ras.core.plugins.interoperability.opennebula.OpenNebulaClientUtil;
import cloud.fogbow.ras.core.plugins.interoperability.opennebula.OpenNebulaConfigurationPropertyKeys;

public class OpenNebulaSecurityRulePlugin implements SecurityRulePlugin<CloudUser> {

    public static final Logger LOGGER = Logger.getLogger(OpenNebulaSecurityRulePlugin.class);

    private static final String ALL_ADDRESSES_REMOTE_PREFIX = "0.0.0.0/0";
    private static final String CIDR_SEPARATOR = "/";
    private static final String CONTENT_SEPARATOR = ",";
    private static final String EMPTY_STRING = "";
    private static final String ID_SEPARATOR = "@";
    private static final String RANGE_SEPARATOR = ":";
    private static final String SECURITY_GROUPS_PATH = "/VNET/TEMPLATE/SECURITY_GROUPS";

    // directions template values
    private static final String INBOUND_TEMPLATE_VALUE = "inbound";
    private static final String OUTBOUND_TEMPLATE_VALUE = "outbound";

    // fields indexes for instance id
    private static final int PROTOCOL_INDEX = 0;
    private static final int IP_INDEX = 1;
    private static final int SIZE_INDEX = 2;
    private static final int RANGE_INDEX = 3;
    private static final int TYPE_INDEX = 4;
    private static final int NETWORK_ID_INDEX = 5;
    private static final int SECURITY_GROUP_INDEX = 6;

    // protocols template values
    private static final String ALL_TEMPLATE_VALUE = "ALL";
    private static final String IPSEC_TEMPLATE_VALUE = "IPSEC";
    private static final String ICMP_TEMPLATE_VALUE = "ICMP";
    private static final String ICMPV6_TEMPLATE_VALUE = "ICMPV6";
    private static final String TCP_TEMPLATE_VALUE = "TCP";
    private static final String UDP_TEMPLATE_VALUE = "UDP";

    // range ports
    private static final int MINIMUM_RANGE_PORT = 1;
    private static final int MAXIMUM_RANGE_PORT = 65536;

    private static final int BASE_VALUE = 2;
    private static final int IPV4_AMOUNT_BITS = 32;
    private static final int IPV6_AMOUNT_BITS = 128;
    private static final int INT_ERROR_CODE = -1;
    private static final int PORT_FROM_INDEX = 0;
    private static final int PORT_TO_INDEX = 1;
    private static final int SECURITY_RULE_ID_FIELDS_NUMBER = 7;

    private String endpoint;

    public OpenNebulaSecurityRulePlugin(String confFilePath) throws FatalErrorException {
        Properties properties = PropertiesUtil.readProperties(confFilePath);
        this.endpoint = properties.getProperty(OpenNebulaConfigurationPropertyKeys.OPENNEBULA_RPC_ENDPOINT_KEY);
    }

    @Override
    public String requestSecurityRule(SecurityRule securityRule, Order majorOrder, CloudUser cloudUser)
            throws FogbowException {

        Client client = OpenNebulaClientUtil.createClient(this.endpoint, cloudUser.getToken());
        SecurityGroup securityGroup = getSecurityGroup(client, majorOrder);
        Rule rule = doCreateSecurityRuleRequest(securityRule, securityGroup);
        return doRequestSecurityRule(securityGroup, rule);
    }
    
    @Override
    public List<SecurityRuleInstance> getSecurityRules(Order majorOrder, CloudUser cloudUser) throws FogbowException {
        Client client = OpenNebulaClientUtil.createClient(this.endpoint, cloudUser.getToken());
        SecurityGroup securityGroup = getSecurityGroup(client, majorOrder);
        return doGetSecurityRules(securityGroup);
    }

    @Override
    public void deleteSecurityRule(String securityRuleId, CloudUser cloudUser) throws FogbowException {
        Client client = OpenNebulaClientUtil.createClient(this.endpoint, cloudUser.getToken());
        Rule rule = doUnpakingSecurityRuleId(securityRuleId);
        String securityGroupId = rule.getGroupId();
        doDeleteSecurityRule(client, rule, securityGroupId);
    }

    private void doDeleteSecurityRule(Client client, Rule rule, String securityGroupId)
            throws FogbowException {

        SecurityGroup securityGroup = OpenNebulaClientUtil.getSecurityGroup(client, securityGroupId);
        GetSecurityGroupResponse group = doGetSecurityGroupResponse(securityGroup);

        List<Rule> rules = group.getTemplate().getRules();
        if (rules != null && rules.contains(rule))
            rules.remove(rule);

        String template = doCreateSecurityGroupTemplate(group, rules);
        updateSecurityGroup(securityGroup, template);
    }

    private Rule doUnpakingSecurityRuleId(String securityRuleId) throws FogbowException {
        String[] fields = securityRuleId.split(ID_SEPARATOR);
        if (fields.length == SECURITY_RULE_ID_FIELDS_NUMBER) {
            String protocol = getValueFrom(fields[PROTOCOL_INDEX]);
            String ip = getValueFrom(fields[IP_INDEX]);
            String size = getValueFrom(fields[SIZE_INDEX]);
            String range = getValueFrom(fields[RANGE_INDEX]);
            String type = getValueFrom(fields[TYPE_INDEX]);
            String networkId = getValueFrom(fields[NETWORK_ID_INDEX]);
            String groupId = getValueFrom(fields[SECURITY_GROUP_INDEX]);

            Rule rule = Rule.builder().protocol(protocol).ip(ip).size(size).range(range)
                    .type(type).networkId(networkId).groupId(groupId).build();

            return rule;
        }
        throw new InvalidParameterException(String.format(Messages.Exception.INVALID_PARAMETER_S, securityRuleId));
    }

    private String getValueFrom(String value) {
        return !value.isEmpty() ? value : null;
    }
    
    private List<SecurityRuleInstance> doGetSecurityRules(SecurityGroup securityGroup) {
        List<SecurityRuleInstance> instances = new ArrayList();
        GetSecurityGroupResponse group = doGetSecurityGroupResponse(securityGroup);
        List<Rule> rules = group.getTemplate().getRules();
        for (Rule rule : rules) {
            SecurityRuleInstance instance = buildSecurityRule(rule);
            instances.add(instance);
        }
        return instances;
    }

    private SecurityRuleInstance buildSecurityRule(Rule rule) {
        Direction direction = getDirectionFrom(rule.getType());
        EtherType etherType = getEtherTypeFrom(rule.getIp());
        Protocol protocol = getProtocolFrom(rule.getProtocol());
        int portFrom = getPortInRange(rule.getRange(), PORT_FROM_INDEX);
        int portTo = getPortInRange(rule.getRange(), PORT_TO_INDEX);
        String cidr = buildAddressCidr(rule);
        String id = doPackingSecurityRuleId(rule);
        return new SecurityRuleInstance(id, direction, portFrom, portTo, cidr, etherType, protocol);
    }

    private String buildAddressCidr(Rule rule) {
        String size = rule.getSize();
        String ipAddress = rule.getIp();
        EtherType etherType = getEtherTypeFrom(ipAddress);
        if (etherType != null && (size != null && !size.isEmpty())) {
            int range = Integer.parseInt(size);
            int cidr = calculateCidr(range, etherType);
            return ipAddress + CIDR_SEPARATOR + String.valueOf(cidr);
        }
        return ALL_TEMPLATE_VALUE;
    }

    protected int calculateCidr(int range, EtherType etherType) {
        int amountBits = etherType.equals(EtherType.IPv4) ? IPV4_AMOUNT_BITS : IPV6_AMOUNT_BITS;
        int exponent = 1;
        int value = 0;
        for (int i = 0; i < amountBits; i++) {
            if (exponent >= range) {
                value = amountBits - i;
                return value;
            } else {
                exponent *= BASE_VALUE;
            }
        }
        return value;
    }

    private int getPortInRange(String range, int index) {
        if (range == null || range.isEmpty()) {
            switch (index) {
            case PORT_FROM_INDEX:
                return MINIMUM_RANGE_PORT;
            case PORT_TO_INDEX:
                return MAXIMUM_RANGE_PORT;
            }
        }
        try {
            String[] splitPorts = range.split(RANGE_SEPARATOR);
            if (splitPorts.length == 1) {
                return Integer.parseInt(range);
            } else if (splitPorts.length == 2) {
                return Integer.parseInt(splitPorts[index]);
            } else {
                LOGGER.warn(String.format(Messages.Warn.INCONSISTENT_RANGE_S, range));
            }
        } catch (Exception e) {
            LOGGER.warn(String.format(Messages.Exception.INVALID_PARAMETER_S, range), e);
        }
        return INT_ERROR_CODE;
    }

    private Protocol getProtocolFrom(String protocol) {
        if (protocol != null && !protocol.isEmpty()) {
            switch (protocol) {
            case TCP_TEMPLATE_VALUE:
                return SecurityRule.Protocol.TCP;
            case UDP_TEMPLATE_VALUE:
                return SecurityRule.Protocol.UDP;
            case ICMP_TEMPLATE_VALUE:
            case ICMPV6_TEMPLATE_VALUE:
                return SecurityRule.Protocol.ICMP;
            case ALL_TEMPLATE_VALUE:
                return SecurityRule.Protocol.ANY;
            case IPSEC_TEMPLATE_VALUE:
            default:
                LOGGER.warn(String.format(Messages.Warn.INCONSISTENT_PROTOCOL_S, protocol));
                return null;
            }
        }
        LOGGER.warn(String.format(Messages.Exception.INVALID_PARAMETER_S, protocol));
        return null;
    }

    private EtherType getEtherTypeFrom(String ipAddress) {
        if (ipAddress != null && !ipAddress.isEmpty()) {
            if (CidrUtils.isIpv4(ipAddress)) {
                return SecurityRule.EtherType.IPv4;
            } else if (CidrUtils.isIpv6(ipAddress)) {
                return SecurityRule.EtherType.IPv6;
            }
        }
        LOGGER.warn(String.format(Messages.Exception.INVALID_PARAMETER_S, ipAddress));
        return null;
    }

    private Direction getDirectionFrom(String type) {
        if (type != null && !type.isEmpty()) {
            switch (type) {
            case INBOUND_TEMPLATE_VALUE:
                return Direction.IN;
            case OUTBOUND_TEMPLATE_VALUE:
                return Direction.OUT;
            default:
                LOGGER.warn(String.format(Messages.Warn.INCONSISTENT_DIRECTION, type));
                return null;
            }
        }
        LOGGER.warn(String.format(Messages.Exception.INVALID_PARAMETER_S, type));
        return null;
    }
    
    private String doRequestSecurityRule(SecurityGroup securityGroup, Rule rule) throws FogbowException {

        GetSecurityGroupResponse group = doGetSecurityGroupResponse(securityGroup);
        List<Rule> rules = group.getTemplate().getRules();
        rules.add(rule);

        String template = doCreateSecurityGroupTemplate(group, rules);
        updateSecurityGroup(securityGroup, template);
        return doPackingSecurityRuleId(rule);
    }

    private void updateSecurityGroup(SecurityGroup securityGroup, String template) throws FogbowException {
        OneResponse response = securityGroup.update(template);
        if (response.isError()) {
            String message = String.format(Messages.Error.ERROR_WHILE_UPDATING_SECURITY_GROUPS, template);
            LOGGER.error(message);
            throw new UnexpectedException(message);
        }
    }

    private String doPackingSecurityRuleId(Rule rule) {
        String[] attributes = new String[7];
        attributes[PROTOCOL_INDEX] = rule.getProtocol();
        attributes[IP_INDEX] = rule.getGroupId();
        attributes[SIZE_INDEX] = rule.getSize();
        attributes[RANGE_INDEX] = rule.getRange();
        attributes[TYPE_INDEX] = rule.getType();
        attributes[NETWORK_ID_INDEX] = rule.getNetworkId() != null ? String.valueOf(rule.getNetworkId()) : EMPTY_STRING;
        attributes[SECURITY_GROUP_INDEX] = rule.getGroupId();

        String instanceId = StringUtils.join(attributes, ID_SEPARATOR);
        return instanceId;
    }

    private String doCreateSecurityGroupTemplate(GetSecurityGroupResponse group, List<Rule> rules) {
        String id = group.getId();
        String name = group.getName();

        CreateSecurityGroupRequest template = CreateSecurityGroupRequest.builder()
                .id(id)
                .name(name)
                .rules(rules)
                .build();

        return template.marshalTemplate();
    }

    private GetSecurityGroupResponse doGetSecurityGroupResponse(SecurityGroup securityGroup) {
        String xml = securityGroup.info().getMessage();
        return GetSecurityGroupResponse.unmarshal(xml);
    }

    private Rule doCreateSecurityRuleRequest(SecurityRule securityRule, SecurityGroup securityGroup) {
        int portFrom = securityRule.getPortFrom();
        int portTo = securityRule.getPortTo();

        String[] addressCidrSliced = getCidrFrom(securityRule);
        String ip = addressCidrSliced[0];
        String size = addressCidrSliced[1];
        String protocol = securityRule.getProtocol().toString().toUpperCase();
        String type = getRuleType(securityRule.getDirection());
        String groupId = securityGroup.getId();
        String range = portFrom == portTo ? String.valueOf(portFrom) : portFrom + RANGE_SEPARATOR + portTo;

        Rule request = Rule.builder()
                .protocol(protocol)
                .ip(ip)
                .size(size)
                .range(range)
                .type(type)
                .groupId(groupId)
                .build();

        return request;
    }

    private String getRuleType(Direction direction) {
        String type = null;
        switch (direction) {
        case IN:
            type = INBOUND_TEMPLATE_VALUE;
        case OUT:
            type = OUTBOUND_TEMPLATE_VALUE;
        }
        return type;
    }

    // TODO check this method better
    private String[] getCidrFrom(SecurityRule rule) {
        SubnetUtils subnetUtils = new SubnetUtils(rule.getCidr());
        SubnetInfo subnetInfo = subnetUtils.getInfo();

        String[] cidrAddress = new String[2];
        if (!subnetInfo.getCidrSignature().equals(ALL_ADDRESSES_REMOTE_PREFIX)) {
            cidrAddress[0] = subnetInfo.getLowAddress();
            cidrAddress[1] = String.valueOf(subnetInfo.getAddressCountLong());
        }
        return cidrAddress;
    }

    private SecurityGroup getSecurityGroup(Client client, Order majorOrder) throws FogbowException {
        String securityGroupName = retrieveSecurityGroupName(majorOrder);
        String virtualNetworkId = majorOrder.getInstanceId();
        VirtualNetwork virtualNetwork = OpenNebulaClientUtil.getVirtualNetwork(client, virtualNetworkId);
        String content = getSecurityGroupContentFrom(virtualNetwork);
        SecurityGroup securityGroup = findSecurityGroupByName(client, content, securityGroupName);
        return securityGroup;
    }

    private SecurityGroup findSecurityGroupByName(Client client, String content, String name) throws FogbowException {
        String[] securityGroupIds = content.split(CONTENT_SEPARATOR);
        SecurityGroup securityGroup = null;
        for (String securityGroupId : securityGroupIds) {
            securityGroup = OpenNebulaClientUtil.getSecurityGroup(client, securityGroupId);
            if (securityGroup.getName().equals(name)) {
                return securityGroup;
            }
        }
        throw new InstanceNotFoundException(Messages.Exception.INSTANCE_NOT_FOUND);
    }

    private String getSecurityGroupContentFrom(VirtualNetwork virtualNetwork) throws FogbowException {
        String content = virtualNetwork.xpath(SECURITY_GROUPS_PATH);
        if (content == null || content.isEmpty()) {
            String message = Messages.Error.CONTENT_SECURITY_GROUP_NOT_DEFINED;
            throw new UnexpectedException(message);
        }
        return content;
    }

    private String retrieveSecurityGroupName(Order majorOrder) throws FogbowException {
        switch (majorOrder.getType()) {
        case NETWORK:
            return SystemConstants.PN_SECURITY_GROUP_PREFIX + majorOrder.getInstanceId();
        case PUBLIC_IP:
            return SystemConstants.PIP_SECURITY_GROUP_PREFIX + majorOrder.getInstanceId();
        default:
            throw new InvalidParameterException(Messages.Exception.INVALID_RESOURCE);
        }
    }

}
