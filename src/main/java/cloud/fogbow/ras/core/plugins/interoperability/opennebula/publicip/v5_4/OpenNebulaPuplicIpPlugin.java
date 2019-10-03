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
import cloud.fogbow.common.exceptions.UnexpectedException;
import cloud.fogbow.common.models.CloudUser;
import cloud.fogbow.common.util.PropertiesUtil;
import cloud.fogbow.ras.api.http.response.InstanceState;
import cloud.fogbow.ras.api.http.response.PublicIpInstance;
import cloud.fogbow.ras.constants.Messages;
import cloud.fogbow.ras.constants.SystemConstants;
import cloud.fogbow.ras.core.models.ResourceType;
import cloud.fogbow.ras.core.models.orders.PublicIpOrder;
import cloud.fogbow.ras.core.plugins.interoperability.PublicIpPlugin;
import cloud.fogbow.ras.core.plugins.interoperability.opennebula.OpenNebulaClientUtil;
import cloud.fogbow.ras.core.plugins.interoperability.opennebula.OpenNebulaConfigurationPropertyKeys;
import cloud.fogbow.ras.core.plugins.interoperability.opennebula.OpenNebulaStateMapper;
import cloud.fogbow.ras.core.plugins.interoperability.opennebula.network.v5_4.CreateNetworkReserveRequest;
import cloud.fogbow.ras.core.plugins.interoperability.opennebula.network.v5_4.CreateNetworkUpdateRequest;
import cloud.fogbow.ras.core.plugins.interoperability.opennebula.securityrule.v5_4.CreateSecurityGroupRequest;
import cloud.fogbow.ras.core.plugins.interoperability.opennebula.securityrule.v5_4.CreateSecurityGroupTemplate;
import cloud.fogbow.ras.core.plugins.interoperability.opennebula.securityrule.v5_4.Rule;
import cloud.fogbow.ras.core.plugins.interoperability.opennebula.securityrule.v5_4.SecurityRuleRequest;

public class OpenNebulaPuplicIpPlugin implements PublicIpPlugin<CloudUser> {

	private static final Logger LOGGER = Logger.getLogger(OpenNebulaPuplicIpPlugin.class);

	private static final String ALL_PROTOCOLS = "ALL";
	private static final String INPUT_RULE_TYPE = "inbound";
	private static final String OUTPUT_RULE_TYPE = "outbound";
	private static final String SECURITY_GROUP_SEPARATOR = ",";
	private static final String PUBLIC_IP_RESOURCE = "Public IP";

	private static final int SIZE_ADDRESS_PUBLIC_IP = 1;
	private static final int ATTEMPTS_LIMIT_NUMBER = 5;

	protected static final String EXPRESSION_IP_FROM_NETWORK = "//NIC[NETWORK_ID = %s]/IP/text()";
	protected static final String EXPRESSION_NIC_ID_FROM_NETWORK = "//NIC[NETWORK_ID = %s]/NIC_ID/text()";
	protected static final String EXPRESSION_SECURITY_GROUPS_FROM_NIC_ID = "//NIC[NIC_ID = %s]/SECURITY_GROUPS/text()";
	protected static final String VNET_TEMPLATE_SECURITY_GROUPS_PATH = "/VNET/TEMPLATE/SECURITY_GROUPS";
	protected static final String POWEROFF_STATE = "POWEROFF";
	
	protected static final long ONE_POINT_TWO_SECONDS = 1200;


	private String endpoint;
	private String defaultPublicNetwork;

	public OpenNebulaPuplicIpPlugin(String confFilePath) throws FatalErrorException {
		Properties properties = PropertiesUtil.readProperties(confFilePath);
		this.endpoint = properties.getProperty(OpenNebulaConfigurationPropertyKeys.OPENNEBULA_RPC_ENDPOINT_KEY);
		this.defaultPublicNetwork = properties.getProperty(OpenNebulaConfigurationPropertyKeys.DEFAULT_PUBLIC_NETWORK_ID_KEY);
	}

	@Override
	public boolean isReady(String cloudState) {
		return OpenNebulaStateMapper.map(ResourceType.PUBLIC_IP, cloudState).equals(InstanceState.READY);
	}

	@Override
	public boolean hasFailed(String cloudState) {
		return OpenNebulaStateMapper.map(ResourceType.PUBLIC_IP, cloudState).equals(InstanceState.FAILED);
	}

	@Override
	public String requestInstance(PublicIpOrder publicIpOrder, CloudUser cloudUser) throws FogbowException {
		Client client = OpenNebulaClientUtil.createClient(this.endpoint, cloudUser.getToken());

		int defaultPublicNetworkId = convertToInteger(this.defaultPublicNetwork);
		String name = SystemConstants.FOGBOW_INSTANCE_NAME_PREFIX + getRandomUUID();
		int size = SIZE_ADDRESS_PUBLIC_IP;

		CreateNetworkReserveRequest reserveRequest = new CreateNetworkReserveRequest.Builder()
				.name(name)
				.size(size)
				.build();

		String publicNetworkReserveTemplate = reserveRequest.getVirtualNetworkReserved().marshalTemplate();
		String publicIpInstanceId = OpenNebulaClientUtil.reserveVirtualNetwork(client, defaultPublicNetworkId,
				publicNetworkReserveTemplate);

		String securityGroupInstanceId = createSecurityGroup(client, publicIpOrder);
		CreateNetworkUpdateRequest updateSecurityGroupsRequest = new CreateNetworkUpdateRequest.Builder()
				.securityGroups(securityGroupInstanceId)
				.build();

		int virtualNetworkId = convertToInteger(publicIpInstanceId);
		String publicNetworkUpdateTemplate = updateSecurityGroupsRequest.getVirtualNetworkUpdate().marshalTemplate();
		OpenNebulaClientUtil.updateVirtualNetwork(client, virtualNetworkId, publicNetworkUpdateTemplate);
		String computeInstanceId = publicIpOrder.getComputeId();

		String template = createNicTemplate(publicIpInstanceId);
		attachNetworkInterfaceConnected(client, computeInstanceId, template);
		return publicIpInstanceId;
	}

	@Override
	public void deleteInstance(PublicIpOrder publicIpOrder, CloudUser cloudUser) throws FogbowException {
		String computeInstanceId = publicIpOrder.getComputeId();
		String publicIpInstanceId = publicIpOrder.getInstanceId();
		
		Client client = OpenNebulaClientUtil.createClient(this.endpoint, cloudUser.getToken());
		VirtualMachine virtualMachine = OpenNebulaClientUtil.getVirtualMachine(client, computeInstanceId);
		String nicId = virtualMachine.xpath(String.format(EXPRESSION_NIC_ID_FROM_NETWORK, publicIpInstanceId));

		VirtualNetwork virtualNetwork = OpenNebulaClientUtil.getVirtualNetwork(client, publicIpInstanceId);
		String securityGroupId = null;
		SecurityGroup securityGroup = this.getSecurityGroupForPublicIpNetwork(client, virtualNetwork, publicIpInstanceId);
		if (securityGroup != null) securityGroupId = securityGroup.getId();
		else throw new UnexpectedException();

		// NOTE(pauloewerton): ONe does not allow deleting a resource associated to a VM, so we're using a workaround
		// by shutting down the VM, releasing network and secgroup resources, and then resuming it afterwards.
		virtualMachine.poweroff(true);
		if (this.isPowerOff(virtualMachine)) {
			detachNetworkInterfaceConnected(virtualMachine, nicId);
			deleteSecurityGroup(client, securityGroupId);
			deletePublicNetwork(client, publicIpInstanceId);
			virtualMachine.resume();
		} else {
			LOGGER.error(String.format(Messages.Error.ERROR_WHILE_REMOVING_RESOURCE, PUBLIC_IP_RESOURCE, publicIpInstanceId));
			throw new UnexpectedException();
		}
	}

	@Override
	public PublicIpInstance getInstance(PublicIpOrder publicIpOrder, CloudUser cloudUser) throws FogbowException {
		String virtualMachineId = publicIpOrder.getComputeId();
		String publicIpInstanceId = publicIpOrder.getInstanceId();

		Client client = OpenNebulaClientUtil.createClient(this.endpoint, cloudUser.getToken());
		VirtualMachine virtualMachine = OpenNebulaClientUtil.getVirtualMachine(client, virtualMachineId);
		String publicIp = virtualMachine.xpath(String.format(EXPRESSION_IP_FROM_NETWORK, publicIpInstanceId));

		LOGGER.info(String.format(Messages.Info.MOUNTING_INSTANCE, publicIpInstanceId));
		PublicIpInstance publicIpInstance = new PublicIpInstance(publicIpInstanceId, OpenNebulaStateMapper.DEFAULT_READY_STATE, publicIp);
		return publicIpInstance;
	}

	protected SecurityGroup getSecurityGroupForPublicIpNetwork(Client client, VirtualNetwork virtualNetwork, String orderId)
			throws UnauthorizedRequestException, InstanceNotFoundException, InvalidParameterException {
		String securityGroupIdsStr = virtualNetwork.xpath(VNET_TEMPLATE_SECURITY_GROUPS_PATH);
		if (securityGroupIdsStr == null || securityGroupIdsStr.isEmpty()) {
			LOGGER.warn(Messages.Error.CONTENT_SECURITY_GROUP_NOT_DEFINED);
			return null;
		}

		String[] securityGroupIds =  securityGroupIdsStr.split(SECURITY_GROUP_SEPARATOR);
		String securityGroupName = this.generateSecurityGroupName(orderId);
		SecurityGroup securityGroup = null;
		for (String securityGroupId : securityGroupIds) {
			securityGroup = OpenNebulaClientUtil.getSecurityGroup(client, securityGroupId);
			if (securityGroup.getName().equals(securityGroupName)) break;
		}

		return securityGroup;
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

	protected void detachNetworkInterfaceConnected(VirtualMachine virtualMachine, String nicId)
			throws InvalidParameterException {
		
		int id = convertToInteger(nicId);
		OneResponse response = virtualMachine.nicDetach(id);
		if (response.isError()) {
			String message = response.getErrorMessage();
			LOGGER.error(String.format(Messages.Error.ERROR_MESSAGE, message));
		}
	}
	
	protected boolean isPowerOff(VirtualMachine virtualMachine) {
		String state;
		int count = 0;
		while (count < ATTEMPTS_LIMIT_NUMBER) {
			count++;
			waitMoment();
			virtualMachine.info();
			state = virtualMachine.stateStr();
			if (state.equalsIgnoreCase(POWEROFF_STATE)) {
				return true;
			}
		}
		return false;
	}

	protected void waitMoment() {
		try {
			Thread.sleep(ONE_POINT_TWO_SECONDS);
		} catch (InterruptedException e) {
			LOGGER.error(String.format(Messages.Error.ERROR_MESSAGE, e), e);
		}
	}
	
	protected void attachNetworkInterfaceConnected(Client client, String computeInstanceId, String template)
			throws UnauthorizedRequestException, InstanceNotFoundException, InvalidParameterException {

		VirtualMachine virtualMachine = OpenNebulaClientUtil.getVirtualMachine(client, computeInstanceId);
		OneResponse response = virtualMachine.nicAttach(template);
		if (response.isError()) {
			String message = response.getErrorMessage();
			LOGGER.error(String.format(Messages.Error.ERROR_WHILE_CREATING_NIC, template));
			LOGGER.error(String.format(Messages.Error.ERROR_MESSAGE, message));
			throw new InvalidParameterException();
		}
	}
	
	protected String createNicTemplate(String virtualNetworkId) {
		String template;
		CreateNicRequest request = new CreateNicRequest.Builder()
				.networkId(virtualNetworkId)
				.build();

		template = request.getNic().marshalTemplate();
		return template;
	}
	
	protected String createSecurityGroup(Client client, PublicIpOrder publicIpOrder) throws InvalidParameterException {
		String name = generateSecurityGroupName(publicIpOrder.getId());

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
		
		SecurityRuleRequest inputRule = SecurityRuleRequest.builder()
		        .protocol(protocol)
		        .ip(ipAny)
		        .size(sizeAny)
		        .range(rangeAll)
		        .type(INPUT_RULE_TYPE)
		        .networkId(virtualNetworkId)
		        .groupId(securityGroupId)
		        .build();
		
		SecurityRuleRequest outputRule = SecurityRuleRequest.builder()
		        .protocol(protocol)
		        .ip(ipAny)
		        .size(sizeAny)
		        .range(rangeAll)
		        .type(OUTPUT_RULE_TYPE)
		        .networkId(virtualNetworkId)
		        .groupId(securityGroupId)
		        .build();

		List<SecurityRuleRequest> rules = new ArrayList<>();
		rules.add(inputRule);
		rules.add(outputRule);
		
		CreateSecurityGroupTemplate request = CreateSecurityGroupTemplate.builder()
				.name(name)
				.rules(rules)
				.build();

		String template = request.marshalTemplate();
		return OpenNebulaClientUtil.allocateSecurityGroup(client, template);
	}

	private static String generateSecurityGroupName(String orderId) {
		return SystemConstants.PIP_SECURITY_GROUP_PREFIX + orderId;
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
