package cloud.fogbow.ras.core.plugins.interoperability.aws.volume.v2;

import java.io.File;
import java.util.HashMap;

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

import cloud.fogbow.common.exceptions.FogbowException;
import cloud.fogbow.common.exceptions.InstanceNotFoundException;
import cloud.fogbow.common.exceptions.UnexpectedException;
import cloud.fogbow.common.models.AwsV2User;
import cloud.fogbow.common.models.linkedlists.SynchronizedDoublyLinkedList;
import cloud.fogbow.common.util.HomeDir;
import cloud.fogbow.ras.api.http.response.VolumeInstance;
import cloud.fogbow.ras.constants.SystemConstants;
import cloud.fogbow.ras.core.BaseUnitTests;
import cloud.fogbow.ras.core.SharedOrderHolders;
import cloud.fogbow.ras.core.models.orders.OrderState;
import cloud.fogbow.ras.core.models.orders.VolumeOrder;
import cloud.fogbow.ras.core.plugins.interoperability.aws.AwsV2ClientUtil;
import cloud.fogbow.ras.core.plugins.interoperability.aws.AwsV2StateMapper;
import software.amazon.awssdk.services.ec2.Ec2Client;
import software.amazon.awssdk.services.ec2.model.CreateTagsRequest;
import software.amazon.awssdk.services.ec2.model.CreateVolumeRequest;
import software.amazon.awssdk.services.ec2.model.CreateVolumeResponse;
import software.amazon.awssdk.services.ec2.model.DeleteVolumeRequest;
import software.amazon.awssdk.services.ec2.model.DescribeVolumesRequest;
import software.amazon.awssdk.services.ec2.model.DescribeVolumesResponse;
import software.amazon.awssdk.services.ec2.model.Ec2Exception;
import software.amazon.awssdk.services.ec2.model.Tag;
import software.amazon.awssdk.services.ec2.model.Volume;

@RunWith(PowerMockRunner.class)
@PrepareForTest({ AwsV2ClientUtil.class, SharedOrderHolders.class })
public class AwsV2VolumePluginTest extends BaseUnitTests {

	private static final String AWS_TAG_NAME = "Name";
	private static final String CLOUD_NAME = "amazon";
	private static final String FAKE_TAG_NAME = "fake-tag-name";
	private static final int ONE_GIGABYTE = 1;

	private AwsV2VolumePlugin plugin;
	private SharedOrderHolders sharedOrderHolders;
	
	@Before
	public void setUp() {
		String awsConfFilePath = HomeDir.getPath() 
				+ SystemConstants.CLOUDS_CONFIGURATION_DIRECTORY_NAME
				+ File.separator 
				+ CLOUD_NAME 
				+ File.separator 
				+ SystemConstants.CLOUD_SPECIFICITY_CONF_FILE_NAME;

		this.plugin = Mockito.spy(new AwsV2VolumePlugin(awsConfFilePath));
		this.sharedOrderHolders = Mockito.mock(SharedOrderHolders.class);

		PowerMockito.mockStatic(SharedOrderHolders.class);
		BDDMockito.given(SharedOrderHolders.getInstance()).willReturn(this.sharedOrderHolders);

		Mockito.when(this.sharedOrderHolders.getOrdersList(Mockito.any(OrderState.class)))
				.thenReturn(new SynchronizedDoublyLinkedList<>());

		Mockito.when(this.sharedOrderHolders.getActiveOrdersMap()).thenReturn(new HashMap<>());
	}
	
	// test case: When calling the isReady method with the cloud states AVAILABLE or
	// IN-USE, this means that the state of volume is READY and it must return true.
	@Test
	public void testIsReadySuccessful() {
		// set up
		String[] cloudStates = { AwsV2StateMapper.AVAILABLE_STATE, AwsV2StateMapper.IN_USE_STATE };

		String cloudState;
		for (int i = 0; i < cloudStates.length; i++) {
			cloudState = cloudStates[i];

			// exercise
			boolean status = this.plugin.isReady(cloudState);

			// verify
			Assert.assertTrue(status);
		}
	}

	// test case: When calling the isReady method with the cloud states different
	// than AVAILABLE or IN USE, this means that the state of volume is not READY
	// and it must return false.
	@Test
	public void testIsReadyUnsuccessful() {
		// set up
		String[] cloudStates = { AwsV2StateMapper.CREATING_STATE, AwsV2StateMapper.DELETING_STATE,
				AwsV2StateMapper.ERROR_STATE };

		for (String cloudState : cloudStates) {
			// exercise
			boolean status = this.plugin.isReady(cloudState);

			// verify
			Assert.assertFalse(status);
		}
	}

	// test case: When calling the hasFailed method with the cloud states ERROR,
	// this means that the state of volume is FAILED and it must return true.
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
	// than ERROR, this means that the state of attachment is not FAILED and it must
	// return false.
	@Test
	public void testHasFailedUnsuccessful() {
		// set up
		String[] cloudStates = { AwsV2StateMapper.AVAILABLE_STATE, AwsV2StateMapper.BUSY_STATE,
				AwsV2StateMapper.CREATING_STATE, AwsV2StateMapper.IN_USE_STATE };

		for (String cloudState : cloudStates) {
			// exercise
			boolean status = this.plugin.hasFailed(cloudState);

			// verify
			Assert.assertFalse(status);
		}
	}
	
	// test case: When calling the requestInstance method, with a volume order
	// and cloud user valid, a client is invoked to create a volume instance,
	// returning the volume ID.
	@Test
	public void testRequestInstanceSuccessful() throws FogbowException {
		// set up
		Ec2Client client = Mockito.mock(Ec2Client.class);
		PowerMockito.mockStatic(AwsV2ClientUtil.class);
		BDDMockito.given(AwsV2ClientUtil.createEc2Client(Mockito.anyString(), Mockito.anyString())).willReturn(client);

		CreateVolumeResponse response = CreateVolumeResponse.builder()
				.volumeId(BaseUnitTests.FAKE_VOLUME_ID)
				.build();
		
		Mockito.when(client.createVolume(Mockito.any(CreateVolumeRequest.class))).thenReturn(response);

		VolumeOrder volumeOrder = createVolumeOrder();
		AwsV2User cloudUser = Mockito.mock(AwsV2User.class);

		String expected = BaseUnitTests.FAKE_VOLUME_ID;

		// exercise
		String volumeId = this.plugin.requestInstance(volumeOrder, cloudUser);

		// verify
		PowerMockito.verifyStatic(AwsV2ClientUtil.class, VerificationModeFactory.times(1));
		AwsV2ClientUtil.createEc2Client(Mockito.anyString(), Mockito.anyString());

		Mockito.verify(client, Mockito.times(1)).createVolume(Mockito.any(CreateVolumeRequest.class));
		Mockito.verify(client, Mockito.times(1)).createTags(Mockito.any(CreateTagsRequest.class));

		Assert.assertEquals(expected, volumeId);
	}

	// test case: When calling the getInstance method, with a volume order and
	// cloud user valid, a client is invoked to request a volume in the cloud,
	// and mount a volume instance.
	@Test
	public void testGetInstanceSuccessful() throws FogbowException {
		// set up
		Ec2Client client = Mockito.mock(Ec2Client.class);
		PowerMockito.mockStatic(AwsV2ClientUtil.class);
		BDDMockito.given(AwsV2ClientUtil.createEc2Client(Mockito.anyString(), Mockito.anyString())).willReturn(client);

		DescribeVolumesResponse response = createVolumeResponse();
		Mockito.when(client.describeVolumes(Mockito.any(DescribeVolumesRequest.class))).thenReturn(response);

		VolumeOrder volumeOrder = createVolumeOrder();
		AwsV2User cloudUser = Mockito.mock(AwsV2User.class);

		VolumeInstance expected = createVolumeInstance();

		// exercise
		VolumeInstance volumeInstance = this.plugin.getInstance(volumeOrder, cloudUser);

		// verify
		PowerMockito.verifyStatic(AwsV2ClientUtil.class, VerificationModeFactory.times(1));
		AwsV2ClientUtil.createEc2Client(Mockito.anyString(), Mockito.anyString());

		Mockito.verify(client, Mockito.times(1)).describeVolumes(Mockito.any(DescribeVolumesRequest.class));
		Mockito.verify(this.plugin, Mockito.times(1)).mountVolumeInstance(Mockito.any(DescribeVolumesResponse.class));

		Assert.assertEquals(expected, volumeInstance);
	}
	
	// test case: When calling the deleteInstance method, with a volume order
	// and cloud user valid, the volume instance in the cloud must be deleted.
	@Test
	public void testDeleteInstanceSuccessful() throws FogbowException {
		// set up
		Ec2Client client = Mockito.mock(Ec2Client.class);
		PowerMockito.mockStatic(AwsV2ClientUtil.class);
		BDDMockito.given(AwsV2ClientUtil.createEc2Client(Mockito.anyString(), Mockito.anyString())).willReturn(client);

		VolumeOrder volumeOrder = createVolumeOrder();
		AwsV2User cloudUser = Mockito.mock(AwsV2User.class);

		// exercise
		this.plugin.deleteInstance(volumeOrder, cloudUser);

		// verify
		PowerMockito.verifyStatic(AwsV2ClientUtil.class, VerificationModeFactory.times(1));
		AwsV2ClientUtil.createEc2Client(Mockito.anyString(), Mockito.anyString());

		Mockito.verify(client, Mockito.times(1)).deleteVolume(Mockito.any(DeleteVolumeRequest.class));
	}
	
	// test case: When calling the deleteInstance method, with a volume order and
	// cloud user valid, and an error will occur, the UnexpectedException will be
	// thrown.
	@Test(expected = UnexpectedException.class) // verify
	public void testDeleteInstanceUnsuccessful() throws FogbowException {
		// set up
		Ec2Client client = Mockito.mock(Ec2Client.class);
		PowerMockito.mockStatic(AwsV2ClientUtil.class);
		BDDMockito.given(AwsV2ClientUtil.createEc2Client(Mockito.anyString(), Mockito.anyString())).willReturn(client);

		Mockito.doThrow(Ec2Exception.class).when(client).deleteVolume(Mockito.any(DeleteVolumeRequest.class));

		VolumeOrder attachmentOrder = createVolumeOrder();
		AwsV2User cloudUser = Mockito.mock(AwsV2User.class);

		// exercise
		this.plugin.deleteInstance(attachmentOrder, cloudUser);
	}
	
	// test case: When calling the mountVolumeInstance method, with an empty
	// volume list, it must return an InstanceNotFoundException.
	@Test(expected = InstanceNotFoundException.class) // verify
	public void testMountVolumeInstanceWithoutVolumes() throws FogbowException {
		// set up
		DescribeVolumesResponse response = DescribeVolumesResponse.builder().build();

		// exercise
		this.plugin.mountVolumeInstance(response);
	}
	
	private VolumeInstance createVolumeInstance() {
		String id = BaseUnitTests.FAKE_VOLUME_ID;
		String cloudState = AwsV2StateMapper.AVAILABLE_STATE;
		String name = SystemConstants.FOGBOW_INSTANCE_NAME_PREFIX + FAKE_VOLUME_ID;
		int volumeSize = ONE_GIGABYTE;
		return new VolumeInstance(id, cloudState, name, volumeSize);
	}

	private DescribeVolumesResponse createVolumeResponse() {
		Tag tag = Tag.builder()
				.key(AWS_TAG_NAME)
				.value(FAKE_TAG_NAME)
				.build();
		
		Volume volume = Volume.builder()
				.tags(tag)
				.volumeId(BaseUnitTests.FAKE_VOLUME_ID)
				.size(ONE_GIGABYTE)
				.build();
		
		DescribeVolumesResponse response = DescribeVolumesResponse.builder()
				.volumes(volume)
				.build();
		
		return response;
	}

	private VolumeOrder createVolumeOrder() {
		VolumeOrder volumeOrder = createLocalVolumeOrder();
		volumeOrder.setCloudName(CLOUD_NAME);
		volumeOrder.setInstanceId(BaseUnitTests.FAKE_VOLUME_ID);
		this.sharedOrderHolders.getActiveOrdersMap().put(volumeOrder.getId(), volumeOrder);
		return volumeOrder;
	}
	
}
