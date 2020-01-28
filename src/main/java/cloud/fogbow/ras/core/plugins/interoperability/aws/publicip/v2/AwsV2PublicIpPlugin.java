package cloud.fogbow.ras.core.plugins.interoperability.aws.publicip.v2;

import java.util.List;
import java.util.Properties;

import org.apache.log4j.Logger;

import cloud.fogbow.common.exceptions.FogbowException;
import cloud.fogbow.common.exceptions.InstanceNotFoundException;
import cloud.fogbow.common.exceptions.UnexpectedException;
import cloud.fogbow.common.models.AwsV2User;
import cloud.fogbow.common.util.PropertiesUtil;
import cloud.fogbow.ras.api.http.response.InstanceState;
import cloud.fogbow.ras.api.http.response.PublicIpInstance;
import cloud.fogbow.ras.api.http.response.quotas.allocation.PublicIpAllocation;
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
import software.amazon.awssdk.services.ec2.model.InstanceNetworkInterface;
import software.amazon.awssdk.services.ec2.model.ModifyNetworkInterfaceAttributeRequest;
import software.amazon.awssdk.services.ec2.model.ReleaseAddressRequest;
import software.amazon.awssdk.services.ec2.model.Reservation;
import software.amazon.awssdk.services.ec2.model.Tag;

public class AwsV2PublicIpPlugin implements PublicIpPlugin<AwsV2User> {

    private static final Logger LOGGER = Logger.getLogger(AwsV2PublicIpPlugin.class);

    protected static final String AWS_TAG_ASSOCIATION_ID = "associationId";
    protected static final String DEFAULT_DESTINATION_CIDR = "0.0.0.0/0";
    protected static final String SECURITY_GROUP_DESCRIPTION = "Security group associated with a fogbow public IP.";
    protected static final String TCP_PROTOCOL = "tcp";
    protected static final int PUBLIC_IP_ALLOCATION_NUMBER = 1;
    protected static final int SSH_DEFAULT_PORT = 22;

    private String defaultGroupId;
    private String defaultSubnetId;
    private String defaultVpcId;
    private String region;

    public AwsV2PublicIpPlugin(String confFilePath) {
        Properties properties = PropertiesUtil.readProperties(confFilePath);
        this.defaultGroupId = properties.getProperty(AwsV2ConfigurationPropertyKeys.AWS_DEFAULT_SECURITY_GROUP_ID_KEY);
        this.defaultSubnetId = properties.getProperty(AwsV2ConfigurationPropertyKeys.AWS_DEFAULT_SUBNET_ID_KEY);
        this.defaultVpcId = properties.getProperty(AwsV2ConfigurationPropertyKeys.AWS_DEFAULT_VPC_ID_KEY);
        this.region = properties.getProperty(AwsV2ConfigurationPropertyKeys.AWS_REGION_SELECTION_KEY);
    }

    @Override
    public String requestInstance(PublicIpOrder publicIpOrder, AwsV2User cloudUser) throws FogbowException {
        LOGGER.info(String.format(Messages.Info.REQUESTING_INSTANCE_FROM_PROVIDER));
        Ec2Client client = AwsV2ClientUtil.createEc2Client(cloudUser.getToken(), this.region);
        return doRequestInstance(publicIpOrder, client);
    }

    @Override
    public void deleteInstance(PublicIpOrder publicIpOrder, AwsV2User cloudUser) throws FogbowException {
        LOGGER.info(String.format(Messages.Info.DELETING_INSTANCE_S, publicIpOrder.getInstanceId()));
        Ec2Client client = AwsV2ClientUtil.createEc2Client(cloudUser.getToken(), this.region);
        String allocationId = publicIpOrder.getInstanceId();
        doDeleteInstance(allocationId, client);
    }

    @Override
    public PublicIpInstance getInstance(PublicIpOrder publicIpOrder, AwsV2User cloudUser) throws FogbowException {
        LOGGER.info(String.format(Messages.Info.GETTING_INSTANCE_S, publicIpOrder.getInstanceId()));
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

    protected void doDeleteInstance(String allocationId, Ec2Client client) throws FogbowException {
        Address address = AwsV2CloudUtil.getAddressById(allocationId, client);
        String networkInterfaceId = address.networkInterfaceId();
        String associationId = getAssociationIdFrom(address.tags());
        String groupId = AwsV2CloudUtil.getGroupIdFrom(address.tags());
        doModifyNetworkInterfaceAttributes(allocationId, this.defaultGroupId, networkInterfaceId, client);
        try {
            AwsV2CloudUtil.doDeleteSecurityGroup(groupId, client);
        } catch (UnexpectedException exception) {
            LOGGER.error(String.format(Messages.Error.ERROR_WHILE_REMOVING_RESOURCE,
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
        throw new UnexpectedException(Messages.Exception.UNEXPECTED_ERROR);
    }

    protected void doDisassociateAddresses(String associationId, Ec2Client client) throws FogbowException {
        DisassociateAddressRequest request = DisassociateAddressRequest.builder()
                .associationId(associationId)
                .build();
        try {
            client.disassociateAddress(request);
        } catch (SdkException e) {
            throw new UnexpectedException(String.format(Messages.Exception.GENERIC_EXCEPTION, e), e);
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
        String groupId = handleSecurityIssues(allocationId, client);
        String computeId = order.getComputeId();
        String networkInterfaceId = getInstanceNetworkInterfaceId(computeId, client);
        doModifyNetworkInterfaceAttributes(allocationId, groupId, networkInterfaceId, client);
        doAssociateAddress(allocationId, networkInterfaceId, client);
        updatePublicIpAllocation(order);
        return allocationId;
    }

    protected void updatePublicIpAllocation(PublicIpOrder publicIpOrder) {
        int publicIps = PUBLIC_IP_ALLOCATION_NUMBER;
        synchronized (publicIpOrder) {
            PublicIpAllocation actualAllocation = new PublicIpAllocation(publicIps);
            publicIpOrder.setActualAllocation(actualAllocation);
        }
        
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
            throw new UnexpectedException(String.format(Messages.Exception.GENERIC_EXCEPTION, e), e);
        }
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
            throw new UnexpectedException(String.format(Messages.Exception.GENERIC_EXCEPTION, e), e);
        }
    }

    protected void doReleaseAddresses(String allocationId, Ec2Client client) throws FogbowException {
        ReleaseAddressRequest request = ReleaseAddressRequest.builder()
                .allocationId(allocationId)
                .build();
        try {
            client.releaseAddress(request);
        } catch (SdkException e) {
            throw new UnexpectedException(String.format(Messages.Exception.GENERIC_EXCEPTION, e), e);
        }
    }

    protected String getInstanceNetworkInterfaceId(String instanceId, Ec2Client client) throws FogbowException {
        DescribeInstancesResponse response = AwsV2CloudUtil.doDescribeInstanceById(instanceId, client);
        Instance instance = getInstanceReservation(response);
        InstanceNetworkInterface networkInterface = selectNetworkInterfaceFrom(instance);
        return networkInterface.networkInterfaceId();
    }

    protected InstanceNetworkInterface selectNetworkInterfaceFrom(Instance instance) {
        // Select the first network interface different from the default.
        for (InstanceNetworkInterface networkInterface : instance.networkInterfaces()) {
            if (!networkInterface.subnetId().equals(this.defaultSubnetId)) {
                return networkInterface;
            }
        }
        // Returns the default network in the absence of others.
        return instance.networkInterfaces().listIterator().next();
    }

    protected Instance getInstanceReservation(DescribeInstancesResponse response) throws FogbowException {
        if (response != null && !response.reservations().isEmpty()) {
            Reservation reservation = response.reservations().listIterator().next();
            if (!reservation.instances().isEmpty()) {
                return reservation.instances().listIterator().next();
            }
        }
        throw new InstanceNotFoundException(Messages.Exception.INSTANCE_NOT_FOUND);
    }

    protected String handleSecurityIssues(String allocationId, Ec2Client client) throws FogbowException {
        String groupName = SystemConstants.PIP_SECURITY_GROUP_PREFIX + allocationId;
        try {
            String groupId = AwsV2CloudUtil.createSecurityGroup(this.defaultVpcId, groupName,
                    SECURITY_GROUP_DESCRIPTION, client);

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
            throw new UnexpectedException(String.format(Messages.Exception.GENERIC_EXCEPTION, e), e);
        }
    }
    
    protected String doAllocateAddresses(Ec2Client client) throws FogbowException {
        AllocateAddressRequest request = AllocateAddressRequest.builder()
                .domain(DomainType.VPC)
                .build();
        try {
            AllocateAddressResponse response = client.allocateAddress(request);
            return response.allocationId();
        } catch (SdkException e) {
            throw new UnexpectedException(String.format(Messages.Exception.GENERIC_EXCEPTION, e), e);
        }
    }

}
