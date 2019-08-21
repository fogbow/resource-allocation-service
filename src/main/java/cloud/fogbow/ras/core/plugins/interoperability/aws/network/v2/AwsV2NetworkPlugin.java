package cloud.fogbow.ras.core.plugins.interoperability.aws.network.v2;

import java.util.List;
import java.util.Properties;

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
import cloud.fogbow.ras.core.plugins.interoperability.aws.AwsV2CloudUtil;
import cloud.fogbow.ras.core.plugins.interoperability.aws.AwsV2ConfigurationPropertyKeys;
import cloud.fogbow.ras.core.plugins.interoperability.aws.AwsV2StateMapper;
import cloud.fogbow.ras.core.plugins.interoperability.util.FogbowCloudUtil;
import software.amazon.awssdk.core.exception.SdkException;
import software.amazon.awssdk.services.ec2.Ec2Client;
import software.amazon.awssdk.services.ec2.model.AssociateRouteTableRequest;
import software.amazon.awssdk.services.ec2.model.AuthorizeSecurityGroupIngressRequest;
import software.amazon.awssdk.services.ec2.model.CreateSubnetRequest;
import software.amazon.awssdk.services.ec2.model.CreateSubnetResponse;
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
	private static final String SUBNET_RESOURCE = "Subnet";

	protected static final String AWS_TAG_GROUP_ID = "groupId";
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
		LOGGER.info(String.format(Messages.Info.REQUESTING_INSTANCE_FROM_PROVIDER));

		Ec2Client client = AwsV2ClientUtil.createEc2Client(cloudUser.getToken(), this.region);
		String cidr = networkOrder.getCidr();
		String name = FogbowCloudUtil.defineInstanceName(networkOrder.getName());

		CreateSubnetRequest request = CreateSubnetRequest.builder()
				.availabilityZone(this.zone)
				.cidrBlock(cidr)
				.vpcId(this.defaultVpcId)
				.build();

		String subnetId = doCreateSubnetResquest(client, request, name);
		doAssociateRouteTables(client, subnetId);
		handleSecurityIssues(client, cidr, subnetId);
		return subnetId;
	}

	@Override
	public NetworkInstance getInstance(NetworkOrder networkOrder, AwsV2User cloudUser) throws FogbowException {
		LOGGER.info(String.format(Messages.Info.GETTING_INSTANCE_S, networkOrder.getInstanceId()));

		Ec2Client client = AwsV2ClientUtil.createEc2Client(cloudUser.getToken(), this.region);
		String subnetId = networkOrder.getInstanceId();
		Subnet subnet = getSubnetById(client, subnetId);
		RouteTable routeTable = getRouteTables(client);
		return mountNetworkInstance(subnet, routeTable);
	}

	@Override
	public void deleteInstance(NetworkOrder networkOrder, AwsV2User cloudUser) throws FogbowException {
		LOGGER.info(String.format(Messages.Info.DELETING_INSTANCE_S, networkOrder.getInstanceId()));

		Ec2Client client = AwsV2ClientUtil.createEc2Client(cloudUser.getToken(), this.region);
		String subnetId = networkOrder.getInstanceId();
		Subnet subnet = getSubnetById(client, subnetId); 
		String groupId = getGroupIdFrom(subnet);
		doDeleteSubnet(client, subnetId);
		AwsV2CloudUtil.doDeleteSecurityGroup(groupId, client);
	}

	@Override
	public boolean isReady(String instanceState) {
		return AwsV2StateMapper.map(ResourceType.NETWORK, instanceState).equals(InstanceState.READY);
	}

	@Override
	public boolean hasFailed(String instanceState) {
		return false;
	}
	
	protected String getGroupIdFrom(Subnet subnet) throws FogbowException {
		for (Tag tag : subnet.tags()) {
			if (tag.key().equals(AWS_TAG_GROUP_ID)) {
				return tag.value();
			}
		}
		throw new UnexpectedException(Messages.Exception.UNEXPECTED_ERROR);
	}

    protected NetworkInstance mountNetworkInstance(Subnet subnet, RouteTable routeTable) {
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

    protected void handleSecurityIssues(Ec2Client client, String subnetId, String cidr) throws FogbowException {
        String groupName = SystemConstants.PN_SECURITY_GROUP_PREFIX + subnetId;
        try {
            String groupId = AwsV2CloudUtil.createSecurityGroup(client, this.defaultVpcId, groupName,
                    SECURITY_GROUP_DESCRIPTION);
            
            AuthorizeSecurityGroupIngressRequest request = AuthorizeSecurityGroupIngressRequest.builder()   
                    .cidrIp(cidr)   
                    .groupId(groupId)   
                    .ipProtocol(ALL_PROTOCOLS)  
                    .build();

            AwsV2CloudUtil.doAuthorizeSecurityGroupIngress(client, request);
            AwsV2CloudUtil.createTagsRequest(subnetId, AWS_TAG_GROUP_ID, groupId, client);
        } catch (UnexpectedException e) {
            doDeleteSubnet(client, subnetId);
            throw new UnexpectedException(String.format(Messages.Exception.GENERIC_EXCEPTION, e), e);
        }
    }

	protected void doDeleteSubnet(Ec2Client client, String subnetId) throws FogbowException {
		DeleteSubnetRequest request = DeleteSubnetRequest.builder()
				.subnetId(subnetId)
				.build();
		try {
			client.deleteSubnet(request);
		} catch (SdkException e) {
			LOGGER.error(String.format(Messages.Error.ERROR_WHILE_REMOVING_RESOURCE, SUBNET_RESOURCE, subnetId), e);
			throw new UnexpectedException();
		}
	}
	
	protected void doAssociateRouteTables(Ec2Client client, String subnetId) throws FogbowException {
		RouteTable routeTable = getRouteTables(client);
		String routeTableId = routeTable.routeTableId();
		AssociateRouteTableRequest request = AssociateRouteTableRequest.builder()
				.routeTableId(routeTableId)
				.subnetId(subnetId)
				.build();
		try {
			client.associateRouteTable(request);
		} catch (SdkException e) {
			doDeleteSubnet(client, subnetId);
			throw new UnexpectedException(String.format(Messages.Exception.GENERIC_EXCEPTION, e), e);
		}
	}
	
	protected RouteTable getRouteTables(Ec2Client client) throws FogbowException {
		DescribeRouteTablesResponse response = doDescribeRouteTables(client);
		List<RouteTable> routeTables = response.routeTables();
		for (RouteTable routeTable : routeTables) {
			if (routeTable.vpcId().equals(this.defaultVpcId)) {
				return routeTable;
			}
		}
		throw new InstanceNotFoundException(Messages.Exception.INSTANCE_NOT_FOUND);
	}

	protected DescribeRouteTablesResponse doDescribeRouteTables(Ec2Client client) throws FogbowException {
		try {
			return client.describeRouteTables();
		} catch (SdkException e) {
			throw new UnexpectedException(String.format(Messages.Exception.GENERIC_EXCEPTION, e), e);
		}
	}
	
	protected Subnet getSubnetById(Ec2Client client, String subnetId) throws FogbowException {
        DescribeSubnetsRequest request = DescribeSubnetsRequest.builder()
                .subnetIds(subnetId)
                .build();
        
        DescribeSubnetsResponse response = doDescribeSubnetsRequest(client, request);
        return getSubnetFrom(response);
    }
	
	protected Subnet getSubnetFrom(DescribeSubnetsResponse response) throws FogbowException {
        if (response != null && !response.subnets().isEmpty()) {
            return response.subnets().listIterator().next();
        }
        throw new InstanceNotFoundException(Messages.Exception.INSTANCE_NOT_FOUND);
    }

    protected DescribeSubnetsResponse doDescribeSubnetsRequest(Ec2Client client, DescribeSubnetsRequest request)
            throws FogbowException {
        try {
            return client.describeSubnets(request);
        } catch (SdkException e) {
            throw new UnexpectedException(String.format(Messages.Exception.GENERIC_EXCEPTION, e), e);
        }
    }

    protected String doCreateSubnetResquest(Ec2Client client, CreateSubnetRequest request, String name)
            throws FogbowException {

        String subnetId = null;
        try {
            CreateSubnetResponse response = client.createSubnet(request);
            subnetId = response.subnet().subnetId();
            AwsV2CloudUtil.createTagsRequest(subnetId, AwsV2CloudUtil.AWS_TAG_NAME, name, client);
        } catch (SdkException e) {
            throw new UnexpectedException(String.format(Messages.Exception.GENERIC_EXCEPTION, e), e);
        }
        return subnetId;
    }

}
