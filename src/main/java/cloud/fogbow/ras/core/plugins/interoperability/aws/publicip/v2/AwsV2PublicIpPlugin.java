package cloud.fogbow.ras.core.plugins.interoperability.aws.publicip.v2;

import java.util.Properties;

import org.apache.log4j.Logger;

import cloud.fogbow.common.exceptions.FogbowException;
import cloud.fogbow.common.exceptions.UnexpectedException;
import cloud.fogbow.common.models.AwsV2User;
import cloud.fogbow.common.util.PropertiesUtil;
import cloud.fogbow.ras.api.http.response.PublicIpInstance;
import cloud.fogbow.ras.constants.Messages;
import cloud.fogbow.ras.constants.SystemConstants;
import cloud.fogbow.ras.core.models.orders.PublicIpOrder;
import cloud.fogbow.ras.core.plugins.interoperability.PublicIpPlugin;
import cloud.fogbow.ras.core.plugins.interoperability.aws.AwsV2ClientUtil;
import cloud.fogbow.ras.core.plugins.interoperability.aws.AwsV2ConfigurationPropertyKeys;
import software.amazon.awssdk.services.ec2.Ec2Client;
import software.amazon.awssdk.services.ec2.model.Address;
import software.amazon.awssdk.services.ec2.model.AllocateAddressRequest;
import software.amazon.awssdk.services.ec2.model.AllocateAddressResponse;
import software.amazon.awssdk.services.ec2.model.AssociateAddressRequest;
import software.amazon.awssdk.services.ec2.model.AuthorizeSecurityGroupIngressRequest;
import software.amazon.awssdk.services.ec2.model.CreateSecurityGroupRequest;
import software.amazon.awssdk.services.ec2.model.CreateSecurityGroupResponse;
import software.amazon.awssdk.services.ec2.model.CreateTagsRequest;
import software.amazon.awssdk.services.ec2.model.DescribeAddressesRequest;
import software.amazon.awssdk.services.ec2.model.DescribeAddressesResponse;
import software.amazon.awssdk.services.ec2.model.DescribeInstancesRequest;
import software.amazon.awssdk.services.ec2.model.DescribeInstancesResponse;
import software.amazon.awssdk.services.ec2.model.DomainType;
import software.amazon.awssdk.services.ec2.model.Instance;
import software.amazon.awssdk.services.ec2.model.InstanceNetworkInterface;
import software.amazon.awssdk.services.ec2.model.ModifyNetworkInterfaceAttributeRequest;
import software.amazon.awssdk.services.ec2.model.ReleaseAddressRequest;
import software.amazon.awssdk.services.ec2.model.Tag;

public class AwsV2PublicIpPlugin implements PublicIpPlugin<AwsV2User> {

	private static final Logger LOGGER = Logger.getLogger(AwsV2PublicIpPlugin.class);

	private static final String AWS_TAG_GROUP_ID = null;
	private static final String DEFAULT_DESTINATION_CIDR = "0.0.0.0/0";
	private static final String SECURITY_GROUP_DESCRIPTION = "Security group associated with a fogbow public IP.";
	private static final String TCP_PROTOCOL = "tcp";
	
	private String region;
	private String vpcId;

    public AwsV2PublicIpPlugin(String confFilePath) {
        Properties properties = PropertiesUtil.readProperties(confFilePath);
        this.region = properties.getProperty(AwsV2ConfigurationPropertyKeys.AWS_REGION_SELECTION_KEY);
        this.vpcId = properties.getProperty(AwsV2ConfigurationPropertyKeys.AWS_DEFAULT_VPC_ID_KEY);
    }

    @Override
    public String requestInstance(PublicIpOrder publicIpOrder, AwsV2User cloudUser) throws FogbowException {
    	LOGGER.info(String.format(Messages.Info.REQUESTING_INSTANCE, cloudUser.getToken()));
    	Ec2Client client = AwsV2ClientUtil.createEc2Client(cloudUser.getToken(), this.region);
    	String allocationId = doAllocateAddressRequests(client);
    	
    	String instanceId = publicIpOrder.getComputeId();
    	InstanceNetworkInterface networkInterface = getNetworkInterfaceByInstance(instanceId, client);
		
    	String networkInterfaceId = networkInterface.networkInterfaceId();

    	String groupId = doCreateSecurityGroupRequests(allocationId, client);
		doModifyNetworkInterfaceAttributeRequests(groupId, networkInterfaceId, client);
		
		AssociateAddressRequest request = AssociateAddressRequest.builder()
				.instanceId(instanceId)
				.allocationId(allocationId)
				.networkInterfaceId(networkInterfaceId)
				.build();
		
		doAssociateAddressRequests(client, request);
		return allocationId;
    }

	private void doAssociateAddressRequests(Ec2Client client, AssociateAddressRequest request)
			throws UnexpectedException {
		try {
			client.associateAddress(request);
		} catch (Exception e) {
			throw new UnexpectedException(String.format(Messages.Exception.GENERIC_EXCEPTION, e), e);
		}
	}

	private void doModifyNetworkInterfaceAttributeRequests(String groupIds, String networkInterfaceId,
			Ec2Client client) throws UnexpectedException {
		
		ModifyNetworkInterfaceAttributeRequest request = ModifyNetworkInterfaceAttributeRequest
				.builder().groups(groupIds)
				.networkInterfaceId(networkInterfaceId)
				.build();
		try {
			client.modifyNetworkInterfaceAttribute(request);
		} catch (Exception e) {
			throw new UnexpectedException(String.format(Messages.Exception.GENERIC_EXCEPTION, e), e);
		}
	}

	private String doCreateSecurityGroupRequests(String allocationId, Ec2Client client) throws UnexpectedException {
		String groupName = SystemConstants.PN_SECURITY_GROUP_PREFIX + allocationId;
		CreateSecurityGroupRequest request = CreateSecurityGroupRequest.builder()
				.description(SECURITY_GROUP_DESCRIPTION)
				.groupName(groupName)
				.vpcId(this.vpcId)
				.build();

		String groupId = null;
		try {
			CreateSecurityGroupResponse response = client.createSecurityGroup(request);
			groupId = response.groupId();
			doCreateTagsRequests(AWS_TAG_GROUP_ID, groupId, allocationId, client);
			doAuthorizeSecurityGroupIngressRequests(allocationId, groupId, client);
		} catch (Exception e) {
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
			throw new UnexpectedException(String.format(Messages.Exception.GENERIC_EXCEPTION, e), e);
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

	private InstanceNetworkInterface getNetworkInterfaceByInstance(String instanceId, Ec2Client client) {
		DescribeInstancesRequest describeInstancesRequest = DescribeInstancesRequest.builder().instanceIds(instanceId).build();
		DescribeInstancesResponse describeInstancesResponse = client.describeInstances(describeInstancesRequest); // FIXME
		Instance instance = describeInstancesResponse.reservations().listIterator().next().instances().listIterator().next();
		return instance.networkInterfaces().listIterator().next(); // FIXME
	}

	private String doAllocateAddressRequests(Ec2Client client) {
		DomainType domainType = DomainType.VPC;
    	AllocateAddressRequest allocateAddressRequest = AllocateAddressRequest.builder().domain(domainType).build();
		AllocateAddressResponse allocateAddressResponse = client.allocateAddress(allocateAddressRequest); // FIXME
		return allocateAddressResponse.allocationId();
	}

    @Override
    public void deleteInstance(PublicIpOrder publicIpOrder, AwsV2User cloudUser) throws FogbowException {
    	LOGGER.info(String.format(Messages.Info.DELETING_INSTANCE, publicIpOrder.getInstanceId(), cloudUser.getToken()));
    	Ec2Client client = AwsV2ClientUtil.createEc2Client(cloudUser.getToken(), this.region);
    	String allocationId = publicIpOrder.getInstanceId();
    	ReleaseAddressRequest releaseAddressRequest = ReleaseAddressRequest.builder().allocationId(allocationId).build();
    	client.releaseAddress(releaseAddressRequest); // FIXME
    }

    @Override
    public PublicIpInstance getInstance(PublicIpOrder publicIpOrder, AwsV2User cloudUser) throws FogbowException {
    	LOGGER.info(String.format(Messages.Info.GETTING_INSTANCE, publicIpOrder.getInstanceId(), cloudUser.getToken()));
    	Ec2Client client = AwsV2ClientUtil.createEc2Client(cloudUser.getToken(), this.region);
    	String allocationId = publicIpOrder.getInstanceId();
    	DescribeAddressesRequest describeAddressesRequest = DescribeAddressesRequest.builder().allocationIds(allocationId).build();
		DescribeAddressesResponse describeAddressesResponse = client.describeAddresses(describeAddressesRequest);
		Address address = describeAddressesResponse.addresses().listIterator().next();
		String id = address.allocationId(); 
		// FIXME if the address has an instanceId it is because associated with an instance...
		String cloudState = address.domainAsString(); // FIXME the Address class does not have states...
		String ip = address.publicIp();
		PublicIpInstance publicIpInstance = new PublicIpInstance(id, cloudState, ip);
		return publicIpInstance;
    }

    @Override
    public boolean isReady(String instanceState) {
        return true; // FIXME
    }

    @Override
    public boolean hasFailed(String instanceState) {
        return false;
    }
}
