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
import cloud.fogbow.common.models.CloudUser;
import cloud.fogbow.common.models.linkedlists.SynchronizedDoublyLinkedList;
import cloud.fogbow.common.util.HomeDir;
import cloud.fogbow.ras.constants.SystemConstants;
import cloud.fogbow.ras.core.SharedOrderHolders;
import cloud.fogbow.ras.core.models.orders.AttachmentOrder;
import cloud.fogbow.ras.core.models.orders.ComputeOrder;
import cloud.fogbow.ras.core.models.orders.OrderState;
import cloud.fogbow.ras.core.models.orders.VolumeOrder;
import cloud.fogbow.ras.core.plugins.interoperability.aws.AwsV2ClientUtil;
import cloud.fogbow.ras.core.plugins.interoperability.aws.AwsV2StateMapper;
import software.amazon.awssdk.services.ec2.Ec2Client;
import software.amazon.awssdk.services.ec2.model.AttachVolumeRequest;
import software.amazon.awssdk.services.ec2.model.CreateTagsRequest;
import software.amazon.awssdk.services.ec2.model.Tag;

@RunWith(PowerMockRunner.class)
@PrepareForTest({AwsV2ClientUtil.class, SharedOrderHolders.class})
public class AwsV2AttachmentPluginTest {

	private static final String CLOUD_NAME = "amazon";
	private static final String FAKE_ATTACHMENT_ID = "fake-attachment-id";
	private static final String FAKE_INSTANCE_ID = "fake-instance-id";
	private static final String FAKE_VOLUME_ID = "fake-volume-id";
	
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
	
	// test case: ...
	@Test
	public void testIsReadySuccessful() {
		// set up
		String cloudState = AwsV2StateMapper.ATTACHED_STATE;

		// exercise
		boolean status = this.plugin.isReady(cloudState);

		// verify
		Assert.assertTrue(status);
	}
	
	// test case: ...
	@Test
	public void testIsReadyUnsuccessful() {
		// set up
		String cloudState = AwsV2StateMapper.DEFAULT_ERROR_STATE;

		// exercise
		boolean status = this.plugin.isReady(cloudState);

		// verify
		Assert.assertFalse(status);
	}
	
	// test case: ...
	@Test
	public void testHasFailedSuccessful() {
		// set up
		String cloudState = AwsV2StateMapper.DEFAULT_ERROR_STATE;

		// exercise
		boolean status = this.plugin.hasFailed(cloudState);

		// verify
		Assert.assertTrue(status);
	}
	
	// test case: ...
	@Test
	public void testHasFailedUnsuccessful() {
		// set up
		String cloudState = AwsV2StateMapper.ATTACHED_STATE;

		// exercise
		boolean status = this.plugin.hasFailed(cloudState);

		// verify
		Assert.assertFalse(status);
	}
	
	// test case: ...
	@Test
	public void testRequestInstanceSuccessful() throws FogbowException {
		// set up
		Ec2Client client = Mockito.mock(Ec2Client.class);
		PowerMockito.mockStatic(AwsV2ClientUtil.class);
		BDDMockito.given(AwsV2ClientUtil.createEc2Client(Mockito.anyString(), Mockito.anyString())).willReturn(client);
		
		String device = AwsV2AttachmentPlugin.DEFAULT_DEVICE_NAME;
		AttachmentOrder attachmentOrder = createAttachmentOrder(device);

		String instanceId = FAKE_INSTANCE_ID;
		String volumeId = FAKE_VOLUME_ID;
		AttachVolumeRequest attachmentRequest = AttachVolumeRequest.builder()
				.device(device)
				.instanceId(instanceId)
				.volumeId(volumeId)
				.build();
		
		Mockito.when(this.plugin.getRandomUUID()).thenReturn(FAKE_ATTACHMENT_ID);
		String expected = AwsV2AttachmentPlugin.ATTACHMENT_ID_PREFIX + FAKE_ATTACHMENT_ID;
		
		Tag tagAttachmentId = Tag.builder()
				.key(AwsV2AttachmentPlugin.ATTACHMENT_ID_TAG)
				.value(expected)
				.build();
		
		CreateTagsRequest tagRequest = CreateTagsRequest.builder()
				.resources(volumeId)
				.tags(tagAttachmentId)
				.build();
		
		Mockito.when(this.plugin.createTagAttachmentId(expected, volumeId)).thenReturn(tagRequest);

		CloudUser cloudUser = Mockito.mock(CloudUser.class);
		
		// exercise
		String attachmentId = this.plugin.requestInstance(attachmentOrder, cloudUser);
		
		// verify
		PowerMockito.verifyStatic(AwsV2ClientUtil.class, VerificationModeFactory.times(1));
		AwsV2ClientUtil.createEc2Client(Mockito.anyString(), Mockito.anyString());
		
		Mockito.verify(client, Mockito.times(1)).attachVolume((Mockito.eq(attachmentRequest)));
		Mockito.verify(client, Mockito.times(1)).createTags(Mockito.eq(tagRequest));
		
		Assert.assertEquals(expected, attachmentId);
	}
		
	// test case: ...
	@Test
	public void test() {
		// set up
		
		// exercise
		
		// verify
	}
	
	private AttachmentOrder createAttachmentOrder(String device) {
		ComputeOrder computeOrder = new ComputeOrder();
		computeOrder.setCloudName(CLOUD_NAME);
		computeOrder.setInstanceId(FAKE_INSTANCE_ID);
		computeOrder.setOrderStateInTestMode(OrderState.FULFILLED);
		this.sharedOrderHolders.getActiveOrdersMap().put(computeOrder.getId(), computeOrder);
		
		VolumeOrder volumeOrder = new VolumeOrder();
		volumeOrder.setCloudName(CLOUD_NAME);
		volumeOrder.setInstanceId(FAKE_VOLUME_ID);
		volumeOrder.setOrderStateInTestMode(OrderState.FULFILLED);
		this.sharedOrderHolders.getActiveOrdersMap().put(volumeOrder.getId(), volumeOrder);

		AttachmentOrder attachmentOrder = new AttachmentOrder(null, CLOUD_NAME, computeOrder.getId(), volumeOrder.getId(), device);
		attachmentOrder.setInstanceId(FAKE_ATTACHMENT_ID);
		this.sharedOrderHolders.getActiveOrdersMap().put(attachmentOrder.getId(), attachmentOrder);
		return attachmentOrder;
	}

}
