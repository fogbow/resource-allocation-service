package cloud.fogbow.ras.core.plugins.interoperability.aws.publicip.v2;

import java.util.List;
import java.util.Properties;

import org.apache.log4j.Logger;

import cloud.fogbow.common.exceptions.FogbowException;
import cloud.fogbow.common.exceptions.InstanceNotFoundException;
import cloud.fogbow.common.exceptions.InternalServerErrorException;
import cloud.fogbow.common.models.AwsV2User;
import cloud.fogbow.common.util.PropertiesUtil;
import cloud.fogbow.ras.api.http.response.InstanceState;
import cloud.fogbow.ras.api.http.response.PublicIpInstance;
import cloud.fogbow.ras.constants.Messages;
import cloud.fogbow.ras.constants.SystemConstants;
import cloud.fogbow.ras.core.models.ResourceType;
import cloud.fogbow.ras.core.models.orders.PublicIpOrder;
import cloud.fogbow.ras.core.plugins.interoperability.PublicIpPlugin;
import cloud.fogbow.ras.core.plugins.interoperability.aws.AwsV2ClientUtil;
import cloud.fogbow.ras.core.plugins.interoperability.aws.AwsV2CloudUtil;
import cloud.fogbow.ras.core.plugins.interoperability.aws.AwsV2ConfigurationPropertyKeys;
import cloud.fogbow.ras.core.plugins.interoperability.aws.AwsV2StateMapper;
import software.amazon.awssdk.core.exception.SdkException;
import software.amazon.awssdk.services.ec2.Ec2Client;
import software.amazon.awssdk.services.ec2.model.Address;
import software.amazon.awssdk.services.ec2.model.AllocateAddressRequest;
import software.amazon.awssdk.services.ec2.model.AllocateAddressResponse;
import software.amazon.awssdk.services.ec2.model.AssociateAddressRequest;
import software.amazon.awssdk.services.ec2.model.AssociateAddressResponse;
import software.amazon.awssdk.services.ec2.model.AuthorizeSecurityGroupIngressRequest;
import software.amazon.awssdk.services.ec2.model.DescribeInstancesResponse;
import software.amazon.awssdk.services.ec2.model.DisassociateAddressRequest;
import software.amazon.awssdk.services.ec2.model.DomainType;
import software.amazon.awssdk.services.ec2.model.Instance;
import software.amazon.awssdk.services.ec2.model.ModifyNetworkInterfaceAttributeRequest;
import software.amazon.awssdk.services.ec2.model.ReleaseAddressRequest;
import software.amazon.awssdk.services.ec2.model.Reservation;
import software.amazon.awssdk.services.ec2.model.Subnet;
import software.amazon.awssdk.services.ec2.model.Tag;

public class AwsPublicIpPlugin implements PublicIpPlugin<AwsV2User> {

    private static final Logger LOGGER = Logger.getLogger(AwsPublicIpPlugin.class);

    protected static final String AWS_TAG_ASSOCIATION_ID = "associationId";
    protected static final String DEFAULT_DESTINATION_CIDR = "0.0.0.0/0";
    protected static final String SECURITY_GROUP_DESCRIPTION = "Security group associated with a fogbow public IP.";
    protected static final String TCP_PROTOCOL = "tcp";
    protected static final int PUBLIC_IP_ALLOCATION_NUMBER = 1;
    protected static final int SSH_DEFAULT_PORT = 22;

    private String defaultGroupId;
    private String region;

    public AwsPublicIpPlugin(String confFilePath) {
        Properties properties = PropertiesUtil.readProperties(confFilePath);
        this.defaultGroupId = properties.getProperty(AwsV2ConfigurationPropertyKeys.AWS_DEFAULT_SECURITY_GROUP_ID_KEY);
        this.region = properties.getProperty(AwsV2ConfigurationPropertyKeys.AWS_REGION_SELECTION_KEY);
    }

    @Override
    public String requestInstance(PublicIpOrder publicIpOrder, AwsV2User cloudUser) throws FogbowException {
        LOGGER.info(String.format(Messages.Log.REQUESTING_INSTANCE_FROM_PROVIDER));
        Ec2Client client = AwsV2ClientUtil.createEc2Client(cloudUser.getToken(), this.region);
        return doRequestInstance(publicIpOrder, client);
    }

    @Override
    public void deleteInstance(PublicIpOrder publicIpOrder, AwsV2User cloudUser) throws FogbowException {
        LOGGER.info(String.format(Messages.Log.DELETING_INSTANCE_S, publicIpOrder.getInstanceId()));
        Ec2Client client = AwsV2ClientUtil.createEc2Client(cloudUser.getToken(), this.region);
        String allocationId = publicIpOrder.getInstanceId();
        String computeId = publicIpOrder.getComputeId();
        doDeleteInstance(allocationId, computeId, client);
    }

    @Override
    public PublicIpInstance getInstance(PublicIpOrder publicIpOrder, AwsV2User cloudUser) throws FogbowException {
        LOGGER.info(String.format(Messages.Log.GETTING_INSTANCE_S, publicIpOrder.getInstanceId()));
        Ec2Client client = AwsV2ClientUtil.createEc2Client(cloudUser.getToken(), this.region);
        String allocationId = publicIpOrder.getInstanceId();
        return doGetInstance(allocationId, client);
    }

    @Override
    public boolean isReady(String instanceState) {
        return AwsV2StateMapper.map(ResourceType.PUBLIC_IP, instanceState).equals(InstanceState.READY);
    }

    @Override
    public boolean hasFailed(String instanceState) {
        return AwsV2StateMapper.map(ResourceType.PUBLIC_IP, instanceState).equals(InstanceState.FAILED);
    }

    protected void doDeleteInstance(String allocationId, String instanceId, Ec2Client client) throws FogbowException {
        Address address = AwsV2CloudUtil.getAddressById(allocationId, client);
        String networkInterfaceId = address.networkInterfaceId();
        String defaultGroupId = getDefaultGroupId(instanceId, client);
        doModifyNetworkInterfaceAttributes(allocationId, defaultGroupId, networkInterfaceId, client);

        List<Tag> addressTags = address.tags();
        String associationId = getAssociationIdFrom(addressTags);
        String groupId = AwsV2CloudUtil.getGroupIdFrom(addressTags);

        try {
            AwsV2CloudUtil.doDeleteSecurityGroup(groupId, client);
        } catch (InternalServerErrorException exception) {
            LOGGER.error(String.format(Messages.Log.ERROR_WHILE_REMOVING_RESOURCE_S_S,
                    AwsV2CloudUtil.SECURITY_GROUP_RESOURCE, groupId), exception);
            throw exception;
        } finally {
            doDisassociateAddresses(associationId, client);
            doReleaseAddresses(allocationId, client);
        }
    }
    
    public String getAssociationIdFrom(List<Tag> tags) throws FogbowException {
        for (Tag tag : tags) {
            if (tag.key().equals(AWS_TAG_ASSOCIATION_ID)) {
                return tag.value();
            }
        }
        throw new InternalServerErrorException(Messages.Exception.UNEXPECTED_ERROR);
    }

    protected void doDisassociateAddresses(String associationId, Ec2Client client) throws FogbowException {
        DisassociateAddressRequest request = DisassociateAddressRequest.builder()
                .associationId(associationId)
                .build();
        try {
            client.disassociateAddress(request);
        } catch (SdkException e) {
            throw new InternalServerErrorException(e.getMessage());
        }
    }

    protected PublicIpInstance doGetInstance(String allocationId, Ec2Client client) throws FogbowException {
        Address address = AwsV2CloudUtil.getAddressById(allocationId, client);
        return buildPublicIpInstance(address);
    }

    protected PublicIpInstance buildPublicIpInstance(Address address) {
        String id = address.allocationId();
        String cloudState = setPublicIpInstanceState(address);
        String ip = address.publicIp();
        PublicIpInstance publicIpInstance = new PublicIpInstance(id, cloudState, ip);
        return publicIpInstance;
    }
    
    protected String setPublicIpInstanceState(Address address) {
        if (address.instanceId() != null) {
            return AwsV2StateMapper.AVAILABLE_STATE;
        }
        return AwsV2StateMapper.ERROR_STATE;
    }

    protected String doRequestInstance(PublicIpOrder order, Ec2Client client) throws FogbowException {
        String allocationId = doAllocateAddresses(client);
        String computeId = order.getComputeId();
        Instance instance = getInstanceReservation(computeId, client);
        String groupId = handleSecurityIssues(allocationId, instance, client);
        String networkInterfaceId = getNetworkInterfaceIdFrom(instance);
        doModifyNetworkInterfaceAttributes(allocationId, groupId, networkInterfaceId, client);
        doAssociateAddress(allocationId, networkInterfaceId, client);
        return allocationId;
    }

    protected void doAssociateAddress(String allocationId, String networkInterfaceId, Ec2Client client)
            throws FogbowException {

        AssociateAddressRequest request = AssociateAddressRequest.builder()
                .allocationId(allocationId)
                .networkInterfaceId(networkInterfaceId)
                .build();
        try {
            AssociateAddressResponse response = client.associateAddress(request);
            String associationId = response.associationId();
            AwsV2CloudUtil.createTagsRequest(allocationId, AWS_TAG_ASSOCIATION_ID, associationId, client);
        } catch (SdkException e) {
            throw new InternalServerErrorException(e.getMessage());
        }
    }

    protected String getDefaultGroupId(String instanceId, Ec2Client client) throws FogbowException {
        Instance instance = getInstanceReservation(instanceId, client);
        String subnetId = instance.subnetId();
        Subnet subnet = AwsV2CloudUtil.getSubnetById(subnetId, client);
        List<Tag> subnetTags = subnet.tags();
        return AwsV2CloudUtil.getGroupIdFrom(subnetTags);
    }

    protected void doModifyNetworkInterfaceAttributes(String allocationId, String groupId, String networkInterfaceId,
            Ec2Client client) throws FogbowException {

        ModifyNetworkInterfaceAttributeRequest request = ModifyNetworkInterfaceAttributeRequest.builder()
                .groups(groupId)
                .networkInterfaceId(networkInterfaceId)
                .build();
        try {
            client.modifyNetworkInterfaceAttribute(request);
        } catch (SdkException e) {
            // if the group associated to the instance is the default group, it shouldn't be
            // removed in a failure case otherwise it should because it'd be useless in 
            // other context.
            if (!groupId.equals(this.defaultGroupId)) {
                AwsV2CloudUtil.doDeleteSecurityGroup(groupId, client);
            }
            doReleaseAddresses(allocationId, client);
            throw new InternalServerErrorException(e.getMessage());
        }
    }

    protected void doReleaseAddresses(String allocationId, Ec2Client client) throws FogbowException {
        ReleaseAddressRequest request = ReleaseAddressRequest.builder()
                .allocationId(allocationId)
                .build();
        try {
            client.releaseAddress(request);
        } catch (SdkException e) {
            throw new InternalServerErrorException(e.getMessage());
        }
    }

    protected String getNetworkInterfaceIdFrom(Instance instance) {
        return instance.networkInterfaces().listIterator().next().networkInterfaceId();
    }

    protected String handleSecurityIssues(String allocationId, Instance instance, Ec2Client client) throws FogbowException {
        String groupName = SystemConstants.PIP_SECURITY_GROUP_PREFIX + allocationId;
        String vpcId = instance.vpcId();
        try {
            String groupId = AwsV2CloudUtil.createSecurityGroup(vpcId, groupName, SECURITY_GROUP_DESCRIPTION, client);

            AuthorizeSecurityGroupIngressRequest request = AuthorizeSecurityGroupIngressRequest.builder()
                    .cidrIp(DEFAULT_DESTINATION_CIDR)
                    .fromPort(SSH_DEFAULT_PORT)
                    .toPort(SSH_DEFAULT_PORT)
                    .groupId(groupId)
                    .ipProtocol(TCP_PROTOCOL)
                    .build();

            AwsV2CloudUtil.doAuthorizeSecurityGroupIngress(request, client);
            AwsV2CloudUtil.createTagsRequest(allocationId, AwsV2CloudUtil.AWS_TAG_GROUP_ID, groupId, client);
            return groupId;
        } catch (FogbowException e) {
            doReleaseAddresses(allocationId, client);
            throw new InternalServerErrorException(e.getMessage());
        }
    }
    
    protected Instance getInstanceReservation(String instanceId, Ec2Client client) throws FogbowException {
        DescribeInstancesResponse response = AwsV2CloudUtil.doDescribeInstanceById(instanceId, client);
        if (response != null && !response.reservations().isEmpty()) {
            Reservation reservation = response.reservations().listIterator().next();
            if (!reservation.instances().isEmpty()) {
                return reservation.instances().listIterator().next();
            }
        }
        throw new InstanceNotFoundException(Messages.Exception.INSTANCE_NOT_FOUND);
    }

    protected String doAllocateAddresses(Ec2Client client) throws FogbowException {
        AllocateAddressRequest request = AllocateAddressRequest.builder()
                .domain(DomainType.VPC)
                .build();
        try {
            AllocateAddressResponse response = client.allocateAddress(request);
            return response.allocationId();
        } catch (SdkException e) {
            throw new InternalServerErrorException(e.getMessage());
        }
    }

}
