package org.fogbowcloud.ras.core.plugins.interoperability.opennebula.securityrule.v5_4;

import org.apache.log4j.Logger;
import org.fogbowcloud.ras.core.constants.Messages;
import org.fogbowcloud.ras.core.exceptions.FogbowRasException;
import org.fogbowcloud.ras.core.exceptions.InvalidParameterException;
import org.fogbowcloud.ras.core.exceptions.UnexpectedException;
import org.fogbowcloud.ras.core.models.orders.Order;
import org.fogbowcloud.ras.core.models.securityrules.Direction;
import org.fogbowcloud.ras.core.models.securityrules.EtherType;
import org.fogbowcloud.ras.core.models.securityrules.Protocol;
import org.fogbowcloud.ras.core.models.securityrules.SecurityRule;
import org.fogbowcloud.ras.core.models.tokens.OpenNebulaToken;
import org.fogbowcloud.ras.core.plugins.interoperability.SecurityRulePlugin;
import org.fogbowcloud.ras.core.plugins.interoperability.opennebula.OpenNebulaClientFactory;
import org.fogbowcloud.ras.core.plugins.interoperability.opennebula.securityrule.v5_4.SecurityGroupInfo.Template;
import org.opennebula.client.Client;
import org.opennebula.client.OneResponse;
import org.opennebula.client.secgroup.SecurityGroup;
import org.opennebula.client.vnet.VirtualNetwork;

import java.util.ArrayList;
import java.util.List;

// TODO use the class Message to its messages
public class OpenNebulaSecurityRulePlugin implements SecurityRulePlugin<OpenNebulaToken> {

	public static final Logger LOGGER = Logger.getLogger(OpenNebulaSecurityRulePlugin.class);
	
	private static final int SLICE_POSITION_SECURITY_GROUP = 1;
    protected static final String OPENNEBULA_XML_ARRAY_SEPARATOR = ",";
	protected static final String TEMPLATE_VNET_SECURITY_GROUPS_PATH = "/VNET/TEMPLATE/SECURITY_GROUPS";
    private static final String OPENNEBULA_XML_ARRAY_SEPARETOR = ",";
	private static final String INBOUND_TEMPLATE_VALUE = "inbound";
	private static final String OUTBOUND_TEMPLATE_VALUE = "outbound";
	private static final String CIDR_SLICE = "[/]";
	private static final String RANGE_PORT_SEPARATOR = ":";

	private static final int BASE_VALUE = 2;
	private static final int IPV4_AMOUNT_BITS = 32;
	
    private OpenNebulaClientFactory factory;
    
    public OpenNebulaSecurityRulePlugin() {
        this.factory = new OpenNebulaClientFactory();
    }

    @Override
    public String requestSecurityRule(SecurityRule securityRule, Order majorOrder, OpenNebulaToken localUserAttributes)
            throws FogbowRasException, UnexpectedException {
        
    	Client client = this.factory.createClient(localUserAttributes.getTokenValue());
    	
    	String virtualNetworkId = majorOrder.getInstanceId();
    	VirtualNetwork virtualNetwork = this.factory.createVirtualNetwork(client, virtualNetworkId);

		String securityGroupId = getSecurityGroupBy(virtualNetwork);		
		SecurityGroup securityGroup = this.factory.getSecurityGroup(client, securityGroupId);
		
		String securityGroupXml = securityGroup.info().getMessage();
		SecurityGroupInfo securityGroupInfo = SecurityGroupInfo.unmarshal(securityGroupXml);
		
		Rule rule = createRuleBy(securityRule);
		rule.setSecurityGroupId(securityGroupId);
		
		String template = createSecurityGroupsTemplate(securityGroupInfo, rule);
		OneResponse response = securityGroup.update(template);
		if (response.isError()) {
			String message = response.getErrorMessage();
			LOGGER.error(String.format(Messages.Error.ERROR_WHILE_UPDATING_SECURITY_GROUPS, template));
			LOGGER.error(String.format(Messages.Error.ERROR_MESSAGE, message));
			throw new InvalidParameterException();
		}
		String instanceId = rule.serialize();
		return instanceId;
    }

	private Rule createRuleBy(SecurityRule securityRule) {
		String protocol = securityRule.getProtocol().toString();
		String slice[] = securityRule.getCidr().split(CIDR_SLICE);
		String ip = slice[0];
		int value = Integer.parseInt(slice[1]);
		int size = (int) Math.pow(BASE_VALUE, IPV4_AMOUNT_BITS - value);
		String range = securityRule.getPortFrom() + RANGE_PORT_SEPARATOR + securityRule.getPortTo();
		String type = mapRuleType(securityRule.getDirection());

		Rule rule = new Rule();
		rule.setProtocol(protocol);
		rule.setIp(ip);
		rule.setSize(size);
		rule.setRange(range);
		rule.setType(type);
		return rule;
	}

	private String createSecurityGroupsTemplate(SecurityGroupInfo securityGroupInfo, Rule rule) {
		String id = securityGroupInfo.getId();
		String name = securityGroupInfo.getName();
		List<Rule> rules = securityGroupInfo.getTemplate().getRules();
		rules.add(rule);

		CreateSecurityGroupRequest request = new CreateSecurityGroupRequest.Builder()
				.id(id)
				.name(name)
				.rules(rules)
				.build();

		String template = request.getSecurityGroup().marshalTemplate();
		return template;
	}

	private String mapRuleType(Direction direction) {
		String type = null;
		switch (direction) {
		case IN:
			type = INBOUND_TEMPLATE_VALUE;
			break;
		case OUT:
			type = OUTBOUND_TEMPLATE_VALUE;
			break;
		default:
			LOGGER.warn(String.format(Messages.Warn.INCONSISTENT_DIRECTION, direction));
			break;
		}
		return type;
	}

	@Override
    public List<SecurityRule> getSecurityRules(Order majorOrder, OpenNebulaToken localUserAttributes)
            throws FogbowRasException, UnexpectedException {    	
    	Client client = this.factory.createClient(localUserAttributes.getTokenValue());
    	
    	// Both NetworkOrder's instanceId and PublicIdOrder's instanceId are network ids in the opennebula context 
    	String virtualNetworkId = majorOrder.getInstanceId();
    	VirtualNetwork virtualNetwork = this.factory.createVirtualNetwork(client, virtualNetworkId);

		String securityGroupId = getSecurityGroupBy(virtualNetwork);		
		SecurityGroup securityGroup = this.factory.getSecurityGroup(client, securityGroupId);

		return getSecurityRules(securityGroup);
    }

	protected List<SecurityRule> getSecurityRules(SecurityGroup securityGroup) throws FogbowRasException {
		List<SecurityRule> securityRules = new ArrayList<SecurityRule>();
		try {
			List<Rule> rules = getRules(securityGroup);

			for (Rule rule : rules) {
				Direction direction = rule.getDirection();
				int portFrom = rule.getPortFrom();
				int portTo = rule.getPortTo();
				String cidr = rule.getCIDR();
				EtherType etherType = rule.getEtherType();
				Protocol protocol = rule.getSRProtocol();

				securityRules.add(new SecurityRule(direction, portFrom, portTo, cidr, etherType, protocol));
			}
		} catch (Exception e) {
			throw new FogbowRasException(e.getMessage(), e);
		}
		return securityRules;
	}

	protected List<Rule> getRules(SecurityGroup securityGroup) {
		String securityGroupXml = securityGroup.info().getMessage();
		SecurityGroupInfo securityGroupInfo = SecurityGroupInfo.unmarshal(securityGroupXml);
		Template template = securityGroupInfo.getTemplate();
		return template.getRules();
	}

	/**
     * Note: In the Fogbow context, everytime the Virtual Network(VN) will have 
     * two security groups; The first is the default contained in every VN and
     * the second is the security group used by Fogbow because is created by own
     * 
     * @param  virtualNetwork : Opennebula client object regarding to Virtual Network
     * @return the correct security group associated with the Virtual Network
     */
	protected String getSecurityGroupBy(VirtualNetwork virtualNetwork) {
		String securityGroupXMLContent = virtualNetwork.xpath(TEMPLATE_VNET_SECURITY_GROUPS_PATH);
		if (securityGroupXMLContent == null || securityGroupXMLContent.isEmpty()) {
			LOGGER.warn("Security Groups int the XML Template of the VirtualNetwork is null");
			return null;
		}
		String[] securityGroupXMLContentSlices = securityGroupXMLContent.split(OPENNEBULA_XML_ARRAY_SEPARATOR);
		if (securityGroupXMLContentSlices.length < 2) {
			LOGGER.warn("Security Groups int the XML Template of the VirtualNetwork is with wrong format");
			return null;
		}
		return securityGroupXMLContentSlices[SLICE_POSITION_SECURITY_GROUP];
	}

	// TODO implementing the structure only
    @Override
    public void deleteSecurityRule(String securityRuleId, OpenNebulaToken localUserAttributes)
            throws FogbowRasException, UnexpectedException {
        LOGGER.info(String.format(Messages.Info.DELETING_INSTANCE, securityRuleId, localUserAttributes.getTokenValue()));
        Client client = this.factory.createClient(localUserAttributes.getTokenValue());

		String virtualNetworkId = "";
		VirtualNetwork virtualNetwork = this.factory.createVirtualNetwork(client, virtualNetworkId);

		String securityGroupId = getSecurityGroupBy(virtualNetwork);
		SecurityGroup securityGroup = this.factory.getSecurityGroup(client, securityGroupId);
		String securityGroupXml = securityGroup.info().getMessage();
		SecurityGroupInfo securityGroupInfo = SecurityGroupInfo.unmarshal(securityGroupXml);
		List<Rule> rules = securityGroupInfo.getTemplate().getRules();
		// TODO use the rule
		Object ruleToRemove = new Object();
		for (Rule rule: new ArrayList<>(rules)) {
			if (rule.equals(ruleToRemove)) {
				rules.remove(ruleToRemove);
			}
		}

		// TODO implement the marshall methods
		// to xml
		String xml = "";
		OneResponse response = securityGroup.update(xml);
		if (response.isError()) {
			LOGGER.error(String.format(Messages.Error.ERROR_WHILE_REMOVING_SECURITY_RULE, securityGroupId, response.getMessage()));
		}
    }

	protected void setFactory(OpenNebulaClientFactory factory) {
		this.factory = factory;
	}
}
