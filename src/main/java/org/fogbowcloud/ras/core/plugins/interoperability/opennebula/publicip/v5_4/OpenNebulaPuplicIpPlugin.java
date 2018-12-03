package org.fogbowcloud.ras.core.plugins.interoperability.opennebula.publicip.v5_4;

import java.util.ArrayList;
import java.util.List;
import java.util.Properties;

import org.apache.log4j.Logger;
import org.fogbowcloud.ras.core.HomeDir;
import org.fogbowcloud.ras.core.constants.DefaultConfigurationConstants;
import org.fogbowcloud.ras.core.constants.Messages;
import org.fogbowcloud.ras.core.exceptions.FogbowRasException;
import org.fogbowcloud.ras.core.exceptions.InstanceNotFoundException;
import org.fogbowcloud.ras.core.exceptions.InvalidParameterException;
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
import org.fogbowcloud.ras.core.plugins.interoperability.opennebula.publicip.v5_4.PublicNetwork.LeaseIp;
import org.fogbowcloud.ras.util.PropertiesUtil;
import org.opennebula.client.Client;
import org.opennebula.client.OneResponse;
import org.opennebula.client.vm.VirtualMachine;

public class OpenNebulaPuplicIpPlugin implements PublicIpPlugin<OpenNebulaToken> {

	private static final Logger LOGGER = Logger.getLogger(OpenNebulaPuplicIpPlugin.class);

	private static final String DEFAULT_PUBLIC_IP_ADDRESS_KEY = "default_public_ip_address";

	private static final String ID_SEPARATOR = " ";
	private static final String IP_SEPARATOR = ",";
	private static final String INSTANCE_ID = "%s %s %s %s";
	private static final String PUBLIC_IP_NAME = "Public_IP";
	private static final String SECURITY_GROUP_INPUT_RULE_TYPE = "inbound";
	private static final String SECURITY_GROUP_OUTPUT_RULE_TYPE = "outbound";
	private static final String VIRTUAL_MACHINE_NIC_IP_PATH = "VM/NIC/IP";
	private static final String SECURITY_GROUP_DEFAULT_PROTOCOL = "TCP";
	private static final String SECURITY_GROUP_DEFAULT_RANGE_VALUE = "1:65536";

	private OpenNebulaClientFactory factory;
	private String publicIpAddress;

	public OpenNebulaPuplicIpPlugin() {
		Properties properties = PropertiesUtil
				.readProperties(HomeDir.getPath() + DefaultConfigurationConstants.OPENNEBULA_CONF_FILE_NAME);

		this.publicIpAddress = properties.getProperty(DEFAULT_PUBLIC_IP_ADDRESS_KEY);
		this.factory = new OpenNebulaClientFactory();
	}

	@Override
	public String requestInstance(PublicIpOrder publicIpOrder, String computeInstanceId, OpenNebulaToken localUserAttributes)
			throws FogbowRasException, UnexpectedException {

		LOGGER.info(String.format(Messages.Info.REQUESTING_INSTANCE, localUserAttributes.getTokenValue()));
		Client client = this.factory.createClient(localUserAttributes.getTokenValue());

		String template;
		template = createPublicNetworkTemplate();
		String virtualNetworkId = this.factory.allocateVirtualNetwork(client, template);
		
		template = createSecurityGroupsTemplate(virtualNetworkId);
		System.out.println(template);
		String securityGroupsId = this.factory.allocateSecurityGroup(client, template);

		template = createNicTemplate(virtualNetworkId, securityGroupsId);
		VirtualMachine virtualMachine = attachNetworkInterfaceConnected(client, computeInstanceId, template);
		String nicId = getNicIdFromContenOf(virtualMachine);

		String instanceId = String.format(INSTANCE_ID, computeInstanceId, virtualNetworkId, securityGroupsId, nicId);
		return instanceId;
	}

	@Override
	public void deleteInstance(String publicIpInstanceId, String computeInstanceId, OpenNebulaToken localUserAttributes)
			throws FogbowRasException, UnexpectedException {

		LOGGER.info(String.format(Messages.Info.DELETING_INSTANCE, publicIpInstanceId,
				localUserAttributes.getTokenValue()));
		Client client = this.factory.createClient(localUserAttributes.getTokenValue());

		String[] instanceIds = publicIpInstanceId.split(ID_SEPARATOR);
		int nicId = Integer.parseInt(instanceIds[2]);

		detachNicFromVirtualMachine(client, computeInstanceId, nicId);
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
		String publicIp = virtualMachine.xpath(VIRTUAL_MACHINE_NIC_IP_PATH);
		InstanceState instanceState = InstanceState.READY;
		
		LOGGER.info(String.format(Messages.Info.MOUNTING_INSTANCE, publicIpInstanceId));
		PublicIpInstance publicIpInstance = new PublicIpInstance(publicIpInstanceId, instanceState, publicIp);
		return publicIpInstance;
	}

	private void detachNicFromVirtualMachine(Client client, String virtualMachineId, int nicId)
			throws UnauthorizedRequestException, InstanceNotFoundException, InvalidParameterException {
		
		VirtualMachine virtualMachine = this.factory.createVirtualMachine(client, virtualMachineId);
		OneResponse response = virtualMachine.nicDetach(nicId);
		if (response.isError()) {
			String message = response.getErrorMessage();
			LOGGER.error(String.format(Messages.Error.ERROR_MESSAGE, message));
		}
	}
	
	private String getNicIdFromContenOf(VirtualMachine virtualMachine) {
		OneResponse response = virtualMachine.info();
		String xml = response.getMessage();
		OpenNebulaUnmarshallerContents unmarshallerContents = new OpenNebulaUnmarshallerContents(xml);
		String content = unmarshallerContents.unmarshalLastItemOf(OpenNebulaTagNameConstants.NIC_ID);
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

	private String createPublicNetworkTemplate() {
		String name = "public-network";
		String type = "FIXED";
		String bridge = "vbr1";
		String bridgedDrive = "fw";
		String ipAddress = this.publicIpAddress;
		
		List<LeaseIp> leases = new ArrayList<>();
		String[] address = ipAddress.split(IP_SEPARATOR);
		for (int i = 0; i < address.length; i++) {
			leases.add(PublicNetwork.allocateIpAddress(address[i].trim()));
		}
		
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
	
	private String createSecurityGroupsTemplate(String virtualNetworkId) throws InvalidParameterException {
		String name = PUBLIC_IP_NAME;
		String protocol = SECURITY_GROUP_DEFAULT_PROTOCOL;
		String inputRuleType = SECURITY_GROUP_INPUT_RULE_TYPE;
		String outputRuleType = SECURITY_GROUP_OUTPUT_RULE_TYPE;
		String portRange = SECURITY_GROUP_DEFAULT_RANGE_VALUE;
		int networkId = Integer.parseInt(virtualNetworkId);

		SecurityGroups.Rule defaultInputRule = SecurityGroups.allocateSafetyRule(
				protocol, 
				inputRuleType, 
				portRange,
				networkId);
		
		SecurityGroups.Rule defaultOutputRule = SecurityGroups.allocateSafetyRule(
				protocol, 
				outputRuleType, 
				portRange,
				networkId);

		List<SecurityGroups.Rule> rules = new ArrayList<>();
		rules.add(defaultInputRule);
		rules.add(defaultOutputRule);

		CreateSecurityGroupsRequest request = new CreateSecurityGroupsRequest.Builder()
				.name(name)
				.rules(rules)
				.build();

		String template = request.getSecurityGroups().marshalTemplate();
		return template;
	}

	protected void setFactory(OpenNebulaClientFactory factory) {
		this.factory = factory;
	}
}
