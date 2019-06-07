package cloud.fogbow.ras.core.plugins.interoperability.aws.network.v2;

import java.util.Arrays;
import java.util.List;
import java.util.Properties;
import java.util.UUID;

import org.apache.log4j.Logger;

import cloud.fogbow.common.exceptions.FogbowException;
import cloud.fogbow.common.exceptions.InstanceNotFoundException;
import cloud.fogbow.common.exceptions.UnexpectedException;
import cloud.fogbow.common.models.AwsV2User;
import cloud.fogbow.common.util.PropertiesUtil;
import cloud.fogbow.ras.api.http.response.InstanceState;
import cloud.fogbow.ras.api.http.response.NetworkInstance;
import cloud.fogbow.ras.constants.Messages;
import cloud.fogbow.ras.constants.SystemConstants;
import cloud.fogbow.ras.core.models.NetworkAllocationMode;
import cloud.fogbow.ras.core.models.ResourceType;
import cloud.fogbow.ras.core.models.orders.NetworkOrder;
import cloud.fogbow.ras.core.plugins.interoperability.NetworkPlugin;
import cloud.fogbow.ras.core.plugins.interoperability.aws.AwsV2ClientUtil;
import cloud.fogbow.ras.core.plugins.interoperability.aws.AwsV2ConfigurationPropertyKeys;
import cloud.fogbow.ras.core.plugins.interoperability.aws.AwsV2StateMapper;
import software.amazon.awssdk.services.ec2.Ec2Client;
import software.amazon.awssdk.services.ec2.model.AttachInternetGatewayRequest;
import software.amazon.awssdk.services.ec2.model.AttributeBooleanValue;
import software.amazon.awssdk.services.ec2.model.AuthorizeSecurityGroupIngressRequest;
import software.amazon.awssdk.services.ec2.model.CreateInternetGatewayResponse;
import software.amazon.awssdk.services.ec2.model.CreateRouteRequest;
import software.amazon.awssdk.services.ec2.model.CreateSecurityGroupRequest;
import software.amazon.awssdk.services.ec2.model.CreateSecurityGroupResponse;
import software.amazon.awssdk.services.ec2.model.CreateSubnetRequest;
import software.amazon.awssdk.services.ec2.model.CreateSubnetResponse;
import software.amazon.awssdk.services.ec2.model.CreateTagsRequest;
import software.amazon.awssdk.services.ec2.model.CreateVpcRequest;
import software.amazon.awssdk.services.ec2.model.CreateVpcResponse;
import software.amazon.awssdk.services.ec2.model.DeleteInternetGatewayRequest;
import software.amazon.awssdk.services.ec2.model.DeleteSecurityGroupRequest;
import software.amazon.awssdk.services.ec2.model.DeleteSubnetRequest;
import software.amazon.awssdk.services.ec2.model.DeleteVpcRequest;
import software.amazon.awssdk.services.ec2.model.DescribeInternetGatewaysResponse;
import software.amazon.awssdk.services.ec2.model.DescribeRouteTablesResponse;
import software.amazon.awssdk.services.ec2.model.DescribeSubnetsRequest;
import software.amazon.awssdk.services.ec2.model.DescribeSubnetsResponse;
import software.amazon.awssdk.services.ec2.model.DescribeVpcsRequest;
import software.amazon.awssdk.services.ec2.model.DescribeVpcsResponse;
import software.amazon.awssdk.services.ec2.model.DetachInternetGatewayRequest;
import software.amazon.awssdk.services.ec2.model.InternetGateway;
import software.amazon.awssdk.services.ec2.model.InternetGatewayAttachment;
import software.amazon.awssdk.services.ec2.model.ModifyVpcAttributeRequest;
import software.amazon.awssdk.services.ec2.model.RouteTable;
import software.amazon.awssdk.services.ec2.model.Subnet;
import software.amazon.awssdk.services.ec2.model.Tag;
import software.amazon.awssdk.services.ec2.model.Vpc;

public class AwsV2NetworkPlugin implements NetworkPlugin<AwsV2User> {

	private static final Logger LOGGER = Logger.getLogger(AwsV2NetworkPlugin.class);

	private static final String ALL_PROTOCOLS = "-1";
	private static final String AWS_TAG_GROUP_ID = "groupId";
	private static final String AWS_TAG_NAME = "Name";
	private static final String DEFAULT_DESTINATION_CIDR = "0.0.0.0/0";
	private static final String GATEWAY_RESOURCE = "Gateway";
	private static final String SECURITY_GROUP_DESCRIPTION = "Security group associated with a fogbow network.";
	private static final String SECURITY_GROUP_RESOURCE = "Security Group";
	private static final String SUBNET_RESOURCE = "Subnet";
	private static final String VPC_RESOURCE = "VPC";

	private String region;

	public AwsV2NetworkPlugin(String confFilePath) {
		Properties properties = PropertiesUtil.readProperties(confFilePath);
		this.region = properties.getProperty(AwsV2ConfigurationPropertyKeys.AWS_REGION_SELECTION_KEY);
	}

	@Override
	public String requestInstance(NetworkOrder networkOrder, AwsV2User cloudUser) throws FogbowException {
		LOGGER.info(String.format(Messages.Info.REQUESTING_INSTANCE, cloudUser.getToken()));

		Ec2Client client = AwsV2ClientUtil.createEc2Client(cloudUser.getToken(), this.region);
		String cidr = networkOrder.getCidr();
		String name = defineInstanceName(networkOrder.getName());
		String vpcId = doCreateVpcRequests(cidr, client);
		doCreateSecurityGroupRequests(cidr, vpcId, client);
		doModifyVpcAttributesRequests(vpcId, client);
		String gatewayId = doCreateInternetGatewayRequests(vpcId, client);
		doAttachInternetGatewayRequests(gatewayId, vpcId, client);
		doCreateRouteRequests(cidr, gatewayId, vpcId, client);
		return doCreateSubnetResquests(name, cidr, vpcId, client);
	}

	@Override
	public NetworkInstance getInstance(NetworkOrder networkOrder, AwsV2User cloudUser) throws FogbowException {
		LOGGER.info(String.format(Messages.Info.GETTING_INSTANCE, networkOrder.getInstanceId(), cloudUser.getToken()));

		String subnetId = networkOrder.getInstanceId();
		Ec2Client client = AwsV2ClientUtil.createEc2Client(cloudUser.getToken(), this.region);
		Subnet subnet = getSubnetById(subnetId, client);
		RouteTable routeTable = getRouteTableByVpc(subnet.vpcId(), client);
		return mountNetworkInstance(subnet, routeTable);
	}

	@Override
	public void deleteInstance(NetworkOrder networkOrder, AwsV2User cloudUser) throws FogbowException {
		LOGGER.info(String.format(Messages.Info.DELETING_INSTANCE, networkOrder.getInstanceId(), cloudUser.getToken()));

		String subnetId = networkOrder.getInstanceId();
		Ec2Client client = AwsV2ClientUtil.createEc2Client(cloudUser.getToken(), this.region);
		Subnet subnet = getSubnetById(subnetId, client);
		String groupId = getGroupIdByVpc(subnet.vpcId(), client);
		if (groupId != null) {
			doDeleteSecurityGroupRequests(groupId, client);
		} 
		String gatewayId = getGatewayIdBySubnet(subnet.vpcId(), client);
		if (gatewayId != null) {
			doDetachInternetGatewayRequests(gatewayId, subnet.vpcId(), client);
			doDeleteInternetGatewayRequests(gatewayId, client);
		}
		doDeleteSubnetRequests(subnet.vpcId(), subnetId, client);
	}

	protected String getGroupIdByVpc(String vpcId, Ec2Client client)
			throws UnexpectedException, InstanceNotFoundException {
		
		Vpc vpc = doDescribeVpcsRequests(vpcId, client);
		for (Tag tag : vpc.tags()) {
			if (tag.key().equals(AWS_TAG_GROUP_ID)) {
				return tag.value();
			}
		}
		throw new InstanceNotFoundException(Messages.Exception.INSTANCE_NOT_FOUND);
	}

	protected Vpc doDescribeVpcsRequests(String vpcId, Ec2Client client) throws UnexpectedException {
		DescribeVpcsRequest request = DescribeVpcsRequest.builder()
				.vpcIds(vpcId)
				.build();
		try {
			DescribeVpcsResponse response = client.describeVpcs(request);
			return response.vpcs().listIterator().next();
		} catch (Exception e) {
			throw new UnexpectedException(String.format(Messages.Exception.GENERIC_EXCEPTION, e), e);
		}
	}

	@Override
	public boolean isReady(String instanceState) {
		return AwsV2StateMapper.map(ResourceType.NETWORK, instanceState).equals(InstanceState.READY);
	}

	@Override
	public boolean hasFailed(String instanceState) {
		return false;
	}
	
	protected void doDeleteSubnetRequests(String vpcId, String subnetId, Ec2Client client) throws UnexpectedException {
		DeleteSubnetRequest request = DeleteSubnetRequest.builder()
				.subnetId(subnetId)
				.build();
		try {
			client.deleteSubnet(request);
			doDeleteVpcRequests(vpcId, client);
		} catch (Exception e) {
			LOGGER.error(String.format(Messages.Error.ERROR_WHILE_REMOVING_RESOURCE, SUBNET_RESOURCE, subnetId), e);
			throw new UnexpectedException();
		}
	}
	
	protected NetworkInstance mountNetworkInstance(Subnet subnet, RouteTable routeTable) throws UnexpectedException {
		String id = subnet.subnetId();
		String cloudState = subnet.stateAsString();
		String name = subnet.tags().listIterator().next().value();
		String cidr = subnet.cidrBlock();
		String gateway = routeTable.routes().listIterator().next().destinationCidrBlock();
		String vLAN = null;
		String networkInterface = null;
		String macInterface = null;
		String interfaceState = null;
		NetworkAllocationMode networkAllocationMode = NetworkAllocationMode.DYNAMIC;
		return new NetworkInstance(id, cloudState, name, cidr, gateway, vLAN, networkAllocationMode, networkInterface,
				macInterface, interfaceState);
	}

	protected DescribeSubnetsResponse doDescribeSubnetsRequests(String subnetId, Ec2Client client)
			throws UnexpectedException {

		DescribeSubnetsRequest request = DescribeSubnetsRequest.builder()
				.subnetIds(subnetId)
				.build();
		try {
			return client.describeSubnets(request);
		} catch (Exception e) {
			throw new UnexpectedException(String.format(Messages.Exception.GENERIC_EXCEPTION, e), e);
		}
	}
	
	protected String doCreateSubnetResquests(String name, String cidr, String vpcId, Ec2Client client) throws UnexpectedException {
		CreateSubnetRequest request = CreateSubnetRequest.builder()
				.cidrBlock(cidr)
				.vpcId(vpcId)
				.build();
		try {
			CreateSubnetResponse response = client.createSubnet(request);
			String subnetId = response.subnet().subnetId();
			doCreateTagsRequests(AWS_TAG_NAME, name, subnetId, client);
			return subnetId;
		} catch (Exception e) {
			doDeleteVpcRequests(vpcId, client);
			throw new UnexpectedException(String.format(Messages.Exception.GENERIC_EXCEPTION, e), e);
		}
	}

	protected void doCreateRouteRequests(String cidr, String gatewayId, String vpcId, Ec2Client client)
			throws UnexpectedException, InstanceNotFoundException {
		
		RouteTable routeTable = getRouteTableByVpc(vpcId, client);
		String routerId = routeTable.routeTableId();
		CreateRouteRequest request = CreateRouteRequest.builder()
				.destinationCidrBlock(DEFAULT_DESTINATION_CIDR)
				.gatewayId(gatewayId)
				.routeTableId(routerId)
				.build();
		try {
			client.createRoute(request);
		} catch (Exception e) {
			doDetachInternetGatewayRequests(gatewayId, vpcId, client);
			doDeleteInternetGatewayRequests(gatewayId, client);
			doDeleteVpcRequests(vpcId, client);
			throw new UnexpectedException(String.format(Messages.Exception.GENERIC_EXCEPTION, e), e);
		}
	}
	
	protected Subnet getSubnetById(String subnetId, Ec2Client client) throws UnexpectedException, InstanceNotFoundException {
		DescribeSubnetsResponse response = doDescribeSubnetsRequests(subnetId, client);
		if (response != null && !response.subnets().isEmpty()) {
			return response.subnets().listIterator().next();
		}
		throw new InstanceNotFoundException(Messages.Exception.INSTANCE_NOT_FOUND);
	}

	protected RouteTable getRouteTableByVpc(String vpcId, Ec2Client client)
			throws UnexpectedException, InstanceNotFoundException {
		
		DescribeRouteTablesResponse response = doDescribeRouteTablesRequests(client);
		List<RouteTable> routeTables = response.routeTables();
		for (RouteTable routeTable : routeTables) {
			if (routeTable.vpcId().equals(vpcId)) {
				return routeTable;
			}
		}
		throw new InstanceNotFoundException(Messages.Exception.INSTANCE_NOT_FOUND);
	}

	protected DescribeRouteTablesResponse doDescribeRouteTablesRequests(Ec2Client client) throws UnexpectedException {
		try {
			return client.describeRouteTables();
		} catch (Exception e) {
			throw new UnexpectedException(String.format(Messages.Exception.GENERIC_EXCEPTION, e), e);
		}
	}

	protected void doAttachInternetGatewayRequests(String gatewayId, String vpcId, Ec2Client client)
			throws UnexpectedException {
		
		AttachInternetGatewayRequest request = AttachInternetGatewayRequest.builder()
				.internetGatewayId(gatewayId)
				.vpcId(vpcId)
				.build();
		try {
			client.attachInternetGateway(request);
		} catch (Exception e) {
			doDeleteInternetGatewayRequests(gatewayId, client);
			doDeleteVpcRequests(vpcId, client);
			throw new UnexpectedException(String.format(Messages.Exception.GENERIC_EXCEPTION, e), e);
		}
	}

	protected void doDeleteInternetGatewayRequests(String gatewayId, Ec2Client client) throws UnexpectedException {
		DeleteInternetGatewayRequest request = DeleteInternetGatewayRequest.builder()
				.internetGatewayId(gatewayId)
				.build();
		try {
			client.deleteInternetGateway(request);
		} catch (Exception e) {
			LOGGER.error(String.format(Messages.Error.ERROR_WHILE_REMOVING_RESOURCE, GATEWAY_RESOURCE, gatewayId), e);
			throw new UnexpectedException();
		}
	}

	protected void doDetachInternetGatewayRequests(String gatewayId, String vpcId, Ec2Client client)
			throws UnexpectedException {

		DetachInternetGatewayRequest request = DetachInternetGatewayRequest.builder()
				.internetGatewayId(gatewayId)
				.vpcId(vpcId)
				.build();
		try {
			client.detachInternetGateway(request);
		} catch (Exception e) {
			throw new UnexpectedException(String.format(Messages.Exception.GENERIC_EXCEPTION, e), e);
		}
	}

	protected String getGatewayIdBySubnet(String vpcId, Ec2Client client)
			throws UnexpectedException, InstanceNotFoundException {
		
		DescribeInternetGatewaysResponse response = client.describeInternetGateways();
		for (InternetGateway internetGateway : response.internetGateways()) {
			for (InternetGatewayAttachment attachment : internetGateway.attachments()) {
				if (attachment.vpcId().equals(vpcId)) {
					return internetGateway.internetGatewayId();
				}
			}
		}
		throw new InstanceNotFoundException(Messages.Exception.INSTANCE_NOT_FOUND);
	}

	protected String doCreateInternetGatewayRequests(String vpcId, Ec2Client client)
			throws UnexpectedException {
		try {
			CreateInternetGatewayResponse response = client.createInternetGateway();
			return response.internetGateway().internetGatewayId();
		} catch (Exception e) {
			doDeleteVpcRequests(vpcId, client);
			throw new UnexpectedException(String.format(Messages.Exception.GENERIC_EXCEPTION, e), e);
		}
	}
	
	protected void doModifyVpcAttributesRequests(String vpcId, Ec2Client client)
			throws UnexpectedException {

		ModifyVpcAttributeRequest[] modifyVpcAttributes = { doEnableDnsHostnamesRequest(vpcId),
				doEnableDnsSupportRequest(vpcId) };
		/*
		 * AWS does not allow two attributes of a VPC to be modified in a single
		 * request. For this, you must send a request for each modification you make.
		 */
		List<ModifyVpcAttributeRequest> requests = Arrays.asList(modifyVpcAttributes);
		for (ModifyVpcAttributeRequest request : requests) {
			try {
				client.modifyVpcAttribute(request);
			} catch (Exception e) {
				doDeleteVpcRequests(vpcId, client);
				throw new UnexpectedException(String.format(Messages.Exception.GENERIC_EXCEPTION, e), e);
			}
		}
	}
	
	protected ModifyVpcAttributeRequest doEnableDnsHostnamesRequest(String vpcId) {
		AttributeBooleanValue enableDnsHostnames = AttributeBooleanValue.builder()
				.value(true)
				.build();

		return ModifyVpcAttributeRequest.builder()
				.vpcId(vpcId)
				.enableDnsHostnames(enableDnsHostnames)
				.build();
	}
	
	protected ModifyVpcAttributeRequest doEnableDnsSupportRequest(String vpcId) {
		AttributeBooleanValue enableDnsSupport = AttributeBooleanValue.builder()
				.value(true)
				.build();

		return ModifyVpcAttributeRequest.builder()
				.vpcId(vpcId)
				.enableDnsSupport(enableDnsSupport)
				.build();
	}

	protected void doCreateSecurityGroupRequests(String cidr, String vpcId, Ec2Client client) throws UnexpectedException {
		String groupName = SystemConstants.PN_SECURITY_GROUP_PREFIX + vpcId;
		CreateSecurityGroupRequest request = CreateSecurityGroupRequest.builder()
                .description(SECURITY_GROUP_DESCRIPTION)
                .groupName(groupName)
                .vpcId(vpcId)
                .build();
		try {
			CreateSecurityGroupResponse response = client.createSecurityGroup(request);
			String groupId = response.groupId();
			doAuthorizeSecurityGroupIngressRequests(cidr, groupId, vpcId, client);
			doCreateTagsRequests(AWS_TAG_GROUP_ID, groupId, vpcId, client);
		} catch (Exception e) {
			doDeleteVpcRequests(vpcId, client);
			throw new UnexpectedException(String.format(Messages.Exception.GENERIC_EXCEPTION, e), e);
		}
	}
	
	protected void doCreateTagsRequests(String key, String value, String resourceId, Ec2Client client) throws UnexpectedException {
		Tag tag = Tag.builder()
				.key(key)
				.value(value)
				.build();

		CreateTagsRequest request = CreateTagsRequest.builder()
				.resources(resourceId)
				.tags(tag)
				.build();
		try {
			client.createTags(request);
		} catch (Exception e) {
			throw new UnexpectedException(String.format(Messages.Exception.GENERIC_EXCEPTION, e), e);
		}
	}

	protected void doAuthorizeSecurityGroupIngressRequests(String cidr, String groupId, String vpcId, Ec2Client client) throws UnexpectedException {
		AuthorizeSecurityGroupIngressRequest authorizeSecurityGroupIngressRequest = AuthorizeSecurityGroupIngressRequest.builder()
                .cidrIp(cidr)
                .groupId(groupId)
                .ipProtocol(ALL_PROTOCOLS)
                .build();
		try {
			client.authorizeSecurityGroupIngress(authorizeSecurityGroupIngressRequest);
		} catch (Exception e) {
			doDeleteSecurityGroupRequests(groupId, client);
			doDeleteVpcRequests(vpcId, client);
			throw new UnexpectedException(String.format(Messages.Exception.GENERIC_EXCEPTION, e), e);
		}
	}
	
	protected void doDeleteVpcRequests(String vpcId, Ec2Client client) throws UnexpectedException {
		DeleteVpcRequest request = DeleteVpcRequest.builder()
				.vpcId(vpcId)
				.build();
		try {
			client.deleteVpc(request);
		} catch (Exception e) {
			LOGGER.error(String.format(Messages.Error.ERROR_WHILE_REMOVING_RESOURCE, VPC_RESOURCE, vpcId), e);
			throw new UnexpectedException();
		}
	}

	protected void doDeleteSecurityGroupRequests(String groupId, Ec2Client client) throws UnexpectedException {
		DeleteSecurityGroupRequest request = DeleteSecurityGroupRequest.builder()
				.groupId(groupId)
				.build();
		try {
			client.deleteSecurityGroup(request);
		} catch (Exception e) {
			LOGGER.error(String.format(Messages.Error.ERROR_WHILE_REMOVING_RESOURCE, SECURITY_GROUP_RESOURCE, groupId), e);
			throw new UnexpectedException();
		}
	}

	protected String doCreateVpcRequests(String cidr, Ec2Client client) throws UnexpectedException {
		CreateVpcRequest request = CreateVpcRequest.builder()
				.cidrBlock(cidr)
				.build();
		try {
			CreateVpcResponse response = client.createVpc(request);
			return response.vpc().vpcId();
		} catch (Exception e) {
			throw new UnexpectedException(String.format(Messages.Exception.GENERIC_EXCEPTION, e), e);
		}
	}
	
	protected String defineInstanceName(String instanceName) {
		return instanceName == null ? SystemConstants.FOGBOW_INSTANCE_NAME_PREFIX + getRandomUUID() : instanceName;
	}

	protected String getRandomUUID() {
		return UUID.randomUUID().toString();
	}
	
}
