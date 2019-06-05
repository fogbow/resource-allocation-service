package cloud.fogbow.ras.core.plugins.interoperability.aws.network.v2;

import java.util.List;
import java.util.Properties;
import java.util.UUID;

import org.apache.log4j.Logger;

import cloud.fogbow.common.exceptions.FogbowException;
import cloud.fogbow.common.exceptions.UnexpectedException;
import cloud.fogbow.common.models.AwsV2User;
import cloud.fogbow.common.util.PropertiesUtil;
import cloud.fogbow.ras.api.http.response.NetworkInstance;
import cloud.fogbow.ras.constants.Messages;
import cloud.fogbow.ras.constants.SystemConstants;
import cloud.fogbow.ras.core.models.NetworkAllocationMode;
import cloud.fogbow.ras.core.models.orders.NetworkOrder;
import cloud.fogbow.ras.core.plugins.interoperability.NetworkPlugin;
import cloud.fogbow.ras.core.plugins.interoperability.aws.AwsV2ClientUtil;
import cloud.fogbow.ras.core.plugins.interoperability.aws.AwsV2ConfigurationPropertyKeys;
import software.amazon.awssdk.services.ec2.Ec2Client;
import software.amazon.awssdk.services.ec2.model.AttachInternetGatewayRequest;
import software.amazon.awssdk.services.ec2.model.AttributeBooleanValue;
import software.amazon.awssdk.services.ec2.model.CreateInternetGatewayResponse;
import software.amazon.awssdk.services.ec2.model.CreateRouteRequest;
import software.amazon.awssdk.services.ec2.model.CreateSubnetRequest;
import software.amazon.awssdk.services.ec2.model.CreateSubnetResponse;
import software.amazon.awssdk.services.ec2.model.CreateTagsRequest;
import software.amazon.awssdk.services.ec2.model.CreateVpcRequest;
import software.amazon.awssdk.services.ec2.model.CreateVpcResponse;
import software.amazon.awssdk.services.ec2.model.DeleteInternetGatewayRequest;
import software.amazon.awssdk.services.ec2.model.DeleteSubnetRequest;
import software.amazon.awssdk.services.ec2.model.DeleteVpcRequest;
import software.amazon.awssdk.services.ec2.model.DescribeInternetGatewaysResponse;
import software.amazon.awssdk.services.ec2.model.DescribeRouteTablesResponse;
import software.amazon.awssdk.services.ec2.model.DescribeSubnetsRequest;
import software.amazon.awssdk.services.ec2.model.DescribeSubnetsResponse;
import software.amazon.awssdk.services.ec2.model.DetachInternetGatewayRequest;
import software.amazon.awssdk.services.ec2.model.InternetGateway;
import software.amazon.awssdk.services.ec2.model.InternetGatewayAttachment;
import software.amazon.awssdk.services.ec2.model.ModifyVpcAttributeRequest;
import software.amazon.awssdk.services.ec2.model.RouteTable;
import software.amazon.awssdk.services.ec2.model.Subnet;
import software.amazon.awssdk.services.ec2.model.Tag;

public class AwsV2NetworkPlugin implements NetworkPlugin<AwsV2User> {

	private static final Logger LOGGER = Logger.getLogger(AwsV2NetworkPlugin.class);

	private static final String AWS_TAG_NAME = "Name";
	private static final String DEFAULT_DESTINATION_CIRD = "0.0.0.0/0";
	private static final String GATEWAY_RESOURCE = "Gateway";
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
    	String vpcId = doCreateVpcRequests(cidr, client);
    	String subnetId = doCreateSubnetResquests(vpcId, cidr, client);
    	doEnableDnsHostnamesRequests(vpcId, subnetId, client);
    	doEnableDnsSupportRequests(vpcId, subnetId, client);
    	String gatewayId = doCreateInternetGatewayRequests(vpcId, subnetId, client);
    	doAttachInternetGatewayRequests(vpcId, subnetId, gatewayId, client);
    	doCreateRouteRequests(cidr, vpcId, subnetId, gatewayId, client);
    	String name = defineInstanceName(networkOrder.getName());
    	doCreateTagsRequests(name, subnetId, client);

    	return subnetId;
    }
    
    protected String defineInstanceName(String instanceName) {
		return instanceName == null ? SystemConstants.FOGBOW_INSTANCE_NAME_PREFIX + getRandomUUID() : instanceName;
	}
    
	private String getRandomUUID() {
		return UUID.randomUUID().toString();
	}

	private void doCreateTagsRequests(String name, String subnetId, Ec2Client client) throws UnexpectedException {
		Tag tagName = Tag.builder()
				.key(AWS_TAG_NAME)
				.value(name)
				.build();

		CreateTagsRequest request = CreateTagsRequest.builder()
				.resources(subnetId)
				.tags(tagName)
				.build();
		try {
			client.createTags(request);
		} catch (Exception e) {
			throw new UnexpectedException(String.format(Messages.Exception.GENERIC_EXCEPTION, e), e);
		}
	}

	private void doCreateRouteRequests(String cidr, String vpcId, String subnetId, String gatewayId, Ec2Client client)
			throws UnexpectedException {
		
		String routerId = getRouterIdByVpc(vpcId, client);
		CreateRouteRequest request = CreateRouteRequest.builder()
				.destinationCidrBlock(DEFAULT_DESTINATION_CIRD)
				.gatewayId(gatewayId)
				.routeTableId(routerId)
				.build();
		try {
			client.createRoute(request);
		} catch (Exception e) {
			doDetachInternetGatewayRequests(gatewayId, vpcId, client);
			doDeleteInternetGatewayRequests(gatewayId, client);
			doDeleteSubnetRequests(subnetId, vpcId, client);
			throw new UnexpectedException(String.format(Messages.Exception.GENERIC_EXCEPTION, e), e);
		}
	}

	private String getRouterIdByVpc(String vpcId, Ec2Client client) throws UnexpectedException {
		DescribeRouteTablesResponse response = doDescribeRouteTablesRequests(client);
		List<RouteTable> routeTables = response.routeTables();
		for (RouteTable routeTable : routeTables) {
			if (routeTable.vpcId().equals(vpcId)) {
				return routeTable.routeTableId();
			}
		}
		return null;
	}

	private DescribeRouteTablesResponse doDescribeRouteTablesRequests(Ec2Client client) throws UnexpectedException {
		DescribeRouteTablesResponse response = null;
		try {
			response = client.describeRouteTables();
		} catch (Exception e) {
			throw new UnexpectedException(String.format(Messages.Exception.GENERIC_EXCEPTION, e), e);
		}
		return response;
	}

	private void doAttachInternetGatewayRequests(String vpcId, String subnetId, String gatewayId, Ec2Client client)
			throws UnexpectedException {
		
		AttachInternetGatewayRequest request = AttachInternetGatewayRequest.builder()
				.internetGatewayId(gatewayId)
				.vpcId(vpcId)
				.build();
		try {
			client.attachInternetGateway(request);
		} catch (Exception e) {
			doDeleteInternetGatewayRequests(gatewayId, client);
			doDeleteSubnetRequests(subnetId, vpcId, client);
			throw new UnexpectedException(String.format(Messages.Exception.GENERIC_EXCEPTION, e), e);
		}
	}

	private void doDeleteInternetGatewayRequests(String gatewayId, Ec2Client client) throws UnexpectedException {
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

	private void doDetachInternetGatewayRequests(String gatewayId, String vpcId, Ec2Client client)
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

	private String getGatewayIdBySubnet(String vpcId, Ec2Client client) throws UnexpectedException {
    	DescribeInternetGatewaysResponse response = client.describeInternetGateways();
    	for (InternetGateway internetGateway : response.internetGateways()) {
			for (InternetGatewayAttachment attachment : internetGateway.attachments()) {
				if (attachment.vpcId().equals(vpcId)) {
					return internetGateway.internetGatewayId();
				}
			}
		}
		return null;
	}

	private String doCreateInternetGatewayRequests(String vpcId, String subnetId, Ec2Client client) throws UnexpectedException {
		try {
			CreateInternetGatewayResponse response = client.createInternetGateway();
			return response.internetGateway().internetGatewayId();
		} catch (Exception e) {
			doDeleteSubnetRequests(subnetId, vpcId, client);
			throw new UnexpectedException(String.format(Messages.Exception.GENERIC_EXCEPTION, e), e);
		}
	}

    private void doEnableDnsHostnamesRequests(String vpcId, String subnetId, Ec2Client client) throws UnexpectedException {
    	AttributeBooleanValue enableDnsHostnames = AttributeBooleanValue.builder()
    			.value(true)
    			.build();
    	
    	ModifyVpcAttributeRequest request = ModifyVpcAttributeRequest.builder()
				.vpcId(vpcId)
				.enableDnsHostnames(enableDnsHostnames)
				.build();
    	try {
			client.modifyVpcAttribute(request);
		} catch (Exception e) {
			doDeleteSubnetRequests(subnetId, vpcId, client); 
			throw new UnexpectedException(String.format(Messages.Exception.GENERIC_EXCEPTION, e), e);
		}
	}
    
    private void doEnableDnsSupportRequests(String vpcId, String subnetId, Ec2Client client) throws UnexpectedException {
    	AttributeBooleanValue enableDnsSupport = AttributeBooleanValue.builder()
    			.value(true)
    			.build();
    	
    	ModifyVpcAttributeRequest request = ModifyVpcAttributeRequest.builder()
				.vpcId(vpcId)
				.enableDnsSupport(enableDnsSupport)
				.build();
    	try {
			client.modifyVpcAttribute(request);
		} catch (Exception e) {
			doDeleteSubnetRequests(subnetId, vpcId, client); 
			throw new UnexpectedException(String.format(Messages.Exception.GENERIC_EXCEPTION, e), e);
		}
	}

	private void doDeleteSubnetRequests(String subnetId, String vpcId, Ec2Client client) throws UnexpectedException {
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

	private String getVpcIdBySubnet(String subnetId, Ec2Client client) throws UnexpectedException {
		DescribeSubnetsResponse response = doDescribeSubnetsRequests(subnetId, client);
		return response.subnets().listIterator().next().vpcId();
	}

	private String doCreateSubnetResquests(String vpcId, String cidr, Ec2Client client) throws UnexpectedException {
		CreateSubnetRequest request = CreateSubnetRequest.builder()
				.vpcId(vpcId)
				.cidrBlock(cidr)
				.build();
		try {
			CreateSubnetResponse subnetResponse = client.createSubnet(request);
			return subnetResponse.subnet().subnetId();
		} catch (Exception e) {
			doDeleteVpcRequests(vpcId, client);
			throw new UnexpectedException(String.format(Messages.Exception.GENERIC_EXCEPTION, e), e);
		}
	}

	private void doDeleteVpcRequests(String vpcId, Ec2Client client) throws UnexpectedException {
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

	private String doCreateVpcRequests(String cidr, Ec2Client client) throws UnexpectedException {
    	CreateVpcRequest request = CreateVpcRequest.builder()
    			.cidrBlock(cidr)
    			.build();
    	try {
    		CreateVpcResponse vpcResponse = client.createVpc(request);
    		return vpcResponse.vpc().vpcId();
		} catch (Exception e) {
			throw new UnexpectedException(String.format(Messages.Exception.GENERIC_EXCEPTION, e), e);
		}
	}

	@Override
    public NetworkInstance getInstance(NetworkOrder networkOrder, AwsV2User cloudUser) throws FogbowException {
		LOGGER.info(String.format(Messages.Info.GETTING_INSTANCE, networkOrder.getInstanceId(), cloudUser.getToken()));

		String subnetId = networkOrder.getInstanceId();
		Ec2Client client = AwsV2ClientUtil.createEc2Client(cloudUser.getToken(), this.region);
		DescribeSubnetsResponse response = doDescribeSubnetsRequests(subnetId, client);
		return mountNetworkInstance(response);
    }

	private NetworkInstance mountNetworkInstance(DescribeSubnetsResponse response) throws UnexpectedException {
		NetworkInstance networkInstance = null;
		if (response != null && !response.subnets().isEmpty()) {
			Subnet subnet = response.subnets().listIterator().next();
			String id = subnet.subnetId();
			String cloudState = subnet.stateAsString();
			String name = subnet.tags().listIterator().next().value();
			String cidr = subnet.cidrBlock();
			String gateway = null;
			String vLAN = null;
			String networkInterface = null;
			String macInterface = null;
			String interfaceState = null;
			NetworkAllocationMode networkAllocationMode = NetworkAllocationMode.DYNAMIC;
			networkInstance = new NetworkInstance(id, cloudState, name, cidr, gateway, vLAN, networkAllocationMode,
					networkInterface, macInterface, interfaceState);
		}
		return networkInstance;
	}

	private DescribeSubnetsResponse doDescribeSubnetsRequests(String subnetId, Ec2Client client)
			throws UnexpectedException {
		
		DescribeSubnetsResponse response = null;
		DescribeSubnetsRequest request = DescribeSubnetsRequest.builder()
				.subnetIds(subnetId)
				.build();
		try {
			response = client.describeSubnets(request);
		} catch (Exception e) {
			throw new UnexpectedException(String.format(Messages.Exception.GENERIC_EXCEPTION, e), e);
		}
		return response;
	}

	@Override
    public void deleteInstance(NetworkOrder networkOrder, AwsV2User cloudUser) throws FogbowException {
    	LOGGER.info(String.format(Messages.Info.DELETING_INSTANCE, networkOrder.getInstanceId(), cloudUser.getToken()));
    	
    	String subnetId = networkOrder.getInstanceId();
    	Ec2Client client = AwsV2ClientUtil.createEc2Client(cloudUser.getToken(), this.region);
    	
    	String vpcId = getVpcIdBySubnet(subnetId, client);
    	String gatewayId = getGatewayIdBySubnet(vpcId, client);
		if (gatewayId != null) {
			doDetachInternetGatewayRequests(gatewayId, vpcId, client);
			doDeleteInternetGatewayRequests(gatewayId, client);
		} 
    	doDeleteSubnetRequests(subnetId, vpcId, client);
    }

	@Override
    public boolean isReady(String instanceState) {
        return false; // TODO mapper cloud state...
    }

    @Override
    public boolean hasFailed(String instanceState) {
        return true; // TODO mapper cloud state...
    }
}
