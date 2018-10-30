package org.fogbowcloud.ras.core.plugins.interoperability.opennebula.publicip.v5_4;

import java.util.ArrayList;
import java.util.List;
import java.util.Properties;
import java.util.UUID;

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
import org.fogbowcloud.ras.core.models.tokens.Token;
import org.fogbowcloud.ras.core.plugins.interoperability.PublicIpPlugin;
import org.fogbowcloud.ras.core.plugins.interoperability.opennebula.OpenNebulaClientFactory;
import org.fogbowcloud.ras.util.PropertiesUtil;
import org.opennebula.client.Client;
import org.opennebula.client.OneResponse;
import org.opennebula.client.secgroup.SecurityGroup;
import org.opennebula.client.vm.VirtualMachine;
import org.opennebula.client.vnet.VirtualNetwork;

public class OpenNebulaPuplicIpPlugin implements PublicIpPlugin<Token> {

	private static final Logger LOGGER = Logger.getLogger(OpenNebulaPuplicIpPlugin.class);
	private static final String DEFAULT_NETWORK_ID_KEY = "default_network_id";
	private static final String DEFAULT_ADDRESS_RANGE_IP_KEY = "start_ip_address_range";
	private static final String DEFAULT_ADDRESS_RANGE_TYPE = "IP4";
	private static final String DEFAULT_ADDRESS_RANGE_SIZE_KEY = "default_address_range_size";
	private static final String DEFAULT_SECURITY_GROUPS_ID_KEY = "default_security_groups_id";
	private static final String ID_SEPARATOR = " ";
	private static final String INSTANCE_ID = "%s %s %s";
	private static final String PUBLIC_IP_NAME = "Public_IP";
	private static final String SECURITY_GROUP_PROTOCOL = "ICMP";
	private static final String SECURITY_GROUP_INPUT_RULE_TYPE = "inbound";
	private static final String SECURITY_GROUP_OUTPUT_RULE_TYPE = "outbound";
	private static final String VIRTUAL_MACHINE_NIC_IP_PATH = "VM/NIC/IP";

	private OpenNebulaClientFactory factory;
	private String networkId;
	private String securityGroupsId;
	private String addressRangeIp;
	private String addressRangeSize;

	public OpenNebulaPuplicIpPlugin(OpenNebulaClientFactory factory) {
		Properties properties = PropertiesUtil
				.readProperties(HomeDir.getPath() + DefaultConfigurationConstants.OPENNEBULA_CONF_FILE_NAME);

		this.networkId = properties.getProperty(DEFAULT_NETWORK_ID_KEY);
		this.securityGroupsId = properties.getProperty(DEFAULT_SECURITY_GROUPS_ID_KEY);
		this.addressRangeIp = properties.getProperty(DEFAULT_ADDRESS_RANGE_IP_KEY);
		this.addressRangeSize = properties.getProperty(DEFAULT_ADDRESS_RANGE_SIZE_KEY);
		this.factory = new OpenNebulaClientFactory();
	}

	@Override
	public String requestInstance(PublicIpOrder publicIpOrder, String computeInstanceId, Token localUserAttributes)
			throws FogbowRasException, UnexpectedException {

		LOGGER.info(String.format(Messages.Info.REQUESTING_INSTANCE, localUserAttributes.getTokenValue()));
		Client client = this.factory.createClient(localUserAttributes.getTokenValue());

		int id = Integer.parseInt(this.networkId);
		VirtualNetwork virtualNetwork = new VirtualNetwork(id, client);
		String addressRangeId = addPublicIp(virtualNetwork);
		String securityGroupsId = updateSecurityGroups(client, virtualNetwork);
		String nicId = UUID.randomUUID().toString();

		CreateNicRequest request = new CreateNicRequest.Builder()
				.nicId(nicId)
				.networkId(this.networkId)
				.securityGroups(securityGroupsId)
				.build();
		
		String template = request.getNic().generateTemplate();
		
		VirtualMachine virtualMachine = this.factory.createVirtualMachine(client, computeInstanceId);
		OneResponse response = virtualMachine.nicAttach(template);
		if (response.isError()) {
			String message = response.getErrorMessage();
			LOGGER.error(String.format(Messages.Error.ERROR_MESSAGE, message));
		}
		
		String instanceId = String.format(INSTANCE_ID, virtualMachine.getId(), nicId, addressRangeId);
		return instanceId;
	}

	@Override
	public void deleteInstance(String publicIpInstanceId, String computeInstanceId, Token localUserAttributes)
			throws FogbowRasException, UnexpectedException {

		LOGGER.info(String.format(Messages.Info.DELETING_INSTANCE, publicIpInstanceId,
				localUserAttributes.getTokenValue()));
		Client client = this.factory.createClient(localUserAttributes.getTokenValue());

		String[] instanceIds = publicIpInstanceId.split(ID_SEPARATOR);
		String virtualMachineId = instanceIds[0];
		int nicId = Integer.parseInt(instanceIds[1]);
		int arId = Integer.parseInt(instanceIds[2]);
		
		detachNicFromVirtualMachine(client, virtualMachineId, nicId);
		freeAddressRangeFromVirtualNetwork(client, arId);
	}

	@Override
	public PublicIpInstance getInstance(String publicIpInstanceId, Token localUserAttributes)
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

	private void freeAddressRangeFromVirtualNetwork(Client client, int arId)
			throws UnauthorizedRequestException, InstanceNotFoundException, InvalidParameterException {
		
		VirtualNetwork virtualNetwork = this.factory.createVirtualNetwork(client, this.networkId);
		OneResponse response = virtualNetwork.free(arId);
		if (response.isError()) {
			String message = response.getErrorMessage();
			LOGGER.error(String.format(Messages.Error.ERROR_MESSAGE, message));
		}
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
	
	private String updateSecurityGroups(Client client, VirtualNetwork virtualNetwork) {
		int id = Integer.parseInt(this.securityGroupsId);
		SecurityGroup securityGroup = new SecurityGroup(id, client);
		
		String name = PUBLIC_IP_NAME;
		String protocol = SECURITY_GROUP_PROTOCOL;
		String inputRuleType = SECURITY_GROUP_INPUT_RULE_TYPE;
		String outputRuleType = SECURITY_GROUP_OUTPUT_RULE_TYPE;
		int networkId = 0;
		
		SecurityGroups.Rule defaultInputRule = new SecurityGroups.DefaultRule(protocol, inputRuleType, networkId); 
		SecurityGroups.Rule defaultOutputRule = new SecurityGroups.DefaultRule(protocol, outputRuleType, networkId);
		
		List<SecurityGroups.Rule> rules = new ArrayList<>();
		rules.add(defaultInputRule);
		rules.add(defaultOutputRule);
		
		CreateSecurityGroupsRequest request = new CreateSecurityGroupsRequest.Builder()
				.name(name)
				.rules(rules)
				.build();
		
		String template = request.getSecurityGroups().generateTemplate();
		OneResponse response = securityGroup.update(template);
		if (response.isError()) {
			String message = response.getErrorMessage();
			LOGGER.error(String.format(Messages.Error.ERROR_MESSAGE, message));
		}
		return securityGroup.getId();
	}

	private String addPublicIp(VirtualNetwork virtualNetwork) throws InvalidParameterException {
		String arId = UUID.randomUUID().toString();
		String type = DEFAULT_ADDRESS_RANGE_TYPE;
		String ip = this.addressRangeIp;
		String size = this.addressRangeSize;
		
		CreateAddressRangeRequest request = new CreateAddressRangeRequest.Builder()
				.arId(arId)
				.type(type)
				.ip(ip)
				.size(size)
				.build();
		
		String template = request.getAddressRange().generateTemplate();
		OneResponse response = virtualNetwork.addAr(template);
		if (response.isError()) {
			String message = response.getErrorMessage();
			LOGGER.error(String.format(Messages.Error.ERROR_MESSAGE, message));
			throw new InvalidParameterException(message);
		}
		return arId;
	}

}
