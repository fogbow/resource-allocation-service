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
import org.fogbowcloud.ras.util.PropertiesUtil;
import org.opennebula.client.Client;
import org.opennebula.client.OneResponse;
import org.opennebula.client.vm.VirtualMachine;
import org.opennebula.client.vnet.VirtualNetwork;

public class OpenNebulaPuplicIpPlugin implements PublicIpPlugin<OpenNebulaToken> {

	private static final Logger LOGGER = Logger.getLogger(OpenNebulaPuplicIpPlugin.class);
	private static final String DEFAULT_NETWORK_ID_KEY = "default_network_id";
	private static final String DEFAULT_ADDRESS_RANGE_IP_KEY = "default_start_public_ip_address_range";
	private static final String DEFAULT_ADDRESS_RANGE_TYPE = "IP4";
	private static final String DEFAULT_ADDRESS_RANGE_SIZE_KEY = "default_public_ip_address_range_size";
	private static final String ID_SEPARATOR = " ";
	private static final String INSTANCE_ID = "%s %s %s %s";
	private static final String PUBLIC_IP_NAME = "Public_IP";
	private static final String SECURITY_GROUP_INPUT_RULE_TYPE = "inbound";
	private static final String SECURITY_GROUP_OUTPUT_RULE_TYPE = "outbound";
	private static final String VIRTUAL_MACHINE_NIC_IP_PATH = "VM/NIC/IP";
	private static final String SECURITY_GROUP_DEFAULT_PROTOCOL = "TCP";
	private static final String SECURITY_GROUP_DEFAULT_RANGE_VALUE = "1000:2000";

	private OpenNebulaClientFactory factory;
	private String networkId;
	private String addressRangeIp;
	private String addressRangeSize;

	public OpenNebulaPuplicIpPlugin() {
		Properties properties = PropertiesUtil
				.readProperties(HomeDir.getPath() + DefaultConfigurationConstants.OPENNEBULA_CONF_FILE_NAME);

		this.networkId = properties.getProperty(DEFAULT_NETWORK_ID_KEY);
		this.addressRangeIp = properties.getProperty(DEFAULT_ADDRESS_RANGE_IP_KEY);
		this.addressRangeSize = properties.getProperty(DEFAULT_ADDRESS_RANGE_SIZE_KEY);
		this.factory = new OpenNebulaClientFactory();
	}

	@Override
	public String requestInstance(PublicIpOrder publicIpOrder, String computeInstanceId, OpenNebulaToken localUserAttributes)
			throws FogbowRasException, UnexpectedException {

		LOGGER.info(String.format(Messages.Info.REQUESTING_INSTANCE, localUserAttributes.getTokenValue()));
		Client client = this.factory.createClient(localUserAttributes.getTokenValue());

		String template;
		template = createSecurityGroupsTemplate();
		String securityGroupsId = this.factory.allocateSecurityGroup(client, template);
		
		template = createAddressRangeTemplate();
		VirtualNetwork virtualNetwork = addAddressRange(client, template);
		String addressRangeId = getAddressRangeIdFromContentOf(virtualNetwork);

		template = createNicTemplate(securityGroupsId);
		String nicId = attachNicToVirtualMachine(client, computeInstanceId, template);

		String instanceId = String.format(INSTANCE_ID, computeInstanceId, nicId, addressRangeId, securityGroupsId);
		return instanceId;
	}

	@Override
	public void deleteInstance(String publicIpInstanceId, String computeInstanceId, OpenNebulaToken localUserAttributes)
			throws FogbowRasException, UnexpectedException {

		LOGGER.info(String.format(Messages.Info.DELETING_INSTANCE, publicIpInstanceId,
				localUserAttributes.getTokenValue()));
		Client client = this.factory.createClient(localUserAttributes.getTokenValue());

		String[] instanceIds = publicIpInstanceId.split(ID_SEPARATOR);
		int nicId = Integer.parseInt(instanceIds[1]);
		int addressRangeId = Integer.parseInt(instanceIds[2]);

		detachNicFromVirtualMachine(client, computeInstanceId, nicId);
		freeAddressRangeFromVirtualNetwork(client, addressRangeId);
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

	protected void freeAddressRangeFromVirtualNetwork(Client client, int arId)
			throws UnauthorizedRequestException, InstanceNotFoundException, InvalidParameterException {
		
		VirtualNetwork virtualNetwork = this.factory.createVirtualNetwork(client, this.networkId);
		OneResponse response = virtualNetwork.free(arId);
		if (response.isError()) {
			String message = response.getErrorMessage();
			LOGGER.error(String.format(Messages.Error.ERROR_MESSAGE, message));
		}
	}

	protected void detachNicFromVirtualMachine(Client client, String virtualMachineId, int nicId)
			throws UnauthorizedRequestException, InstanceNotFoundException, InvalidParameterException {
		
		VirtualMachine virtualMachine = this.factory.createVirtualMachine(client, virtualMachineId);
		OneResponse response = virtualMachine.nicDetach(nicId);
		if (response.isError()) {
			String message = response.getErrorMessage();
			LOGGER.error(String.format(Messages.Error.ERROR_MESSAGE, message));
		}
	}
	
	protected String attachNicToVirtualMachine(Client client, String computeInstanceId, String template)
			throws InvalidParameterException, UnauthorizedRequestException, InstanceNotFoundException {
		
		VirtualMachine virtualMachine = this.factory.createVirtualMachine(client, computeInstanceId);
		OneResponse response = virtualMachine.nicAttach(template);
		if (response.isError()) {
			String message = response.getErrorMessage();
			LOGGER.error(String.format(Messages.Error.ERROR_WHILE_CREATING_NIC, template));
			LOGGER.error(String.format(Messages.Error.ERROR_MESSAGE, message));
			throw new InvalidParameterException();
		}
		VirtualMachine.chmod(client, response.getIntMessage(), 744);
		String nicId = getContentFromNicId(virtualMachine);
		return nicId;
	}

	protected String getContentFromNicId(VirtualMachine virtualMachine) {
		OneResponse response = virtualMachine.info();
		String xml = response.getMessage();
		OpenNebulaUnmarshallerContents unmarshallerContents = new OpenNebulaUnmarshallerContents(xml);
		String content = unmarshallerContents.unmarshalLastItemOf(OpenNebulaTagNameConstants.NIC_ID);
		return content;
	}
	
	protected String createNicTemplate(String securityGroupsId) {
		String template;
		CreateNicRequest request = new CreateNicRequest.Builder()
				.networkId(this.networkId)
				.securityGroups(securityGroupsId)
				.build();

		template = request.getNic().marshalTemplate();
		return template;
	}

	protected String getAddressRangeIdFromContentOf(VirtualNetwork virtualNetwork) {
		OneResponse response = virtualNetwork.info();
		String xml = response.getMessage();
		OpenNebulaUnmarshallerContents unmarshallerContents = new OpenNebulaUnmarshallerContents(xml);
		String content = unmarshallerContents.unmarshalLastItemOf(OpenNebulaTagNameConstants.AR_ID);
		return content;
	}

	protected VirtualNetwork addAddressRange(Client client, String template) throws UnauthorizedRequestException, InstanceNotFoundException, InvalidParameterException {
		VirtualNetwork virtualNetwork = this.factory.createVirtualNetwork(client, this.networkId);
		OneResponse response = virtualNetwork.addAr(template);
		if (response.isError()) {
			String message = response.getErrorMessage();
			LOGGER.error(String.format(Messages.Error.ERROR_MESSAGE, message));
			LOGGER.error(String.format(Messages.Error.ERROR_WHILE_CREATING_AR, template));
			throw new InvalidParameterException(message);
		}
		return virtualNetwork;
	}
	
	protected String createAddressRangeTemplate()
			throws InvalidParameterException, UnauthorizedRequestException, InstanceNotFoundException {
		
		String type = DEFAULT_ADDRESS_RANGE_TYPE;
		String ip = this.addressRangeIp;
		String size = this.addressRangeSize;

		CreateAddressRangeRequest request = new CreateAddressRangeRequest.Builder()
				.type(type)
				.ip(ip)
				.size(size)
				.build();

		String template = request.getAddressRange().marshalTemplate();
		return template;
	}
	
	protected String createSecurityGroupsTemplate() throws InvalidParameterException {
		String name = PUBLIC_IP_NAME;
		String protocol = SECURITY_GROUP_DEFAULT_PROTOCOL;
		String inputRuleType = SECURITY_GROUP_INPUT_RULE_TYPE;
		String outputRuleType = SECURITY_GROUP_OUTPUT_RULE_TYPE;
		String portRange = SECURITY_GROUP_DEFAULT_RANGE_VALUE;
		int networkId = Integer.parseInt(this.networkId);

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
