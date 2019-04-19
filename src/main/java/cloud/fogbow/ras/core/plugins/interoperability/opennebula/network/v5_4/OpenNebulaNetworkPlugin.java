package cloud.fogbow.ras.core.plugins.interoperability.opennebula.network.v5_4;

import java.util.ArrayList;
import java.util.List;
import java.util.Properties;
import java.util.UUID;

import cloud.fogbow.ras.constants.SystemConstants;
import cloud.fogbow.ras.core.models.ResourceType;
import cloud.fogbow.ras.core.plugins.interoperability.opennebula.OpenNebulaStateMapper;
import org.apache.log4j.Logger;
import org.opennebula.client.Client;
import org.opennebula.client.OneResponse;
import org.opennebula.client.secgroup.SecurityGroup;
import org.opennebula.client.vnet.VirtualNetwork;

import cloud.fogbow.common.exceptions.FatalErrorException;
import cloud.fogbow.common.exceptions.FogbowException;
import cloud.fogbow.common.exceptions.InstanceNotFoundException;
import cloud.fogbow.common.exceptions.InvalidParameterException;
import cloud.fogbow.common.exceptions.UnauthorizedRequestException;
import cloud.fogbow.common.models.CloudUser;
import cloud.fogbow.common.util.PropertiesUtil;
import cloud.fogbow.ras.api.http.response.InstanceState;
import cloud.fogbow.ras.api.http.response.NetworkInstance;
import cloud.fogbow.ras.constants.Messages;
import cloud.fogbow.ras.core.models.NetworkAllocationMode;
import cloud.fogbow.ras.core.models.orders.NetworkOrder;
import cloud.fogbow.ras.core.plugins.interoperability.NetworkPlugin;
import cloud.fogbow.ras.core.plugins.interoperability.opennebula.OpenNebulaClientUtil;
import cloud.fogbow.ras.core.plugins.interoperability.opennebula.OpenNebulaConfigurationPropertyKeys;
import cloud.fogbow.ras.core.plugins.interoperability.opennebula.securityrule.v5_4.CreateSecurityGroupRequest;
import cloud.fogbow.ras.core.plugins.interoperability.opennebula.securityrule.v5_4.Rule;

public class OpenNebulaNetworkPlugin implements NetworkPlugin<CloudUser> {

	private static final Logger LOGGER = Logger.getLogger(OpenNebulaNetworkPlugin.class);

	private static final String ALL_PROTOCOLS = "ALL";
	private static final String CIDR_FORMAT = "%s/%s";
	private static final String CIDR_SEPARATOR = "[/]";
	private static final String DEFAULT_NETWORK_ID_KEY = "default_network_id";
	private static final String DEFAULT_SECURITY_GROUP_ID_KEY = "default_security_group_id";
	private static final String INPUT_RULE_TYPE = "inbound";
	private static final String OUTPUT_RULE_TYPE = "outbound";
	private static final String SECURITY_GROUP_RESOURCE = "SecurityGroup";
	private static final String SECURITY_GROUPS_FORMAT = "%s,%s";
	private static final String VIRTUAL_NETWORK_RESOURCE = "VirtualNetwork";

	private static final int BASE_VALUE = 2;
	private static final int SECURITY_GROUP_VALID_POSITION = 1;
	
	protected static final String SECURITY_GROUPS_SEPARATOR = ",";
	protected static final String VNET_ADDRESS_RANGE_IP_PATH = "/VNET/AR_POOL/AR/IP";
	protected static final String VNET_ADDRESS_RANGE_SIZE_PATH = "/VNET/AR_POOL/AR/SIZE";
	protected static final String VNET_TEMPLATE_SECURITY_GROUPS_PATH = "/VNET/TEMPLATE/SECURITY_GROUPS";
	protected static final String VNET_TEMPLATE_VLAN_ID_PATH = "/VNET/TEMPLATE/VLAN_ID";

	protected static final int IPV4_AMOUNT_BITS = 32;

	private String endpoint;
	private String defaultNetwork;
	private String defaultSecurityGroup;

	public OpenNebulaNetworkPlugin(String confFilePath) throws FatalErrorException {
		Properties properties = PropertiesUtil.readProperties(confFilePath);
		this.endpoint = properties.getProperty(OpenNebulaConfigurationPropertyKeys.OPENNEBULA_RPC_ENDPOINT_KEY);
		this.defaultNetwork = properties.getProperty(DEFAULT_NETWORK_ID_KEY);
		this.defaultSecurityGroup = properties.getProperty(DEFAULT_SECURITY_GROUP_ID_KEY);
	}

	@Override
	public boolean isReady(String cloudState) {
		return OpenNebulaStateMapper.map(ResourceType.NETWORK, cloudState).equals(InstanceState.READY);
	}

	@Override
	public boolean hasFailed(String cloudState) {
		return OpenNebulaStateMapper.map(ResourceType.NETWORK, cloudState).equals(InstanceState.FAILED);
	}

	@Override
	public String requestInstance(NetworkOrder networkOrder, CloudUser cloudUser) throws FogbowException {
		LOGGER.info(String.format(Messages.Info.REQUESTING_INSTANCE, cloudUser.getToken()));
		Client client = OpenNebulaClientUtil.createClient(this.endpoint, cloudUser.getToken());

		int defaultNetworkID = convertToInteger(this.defaultNetwork);
		String networkName = networkOrder.getName();
		String name = networkName == null ? SystemConstants.FOGBOW_INSTANCE_NAME_PREFIX + getRandomUUID() : networkName;
		int size = getAddressRangeSize(networkOrder.getCidr());

		CreateNetworkReserveRequest reserveRequest = new CreateNetworkReserveRequest.Builder()
				.name(name)
				.size(size)
				.build();

		String networkReserveTemplate = reserveRequest.getVirtualNetworkReserved().marshalTemplate();
		String networkInstance = OpenNebulaClientUtil.reserveVirtualNetwork(client, defaultNetworkID,
				networkReserveTemplate);

		String securityGroupInstance = createSecurityGroup(client, networkInstance, networkOrder);
		String securityGroups = String.format(SECURITY_GROUPS_FORMAT, this.defaultSecurityGroup, securityGroupInstance);

		CreateNetworkUpdateRequest updateRequest = new CreateNetworkUpdateRequest.Builder()
				.securityGroups(securityGroups)
				.build();

		int virtualNetworkID = convertToInteger(networkInstance);
		String networkUpdateTemplate = updateRequest.getVirtualNetworkUpdate().marshalTemplate();
		return OpenNebulaClientUtil.updateVirtualNetwork(client, virtualNetworkID, networkUpdateTemplate);
	}

	@Override
	public NetworkInstance getInstance(NetworkOrder networkOrder, CloudUser cloudUser) throws FogbowException {
		LOGGER.info(String.format(Messages.Info.GETTING_INSTANCE, networkOrder.getInstanceId(), cloudUser.getToken()));
		Client client = OpenNebulaClientUtil.createClient(this.endpoint, cloudUser.getToken());
		VirtualNetwork virtualNetwork = OpenNebulaClientUtil.getVirtualNetwork(client, networkOrder.getInstanceId());
		return createInstance(virtualNetwork);
	}

	@Override
	public void deleteInstance(NetworkOrder networkOrder, CloudUser cloudUser) throws FogbowException {
		LOGGER.info(String.format(Messages.Info.DELETING_INSTANCE, networkOrder.getInstanceId(), cloudUser.getToken()));
		Client client = OpenNebulaClientUtil.createClient(this.endpoint, cloudUser.getToken());
		VirtualNetwork virtualNetwork = OpenNebulaClientUtil.getVirtualNetwork(client, networkOrder.getInstanceId());
		String securityGroupId = getSecurityGroupBy(virtualNetwork);
		SecurityGroup securityGroup = OpenNebulaClientUtil.getSecurityGroup(client, securityGroupId);
		deleteVirtualNetwork(virtualNetwork);
		deleteSecurityGroup(securityGroup);
	}

	private void deleteSecurityGroup(SecurityGroup securityGroup) {
		OneResponse response = securityGroup.delete();
		if (response.isError()) {
			LOGGER.error(String.format(Messages.Error.ERROR_WHILE_REMOVING_RESOURCE, SECURITY_GROUP_RESOURCE, response.getMessage()));
		}
	}

	private void deleteVirtualNetwork(VirtualNetwork virtualNetwork) {
		OneResponse response = virtualNetwork.delete();
		if (response.isError()) {
			LOGGER.error(String.format(Messages.Error.ERROR_WHILE_REMOVING_RESOURCE, VIRTUAL_NETWORK_RESOURCE, response.getMessage()));
		}
	}

	protected String getSecurityGroupBy(VirtualNetwork virtualNetwork) {
		String content = virtualNetwork.xpath(VNET_TEMPLATE_SECURITY_GROUPS_PATH);
		if (content == null || content.isEmpty()) {
			LOGGER.warn(Messages.Error.CONTENT_SECURITY_GROUP_NOT_DEFINED);
			return null;
		}
		String[] contentSlices = content.split(SECURITY_GROUPS_SEPARATOR);
		if (contentSlices.length < 2) {
			LOGGER.warn(Messages.Error.CONTENT_SECURITY_GROUP_WRONG_FORMAT);
			return null;
		}
		return contentSlices[SECURITY_GROUP_VALID_POSITION];
	}

	protected String createSecurityGroup(Client client, String virtualNetworkId, NetworkOrder networkOrder)
			throws InvalidParameterException, UnauthorizedRequestException, InstanceNotFoundException {

		VirtualNetwork virtualNetwork = OpenNebulaClientUtil.getVirtualNetwork(client, virtualNetworkId);
		String ip = virtualNetwork.xpath(VNET_ADDRESS_RANGE_IP_PATH);
		String size = virtualNetwork.xpath(VNET_ADDRESS_RANGE_SIZE_PATH);
		String name = generateSecurityGroupName(networkOrder);
		
		// "ALL" setting applies to all protocols if a port range is not defined
		String protocol = ALL_PROTOCOLS;

		// An undefined port range is interpreted by opennebula as all open
		String rangeAll = null;

		// An undefined ip and size is interpreted by opennebula as any network
		String ipAny = null;
		String sizeAny = null;

		// The networkId and securityGroupId parameters are not used in this context.
		String networkId = null;
		String securityGroupId = null;

		Rule inputRule = new Rule(protocol, ip, size, rangeAll, INPUT_RULE_TYPE, networkId, securityGroupId);
		Rule outputRule = new Rule(protocol, ipAny, sizeAny, rangeAll, OUTPUT_RULE_TYPE, networkId, securityGroupId);

		List<Rule> rules = new ArrayList<>();
		rules.add(inputRule);
		rules.add(outputRule);

		CreateSecurityGroupRequest request = new CreateSecurityGroupRequest.Builder()
				.name(name)
				.rules(rules)
				.build();

		String template = request.getSecurityGroup().marshalTemplate();
		return OpenNebulaClientUtil.allocateSecurityGroup(client, template);
	}
	
	protected String generateSecurityGroupName(NetworkOrder networkOrder) {
		return SystemConstants.PN_SECURITY_GROUP_PREFIX + networkOrder.getId();
	}
	
	protected NetworkInstance createInstance(VirtualNetwork virtualNetwork) throws InvalidParameterException {
		String id = virtualNetwork.getId();
		String name = virtualNetwork.getName();
		String vLan = virtualNetwork.xpath(VNET_TEMPLATE_VLAN_ID_PATH);
		String gateway = virtualNetwork.xpath(VNET_ADDRESS_RANGE_IP_PATH);
		String rangeSize = virtualNetwork.xpath(VNET_ADDRESS_RANGE_SIZE_PATH);
		String address = generateAddressCIDR(gateway, rangeSize);
		String networkInterface = null;
		String macInterface = null;
		String interfaceState = null;
		NetworkAllocationMode allocationMode = NetworkAllocationMode.DYNAMIC;
		
		NetworkInstance networkInstance = new NetworkInstance(
				id, 
				OpenNebulaStateMapper.DEFAULT_READY_STATE,
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
	
	protected String generateAddressCIDR(String address, String rangeSize) throws InvalidParameterException {
		int size = convertToInteger(rangeSize);
		int value = calculateCIDR(size);
		return String.format(CIDR_FORMAT, address, String.valueOf(value));
	}
	
	protected int calculateCIDR(int size) {
		int exponent = 1;
		int value = 0;
		for (int i = 0; i < IPV4_AMOUNT_BITS; i++)
			if (exponent >= size) {
					value = IPV4_AMOUNT_BITS - i;
					return value;
			} else {
				exponent *= BASE_VALUE;
			}
		return value;
	}

	protected int getAddressRangeSize(String cidr) throws InvalidParameterException {
		try {
			String[] slice = cidr.split(CIDR_SEPARATOR);
			String addressRangeSize = slice[1];
			int size = convertToInteger(addressRangeSize);
			return (int) Math.pow(BASE_VALUE, IPV4_AMOUNT_BITS - size);
		} catch (Exception e) {
			throw new InvalidParameterException(String.format(Messages.Exception.INVALID_CIDR, cidr));
		}
	}
	
	protected int convertToInteger(String number) throws InvalidParameterException {
		try {
			return Integer.parseInt(number);
		} catch (NumberFormatException e) {
			LOGGER.error(String.format(Messages.Error.ERROR_WHILE_CONVERTING_TO_INTEGER), e);
			throw new InvalidParameterException();
		}
	}
	
	protected String getRandomUUID() {
		return UUID.randomUUID().toString();
	}
	
}
