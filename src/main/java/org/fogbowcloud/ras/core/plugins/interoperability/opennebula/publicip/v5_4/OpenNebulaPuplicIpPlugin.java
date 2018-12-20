package org.fogbowcloud.ras.core.plugins.interoperability.opennebula.publicip.v5_4;

import java.util.ArrayList;
import java.util.List;
import java.util.Properties;

import org.apache.log4j.Logger;
import org.fogbowcloud.ras.core.constants.Messages;
import org.fogbowcloud.ras.core.exceptions.FogbowRasException;
import org.fogbowcloud.ras.core.exceptions.InstanceNotFoundException;
import org.fogbowcloud.ras.core.exceptions.InvalidParameterException;
import org.fogbowcloud.ras.core.exceptions.QuotaExceededException;
import org.fogbowcloud.ras.core.exceptions.UnauthorizedRequestException;
import org.fogbowcloud.ras.core.exceptions.UnexpectedException;
import org.fogbowcloud.ras.core.models.instances.InstanceState;
import org.fogbowcloud.ras.core.models.instances.PublicIpInstance;
import org.fogbowcloud.ras.core.models.orders.PublicIpOrder;
import org.fogbowcloud.ras.core.models.tokens.OpenNebulaToken;
import org.fogbowcloud.ras.core.plugins.interoperability.PublicIpPlugin;
import org.fogbowcloud.ras.core.plugins.interoperability.opennebula.OpenNebulaClientFactory;
import org.fogbowcloud.ras.core.plugins.interoperability.opennebula.OpenNebulaTagNameConstants;
import org.fogbowcloud.ras.core.plugins.interoperability.opennebula.OpenNebulaUnmarshallerContents;
import org.fogbowcloud.ras.core.plugins.interoperability.opennebula.publicip.v5_4.PublicNetworkTemplate.LeaseIp;
import org.fogbowcloud.ras.core.plugins.interoperability.opennebula.securityrule.v5_4.CreateSecurityGroupRequest;
import org.fogbowcloud.ras.core.plugins.interoperability.opennebula.securityrule.v5_4.Rule;
import org.fogbowcloud.ras.util.PropertiesUtil;
import org.opennebula.client.Client;
import org.opennebula.client.OneResponse;
import org.opennebula.client.secgroup.SecurityGroup;
import org.opennebula.client.vm.VirtualMachine;
import org.opennebula.client.vnet.VirtualNetwork;
import org.opennebula.client.vnet.VirtualNetworkPool;

public class OpenNebulaPuplicIpPlugin implements PublicIpPlugin<OpenNebulaToken> {

	private static final Logger LOGGER = Logger.getLogger(OpenNebulaPuplicIpPlugin.class);

	private static final String FIXED_PUBLIC_IP_ADDRESSES_KEY = "fixed_public_ip_addresses";
	private static final String ID_SEPARATOR = " ";
	private static final String IP_SEPARATOR = ",";
	private static final String INSTANCE_ID = "%s %s %s %s";
	private static final String INPUT_RULE_TYPE = "inbound";
	private static final String OUTPUT_RULE_TYPE = "outbound";
	private static final String NIC_IP_PATH = "VM/NIC/IP";
	private static final String ALL_PROTOCOLS = "ALL";
	private static final String NETWORK_TYPE_FIXED = "FIXED";
	private static final String PUBLIC_NETWORK_BRIDGE_KEY = "public_network_bridge";
	private static final String DEFAULT_VIRTUAL_NETWORK_BRIDGED_DRIVE = "fw";
	private static final String XPATH_EXPRESSION_FORMAT = "//VNET_POOL/VNET/TEMPLATE/LEASES[descendant::IP[text()='%s']]";

	private OpenNebulaClientFactory factory;
	private String publicIpAddresses;
	private String bridge;

	public OpenNebulaPuplicIpPlugin(String confFilePath) {
		Properties properties = PropertiesUtil.readProperties(confFilePath);

		this.publicIpAddresses = properties.getProperty(FIXED_PUBLIC_IP_ADDRESSES_KEY);
		this.bridge = properties.getProperty(PUBLIC_NETWORK_BRIDGE_KEY);
		this.factory = new OpenNebulaClientFactory(confFilePath);
	}

	@Override
	public String requestInstance(PublicIpOrder publicIpOrder, String computeInstanceId, OpenNebulaToken localUserAttributes)
			throws FogbowRasException, UnexpectedException {

		LOGGER.info(String.format(Messages.Info.REQUESTING_INSTANCE, localUserAttributes.getTokenValue()));
		
		Client client = this.factory.createClient(localUserAttributes.getTokenValue());

		// check if fixed ip is in use...
		String fixedIp = getAvailableFixedIp(client);
		if (fixedIp == null) {
			LOGGER.error(Messages.Error.FIXED_IP_EXCEEDED);
			throw new QuotaExceededException();
		}
		
		String template;
		template = createPublicNetworkTemplate(publicIpOrder, fixedIp);
		String virtualNetworkId = this.factory.allocateVirtualNetwork(client, template);
		
		template = createSecurityGroupsTemplate(publicIpOrder, virtualNetworkId);
		String securityGroupsId = this.factory.allocateSecurityGroup(client, template);

		template = createNicTemplate(virtualNetworkId, securityGroupsId);
		VirtualMachine virtualMachine = attachNetworkInterfaceConnected(client, computeInstanceId, template);
		String nicId = getNicIdFromContenOf(virtualMachine);

		String instanceId = String.format(INSTANCE_ID, 
				computeInstanceId, 
				virtualNetworkId, 
				securityGroupsId, 
				nicId);
		
		return instanceId;
	}

	private String getAvailableFixedIp(Client client) {
		String[] sliceFixedIp = this.publicIpAddresses.split(IP_SEPARATOR);
		
		OneResponse response = VirtualNetworkPool.infoAll(client);
		if (response.isError()) {
			LOGGER.error(String.format(Messages.Error.ERROR_MESSAGE, response.getErrorMessage()));
		}
		OpenNebulaUnmarshallerContents unmarshallerContents = new OpenNebulaUnmarshallerContents(response.getMessage());
		String content = null;
		String expression = null;
		for (int i = 0; i < sliceFixedIp.length; i++) {
			content = sliceFixedIp[i];
			expression = String.format(XPATH_EXPRESSION_FORMAT, content);
			if (!unmarshallerContents.containsExpressionContext(expression, content)) {
				return content;
			}
		}
		return null;
	}

	@Override
	public void deleteInstance(String publicIpInstanceId, String computeInstanceId, OpenNebulaToken localUserAttributes)
			throws FogbowRasException, UnexpectedException {

		LOGGER.info(String.format(Messages.Info.DELETING_INSTANCE, publicIpInstanceId,
				localUserAttributes.getTokenValue()));
		
		Client client = this.factory.createClient(localUserAttributes.getTokenValue());

		String[] instanceIds = publicIpInstanceId.split(ID_SEPARATOR);
		String virtualNetworkId = instanceIds[1];
		String securityGroupId = instanceIds[2];
		String nicId = instanceIds[3];

		detachNicFromVirtualMachine(client, computeInstanceId, nicId);
		deletePublicNetwork(client, virtualNetworkId);
		deleteSecurityGroup(client, securityGroupId);
	}

	@Override
	public PublicIpInstance getInstance(String publicIpInstanceId, OpenNebulaToken localUserAttributes)
			throws FogbowRasException, UnexpectedException {

		LOGGER.info(
				String.format(Messages.Info.GETTING_INSTANCE, publicIpInstanceId, localUserAttributes.getTokenValue()));
		
		Client client = this.factory.createClient(localUserAttributes.getTokenValue());

		String[] instanceIds = publicIpInstanceId.split(ID_SEPARATOR);
		String virtualMachineId = instanceIds[0];

		VirtualMachine virtualMachine = this.factory.createVirtualMachine(client, virtualMachineId);
		String publicIp = virtualMachine.xpath(NIC_IP_PATH);
		InstanceState instanceState = InstanceState.READY;
		
		LOGGER.info(String.format(Messages.Info.MOUNTING_INSTANCE, publicIpInstanceId));
		
		PublicIpInstance publicIpInstance = new PublicIpInstance(publicIpInstanceId, instanceState, publicIp);
		return publicIpInstance;
	}

	private void deleteSecurityGroup(Client client, String securityGroupId)
			throws UnauthorizedRequestException, InvalidParameterException, InstanceNotFoundException {
		
		SecurityGroup securityGroup = this.factory.createSecurityGroup(client, securityGroupId);
		OneResponse response = securityGroup.delete();
		if (response.isError()) {
			String message = response.getErrorMessage();
			LOGGER.error(String.format(Messages.Error.ERROR_MESSAGE, message));
		}
	}

	private void deletePublicNetwork(Client client, String virtualNetworkId)
			throws UnauthorizedRequestException, InstanceNotFoundException, InvalidParameterException {
		
		VirtualNetwork virtualNetwork = this.factory.createVirtualNetwork(client, virtualNetworkId);
		OneResponse response = virtualNetwork.delete();
		if (response.isError()) {
			String message = response.getErrorMessage();
			LOGGER.error(String.format(Messages.Error.ERROR_MESSAGE, message));
		}
	}
	
	private void detachNicFromVirtualMachine(Client client, String virtualMachineId, String nicId)
			throws UnauthorizedRequestException, InstanceNotFoundException, InvalidParameterException {
		
		int id = Integer.parseInt(nicId);
		VirtualMachine virtualMachine = this.factory.createVirtualMachine(client, virtualMachineId);
		OneResponse response = virtualMachine.nicDetach(id);
		if (response.isError()) {
			String message = response.getErrorMessage();
			LOGGER.error(String.format(Messages.Error.ERROR_MESSAGE, message));
		}
	}
	
	private String getNicIdFromContenOf(VirtualMachine virtualMachine) {
		OneResponse response = virtualMachine.info();
		String xml = response.getMessage();
		OpenNebulaUnmarshallerContents unmarshallerContents = new OpenNebulaUnmarshallerContents(xml);
		String content = unmarshallerContents.getContentOfLastElement(OpenNebulaTagNameConstants.NIC_ID);
		return content;
	}

	private VirtualMachine attachNetworkInterfaceConnected(Client client, String computeInstanceId, String template)
			throws UnauthorizedRequestException, InstanceNotFoundException, InvalidParameterException {
		
		VirtualMachine virtualMachine = this.factory.createVirtualMachine(client, computeInstanceId);
		OneResponse response = virtualMachine.nicAttach(template);
		if (response.isError()) {
			String message = response.getErrorMessage();
			LOGGER.error(String.format(Messages.Error.ERROR_WHILE_CREATING_NIC, template));
			LOGGER.error(String.format(Messages.Error.ERROR_MESSAGE, message));
			throw new InvalidParameterException();
		}
		return virtualMachine;
	}
	
	private String createNicTemplate(String virtualNetworkId, String securityGroupsId) {
		String template;
		CreateNicRequest request = new CreateNicRequest.Builder()
				.networkId(virtualNetworkId)
				.securityGroups(securityGroupsId)
				.build();

		template = request.getNic().marshalTemplate();
		return template;
	}

	private String createPublicNetworkTemplate(PublicIpOrder publicIpOrder, String fixedIp) {
		String name = publicIpOrder.getCloudName();
		String type = NETWORK_TYPE_FIXED;
		String bridge = this.bridge;
		String bridgedDrive = DEFAULT_VIRTUAL_NETWORK_BRIDGED_DRIVE;
		
		List<LeaseIp> leases = new ArrayList<>();
		leases.add(PublicNetworkTemplate.allocateIpAddress(fixedIp));
		
		CreatePublicNetworkRequest request = new CreatePublicNetworkRequest.Builder()
				.name(name)
				.type(type)
				.bridge(bridge)
				.bridgedDrive(bridgedDrive)
				.leases(leases)
				.build();
				
		String template = request.getPublicNetwork().marshalTemplate();
		return template;
	}
	
	private String createSecurityGroupsTemplate(PublicIpOrder publicIpOrder, String virtualNetworkId)
			throws InvalidParameterException {
		
		String name = SECURITY_GROUP_PREFIX + publicIpOrder.getId();

		// "ALL" setting applies to all protocols if a port range is not defined
		String protocol = ALL_PROTOCOLS;

		// An undefined port range is interpreted by opennebula as all open
		String rangeAll = null;

		// An undefined ip and size is interpreted by opennebula as any network
		String ipAny = null;
		String sizeAny = null;

		// The securityGroupId parameters are not used in this context.
		String securityGroupId = null;

		Rule inputRule = new Rule(protocol, ipAny, sizeAny, rangeAll, INPUT_RULE_TYPE, virtualNetworkId,
				securityGroupId);
		Rule outputRule = new Rule(protocol, ipAny, sizeAny, rangeAll, OUTPUT_RULE_TYPE, virtualNetworkId,
				securityGroupId);

		List<Rule> rules = new ArrayList<>();
		rules.add(inputRule);
		rules.add(outputRule);

		CreateSecurityGroupRequest request = new CreateSecurityGroupRequest.Builder().name(name).rules(rules).build();

		String template = request.getSecurityGroup().marshalTemplate();
		return template;
	}

	protected void setFactory(OpenNebulaClientFactory factory) {
		this.factory = factory;
	}
}
