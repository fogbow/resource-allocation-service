package org.fogbowcloud.ras.core.plugins.interoperability.opennebula.securityrule.v5_4;

import java.util.ArrayList;
import java.util.List;

import org.apache.log4j.Logger;
import org.fogbowcloud.ras.core.constants.Messages;
import org.fogbowcloud.ras.core.exceptions.FogbowRasException;
import org.fogbowcloud.ras.core.exceptions.InvalidParameterException;
import org.fogbowcloud.ras.core.exceptions.UnexpectedException;
import org.fogbowcloud.ras.core.models.orders.Order;
import org.fogbowcloud.ras.core.models.securityrules.Direction;
import org.fogbowcloud.ras.core.models.securityrules.SecurityRule;
import org.fogbowcloud.ras.core.models.tokens.OpenNebulaToken;
import org.fogbowcloud.ras.core.plugins.interoperability.SecurityRulePlugin;
import org.fogbowcloud.ras.core.plugins.interoperability.opennebula.OpenNebulaClientFactory;
import org.fogbowcloud.ras.core.plugins.interoperability.opennebula.publicip.v5_4.CreateSecurityGroupsRequest;
import org.fogbowcloud.ras.core.plugins.interoperability.opennebula.publicip.v5_4.SecurityGroups;
import org.opennebula.client.Client;
import org.opennebula.client.secgroup.SecurityGroup;
import org.opennebula.client.vnet.VirtualNetwork;

public class OpenNebulaSecurityRulePlugin implements SecurityRulePlugin<OpenNebulaToken> {

	public static final Logger LOGGER = Logger.getLogger(OpenNebulaSecurityRulePlugin.class);
	
	private static final int SLICE_POSITION_SECURITY_GROUP = 1;
    private static final String OPENNEBULA_XML_ARRAY_SEPARETOR = ",";
    
	private static final String TEMPLATE_VNET_SECURITY_GROUPS_PATH = "/VNET/TEMPLATE/SECURITY_GROUPS";

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
		
		String template = createSecurityGroupsTemplate(securityGroupInfo, securityRule);
		
		// TODO finish this implementaion.
    	
    	throw new UnsupportedOperationException();
    }

	private String createSecurityGroupsTemplate(SecurityGroupInfo securityGroupInfo, SecurityRule securityRule) {

		String name = securityGroupInfo.getName();
		List<Rule> rules = securityGroupInfo.getTemplate().getRules();
		
		String protocol = securityRule.getProtocol().toString();
		String cidrSlice[] = securityRule.getCidr().split("[/]");
		String ip = cidrSlice[0];
		int size = cidrSlice[1].equals("24") ? 256 : 0; // FIXME!
		String range = securityRule.getPortFrom() + ":" + securityRule.getPortTo();
		String type = mapRuleType(securityRule.getDirection());

		Rule rule = new Rule();
		rule.setProtocol(protocol);
		rule.setIp(ip);
		rule.setSize(size);
		rule.setRange(range);
		rule.setType(type);
		
		rules.add(rule);
		
		CreateSecurityGroupRequest request = new CreateSecurityGroupRequest.Builder()
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
			type = "inbound";
			break;
		case OUT:
			type = "outbound";
			break;
		default:
			// TODO Fix error message...
			break;
		}
		return type;
	}

	@Override
    public List<SecurityRule> getSecurityRules(Order majorOrder, OpenNebulaToken localUserAttributes)
            throws FogbowRasException, UnexpectedException {    	
    	Client client = this.factory.createClient(localUserAttributes.getTokenValue());
    	
    	// Both NetworkOrder's instanceId and PublicIdOrder's instanceId are network id in the opennebula context 
    	String virtualNetworkId = majorOrder.getInstanceId();
    	VirtualNetwork virtualNetwork = this.factory.createVirtualNetwork(client, virtualNetworkId);

		String securityGroupId = getSecurityGroupBy(virtualNetwork);		
		SecurityGroup securityGroup = this.factory.getSecurityGroup(client, securityGroupId);
		String securityGroupXml = securityGroup.info().getMessage();
		// TODO unmarshal. Waiting for others commits with rule's object and methods
		List<Object> rules = new ArrayList<Object>();
		
    	return convertToSecurityRules(rules);
    }

    // TODO finish this implementaion. Waiting for others commits with rule's object and methods 
    private List<SecurityRule> convertToSecurityRules(List<Object> rules) {
    	List<SecurityRule> securityRules = new ArrayList<SecurityRule>();
    	for (Object rule : rules) {
			securityRules.add(new SecurityRule());
		}
		return securityRules;
	}
    
	/**
     * Note: In the Fogbow context, everytime the Virtual Network(VN) will have 
     * two security groups; The first is the default contained in every VN and
     * the second is the security group used by Fogbow because is created by own
     * 
     * @param  Opennebula client object regarding to Virtual Network 
     * @return the correct security group associated with the Virtual Network 
     * 
     */
	protected String getSecurityGroupBy(VirtualNetwork virtualNetwork) {
		String securityGroupXMLContent = virtualNetwork.xpath(TEMPLATE_VNET_SECURITY_GROUPS_PATH);
		if (securityGroupXMLContent == null || securityGroupXMLContent.isEmpty()) {
			return null;
		}
		String[] securityGroupXMLContentSlices = securityGroupXMLContent.split(OPENNEBULA_XML_ARRAY_SEPARETOR);
		if (securityGroupXMLContentSlices.length < 2) {
			return null;
		}
		String securityGroupId = securityGroupXMLContentSlices[SLICE_POSITION_SECURITY_GROUP];
		return securityGroupId;
	}

	// TODO fix this implementation. This one is deleting the whole security group and 
	// this is not according to specification
    @Override
    public void deleteSecurityRule(String securityRuleId, OpenNebulaToken localUserAttributes)
            throws FogbowRasException, UnexpectedException {
        LOGGER.info(
                String.format(Messages.Info.DELETING_INSTANCE, securityRuleId, localUserAttributes.getTokenValue()));
        Client client = this.factory.createClient(localUserAttributes.getTokenValue());
        int id;
        try {
            id = Integer.parseInt(securityRuleId);
        } catch (Exception e) {
            LOGGER.error(String.format(Messages.Error.ERROR_WHILE_CONVERTING_INSTANCE_ID, securityRuleId));
            throw new InvalidParameterException(Messages.Exception.INVALID_PARAMETER);
        }
        SecurityGroup.delete(client, id);
    }
   
}
