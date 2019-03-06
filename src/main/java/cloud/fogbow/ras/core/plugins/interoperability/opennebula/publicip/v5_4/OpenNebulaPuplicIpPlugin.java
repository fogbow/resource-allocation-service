package cloud.fogbow.ras.core.plugins.interoperability.opennebula.publicip.v5_4;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;

import cloud.fogbow.ras.core.plugins.interoperability.PublicIpPlugin;
import cloud.fogbow.ras.core.plugins.interoperability.opennebula.OpenNebulaConfigurationPropertyKeys;
import org.apache.log4j.Logger;
import org.opennebula.client.Client;
import org.opennebula.client.OneResponse;
import org.opennebula.client.secgroup.SecurityGroup;
import org.opennebula.client.vm.VirtualMachine;
import org.opennebula.client.vnet.VirtualNetwork;
import org.opennebula.client.vnet.VirtualNetworkPool;

import cloud.fogbow.common.exceptions.FogbowException;
import cloud.fogbow.common.exceptions.InstanceNotFoundException;
import cloud.fogbow.common.exceptions.InvalidParameterException;
import cloud.fogbow.common.exceptions.QuotaExceededException;
import cloud.fogbow.common.exceptions.UnauthorizedRequestException;
import cloud.fogbow.common.util.HomeDir;
import cloud.fogbow.common.models.CloudUser;
import cloud.fogbow.common.util.PropertiesUtil;
import cloud.fogbow.ras.api.http.response.InstanceState;
import cloud.fogbow.ras.api.http.response.PublicIpInstance;
import cloud.fogbow.ras.constants.Messages;
import cloud.fogbow.ras.constants.SystemConstants;
import cloud.fogbow.ras.core.models.orders.PublicIpOrder;
import cloud.fogbow.ras.core.plugins.interoperability.opennebula.OpenNebulaClientUtil;
import cloud.fogbow.ras.core.plugins.interoperability.opennebula.XmlUnmarshaller;
import cloud.fogbow.common.util.connectivity.cloud.opennebula.OpenNebulaTagNameConstants;
import cloud.fogbow.ras.core.plugins.interoperability.opennebula.publicip.v5_4.PublicNetworkTemplate.LeaseIp;
import cloud.fogbow.ras.core.plugins.interoperability.opennebula.securityrule.v5_4.CreateSecurityGroupRequest;
import cloud.fogbow.ras.core.plugins.interoperability.opennebula.securityrule.v5_4.Rule;

public class OpenNebulaPuplicIpPlugin implements PublicIpPlugin<CloudUser> {

	private static final Logger LOGGER = Logger.getLogger(OpenNebulaPuplicIpPlugin.class);

	private static final String ALL_PROTOCOLS = "ALL";
	private static final String CARACTER_SEPARATOR = ",";
	private static final String DEFAULT_SECURITY_GROUP = "0";
	private static final String DEFAULT_VIRTUAL_NETWORK_BRIDGED_DRIVE = "fw";
	private static final String FIXED_PUBLIC_IP_ADDRESSES_KEY = "fixed_public_ip_addresses";
	private static final String ID_SEPARATOR = " ";
	private static final String INSTANCE_ID = "%s %s %s %s";
	private static final String INPUT_RULE_TYPE = "inbound";
	private static final String NETWORK_TYPE_FIXED = "FIXED";
	private static final String NIC_IP_PATH = "VM/NIC/IP";
	private static final String OUTPUT_RULE_TYPE = "outbound";
	private static final String PUBLIC_NETWORK_BRIDGE_KEY = "public_network_bridge";
	private static final String XPATH_EXPRESSION_FORMAT = "//VNET_POOL/VNET/TEMPLATE/LEASES[descendant::IP[text()='%s']]";

	@Override
	public String requestInstance(PublicIpOrder publicIpOrder, String computeInstanceId, CloudUser cloudUser)
			throws FogbowException {

		LOGGER.info(String.format(Messages.Info.REQUESTING_INSTANCE, cloudUser.getToken()));
		Client client = OpenNebulaClientUtil.createClient(getEndpoint(), cloudUser.getToken());

		// Check if fixed IP is in use
		String fixedIp = getAvailableFixedIp(client);
		if (fixedIp == null) {
			LOGGER.error(Messages.Error.FIXED_IP_EXCEEDED);
			throw new QuotaExceededException();
		}
		
		String template = createSecurityGroupsTemplate(publicIpOrder);
		String securityGroupsId = OpenNebulaClientUtil.allocateSecurityGroup(client, template);
		
		template = createPublicNetworkTemplate(publicIpOrder, securityGroupsId, fixedIp);
		String virtualNetworkId = OpenNebulaClientUtil.allocateVirtualNetwork(client, template);

		template = createNicTemplate(virtualNetworkId);
		VirtualMachine virtualMachine = attachNetworkInterfaceConnected(client, computeInstanceId, template);
		String nicId = getNicIdFromContenOf(virtualMachine);

		String instanceId = String.format(INSTANCE_ID, 
				computeInstanceId, 
				virtualNetworkId, 
				securityGroupsId, 
				nicId);
		
		return instanceId;
	}

	@Override
	public void deleteInstance(String publicIpInstanceId, String computeInstanceId, CloudUser cloudUser)
			throws FogbowException {

		LOGGER.info(String.format(Messages.Info.DELETING_INSTANCE, publicIpInstanceId, cloudUser.getToken()));
		Client client = OpenNebulaClientUtil.createClient(getEndpoint(), cloudUser.getToken());

		String[] instanceIds = publicIpInstanceId.split(ID_SEPARATOR);
		String virtualNetworkId = instanceIds[1];
		String securityGroupId = instanceIds[2];
		String nicId = instanceIds[3];

		detachNicFromVirtualMachine(client, computeInstanceId, nicId);
		deletePublicNetwork(client, virtualNetworkId);
		deleteSecurityGroup(client, securityGroupId);
	}

	@Override
	public PublicIpInstance getInstance(String publicIpInstanceId, CloudUser cloudUser) throws FogbowException {

		LOGGER.info(String.format(Messages.Info.GETTING_INSTANCE, publicIpInstanceId, cloudUser.getToken()));
		Client client = OpenNebulaClientUtil.createClient(getEndpoint(), cloudUser.getToken());

		String[] instanceIds = publicIpInstanceId.split(ID_SEPARATOR);
		String virtualMachineId = instanceIds[0];

		VirtualMachine virtualMachine = OpenNebulaClientUtil.getVirtualMachine(client, virtualMachineId);
		String publicIp = virtualMachine.xpath(NIC_IP_PATH);
		InstanceState instanceState = InstanceState.READY;
		
		LOGGER.info(String.format(Messages.Info.MOUNTING_INSTANCE, publicIpInstanceId));
		PublicIpInstance publicIpInstance = new PublicIpInstance(publicIpInstanceId, instanceState, publicIp);
		return publicIpInstance;
	}

	private void deleteSecurityGroup(Client client, String securityGroupId)
			throws UnauthorizedRequestException, InvalidParameterException, InstanceNotFoundException {
		
		SecurityGroup securityGroup = OpenNebulaClientUtil.getSecurityGroup(client, securityGroupId);
		OneResponse response = securityGroup.delete();
		if (response.isError()) {
			String message = response.getErrorMessage();
			LOGGER.error(String.format(Messages.Error.ERROR_MESSAGE, message));
		}
	}

	private void deletePublicNetwork(Client client, String virtualNetworkId)
			throws UnauthorizedRequestException, InstanceNotFoundException, InvalidParameterException {
		
		VirtualNetwork virtualNetwork = OpenNebulaClientUtil.getVirtualNetwork(client, virtualNetworkId);
		OneResponse response = virtualNetwork.delete();
		if (response.isError()) {
			String message = response.getErrorMessage();
			LOGGER.error(String.format(Messages.Error.ERROR_MESSAGE, message));
		}
	}
	
	private void detachNicFromVirtualMachine(Client client, String virtualMachineId, String nicId)
			throws UnauthorizedRequestException, InstanceNotFoundException, InvalidParameterException {
		
		int id = Integer.parseInt(nicId);
		VirtualMachine virtualMachine = OpenNebulaClientUtil.getVirtualMachine(client, virtualMachineId);
		OneResponse response = virtualMachine.nicDetach(id);
		if (response.isError()) {
			String message = response.getErrorMessage();
			LOGGER.error(String.format(Messages.Error.ERROR_MESSAGE, message));
		}
	}
	
	private String createNicTemplate(String virtualNetworkId) {
		String template;
		CreateNicRequest request = new CreateNicRequest.Builder()
				.networkId(virtualNetworkId)
				.build();

		template = request.getNic().marshalTemplate();
		return template;
	}

	private String createPublicNetworkTemplate(PublicIpOrder publicIpOrder, String securityGroupId, String fixedIp) {
		String name = publicIpOrder.getCloudName();
		String type = NETWORK_TYPE_FIXED;
		String bridge = getBridge();
		String bridgedDrive = DEFAULT_VIRTUAL_NETWORK_BRIDGED_DRIVE;
		String securityGroups = DEFAULT_SECURITY_GROUP + CARACTER_SEPARATOR  + securityGroupId;
		
		List<LeaseIp> leases = new ArrayList<>();
		leases.add(PublicNetworkTemplate.allocateIpAddress(fixedIp));
		
		CreatePublicNetworkRequest request = new CreatePublicNetworkRequest.Builder()
				.name(name)
				.type(type)
				.bridge(bridge)
				.bridgedDrive(bridgedDrive)
				.leases(leases)
				.securityGroups(securityGroups)
				.build();
				
		String template = request.getPublicNetwork().marshalTemplate();
		return template;
	}
	
	private String createSecurityGroupsTemplate(PublicIpOrder publicIpOrder)
			throws InvalidParameterException {
		
		String name = SECURITY_GROUP_PREFIX + publicIpOrder.getId();

		// "ALL" setting applies to all protocols if a port range is not defined
		String protocol = ALL_PROTOCOLS;

		// An undefined port range is interpreted by opennebula as all open
		String rangeAll = null;

		// An undefined ip and size is interpreted by opennebula as any network
		String ipAny = null;
		String sizeAny = null;

		// The virtualNetworkId and securityGroupId parameters are not used in this context.
		String virtualNetworkId = null;
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

	private String getNicIdFromContenOf(VirtualMachine virtualMachine) {
		OneResponse response = virtualMachine.info();
		String xml = response.getMessage();
		XmlUnmarshaller xmlUnmarshaller = new XmlUnmarshaller(xml);
		String content = xmlUnmarshaller.getContentOfLastElement(OpenNebulaTagNameConstants.NIC_ID);
		return content;
	}
	
	private VirtualMachine attachNetworkInterfaceConnected(Client client, String computeInstanceId, String template)
			throws UnauthorizedRequestException, InstanceNotFoundException, InvalidParameterException {
		
		VirtualMachine virtualMachine = OpenNebulaClientUtil.getVirtualMachine(client, computeInstanceId);
		OneResponse response = virtualMachine.nicAttach(template);
		if (response.isError()) {
			String message = response.getErrorMessage();
			LOGGER.error(String.format(Messages.Error.ERROR_WHILE_CREATING_NIC, template));
			LOGGER.error(String.format(Messages.Error.ERROR_MESSAGE, message));
			throw new InvalidParameterException();
		}
		return virtualMachine;
	}
	
	protected String getAvailableFixedIp(Client client) {
		String publicIpAddresses = getPublicIpAddresses();
		String[] sliceFixedIp = publicIpAddresses.split(CARACTER_SEPARATOR);
		
		OneResponse response = VirtualNetworkPool.infoAll(client);
		if (response.isError()) {
			LOGGER.error(String.format(Messages.Error.ERROR_MESSAGE, response.getErrorMessage()));
		}
		String xml = response.getMessage();
		XmlUnmarshaller xmlUnmarshaller = new XmlUnmarshaller(xml);
		String content = null;
		String expression = null;
		for (int i = 0; i < sliceFixedIp.length; i++) {
			content = sliceFixedIp[i];
			expression = String.format(XPATH_EXPRESSION_FORMAT, content);
			if (!xmlUnmarshaller.containsExpressionContext(expression)) {
				return content;
			}
		}
		return null;
	}
	
	protected String getPublicIpAddresses() {
		Properties properties = getProperties();
		String addresses = properties.getProperty(FIXED_PUBLIC_IP_ADDRESSES_KEY);
		return addresses;
	}
	
	protected String getBridge() {
		Properties properties = getProperties();
		String bridge = properties.getProperty(PUBLIC_NETWORK_BRIDGE_KEY);
		return bridge;
	}
	
	protected String getEndpoint() {
		Properties properties = getProperties();
		String endpoint = properties.getProperty(OpenNebulaConfigurationPropertyKeys.OPENNEBULA_RPC_ENDPOINT_KEY);
		return endpoint;
	}
	
	protected Properties getProperties() {
		String opennebulaConfFilePath = HomeDir.getPath() 
				+ SystemConstants.CLOUDS_CONFIGURATION_DIRECTORY_NAME
				+ File.separator 
				+ SystemConstants.OPENNEBULA_CLOUD_NAME_DIRECTORY 
				+ File.separator 
				+ SystemConstants.CLOUD_SPECIFICITY_CONF_FILE_NAME;
		
		Properties properties = PropertiesUtil.readProperties(opennebulaConfFilePath);
		return properties;
	}
	
}
