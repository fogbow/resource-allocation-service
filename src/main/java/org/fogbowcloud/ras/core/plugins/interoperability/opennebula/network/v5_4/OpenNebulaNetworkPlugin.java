package org.fogbowcloud.ras.core.plugins.interoperability.opennebula.network.v5_4;

import java.util.ArrayList;
import java.util.List;
import java.util.Properties;

import org.apache.log4j.Logger;
import org.fogbowcloud.ras.core.constants.Messages;
import org.fogbowcloud.ras.core.exceptions.FogbowRasException;
import org.fogbowcloud.ras.core.exceptions.InvalidParameterException;
import org.fogbowcloud.ras.core.exceptions.UnexpectedException;
import org.fogbowcloud.ras.core.models.ResourceType;
import org.fogbowcloud.ras.core.models.instances.InstanceState;
import org.fogbowcloud.ras.core.models.instances.NetworkInstance;
import org.fogbowcloud.ras.core.models.orders.NetworkAllocationMode;
import org.fogbowcloud.ras.core.models.orders.NetworkOrder;
import org.fogbowcloud.ras.core.models.tokens.OpenNebulaToken;
import org.fogbowcloud.ras.core.plugins.interoperability.NetworkPlugin;
import org.fogbowcloud.ras.core.plugins.interoperability.opennebula.OpenNebulaClientFactory;
import org.fogbowcloud.ras.core.plugins.interoperability.opennebula.OpenNebulaStateMapper;
import org.fogbowcloud.ras.core.plugins.interoperability.opennebula.securityrule.v5_4.CreateSecurityGroupRequest;
import org.fogbowcloud.ras.core.plugins.interoperability.opennebula.securityrule.v5_4.Rule;
import org.fogbowcloud.ras.util.PropertiesUtil;
import org.opennebula.client.Client;
import org.opennebula.client.OneResponse;
import org.opennebula.client.vnet.VirtualNetwork;

public class OpenNebulaNetworkPlugin implements NetworkPlugin<OpenNebulaToken> {

	private static final Logger LOGGER = Logger.getLogger(OpenNebulaNetworkPlugin.class);

	private static final String DEFAULT_NETWORK_BRIDGE_KEY = "default_network_bridge";
	private static final String DEFAULT_NETWORK_DESCRIPTION = "Virtual network created by %s";
	private static final String DEFAULT_NETWORK_TYPE = "RANGED";
	private static final String DEFAULT_VIRTUAL_NETWORK_BRIDGED_DRIVE = "fw";
	private static final String NETWORK_ADDRESS_RANGE_TYPE = "IP4";
	private static final String TEMPLATE_NETWORK_ADDRESS_PATH = "TEMPLATE/NETWORK_ADDRESS";
	private static final String TEMPLATE_NETWORK_GATEWAY_PATH = "TEMPLATE/NETWORK_GATEWAY";
	private static final String TEMPLATE_VLAN_ID_PATH = "TEMPLATE/VLAN_ID";
	private static final String CIDR_SEPARATOR = "[/]";
	private static final String ALL_PROTOCOLS = "ALL";
	private static final String INPUT_RULE_TYPE = "inbound";
	private static final String OUTPUT_RULE_TYPE = "outbound";
	
	private OpenNebulaClientFactory factory;

	private String bridge;
	
	public OpenNebulaNetworkPlugin(String confFilePath) {
		Properties properties = PropertiesUtil.readProperties(confFilePath);
		this.bridge = properties.getProperty(DEFAULT_NETWORK_BRIDGE_KEY);
		this.factory = new OpenNebulaClientFactory(confFilePath);
	}

	@Override
	public String requestInstance(NetworkOrder networkOrder, OpenNebulaToken localUserAttributes)
			throws FogbowRasException, UnexpectedException {
		
		LOGGER.info(String.format(Messages.Info.REQUESTING_INSTANCE, localUserAttributes.getUserName()));
		Client client = this.factory.createClient(localUserAttributes.getTokenValue());
		
		String name = networkOrder.getName();
		String description = String.format(DEFAULT_NETWORK_DESCRIPTION, localUserAttributes.getUserName());
		String type = DEFAULT_NETWORK_TYPE;
		String bridge = this.bridge;
		String bridgedDrive = DEFAULT_VIRTUAL_NETWORK_BRIDGED_DRIVE;
		String address = networkOrder.getCidr();
		String gateway = networkOrder.getGateway();
		String securityGroupId = createSecurityGroup(client, networkOrder);
		
		String[] slice = sliceCIDR(networkOrder.getCidr());
		String rangeIp = slice[0];
		String rangeSize = slice[1];
		String rangeType = NETWORK_ADDRESS_RANGE_TYPE;
		
		
		CreateNetworkRequest request = new CreateNetworkRequest.Builder()
				.name(name)
				.description(description)
				.type(type)
				.bridge(bridge)
				.bridgedDrive(bridgedDrive)
				.address(address)
				.gateway(gateway)
				.securityGroupId(securityGroupId)
				.rangeType(rangeType)
				.rangeIp(rangeIp)
				.rangeSize(rangeSize)
				.build();
		
		String template = request.getVirtualNetwork().marshalTemplate();
		return this.factory.allocateVirtualNetwork(client, template);
	}

	@Override
	public NetworkInstance getInstance(String networkInstanceId, OpenNebulaToken localUserAttributes)
			throws FogbowRasException, UnexpectedException {

		LOGGER.info(
				String.format(Messages.Info.GETTING_INSTANCE, networkInstanceId, localUserAttributes.getUserName()));
		
		Client client = this.factory.createClient(localUserAttributes.getTokenValue());
		VirtualNetwork virtualNetwork = this.factory.createVirtualNetwork(client, networkInstanceId);
		return createInstance(virtualNetwork);
	}

	@Override
	public void deleteInstance(String networkInstanceId, OpenNebulaToken localUserAttributes)
			throws FogbowRasException, UnexpectedException {

		LOGGER.info(
				String.format(Messages.Info.DELETING_INSTANCE, networkInstanceId, localUserAttributes.getUserName()));
		
		Client client = this.factory.createClient(localUserAttributes.getTokenValue());
		VirtualNetwork virtualNetwork = this.factory.createVirtualNetwork(client, networkInstanceId);
		OneResponse response = virtualNetwork.delete();
		if (response.isError()) {
			LOGGER.error(
					String.format(Messages.Error.ERROR_WHILE_REMOVING_VN, networkInstanceId, response.getMessage()));
		}
	}

	private String[] sliceCIDR(String cidr) throws InvalidParameterException {
		String[] slice = null;
		try {
			slice = cidr.split(CIDR_SEPARATOR);
			int value = Integer.parseInt(slice[1]);
			slice[1] = String.valueOf((int) Math.pow(2, 32 - value));
		} catch (Exception e) {
			throw new InvalidParameterException();
		}
		return slice;
	}

	protected String createSecurityGroup(Client client, NetworkOrder networkOrder) throws InvalidParameterException {
		String name = SECURITY_GROUP_PREFIX + networkOrder.getId();
		String protocol = ALL_PROTOCOLS;
		
		String[] slice = sliceCIDR(networkOrder.getCidr());
		String ip = slice[0];
		String size = slice[1];
		
		Rule inputRule = new Rule(protocol, ip, size, null, INPUT_RULE_TYPE, null, null);
		Rule outputRule = new Rule(protocol, null, null, null, OUTPUT_RULE_TYPE, null, null);
		
		List<Rule> rules = new ArrayList<>();
		rules.add(inputRule);
		rules.add(outputRule);
		
		CreateSecurityGroupRequest request = new CreateSecurityGroupRequest.Builder()
				.name(name)
				.rules(rules)
				.build();
		
		String template = request.getSecurityGroup().marshalTemplate();
		return this.factory.allocateSecurityGroup(client, template);
	}
	
	protected NetworkInstance createInstance(VirtualNetwork virtualNetwork) {
		String id = virtualNetwork.getId();
		String name = virtualNetwork.getName();
		String address = virtualNetwork.xpath(TEMPLATE_NETWORK_ADDRESS_PATH);
		String gateway = virtualNetwork.xpath(TEMPLATE_NETWORK_GATEWAY_PATH);
		String vLan = virtualNetwork.xpath(TEMPLATE_VLAN_ID_PATH);
		String networkInterface = null;
		String macInterface = null;
		String interfaceState = null;
		int state = virtualNetwork.state();
		InstanceState instanceState = OpenNebulaStateMapper.map(ResourceType.NETWORK, state);
		NetworkAllocationMode allocationMode = NetworkAllocationMode.DYNAMIC;
		
		NetworkInstance networkInstance = new NetworkInstance(
				id, 
				instanceState, 
				name, 
				address, 
				gateway, 
				vLan, 
				allocationMode, 
				networkInterface, 
				macInterface, 
				interfaceState);
		
		return networkInstance;
	}
	
	protected void setFactory(OpenNebulaClientFactory factory) {
		this.factory = factory;
	}
	
}
