package cloud.fogbow.ras.core.plugins.interoperability.aws.attachment.v2;

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
import cloud.fogbow.ras.api.http.response.AttachmentInstance;
import cloud.fogbow.ras.constants.SystemConstants;
import cloud.fogbow.ras.core.BaseUnitTests;
import cloud.fogbow.ras.core.SharedOrderHolders;
import cloud.fogbow.ras.core.models.orders.AttachmentOrder;
import cloud.fogbow.ras.core.models.orders.ComputeOrder;
import cloud.fogbow.ras.core.models.orders.OrderState;
import cloud.fogbow.ras.core.models.orders.VolumeOrder;
import cloud.fogbow.ras.core.plugins.interoperability.aws.AwsV2ClientUtil;
import cloud.fogbow.ras.core.plugins.interoperability.aws.AwsV2StateMapper;
import software.amazon.awssdk.services.ec2.Ec2Client;
import software.amazon.awssdk.services.ec2.model.AttachVolumeRequest;
import software.amazon.awssdk.services.ec2.model.DescribeVolumesRequest;
import software.amazon.awssdk.services.ec2.model.DescribeVolumesResponse;
import software.amazon.awssdk.services.ec2.model.DetachVolumeRequest;
import software.amazon.awssdk.services.ec2.model.Ec2Exception;
import software.amazon.awssdk.services.ec2.model.Volume;
import software.amazon.awssdk.services.ec2.model.VolumeAttachment;
import software.amazon.awssdk.services.ec2.model.VolumeAttachmentState;

@RunWith(PowerMockRunner.class)
@PrepareForTest({ AwsV2ClientUtil.class, SharedOrderHolders.class })
public class AwsV2AttachmentPluginTest extends BaseUnitTests {

	private static final String ANY_VALUE = "anything";
	private static final String CLOUD_NAME = "amazon";
	private static final String EMPTY_STRING = "";

	private AwsV2AttachmentPlugin plugin;
	private SharedOrderHolders sharedOrderHolders;

	@Before
	public void setUp() {
		String awsConfFilePath = HomeDir.getPath() 
				+ SystemConstants.CLOUDS_CONFIGURATION_DIRECTORY_NAME
				+ File.separator 
				+ CLOUD_NAME 
				+ File.separator 
				+ SystemConstants.CLOUD_SPECIFICITY_CONF_FILE_NAME;

		this.plugin = Mockito.spy(new AwsV2AttachmentPlugin(awsConfFilePath));
		this.sharedOrderHolders = Mockito.mock(SharedOrderHolders.class);

		PowerMockito.mockStatic(SharedOrderHolders.class);
		BDDMockito.given(SharedOrderHolders.getInstance()).willReturn(this.sharedOrderHolders);

		Mockito.when(this.sharedOrderHolders.getOrdersList(Mockito.any(OrderState.class)))
				.thenReturn(new SynchronizedDoublyLinkedList<>());

		Mockito.when(this.sharedOrderHolders.getActiveOrdersMap()).thenReturn(new HashMap<>());
	}

	// test case: When calling the isReady method with the cloud states ATTACHED,
	// this means that the state of attachment is READY and it must return true.
	@Test
	public void testIsReadySuccessful() {
		// set up
		String cloudState = AwsV2StateMapper.ATTACHED_STATE;

		// exercise
		boolean status = this.plugin.isReady(cloudState);

		// verify
		Assert.assertTrue(status);
	}

	// test case: When calling the isReady method with the cloud states different
	// than ATTACHED, this means that the state of attachment is not READY and it
	// must return false.
	@Test
	public void testIsReadyUnsuccessful() {
		// set up
		String[] cloudStates = { AwsV2StateMapper.BUSY_STATE, AwsV2StateMapper.CREATING_STATE };

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

	// test case: When calling the requestInstance method, with an attachment order
	// and cloud user valid, a client is invoked to attach a volume to an instance.
	@Test
	public void testRequestInstanceSuccessful() throws FogbowException {
		// set up
		Ec2Client client = Mockito.mock(Ec2Client.class);
		PowerMockito.mockStatic(AwsV2ClientUtil.class);
		BDDMockito.given(AwsV2ClientUtil.createEc2Client(Mockito.anyString(), Mockito.anyString())).willReturn(client);

		AttachmentOrder attachmentOrder = createAttachmentOrder();
		AwsV2User cloudUser = Mockito.mock(AwsV2User.class);

		// exercise
		this.plugin.requestInstance(attachmentOrder, cloudUser);

		// verify
		PowerMockito.verifyStatic(AwsV2ClientUtil.class, VerificationModeFactory.times(1));
		AwsV2ClientUtil.createEc2Client(Mockito.anyString(), Mockito.anyString());

		Mockito.verify(client, Mockito.times(1)).attachVolume((Mockito.any(AttachVolumeRequest.class)));
	}

	// test case: When calling the deleteInstance method, with an attachment order
	// and cloud user valid, the volume associated with an instance will be
	// detached.
	@Test
	public void testDeleteInstanceSuccessful() throws FogbowException {
		// set up
		Ec2Client client = Mockito.mock(Ec2Client.class);
		PowerMockito.mockStatic(AwsV2ClientUtil.class);
		BDDMockito.given(AwsV2ClientUtil.createEc2Client(Mockito.anyString(), Mockito.anyString())).willReturn(client);

		AttachmentOrder attachmentOrder = createAttachmentOrder();
		AwsV2User cloudUser = Mockito.mock(AwsV2User.class);

		// exercise
		this.plugin.deleteInstance(attachmentOrder, cloudUser);

		// verify
		PowerMockito.verifyStatic(AwsV2ClientUtil.class, VerificationModeFactory.times(1));
		AwsV2ClientUtil.createEc2Client(Mockito.anyString(), Mockito.anyString());

		Mockito.verify(client, Mockito.times(1)).detachVolume((Mockito.any(DetachVolumeRequest.class)));
	}

	// test case: When calling the deleteInstance method, with an attachment order
	// and cloud user valid, and an error will occur, the UnexpectedException will
	// be thrown.
	@Test(expected = UnexpectedException.class) // verify
	public void testDeleteInstanceUnsuccessful() throws FogbowException {
		// set up
		Ec2Client client = Mockito.mock(Ec2Client.class);
		PowerMockito.mockStatic(AwsV2ClientUtil.class);
		BDDMockito.given(AwsV2ClientUtil.createEc2Client(Mockito.anyString(), Mockito.anyString())).willReturn(client);

		Mockito.doThrow(Ec2Exception.class).when(client).detachVolume(Mockito.any(DetachVolumeRequest.class));

		AttachmentOrder attachmentOrder = createAttachmentOrder();
		AwsV2User cloudUser = Mockito.mock(AwsV2User.class);

		// exercise
		this.plugin.deleteInstance(attachmentOrder, cloudUser);
	}

	// test case: When calling the getInstance method, with an attachment order and
	// cloud user valid, a client is invoked to request a volume attachment in the
	// cloud, and mount an attachment instance.
	@Test
	public void testGetInstanceSuccessful() throws FogbowException {
		// set up
		Ec2Client client = Mockito.mock(Ec2Client.class);
		PowerMockito.mockStatic(AwsV2ClientUtil.class);
		BDDMockito.given(AwsV2ClientUtil.createEc2Client(Mockito.anyString(), Mockito.anyString())).willReturn(client);

		DescribeVolumesResponse response = createVolumeResponse();
		Mockito.when(client.describeVolumes(Mockito.any(DescribeVolumesRequest.class))).thenReturn(response);

		AttachmentOrder attachmentOrder = createAttachmentOrder();
		AwsV2User cloudUser = Mockito.mock(AwsV2User.class);

		AttachmentInstance expected = createAttachmentInstance();

		// exercise
		AttachmentInstance attachmentInstance = this.plugin.getInstance(attachmentOrder, cloudUser);

		// verify
		PowerMockito.verifyStatic(AwsV2ClientUtil.class, VerificationModeFactory.times(1));
		AwsV2ClientUtil.createEc2Client(Mockito.anyString(), Mockito.anyString());

		Mockito.verify(client, Mockito.times(1)).describeVolumes(Mockito.any(DescribeVolumesRequest.class));

		Mockito.verify(this.plugin, Mockito.times(1))
				.mountAttachmentInstance(Mockito.any(DescribeVolumesResponse.class));

		Assert.assertEquals(expected, attachmentInstance);
	}

	// test case: When calling the defineDeviceNameAttached method, with a device
	// name null, it must return a default device name.
	@Test
	public void testDefineDeviceNameAttachedWithNullValue() {
		// set up
		String expected = AwsV2AttachmentPlugin.DEFAULT_DEVICE_NAME;

		// exercise
		String actual = this.plugin.defineDeviceNameAttached(null);

		// verify
		Assert.assertEquals(expected, actual);
	}
	
	// test case: When calling the defineDeviceNameAttached method, with an empty
	// device name, it must return a default device name.
	@Test
	public void testDefineDeviceNameAttachedWithEmptyString() {
		// set up
		String expected = AwsV2AttachmentPlugin.DEFAULT_DEVICE_NAME;

		// exercise
		String device = this.plugin.defineDeviceNameAttached(EMPTY_STRING);

		// verify
		Assert.assertEquals(expected, device);
	}
	
	// test case: When calling the mountAttachmentInstance method, with an empty
	// attachment list, it must return an InstanceNotFoundException.
	@Test (expected = InstanceNotFoundException.class) // verify
	public void testMountAttachmentInstanceWithoutAttachments() throws FogbowException {
		// set up
		Volume volume = Volume.builder()
				.build();

		DescribeVolumesResponse response = DescribeVolumesResponse.builder()
				.volumes(volume)
				.build();

		// exercise
		this.plugin.mountAttachmentInstance(response);
	}
	
	// test case: When calling the mountAttachmentInstance method, with an empty
	// volume list, it must return an InstanceNotFoundException.
	@Test (expected = InstanceNotFoundException.class) // verify
	public void testMountAttachmentInstanceWithoutVolumes() throws FogbowException {
		// set up
		DescribeVolumesResponse response = DescribeVolumesResponse.builder().build();

		// exercise
		this.plugin.mountAttachmentInstance(response);
	}
	
	private AttachmentInstance createAttachmentInstance() {
        String id = BaseUnitTests.FAKE_VOLUME_ID;
        String cloudState = AwsV2StateMapper.ATTACHED_STATE;
        String computeId = BaseUnitTests.FAKE_INSTANCE_ID;
        String volumeId = BaseUnitTests.FAKE_VOLUME_ID;
        String device = AwsV2AttachmentPlugin.XVDH_DEVICE_NAME;
        return new AttachmentInstance(id, cloudState, computeId, volumeId, device);
    }
	
	private DescribeVolumesResponse createVolumeResponse() {
		VolumeAttachment attachment = createVolumeAttachment();
		Volume volume = createVolume(attachment);
		return DescribeVolumesResponse.builder().volumes(volume).build();
	}

	private Volume createVolume(VolumeAttachment attachment) {
		return Volume.builder().attachments(attachment).build();
	}

	private VolumeAttachment createVolumeAttachment() {
		VolumeAttachment attachment = VolumeAttachment.builder()
				.state(VolumeAttachmentState.ATTACHED)
				.instanceId(BaseUnitTests.FAKE_INSTANCE_ID)
				.volumeId(BaseUnitTests.FAKE_VOLUME_ID)
				.device(AwsV2AttachmentPlugin.XVDH_DEVICE_NAME)
				.build();
		
		return attachment;
	}

	private AttachmentOrder createAttachmentOrder() {
		ComputeOrder computeOrder = createLocalComputeOrder();
		computeOrder.setCloudName(CLOUD_NAME);
		computeOrder.setInstanceId(BaseUnitTests.FAKE_INSTANCE_ID);
		this.sharedOrderHolders.getActiveOrdersMap().put(computeOrder.getId(), computeOrder);

		VolumeOrder volumeOrder = createLocalVolumeOrder();
		volumeOrder.setCloudName(CLOUD_NAME);
		volumeOrder.setInstanceId(BaseUnitTests.FAKE_VOLUME_ID);
		this.sharedOrderHolders.getActiveOrdersMap().put(volumeOrder.getId(), volumeOrder);

		String device = AwsV2AttachmentPlugin.XVDH_DEVICE_NAME;
		AttachmentOrder attachmentOrder = new AttachmentOrder(computeOrder.getId(),
				volumeOrder.getId(), device);
		attachmentOrder.setInstanceId(BaseUnitTests.FAKE_VOLUME_ID);
		this.sharedOrderHolders.getActiveOrdersMap().put(attachmentOrder.getId(), attachmentOrder);
		return attachmentOrder;
	}

}
