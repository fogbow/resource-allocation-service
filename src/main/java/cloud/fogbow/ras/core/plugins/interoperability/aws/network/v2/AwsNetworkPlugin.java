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
import software.amazon.awssdk.core.exception.SdkException;
import software.amazon.awssdk.services.ec2.Ec2Client;
import software.amazon.awssdk.services.ec2.model.AssociateRouteTableRequest;
import software.amazon.awssdk.services.ec2.model.AuthorizeSecurityGroupIngressRequest;
import software.amazon.awssdk.services.ec2.model.CreateSubnetRequest;
import software.amazon.awssdk.services.ec2.model.CreateSubnetResponse;
import software.amazon.awssdk.services.ec2.model.DeleteSubnetRequest;
import software.amazon.awssdk.services.ec2.model.DescribeRouteTablesResponse;
import software.amazon.awssdk.services.ec2.model.Route;
import software.amazon.awssdk.services.ec2.model.RouteTable;
import software.amazon.awssdk.services.ec2.model.Subnet;

public class AwsNetworkPlugin implements NetworkPlugin<AwsV2User> {

    private static final Logger LOGGER = Logger.getLogger(AwsNetworkPlugin.class);

    protected static final String ALL_PROTOCOLS = "-1";
    protected static final String LOCAL_GATEWAY_DESTINATION = "local";
    protected static final String SECURITY_GROUP_DESCRIPTION = "Security group associated with a fogbow network.";
    protected static final String SUBNET_RESOURCE = "Subnet";

    private String defaultVpcId;
    private String region;
    private String zone;

    public AwsNetworkPlugin(String confFilePath) {
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

        CreateSubnetRequest request = CreateSubnetRequest.builder()
                .availabilityZone(this.zone)
                .cidrBlock(cidr)
                .vpcId(this.defaultVpcId)
                .build();

        return doRequestInstance(networkOrder, request, client);
    }

    @Override
    public NetworkInstance getInstance(NetworkOrder networkOrder, AwsV2User cloudUser) throws FogbowException {
        LOGGER.info(String.format(Messages.Info.GETTING_INSTANCE_S, networkOrder.getInstanceId()));
        Ec2Client client = AwsV2ClientUtil.createEc2Client(cloudUser.getToken(), this.region);
        String subnetId = networkOrder.getInstanceId();
        return doGetInstance(subnetId, client);
    }

    @Override
    public void deleteInstance(NetworkOrder networkOrder, AwsV2User cloudUser) throws FogbowException {
        LOGGER.info(String.format(Messages.Info.DELETING_INSTANCE_S, networkOrder.getInstanceId()));
        Ec2Client client = AwsV2ClientUtil.createEc2Client(cloudUser.getToken(), this.region);
        String subnetId = networkOrder.getInstanceId();
        doDeleteInstance(subnetId, client);
    }

    @Override
    public boolean isReady(String instanceState) {
        return AwsV2StateMapper.map(ResourceType.NETWORK, instanceState).equals(InstanceState.READY);
    }

    @Override
    public boolean hasFailed(String instanceState) {
        return false;
    }

    protected void doDeleteInstance(String subnetId, Ec2Client client) throws FogbowException {
        Subnet subnet = AwsV2CloudUtil.getSubnetById(subnetId, client);
        String groupId = AwsV2CloudUtil.getGroupIdFrom(subnet.tags());
        AwsV2CloudUtil.doDeleteSecurityGroup(groupId, client);
        doDeleteSubnet(subnetId, client);
    }

    protected NetworkInstance doGetInstance(String subnetId, Ec2Client client) throws FogbowException {
        Subnet subnet = AwsV2CloudUtil.getSubnetById(subnetId, client);
        RouteTable routeTable = getRouteTables(client);
        return buildNetworkInstance(subnet, routeTable);
    }

    protected NetworkInstance buildNetworkInstance(Subnet subnet, RouteTable routeTable) {
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

    protected String doRequestInstance(NetworkOrder order, CreateSubnetRequest request, Ec2Client client)
            throws FogbowException {
        
        String subnetId = doCreateSubnetResquest(order.getName(), request, client);
        doAssociateRouteTables(subnetId, client);
        handleSecurityIssues(subnetId, request.cidrBlock(), client);
        return subnetId;
    }

    protected void handleSecurityIssues(String subnetId, String cidr, Ec2Client client) throws FogbowException {
        String groupName = SystemConstants.PN_SECURITY_GROUP_PREFIX + subnetId;
        try {
            String groupId = AwsV2CloudUtil.createSecurityGroup(this.defaultVpcId, groupName,
                    SECURITY_GROUP_DESCRIPTION, client);

            AuthorizeSecurityGroupIngressRequest request = AuthorizeSecurityGroupIngressRequest.builder()
                    .cidrIp(cidr)
                    .groupId(groupId)
                    .ipProtocol(ALL_PROTOCOLS)
                    .build();

            AwsV2CloudUtil.doAuthorizeSecurityGroupIngress(request, client);
            AwsV2CloudUtil.createTagsRequest(subnetId, AwsV2CloudUtil.AWS_TAG_GROUP_ID, groupId, client);
        } catch (UnexpectedException e) {
            doDeleteSubnet(subnetId, client);
            throw new UnexpectedException(String.format(Messages.Exception.GENERIC_EXCEPTION, e), e);
        }
    }

    protected void doAssociateRouteTables(String subnetId, Ec2Client client) throws FogbowException {
        RouteTable routeTable = getRouteTables(client);
        String routeTableId = routeTable.routeTableId();
        AssociateRouteTableRequest request = AssociateRouteTableRequest.builder()
                .routeTableId(routeTableId)
                .subnetId(subnetId)
                .build();
        try {
            client.associateRouteTable(request);
        } catch (SdkException e) {
            doDeleteSubnet(subnetId, client);
            throw new UnexpectedException(String.format(Messages.Exception.GENERIC_EXCEPTION, e), e);
        }
    }

    protected void doDeleteSubnet(String subnetId, Ec2Client client) throws FogbowException {
        DeleteSubnetRequest request = DeleteSubnetRequest.builder()
                .subnetId(subnetId)
                .build();
        try {
            client.deleteSubnet(request);
        } catch (SdkException e) {
            String message = String.format(Messages.Error.ERROR_WHILE_REMOVING_RESOURCE, SUBNET_RESOURCE, subnetId);
            LOGGER.error(message, e);
            throw new UnexpectedException(message);
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

    protected String doCreateSubnetResquest(String instanceName, CreateSubnetRequest request, Ec2Client client)
            throws FogbowException {

        String subnetId = null;
        try {
            CreateSubnetResponse response = client.createSubnet(request);
            subnetId = response.subnet().subnetId();
            AwsV2CloudUtil.createTagsRequest(subnetId, AwsV2CloudUtil.AWS_TAG_NAME, instanceName, client);
        } catch (SdkException e) {
            throw new UnexpectedException(String.format(Messages.Exception.GENERIC_EXCEPTION, e), e);
        }
        return subnetId;
    }

}
