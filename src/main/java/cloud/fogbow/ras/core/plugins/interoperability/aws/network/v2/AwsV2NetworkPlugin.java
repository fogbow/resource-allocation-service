package cloud.fogbow.ras.core.plugins.interoperability.aws.network.v2;

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
import software.amazon.awssdk.services.ec2.model.AssociateRouteTableRequest;
import software.amazon.awssdk.services.ec2.model.AuthorizeSecurityGroupIngressRequest;
import software.amazon.awssdk.services.ec2.model.CreateSecurityGroupRequest;
import software.amazon.awssdk.services.ec2.model.CreateSecurityGroupResponse;
import software.amazon.awssdk.services.ec2.model.CreateSubnetRequest;
import software.amazon.awssdk.services.ec2.model.CreateSubnetResponse;
import software.amazon.awssdk.services.ec2.model.CreateTagsRequest;
import software.amazon.awssdk.services.ec2.model.DeleteSecurityGroupRequest;
import software.amazon.awssdk.services.ec2.model.DeleteSubnetRequest;
import software.amazon.awssdk.services.ec2.model.DescribeRouteTablesResponse;
import software.amazon.awssdk.services.ec2.model.DescribeSubnetsRequest;
import software.amazon.awssdk.services.ec2.model.DescribeSubnetsResponse;
import software.amazon.awssdk.services.ec2.model.Route;
import software.amazon.awssdk.services.ec2.model.RouteTable;
import software.amazon.awssdk.services.ec2.model.Subnet;
import software.amazon.awssdk.services.ec2.model.Tag;

public class AwsV2NetworkPlugin implements NetworkPlugin<AwsV2User> {

	private static final Logger LOGGER = Logger.getLogger(AwsV2NetworkPlugin.class);

	private static final String ALL_PROTOCOLS = "-1";
	private static final String SECURITY_GROUP_DESCRIPTION = "Security group associated with a fogbow network.";
	private static final Object SECURITY_GROUP_RESOURCE = "Security Groups";
	private static final String SUBNET_RESOURCE = "Subnet";

	protected static final String AWS_TAG_GROUP_ID = "groupId";
	protected static final String AWS_TAG_NAME = "Name";
	protected static final String LOCAL_GATEWAY_DESTINATION = "local";
	
	private String defaultVpcId;
	private String region;
	private String zone;

	public AwsV2NetworkPlugin(String confFilePath) {
		Properties properties = PropertiesUtil.readProperties(confFilePath);
		this.defaultVpcId = properties.getProperty(AwsV2ConfigurationPropertyKeys.AWS_DEFAULT_VPC_ID_KEY);
		this.region = properties.getProperty(AwsV2ConfigurationPropertyKeys.AWS_REGION_SELECTION_KEY);
		this.zone = properties.getProperty(AwsV2ConfigurationPropertyKeys.AWS_AVAILABILITY_ZONE_KEY);
	}

	@Override
	public String requestInstance(NetworkOrder networkOrder, AwsV2User cloudUser) throws FogbowException {
		LOGGER.info(String.format(Messages.Info.REQUESTING_INSTANCE, cloudUser.getToken()));

		String cidr = networkOrder.getCidr();
		String name = defineInstanceName(networkOrder.getName());

		CreateSubnetRequest request = CreateSubnetRequest.builder()
				.availabilityZone(this.zone)
				.cidrBlock(cidr)
				.vpcId(this.defaultVpcId)
				.build();

		Ec2Client client = AwsV2ClientUtil.createEc2Client(cloudUser.getToken(), this.region);
		String subnetId = doCreateSubnetResquests(name, request, client);
		doAssociateRouteTables(subnetId, client);
		handleSecurityIssues(cidr, subnetId, client);
		return subnetId;
	}

	@Override
	public NetworkInstance getInstance(NetworkOrder networkOrder, AwsV2User cloudUser) throws FogbowException {
		LOGGER.info(String.format(Messages.Info.GETTING_INSTANCE, networkOrder.getInstanceId(), cloudUser.getToken()));

		Ec2Client client = AwsV2ClientUtil.createEc2Client(cloudUser.getToken(), this.region);
		String subnetId = networkOrder.getInstanceId();
		Subnet subnet = getSubnetById(subnetId, client);
		RouteTable routeTable = getRouteTables(client);
		return mountNetworkInstance(subnet, routeTable);
	}

	@Override
	public void deleteInstance(NetworkOrder networkOrder, AwsV2User cloudUser) throws FogbowException {
		LOGGER.info(String.format(Messages.Info.DELETING_INSTANCE, networkOrder.getInstanceId(), cloudUser.getToken()));

		Ec2Client client = AwsV2ClientUtil.createEc2Client(cloudUser.getToken(), this.region);
		String subnetId = networkOrder.getInstanceId();
		String groupId = getGroupIdBySubnet(subnetId, client);
		doDeleteSecurityGroups(groupId, client);
		doDeleteSubnets(subnetId, client);
	}

	@Override
	public boolean isReady(String instanceState) {
		return AwsV2StateMapper.map(ResourceType.NETWORK, instanceState).equals(InstanceState.READY);
	}

	@Override
	public boolean hasFailed(String instanceState) {
		return false;
	}
	
	protected String getGroupIdBySubnet(String subnetId, Ec2Client client)
			throws UnexpectedException, InstanceNotFoundException {
		
		Subnet subnet = getSubnetById(subnetId, client);
		for (Tag tag : subnet.tags()) {
			if (tag.key().equals(AWS_TAG_GROUP_ID)) {
				return tag.value();
			}
		}
		throw new UnexpectedException(Messages.Exception.UNEXPECTED_ERROR);
	}

	protected NetworkInstance mountNetworkInstance(Subnet subnet, RouteTable routeTable) throws UnexpectedException {
		String id = subnet.subnetId();
		String cloudState = subnet.stateAsString();
		String name = subnet.tags().listIterator().next().value();
		String cidr = subnet.cidrBlock();
		String gateway = getGatewayFromRouteTables(routeTable.routes());
		String vLAN = null;
		String networkInterface = null;
		String macInterface = null;
		String interfaceState = null;
		NetworkAllocationMode networkAllocationMode = NetworkAllocationMode.DYNAMIC;
		return new NetworkInstance(id, cloudState, name, cidr, gateway, vLAN, networkAllocationMode, networkInterface,
				macInterface, interfaceState);
	}

	protected String getGatewayFromRouteTables(List<Route> routes) {
		for (Route route : routes) {
			if (!route.gatewayId().equals(LOCAL_GATEWAY_DESTINATION)) {
				return route.destinationCidrBlock();
			}
		}
		return null;
	}

	protected Subnet getSubnetById(String subnetId, Ec2Client client)
			throws UnexpectedException, InstanceNotFoundException {

		DescribeSubnetsResponse response = doDescribeSubnets(subnetId, client);
		if (response != null && !response.subnets().isEmpty()) {
			return response.subnets().listIterator().next();
		}
		throw new InstanceNotFoundException(Messages.Exception.INSTANCE_NOT_FOUND);
	}

	protected DescribeSubnetsResponse doDescribeSubnets(String subnetId, Ec2Client client)
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

	protected void handleSecurityIssues(String cidr, String subnetId, Ec2Client client)
			throws UnexpectedException, InstanceNotFoundException {

		String groupName = SystemConstants.PN_SECURITY_GROUP_PREFIX + subnetId;
		CreateSecurityGroupRequest request = CreateSecurityGroupRequest.builder()
				.description(SECURITY_GROUP_DESCRIPTION)
				.groupName(groupName)
				.vpcId(this.defaultVpcId)
				.build();

		String groupId = null;
		try {
			CreateSecurityGroupResponse response = client.createSecurityGroup(request);
			groupId = response.groupId();
			doCreateTagsRequests(AWS_TAG_GROUP_ID, groupId, subnetId, client);
			doAuthorizeSecurityGroupIngress(cidr, subnetId, groupId, client);
		} catch (Exception e) {
			doDeleteSubnets(subnetId, client);
			throw new UnexpectedException(String.format(Messages.Exception.GENERIC_EXCEPTION, e), e);
		}
	}

	protected void doDeleteSecurityGroups(String groupId, Ec2Client client) throws UnexpectedException {
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

	protected void doAuthorizeSecurityGroupIngress(String cidr, String subnetId, String groupId, Ec2Client client)
			throws FogbowException {

		AuthorizeSecurityGroupIngressRequest request = AuthorizeSecurityGroupIngressRequest.builder()
				.cidrIp(cidr)
				.groupId(groupId)
				.ipProtocol(ALL_PROTOCOLS)
				.build();
		try {
			client.authorizeSecurityGroupIngress(request);
		} catch (Exception e) {
			doDeleteSecurityGroups(groupId, client);
			doDeleteSubnets(subnetId, client);
			throw new UnexpectedException(String.format(Messages.Exception.GENERIC_EXCEPTION, e), e);
		}
	}

	protected void doDeleteSubnets(String subnetId, Ec2Client client)
			throws UnexpectedException, InstanceNotFoundException {

		DeleteSubnetRequest request = DeleteSubnetRequest.builder()
				.subnetId(subnetId)
				.build();
		try {
			client.deleteSubnet(request);
		} catch (Exception e) {
			LOGGER.error(String.format(Messages.Error.ERROR_WHILE_REMOVING_RESOURCE, SUBNET_RESOURCE, subnetId), e);
			throw new UnexpectedException();
		}
	}
	
	protected void doAssociateRouteTables(String subnetId, Ec2Client client)
			throws UnexpectedException, InstanceNotFoundException {

		RouteTable routeTable = getRouteTables(client);
		String routeTableId = routeTable.routeTableId();
		AssociateRouteTableRequest request = AssociateRouteTableRequest.builder()
				.routeTableId(routeTableId)
				.subnetId(subnetId)
				.build();
		try {
			client.associateRouteTable(request);
		} catch (Exception e) {
			doDeleteSubnets(subnetId, client);
			throw new UnexpectedException(String.format(Messages.Exception.GENERIC_EXCEPTION, e), e);
		}
	}
	
	protected RouteTable getRouteTables(Ec2Client client) throws UnexpectedException, InstanceNotFoundException {
		DescribeRouteTablesResponse response = doDescribeRouteTables(client);
		List<RouteTable> routeTables = response.routeTables();
		for (RouteTable routeTable : routeTables) {
			if (routeTable.vpcId().equals(this.defaultVpcId)) {
				return routeTable;
			}
		}
		throw new InstanceNotFoundException(Messages.Exception.INSTANCE_NOT_FOUND);
	}

	protected DescribeRouteTablesResponse doDescribeRouteTables(Ec2Client client) throws UnexpectedException {
		try {
			return client.describeRouteTables();
		} catch (Exception e) {
			throw new UnexpectedException(String.format(Messages.Exception.GENERIC_EXCEPTION, e), e);
		}
	}

	protected String doCreateSubnetResquests(String name, CreateSubnetRequest request, Ec2Client client)
			throws UnexpectedException {

		String subnetId = null;
		try {
			CreateSubnetResponse response = client.createSubnet(request);
			subnetId = response.subnet().subnetId();
			doCreateTagsRequests(AWS_TAG_NAME, name, subnetId, client);
			return subnetId;
		} catch (Exception e) {
			throw new UnexpectedException(String.format(Messages.Exception.GENERIC_EXCEPTION, e), e);
		}
	}

	protected void doCreateTagsRequests(String key, String value, String resourceId, Ec2Client client)
			throws UnexpectedException {
		
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

	protected String defineInstanceName(String instanceName) {
		return instanceName == null ? SystemConstants.FOGBOW_INSTANCE_NAME_PREFIX + getRandomUUID() : instanceName;
	}

	protected String getRandomUUID() {
		return UUID.randomUUID().toString();
	}

}
