package cloud.fogbow.ras.core.plugins.interoperability.opennebula.attachment.v5_4;

import cloud.fogbow.common.exceptions.*;
import cloud.fogbow.ras.api.http.response.AttachmentInstance;
import cloud.fogbow.ras.constants.Messages;
import cloud.fogbow.ras.core.TestUtils;
import cloud.fogbow.ras.core.datastore.DatabaseManager;
import cloud.fogbow.ras.core.plugins.interoperability.opennebula.OpenNebulaBaseTests;
import cloud.fogbow.ras.core.plugins.interoperability.opennebula.XmlUnmarshaller;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;
import org.opennebula.client.Client;
import org.opennebula.client.OneResponse;
import org.opennebula.client.image.Image;
import org.opennebula.client.image.ImagePool;
import org.opennebula.client.vm.VirtualMachine;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.core.classloader.annotations.PrepareForTest;

import cloud.fogbow.common.models.CloudUser;
import cloud.fogbow.ras.core.SharedOrderHolders;
import cloud.fogbow.ras.core.models.orders.AttachmentOrder;
import cloud.fogbow.ras.core.models.orders.ComputeOrder;
import cloud.fogbow.ras.core.models.orders.VolumeOrder;
import cloud.fogbow.ras.core.plugins.interoperability.opennebula.OpenNebulaClientUtil;
import cloud.fogbow.ras.core.plugins.interoperability.opennebula.OpenNebulaStateMapper;

import static cloud.fogbow.ras.core.plugins.interoperability.opennebula.attachment.v5_4.OpenNebulaAttachmentPlugin.IMAGE_ID_PATH_FORMAT;
import static cloud.fogbow.ras.core.plugins.interoperability.opennebula.attachment.v5_4.OpenNebulaAttachmentPlugin.TARGET_PATH_FORMAT;

@PrepareForTest({DatabaseManager.class, OpenNebulaClientUtil.class, VirtualMachine.class})
public class OpenNebulaAttachmentPluginTest extends OpenNebulaBaseTests {

	private static final String FAKE_DEVICE = "hdb";
	private static final String FAKE_USER_NAME = "fake-user-name";
	private static final String FAKE_VIRTUAL_MACHINE_ID = "1";
	private static final String FAKE_VOLUME_ID = "1";
	private static final String IMAGE_STATE_READY = "READY";
	private static final String LOCAL_TOKEN_VALUE = "user:password";
	private static final String VIRTUAL_MACHINE_CONTENT = "<DISK_ID>1</DISK_ID>";

	private OpenNebulaAttachmentPlugin plugin;
	private AttachmentOrder attachmentOrder;
	private VirtualMachine virtualMachine;
	private String template;
	private OneResponse response;
	private CloudUser cloudUser;
	private Client client;

	@Before
	public void setUp() throws UnexpectedException, UnauthorizedRequestException, InstanceNotFoundException,
			InvalidParameterException {
	    super.setUp();
		this.plugin = Mockito.spy(new OpenNebulaAttachmentPlugin(this.openNebulaConfFilePath));
		this.attachmentOrder = createAttachmentOrder();

		this.virtualMachine = Mockito.mock(VirtualMachine.class);
		this.response = Mockito.mock(OneResponse.class);
		this.client = Mockito.mock(Client.class);
		this.template = this.generateAttachmentTemplate();
		this.cloudUser = this.createCloudUser();

        PowerMockito.when(OpenNebulaClientUtil.getVirtualMachine(Mockito.any(Client.class), Mockito.anyString()))
                .thenReturn(this.virtualMachine);
		Mockito.when(this.virtualMachine.info()).thenReturn(this.response);
		Mockito.when(this.response.getMessage()).thenReturn(VIRTUAL_MACHINE_CONTENT);
	}
	
	// test case: When calling the isReady method with the cloud states USED or
	// USED_PERS, this means that the state of attachment is READY and it must
	// return true.
	@Test
	public void testIsReady() {
		// set up
		String[] cloudStates = { OpenNebulaStateMapper.USED_STATE,
				OpenNebulaStateMapper.ATTACHMENT_USED_PERSISTENT_STATE };

		String cloudState;
		for (int i = 0; i < cloudStates.length; i++) {
			cloudState = cloudStates[i];

			// exercise
			boolean status = this.plugin.isReady(cloudState);

			// verify
			Assert.assertTrue(status);
		}
	}
	
	// test case: When calling the isReady method with the cloud states ERROR, this
	// means that the state of attachment is FAILED and it must return false.
	@Test
	public void testIsReadyFail() {
		// set up
		String cloudState = OpenNebulaStateMapper.DEFAULT_ERROR_STATE;

		// exercise
		boolean status = this.plugin.isReady(cloudState);

		// verify
		Assert.assertFalse(status);
	}
	
	// test case: When calling the hasFailed method with the cloud states ERROR,
	// this means that the state of attachment is FAILED and it must return true.
	@Test
	public void testHasFailed() {
		// set up
		String cloudState = OpenNebulaStateMapper.DEFAULT_ERROR_STATE;

		// exercise
		boolean status = this.plugin.hasFailed(cloudState);

		// verify
		Assert.assertTrue(status);
	}
	
	// test case: When calling the hasFailed method with the cloud states USED or
	// USED_PERS, this means that the state of attachment is READY and it must
	// return false.
	@Test
	public void testHasFailedFail() {
		// set up
		String[] cloudStates = { OpenNebulaStateMapper.USED_STATE,
				OpenNebulaStateMapper.ATTACHMENT_USED_PERSISTENT_STATE };

		String cloudState;
		for (int i = 0; i < cloudStates.length; i++) {
			cloudState = cloudStates[i];

			// exercise
			boolean status = this.plugin.hasFailed(cloudState);

			// verify
			Assert.assertFalse(status);
		}
	}
	
	// test case: When invoking the requestInstance method, with a valid client
	// and template, a virtual machine will be instantiated to attach a volume image
	// disk and return the attached disk ID
	@Test
	public void testRequestInstance() throws FogbowException {
		// set up
		Mockito.doReturn(this.virtualMachine).when(this.plugin).doRequestInstance(
				Mockito.any(Client.class),
				Mockito.eq(this.attachmentOrder.getComputeId()),
				Mockito.eq(this.attachmentOrder.getVolumeId()),
				Mockito.eq(this.template));
		Mockito.doReturn(FAKE_VOLUME_ID).when(this.plugin).getDiskIdFromContentOf(this.virtualMachine);

		// exercise
		this.plugin.requestInstance(this.attachmentOrder, this.cloudUser);

		// verify
		PowerMockito.verifyStatic(OpenNebulaClientUtil.class, Mockito.times(TestUtils.RUN_ONCE));
		OpenNebulaClientUtil.createClient(Mockito.anyString(), Mockito.eq(this.cloudUser.getToken()));

		Mockito.verify(this.plugin, Mockito.times(TestUtils.RUN_ONCE)).doRequestInstance(
				Mockito.any(Client.class), Mockito.eq(this.attachmentOrder.getComputeId()),
				Mockito.eq(this.attachmentOrder.getVolumeId()), Mockito.eq(this.template));
		Mockito.verify(this.plugin, Mockito.times(TestUtils.RUN_ONCE)).getDiskIdFromContentOf(Mockito.eq(this.virtualMachine));
	}

	// test case: When calling the doRequestInstance method with a valid template, OpenNebulaClientUtil should retrieve
	// the VM, call the diskAttach method and return a VirtualMachine object with the attached disk.
	@Test
	public void testDoRequestInstance() throws FogbowException {
		// set up
		Mockito.when(this.virtualMachine.diskAttach(this.template)).thenReturn(this.response);
		Mockito.when(this.response.isError()).thenReturn(false);

		String computeId = this.attachmentOrder.getComputeId();

		// exercise
		this.plugin.doRequestInstance(this.client, computeId, this.attachmentOrder.getVolumeId(), this.template);

        // verify
		PowerMockito.verifyStatic(OpenNebulaClientUtil.class, Mockito.times(TestUtils.RUN_ONCE));
		OpenNebulaClientUtil.getVirtualMachine(Mockito.any(Client.class), Mockito.eq(computeId));

		Mockito.verify(this.response, Mockito.times(TestUtils.RUN_ONCE)).isError();
		Mockito.verify(this.virtualMachine, Mockito.times(TestUtils.RUN_ONCE)).diskAttach(Mockito.eq(this.template));
	}

	// test case: When calling the doRequestInstance method, with an invalid instance ID
	// or an invalid token, an error will occur and an InvalidParameterException
	// will be thrown.
    @Test
	public void testDoRequestInstanceFail() throws FogbowException {
		// set up
		Mockito.when(this.virtualMachine.diskAttach(this.template)).thenReturn(this.response);
		Mockito.when(this.response.isError()).thenReturn(true);

		String fakeVolumeId = this.attachmentOrder.getVolumeId();
		String computeId = this.attachmentOrder.getComputeId();
        String expectedMessage = String.format(Messages.Error.ERROR_WHILE_ATTACHING_VOLUME, fakeVolumeId, VIRTUAL_MACHINE_CONTENT);

		try {
			// exercise
			this.plugin.doRequestInstance(this.client, computeId, fakeVolumeId, this.template);
			Assert.fail();
		} catch (InvalidParameterException e) {
			// Verify
			PowerMockito.verifyStatic(OpenNebulaClientUtil.class, Mockito.times(TestUtils.RUN_ONCE));
			OpenNebulaClientUtil.getVirtualMachine(Mockito.any(Client.class), Mockito.eq(computeId));

			Mockito.verify(this.response, Mockito.times(TestUtils.RUN_ONCE)).isError();
			Mockito.verify(this.response, Mockito.times(TestUtils.RUN_ONCE)).getMessage();
			Mockito.verify(this.virtualMachine, Mockito.times(TestUtils.RUN_ONCE)).diskAttach(Mockito.eq(this.template));

			Assert.assertEquals(expectedMessage, e.getMessage());
		}
	}

	// test case: when calling getDiskIdFromContentOf with a valid virtual machine object, the plugin
	// should unmarshall the vm xml content, extract and return the appropriate disk id.
	@Test
	public void testGetDiskIdFromContentOf() {
	    // set up
		String expectedDiskId = FAKE_VOLUME_ID;

		// exercise
		String diskId = this.plugin.getDiskIdFromContentOf(this.virtualMachine);

		// verify
		Mockito.verify(this.virtualMachine, Mockito.times(TestUtils.RUN_ONCE)).info();
		Mockito.verify(this.response, Mockito.times(TestUtils.RUN_ONCE)).getMessage();

		Assert.assertEquals(diskId, expectedDiskId);
	}

	// test case: When calling the deleteInstance method, with an attachmentOrder
	// and a valid cloudUser, the volume image disk associated to a virtual machine
	// will be detached.
	@Test
	public void testDeleteInstance() throws FogbowException {
		// set up
		int diskId = Integer.parseInt(this.attachmentOrder.getVolumeId());
		String computeId = this.attachmentOrder.getComputeId();

		Mockito.doNothing().when(this.plugin).doDeleteInstance(
				Mockito.any(Client.class), Mockito.eq(computeId), Mockito.eq(diskId));

		// exercise
		this.plugin.deleteInstance(this.attachmentOrder, this.cloudUser);

		// verify
		PowerMockito.verifyStatic(OpenNebulaClientUtil.class, Mockito.times(TestUtils.RUN_ONCE));
		OpenNebulaClientUtil.createClient(Mockito.anyString(), Mockito.eq(this.cloudUser.getToken()));

		Mockito.verify(this.plugin, Mockito.times(TestUtils.RUN_ONCE)).doDeleteInstance(
				Mockito.any(Client.class), Mockito.eq(computeId), Mockito.eq(diskId));
	}

	// test case: When calling the deleteInstance method with an nonexistent AttachmentOrder,
	// an InstanceNotFoundException will be thrown.
    @Test
	public void testDeleteInstanceFail() throws FogbowException {
		// set up
		AttachmentOrder attachmentOrder = null;

        try {
			// exercise
			this.plugin.deleteInstance(attachmentOrder, this.cloudUser);
			Assert.fail();
		} catch (InstanceNotFoundException e) {
            // verify
        	Assert.assertEquals(e.getMessage(), Messages.Exception.INSTANCE_NOT_FOUND);
		}
	}

	// test case: When calling the doDeleteInstance method, with a valid attachmentOrder
	// and client, the VM associated to the order should be retrieved and the the method diskDetach should
	// be called successfully passing the order volumeId relating to the disk image used.
	@Test
	public void testDoDeleteInstance() throws FogbowException {
		// set up
		int diskId = Integer.parseInt(this.attachmentOrder.getVolumeId());
		String computeId = this.attachmentOrder.getComputeId();

		Mockito.when(this.virtualMachine.diskDetach(diskId)).thenReturn(this.response);
		Mockito.when(this.response.isError()).thenReturn(false);

        // exercise
        this.plugin.doDeleteInstance(this.client, computeId, diskId);

        // verify
        PowerMockito.verifyStatic(OpenNebulaClientUtil.class, Mockito.times(TestUtils.RUN_ONCE));
        OpenNebulaClientUtil.getVirtualMachine(Mockito.any(Client.class), Mockito.eq(computeId));

        Mockito.verify(this.virtualMachine, Mockito.times(TestUtils.RUN_ONCE)).diskDetach(Mockito.eq(diskId));
        Mockito.verify(this.response, Mockito.times(TestUtils.RUN_ONCE)).isError();
	}

	// test case: When calling the doDeleteInstance method, with an invalid attachmentOrder
	// or cloudUser, an UnexpectedException will occur
	@Test
	public void testDoDeleteInstanceFail() throws FogbowException {
		// set up
		int diskId = Integer.parseInt(this.attachmentOrder.getVolumeId());
		String computeId = this.attachmentOrder.getComputeId();
		String expectedMessage = String.format(Messages.Error.ERROR_WHILE_DETACHING_VOLUME, diskId, VIRTUAL_MACHINE_CONTENT);

		Mockito.when(this.virtualMachine.diskDetach(diskId)).thenReturn(this.response);
		Mockito.when(this.response.isError()).thenReturn(true);

        try {
			// exercise
			this.plugin.doDeleteInstance(this.client, computeId, diskId);
			Assert.fail();
		} catch (UnexpectedException e) {
			// verify
			Assert.assertEquals(e.getMessage(), expectedMessage);
		}

		// verify
		PowerMockito.verifyStatic(OpenNebulaClientUtil.class, Mockito.times(TestUtils.RUN_ONCE));
		OpenNebulaClientUtil.getVirtualMachine(Mockito.any(Client.class), Mockito.eq(computeId));

		Mockito.verify(this.virtualMachine, Mockito.times(TestUtils.RUN_ONCE)).diskDetach(Mockito.eq(diskId));
		Mockito.verify(this.response, Mockito.times(TestUtils.RUN_ONCE)).isError();
		Mockito.verify(this.response, Mockito.times(TestUtils.RUN_ONCE)).getMessage();
	}

	// test case: When calling the getInstance method, with the instance ID and a
	// valid token, a set of images will be loaded and the specific instance of the
	// image must be loaded.
	@Test
	public void testGetInstance() throws FogbowException {
		// set up
		String fakeInstanceId = this.attachmentOrder.getInstanceId();

        Mockito.doReturn(new AttachmentInstance(fakeInstanceId)).when(this.plugin).doGetInstance(
        		Mockito.any(Client.class),
				Mockito.eq(fakeInstanceId),
				Mockito.eq(this.attachmentOrder.getComputeId()),
				Mockito.eq(this.attachmentOrder.getVolumeId()));

		// exercise
		this.plugin.getInstance(this.attachmentOrder, this.cloudUser);

		// verify
		PowerMockito.verifyStatic(OpenNebulaClientUtil.class, Mockito.times(TestUtils.RUN_ONCE));
		OpenNebulaClientUtil.createClient(Mockito.anyString(), Mockito.eq(this.cloudUser.getToken()));

		Mockito.verify(this.plugin, Mockito.times(TestUtils.RUN_ONCE)).doGetInstance(
			Mockito.any(Client.class), Mockito.eq(fakeInstanceId),
			Mockito.eq(this.attachmentOrder.getComputeId()), Mockito.eq(this.attachmentOrder.getVolumeId()));
	}

	// test case: When calling the getInstance method with a nonexistent AttachmentOrder
	// an InstanceNotFoundException will be thrown.
	@Test
	public void testGetInstanceFail() throws FogbowException {
		// set up
		AttachmentOrder attachmentOrder = null;

		// exercise
		try {
			this.plugin.getInstance(attachmentOrder, this.cloudUser);
			Assert.fail();
		} catch (InstanceNotFoundException e) {
			// verify
			Assert.assertEquals(e.getMessage(), Messages.Exception.INSTANCE_NOT_FOUND);
		}
	}

	// test case: When calling the getInstance method, with the instance ID and a
	// valid token, a set of images will be loaded and the specific instance of the
	// image must be loaded.
	@Test
	public void testDoGetInstance() throws FogbowException {
		// set up
		int diskId = 1;
		ImagePool imagePool = Mockito.mock(ImagePool.class);
		Image image = Mockito.mock(Image.class);
		String computeId = this.attachmentOrder.getComputeId();
		String volumeId = this.attachmentOrder.getVolumeId();
		String imagePath = String.format(IMAGE_ID_PATH_FORMAT, diskId);
		String targetPath = String.format(TARGET_PATH_FORMAT, diskId);

		Mockito.when(OpenNebulaClientUtil.getImagePool(Mockito.any(Client.class))).thenReturn(imagePool);
		Mockito.when(imagePool.getById(diskId)).thenReturn(image);
		Mockito.when(image.stateString()).thenReturn(IMAGE_STATE_READY);
		Mockito.when(this.virtualMachine.xpath(imagePath)).thenReturn(String.valueOf(diskId));
		Mockito.when(this.virtualMachine.xpath(targetPath)).thenReturn(FAKE_DEVICE);

		// exercise
		this.plugin.doGetInstance(this.client, this.attachmentOrder.getInstanceId(), computeId, volumeId);

		// verify
		PowerMockito.verifyStatic(OpenNebulaClientUtil.class, Mockito.times(TestUtils.RUN_ONCE));
		OpenNebulaClientUtil.getImagePool(Mockito.any(Client.class));
		PowerMockito.verifyStatic(OpenNebulaClientUtil.class, Mockito.times(TestUtils.RUN_ONCE));
		OpenNebulaClientUtil.getVirtualMachine(Mockito.any(Client.class), Mockito.eq(computeId));

		Mockito.verify(this.plugin, Mockito.times(TestUtils.RUN_ONCE)).getTargetDevice(
				Mockito.any(Client.class), Mockito.eq(computeId), Mockito.eq(volumeId));
		Mockito.verify(this.plugin, Mockito.times(TestUtils.RUN_ONCE)).getState(
				Mockito.any(Client.class), Mockito.eq(volumeId));
		Mockito.verify(imagePool, Mockito.times(TestUtils.RUN_ONCE)).getById(Mockito.eq(diskId));
		Mockito.verify(image, Mockito.times(TestUtils.RUN_ONCE)).stateString();
		Mockito.verify(this.virtualMachine, Mockito.times(TestUtils.RUN_ONCE)).xpath(Mockito.eq(imagePath));
		Mockito.verify(this.virtualMachine, Mockito.times(TestUtils.RUN_ONCE)).xpath(Mockito.eq(targetPath));
	}

	// test case: When calling the getDoInstance method with an invalid volume id then the method getState should
    // throw an InstanceNotFoundException
	@Test
	public void testDoGetInstanceGetStateFail() throws FogbowException {
		// set up
		int diskId = 1;
		ImagePool imagePool = Mockito.mock(ImagePool.class);
		String volumeId = this.attachmentOrder.getVolumeId();

		Mockito.when(OpenNebulaClientUtil.getImagePool(Mockito.any(Client.class))).thenReturn(imagePool);
		Mockito.when(imagePool.getById(diskId)).thenReturn(null);

        try {
			// exercise
			this.plugin.doGetInstance(this.client, this.attachmentOrder.getInstanceId(), this.attachmentOrder.getComputeId(),
					volumeId);
			Assert.fail();
		} catch (InstanceNotFoundException e) {
            // verify
        	Assert.assertEquals(e.getMessage(), Messages.Exception.INSTANCE_NOT_FOUND);
		}

		// verify
		PowerMockito.verifyStatic(OpenNebulaClientUtil.class, Mockito.times(TestUtils.RUN_ONCE));
		OpenNebulaClientUtil.getImagePool(Mockito.any(Client.class));

		Mockito.verify(this.plugin, Mockito.times(TestUtils.RUN_ONCE)).getState(
				Mockito.any(Client.class), Mockito.eq(volumeId));
		Mockito.verify(imagePool, Mockito.times(TestUtils.RUN_ONCE)).getById(Mockito.eq(diskId));
	}

	// test case: When calling the getDoInstance method with an invalid volume id then the method getTargetDevice should
	// throw an InstanceNotFoundException
	@Test
	public void testDoGetInstanceGetTargetDeviceFail() throws FogbowException {
		// set up
		int diskId = 1;
		String fakeDiskId = "2";
		ImagePool imagePool = Mockito.mock(ImagePool.class);
		Image image = Mockito.mock(Image.class);
		String computeId = this.attachmentOrder.getComputeId();
		String volumeId = this.attachmentOrder.getVolumeId();
		String imagePath = String.format(IMAGE_ID_PATH_FORMAT, diskId);

		Mockito.when(OpenNebulaClientUtil.getImagePool(Mockito.any(Client.class))).thenReturn(imagePool);
		Mockito.when(imagePool.getById(diskId)).thenReturn(image);
		Mockito.when(image.stateString()).thenReturn(IMAGE_STATE_READY);
		Mockito.when(this.virtualMachine.xpath(Mockito.anyString())).thenReturn(fakeDiskId);

        try {
			// exercise
			this.plugin.doGetInstance(this.client, this.attachmentOrder.getInstanceId(), computeId, volumeId);
			Assert.fail();
		} catch (InstanceNotFoundException e) {
			// verify
        	Assert.assertEquals(e.getMessage(), Messages.Exception.INSTANCE_NOT_FOUND);
		}

		// verify
		PowerMockito.verifyStatic(OpenNebulaClientUtil.class, Mockito.times(TestUtils.RUN_ONCE));
		OpenNebulaClientUtil.getImagePool(Mockito.any(Client.class));
		PowerMockito.verifyStatic(OpenNebulaClientUtil.class, Mockito.times(TestUtils.RUN_ONCE));
		OpenNebulaClientUtil.getVirtualMachine(Mockito.any(Client.class), Mockito.eq(computeId));

		Mockito.verify(this.plugin, Mockito.times(TestUtils.RUN_ONCE)).getTargetDevice(
				Mockito.any(Client.class), Mockito.eq(computeId), Mockito.eq(volumeId));
		Mockito.verify(this.plugin, Mockito.times(TestUtils.RUN_ONCE)).getState(
				Mockito.any(Client.class), Mockito.eq(volumeId));
		Mockito.verify(imagePool, Mockito.times(TestUtils.RUN_ONCE)).getById(Mockito.eq(diskId));
		Mockito.verify(image, Mockito.times(TestUtils.RUN_ONCE)).stateString();
		Mockito.verify(this.virtualMachine, Mockito.times(TestUtils.RUN_ONCE)).xpath(Mockito.eq(imagePath));
	}

	private String generateAttachmentTemplate() {
		String template = "<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"yes\"?>\n" + 
				"<TEMPLATE>\n" + 
				"    <DISK>\n" + 
				"        <IMAGE_ID>1</IMAGE_ID>\n" +
				"        <TARGET>hdb</TARGET>\n" +
				"    </DISK>\n" +
				"</TEMPLATE>\n";
		return template;
	}

	private AttachmentOrder createAttachmentOrder() {
		ComputeOrder computeOrder = this.testUtils.createLocalComputeOrder();
		computeOrder.setInstanceId(FAKE_VIRTUAL_MACHINE_ID);
		SharedOrderHolders.getInstance().getActiveOrdersMap().put(computeOrder.getId(), computeOrder);

		VolumeOrder volumeOrder = this.testUtils.createLocalVolumeOrder();
		volumeOrder.setInstanceId(FAKE_VOLUME_ID);
		SharedOrderHolders.getInstance().getActiveOrdersMap().put(volumeOrder.getId(), volumeOrder);

		AttachmentOrder attachmentOrder = this.testUtils.createLocalAttachmentOrder(computeOrder, volumeOrder);
		return attachmentOrder;
	}

	private CloudUser createCloudUser() {
		String userId = null;
		String userName = FAKE_USER_NAME;
		String tokenValue = LOCAL_TOKEN_VALUE;
		return new CloudUser(userId, userName, tokenValue);
	}
}
