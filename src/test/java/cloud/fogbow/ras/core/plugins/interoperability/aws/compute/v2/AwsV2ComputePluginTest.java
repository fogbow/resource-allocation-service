package cloud.fogbow.ras.core.plugins.interoperability.aws.compute.v2;

import static org.mockito.Mockito.times;

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
import cloud.fogbow.common.exceptions.NoAvailableResourcesException;
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
import software.amazon.awssdk.awscore.exception.AwsServiceException;
import software.amazon.awssdk.services.ec2.Ec2Client;
import software.amazon.awssdk.services.ec2.model.BlockDeviceMapping;
import software.amazon.awssdk.services.ec2.model.CpuOptions;
import software.amazon.awssdk.services.ec2.model.CreateTagsRequest;
import software.amazon.awssdk.services.ec2.model.DescribeImagesRequest;
import software.amazon.awssdk.services.ec2.model.DescribeImagesResponse;
import software.amazon.awssdk.services.ec2.model.DescribeInstancesRequest;
import software.amazon.awssdk.services.ec2.model.DescribeInstancesResponse;
import software.amazon.awssdk.services.ec2.model.DescribeVolumesRequest;
import software.amazon.awssdk.services.ec2.model.DescribeVolumesResponse;
import software.amazon.awssdk.services.ec2.model.EbsBlockDevice;
import software.amazon.awssdk.services.ec2.model.EbsInstanceBlockDevice;
import software.amazon.awssdk.services.ec2.model.Ec2Exception;
import software.amazon.awssdk.services.ec2.model.Image;
import software.amazon.awssdk.services.ec2.model.Instance;
import software.amazon.awssdk.services.ec2.model.InstanceBlockDeviceMapping;
import software.amazon.awssdk.services.ec2.model.InstanceNetworkInterface;
import software.amazon.awssdk.services.ec2.model.InstancePrivateIpAddress;
import software.amazon.awssdk.services.ec2.model.InstanceState;
import software.amazon.awssdk.services.ec2.model.InstanceType;
import software.amazon.awssdk.services.ec2.model.Reservation;
import software.amazon.awssdk.services.ec2.model.RunInstancesRequest;
import software.amazon.awssdk.services.ec2.model.RunInstancesResponse;
import software.amazon.awssdk.services.ec2.model.Tag;
import software.amazon.awssdk.services.ec2.model.TerminateInstancesRequest;
import software.amazon.awssdk.services.ec2.model.Volume;

@RunWith(PowerMockRunner.class)
@PrepareForTest({ AwsV2ClientUtil.class, SharedOrderHolders.class })
public class AwsV2ComputePluginTest {

	private static final String ANY_VALUE = "anything";
	private static final String AWS_TAG_NAME = "Name";
	private static final String CLOUD_NAME = "amazon";
	private static final String FAKE_IMAGE_ID = "fake-image-id";
	private static final String FAKE_INSTANCE_ID = "fake-instance-id";
	private static final String FAKE_INSTANCE_NAME = "fake-instance-name";
	private static final String FAKE_IP_ADDRESS = "0.0.0.0";
	private static final String FAKE_PUBLIC_KEY = "fake-public-key";
	private static final String FAKE_TAG = "fake-tag";
	private static final String FAKE_USER_DATA = "fake-user-data";
	private static final String FAKE_VOLUME_ID = "fake-volume-id";

	private static final int ONE_GIGABYTE = 1024;
	private static final int AMOUNT_SSD_STORAGE = 2;
	
	private static final UserData[] FAKE_USER_DATA_ARRAY = new UserData[] {
			new UserData(FAKE_USER_DATA, CloudInitUserDataBuilder.FileType.CLOUD_CONFIG, FAKE_TAG) };

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
	
	// test case: When calling the requestInstance method, with a compute order and
	// cloud user valid, a client is invoked to run instances, returning its ID.
	@Test
	public void testRequestInstanceSuccessful() throws FogbowException {
		// set up
		Ec2Client client = Mockito.mock(Ec2Client.class);
		PowerMockito.mockStatic(AwsV2ClientUtil.class);
		BDDMockito.given(AwsV2ClientUtil.createEc2Client(Mockito.anyString(), Mockito.anyString())).willReturn(client);
		mockDescribeImagesResponse(client);

		Instance instance = Instance.builder()
				.instanceId(FAKE_INSTANCE_ID)
				.build();

		RunInstancesResponse response = RunInstancesResponse.builder()
				.instances(instance)
				.build();

		Mockito.when(client.runInstances(Mockito.any(RunInstancesRequest.class))).thenReturn(response);

		ComputeOrder computeOrder = createComputeOrder(null);
		AwsV2User cloudUser = Mockito.mock(AwsV2User.class);

		// exercise
		this.plugin.requestInstance(computeOrder, cloudUser);

		// verify

		PowerMockito.verifyStatic(AwsV2ClientUtil.class, VerificationModeFactory.times(2));
		AwsV2ClientUtil.createEc2Client(Mockito.anyString(), Mockito.anyString());

		Mockito.verify(client, Mockito.times(1)).runInstances(Mockito.any(RunInstancesRequest.class));
		Mockito.verify(client, Mockito.times(1)).createTags(Mockito.any(CreateTagsRequest.class));
		Mockito.verify(this.plugin, Mockito.times(1)).loadNetworkInterfaces(Mockito.eq(computeOrder));
		Mockito.verify(this.plugin, Mockito.times(1)).findSmallestFlavor(Mockito.eq(computeOrder),
				Mockito.any(AwsV2User.class));
	}
    
	// test case: When calling the requestInstance method with a valid compute order
	// and an error occurs when the client attempts to execute instances, an
	// UnexpectedException will be thrown.
	@Test(expected = UnexpectedException.class) // verify
	public void testRequestInstanceUnsuccessful() throws FogbowException {
		// set up
		Ec2Client client = Mockito.mock(Ec2Client.class);
		PowerMockito.mockStatic(AwsV2ClientUtil.class);
		BDDMockito.given(AwsV2ClientUtil.createEc2Client(Mockito.anyString(), Mockito.anyString())).willReturn(client);
		mockDescribeImagesResponse(client);

		RunInstancesResponse response = RunInstancesResponse.builder().build();
		Mockito.when(client.runInstances(Mockito.any(RunInstancesRequest.class))).thenReturn(response);

		Mockito.doThrow(AwsServiceException.class).when(client).runInstances(Mockito.any(RunInstancesRequest.class));

		ComputeOrder computeOrder = createComputeOrder(null);
		AwsV2User cloudUser = Mockito.mock(AwsV2User.class);

		// exercise
		this.plugin.requestInstance(computeOrder, cloudUser);
	}
	
	// test case: When calling the getInstance method, with a valid compute order
	// and cloud user, a client is called to request an instance in the cloud and
	// mount this instance.
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
		Mockito.when(client.describeInstances(Mockito.any(DescribeInstancesRequest.class)))
				.thenReturn(instanceResponse);
		Mockito.when(this.plugin.getRandomUUID()).thenReturn(FAKE_INSTANCE_ID);

		ComputeOrder computeOrder = createComputeOrder(null);

		// exercise
		this.plugin.getInstance(computeOrder, cloudUser);

		// verify
		PowerMockito.verifyStatic(AwsV2ClientUtil.class, VerificationModeFactory.times(2));
		AwsV2ClientUtil.createEc2Client(Mockito.anyString(), Mockito.anyString());

		Mockito.verify(client, Mockito.times(1)).describeInstances(Mockito.any(DescribeInstancesRequest.class));
		Mockito.verify(this.plugin, times(1)).getInstanceReservation(Mockito.any(DescribeInstancesResponse.class));
		Mockito.verify(this.plugin, times(1)).getInstanceVolumes(Mockito.any(Instance.class), Mockito.eq(client));
		Mockito.verify(this.plugin, Mockito.times(1)).mountComputeInstance(Mockito.any(Instance.class),
				Mockito.anyList());
	}
	
	// test case: When calling the getInstance method with a valid computation order
	// and an error occurs when the client attempts to describe instances, an
	// UnexpectedException will be thrown.
	@Test(expected = UnexpectedException.class) // verify
	public void testGetInstanceUnsuccessful() throws FogbowException {
		// set up
		Ec2Client client = Mockito.mock(Ec2Client.class);
		PowerMockito.mockStatic(AwsV2ClientUtil.class);
		BDDMockito.given(AwsV2ClientUtil.createEc2Client(Mockito.anyString(), Mockito.anyString())).willReturn(client);

		ComputeOrder computeOrder = createComputeOrder(null);
		AwsV2User cloudUser = Mockito.mock(AwsV2User.class);

		// exercise
		this.plugin.getInstance(computeOrder, cloudUser);
	}
	
	// test case: When calling the deleteInstance method, with a compute order and
	// cloud user valid, the instance in the cloud must be terminated.
	@Test
	public void testDeleteInstanceSuccessful() throws FogbowException {
		// set up
		Ec2Client client = Mockito.mock(Ec2Client.class);
		PowerMockito.mockStatic(AwsV2ClientUtil.class);
		BDDMockito.given(AwsV2ClientUtil.createEc2Client(Mockito.anyString(), Mockito.anyString())).willReturn(client);

		ComputeOrder computeOrder = createComputeOrder(null);
		AwsV2User cloudUser = Mockito.mock(AwsV2User.class);

		// exercise
		this.plugin.deleteInstance(computeOrder, cloudUser);

		// verify
		PowerMockito.verifyStatic(AwsV2ClientUtil.class, VerificationModeFactory.times(1));
		AwsV2ClientUtil.createEc2Client(Mockito.anyString(), Mockito.anyString());

		Mockito.verify(client, Mockito.times(1)).terminateInstances(Mockito.any(TerminateInstancesRequest.class));
	}
	
	// test case: When calling the deleteInstance method, with a compute order and
	// cloud user valid, and an error will occur, the UnexpectedException will be
	// thrown.
	@Test(expected = UnexpectedException.class) // verify
	public void testDeleteInstanceUnsuccessful() throws FogbowException {
		// set up
		Ec2Client client = Mockito.mock(Ec2Client.class);
		PowerMockito.mockStatic(AwsV2ClientUtil.class);
		BDDMockito.given(AwsV2ClientUtil.createEc2Client(Mockito.anyString(), Mockito.anyString())).willReturn(client);

		Mockito.doThrow(Ec2Exception.class).when(client)
				.terminateInstances(Mockito.any(TerminateInstancesRequest.class));

		ComputeOrder computeOrder = createComputeOrder(null);
		AwsV2User cloudUser = Mockito.mock(AwsV2User.class);

		// exercise
		this.plugin.deleteInstance(computeOrder, cloudUser);
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
	
	// test case: When calling the findSmallestFlavor method, with a compute order
	// and cloud user valid, and return the null result, the
	// NoAvailableResourcesException will be thrown.
	@Test(expected = NoAvailableResourcesException.class) // verify
	public void testFindSmallestFlavorUnsuccessful() throws InvalidParameterException, UnexpectedException,
			ConfigurationErrorException, NoAvailableResourcesException {

		// set up
		Ec2Client client = Mockito.mock(Ec2Client.class);
		PowerMockito.mockStatic(AwsV2ClientUtil.class);
		BDDMockito.given(AwsV2ClientUtil.createEc2Client(Mockito.anyString(), Mockito.anyString())).willReturn(client);

		ComputeOrder computeOrder = createComputeOrder(null);
		AwsV2User cloudUser = Mockito.mock(AwsV2User.class);
		Mockito.doNothing().when(this.plugin).updateHardwareRequirements(cloudUser);

		TreeSet<AwsHardwareRequirements> flavors = new TreeSet<AwsHardwareRequirements>();
		Map<String, String> requirements = null;
		AwsHardwareRequirements flavor = createFlavor(requirements);
		flavors.add(flavor);
		Mockito.doReturn(flavors).when(this.plugin).getFlavorsByRequirements(requirements);

		// exercise
		this.plugin.findSmallestFlavor(computeOrder, cloudUser);
	}
    
	// test case: When calling the defineInstanceName method passing the instance
	// name by parameter, it must return this same name.
	@Test
	public void testDefineInstanceNameByParameterRequested() {
		// set up
		String expected = FAKE_INSTANCE_NAME;

		// exercise
		String instanceName = this.plugin.defineInstanceName(FAKE_INSTANCE_NAME);

		// verify
		Assert.assertEquals(expected, instanceName);
	}
    
	// test case: When calling the getInstanceReservation method, without a
	// reservation object encapsulated in the response, the
	// InstanceNotFoundException will be thrown.
	@Test(expected = InstanceNotFoundException.class) // verify
	public void testGetInstanceReservationWithoutReservationObject() throws InstanceNotFoundException {
		// set up
		DescribeInstancesResponse response = DescribeInstancesResponse.builder().build();

		// exercise
		this.plugin.getInstanceReservation(response);
	}
    
	// test case: When calling the getInstanceReservation method, without an
	// instance object encapsulated in a reservation response, the
	// InstanceNotFoundException will be thrown.
	@Test(expected = InstanceNotFoundException.class) // verify
	public void testGetInstanceReservationWithoutInstanceObject() throws InstanceNotFoundException {
		// set up
		Reservation reservation = Reservation.builder().build();
		DescribeInstancesResponse response = DescribeInstancesResponse.builder().reservations(reservation).build();

		// exercise
		this.plugin.getInstanceReservation(response);
	}
	
	// test case: When calling the getFlavorsByRequirements method, with a
	// requirements map, it must filter the possibilities according to that map,
	// returning the corresponding results.
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
		int expected = AMOUNT_SSD_STORAGE;

		// exercise
		TreeSet<AwsHardwareRequirements> flavors = this.plugin.getFlavorsByRequirements(computeOrder.getRequirements());

		// verify
		PowerMockito.verifyStatic(AwsV2ClientUtil.class, VerificationModeFactory.times(1));
		AwsV2ClientUtil.createEc2Client(Mockito.anyString(), Mockito.anyString());

		Mockito.verify(this.plugin, Mockito.times(4)).filterFlavors(Mockito.any());
		Assert.assertEquals(expected, flavors.size());
	}
	
	// test case: When calling the getFlavorsByRequirements method, with a null map,
	// there will be no filter to limit the results, returning all the flavors
	// obtained in the last update.
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
		int expected = this.plugin.loadLinesFromFlavorFile().size() - 1;

		// exercise
		TreeSet<AwsHardwareRequirements> flavors = this.plugin.getFlavorsByRequirements(computeOrder.getRequirements());

		// verify
		PowerMockito.verifyStatic(AwsV2ClientUtil.class, VerificationModeFactory.times(1));
		AwsV2ClientUtil.createEc2Client(Mockito.anyString(), Mockito.anyString());

		Assert.assertEquals(expected, flavors.size());
	}
	
	// test case: When calling the getFlavorsByRequirements method, with an empty
	// map, there will be no filter to limit the results, returning all the flavors
	// obtained in the last update.
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
		int expected = this.plugin.loadLinesFromFlavorFile().size() - 1;

		// exercise
		TreeSet<AwsHardwareRequirements> flavors = this.plugin.getFlavorsByRequirements(computeOrder.getRequirements());

		// verify
		PowerMockito.verifyStatic(AwsV2ClientUtil.class, VerificationModeFactory.times(1));
		AwsV2ClientUtil.createEc2Client(Mockito.anyString(), Mockito.anyString());

		Assert.assertEquals(expected, flavors.size());
	}
		
	// test case: When calling the loadLinesFromFlavorFile method, without a valid
	// file path, the ConfigurationErrorException will be thrown.
	@Test(expected = ConfigurationErrorException.class) // verify
	public void testLoadLinesFromFlavorFileUnsuccessful() throws ConfigurationErrorException {
		// set up
		this.plugin.setFlavorsFilePath(ANY_VALUE);

		// exercise
		this.plugin.loadLinesFromFlavorFile();
	}
		
	// test case: When calling the updateHardwareRequirements method, with a valid
	// cloud user, there will be updating the set of available flavors.
	@Test
	public void testUpdateHardwareRequirementsSuccessful()
			throws ConfigurationErrorException, InvalidParameterException, UnexpectedException {

		// set up
		Ec2Client client = Mockito.mock(Ec2Client.class);
		PowerMockito.mockStatic(AwsV2ClientUtil.class);
		BDDMockito.given(AwsV2ClientUtil.createEc2Client(Mockito.anyString(), Mockito.anyString())).willReturn(client);
		mockDescribeImagesResponse(client);

		AwsV2User cloudUser = Mockito.mock(AwsV2User.class);

		int expected = this.plugin.loadLinesFromFlavorFile().size() - 1;

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
				.value(FAKE_INSTANCE_NAME)
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
	
	private AwsHardwareRequirements createFlavor(Map<String, String> requirements) {
		String name = FAKE_INSTANCE_NAME;
		String flavorId = FAKE_INSTANCE_ID;
		int cpu = 1;
		int memory = 1;
		int disk = 4;
		String imageId = FAKE_IMAGE_ID;
		return new AwsHardwareRequirements(name, flavorId, cpu, memory, disk, imageId, requirements);
	}
	
	private ComputeOrder createComputeOrder(Map<String, String> requirements) {
		int cpu = 2;
		int memory = 627;
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
