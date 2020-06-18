package cloud.fogbow.ras.core.plugins.interoperability.aws.network.v2;

import java.util.Arrays;
import java.util.List;
import java.util.Properties;

import cloud.fogbow.common.exceptions.InternalServerErrorException;
import org.apache.log4j.Logger;

import com.google.common.annotations.VisibleForTesting;

import cloud.fogbow.common.exceptions.FogbowException;
import cloud.fogbow.common.exceptions.InstanceNotFoundException;
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
import software.amazon.awssdk.core.exception.SdkClientException;
import software.amazon.awssdk.core.exception.SdkException;
import software.amazon.awssdk.services.ec2.Ec2Client;
import software.amazon.awssdk.services.ec2.model.AssociateRouteTableRequest;
import software.amazon.awssdk.services.ec2.model.AttachInternetGatewayRequest;
import software.amazon.awssdk.services.ec2.model.AttributeBooleanValue;
import software.amazon.awssdk.services.ec2.model.AuthorizeSecurityGroupIngressRequest;
import software.amazon.awssdk.services.ec2.model.CreateInternetGatewayResponse;
import software.amazon.awssdk.services.ec2.model.CreateRouteRequest;
import software.amazon.awssdk.services.ec2.model.CreateSubnetRequest;
import software.amazon.awssdk.services.ec2.model.CreateSubnetResponse;
import software.amazon.awssdk.services.ec2.model.CreateVpcRequest;
import software.amazon.awssdk.services.ec2.model.CreateVpcResponse;
import software.amazon.awssdk.services.ec2.model.DeleteInternetGatewayRequest;
import software.amazon.awssdk.services.ec2.model.DeleteSubnetRequest;
import software.amazon.awssdk.services.ec2.model.DeleteVpcRequest;
import software.amazon.awssdk.services.ec2.model.DescribeInternetGatewaysResponse;
import software.amazon.awssdk.services.ec2.model.DescribeRouteTablesResponse;
import software.amazon.awssdk.services.ec2.model.DetachInternetGatewayRequest;
import software.amazon.awssdk.services.ec2.model.InternetGateway;
import software.amazon.awssdk.services.ec2.model.InternetGatewayAttachment;
import software.amazon.awssdk.services.ec2.model.ModifyVpcAttributeRequest;
import software.amazon.awssdk.services.ec2.model.Route;
import software.amazon.awssdk.services.ec2.model.RouteTable;
import software.amazon.awssdk.services.ec2.model.Subnet;

public class AwsNetworkPlugin implements NetworkPlugin<AwsV2User> {

    private static final Logger LOGGER = Logger.getLogger(AwsNetworkPlugin.class);

    protected static final String ALL_PROTOCOLS = "-1";
    protected static final String DEFAULT_DESTINATION_CIDR = "0.0.0.0/0";
    protected static final String GATEWAY_RESOURCE = "Gateway";
    protected static final String LOCAL_GATEWAY_DESTINATION = "local";
    protected static final String SECURITY_GROUP_DESCRIPTION = "Security group associated with a fogbow network.";
    protected static final String SUBNET_RESOURCE = "Subnet";
    protected static final String VPC_RESOURCE = "VPC";

    private String region;
    private String zone;

    public AwsNetworkPlugin(String confFilePath) {
        Properties properties = PropertiesUtil.readProperties(confFilePath);
        this.region = properties.getProperty(AwsV2ConfigurationPropertyKeys.AWS_REGION_SELECTION_KEY);
        this.zone = properties.getProperty(AwsV2ConfigurationPropertyKeys.AWS_AVAILABILITY_ZONE_KEY);
    }

    @Override
    public String requestInstance(NetworkOrder networkOrder, AwsV2User cloudUser) throws FogbowException {
        LOGGER.info(String.format(Messages.Log.REQUESTING_INSTANCE_FROM_PROVIDER));
        Ec2Client client = AwsV2ClientUtil.createEc2Client(cloudUser.getToken(), this.region);
        String instanceName = networkOrder.getName();
        String cidr = networkOrder.getCidr();
        String vpcId = doCreateAndConfigureVpc(cidr, client);

        CreateSubnetRequest request = CreateSubnetRequest.builder()
                .availabilityZone(this.zone)
                .cidrBlock(cidr)
                .vpcId(vpcId)
                .build();

        return doRequestInstance(instanceName, request, client);
    }

    @Override
    public NetworkInstance getInstance(NetworkOrder networkOrder, AwsV2User cloudUser) throws FogbowException {
        LOGGER.info(String.format(Messages.Log.GETTING_INSTANCE_S, networkOrder.getInstanceId()));
        Ec2Client client = AwsV2ClientUtil.createEc2Client(cloudUser.getToken(), this.region);
        String subnetId = networkOrder.getInstanceId();
        return doGetInstance(subnetId, client);
    }

    @Override
    public void deleteInstance(NetworkOrder networkOrder, AwsV2User cloudUser) throws FogbowException {
        LOGGER.info(String.format(Messages.Log.DELETING_INSTANCE_S, networkOrder.getInstanceId()));
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

    @VisibleForTesting
    void doDeleteInstance(String subnetId, Ec2Client client) throws FogbowException {
        Subnet subnet = AwsV2CloudUtil.getSubnetById(subnetId, client);
        String groupId = AwsV2CloudUtil.getGroupIdFrom(subnet.tags());
        String vpcId = subnet.vpcId();
        AwsV2CloudUtil.doDeleteSecurityGroup(groupId, client);
        doDeleteSubnet(subnetId, client);
        String gatewayId = getGatewayIdAttachedToVpc(vpcId, client);
        doRollbackAllConfigurationAndDeleteVpc(gatewayId, vpcId, client);
    }

    @VisibleForTesting
    NetworkInstance doGetInstance(String subnetId, Ec2Client client) throws FogbowException {
        Subnet subnet = AwsV2CloudUtil.getSubnetById(subnetId, client);
        String vpcId = subnet.vpcId();
        RouteTable routeTable = getRouteTables(vpcId, client);
        return buildNetworkInstance(subnet, routeTable);
    }

    @VisibleForTesting
    NetworkInstance buildNetworkInstance(Subnet subnet, RouteTable routeTable) {
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

    @VisibleForTesting
    String getGatewayFromRouteTables(List<Route> routes) {
        for (Route route : routes) {
            if (!route.gatewayId().equals(LOCAL_GATEWAY_DESTINATION)) {
                return route.destinationCidrBlock();
            }
        }
        return null;
    }

    @VisibleForTesting
    String doRequestInstance(String instanceName, CreateSubnetRequest request, Ec2Client client)
            throws FogbowException {
        
        String cidrBlock = request.cidrBlock();
        String vpcId = request.vpcId();
        String subnetId = doCreateSubnetResquest(instanceName, request, client);
        doAssociateRouteTables(subnetId, vpcId, client);
        handleSecurityIssues(subnetId, vpcId, cidrBlock, client);
        return subnetId;
    }

    @VisibleForTesting
    void handleSecurityIssues(String subnetId, String vpcId, String cidrIp, Ec2Client client)
            throws FogbowException {

        String groupName = SystemConstants.PN_SECURITY_GROUP_PREFIX + subnetId;
        String groupDescription = SECURITY_GROUP_DESCRIPTION;
        try {
            String groupId = AwsV2CloudUtil.createSecurityGroup(vpcId, groupName, groupDescription, client);

            AuthorizeSecurityGroupIngressRequest request = AuthorizeSecurityGroupIngressRequest.builder()
                    .cidrIp(cidrIp)
                    .groupId(groupId)
                    .ipProtocol(ALL_PROTOCOLS)
                    .build();

            AwsV2CloudUtil.doAuthorizeSecurityGroupIngress(request, client);
            AwsV2CloudUtil.createTagsRequest(subnetId, AwsV2CloudUtil.AWS_TAG_GROUP_ID, groupId, client);
        } catch (InternalServerErrorException e) {
            doDeleteSubnet(subnetId, client);
            String gatewayId = getGatewayIdAttachedToVpc(vpcId, client);
            doRollbackAllConfigurationAndDeleteVpc(gatewayId, vpcId, client);
            throw new InternalServerErrorException(e.getMessage());
        }
    }

    @VisibleForTesting
    void doAssociateRouteTables(String subnetId, String vpcId, Ec2Client client) throws FogbowException {
        RouteTable routeTable = getRouteTables(vpcId, client);
        String routeTableId = routeTable.routeTableId();

        AssociateRouteTableRequest request = AssociateRouteTableRequest.builder()
                .routeTableId(routeTableId)
                .subnetId(subnetId)
                .build();
        try {
            client.associateRouteTable(request);
        } catch (SdkException e) {
            doDeleteSubnet(subnetId, client);
            String gatewayId = getGatewayIdAttachedToVpc(vpcId, client);
            doRollbackAllConfigurationAndDeleteVpc(gatewayId, vpcId, client);
            throw new InternalServerErrorException(e.getMessage());
        }
    }

    @VisibleForTesting
    String getGatewayIdAttachedToVpc(String vpcId, Ec2Client client) throws FogbowException {
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

    protected void doDeleteSubnet(String subnetId, Ec2Client client) throws FogbowException {
        DeleteSubnetRequest request = DeleteSubnetRequest.builder()
                .subnetId(subnetId)
                .build();
        try {
            client.deleteSubnet(request);
        } catch (SdkException e) {
            LOGGER.error(String.format(Messages.Log.ERROR_WHILE_REMOVING_RESOURCE_S_S, SUBNET_RESOURCE, subnetId), e);
            throw new InternalServerErrorException(String.format(Messages.Exception.ERROR_WHILE_REMOVING_RESOURCE_S_S, SUBNET_RESOURCE, subnetId));
        }
    }

    @VisibleForTesting
    RouteTable getRouteTables(String vpcId, Ec2Client client) throws FogbowException {
        DescribeRouteTablesResponse response = doDescribeRouteTables(client);
        List<RouteTable> routeTables = response.routeTables();
        for (RouteTable routeTable : routeTables) {
            if (routeTable.vpcId().equals(vpcId)) {
                return routeTable;
            }
        }
        throw new InstanceNotFoundException(Messages.Exception.INSTANCE_NOT_FOUND);
    }

    @VisibleForTesting
    DescribeRouteTablesResponse doDescribeRouteTables(Ec2Client client) throws FogbowException {
        try {
            return client.describeRouteTables();
        } catch (SdkException e) {
            throw new InternalServerErrorException(e.getMessage());
        }
    }

    @VisibleForTesting
    String doCreateSubnetResquest(String instanceName, CreateSubnetRequest request, Ec2Client client)
            throws FogbowException {

        String subnetId = null;
        try {
            CreateSubnetResponse response = client.createSubnet(request);
            subnetId = response.subnet().subnetId();
            AwsV2CloudUtil.createTagsRequest(subnetId, AwsV2CloudUtil.AWS_TAG_NAME, instanceName, client);
        } catch (SdkException e) {
            throw new InternalServerErrorException(e.getMessage());
        }
        return subnetId;
    }

    @VisibleForTesting
    String doCreateAndConfigureVpc(String cidr, Ec2Client client) throws FogbowException {
        CreateVpcRequest request = CreateVpcRequest.builder()
                .cidrBlock(cidr)
                .build();
        try {
            CreateVpcResponse response = client.createVpc(request);
            String vpcId = response.vpc().vpcId();
            doModifyVpcAttributes(vpcId, client);
            String gatewayId = doCreateInternetGateway(vpcId, client);
            doAttachInternetGateway(gatewayId, vpcId, client);
            doCreateRouteTables(cidr, gatewayId, vpcId, client);
            return vpcId;
        } catch (SdkException e) {
            throw new InternalServerErrorException(e.getMessage());
        }
    }

    @VisibleForTesting
    void doCreateRouteTables(String cidr, String gatewayId, String vpcId, Ec2Client client)
            throws FogbowException {

        RouteTable routeTable = getRouteTables(vpcId, client);
        String routerId = routeTable.routeTableId();

        CreateRouteRequest request = CreateRouteRequest.builder()
                .destinationCidrBlock(DEFAULT_DESTINATION_CIDR)
                .gatewayId(gatewayId)
                .routeTableId(routerId)
                .build();
        try {
            client.createRoute(request);
        } catch (SdkClientException e) {
            doRollbackAllConfigurationAndDeleteVpc(gatewayId, vpcId, client);
            throw new InternalServerErrorException(e.getMessage());
        }
    }

    @VisibleForTesting
    void doRollbackAllConfigurationAndDeleteVpc(String gatewayId, String vpcId, Ec2Client client)
            throws FogbowException {

        doDetachInternetGateway(gatewayId, vpcId, client);
        doDeleteInternetGateway(gatewayId, client);
        doDeleteVpc(vpcId, client);
    }

    @VisibleForTesting
    void doDetachInternetGateway(String gatewayId, String vpcId, Ec2Client client) throws FogbowException {
        DetachInternetGatewayRequest request = DetachInternetGatewayRequest.builder()
                .internetGatewayId(gatewayId)
                .vpcId(vpcId)
                .build();
        try {
            client.detachInternetGateway(request);
        } catch (SdkClientException e) {
            throw new InternalServerErrorException(e.getMessage());
        }
    }

    @VisibleForTesting
    void doAttachInternetGateway(String gatewayId, String vpcId, Ec2Client client) throws FogbowException {
        AttachInternetGatewayRequest request = AttachInternetGatewayRequest.builder()
                .internetGatewayId(gatewayId)
                .vpcId(vpcId)
                .build();
        try {
            client.attachInternetGateway(request);
        } catch (SdkClientException e) {
            doDeleteInternetGateway(gatewayId, client);
            doDeleteVpc(vpcId, client);
            throw new InternalServerErrorException(e.getMessage());
        }
    }

    @VisibleForTesting
    void doDeleteInternetGateway(String gatewayId, Ec2Client client) throws FogbowException {
        DeleteInternetGatewayRequest request = DeleteInternetGatewayRequest.builder()
                .internetGatewayId(gatewayId)
                .build();
        try {
            client.deleteInternetGateway(request);
        } catch (SdkClientException e) {
            LOGGER.error(String.format(Messages.Log.ERROR_WHILE_REMOVING_RESOURCE_S_S, GATEWAY_RESOURCE, gatewayId), e);
            throw new InternalServerErrorException(String.format(Messages.Exception.ERROR_WHILE_REMOVING_RESOURCE_S_S, GATEWAY_RESOURCE, gatewayId));
        }
    }

    @VisibleForTesting
    String doCreateInternetGateway(String vpcId, Ec2Client client) throws FogbowException {
        try {
            CreateInternetGatewayResponse response = client.createInternetGateway();
            return response.internetGateway().internetGatewayId();
        } catch (SdkException e) {
            doDeleteVpc(vpcId, client);
            throw new InternalServerErrorException(e.getMessage());
        }
    }

    @VisibleForTesting
    void doModifyVpcAttributes(String vpcId, Ec2Client client) throws FogbowException {
        ModifyVpcAttributeRequest[] modifyVpcAttributes = {
                doEnableDnsHostnames(vpcId),
                doEnableDnsSupport(vpcId)
        };

        /*
         * AWS does not allow two attributes of a VPC to be modified in a single
         * request. For this, you must send a request for each modification you
         * make.
         */
        List<ModifyVpcAttributeRequest> requests = Arrays.asList(modifyVpcAttributes);
        for (ModifyVpcAttributeRequest request : requests) {
            try {
                client.modifyVpcAttribute(request);
            } catch (SdkClientException e) {
                doDeleteVpc(vpcId, client);
                throw new InternalServerErrorException(e.getMessage());
            }
        }
    }

    @VisibleForTesting
    void doDeleteVpc(String vpcId, Ec2Client client) throws FogbowException {
        DeleteVpcRequest request = DeleteVpcRequest.builder()
                .vpcId(vpcId)
                .build();
        try {
            client.deleteVpc(request);
        } catch (SdkClientException e) {
            LOGGER.error(String.format(Messages.Log.ERROR_WHILE_REMOVING_RESOURCE_S_S, VPC_RESOURCE, vpcId), e);
            throw new InternalServerErrorException(String.format(Messages.Exception.ERROR_WHILE_REMOVING_RESOURCE_S_S, VPC_RESOURCE, vpcId));
        }
    }

    @VisibleForTesting
    ModifyVpcAttributeRequest doEnableDnsSupport(String vpcId) {
        AttributeBooleanValue enableDnsSupport = buildAttributeBooleanValue();
        return ModifyVpcAttributeRequest.builder()
                .vpcId(vpcId)
                .enableDnsSupport(enableDnsSupport)
                .build();
    }

    @VisibleForTesting
    ModifyVpcAttributeRequest doEnableDnsHostnames(String vpcId) {
        AttributeBooleanValue enableDnsHostnames = buildAttributeBooleanValue();
        return ModifyVpcAttributeRequest.builder()
                .vpcId(vpcId)
                .enableDnsHostnames(enableDnsHostnames)
                .build();
    }

    @VisibleForTesting
    AttributeBooleanValue buildAttributeBooleanValue() {
        return AttributeBooleanValue.builder()
                .value(true)
                .build();
    }

}
