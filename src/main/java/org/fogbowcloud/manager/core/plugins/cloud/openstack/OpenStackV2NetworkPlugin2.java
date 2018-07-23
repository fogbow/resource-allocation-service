package org.fogbowcloud.manager.core.plugins.cloud.openstack;

import java.io.File;
import java.util.Properties;
import java.util.UUID;

import org.apache.http.client.HttpResponseException;
import org.apache.log4j.Logger;
import org.fogbowcloud.manager.core.HomeDir;
import org.fogbowcloud.manager.core.constants.DefaultConfigurationConstants;
import org.fogbowcloud.manager.core.exceptions.FatalErrorException;
import org.fogbowcloud.manager.core.exceptions.FogbowManagerException;
import org.fogbowcloud.manager.core.exceptions.InstanceNotFoundException;
import org.fogbowcloud.manager.core.exceptions.InvalidParameterException;
import org.fogbowcloud.manager.core.exceptions.UnexpectedException;
import org.fogbowcloud.manager.core.models.instances.InstanceState;
import org.fogbowcloud.manager.core.models.instances.InstanceType;
import org.fogbowcloud.manager.core.models.instances.NetworkInstance;
import org.fogbowcloud.manager.core.models.orders.NetworkAllocationMode;
import org.fogbowcloud.manager.core.models.orders.NetworkOrder;
import org.fogbowcloud.manager.core.models.tokens.Token;
import org.fogbowcloud.manager.core.plugins.cloud.NetworkPlugin;
import org.fogbowcloud.manager.util.PropertiesUtil;
import org.fogbowcloud.manager.util.connectivity.HttpRequestClientUtil;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

public class OpenStackV2NetworkPlugin2 implements NetworkPlugin {
	private static final String NETWORK_NEUTRONV2_URL_KEY = "openstack_neutron_v2_url";

	private static final String MSG_LOG_ERROR_MANIPULATE_JSON =
			"An error occurred when manipulate json.";

	protected static final String SUFFIX_ENDPOINT_NETWORK = "/networks";
	protected static final String SUFFIX_ENDPOINT_SUBNET = "/subnets";
	protected static final String SUFFIX_ENDPOINT_ROUTER = "/routers";
	protected static final String SUFFIX_ENDPOINT_PORTS = "/ports";
	protected static final String SUFFIX_ENDPOINT_SECURITY_GROUP_RULES = "/security-group-rules";
	protected static final String SUFFIX_ENDPOINT_SECURITY_GROUP = "/security-groups";
	protected static final String V2_API_ENDPOINT = "/v2.0";

	protected static final String TENANT_ID = "tenantId";
	protected static final String KEY_PROVIDER_SEGMENTATION_ID = "provider:segmentation_id";
	protected static final String KEY_EXTERNAL_GATEWAY_INFO = "external_gateway_info";
	protected static final String KEY_DNS_NAMESERVERS = "dns_nameservers";
	protected static final String KEY_DEVICE_OWNER = "device_owner";
	protected static final String KEY_JSON_SUBNET_ID = "subnet_id";
	protected static final String KEY_ENABLE_DHCP = "enable_dhcp";
	protected static final String KEY_IP_VERSION = "ip_version";
	protected static final String KEY_GATEWAY_IP = "gateway_ip";
	protected static final String KEY_FIXES_IPS = "fixed_ips";
	protected static final String KEY_TENANT_ID = "tenant_id";
	protected static final String KEY_JSON_ROUTERS = "routers";
	protected static final String KEY_JSON_NETWORK = "network";
	protected static final String KEY_NETWORK_ID = "network_id";
	protected static final String KEY_JSON_SUBNET = "subnet";
	protected static final String KEY_SUBNETS = "subnets";
	protected static final String KEY_SECURITY_GROUP = "security_group";
	protected static final String KEY_SECURITY_GROUP_RULE = "security_group_rule";
	protected static final String KEY_JSON_ROUTER = "router";
	protected static final String KEY_JSON_PORTS = "ports";
	protected static final String KEY_DEVICE_ID = "device_id";
	protected static final String KEY_STATUS = "status";
	protected static final String KEY_NAME = "name";
	protected static final String KEY_CIDR = "cidr";
	protected static final String KEY_ID = "id";

	protected static final String DEFAULT_IP_VERSION = "4";
	protected static final String DEFAULT_NETWORK_NAME = "fogbow-network";
	protected static final String DEFAULT_ROUTER_NAME = "fogbow-router";
	protected static final String DEFAULT_SUBNET_NAME = "fogbow-subnet";
	protected static final String DEFAULT_SECURITY_GROUP_NAME = "fogbow-sg";
	protected static final String[] DEFAULT_DNS_NAME_SERVERS = new String[] {"8.8.8.8", "8.8.4.4"};
	protected static final String DEFAULT_NETWORK_ADDRESS = "192.168.0.1/24";
	protected static final String NETWORK_DHCP = "network:dhcp";
	protected static final String COMPUTE_NOVA = "compute:nova";
	protected static final String NETWORK_ROUTER = "network:ha_router_replicated_interface";

	// security group keys
	protected static final String DIRECTION = "direction";
	protected static final String SECURITY_GROUP_ID = "security_group_id";
	protected static final String REMOTE_IP_PREFIX = "remote_ip_prefix";
	protected static final String PROTOCOL = "protocol";
	protected static final String PORT_RANGE_MIN = "port_range_min";
	protected static final String PORT_RANGE_MAX = "port_range_max";
	protected static final String INGRESS_DIRECTION = "ingress";
	protected static final String IPV4 = "IPv4";
	public static final int SSH_PORT = 22;

	private HttpRequestClientUtil client;
	private String networkV2APIEndpoint;
	private String externalNetworkId;
	private String[] dnsList;

	private static final Logger LOGGER = Logger.getLogger(OpenStackV2NetworkPlugin.class);

	public OpenStackV2NetworkPlugin2() throws FatalErrorException {
		HomeDir homeDir = HomeDir.getInstance();
		Properties properties = PropertiesUtil.readProperties(homeDir.getPath() + File.separator
				+ DefaultConfigurationConstants.OPENSTACK_CONF_FILE_NAME);

		this.externalNetworkId = properties.getProperty(KEY_EXTERNAL_GATEWAY_INFO);
		this.networkV2APIEndpoint =
				properties.getProperty(NETWORK_NEUTRONV2_URL_KEY)
						+ V2_API_ENDPOINT;
		setDNSList(properties);

		initClient();
	}

	protected void setDNSList(Properties properties) {
		String dnsProperty = properties.getProperty(KEY_DNS_NAMESERVERS);
		if (dnsProperty != null) {
			this.dnsList = dnsProperty.split(",");
		}
	}

	@Override
	public String requestInstance(NetworkOrder order, Token localToken)
			throws FogbowManagerException, UnexpectedException {
		String tenantId = localToken.getAttributes().get(TENANT_ID);
		String responseStr = null;
		JSONObject jsonRequest = null;

		// Creating network
		try {
			jsonRequest = generateJsonEntityToCreateNetwork(tenantId);
		} catch (JSONException e) {
			String errorMsg = "An error occurred when generating json.";
			LOGGER.error(errorMsg, e);
			throw new InvalidParameterException(errorMsg, e);
		}
		try {
			String endpoint = this.networkV2APIEndpoint + SUFFIX_ENDPOINT_NETWORK;
			responseStr = this.client.doPostRequest(endpoint, localToken, jsonRequest);
		} catch (HttpResponseException e) {
			OpenStackHttpToFogbowManagerExceptionMapper.map(e);
		}
		String networkId = getNetworkIdFromJson(responseStr);

		// Creating subnet
		try {
			jsonRequest = generateJsonEntityToCreateSubnet(networkId, tenantId, order);
		} catch (JSONException e) {
			String errorMsg =
					String.format("Error while trying to generate json subnet entity with networkId %s for order %s",
							networkId, order);
			LOGGER.error(errorMsg, e);
			throw new InvalidParameterException(errorMsg, e);
		}
		try {
			String endpoint = this.networkV2APIEndpoint + SUFFIX_ENDPOINT_SUBNET;
			responseStr = this.client.doPostRequest(endpoint, localToken, jsonRequest);
		} catch (HttpResponseException e) {
			removeNetwork(localToken, networkId);
			OpenStackHttpToFogbowManagerExceptionMapper.map(e);
		}

		// Creating security group
		try {
			jsonRequest = generateJsonEntityToCreateSecurityGroup(networkId, tenantId);
		} catch (JSONException e) {
			String errorMsg =
					String.format("Error while trying to generate json subnet entity with networkId %s for order %s",
							networkId, order);
			LOGGER.error(errorMsg, e);
			throw new InvalidParameterException(errorMsg, e);
		}
		try {
			String endpoint = this.networkV2APIEndpoint + SUFFIX_ENDPOINT_SECURITY_GROUP;
			responseStr = this.client.doPostRequest(endpoint, localToken, jsonRequest);
		} catch (HttpResponseException e) {
			removeNetwork(localToken, networkId);
			OpenStackHttpToFogbowManagerExceptionMapper.map(e);
		}
		String securityGroupId = getSecurityGroupIdFromJson(responseStr);

		// Adding security groups rules
		try {
			jsonRequest = generateJsonEntityToCreateSecurityGroupRule(securityGroupId, INGRESS_DIRECTION, order.getAddress(),
					IPV4, SSH_PORT, SSH_PORT);
		} catch (JSONException e) {
			String errorMsg =
					String.format("Error while trying to generate json subnet entity with networkId %s for order %s",
							networkId, order);
			LOGGER.error(errorMsg, e);
			throw new InvalidParameterException(errorMsg, e);
		}
		try {
			String endpoint = this.networkV2APIEndpoint + SUFFIX_ENDPOINT_SECURITY_GROUP_RULES;
			responseStr = this.client.doPostRequest(endpoint, localToken, jsonRequest);
		} catch (HttpResponseException e) {
			removeNetwork(localToken, networkId);
			OpenStackHttpToFogbowManagerExceptionMapper.map(e);
		}

		return networkId;
	}

	private String getSecurityGroupIdFromJson(String json) throws UnexpectedException {
		String securityGroupId = null;
		try {
			JSONObject rootServer = new JSONObject(json);
			JSONObject securityGroupJSONObject = rootServer.optJSONObject(KEY_SECURITY_GROUP);
			securityGroupId = securityGroupJSONObject.optString(KEY_ID);
		} catch (JSONException e) {
			String errorMsg = String.format("Was not possible retrieve network id from json %s", json);
			LOGGER.error(errorMsg);
			throw new UnexpectedException(errorMsg, e);
		}
		return securityGroupId;
	}

	private JSONObject generateJsonEntityToCreateSecurityGroup(String networkId, String tenantId) {
		JSONObject securityGroup = new JSONObject();
		JSONObject securityGroupObject = new JSONObject();
		securityGroupObject.put(KEY_NAME, DEFAULT_SECURITY_GROUP_NAME + "-" + networkId);
		securityGroupObject.put(KEY_TENANT_ID, tenantId);
		securityGroup.put(KEY_SECURITY_GROUP, securityGroupObject);
		return securityGroup;
	}

	private JSONObject generateJsonEntityToCreateSecurityGroupRule(String securityGroupId, String direction,
	                                                               String remoteIpPrefix, String protocol,int minPort,
	                                                               int maxPort) {
		JSONObject securityGroupRule = new JSONObject();
		JSONObject securityGroupObject = new JSONObject();

		securityGroupObject.put(DIRECTION, direction);
		securityGroupObject.put(SECURITY_GROUP_ID, securityGroupId);
		securityGroupObject.put(REMOTE_IP_PREFIX, remoteIpPrefix);
		securityGroupObject.put(PROTOCOL, protocol);
		securityGroupObject.put(PORT_RANGE_MIN, minPort);
		securityGroupObject.put(PORT_RANGE_MAX, maxPort);

		securityGroupRule.put(KEY_SECURITY_GROUP_RULE, securityGroupObject);
		return securityGroupRule;
	}

	@Override
	public NetworkInstance getInstance(String instanceId, Token token)
			throws FogbowManagerException, UnexpectedException {
		String endpoint = this.networkV2APIEndpoint + SUFFIX_ENDPOINT_NETWORK + "/" + instanceId;
		String responseStr = null;
		try {
			responseStr = this.client.doGetRequest(endpoint, token);
		} catch (HttpResponseException e) {
			OpenStackHttpToFogbowManagerExceptionMapper.map(e);
		}
		return getInstanceFromJson(responseStr, token);

	}

	@Override
	public void deleteInstance(String networkId, Token token) throws FogbowManagerException, UnexpectedException {
		boolean wasNetworkRemoved = false;
		try {
			wasNetworkRemoved = removeNetwork(token, networkId);
		} catch (InstanceNotFoundException e) {
			// continue and try to delete the security group
			wasNetworkRemoved = true;
		}
		if (!wasNetworkRemoved) {
			String errorMsg = String.format("Was not possible delete network with id %s", networkId);
			LOGGER.error(errorMsg);
			throw new UnexpectedException(errorMsg);
		}
		
		boolean wasSecurityGroupRemoved = removeSecurityGroup(token, networkId);		
		if (!wasSecurityGroupRemoved) {
			String errorMsg = String.format("Was not possible delete security group with id %s", networkId);
			LOGGER.error(errorMsg);
			throw new UnexpectedException(errorMsg);
		}		
	}

	protected boolean removeSecurityGroup(Token token, String securityGroupId)
			throws FogbowManagerException, UnexpectedException {		
		LOGGER.debug(String.format("Removing security group %s", securityGroupId));
		try {
			String endpoint = this.networkV2APIEndpoint + SUFFIX_ENDPOINT_SECURITY_GROUP_RULES + "/" + securityGroupId;
			this.client.doDeleteRequest(endpoint, token);
			return true;
		} catch (HttpResponseException e) {
			OpenStackHttpToFogbowManagerExceptionMapper.map(e);
			return false;
		}
	}

	protected NetworkInstance getInstanceFromJson(String json, Token token)
			throws FogbowManagerException, UnexpectedException {
		String networkId = null;
		String label = null;
		String instanceState = null;
		String vlan = null;
		String subnetId = null;
		try {
			JSONObject rootServer = new JSONObject(json);
			JSONObject networkJSONObject = rootServer.optJSONObject(KEY_JSON_NETWORK);
			networkId = networkJSONObject.optString(KEY_ID);

			vlan = networkJSONObject.optString(KEY_PROVIDER_SEGMENTATION_ID);
			instanceState = networkJSONObject.optString(KEY_STATUS);
			label = networkJSONObject.optString(KEY_NAME);
			subnetId = networkJSONObject.optJSONArray(KEY_SUBNETS).optString(0);
		} catch (JSONException e) {
			String errorMsg = String.format("Was not possible to get network informations from json %s", json);
			LOGGER.error(errorMsg, e);
			throw new InvalidParameterException(errorMsg, e);
		}

		String gateway = null;
		String address = null;
		NetworkAllocationMode allocation = null;
		try {
			JSONObject subnetJSONObject = getSubnetInformation(token, subnetId);

			if (subnetJSONObject != null) {
				gateway = subnetJSONObject.optString(KEY_GATEWAY_IP);
				allocation = null;
				boolean enableDHCP = subnetJSONObject.optBoolean(KEY_ENABLE_DHCP);
				if (enableDHCP) {
					allocation = NetworkAllocationMode.DYNAMIC;
				} else {
					allocation = NetworkAllocationMode.STATIC;
				}
				address = subnetJSONObject.optString(KEY_CIDR);
			}
		} catch (JSONException e) {
			String errorMsg = String.format("Was not possible to get network informations from json %s", json);
			LOGGER.error(errorMsg, e);
			throw new InvalidParameterException(errorMsg, e);
		}

		NetworkInstance instance = null;
		if (networkId != null) {
			InstanceState fogbowState = OpenStackStateMapper.map(InstanceType.NETWORK, instanceState);
			instance = new NetworkInstance(networkId, fogbowState, label, address, gateway,
					vlan, allocation, null, null, null);
		}

		return instance;
	}

	private JSONObject getSubnetInformation(Token token, String subnetId)
			throws FogbowManagerException, UnexpectedException {
		String endpoint = this.networkV2APIEndpoint + SUFFIX_ENDPOINT_SUBNET + "/" + subnetId;
		String responseStr = null;
		try {
			responseStr = this.client.doGetRequest(endpoint, token);
		} catch (HttpResponseException e) {
			OpenStackHttpToFogbowManagerExceptionMapper.map(e);
		}

		JSONObject rootServer = new JSONObject(responseStr);
		JSONObject subnetJSON = rootServer.optJSONObject(KEY_JSON_SUBNET);
		return subnetJSON;
	}

	protected String getNetworkIdFromJson(String json) throws UnexpectedException {
		String networkId = null;
		try {
			JSONObject rootServer = new JSONObject(json);
			JSONObject networkJSONObject = rootServer.optJSONObject(KEY_JSON_NETWORK);
			networkId = networkJSONObject.optString(KEY_ID);
		} catch (JSONException e) {
			String errorMsg = String.format("Was not possible retrieve network id from json %s", json);
			LOGGER.error(errorMsg);
			throw new UnexpectedException(errorMsg, e);
		}
		return networkId;
	}

	protected String getSubnetIdFromJson(String json) throws UnexpectedException {
		String subnetId = null;
		try {
			JSONObject rootServer = new JSONObject(json);
			JSONObject networkJSONObject = rootServer.optJSONObject(KEY_JSON_SUBNET);
			subnetId = networkJSONObject.optString(KEY_ID);
		} catch (JSONException e) {
			String errorMsg = String.format("Was not possible retrieve subnet id from json %s", json);
			LOGGER.error(errorMsg);
			throw new UnexpectedException(errorMsg, e);
		}
		return subnetId;
	}

	protected JSONObject generateJsonEntityToCreateNetwork(String tenantId) throws JSONException {
		JSONObject networkContent = new JSONObject();
		networkContent.put(KEY_NAME, DEFAULT_NETWORK_NAME + "-" + UUID.randomUUID());
		networkContent.put(KEY_TENANT_ID, tenantId);

		JSONObject network = new JSONObject();
		network.put(KEY_JSON_NETWORK, networkContent);

		return network;
	}

	protected JSONObject generateJsonEntitySubnetId(String subnetId) throws JSONException {
		JSONObject subnet = new JSONObject();
		subnet.put(KEY_JSON_SUBNET_ID, subnetId);

		return subnet;
	}

	protected JSONObject generateJsonEntityToCreateSubnet(String networkId, String tenantId,
	                                                      NetworkOrder order) throws JSONException {
		JSONObject subnetContent = new JSONObject();
		subnetContent.put(KEY_NAME, DEFAULT_SUBNET_NAME + "-" + UUID.randomUUID());
		subnetContent.put(KEY_TENANT_ID, tenantId);
		subnetContent.put(KEY_NETWORK_ID, networkId);
		subnetContent.put(KEY_IP_VERSION, DEFAULT_IP_VERSION);

		String gateway = order.getGateway();
		if (gateway != null && !gateway.isEmpty()) {
			subnetContent.put(KEY_GATEWAY_IP, gateway);
		}

		String networkAddress = order.getAddress();
		if (networkAddress == null) {
			networkAddress = DEFAULT_NETWORK_ADDRESS;
		}
		subnetContent.put(KEY_CIDR, networkAddress);

		NetworkAllocationMode networkAllocationMode = order.getAllocation();
		if (networkAllocationMode != null) {
			if (networkAllocationMode.equals(NetworkAllocationMode.DYNAMIC)) {
				subnetContent.put(KEY_ENABLE_DHCP, true);
			} else if (networkAllocationMode.equals(NetworkAllocationMode.STATIC)) {
				subnetContent.put(KEY_ENABLE_DHCP, false);
			}
		}

		String[] dnsNamesServers = this.dnsList;
		if (dnsNamesServers == null) {
			dnsNamesServers = DEFAULT_DNS_NAME_SERVERS;
		}
		JSONArray dnsNameServersArray = new JSONArray();
		for (int i = 0; i < dnsNamesServers.length; i++) {
			dnsNameServersArray.put(dnsNamesServers[i]);
		}
		subnetContent.put(KEY_DNS_NAMESERVERS, dnsNameServersArray);

		JSONObject subnet = new JSONObject();
		subnet.put(KEY_JSON_SUBNET, subnetContent);

		return subnet;
	}

	protected boolean removeNetwork(Token token, String networkId) throws UnexpectedException, FogbowManagerException {
		String messageTemplate = "Removing network %s";
		LOGGER.debug(String.format(messageTemplate, networkId));
		String endpoint = this.networkV2APIEndpoint + SUFFIX_ENDPOINT_NETWORK + "/" + networkId;
		try {
			this.client.doDeleteRequest(endpoint, token);
			return true;
		} catch (HttpResponseException e) {
			OpenStackHttpToFogbowManagerExceptionMapper.map(e);
			return false;
		}
	}

	private void initClient() {
		this.client = new HttpRequestClientUtil();
	}

	protected void setClient(HttpRequestClientUtil client) {
		this.client = client;
	}

	protected String[] getDnsList() {
		return dnsList;
	}

}

