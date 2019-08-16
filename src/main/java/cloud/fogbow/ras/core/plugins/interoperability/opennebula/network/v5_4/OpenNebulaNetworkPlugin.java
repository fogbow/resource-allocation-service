package cloud.fogbow.ras.core.plugins.interoperability.opennebula.network.v5_4;

import java.util.ArrayList;
import java.util.List;
import java.util.Properties;
import java.util.UUID;

import cloud.fogbow.common.exceptions.*;
import cloud.fogbow.ras.constants.SystemConstants;
import cloud.fogbow.ras.core.models.ResourceType;
import cloud.fogbow.ras.core.plugins.interoperability.opennebula.OpenNebulaStateMapper;
import org.apache.commons.lang.math.NumberUtils;
import org.apache.commons.net.util.SubnetUtils;
import org.apache.log4j.Logger;
import org.opennebula.client.Client;
import org.opennebula.client.OneResponse;
import org.opennebula.client.secgroup.SecurityGroup;
import org.opennebula.client.vnet.VirtualNetwork;

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
	private static final String DEFAULT_NETWORK_ID_KEY = "default_network_id";
	private static final String INPUT_RULE_TYPE = "inbound";
	private static final String OUTPUT_RULE_TYPE = "outbound";
	private static final String SECURITY_GROUP_RESOURCE = "SecurityGroup";
	private static final String VIRTUAL_NETWORK_RESOURCE = "VirtualNetwork";

	private static final int BASE_VALUE = 2;

	protected static final String SECURITY_GROUPS_SEPARATOR = ",";
	protected static final String VNET_ADDRESS_RANGE_IP_PATH = "/VNET/AR_POOL/AR/IP";
	protected static final String VNET_ADDRESS_RANGE_SIZE_PATH = "/VNET/AR_POOL/AR/SIZE";
	protected static final String VNET_TEMPLATE_SECURITY_GROUPS_PATH = "/VNET/TEMPLATE/SECURITY_GROUPS";
	protected static final String VNET_TEMPLATE_VLAN_ID_PATH = "/VNET/TEMPLATE/VLAN_ID";

	protected static final String ADDRESS_RANGE_ID_PATH_FORMAT = "/VNET/AR_POOL/AR[%s]/AR_ID";
	protected static final String ADDRESS_RANGE_IP_PATH_FORMAT = "/VNET/AR_POOL/AR[%s]/IP";
	protected static final String ADDRESS_RANGE_SIZE_PATH_FORMAT = "/VNET/AR_POOL/AR[%s]/SIZE";
	protected static final String ADDRESS_RANGE_USED_LEASES_PATH_FORMAT = "/VNET/AR_POOL/AR[%s]/USED_LEASES";

	protected static final int IPV4_AMOUNT_BITS = 32;

	private String endpoint;
	private String defaultNetwork;

	public OpenNebulaNetworkPlugin(String confFilePath) throws FatalErrorException {
		Properties properties = PropertiesUtil.readProperties(confFilePath);
		this.endpoint = properties.getProperty(OpenNebulaConfigurationPropertyKeys.OPENNEBULA_RPC_ENDPOINT_KEY);
		this.defaultNetwork = properties.getProperty(DEFAULT_NETWORK_ID_KEY);
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
		Client client = OpenNebulaClientUtil.createClient(this.endpoint, cloudUser.getToken());
		VirtualNetwork virtualNetwork = OpenNebulaClientUtil.getVirtualNetwork(client, this.defaultNetwork);

		int defaultNetworkID = convertToInteger(this.defaultNetwork);
		String networkName = networkOrder.getName();
		String name = networkName == null ? SystemConstants.FOGBOW_INSTANCE_NAME_PREFIX + getRandomUUID() : networkName;

		SubnetUtils.SubnetInfo subnetInfo = new SubnetUtils(networkOrder.getCidr()).getInfo();
		int size = subnetInfo.getAddressCount();
		Integer addressRangeIndex = this.getAddressRangeIndex(virtualNetwork, subnetInfo.getLowAddress(), size);
		String addressRangeId = null;

		if (addressRangeIndex != null) {
			addressRangeId = virtualNetwork.xpath(String.format(ADDRESS_RANGE_ID_PATH_FORMAT, addressRangeIndex));
		} else {
			throw new NoAvailableResourcesException(String.format(Messages.Exception.UNABLE_TO_CREATE_NETWORK_RESERVE,
					networkOrder.getCidr()));
		}

		String ip = this.getNextAvailableAddress(virtualNetwork, addressRangeIndex);
		CreateNetworkReserveRequest reserveRequest = new CreateNetworkReserveRequest.Builder()
				.name(name)
				.size(size)
				.ip(ip)
				.addressRangeId(addressRangeId)
				.build();

		String networkReserveTemplate = reserveRequest.getVirtualNetworkReserved().marshalTemplate();
		String networkInstance = OpenNebulaClientUtil.reserveVirtualNetwork(client, defaultNetworkID,
				networkReserveTemplate);

		String fogbowSecurityGroup = createSecurityGroup(client, networkInstance, networkOrder);

		CreateNetworkUpdateRequest updateRequest = new CreateNetworkUpdateRequest.Builder()
				.securityGroups(fogbowSecurityGroup)
				.build();

		int virtualNetworkID = convertToInteger(networkInstance);
		String networkUpdateTemplate = updateRequest.getVirtualNetworkUpdate().marshalTemplate();
		return OpenNebulaClientUtil.updateVirtualNetwork(client, virtualNetworkID, networkUpdateTemplate);
	}

	@Override
	public NetworkInstance getInstance(NetworkOrder networkOrder, CloudUser cloudUser) throws FogbowException {
		Client client = OpenNebulaClientUtil.createClient(this.endpoint, cloudUser.getToken());
		VirtualNetwork virtualNetwork = OpenNebulaClientUtil.getVirtualNetwork(client, networkOrder.getInstanceId());
		return createInstance(virtualNetwork);
	}

	@Override
	public void deleteInstance(NetworkOrder networkOrder, CloudUser cloudUser) throws FogbowException {
		Client client = OpenNebulaClientUtil.createClient(this.endpoint, cloudUser.getToken());
		VirtualNetwork virtualNetwork = OpenNebulaClientUtil.getVirtualNetwork(client, networkOrder.getInstanceId());

		SecurityGroup securityGroup = this.getSecurityGroupForVirtualNetwork(client, virtualNetwork, networkOrder.getId());
		if (securityGroup != null) {
			deleteSecurityGroup(securityGroup);
		}

		deleteVirtualNetwork(virtualNetwork);
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

	protected SecurityGroup getSecurityGroupForVirtualNetwork(Client client, VirtualNetwork virtualNetwork, String orderId)
			throws UnauthorizedRequestException, InstanceNotFoundException, InvalidParameterException {
		String securityGroupIdsStr = virtualNetwork.xpath(VNET_TEMPLATE_SECURITY_GROUPS_PATH);
		if (securityGroupIdsStr == null || securityGroupIdsStr.isEmpty()) {
			LOGGER.warn(Messages.Error.CONTENT_SECURITY_GROUP_NOT_DEFINED);
			return null;
		}

		String[] securityGroupIds =  securityGroupIdsStr.split(SECURITY_GROUPS_SEPARATOR);
		String securityGroupName = this.generateSecurityGroupName(orderId);
		SecurityGroup securityGroup = null;
		for (String securityGroupId : securityGroupIds) {
			securityGroup = OpenNebulaClientUtil.getSecurityGroup(client, securityGroupId);
			if (securityGroup.getName().equals(securityGroupName)) break;
		}

		return securityGroup;
	}

	protected String createSecurityGroup(Client client, String virtualNetworkId, NetworkOrder networkOrder)
			throws InvalidParameterException, UnauthorizedRequestException, InstanceNotFoundException {

		VirtualNetwork virtualNetwork = OpenNebulaClientUtil.getVirtualNetwork(client, virtualNetworkId);
		String ip = virtualNetwork.xpath(VNET_ADDRESS_RANGE_IP_PATH);
		String size = virtualNetwork.xpath(VNET_ADDRESS_RANGE_SIZE_PATH);
		String name = generateSecurityGroupName(networkOrder.getId());
		
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
	
	protected String generateSecurityGroupName(String instanceId) {
		return SystemConstants.PN_SECURITY_GROUP_PREFIX + instanceId;
	}
	
	protected NetworkInstance createInstance(VirtualNetwork virtualNetwork) throws InvalidParameterException {
		String id = virtualNetwork.getId();
		String name = virtualNetwork.getName();
		String vLan = virtualNetwork.xpath(VNET_TEMPLATE_VLAN_ID_PATH);
		String firstIP = virtualNetwork.xpath(VNET_ADDRESS_RANGE_IP_PATH);
		String rangeSize = virtualNetwork.xpath(VNET_ADDRESS_RANGE_SIZE_PATH);
		String address = this.generateAddressCIDR(firstIP, rangeSize);

		String networkInterface = null;
		String macInterface = null;
		String interfaceState = null;
		NetworkAllocationMode allocationMode = NetworkAllocationMode.DYNAMIC;
		
		NetworkInstance networkInstance = new NetworkInstance(
				id, 
				OpenNebulaStateMapper.DEFAULT_READY_STATE,
				name, 
				address, 
				firstIP,
				vLan,
				allocationMode, 
				networkInterface, 
				macInterface, 
				interfaceState);
		
		return networkInstance;
	}
	
	protected String generateAddressCIDR(String address, String rangeSize) throws InvalidParameterException {
		return String.format(CIDR_FORMAT, address, this.calculateCIDR(this.convertToInteger(rangeSize)));
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

	protected int getAddressRangeSize(String cidr) {
        SubnetUtils.SubnetInfo subnetInfo = new SubnetUtils(cidr).getInfo();
        return subnetInfo.getAddressCount();
	}
	
	protected int convertToInteger(String number) throws InvalidParameterException {
		try {
			return Integer.parseInt(number);
		} catch (NumberFormatException e) {
			LOGGER.error(String.format(Messages.Error.ERROR_WHILE_CONVERTING_TO_INTEGER), e);
			throw new InvalidParameterException();
		}
	}

	protected Integer getAddressRangeIndex(VirtualNetwork virtualNetwork, String lowAddress, int addressRangeSize)
			throws InvalidParameterException {
		for (int i = 1; i < Integer.MAX_VALUE; i++) {
			String addressRangeFirstIp = virtualNetwork.xpath(String.format(ADDRESS_RANGE_IP_PATH_FORMAT, i));
			String currentAddressRangeSize = virtualNetwork.xpath(String.format(ADDRESS_RANGE_SIZE_PATH_FORMAT, i));
			String addressRangeLeases = virtualNetwork.xpath(String.format(ADDRESS_RANGE_USED_LEASES_PATH_FORMAT, i));
			int usedLeases = NumberUtils.toInt(addressRangeLeases);

			// NOTE(pauloewerton): no more address ranges
			if (addressRangeFirstIp.isEmpty() || currentAddressRangeSize.isEmpty()) break;

			String addressRangeCidr = String.format(CIDR_FORMAT, addressRangeFirstIp,
					this.calculateCIDR(this.convertToInteger(currentAddressRangeSize)));

			SubnetUtils.SubnetInfo subnetInfo = new SubnetUtils(addressRangeCidr).getInfo();
			int availableAddresses = subnetInfo.getAddressCount() - usedLeases;
			if (subnetInfo.isInRange(lowAddress) && availableAddresses >= addressRangeSize) {
				return i;
			}
		}

		return null;
	}

	protected String getNextAvailableAddress(VirtualNetwork virtualNetwork, Integer addressRangeIndex)
			throws InvalidParameterException {
		String addressRangeFirstIp = virtualNetwork.xpath(String.format(ADDRESS_RANGE_IP_PATH_FORMAT, addressRangeIndex));
		String currentAddressRangeSize = virtualNetwork.xpath(String.format(ADDRESS_RANGE_SIZE_PATH_FORMAT, addressRangeIndex));
		String addressRangeLeases = virtualNetwork.xpath(String.format(ADDRESS_RANGE_USED_LEASES_PATH_FORMAT, addressRangeIndex));
		int usedLeases = NumberUtils.toInt(addressRangeLeases);

		if (usedLeases > 0) {
			String addressRangeCidr = String.format(CIDR_FORMAT, addressRangeFirstIp,
					this.calculateCIDR(this.convertToInteger(currentAddressRangeSize)));
			SubnetUtils.SubnetInfo subnetInfo = new SubnetUtils(addressRangeCidr).getInfo();

			return subnetInfo.getAllAddresses()[usedLeases];
		}

		return null;
	}

	protected String getRandomUUID() {
		return UUID.randomUUID().toString();
	}
}
