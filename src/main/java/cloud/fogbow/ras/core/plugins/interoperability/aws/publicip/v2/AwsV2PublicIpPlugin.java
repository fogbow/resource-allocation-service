package cloud.fogbow.ras.core.plugins.interoperability.aws.publicip.v2;

import java.util.Properties;

import org.apache.log4j.Logger;

import cloud.fogbow.common.exceptions.FogbowException;
import cloud.fogbow.common.exceptions.InstanceNotFoundException;
import cloud.fogbow.common.exceptions.UnexpectedException;
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
import cloud.fogbow.ras.core.plugins.interoperability.aws.AwsV2ConfigurationPropertyKeys;
import cloud.fogbow.ras.core.plugins.interoperability.aws.AwsV2StateMapper;
import software.amazon.awssdk.services.ec2.Ec2Client;
import software.amazon.awssdk.services.ec2.model.Address;
import software.amazon.awssdk.services.ec2.model.AllocateAddressRequest;
import software.amazon.awssdk.services.ec2.model.AllocateAddressResponse;
import software.amazon.awssdk.services.ec2.model.AssociateAddressRequest;
import software.amazon.awssdk.services.ec2.model.AssociateAddressResponse;
import software.amazon.awssdk.services.ec2.model.AuthorizeSecurityGroupIngressRequest;
import software.amazon.awssdk.services.ec2.model.CreateSecurityGroupRequest;
import software.amazon.awssdk.services.ec2.model.CreateSecurityGroupResponse;
import software.amazon.awssdk.services.ec2.model.CreateTagsRequest;
import software.amazon.awssdk.services.ec2.model.DeleteSecurityGroupRequest;
import software.amazon.awssdk.services.ec2.model.DescribeAddressesRequest;
import software.amazon.awssdk.services.ec2.model.DescribeAddressesResponse;
import software.amazon.awssdk.services.ec2.model.DescribeInstancesRequest;
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

	private static final String AWS_TAG_ASSOCIATION_ID = "associationId";
	private static final String AWS_TAG_GROUP_ID = "groupId";
	private static final String DEFAULT_DESTINATION_CIDR = "0.0.0.0/0";
	private static final String SECURITY_GROUP_DESCRIPTION = "Security group associated with a fogbow public IP.";
	private static final String SECURITY_GROUP_RESOURCE = "Security Group";
	private static final String TCP_PROTOCOL = "tcp";

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
		LOGGER.info(String.format(Messages.Info.REQUESTING_INSTANCE, cloudUser.getToken()));

		Ec2Client client = AwsV2ClientUtil.createEc2Client(cloudUser.getToken(), this.region);
		String allocationId = doAllocateAddresses(client);
		String instanceId = publicIpOrder.getComputeId();
		InstanceNetworkInterface networkInterface = loadInstanceNetworkInterfaces(instanceId, client);
		String networkInterfaceId = networkInterface.networkInterfaceId();

		String groupId = doCreateSecurityGroups(allocationId, client);
		doModifyNetworkInterfaceAttributes(groupId, networkInterfaceId, allocationId, client);

		AssociateAddressRequest request = AssociateAddressRequest.builder()
				.allocationId(allocationId)
				.networkInterfaceId(networkInterfaceId)
				.build();

		return doAssociateAddressRequests(allocationId, request, client);
	}

	@Override
	public void deleteInstance(PublicIpOrder publicIpOrder, AwsV2User cloudUser) throws FogbowException {
		LOGGER.info(String.format(Messages.Info.DELETING_INSTANCE, publicIpOrder.getInstanceId(), 
				cloudUser.getToken()));

		Ec2Client client = AwsV2ClientUtil.createEc2Client(cloudUser.getToken(), this.region);
		String allocationId = publicIpOrder.getInstanceId();
		String associationId = getResourceIdByAddressTag(AWS_TAG_ASSOCIATION_ID, allocationId, client);
		String groupId = getResourceIdByAddressTag(AWS_TAG_GROUP_ID, allocationId, client);

		Address address = getAddressById(allocationId, client);
		String networkInterfaceId = address.networkInterfaceId();

		doModifyNetworkInterfaceAttributes(this.defaultGroupId, networkInterfaceId, allocationId, client);
		doDeleteSecurityGroups(groupId, client);
		doDisassociateAddresses(associationId, client);
		doReleaseAddresses(allocationId, client);
	}

	@Override
	public PublicIpInstance getInstance(PublicIpOrder publicIpOrder, AwsV2User cloudUser) throws FogbowException {
		LOGGER.info(String.format(Messages.Info.GETTING_INSTANCE, publicIpOrder.getInstanceId(), cloudUser.getToken()));

		Ec2Client client = AwsV2ClientUtil.createEc2Client(cloudUser.getToken(), this.region);
		String allocationId = publicIpOrder.getInstanceId();
		Address address = getAddressById(allocationId, client);
		return mountPublicIpInstance(address);
	}

	@Override
	public boolean isReady(String instanceState) {
		return AwsV2StateMapper.map(ResourceType.PUBLIC_IP, instanceState).equals(InstanceState.READY);
	}

	@Override
	public boolean hasFailed(String instanceState) {
		return AwsV2StateMapper.map(ResourceType.PUBLIC_IP, instanceState).equals(InstanceState.FAILED);
	}

	private PublicIpInstance mountPublicIpInstance(Address address) {
		String id = address.allocationId();
		String cloudState = setPublicIpInstanceState(address);
		String ip = address.publicIp();
		PublicIpInstance publicIpInstance = new PublicIpInstance(id, cloudState, ip);
		return publicIpInstance;
	}

	private String setPublicIpInstanceState(Address address) {
		if (address.instanceId() != null) {
			return AwsV2StateMapper.AVAILABLE_STATE;
		}
		return AwsV2StateMapper.ERROR_STATE;
	}

	private void doReleaseAddresses(String allocationId, Ec2Client client) throws UnexpectedException {
		ReleaseAddressRequest request = ReleaseAddressRequest.builder()
				.allocationId(allocationId)
				.build();
		try {
			client.releaseAddress(request);
		} catch (Exception e) {
			throw new UnexpectedException(String.format(Messages.Exception.GENERIC_EXCEPTION, e), e);
		}
	}

	private void doDisassociateAddresses(String associationId, Ec2Client client) throws UnexpectedException {
		DisassociateAddressRequest request = DisassociateAddressRequest.builder()
				.associationId(associationId)
				.build();
		try {
			client.disassociateAddress(request);
		} catch (Exception e) {
			throw new UnexpectedException(String.format(Messages.Exception.GENERIC_EXCEPTION, e), e);
		}
	}

	private String getResourceIdByAddressTag(String key, String resourceId, Ec2Client client) throws FogbowException {
		Address address = getAddressById(resourceId, client);
		for (Tag tag : address.tags()) {
			if (tag.key().equals(key)) {
				return tag.value();
			}
		}
		throw new InstanceNotFoundException(Messages.Exception.INSTANCE_NOT_FOUND);
	}

	private Address getAddressById(String allocationId, Ec2Client client) throws FogbowException {
		DescribeAddressesResponse response = doDescribeAddressesRequests(allocationId, client);
		if (response != null && !response.addresses().isEmpty()) {
			return response.addresses().listIterator().next();
		}
		throw new InstanceNotFoundException(Messages.Exception.INSTANCE_NOT_FOUND);
	}

	private DescribeAddressesResponse doDescribeAddressesRequests(String allocationId, Ec2Client client)
			throws UnexpectedException {

		DescribeAddressesRequest request = DescribeAddressesRequest.builder()
				.allocationIds(allocationId)
				.build();
		try {
			return client.describeAddresses(request);
		} catch (Exception e) {
			throw new UnexpectedException(String.format(Messages.Exception.GENERIC_EXCEPTION, e), e);
		}
	}

	private String doAssociateAddressRequests(String allocationId, AssociateAddressRequest request, Ec2Client client)
			throws UnexpectedException {
		try {
			AssociateAddressResponse response = client.associateAddress(request);
			String associationId = response.associationId();
			doCreateTagsRequests(AWS_TAG_ASSOCIATION_ID, associationId, allocationId, client);
			return allocationId;
		} catch (Exception e) {
			throw new UnexpectedException(String.format(Messages.Exception.GENERIC_EXCEPTION, e), e);
		}
	}

	private void doModifyNetworkInterfaceAttributes(String groupId, String networkInterfaceId,
			String allocationId, Ec2Client client) throws UnexpectedException {

		ModifyNetworkInterfaceAttributeRequest request = ModifyNetworkInterfaceAttributeRequest.builder()
				.groups(groupId)
				.networkInterfaceId(networkInterfaceId)
				.build();
		try {
			client.modifyNetworkInterfaceAttribute(request);
		} catch (Exception e) {
			if (!groupId.equals(this.defaultGroupId)) {
				doDeleteSecurityGroups(groupId, client);
				doReleaseAddresses(allocationId, client);
			}
			throw new UnexpectedException(String.format(Messages.Exception.GENERIC_EXCEPTION, e), e);
		}
	}

	private String doCreateSecurityGroups(String allocationId, Ec2Client client) throws UnexpectedException {
		String groupName = SystemConstants.PIP_SECURITY_GROUP_PREFIX + allocationId;
		CreateSecurityGroupRequest request = CreateSecurityGroupRequest.builder()
				.description(SECURITY_GROUP_DESCRIPTION)
				.groupName(groupName)
				.vpcId(this.defaultVpcId)
				.build();

		String groupId = null;
		try {
			CreateSecurityGroupResponse response = client.createSecurityGroup(request);
			groupId = response.groupId();
			doCreateTagsRequests(AWS_TAG_GROUP_ID, groupId, allocationId, client);
			doAuthorizeSecurityGroupIngressRequests(allocationId, groupId, client);
		} catch (Exception e) {
			if (groupId != null) {
				doDeleteSecurityGroups(groupId, client);
			}
			doReleaseAddresses(allocationId, client);
			throw new UnexpectedException(String.format(Messages.Exception.GENERIC_EXCEPTION, e), e);
		}
		return groupId;
	}

	private void doAuthorizeSecurityGroupIngressRequests(String allocationId, String groupId, Ec2Client client)
			throws UnexpectedException {

		AuthorizeSecurityGroupIngressRequest request = AuthorizeSecurityGroupIngressRequest.builder()
				.cidrIp(DEFAULT_DESTINATION_CIDR)
				.fromPort(22)
				.toPort(22)
				.groupId(groupId)
				.ipProtocol(TCP_PROTOCOL)
				.build();
		try {
			client.authorizeSecurityGroupIngress(request);
		} catch (Exception e) {
			doDeleteSecurityGroups(groupId, client);
			doReleaseAddresses(allocationId, client);
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
			LOGGER.error(String.format(Messages.Error.ERROR_WHILE_REMOVING_RESOURCE, SECURITY_GROUP_RESOURCE, groupId),
					e);
			throw new UnexpectedException();
		}
	}

	private void doCreateTagsRequests(String key, String value, String resourceId, Ec2Client client)
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

	private InstanceNetworkInterface loadInstanceNetworkInterfaces(String instanceId, Ec2Client client)
			throws FogbowException {

		DescribeInstancesResponse response = doDescribeInstancesRequests(instanceId, client);
		Instance instance = getInstanceReservation(response);
		return selectNetworkInterfaceFrom(instance);
	}

	private InstanceNetworkInterface selectNetworkInterfaceFrom(Instance instance) throws InstanceNotFoundException {
		if (instance != null && !instance.networkInterfaces().isEmpty()) {
			for (InstanceNetworkInterface networkInterface : instance.networkInterfaces()) {
				if (!networkInterface.subnetId().equals(this.defaultSubnetId)) {
					return networkInterface;
				}
			}
		}
		return instance.networkInterfaces().listIterator().next();
	}

	private Instance getInstanceReservation(DescribeInstancesResponse response) throws InstanceNotFoundException {
		if (response != null && !response.reservations().isEmpty()) {
			Reservation reservation = response.reservations().listIterator().next();
			if (reservation != null && !reservation.instances().isEmpty()) {
				return reservation.instances().listIterator().next();
			}
		}
		throw new InstanceNotFoundException(Messages.Exception.INSTANCE_NOT_FOUND);
	}

	private DescribeInstancesResponse doDescribeInstancesRequests(String instanceId, Ec2Client client)
			throws UnexpectedException {

		DescribeInstancesRequest describeInstancesRequest = DescribeInstancesRequest.builder()
				.instanceIds(instanceId)
				.build();
		try {
			return client.describeInstances(describeInstancesRequest);
		} catch (Exception e) {
			throw new UnexpectedException(String.format(Messages.Exception.GENERIC_EXCEPTION, e), e);
		}
	}

	private String doAllocateAddresses(Ec2Client client) throws UnexpectedException {
		AllocateAddressRequest allocateAddressRequest = AllocateAddressRequest.builder()
				.domain(DomainType.VPC)
				.build();
		try {
			AllocateAddressResponse response = client.allocateAddress(allocateAddressRequest);
			return response.allocationId();
		} catch (Exception e) {
			throw new UnexpectedException(String.format(Messages.Exception.GENERIC_EXCEPTION, e), e);
		}
	}

}
