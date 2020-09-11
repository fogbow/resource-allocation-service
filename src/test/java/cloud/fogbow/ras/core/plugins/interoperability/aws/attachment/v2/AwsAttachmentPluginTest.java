package cloud.fogbow.ras.core.plugins.interoperability.aws.attachment.v2;

import java.io.File;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.mockito.BDDMockito;
import org.mockito.Mockito;
import org.mockito.internal.verification.VerificationModeFactory;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.core.classloader.annotations.PrepareForTest;

import cloud.fogbow.common.exceptions.FogbowException;
import cloud.fogbow.common.exceptions.InstanceNotFoundException;
import cloud.fogbow.common.exceptions.InternalServerErrorException;
import cloud.fogbow.common.models.AwsV2User;
import cloud.fogbow.common.util.HomeDir;
import cloud.fogbow.ras.api.http.response.AttachmentInstance;
import cloud.fogbow.ras.constants.Messages;
import cloud.fogbow.ras.constants.SystemConstants;
import cloud.fogbow.ras.core.BaseUnitTests;
import cloud.fogbow.ras.core.SharedOrderHolders;
import cloud.fogbow.ras.core.TestUtils;
import cloud.fogbow.ras.core.datastore.DatabaseManager;
import cloud.fogbow.ras.core.models.orders.AttachmentOrder;
import cloud.fogbow.ras.core.models.orders.ComputeOrder;
import cloud.fogbow.ras.core.models.orders.VolumeOrder;
import cloud.fogbow.ras.core.plugins.interoperability.aws.AwsV2ClientUtil;
import cloud.fogbow.ras.core.plugins.interoperability.aws.AwsV2CloudUtil;
import cloud.fogbow.ras.core.plugins.interoperability.aws.AwsV2StateMapper;
import software.amazon.awssdk.core.exception.SdkClientException;
import software.amazon.awssdk.services.ec2.Ec2Client;
import software.amazon.awssdk.services.ec2.model.AttachVolumeRequest;
import software.amazon.awssdk.services.ec2.model.AttachVolumeResponse;
import software.amazon.awssdk.services.ec2.model.DescribeVolumesRequest;
import software.amazon.awssdk.services.ec2.model.DescribeVolumesResponse;
import software.amazon.awssdk.services.ec2.model.DetachVolumeRequest;
import software.amazon.awssdk.services.ec2.model.DetachVolumeResponse;
import software.amazon.awssdk.services.ec2.model.Volume;
import software.amazon.awssdk.services.ec2.model.VolumeAttachment;
import software.amazon.awssdk.services.ec2.model.VolumeAttachmentState;

@PrepareForTest({ AwsV2ClientUtil.class, AwsV2CloudUtil.class, DatabaseManager.class })
public class AwsAttachmentPluginTest extends BaseUnitTests {

    private static final String ANOTHER_DEVICE_NAME = "/dev/xvda";
    private static final String ANY_VALUE = "anything";
    private static final String CLOUD_NAME = "amazon";

    private AwsAttachmentPlugin plugin;
    private Ec2Client client;

    @Before
    public void setUp() throws FogbowException {
        this.testUtils.mockReadOrdersFromDataBase();
        String awsConfFilePath = HomeDir.getPath() 
                + SystemConstants.CLOUDS_CONFIGURATION_DIRECTORY_NAME
                + File.separator 
                + CLOUD_NAME 
                + File.separator 
                + SystemConstants.CLOUD_SPECIFICITY_CONF_FILE_NAME;

        this.plugin = Mockito.spy(new AwsAttachmentPlugin(awsConfFilePath));
        this.client = this.testUtils.getAwsMockedClient();
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
    public void testRequestInstance() throws FogbowException {
        // set up
        AttachmentOrder order = createAttachmentOrder();

        AttachVolumeRequest request = AttachVolumeRequest.builder()
                .device(order.getDevice())
                .instanceId(order.getComputeId())
                .volumeId(order.getVolumeId())
                .build();

        Mockito.doReturn(request.volumeId()).when(this.plugin).doRequestInstance(Mockito.eq(request),
                Mockito.eq(this.client));

        AwsV2User cloudUser = Mockito.mock(AwsV2User.class);
        
        // exercise
        this.plugin.requestInstance(order, cloudUser);

        // verify
        PowerMockito.verifyStatic(AwsV2ClientUtil.class, VerificationModeFactory.times(TestUtils.RUN_ONCE));
        AwsV2ClientUtil.createEc2Client(Mockito.eq(cloudUser.getToken()), Mockito.anyString());

        Mockito.verify(this.plugin, Mockito.times(TestUtils.RUN_ONCE)).doRequestInstance(Mockito.eq(request),
                Mockito.eq(this.client));
    }

    // test case: When calling the deleteInstance method, with an attachment order
    // and cloud user valid, the volume associated with an instance will be
    // detached.
    @Test
    public void testDeleteInstance() throws FogbowException {
        // set up
        AttachmentOrder order = createAttachmentOrder();

        Mockito.doNothing().when(this.plugin).doDeleteInstance(Mockito.eq(order.getVolumeId()),
                Mockito.eq(this.client));

        AwsV2User cloudUser = Mockito.mock(AwsV2User.class);

        // exercise
        this.plugin.deleteInstance(order, cloudUser);

        // verify
        PowerMockito.verifyStatic(AwsV2ClientUtil.class, VerificationModeFactory.times(TestUtils.RUN_ONCE));
        AwsV2ClientUtil.createEc2Client(Mockito.eq(cloudUser.getToken()), Mockito.anyString());

        Mockito.verify(this.plugin, Mockito.times(TestUtils.RUN_ONCE)).doDeleteInstance(Mockito.eq(order.getVolumeId()),
                Mockito.eq(this.client));
    }

    // test case: When calling the getInstance method, with an attachment order and
    // cloud user valid, a client is invoked to request a volume attachment in the
    // cloud, and build an attachment instance.
    @Test
    public void testGetInstance() throws FogbowException {
        // set up
        AttachmentOrder order = createAttachmentOrder();

        AttachmentInstance instance = createAttachmentInstance();
        Mockito.doReturn(instance).when(this.plugin).doGetInstance(Mockito.eq(order.getInstanceId()),
                Mockito.eq(this.client));

        AwsV2User cloudUser = Mockito.mock(AwsV2User.class);

        // exercise
        this.plugin.getInstance(order, cloudUser);

        // verify
        PowerMockito.verifyStatic(AwsV2ClientUtil.class, VerificationModeFactory.times(TestUtils.RUN_ONCE));
        AwsV2ClientUtil.createEc2Client(Mockito.eq(cloudUser.getToken()), Mockito.anyString());

        Mockito.verify(this.plugin, Mockito.times(TestUtils.RUN_ONCE)).doGetInstance(Mockito.eq(order.getInstanceId()),
                Mockito.eq(this.client));
    }
    
    // test case: When calling the doGetInstance method, it must verify
    // that is call was successful.
    @Test
    public void testDoGetInstance() throws FogbowException {
        // set up
        String attachmentId = TestUtils.FAKE_INSTANCE_ID;

        DescribeVolumesRequest request = DescribeVolumesRequest.builder()
                .volumeIds(attachmentId)
                .build();
        
        DescribeVolumesResponse response = createVolumeResponse();
        PowerMockito.mockStatic(AwsV2CloudUtil.class);
        BDDMockito.given(AwsV2CloudUtil.doDescribeVolumesRequest(Mockito.eq(request), Mockito.eq(client)))
                .willReturn(response);
        
        AttachmentInstance instance = createAttachmentInstance();
        Mockito.doReturn(instance).when(this.plugin).buildAttachmentInstance(response);
        
        // exercise
        this.plugin.doGetInstance(attachmentId, this.client);

        // verify
        PowerMockito.verifyStatic(AwsV2CloudUtil.class, VerificationModeFactory.times(TestUtils.RUN_ONCE));
        AwsV2CloudUtil.doDescribeVolumesRequest(Mockito.eq(request), Mockito.eq(this.client));
        
        Mockito.verify(this.plugin, Mockito.times(TestUtils.RUN_ONCE))
                .buildAttachmentInstance(Mockito.any(DescribeVolumesResponse.class));
    }
    
    // test case: When calling the doDeleteInstance method, it must verify
    // that is call was successful.
    @Test
    public void testDoDeleteInstance() throws FogbowException {
        // set up
        String volumeId = TestUtils.FAKE_VOLUME_ID;

        DetachVolumeRequest request = DetachVolumeRequest.builder()
                .volumeId(volumeId)
                .build();

        DetachVolumeResponse response = DetachVolumeResponse.builder().build();
        Mockito.when(client.detachVolume(Mockito.eq(request))).thenReturn(response);

        // exercise
        this.plugin.doDeleteInstance(volumeId, this.client);

        // verify
        Mockito.verify(this.client, Mockito.times(TestUtils.RUN_ONCE)).detachVolume(Mockito.eq(request));
    }
    
    // test case: When calling the doDeleteInstance method, and an unexpected error
    // occurs, it should check if an InternalServerErrorException has been thrown.
    @Test
    public void testDoDeleteInstanceFail() {
        // set up
        String volumeId = TestUtils.FAKE_VOLUME_ID;

        DetachVolumeRequest request = DetachVolumeRequest.builder()
                .volumeId(volumeId)
                .build();

        Mockito.when(this.client.detachVolume(Mockito.eq(request))).thenThrow(SdkClientException.class);

        String expected = String.format(Messages.Log.ERROR_WHILE_REMOVING_RESOURCE_S_S,
                AwsAttachmentPlugin.RESOURCE_NAME, volumeId);
        try {
            // exercise
            this.plugin.doDeleteInstance(volumeId, this.client);
            Assert.fail();
        } catch (InternalServerErrorException e) {
            // verify
            Assert.assertEquals(expected, e.getMessage());
        }
    }
    
    // test case: When calling the buildAttachmentInstance method, it must verify
    // that is call was successful.
    @Test
    public void testBuildAttachmentInstance() throws FogbowException {
        // set up
        VolumeAttachment attachment = createVolumeAttachment();
        Volume volume = createVolume(attachment);
        
        DescribeVolumesResponse response = DescribeVolumesResponse.builder()
                .volumes(volume)
                .build();
        
        PowerMockito.mockStatic(AwsV2CloudUtil.class);
        BDDMockito.given(AwsV2CloudUtil.getVolumeFrom(Mockito.eq(response))).willReturn(volume);
        
        AttachmentInstance expected = createAttachmentInstance();
        
        // exercise
        AttachmentInstance instance = this.plugin.buildAttachmentInstance(response);
        
        // verify
        PowerMockito.verifyStatic(AwsV2CloudUtil.class, VerificationModeFactory.times(TestUtils.RUN_ONCE));
        AwsV2CloudUtil.getVolumeFrom(Mockito.eq(response));
        
        Mockito.verify(this.plugin, Mockito.times(TestUtils.RUN_ONCE)).getAttachmentBy(Mockito.eq(volume));
        
        Assert.assertEquals(expected, instance);
    }
    
    // test case: When calling the getAttachmentBy method, with an empty
    // attachment list, it must throw an InstanceNotFoundException.
    @Test
    public void testGetAttachmentByVolumeWithoutAttachments() throws FogbowException {
        // set up
        Volume volume = Volume.builder().build();
        
        String expected = Messages.Exception.INSTANCE_NOT_FOUND;

        try {
            // exercise
            this.plugin.getAttachmentBy(volume);
            Assert.fail();
        } catch (InstanceNotFoundException e) {
            // verify
            Assert.assertEquals(expected, e.getMessage());
        }
    }
    
    // test case: When calling the doRequestInstance method, it must verify
    // that is call was successful.
    @Test
    public void testDoRequestInstance() throws FogbowException {
        // set up
        AttachVolumeRequest request = AttachVolumeRequest.builder()
                .device(TestUtils.FAKE_DEVICE)
                .instanceId(TestUtils.FAKE_COMPUTE_ID)
                .volumeId(TestUtils.FAKE_VOLUME_ID)
                .build();
        
        AttachVolumeResponse response = AttachVolumeResponse.builder().build();
        Mockito.when(this.client.attachVolume(request)).thenReturn(response);
        
        // exercise
        this.plugin.doRequestInstance(request, this.client);

        // verify
        Mockito.verify(this.client, Mockito.times(TestUtils.RUN_ONCE)).attachVolume(Mockito.eq(request));
    }
    
    // test case: When calling the doRequestInstance method, and an unexpected error
    // occurs, it should check if an InternalServerErrorException has been thrown.
    @Test
    public void testDoRequestInstanceFail() throws FogbowException {
        // set up
        AttachVolumeRequest request = AttachVolumeRequest.builder().build();

        SdkClientException exception = SdkClientException.builder().build();
        Mockito.when(this.client.attachVolume(request)).thenThrow(exception);

        String expected = exception.getMessage();

        try {
            // exercise
            this.plugin.doRequestInstance(request, this.client);
            Assert.fail();
        } catch (InternalServerErrorException e) {
            // verify
            Assert.assertEquals(expected, e.getMessage());
        }
    }

    // test case: When calling the getAttachedDeviceName method, with a device
    // name null, it must return a default device name.
    @Test
    public void testGetDeviceNameAttachedWithNullValue() {
        // set up
        String expected = AwsAttachmentPlugin.DEFAULT_DEVICE_NAME;

        // exercise
        String actual = this.plugin.getAttachedDeviceName(null);

        // verify
        Assert.assertEquals(expected, actual);
    }
	
    // test case: When calling the getAttachedDeviceName method, with an empty
    // device name, it must return a default device name.
    @Test
    public void testGetDeviceNameAttachedWithEmptyString() {
        // set up
        String expected = AwsAttachmentPlugin.DEFAULT_DEVICE_NAME;

        // exercise
        String device = this.plugin.getAttachedDeviceName(TestUtils.EMPTY_STRING);

        // verify
        Assert.assertEquals(expected, device);
    }
	
    private AttachmentInstance createAttachmentInstance() {
        String id = TestUtils.FAKE_VOLUME_ID;
        String cloudState = AwsV2StateMapper.ATTACHED_STATE;
        String computeId = TestUtils.FAKE_INSTANCE_ID;
        String volumeId = TestUtils.FAKE_VOLUME_ID;
        String device = ANOTHER_DEVICE_NAME;
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
                .instanceId(TestUtils.FAKE_INSTANCE_ID)
                .volumeId(TestUtils.FAKE_VOLUME_ID)
                .device(ANOTHER_DEVICE_NAME)
                .build();

        return attachment;
    }
	
    private AttachmentOrder createAttachmentOrder() {
        ComputeOrder computeOrder = this.testUtils.createLocalComputeOrder();
        computeOrder.setInstanceId(TestUtils.FAKE_COMPUTE_ID);
        SharedOrderHolders.getInstance().getActiveOrdersMap().put(computeOrder.getId(), computeOrder);

        VolumeOrder volumeOrder = this.testUtils.createLocalVolumeOrder();
        volumeOrder.setInstanceId(TestUtils.FAKE_VOLUME_ID);
        SharedOrderHolders.getInstance().getActiveOrdersMap().put(volumeOrder.getId(), volumeOrder);

        AttachmentOrder attachmentOrder = this.testUtils.createLocalAttachmentOrder(computeOrder, volumeOrder);
        return attachmentOrder;
    }

}
