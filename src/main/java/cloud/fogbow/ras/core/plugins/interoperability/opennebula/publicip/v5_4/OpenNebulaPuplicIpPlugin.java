package cloud.fogbow.ras.core.plugins.interoperability.opennebula.publicip.v5_4;

import java.util.ArrayList;
import java.util.List;
import java.util.Properties;
import java.util.UUID;

import org.apache.log4j.Logger;
import org.opennebula.client.Client;
import org.opennebula.client.OneResponse;
import org.opennebula.client.secgroup.SecurityGroup;
import org.opennebula.client.vm.VirtualMachine;
import org.opennebula.client.vnet.VirtualNetwork;

import cloud.fogbow.common.exceptions.FatalErrorException;
import cloud.fogbow.common.exceptions.FogbowException;
import cloud.fogbow.common.exceptions.InstanceNotFoundException;
import cloud.fogbow.common.exceptions.InvalidParameterException;
import cloud.fogbow.common.exceptions.UnauthorizedRequestException;
import cloud.fogbow.common.models.CloudUser;
import cloud.fogbow.common.util.PropertiesUtil;
import cloud.fogbow.common.util.connectivity.cloud.opennebula.OpenNebulaTagNameConstants;
import cloud.fogbow.ras.api.http.response.InstanceState;
import cloud.fogbow.ras.api.http.response.PublicIpInstance;
import cloud.fogbow.ras.constants.Messages;
import cloud.fogbow.ras.core.models.orders.PublicIpOrder;
import cloud.fogbow.ras.core.plugins.interoperability.PublicIpPlugin;
import cloud.fogbow.ras.core.plugins.interoperability.opennebula.OpenNebulaClientUtil;
import cloud.fogbow.ras.core.plugins.interoperability.opennebula.OpenNebulaConfigurationPropertyKeys;
import cloud.fogbow.ras.core.plugins.interoperability.opennebula.XmlUnmarshaller;
import cloud.fogbow.ras.core.plugins.interoperability.opennebula.network.v5_4.CreateNetworkReserveRequest;
import cloud.fogbow.ras.core.plugins.interoperability.opennebula.network.v5_4.CreateNetworkUpdateRequest;
import cloud.fogbow.ras.core.plugins.interoperability.opennebula.securityrule.v5_4.CreateSecurityGroupRequest;
import cloud.fogbow.ras.core.plugins.interoperability.opennebula.securityrule.v5_4.Rule;

public class OpenNebulaPuplicIpPlugin implements PublicIpPlugin<CloudUser> {

	private static final Logger LOGGER = Logger.getLogger(OpenNebulaPuplicIpPlugin.class);

	private static final String ALL_PROTOCOLS = "ALL"; // FIXME occurrence in two classes
	private static final String DEFAULT_PUBLIC_NETWORK_ID_KEY = "default_public_network_id"; // FIXME add in the file conf
	private static final String DEFAULT_SECURITY_GROUP_ID_KEY = "default_security_group_id"; // FIXME occurrence in two classes
	private static final String FOGBOW_PUBLIC_IP_NAME = "ras-public-ip-";
	private static final String ID_SEPARATOR = " ";
	private static final String INPUT_RULE_TYPE = "inbound"; // FIXME occurrence in two classes
	private static final String INSTANCE_ID = "%s %s %s %s";
	private static final String NIC_IP_PATH = "/VM/NIC/IP";
	private static final String OUTPUT_RULE_TYPE = "outbound"; // FIXME occurrence in two classes
	private static final String SECURITY_GROUPS_FORMAT = "%s,%s"; // FIXME occurrence in two classes

	private static final int SIZE_ADDRESS_PUBLIC_IP = 1;

	private String endpoint;
	private String defaultPublicNetwork;
	private String defaultSecurityGroup;
	
	public OpenNebulaPuplicIpPlugin(String confFilePath) throws FatalErrorException {
		Properties properties = PropertiesUtil.readProperties(confFilePath);
		this.endpoint = properties.getProperty(OpenNebulaConfigurationPropertyKeys.OPENNEBULA_RPC_ENDPOINT_KEY);
		this.defaultPublicNetwork = properties.getProperty(DEFAULT_PUBLIC_NETWORK_ID_KEY);
		this.defaultSecurityGroup = properties.getProperty(DEFAULT_SECURITY_GROUP_ID_KEY);
	}
	
	@Override
	public String requestInstance(PublicIpOrder publicIpOrder, String computeInstanceId, CloudUser cloudUser)
			throws FogbowException {

		LOGGER.info(String.format(Messages.Info.REQUESTING_INSTANCE, cloudUser.getToken()));
		Client client = OpenNebulaClientUtil.createClient(this.endpoint, cloudUser.getToken());
		
		int defaultPublicNetworkId = convertToInteger(this.defaultPublicNetwork);
		String name = FOGBOW_PUBLIC_IP_NAME + getRandomUUID();
		int size = SIZE_ADDRESS_PUBLIC_IP;

		CreateNetworkReserveRequest reserveRequest = new CreateNetworkReserveRequest.Builder()
				.name(name)
				.size(size)
				.build();

		String publicNetworkReserveTemplate = reserveRequest.getVirtualNetworkReserved().marshalTemplate();
		String publicNetworkInstanceId = OpenNebulaClientUtil.reserveVirtualNetwork(client, defaultPublicNetworkId,
				publicNetworkReserveTemplate);
		
		String securityGroupInstanceId = createSecurityGroups(client, publicIpOrder);
		String securityGroups = String.format(SECURITY_GROUPS_FORMAT, this.defaultSecurityGroup, securityGroupInstanceId);

		CreateNetworkUpdateRequest updateRequest = new CreateNetworkUpdateRequest.Builder()
				.securityGroups(securityGroups)
				.build();
		
		int virtualNetworkId = convertToInteger(publicNetworkInstanceId);
		String networkUpdateTemplate = updateRequest.getVirtualNetworkUpdate().marshalTemplate();
		OpenNebulaClientUtil.updateVirtualNetwork(client, virtualNetworkId, networkUpdateTemplate);
		
		String template = createNicTemplate(publicNetworkInstanceId);
		VirtualMachine virtualMachine = attachNetworkInterfaceConnected(client, computeInstanceId, template);
		String nicId = getNicIdFromContenOf(virtualMachine);

		String instanceId = String.format(INSTANCE_ID, 
				computeInstanceId, 
				publicNetworkInstanceId, 
				securityGroupInstanceId, 
				nicId);
		
		return instanceId;
	}

	@Override
	public void deleteInstance(String publicIpInstanceId, String computeInstanceId, CloudUser cloudUser)
			throws FogbowException {

		LOGGER.info(String.format(Messages.Info.DELETING_INSTANCE, publicIpInstanceId, cloudUser.getToken()));
		Client client = OpenNebulaClientUtil.createClient(this.endpoint, cloudUser.getToken());

		String[] instanceIds = publicIpInstanceId.split(ID_SEPARATOR);
		String virtualNetworkId = instanceIds[1];
		String securityGroupId = instanceIds[2];
		String nicId = instanceIds[3];

		detachNicFromVirtualMachine(client, computeInstanceId, nicId);
		deleteSecurityGroup(client, securityGroupId);
		deletePublicNetwork(client, virtualNetworkId);
	}

	@Override
	public PublicIpInstance getInstance(String publicIpInstanceId, CloudUser cloudUser) throws FogbowException {

		LOGGER.info(String.format(Messages.Info.GETTING_INSTANCE, publicIpInstanceId, cloudUser.getToken()));
		Client client = OpenNebulaClientUtil.createClient(this.endpoint, cloudUser.getToken());

		String[] instanceIds = publicIpInstanceId.split(ID_SEPARATOR);
		String virtualMachineId = instanceIds[0];

		VirtualMachine virtualMachine = OpenNebulaClientUtil.getVirtualMachine(client, virtualMachineId);
		String publicIp = virtualMachine.xpath(NIC_IP_PATH);
		InstanceState instanceState = InstanceState.READY;
		
		LOGGER.info(String.format(Messages.Info.MOUNTING_INSTANCE, publicIpInstanceId));
		PublicIpInstance publicIpInstance = new PublicIpInstance(publicIpInstanceId, instanceState, publicIp);
		return publicIpInstance;
	}
	
	protected void deletePublicNetwork(Client client, String virtualNetworkId)
			throws UnauthorizedRequestException, InstanceNotFoundException, InvalidParameterException {
		
		VirtualNetwork virtualNetwork = OpenNebulaClientUtil.getVirtualNetwork(client, virtualNetworkId);
		OneResponse response = virtualNetwork.delete();
		if (response.isError()) {
			String message = response.getErrorMessage();
			LOGGER.error(String.format(Messages.Error.ERROR_MESSAGE, message));
		}
	}
	
	protected void deleteSecurityGroup(Client client, String securityGroupId)
			throws UnauthorizedRequestException, InvalidParameterException, InstanceNotFoundException {
		
		SecurityGroup securityGroup = OpenNebulaClientUtil.getSecurityGroup(client, securityGroupId);
		OneResponse response = securityGroup.delete();
		if (response.isError()) {
			String message = response.getErrorMessage();
			LOGGER.error(String.format(Messages.Error.ERROR_MESSAGE, message));
		}
	}
	
	protected void detachNicFromVirtualMachine(Client client, String virtualMachineId, String nicId)
			throws UnauthorizedRequestException, InstanceNotFoundException, InvalidParameterException {
		
		int id = Integer.parseInt(nicId);
		VirtualMachine virtualMachine = OpenNebulaClientUtil.getVirtualMachine(client, virtualMachineId);
		OneResponse response = virtualMachine.nicDetach(id);
		if (response.isError()) {
			String message = response.getErrorMessage();
			LOGGER.error(String.format(Messages.Error.ERROR_MESSAGE, message));
		}
	}

	protected String getNicIdFromContenOf(VirtualMachine virtualMachine) {
		OneResponse response = virtualMachine.info();
		String xml = response.getMessage();
		XmlUnmarshaller xmlUnmarshaller = new XmlUnmarshaller(xml);
		String content = xmlUnmarshaller.getContentOfLastElement(OpenNebulaTagNameConstants.NIC_ID);
		return content;
	}
	
	protected VirtualMachine attachNetworkInterfaceConnected(Client client, String computeInstanceId, String template)
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
	
	protected String createNicTemplate(String virtualNetworkId) {
		String template;
		CreateNicRequest request = new CreateNicRequest.Builder()
				.networkId(virtualNetworkId)
				.build();

		template = request.getNic().marshalTemplate();
		return template;
	}
	
	protected String createSecurityGroups(Client client, PublicIpOrder publicIpOrder) throws InvalidParameterException {
		String name = SECURITY_GROUP_PREFIX + publicIpOrder.getId();

		// "ALL" setting applies to all protocols if a port range is not defined
		String protocol = ALL_PROTOCOLS;

		// An undefined port range is interpreted by opennebula as all open
		String rangeAll = null;

		// An undefined ip and size is interpreted by opennebula as any network
		String ipAny = null;
		String sizeAny = null;

		// The virtualNetworkId and securityGroupId parameters are not used in this
		// context.
		String virtualNetworkId = null;
		String securityGroupId = null;

		Rule inputRule = new Rule(protocol, ipAny, sizeAny, rangeAll, INPUT_RULE_TYPE, virtualNetworkId,
				securityGroupId);
		Rule outputRule = new Rule(protocol, ipAny, sizeAny, rangeAll, OUTPUT_RULE_TYPE, virtualNetworkId,
				securityGroupId);

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

	protected String getRandomUUID() {
		return UUID.randomUUID().toString();
	}

	protected int convertToInteger(String number) throws InvalidParameterException {
		try {
			return Integer.parseInt(number);
		} catch (NumberFormatException e) {
			LOGGER.error(String.format(Messages.Error.ERROR_WHILE_CONVERTING_TO_INTEGER), e);
			throw new InvalidParameterException();
		}
	}
	
}
