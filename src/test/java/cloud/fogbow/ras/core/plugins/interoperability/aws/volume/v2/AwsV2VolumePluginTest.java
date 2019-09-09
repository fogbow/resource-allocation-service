package cloud.fogbow.ras.core.plugins.interoperability.aws.volume.v2;

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
import cloud.fogbow.common.exceptions.UnexpectedException;
import cloud.fogbow.common.models.AwsV2User;
import cloud.fogbow.common.util.HomeDir;
import cloud.fogbow.ras.api.http.response.VolumeInstance;
import cloud.fogbow.ras.constants.Messages;
import cloud.fogbow.ras.constants.SystemConstants;
import cloud.fogbow.ras.core.BaseUnitTests;
import cloud.fogbow.ras.core.TestUtils;
import cloud.fogbow.ras.core.datastore.DatabaseManager;
import cloud.fogbow.ras.core.models.orders.VolumeOrder;
import cloud.fogbow.ras.core.plugins.interoperability.aws.AwsV2ClientUtil;
import cloud.fogbow.ras.core.plugins.interoperability.aws.AwsV2CloudUtil;
import cloud.fogbow.ras.core.plugins.interoperability.aws.AwsV2StateMapper;
import cloud.fogbow.ras.core.plugins.interoperability.util.FogbowCloudUtil;
import software.amazon.awssdk.core.exception.SdkClientException;
import software.amazon.awssdk.services.ec2.Ec2Client;
import software.amazon.awssdk.services.ec2.model.CreateVolumeRequest;
import software.amazon.awssdk.services.ec2.model.CreateVolumeResponse;
import software.amazon.awssdk.services.ec2.model.DeleteVolumeRequest;
import software.amazon.awssdk.services.ec2.model.DeleteVolumeResponse;
import software.amazon.awssdk.services.ec2.model.DescribeVolumesRequest;
import software.amazon.awssdk.services.ec2.model.DescribeVolumesResponse;
import software.amazon.awssdk.services.ec2.model.Tag;
import software.amazon.awssdk.services.ec2.model.Volume;

@PrepareForTest({ AwsV2ClientUtil.class, AwsV2CloudUtil.class, DatabaseManager.class, FogbowCloudUtil.class })
public class AwsV2VolumePluginTest extends BaseUnitTests {

    private static final String AWS_TAG_NAME = "Name";
    private static final String CLOUD_NAME = "amazon";
    private static final String FAKE_TAG_NAME = "fake-tag-name";
    private static final String TEST_AVAILABILITY_ZONE = "sa-east-1a";
	
    private static final int ONE_GIGABYTE = 1;

    private AwsV2VolumePlugin plugin;
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

        this.plugin = Mockito.spy(new AwsV2VolumePlugin(awsConfFilePath));
        this.client = this.testUtils.getAwsMockedClient();
    }
	
    // test case: When calling the isReady method with the cloud states AVAILABLE or
    // IN-USE, this means that the state of volume is READY and it must return true.
    @Test
    public void testIsReadySuccessful() {
        // set up
        String[] cloudStates = { AwsV2StateMapper.AVAILABLE_STATE, AwsV2StateMapper.IN_USE_STATE };

        for (String cloudState : cloudStates) {
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
    public void testRequestInstance() throws FogbowException {
        // set up
        VolumeOrder order = this.testUtils.createLocalVolumeOrder();

        String instanceName = TestUtils.FAKE_ORDER_NAME;
        PowerMockito.mockStatic(FogbowCloudUtil.class);
        BDDMockito.given(FogbowCloudUtil.defineInstanceName(Mockito.eq(order.getName()))).willReturn(instanceName);

        CreateVolumeRequest request = CreateVolumeRequest.builder().size(order.getVolumeSize())
                .availabilityZone(TEST_AVAILABILITY_ZONE).build();

        Mockito.doReturn(TestUtils.FAKE_INSTANCE_ID).when(this.plugin).doRequestInstance(Mockito.eq(request),
                Mockito.eq(instanceName), Mockito.eq(client));

        AwsV2User cloudUser = Mockito.mock(AwsV2User.class);

        // exercise
        this.plugin.requestInstance(order, cloudUser);

        // verify
        PowerMockito.verifyStatic(AwsV2ClientUtil.class, VerificationModeFactory.times(TestUtils.RUN_ONCE));
        AwsV2ClientUtil.createEc2Client(Mockito.eq(cloudUser.getToken()), Mockito.anyString());

        PowerMockito.verifyStatic(FogbowCloudUtil.class, VerificationModeFactory.times(TestUtils.RUN_ONCE));
        FogbowCloudUtil.defineInstanceName(Mockito.eq(order.getName()));

        Mockito.verify(this.plugin, Mockito.times(TestUtils.RUN_ONCE)).doRequestInstance(Mockito.eq(request),
                Mockito.eq(instanceName), Mockito.eq(this.client));
    }

    // test case: When calling the getInstance method, with a volume order and
    // cloud user valid, a client is invoked to request a volume in the cloud,
    // and build a volume instance.
    @Test
    public void testGetInstance() throws FogbowException {
        // set up
        VolumeOrder order = this.testUtils.createLocalVolumeOrder();

        VolumeInstance instance = createVolumeInstance();
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
	
    // test case: When calling the deleteInstance method, with a volume order
    // and cloud user valid, the volume instance in the cloud must be deleted.
    @Test
    public void testDeleteInstance() throws FogbowException {
        // set up
        VolumeOrder order = this.testUtils.createLocalVolumeOrder();

        Mockito.doNothing().when(this.plugin).doDeleteInstance(Mockito.eq(order.getInstanceId()),
                Mockito.eq(this.client));

        AwsV2User cloudUser = Mockito.mock(AwsV2User.class);

        // exercise
        this.plugin.deleteInstance(order, cloudUser);

        // verify
        PowerMockito.verifyStatic(AwsV2ClientUtil.class, VerificationModeFactory.times(TestUtils.RUN_ONCE));
        AwsV2ClientUtil.createEc2Client(Mockito.eq(cloudUser.getToken()), Mockito.anyString());

        Mockito.verify(this.plugin, Mockito.times(TestUtils.RUN_ONCE))
                .doDeleteInstance(Mockito.eq(order.getInstanceId()), Mockito.eq(this.client));
    }
	
    // test case: When calling the doDeleteInstance method, with a volume order and
    // cloud user valid, and an error will occur, the UnexpectedException will be
    // thrown.
    @Test
    public void testDoDeleteInstanceFail() {
        // set up
        String volumeId = TestUtils.FAKE_INSTANCE_ID;

        DeleteVolumeRequest request = DeleteVolumeRequest.builder()
                .volumeId(volumeId)
                .build();

        SdkClientException exception = SdkClientException.builder().build();
        Mockito.doThrow(exception).when(this.client).deleteVolume(Mockito.eq(request));

        String expected = String.format(Messages.Exception.GENERIC_EXCEPTION, exception);

        try {
            // exercise
            this.plugin.doDeleteInstance(volumeId, this.client);
            Assert.fail();
        } catch (UnexpectedException e) {
            // verify
            Assert.assertEquals(expected, e.getMessage());
        }
    }
	
    // test case: When calling the doDeleteInstance method, it must verify that call
    // was successful.
    @Test
    public void testDoDeleteInstance() throws FogbowException {
        // set up
        String volumeId = TestUtils.FAKE_INSTANCE_ID;

        DeleteVolumeRequest request = DeleteVolumeRequest.builder()
                .volumeId(volumeId)
                .build();

        DeleteVolumeResponse response = DeleteVolumeResponse.builder().build();
        Mockito.doReturn(response).when(this.client).deleteVolume(Mockito.eq(request));

        // exercise
        this.plugin.doDeleteInstance(volumeId, this.client);

        // verify
        Mockito.verify(this.client, Mockito.times(TestUtils.RUN_ONCE)).deleteVolume(Mockito.eq(request));
    }
    
    // test case: When calling the doGetInstance method, it must verify that call
    // was successful.
    @Test
    public void testDoGetInstance() throws FogbowException {
        // set up
        String volumeId = TestUtils.FAKE_INSTANCE_ID;

        DescribeVolumesRequest request = DescribeVolumesRequest.builder()
                .volumeIds(volumeId)
                .build();

        Volume volume = createVolume();
        DescribeVolumesResponse response = DescribeVolumesResponse.builder()
                .volumes(volume)
                .build();

        PowerMockito.mockStatic(AwsV2CloudUtil.class);
        BDDMockito.given(AwsV2CloudUtil.doDescribeVolumesRequest(Mockito.eq(request), Mockito.eq(this.client)))
                .willReturn(response);

        VolumeInstance instance = createVolumeInstance();
        Mockito.doReturn(instance).when(this.plugin).buildVolumeInstance(Mockito.eq(response));

        // exercise
        this.plugin.doGetInstance(volumeId, this.client);

        // verify
        PowerMockito.verifyStatic(AwsV2CloudUtil.class, VerificationModeFactory.times(TestUtils.RUN_ONCE));
        AwsV2CloudUtil.doDescribeVolumesRequest(Mockito.eq(request), Mockito.eq(this.client));

        Mockito.verify(this.plugin, Mockito.times(TestUtils.RUN_ONCE)).buildVolumeInstance(response);
    }
    
    // test case: When calling the buildVolumeInstance method, it must verify
    // that is call was successful.
    @Test
    public void testBuildAttachmentInstance() throws FogbowException {
        // set up
        Volume volume = createVolume();
        DescribeVolumesResponse response = DescribeVolumesResponse.builder()
                .volumes(volume)
                .build();

        PowerMockito.mockStatic(AwsV2CloudUtil.class);
        BDDMockito.given(AwsV2CloudUtil.getVolumeFrom(Mockito.eq(response))).willReturn(volume);

        VolumeInstance expected = createVolumeInstance();

        // exercise
        VolumeInstance instance = this.plugin.buildVolumeInstance(response);

        // verify
        PowerMockito.verifyStatic(AwsV2CloudUtil.class, VerificationModeFactory.times(TestUtils.RUN_ONCE));
        AwsV2CloudUtil.getVolumeFrom(Mockito.eq(response));

        Assert.assertEquals(expected, instance);
    }
    
    // test case: When calling the doRequestInstance method, with a volume order and
    // cloud user valid, and an error will occur, the UnexpectedException will be
    // thrown.
    @Test
    public void testDoRequestInstanceFail() throws FogbowException {
        // set up
        String name = TestUtils.FAKE_ORDER_NAME;

        CreateVolumeRequest request = CreateVolumeRequest.builder()
                .size(TestUtils.DISK_VALUE)
                .availabilityZone(TEST_AVAILABILITY_ZONE)
                .build();

        SdkClientException exception = SdkClientException.builder().build();
        Mockito.when(this.client.createVolume(Mockito.eq(request))).thenThrow(exception);

        String expected = String.format(Messages.Exception.GENERIC_EXCEPTION, exception);

        try {
            // exercise
            this.plugin.doRequestInstance(request, name, this.client);
            Assert.fail();
        } catch (UnexpectedException e) {
            // verify
            Assert.assertEquals(expected, e.getMessage());
        }
    }
    
    // test case: When calling the doRequestInstance method, it must verify that
    // call was successful.
    @Test
    public void testDoRequestInstance() throws Exception {
        // set up
        String instanceId = TestUtils.FAKE_INSTANCE_ID;
        String instanceName = TestUtils.FAKE_ORDER_NAME;
        String tagName = AwsV2CloudUtil.AWS_TAG_NAME;

        CreateVolumeRequest request = CreateVolumeRequest.builder()
                .size(TestUtils.DISK_VALUE)
                .availabilityZone(TEST_AVAILABILITY_ZONE)
                .build();

        CreateVolumeResponse response = CreateVolumeResponse.builder()
                .volumeId(TestUtils.FAKE_INSTANCE_ID)
                .build();

        Mockito.when(this.client.createVolume(Mockito.eq(request))).thenReturn(response);

        PowerMockito.mockStatic(AwsV2CloudUtil.class);
        PowerMockito.doCallRealMethod().when(AwsV2CloudUtil.class, TestUtils.CREATE_TAGS_REQUEST_METHOD,
                Mockito.eq(instanceId), Mockito.eq(tagName), Mockito.eq(instanceName), Mockito.eq(this.client));

        // exercise
        this.plugin.doRequestInstance(request, instanceName, this.client);

        // verify
        Mockito.verify(this.client, Mockito.times(TestUtils.RUN_ONCE)).createVolume(Mockito.eq(request));

        PowerMockito.verifyStatic(AwsV2CloudUtil.class, VerificationModeFactory.times(TestUtils.RUN_ONCE));
        AwsV2CloudUtil.createTagsRequest(Mockito.eq(instanceId), Mockito.eq(tagName), Mockito.eq(instanceName),
                Mockito.eq(this.client));
    }
	
    private VolumeInstance createVolumeInstance() {
        String id = TestUtils.FAKE_VOLUME_ID;
        String cloudState = AwsV2StateMapper.AVAILABLE_STATE;
        String name = SystemConstants.FOGBOW_INSTANCE_NAME_PREFIX + TestUtils.FAKE_VOLUME_ID;
        int volumeSize = ONE_GIGABYTE;
        return new VolumeInstance(id, cloudState, name, volumeSize);
    }

    private Volume createVolume() {
        Tag tag = Tag.builder()
                .key(AWS_TAG_NAME)
                .value(FAKE_TAG_NAME)
                .build();

        Volume volume = Volume.builder()
                .tags(tag)
                .volumeId(TestUtils.FAKE_VOLUME_ID)
                .size(ONE_GIGABYTE)
                .build();

        return volume;
    }

}
