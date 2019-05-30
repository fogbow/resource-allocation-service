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
import org.junit.Ignore;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.BDDMockito;
import org.mockito.Mockito;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;

import cloud.fogbow.common.exceptions.ConfigurationErrorException;
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
import software.amazon.awssdk.services.ec2.model.DescribeImagesRequest;
import software.amazon.awssdk.services.ec2.model.DescribeImagesResponse;
import software.amazon.awssdk.services.ec2.model.EbsBlockDevice;
import software.amazon.awssdk.services.ec2.model.Image;

@RunWith(PowerMockRunner.class)
@PrepareForTest({ AwsV2ClientUtil.class, SharedOrderHolders.class })
public class AwsV2ComputePluginTest {

	private static final String ANY_VALUE = "anything";
	private static final String CLOUD_NAME = "amazon";
	private static final String FAKE_IMAGE_ID = "fake-image-id";
	private static final String FAKE_INSTANCE_ID = "fake-instance-id";
	private static final String FAKE_PUBLIC_KEY = "fake-public-key";
	private static final String FAKE_TAG = "fake-tag";
	private static final String FAKE_USER_DATA = "fake-user-data";
	
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
	public void testGetFlavorsByOrderRequirements()
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
		int expected = 2; // FIXME

		// exercise
		TreeSet<AwsHardwareRequirements> flavors = this.plugin.getFlavorsBy(computeOrder.getRequirements());

		// verify
		Assert.assertEquals(expected, flavors.size());
	}
		
	// test case: ...
	@Test(expected = ConfigurationErrorException.class) // verify
	public void testLoadLinesFromFlavorFileUnsuccessful() throws ConfigurationErrorException {
		// set up
		this.plugin.setFlavorsFilePath("anything");

		// exercise
		this.plugin.loadLinesFromFlavorFile();
	}
		
	// test case: ...
	@Ignore
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
