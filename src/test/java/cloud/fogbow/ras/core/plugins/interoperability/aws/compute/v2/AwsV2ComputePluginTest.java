package cloud.fogbow.ras.core.plugins.interoperability.aws.compute.v2;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeSet;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.BDDMockito;
import org.mockito.Mockito;
import org.mockito.internal.verification.VerificationModeFactory;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;

import cloud.fogbow.common.exceptions.ConfigurationErrorException;
import cloud.fogbow.common.exceptions.FogbowException;
import cloud.fogbow.common.exceptions.InstanceNotFoundException;
import cloud.fogbow.common.exceptions.InvalidParameterException;
import cloud.fogbow.common.exceptions.UnexpectedException;
import cloud.fogbow.common.models.AwsV2User;
import cloud.fogbow.common.models.SystemUser;
import cloud.fogbow.common.models.linkedlists.SynchronizedDoublyLinkedList;
import cloud.fogbow.common.util.CloudInitUserDataBuilder;
import cloud.fogbow.common.util.HomeDir;
import cloud.fogbow.ras.constants.SystemConstants;
import cloud.fogbow.ras.core.SharedOrderHolders;
import cloud.fogbow.ras.core.models.UserData;
import cloud.fogbow.ras.core.models.orders.ComputeOrder;
import cloud.fogbow.ras.core.models.orders.OrderState;
import cloud.fogbow.ras.core.plugins.interoperability.aws.AwsV2ClientUtil;
import cloud.fogbow.ras.core.plugins.interoperability.aws.AwsV2StateMapper;
import software.amazon.awssdk.services.ec2.Ec2Client;
import software.amazon.awssdk.services.ec2.model.BlockDeviceMapping;
import software.amazon.awssdk.services.ec2.model.CpuOptions;
import software.amazon.awssdk.services.ec2.model.DescribeImagesRequest;
import software.amazon.awssdk.services.ec2.model.DescribeImagesResponse;
import software.amazon.awssdk.services.ec2.model.DescribeInstancesRequest;
import software.amazon.awssdk.services.ec2.model.DescribeInstancesResponse;
import software.amazon.awssdk.services.ec2.model.DescribeVolumesRequest;
import software.amazon.awssdk.services.ec2.model.DescribeVolumesResponse;
import software.amazon.awssdk.services.ec2.model.EbsBlockDevice;
import software.amazon.awssdk.services.ec2.model.EbsInstanceBlockDevice;
import software.amazon.awssdk.services.ec2.model.Image;
import software.amazon.awssdk.services.ec2.model.Instance;
import software.amazon.awssdk.services.ec2.model.InstanceBlockDeviceMapping;
import software.amazon.awssdk.services.ec2.model.InstanceNetworkInterface;
import software.amazon.awssdk.services.ec2.model.InstancePrivateIpAddress;
import software.amazon.awssdk.services.ec2.model.InstanceState;
import software.amazon.awssdk.services.ec2.model.InstanceType;
import software.amazon.awssdk.services.ec2.model.Reservation;
import software.amazon.awssdk.services.ec2.model.Tag;
import software.amazon.awssdk.services.ec2.model.Volume;

@RunWith(PowerMockRunner.class)
@PrepareForTest({ AwsV2ClientUtil.class, SharedOrderHolders.class })
public class AwsV2ComputePluginTest {

	private static final String ANY_VALUE = "anything";
	private static final String AWS_TAG_NAME = "Name";
	private static final String CLOUD_NAME = "amazon";
	private static final String FAKE_IMAGE_ID = "fake-image-id";
	private static final String FAKE_INSTANCE_ID = "fake-instance-id";
	private static final String FAKE_IP_ADDRESS = "0.0.0.0";
	private static final String FAKE_PUBLIC_KEY = "fake-public-key";
	private static final String FAKE_TAG_NAME = "fake-tag-name";
	private static final String FAKE_USER_DATA = "fake-user-data";
	private static final String FAKE_VOLUME_ID = "fake-volume-id";

	private static final Integer ONE_GIGABYTE = 1024;
	
	private static final UserData[] FAKE_USER_DATA_ARRAY = new UserData[] {
			new UserData(FAKE_USER_DATA, CloudInitUserDataBuilder.FileType.CLOUD_CONFIG, FAKE_TAG_NAME) };

	private static final ArrayList<UserData> FAKE_LIST_USER_DATA = new ArrayList<>(Arrays.asList(FAKE_USER_DATA_ARRAY));

	private AwsV2ComputePlugin plugin;
	private SharedOrderHolders sharedOrderHolders;

	@Before
	public void setUp() {
		String awsConfFilePath = HomeDir.getPath() 
				+ SystemConstants.CLOUDS_CONFIGURATION_DIRECTORY_NAME
				+ File.separator 
				+ CLOUD_NAME 
				+ File.separator 
				+ SystemConstants.CLOUD_SPECIFICITY_CONF_FILE_NAME;

		this.plugin = Mockito.spy(new AwsV2ComputePlugin(awsConfFilePath));
		this.sharedOrderHolders = Mockito.mock(SharedOrderHolders.class);

		PowerMockito.mockStatic(SharedOrderHolders.class);
		BDDMockito.given(SharedOrderHolders.getInstance()).willReturn(this.sharedOrderHolders);

		Mockito.when(this.sharedOrderHolders.getOrdersList(Mockito.any(OrderState.class)))
				.thenReturn(new SynchronizedDoublyLinkedList<>());

		Mockito.when(this.sharedOrderHolders.getActiveOrdersMap()).thenReturn(new HashMap<>());
	}
	
	// test case: ...
	@Test
	public void testGetInstanceSuccessful() throws FogbowException {
		// set up
		Ec2Client client = Mockito.mock(Ec2Client.class);
		PowerMockito.mockStatic(AwsV2ClientUtil.class);
		BDDMockito.given(AwsV2ClientUtil.createEc2Client(Mockito.anyString(), Mockito.anyString())).willReturn(client);
		mockDescribeImagesResponse(client);
		
		AwsV2User cloudUser = Mockito.mock(AwsV2User.class);
		this.plugin.updateHardwareRequirements(cloudUser);

		DescribeVolumesResponse volumeResponse = createVolumeResponse();
		Mockito.when(client.describeVolumes(Mockito.any(DescribeVolumesRequest.class))).thenReturn(volumeResponse);
		
		DescribeInstancesResponse instanceResponse = createInstanceResponse();
		Mockito.when(client.describeInstances(Mockito.any(DescribeInstancesRequest.class))).thenReturn(instanceResponse);
		Mockito.when(this.plugin.getRandomUUID()).thenReturn(FAKE_INSTANCE_ID);

		ComputeOrder computeOrder = createComputeOrder(null);

		// exercise
		this.plugin.getInstance(computeOrder, cloudUser);

		// verify
		PowerMockito.verifyStatic(AwsV2ClientUtil.class, VerificationModeFactory.times(2));
		AwsV2ClientUtil.createEc2Client(Mockito.anyString(), Mockito.anyString());

		Mockito.verify(client, Mockito.times(1)).describeInstances(Mockito.any(DescribeInstancesRequest.class));
		Mockito.verify(this.plugin, Mockito.times(1)).mountComputeInstance(Mockito.any(DescribeInstancesRequest.class),
				Mockito.eq(client));
	}
	
	// test case: ...
    @Test(expected = InstanceNotFoundException.class)
    public void testGetInstanceReservationWithoutIReservation() throws InstanceNotFoundException {
        // set up
    	DescribeInstancesResponse response = DescribeInstancesResponse.builder().build();
    	        
        // exercise
    	this.plugin.getInstanceReservation(response);
    }
    
	// test case: ...
    @Test(expected = InstanceNotFoundException.class)
    public void testGetInstanceReservationWithoutInstance() throws InstanceNotFoundException {
        // set up
    	Reservation reservation = Reservation.builder().build();
    	DescribeInstancesResponse response = DescribeInstancesResponse.builder()
    			.reservations(reservation)
    			.build();
    	        
        // exercise
    	this.plugin.getInstanceReservation(response);
    }
	
	// test case: When calling the isReady method with the cloud state RUNNING,
	// this means that the state of compute is READY and it must return true.
	@Test
	public void testIsReadySuccessful() {
		// set up
		String cloudState = AwsV2StateMapper.RUNNING_STATE;

		// exercise
		boolean status = this.plugin.isReady(cloudState);

		// verify
		Assert.assertTrue(status);
	}

	// test case: When calling the isReady method with the cloud states different
	// than RUNNING, this means that the state of compute is not READY and it must
	// return false.
	@Test
	public void testIsReadyUnsuccessful() {
		// set up
		String[] cloudStates = { ANY_VALUE, AwsV2StateMapper.PENDING_STATE, AwsV2StateMapper.SHUTTING_DOWN_STATE,
				AwsV2StateMapper.STOPPING_STATE };

		for (String cloudState : cloudStates) {
			// exercise
			boolean status = this.plugin.isReady(cloudState);

			// verify
			Assert.assertFalse(status);
		}
	}
	
	// test case: Whenever you call the hasFailed method, no matter the value, it
	// must return false.
	@Test
	public void testHasFailed() {
		// set up
		String cloudState = ANY_VALUE;

		// exercise
		boolean status = this.plugin.hasFailed(cloudState);

		// verify
		Assert.assertFalse(status);
	}
	
	// test case: ...
	@Test
	public void testGetFlavorsByRequirementsSuccessful()
			throws InvalidParameterException, UnexpectedException, ConfigurationErrorException {

		// set up
		Ec2Client client = Mockito.mock(Ec2Client.class);
		PowerMockito.mockStatic(AwsV2ClientUtil.class);
		BDDMockito.given(AwsV2ClientUtil.createEc2Client(Mockito.anyString(), Mockito.anyString())).willReturn(client);
		mockDescribeImagesResponse(client);

		AwsV2User cloudUser = Mockito.mock(AwsV2User.class);
		this.plugin.updateHardwareRequirements(cloudUser);

		Map<String, String> requirements = new HashMap<String, String>();
		requirements.put(AwsV2ComputePlugin.STORAGE_REQUIREMENT, "1x75-SSD");
		requirements.put(AwsV2ComputePlugin.BANDWIDTH_REQUIREMENT, "<3500");
		requirements.put(AwsV2ComputePlugin.PERFORMANCE_REQUIREMENT, "<10");
		requirements.put(AwsV2ComputePlugin.PROCESSOR_REQUIREMENT, "Intel_Xeon_Platinum_3.1GHz");

		ComputeOrder computeOrder = createComputeOrder(requirements);
		int expected = 2; // FIXME replace with constant...

		// exercise
		TreeSet<AwsHardwareRequirements> flavors = this.plugin.getFlavorsByRequirements(computeOrder.getRequirements());

		// verify
		PowerMockito.verifyStatic(AwsV2ClientUtil.class, VerificationModeFactory.times(1));
		AwsV2ClientUtil.createEc2Client(Mockito.anyString(), Mockito.anyString());
		
		Assert.assertEquals(expected, flavors.size());
		
		// FIXME remove this loop...
		for (AwsHardwareRequirements flavor : flavors) {
			System.out.println(flavor.toString()); 
		}
	}
	
	// test case: ...
	@Test
	public void testGetFlavorsByRequirementsWithNullMap()
			throws InvalidParameterException, UnexpectedException, ConfigurationErrorException {

		// set up
		Ec2Client client = Mockito.mock(Ec2Client.class);
		PowerMockito.mockStatic(AwsV2ClientUtil.class);
		BDDMockito.given(AwsV2ClientUtil.createEc2Client(Mockito.anyString(), Mockito.anyString())).willReturn(client);
		mockDescribeImagesResponse(client);

		AwsV2User cloudUser = Mockito.mock(AwsV2User.class);
		this.plugin.updateHardwareRequirements(cloudUser);

		Map<String, String> requirements = null;
		ComputeOrder computeOrder = createComputeOrder(requirements);
		int expected = 59; // FIXME replace with constant...

		// exercise
		TreeSet<AwsHardwareRequirements> flavors = this.plugin.getFlavorsByRequirements(computeOrder.getRequirements());

		// verify
		PowerMockito.verifyStatic(AwsV2ClientUtil.class, VerificationModeFactory.times(1));
		AwsV2ClientUtil.createEc2Client(Mockito.anyString(), Mockito.anyString());
		
		Assert.assertEquals(expected, flavors.size());
		
		// FIXME remove this loop...
		for (AwsHardwareRequirements flavor : flavors) {
			System.out.println(flavor.toString()); 
		}
	}
	
	// test case: ...
	@Test
	public void testGetFlavorsByRequirementsWithEmptyMap()
			throws InvalidParameterException, UnexpectedException, ConfigurationErrorException {

		// set up
		Ec2Client client = Mockito.mock(Ec2Client.class);
		PowerMockito.mockStatic(AwsV2ClientUtil.class);
		BDDMockito.given(AwsV2ClientUtil.createEc2Client(Mockito.anyString(), Mockito.anyString())).willReturn(client);
		mockDescribeImagesResponse(client);

		AwsV2User cloudUser = Mockito.mock(AwsV2User.class);
		this.plugin.updateHardwareRequirements(cloudUser);

		Map<String, String> requirements = new HashMap<String, String>();
		ComputeOrder computeOrder = createComputeOrder(requirements);
		int expected = 59; // FIXME replace with constant...

		// exercise
		TreeSet<AwsHardwareRequirements> flavors = this.plugin.getFlavorsByRequirements(computeOrder.getRequirements());

		// verify
		PowerMockito.verifyStatic(AwsV2ClientUtil.class, VerificationModeFactory.times(1));
		AwsV2ClientUtil.createEc2Client(Mockito.anyString(), Mockito.anyString());
		
		Assert.assertEquals(expected, flavors.size());
		
		// FIXME remove this loop...
		for (AwsHardwareRequirements flavor : flavors) {
			System.out.println(flavor.toString()); 
		}
	}
		
	// test case: ...
	@Test(expected = ConfigurationErrorException.class) // verify
	public void testLoadLinesFromFlavorFileUnsuccessful() throws ConfigurationErrorException {
		// set up
		this.plugin.setFlavorsFilePath(ANY_VALUE);

		// exercise
		this.plugin.loadLinesFromFlavorFile();
	}
		
	// test case: ...
	@Test
	public void testUpdateHardwareRequirementsSuccessful()
			throws ConfigurationErrorException, InvalidParameterException, UnexpectedException {

		// set up
		Ec2Client client = Mockito.mock(Ec2Client.class);
		PowerMockito.mockStatic(AwsV2ClientUtil.class);
		BDDMockito.given(AwsV2ClientUtil.createEc2Client(Mockito.anyString(), Mockito.anyString())).willReturn(client);
		mockDescribeImagesResponse(client);

		AwsV2User cloudUser = Mockito.mock(AwsV2User.class);

		int expected = this.plugin.loadLinesFromFlavorFile().size() - 1; // FIXME

		// exercise
		this.plugin.updateHardwareRequirements(cloudUser);

		// verify
		Assert.assertEquals(expected, this.plugin.getFlavors().size());
	}
	
	private DescribeInstancesResponse createInstanceResponse() {
		EbsInstanceBlockDevice ebs = EbsInstanceBlockDevice.builder()
				.volumeId(FAKE_VOLUME_ID)
				.build();
		
		InstanceBlockDeviceMapping blockDeviceMapping = InstanceBlockDeviceMapping.builder()
				.ebs(ebs)
				.build();
		
		CpuOptions cpuOptions = CpuOptions.builder()
				.coreCount(1)
				.build();
		
		InstancePrivateIpAddress instancePrivateIpAddress = InstancePrivateIpAddress.builder()
				.privateIpAddress(FAKE_IP_ADDRESS)
				.build();
		
		InstanceNetworkInterface instanceNetworkInterface = InstanceNetworkInterface.builder()
				.privateIpAddresses(instancePrivateIpAddress)
				.build();
		
		InstanceState instanceState = InstanceState.builder()
				.name(AwsV2StateMapper.AVAILABLE_STATE)
				.build();
		
		Tag tag = Tag.builder()
				.key(AWS_TAG_NAME)
				.value(FAKE_TAG_NAME)
				.build();
		
		Instance instance = Instance.builder()
				.blockDeviceMappings(blockDeviceMapping)
				.cpuOptions(cpuOptions)
				.imageId(FAKE_INSTANCE_ID)
				.instanceId(FAKE_INSTANCE_ID)
				.instanceType(InstanceType.T1_MICRO)
				.networkInterfaces(instanceNetworkInterface)
				.state(instanceState)
				.tags(tag)
				.build();
		
		Reservation reservation = Reservation.builder()
				.instances(instance)
				.build();
		
		DescribeInstancesResponse response = DescribeInstancesResponse.builder()
				.reservations(reservation)
				.build();
		
		return response;
	}

//	private DescribeInstancesResponse describeInstanceResponse(Reservation reservation) {
//		DescribeInstancesResponse response = DescribeInstancesResponse.builder()
//				.reservations(reservation)
//				.build();
//		
//		return response;
//	}
//
//	private Reservation createInstanceReservation(Instance instance) {
//		Reservation reservation = Reservation.builder()
//				.instances(instance)
//				.build();
//		
//		return reservation;
//	}
	
	private DescribeVolumesResponse createVolumeResponse() {
		Volume volume = Volume.builder()
				.volumeId(FAKE_VOLUME_ID)
				.size(ONE_GIGABYTE)
				.build();
		
		DescribeVolumesResponse response = DescribeVolumesResponse.builder()
				.volumes(volume)
				.build();
		
		return response;
	}
	
	private void mockDescribeImagesResponse(Ec2Client client) {
		EbsBlockDevice ebsBlockDevice = EbsBlockDevice.builder()
				.volumeSize(AwsV2ComputePlugin.ONE_GIGABYTE)
				.build();
		
		BlockDeviceMapping blockDeviceMapping = BlockDeviceMapping.builder()
				.ebs(ebsBlockDevice)
				.build();
		
        Image image = Image.builder()
        		.imageId(FAKE_IMAGE_ID)
        		.blockDeviceMappings(blockDeviceMapping)
        		.build();
        
		DescribeImagesResponse response = DescribeImagesResponse.builder()
				.images(image)
				.build();

		Mockito.when(client.describeImages(Mockito.any(DescribeImagesRequest.class))).thenReturn(response);
	}
	
	private ComputeOrder createComputeOrder(Map<String, String> requirements) {
		int cpu = 2;
		int memory = 8;
		int disk = 8;
		
		String imageId = FAKE_IMAGE_ID;
		String name = null, providingMember = null, requestingMember = null, cloudName = null;
		String publicKey = FAKE_PUBLIC_KEY;
		
		SystemUser systemUser = null;
		List<String> networksId = null;
		ArrayList<UserData> userData = FAKE_LIST_USER_DATA;
		
		ComputeOrder computeOrder = new ComputeOrder(
				systemUser,
				requestingMember, 
				providingMember,
				cloudName,
				name, 
				cpu, 
				memory, 
				disk, 
				imageId,
				userData,
				publicKey, 
				networksId);
		
		computeOrder.setCloudName(CLOUD_NAME);
		computeOrder.setInstanceId(FAKE_INSTANCE_ID);
		computeOrder.setOrderStateInTestMode(OrderState.FULFILLED);
		computeOrder.setRequirements(requirements);
		this.sharedOrderHolders.getActiveOrdersMap().put(computeOrder.getId(), computeOrder);
		
		return computeOrder;
	}
	
}
