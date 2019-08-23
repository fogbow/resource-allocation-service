package cloud.fogbow.ras.core.plugins.interoperability.aws.publicip.v2;

import java.io.File;
import java.util.HashMap;

import cloud.fogbow.ras.core.BaseUnitTests;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.BDDMockito;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.internal.verification.VerificationModeFactory;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;

import cloud.fogbow.common.exceptions.FogbowException;
import cloud.fogbow.common.exceptions.InstanceNotFoundException;
import cloud.fogbow.common.exceptions.UnexpectedException;
import cloud.fogbow.common.models.AwsV2User;
import cloud.fogbow.common.models.linkedlists.SynchronizedDoublyLinkedList;
import cloud.fogbow.common.util.HomeDir;
import cloud.fogbow.ras.api.http.response.PublicIpInstance;
import cloud.fogbow.ras.constants.SystemConstants;
import cloud.fogbow.ras.core.SharedOrderHolders;
import cloud.fogbow.ras.core.models.orders.OrderState;
import cloud.fogbow.ras.core.models.orders.PublicIpOrder;
import cloud.fogbow.ras.core.plugins.interoperability.aws.AwsV2ClientUtil;
import cloud.fogbow.ras.core.plugins.interoperability.aws.AwsV2CloudUtil;
import cloud.fogbow.ras.core.plugins.interoperability.aws.AwsV2StateMapper;
import software.amazon.awssdk.core.exception.SdkClientException;
import software.amazon.awssdk.services.ec2.Ec2Client;
import software.amazon.awssdk.services.ec2.model.Address;
import software.amazon.awssdk.services.ec2.model.AllocateAddressRequest;
import software.amazon.awssdk.services.ec2.model.AllocateAddressResponse;
import software.amazon.awssdk.services.ec2.model.AssociateAddressRequest;
import software.amazon.awssdk.services.ec2.model.AssociateAddressResponse;
import software.amazon.awssdk.services.ec2.model.CreateSecurityGroupRequest;
import software.amazon.awssdk.services.ec2.model.CreateSecurityGroupResponse;
import software.amazon.awssdk.services.ec2.model.DeleteSecurityGroupRequest;
import software.amazon.awssdk.services.ec2.model.DescribeAddressesRequest;
import software.amazon.awssdk.services.ec2.model.DescribeAddressesResponse;
import software.amazon.awssdk.services.ec2.model.DescribeInstancesRequest;
import software.amazon.awssdk.services.ec2.model.DescribeInstancesResponse;
import software.amazon.awssdk.services.ec2.model.DisassociateAddressRequest;
import software.amazon.awssdk.services.ec2.model.Instance;
import software.amazon.awssdk.services.ec2.model.InstanceNetworkInterface;
import software.amazon.awssdk.services.ec2.model.InstanceNetworkInterfaceAssociation;
import software.amazon.awssdk.services.ec2.model.InstancePrivateIpAddress;
import software.amazon.awssdk.services.ec2.model.ModifyNetworkInterfaceAttributeRequest;
import software.amazon.awssdk.services.ec2.model.ReleaseAddressRequest;
import software.amazon.awssdk.services.ec2.model.Reservation;
import software.amazon.awssdk.services.ec2.model.Tag;

@RunWith(PowerMockRunner.class)
@PrepareForTest({ AwsV2ClientUtil.class, SharedOrderHolders.class , AwsV2CloudUtil.class})
public class AwsV2PublicIpPluginTest extends BaseUnitTests {

	private static final String ANOTHER_SECURITY_GROUP_ID = "another-security-group-id";
	private static final String ANOTHER_SUBNET_ID = "another-subnet-id";
	private static final String ANY_VALUE = "anything";
	private static final String CLOUD_NAME = "amazon";
	private static final String FAKE_ALLOCATION_ID = "fake-allocation-id";
	private static final String FAKE_ASSOCIATION_ID = "fake-association-id";
	private static final String FAKE_CIDR_ADDRESS = "1.0.1.0/28";
	private static final String FAKE_DEFAULT_SECURITY_GROUP_ID = "fake-default-security-group-id";
	private static final String FAKE_INSTANCE_ID = "fake-instance-id";
	private static final String FAKE_NETWORK_INTERFACE_ID = "fake-network-interface-id";
	private static final String FAKE_SUBNET_ID = "fake-subnet-id";
	
	private AwsV2PublicIpPlugin plugin;
	private SharedOrderHolders sharedOrderHolders;

	@Before
	public void setUp() {
		String awsConfFilePath = HomeDir.getPath() 
				+ SystemConstants.CLOUDS_CONFIGURATION_DIRECTORY_NAME
				+ File.separator 
				+ CLOUD_NAME 
				+ File.separator 
				+ SystemConstants.CLOUD_SPECIFICITY_CONF_FILE_NAME;

		this.plugin = Mockito.spy(new AwsV2PublicIpPlugin(awsConfFilePath));
		this.sharedOrderHolders = Mockito.mock(SharedOrderHolders.class);

		PowerMockito.mockStatic(SharedOrderHolders.class);
		BDDMockito.given(SharedOrderHolders.getInstance()).willReturn(this.sharedOrderHolders);

		Mockito.when(this.sharedOrderHolders.getOrdersList(Mockito.any(OrderState.class)))
				.thenReturn(new SynchronizedDoublyLinkedList<>());

		Mockito.when(this.sharedOrderHolders.getActiveOrdersMap()).thenReturn(new HashMap<>());
	}
	
	// test case: When calling the requestInstance method, with a public IP order
	// and cloud user valid, a client is invoked to create a public IP instance,
	// returning the public IP ID.
	@Test
	public void testRequestInstanceUnsuccessful() throws FogbowException {
		// set up
		Ec2Client client = Mockito.mock(Ec2Client.class);
		PowerMockito.mockStatic(AwsV2ClientUtil.class);
		BDDMockito.given(AwsV2ClientUtil.createEc2Client(Mockito.anyString(), Mockito.anyString())).willReturn(client);

		PublicIpOrder publicIpOrder = Mockito.spy(new PublicIpOrder());
		AwsV2User cloudUser = Mockito.mock(AwsV2User.class);

		allocateAddressMocked(client);
		describeInstanceMocked(client);
		createMockedSecurityGroup(client);
		AssociateAddressMocked(client);

		String expected = FAKE_ALLOCATION_ID;

		// exercise
		String publicIpId = this.plugin.requestInstance(publicIpOrder, cloudUser);

		// verify
		PowerMockito.verifyStatic(AwsV2ClientUtil.class, VerificationModeFactory.times(1));
		AwsV2ClientUtil.createEc2Client(Mockito.anyString(), Mockito.anyString());

		Mockito.verify(client, Mockito.times(1)).allocateAddress(Mockito.any(AllocateAddressRequest.class));
		Mockito.verify(client, Mockito.times(1)).describeInstances(Mockito.any(DescribeInstancesRequest.class));
		Mockito.verify(client, Mockito.times(1)).createSecurityGroup(Mockito.any(CreateSecurityGroupRequest.class));
		Mockito.verify(client, Mockito.times(1)).associateAddress(Mockito.any(AssociateAddressRequest.class));

		Assert.assertEquals(expected, publicIpId);
	}
	
	// test case: When calling the deleteInstance method, with a public IP order and
	// cloud user valid, the elastic IP in the AWS cloud must be released.
	@Test
	public void testDeleteInstanceSuccessful() throws FogbowException {
		// set up
		Ec2Client client = Mockito.mock(Ec2Client.class);
		PowerMockito.mockStatic(AwsV2ClientUtil.class);
		BDDMockito.given(AwsV2ClientUtil.createEc2Client(Mockito.anyString(), Mockito.anyString())).willReturn(client);

		String instanceId = FAKE_INSTANCE_ID;
		Address address = buildAddress(instanceId);
		describeAddressesMocked(address, client);

		PublicIpOrder publicIpOrder = Mockito.spy(new PublicIpOrder());
		AwsV2User cloudUser = Mockito.mock(AwsV2User.class);

		// exercise
		this.plugin.deleteInstance(publicIpOrder, cloudUser);

		// verify
		PowerMockito.verifyStatic(AwsV2ClientUtil.class, VerificationModeFactory.times(1));
		AwsV2ClientUtil.createEc2Client(Mockito.anyString(), Mockito.anyString());

		Mockito.verify(client, Mockito.times(1))
				.modifyNetworkInterfaceAttribute(Mockito.any(ModifyNetworkInterfaceAttributeRequest.class));

		Mockito.verify(client, Mockito.times(1)).deleteSecurityGroup(Mockito.any(DeleteSecurityGroupRequest.class));
		Mockito.verify(client, Mockito.times(1)).disassociateAddress(Mockito.any(DisassociateAddressRequest.class));
		Mockito.verify(client, Mockito.times(1)).releaseAddress(Mockito.any(ReleaseAddressRequest.class));
	}
	
	// test case: When calling the getInstance method, with a public IP order and
	// cloud user valid, a client is invoked to request an address in the cloud, and
	// mount a public IP instance.
	@Test
	public void testGetInstanceSuccessful() throws FogbowException {
		// set up
		Ec2Client client = Mockito.mock(Ec2Client.class);
		PowerMockito.mockStatic(AwsV2ClientUtil.class);
		BDDMockito.given(AwsV2ClientUtil.createEc2Client(Mockito.anyString(), Mockito.anyString())).willReturn(client);

		String instanceId = FAKE_INSTANCE_ID;
		Address address = buildAddress(instanceId);
		describeAddressesMocked(address, client);

		PublicIpOrder publicIpOrder = Mockito.spy(new PublicIpOrder());
		AwsV2User cloudUser = Mockito.mock(AwsV2User.class);

		PublicIpInstance expected = createPublicIpInstance();

		// exercise
		PublicIpInstance publicIpInstance = this.plugin.getInstance(publicIpOrder, cloudUser);

		// verify
		PowerMockito.verifyStatic(AwsV2ClientUtil.class, VerificationModeFactory.times(1));
		AwsV2ClientUtil.createEc2Client(Mockito.anyString(), Mockito.anyString());

		Mockito.verify(client, Mockito.times(1)).describeAddresses(Mockito.any(DescribeAddressesRequest.class));

		Assert.assertEquals(expected, publicIpInstance);
	}

	// test case: When calling the isReady method with the cloud state AVAILABLE,
	// this means that the state of public IP is READY and it must return true.
	@Test
	public void testIsReadySuccessful() {
		// set up
		String cloudState = AwsV2StateMapper.AVAILABLE_STATE;

		// exercise
		boolean status = this.plugin.isReady(cloudState);

		// verify
		Assert.assertTrue(status);
	}
	
	// test case: When calling the isReady method with the cloud states different
	// than AVAILABLE, this means that the state of public IP is not READY and it
	// must return false.
	@Test
	public void testIsReadyUnsuccessful() {
		// set up
		String cloudState = AwsV2StateMapper.ERROR_STATE;

		// exercise
		boolean status = this.plugin.isReady(cloudState);

		// verify
		Assert.assertFalse(status);
	}
	
	// test case: When calling the hasFailed method with the cloud state ERROR,
	// this means that the state of public IP failed and it must return true.
	@Test
	public void testHasFailedSuccessful() {
		// set up
		String cloudState = AwsV2StateMapper.ERROR_STATE;

		// exercise
		boolean status = this.plugin.hasFailed(cloudState);

		// verify
		Assert.assertTrue(status);
	}
	
	// test case: When calling the hasFailed method with the cloud states different
	// than ERROR, this means that the state of public IP failed and it must return
	// true.
	@Test
	public void testHasFailedUnsuccessful() {
		// set up
		String cloudState = AwsV2StateMapper.AVAILABLE_STATE;

		// exercise
		boolean status = this.plugin.hasFailed(cloudState);

		// verify
		Assert.assertFalse(status);
	}
	
	// test case: When calling the setPublicIpInstanceState method, with an address
	// without instance ID, this means that there is no instance associated with the
	// address and must return an ERROR state.
	@Test
	public void testPublicIpStateMapperWithInstanceIdNull() {
		// set up
		String instanceId = null;
		Address address = buildAddress(instanceId);
		String expected = AwsV2StateMapper.ERROR_STATE;

		// exercise
		String state = this.plugin.setPublicIpInstanceState(address);

		// verify
		Assert.assertEquals(expected, state);
	}
	
	// test case: When calling the doDisassociateAddresses method, and an error
	// occurs during the request, an UnexpectedException will be thrown.
	@Test(expected = UnexpectedException.class) // verify
	public void testDoDisassociateAddressesUnsuccessful() throws FogbowException {
		// set up
		Ec2Client client = Mockito.mock(Ec2Client.class);
		PowerMockito.mockStatic(AwsV2ClientUtil.class);
		BDDMockito.given(AwsV2ClientUtil.createEc2Client(Mockito.anyString(), Mockito.anyString())).willReturn(client);

		Mockito.when(client.disassociateAddress(Mockito.any(DisassociateAddressRequest.class)))
				.thenThrow(SdkClientException.builder().build());

		String associationId = FAKE_ASSOCIATION_ID;

		// exercise
		this.plugin.doDisassociateAddresses(associationId, client);
	}
	
	// test case: When calling the getResourceIdByAddressTag method, with a tag
	// different of contained in the address, an UnexpectedException will be thrown.
	@Test(expected = UnexpectedException.class) // verify
	public void testGetResourceIdByAddressTagUnsuccessful() throws FogbowException {
		// set up
		Ec2Client client = Mockito.mock(Ec2Client.class);
		PowerMockito.mockStatic(AwsV2ClientUtil.class);
		BDDMockito.given(AwsV2ClientUtil.createEc2Client(Mockito.anyString(), Mockito.anyString())).willReturn(client);

		String instanceId = FAKE_INSTANCE_ID;
		Address address = buildAddress(instanceId);
		describeAddressesMocked(address, client);

		String resourceId = FAKE_ALLOCATION_ID;
		String key = ANY_VALUE;

		// exercise
		this.plugin.getResourceIdByAddressTag(key, resourceId, client);
	}
	
	// test case: When calling the doDescribeAddressesRequests method, and an error
	// occurs during the request, an UnexpectedException will be thrown.
	@Test(expected = UnexpectedException.class) // verify
	public void testDoDescribeAddressesRequestsUnsuccessful() throws FogbowException {
		// set up
		Ec2Client client = Mockito.mock(Ec2Client.class);
		PowerMockito.mockStatic(AwsV2ClientUtil.class);
		BDDMockito.given(AwsV2ClientUtil.createEc2Client(Mockito.anyString(), Mockito.anyString())).willReturn(client);

		Mockito.when(client.describeAddresses(Mockito.any(DescribeAddressesRequest.class)))
				.thenThrow(SdkClientException.builder().build());

		String allocationId = FAKE_ALLOCATION_ID;

		// exercise
		this.plugin.doDescribeAddressesRequests(allocationId, client);
	}
	
	// test case: When calling the getAddressById method and the response of the
	// request returns a null address, an InstanceNotFoundException will be thrown.
	@Test(expected = InstanceNotFoundException.class)
	public void testGetAddressByIdUnsuccessful() throws FogbowException {
		// set up
		Ec2Client client = Mockito.mock(Ec2Client.class);
		PowerMockito.mockStatic(AwsV2ClientUtil.class);
		BDDMockito.given(AwsV2ClientUtil.createEc2Client(Mockito.anyString(), Mockito.anyString())).willReturn(client);

		Address address = null;
		describeAddressesMocked(address, client);

		String allocationId = FAKE_ALLOCATION_ID;

		// exercise
		this.plugin.getAddressById(allocationId, client);
	}
	
	// test case: When calling the doReleaseAddresses method, and an error
	// occurs during the request, an UnexpectedException will be thrown.
	@Test(expected = UnexpectedException.class)
	public void testDoReleaseAddressesUnsuccessful() throws FogbowException {
		// set up
		Ec2Client client = Mockito.mock(Ec2Client.class);
		PowerMockito.mockStatic(AwsV2ClientUtil.class);
		BDDMockito.given(AwsV2ClientUtil.createEc2Client(Mockito.anyString(), Mockito.anyString())).willReturn(client);

		Mockito.when(client.releaseAddress(Mockito.any(ReleaseAddressRequest.class)))
				.thenThrow(SdkClientException.builder().build());

		String allocationId = FAKE_ALLOCATION_ID;

		// exercise
		this.plugin.doReleaseAddresses(allocationId, client);
	}
	
	// test case: When calling the doAssociateAddressRequests method, and an error
	// occurs during the request, an UnexpectedException will be thrown.
	@Test(expected = UnexpectedException.class) // verify
	public void testDoAssociateAddressRequestsUnsuccessful() throws FogbowException {
		// set up
		Ec2Client client = Mockito.mock(Ec2Client.class);
		PowerMockito.mockStatic(AwsV2ClientUtil.class);
		BDDMockito.given(AwsV2ClientUtil.createEc2Client(Mockito.anyString(), Mockito.anyString())).willReturn(client);

		Mockito.when(client.associateAddress(Mockito.any(AssociateAddressRequest.class)))
				.thenThrow(SdkClientException.builder().build());

		// exercise
		this.plugin.doAssociateAddress(FAKE_ALLOCATION_ID, FAKE_ASSOCIATION_ID, client);
	}
	
	// test case: When calling the doModifyNetworkInterfaceAttributes method, with a
	// group ID equals to default, and an error occurs during the request, an
	// UnexpectedException will be thrown.
	@Test(expected = UnexpectedException.class) // verify
	public void testDoModifyNetworkInterfaceAttributesUnsuccessfulWithDefaultGroupId() throws FogbowException {
		// set up
		Ec2Client client = Mockito.mock(Ec2Client.class);
		PowerMockito.mockStatic(AwsV2ClientUtil.class);
		BDDMockito.given(AwsV2ClientUtil.createEc2Client(Mockito.anyString(), Mockito.anyString())).willReturn(client);

		Mockito.when(client.modifyNetworkInterfaceAttribute(Mockito.any(ModifyNetworkInterfaceAttributeRequest.class)))
				.thenThrow(SdkClientException.builder().build());

		String groupId = FAKE_DEFAULT_SECURITY_GROUP_ID;
		String networkInterfaceId = FAKE_NETWORK_INTERFACE_ID;
		String allocationId = FAKE_ALLOCATION_ID;

		// exercise
		this.plugin.doModifyNetworkInterfaceAttributes(groupId, networkInterfaceId, allocationId, client);
	}
	
	// test case: When calling the doModifyNetworkInterfaceAttributes method, with a
	// group ID different of default, and an error occurs during the request, it
	// must delete security group, release the address and throw an
	// UnexpectedException.
	@Test(expected = UnexpectedException.class) // verify
	public void testDoModifyNetworkInterfaceAttributesUnsuccessfulWithAnotherGroupId() throws FogbowException {
		// set up
		Ec2Client client = Mockito.mock(Ec2Client.class);
		PowerMockito.mockStatic(AwsV2ClientUtil.class);
		BDDMockito.given(AwsV2ClientUtil.createEc2Client(Mockito.anyString(), Mockito.anyString())).willReturn(client);

		Mockito.when(client.modifyNetworkInterfaceAttribute(Mockito.any(ModifyNetworkInterfaceAttributeRequest.class)))
				.thenThrow(SdkClientException.builder().build());

		String groupId = ANOTHER_SECURITY_GROUP_ID;
		String networkInterfaceId = FAKE_NETWORK_INTERFACE_ID;
		String allocationId = FAKE_ALLOCATION_ID;

		// exercise
		this.plugin.doModifyNetworkInterfaceAttributes(groupId, networkInterfaceId, allocationId, client);
	}
	
	// test case: When calling the selectNetworkInterfaceFrom method, without anyone
	// sub-net ID different from the default sub-net, it must return the network
	// interface of default sub-net.
	@Test
	public void testSelectNetworkInterfaceUnsuccessful() {
		// set up
		String defaultSubnetId = FAKE_SUBNET_ID;
		Instance instance = buildInstance(defaultSubnetId);

		// exercise
		InstanceNetworkInterface networkInterface = this.plugin.selectNetworkInterfaceFrom(instance);

		// verify
		Assert.assertEquals(networkInterface.subnetId(), defaultSubnetId);
	}
	
	// test case: When calling the getInstanceReservation method and the response of
	// the request returns a null instance, an InstanceNotFoundException will be
	// thrown.
	@Test(expected = InstanceNotFoundException.class) // verify
	public void testGetInstanceReservationUnsuccessful() throws FogbowException {
		// set up
		Instance instance = null;
		Reservation reservation = buildReservation(instance);
		DescribeInstancesResponse response = buildDescribeInstanceResponse(reservation);

		// exercise
		this.plugin.getInstanceReservation(response);
	}

	
	// test case: When calling the doAllocateAddresses method, and an error occurs
	// during the request, an UnexpectedException will be thrown.
	@Test(expected = UnexpectedException.class) // verify
	public void testDoAllocateAddressesUnsuccessful() throws FogbowException {
		// set up
		Ec2Client client = Mockito.mock(Ec2Client.class);
		PowerMockito.mockStatic(AwsV2ClientUtil.class);
		BDDMockito.given(AwsV2ClientUtil.createEc2Client(Mockito.anyString(), Mockito.anyString())).willReturn(client);

		Mockito.when(client.allocateAddress(Mockito.any(AllocateAddressRequest.class)))
				.thenThrow(SdkClientException.builder().build());

		// exercise
		this.plugin.doAllocateAddresses(client);
	}

	//Test case: check if the testes method make the expected calls
	@Test
	public void testDoRequestInstance() throws FogbowException{
		//setup
		Ec2Client client = testUtils.getAwsMockedClient();
		Mockito.doReturn(FAKE_ALLOCATION_ID).when(plugin).doAllocateAddresses(Mockito.any());
		Mockito.doReturn(FAKE_NETWORK_INTERFACE_ID).when(plugin).getInstanceNetworkInterfaceId(Mockito.any(), Mockito.any());
		Mockito.doReturn(FAKE_DEFAULT_SECURITY_GROUP_ID).when(plugin).handleSecurityIssues(Mockito.any(), Mockito.any());
		Mockito.doNothing().when(plugin).doModifyNetworkInterfaceAttributes(Mockito.any(), Mockito.any(), Mockito.any(), Mockito.any());
		Mockito.doNothing().when(plugin).doAssociateAddress(Mockito.any(), Mockito.any(), Mockito.any());
		PublicIpOrder publicIpOrder = Mockito.spy(new PublicIpOrder());
		Mockito.doReturn(FAKE_INSTANCE_ID).when(publicIpOrder).getComputeId();
		//exercise
		plugin.doRequestInstance(publicIpOrder, client);
		//verify
		Mockito.verify(plugin, Mockito.times(1)).doAllocateAddresses(Mockito.any());
		Mockito.verify(plugin, Mockito.times(1)).getInstanceNetworkInterfaceId(Mockito.any(), Mockito.any());
		Mockito.verify(plugin, Mockito.times(1)).handleSecurityIssues(Mockito.any(), Mockito.any());
		Mockito.verify(plugin, Mockito.times(1)).doModifyNetworkInterfaceAttributes(Mockito.any(), Mockito.any(), Mockito.any(), Mockito.any());
		Mockito.verify(plugin, Mockito.times(1)).doAssociateAddress(Mockito.any(), Mockito.any(), Mockito.any());
		Mockito.verify(publicIpOrder, Mockito.times(1)).getComputeId();
	}

	//Test case: check if the testes method make the expected calls
	@Test
	public void testDoDeleteInstance() throws FogbowException {
		//setup
		PowerMockito.mockStatic(AwsV2CloudUtil.class);
		Mockito.doNothing().when(plugin).doModifyNetworkInterfaceAttributes(Mockito.any(), Mockito.any(), Mockito.any(), Mockito.any());
		Mockito.doNothing().when(plugin).doReleaseAddresses(Mockito.any(), Mockito.any());
		Mockito.doNothing().when(plugin).doDisassociateAddresses(Mockito.any(), Mockito.any());
		Ec2Client client = testUtils.getAwsMockedClient();
		//exercise
		plugin.doDeleteInstance(FAKE_ALLOCATION_ID, FAKE_ASSOCIATION_ID, FAKE_DEFAULT_SECURITY_GROUP_ID, FAKE_NETWORK_INTERFACE_ID, client);
		//verify
		Mockito.verify(plugin, Mockito.times(1)).doModifyNetworkInterfaceAttributes(Mockito.any(), Mockito.any(), Mockito.any(), Mockito.any());
		Mockito.verify(plugin, Mockito.times(1)).doReleaseAddresses(Mockito.any(), Mockito.any());
		Mockito.verify(plugin, Mockito.times(1)).doDisassociateAddresses(Mockito.any(), Mockito.any());
		PowerMockito.verifyStatic(AwsV2CloudUtil.class, Mockito.times(1));
		AwsV2CloudUtil.doDeleteSecurityGroup(FAKE_DEFAULT_SECURITY_GROUP_ID, client);

	}
	
	private PublicIpInstance createPublicIpInstance() {
		String id = FAKE_ALLOCATION_ID;
		String cloudState = AwsV2StateMapper.AVAILABLE_STATE;
		String ip = FAKE_CIDR_ADDRESS;
		return new PublicIpInstance(id, cloudState, ip);
	}
	
	private void describeAddressesMocked(Address address, Ec2Client client) {
		DescribeAddressesResponse response = null;
		if (address != null) {
			response = DescribeAddressesResponse.builder().addresses(address).build();
		}
		Mockito.when(client.describeAddresses(Mockito.any(DescribeAddressesRequest.class))).thenReturn(response);
	}

	private void AssociateAddressMocked(Ec2Client client) {
		AssociateAddressResponse response = AssociateAddressResponse.builder()
				.associationId(FAKE_ASSOCIATION_ID)
				.build();
		
		Mockito.when(client.associateAddress(Mockito.any(AssociateAddressRequest.class))).thenReturn(response);
	}

	private void createMockedSecurityGroup(Ec2Client client) {
		CreateSecurityGroupResponse response = CreateSecurityGroupResponse.builder()
				.groupId(FAKE_DEFAULT_SECURITY_GROUP_ID)
				.build();
		
		Mockito.when(client.createSecurityGroup(Mockito.any(CreateSecurityGroupRequest.class))).thenReturn(response);
	}

	private void describeInstanceMocked(Ec2Client client) {
		String subnetId = ANOTHER_SUBNET_ID;
		Instance instance = buildInstance(subnetId);
		Reservation reservation = buildReservation(instance);
		DescribeInstancesResponse response = buildDescribeInstanceResponse(reservation);
		Mockito.when(client.describeInstances(Mockito.any(DescribeInstancesRequest.class))).thenReturn(response);
	}
	
	private void allocateAddressMocked(Ec2Client client) {
		AllocateAddressResponse response = AllocateAddressResponse.builder()
				.allocationId(FAKE_ALLOCATION_ID)
				.build();
		
		Mockito.when(client.allocateAddress(Mockito.any(AllocateAddressRequest.class))).thenReturn(response);
	}
	
	private Address buildAddress(String instanceId) {
		String allocationId = FAKE_ALLOCATION_ID;
		Tag tagAssociationId = buildTag(AwsV2PublicIpPlugin.AWS_TAG_ASSOCIATION_ID);
		Tag tagGroupId = buildTag(AwsV2CloudUtil.AWS_TAG_GROUP_ID);
		return Address.builder()
				.allocationId(allocationId)
				.instanceId(instanceId)
				.tags(tagAssociationId, tagGroupId)
				.build();
	}
	
	private Tag buildTag(String key) {
		String value = ANOTHER_SECURITY_GROUP_ID;
		return Tag.builder()
				.key(key)
				.value(value)
				.build();
	}
	
	private DescribeInstancesResponse buildDescribeInstanceResponse(Reservation reservation) {
		return DescribeInstancesResponse.builder()
				.reservations(reservation)
				.build();
	}

	private Reservation buildReservation(Instance instance) {
		Reservation reservation;
		if (instance != null) {
			reservation = Reservation.builder()
					.instances(instance)
					.build();
		} else {
			reservation = Reservation.builder()
					.build();
		}
		return reservation;
	}

	private Instance buildInstance(String subnetId) {
		InstanceNetworkInterface instanceNetworkInterface = buildInstanceNetworkInterface(subnetId);
		
		return Instance.builder()
				.instanceId(FAKE_INSTANCE_ID)
				.networkInterfaces(instanceNetworkInterface)
				.build();
	}

	private InstanceNetworkInterface buildInstanceNetworkInterface(String subnetId) {
		InstancePrivateIpAddress instancePrivateIpAddress = buildInstancePrivateIpAddress();
		InstanceNetworkInterfaceAssociation association = buildInstanceNetworkInterfaceAssociation();
		
		return InstanceNetworkInterface.builder()
				.association(association)
				.privateIpAddresses(instancePrivateIpAddress)
				.subnetId(subnetId)
				.build();
	}

	private InstanceNetworkInterfaceAssociation buildInstanceNetworkInterfaceAssociation() {
		return InstanceNetworkInterfaceAssociation.builder()
				.publicIp(FAKE_CIDR_ADDRESS)
				.build();
	}

	private InstancePrivateIpAddress buildInstancePrivateIpAddress() {
		return InstancePrivateIpAddress.builder()
				.privateIpAddress(FAKE_CIDR_ADDRESS)
				.build();
	}
	
}
