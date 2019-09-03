package cloud.fogbow.ras.core.plugins.interoperability.opennebula.attachment.v5_4;

import java.io.File;

import cloud.fogbow.ras.constants.Messages;
import cloud.fogbow.ras.core.BaseUnitTests;
import cloud.fogbow.ras.core.TestUtils;
import cloud.fogbow.ras.core.datastore.DatabaseManager;
import cloud.fogbow.ras.core.plugins.interoperability.cloudstack.compute.v4_9.GetVirtualMachineResponse;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.BDDMockito;
import org.mockito.Mockito;
import org.mockito.internal.verification.VerificationModeFactory;
import org.opennebula.client.Client;
import org.opennebula.client.OneResponse;
import org.opennebula.client.image.Image;
import org.opennebula.client.image.ImagePool;
import org.opennebula.client.vm.VirtualMachine;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;

import cloud.fogbow.common.exceptions.FogbowException;
import cloud.fogbow.common.exceptions.InstanceNotFoundException;
import cloud.fogbow.common.exceptions.InvalidParameterException;
import cloud.fogbow.common.exceptions.UnexpectedException;
import cloud.fogbow.common.models.CloudUser;
import cloud.fogbow.common.util.HomeDir;
import cloud.fogbow.ras.constants.SystemConstants;
import cloud.fogbow.ras.core.SharedOrderHolders;
import cloud.fogbow.ras.core.models.orders.AttachmentOrder;
import cloud.fogbow.ras.core.models.orders.ComputeOrder;
import cloud.fogbow.ras.core.models.orders.VolumeOrder;
import cloud.fogbow.ras.core.plugins.interoperability.opennebula.OpenNebulaClientUtil;
import cloud.fogbow.ras.core.plugins.interoperability.opennebula.OpenNebulaStateMapper;

@RunWith(PowerMockRunner.class)
@PrepareForTest({DatabaseManager.class, OpenNebulaClientUtil.class, SharedOrderHolders.class, VirtualMachine.class})
public class OpenNebulaAttachmentPluginTest extends BaseUnitTests {

	private static final String CLOUD_NAME = "opennebula";
	private static final String DEFAULT_DEVICE_PREFIX = "vd";
	private static final String FAKE_DEVICE = "fake-image-device";
	private static final String FAKE_USER_NAME = "fake-user-name";
	private static final String FAKE_VIRTUAL_MACHINE_ID = "1";
	private static final String FAKE_VOLUME_ID = "1";
	private static final String IMAGE_STATE_READY = "READY";
	private static final String LOCAL_TOKEN_VALUE = "user:password";
	private static final String VIRTUAL_MACHINE_CONTENT = "<DISK_ID>1</DISK_ID>";
	private static final String VIRTUAL_MACHINE_STATE_FAIL = "fail";

	private OpenNebulaAttachmentPlugin plugin;
	private SharedOrderHolders sharedOrderHolders;
	private AttachmentOrder attachmentOrder;

	@Before
	public void setUp() throws UnexpectedException {
		this.testUtils.mockReadOrdersFromDataBase();

		String opennebulaConfFilePath = HomeDir.getPath() + SystemConstants.CLOUDS_CONFIGURATION_DIRECTORY_NAME
				+ File.separator + CLOUD_NAME + File.separator
				+ SystemConstants.CLOUD_SPECIFICITY_CONF_FILE_NAME;

		this.plugin = Mockito.spy(new OpenNebulaAttachmentPlugin(opennebulaConfFilePath));
		this.attachmentOrder = createAttachmentOrder();
	}
	
	// test case: When calling the isReady method with the cloud states USED or
	// USED_PERS, this means that the state of attachment is READY and it must
	// return true.
	@Test
	public void testIsReadySuccessful() {
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
	public void testIsReadyUnsuccessful() {
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
	public void testHasFailedSuccessful() {
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
	public void testHasFailedUnsuccessful() {
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
	
	// test case: When invoking the requestInstance method, with the valid client
	// and template, a virtual machine will be instantiated to attach a volume image
	// disk, returning the attached disk ID in conjunction with the other IDs of
	// that instance.
	@Test
	public void testRequestInstance() throws FogbowException {
		// set up
		VirtualMachine virtualMachine = Mockito.mock(VirtualMachine.class);
		PowerMockito.mockStatic(OpenNebulaClientUtil.class);
		PowerMockito.when(OpenNebulaClientUtil.getVirtualMachine(Mockito.any(Client.class), Mockito.anyString()))
				.thenReturn(virtualMachine);

		String template = generateAttachmentTemplate();
		OneResponse response = Mockito.mock(OneResponse.class);
		Mockito.when(virtualMachine.diskAttach(Mockito.contains(template))).thenReturn(response);
		Mockito.when(response.isError()).thenReturn(false);

		Mockito.when(virtualMachine.info()).thenReturn(response);
		Mockito.when(response.getMessage()).thenReturn(VIRTUAL_MACHINE_CONTENT);

		CloudUser cloudUser = createCloudUser();

		// exercise
		this.plugin.requestInstance(this.attachmentOrder, cloudUser);

		// verify
		PowerMockito.verifyStatic(OpenNebulaClientUtil.class, Mockito.times(TestUtils.RUN_ONCE));
		OpenNebulaClientUtil.createClient(Mockito.anyString(), Mockito.anyString());

		PowerMockito.verifyStatic(OpenNebulaClientUtil.class, Mockito.times(TestUtils.RUN_ONCE));
		OpenNebulaClientUtil.getVirtualMachine(Mockito.any(Client.class), Mockito.anyString());

		Mockito.verify(this.plugin, Mockito.times(TestUtils.RUN_ONCE)).doRequestInstance(
				Mockito.any(Client.class), Mockito.anyString(), Mockito.eq(this.attachmentOrder.getVolumeId()), Mockito.eq(template));
		Mockito.verify(this.plugin, Mockito.times(TestUtils.RUN_ONCE)).getDiskIdFromContenOf(Mockito.eq(virtualMachine));
	}

	// test case: When calling the requestInstance method with a valid template, OpenNebulaClientUtil should retrieve
	// the VM, call the diskAttach method and return a VirtualMachine object with the attached disk.
	@Test
	public void testDoRequestInstance() throws FogbowException {
		// set up
		VirtualMachine virtualMachine = Mockito.mock(VirtualMachine.class);
		PowerMockito.mockStatic(OpenNebulaClientUtil.class);
		PowerMockito.when(OpenNebulaClientUtil.getVirtualMachine(Mockito.any(Client.class), Mockito.anyString()))
				.thenReturn(virtualMachine);

		String template = generateAttachmentTemplate();
		OneResponse response = Mockito.mock(OneResponse.class);
		Mockito.when(virtualMachine.diskAttach(Mockito.contains(template))).thenReturn(response);
		Mockito.when(response.isError()).thenReturn(false);

		Mockito.when(virtualMachine.info()).thenReturn(response);
		Mockito.when(response.getMessage()).thenReturn(VIRTUAL_MACHINE_CONTENT);

		String expectedDiskId = this.attachmentOrder.getVolumeId();

		// exercise
		this.plugin.doRequestInstance(Mockito.any(Client.class), Mockito.anyString(),
				Mockito.anyString(), Mockito.eq(template));

        // verify
		PowerMockito.verifyStatic(OpenNebulaClientUtil.class, Mockito.times(TestUtils.RUN_ONCE));
		OpenNebulaClientUtil.getVirtualMachine(Mockito.any(Client.class), Mockito.anyString());

		Mockito.verify(virtualMachine, Mockito.times(TestUtils.RUN_ONCE)).diskAttach(Mockito.eq(template));
	}

	// test case: When calling the requestInstance method, with the fake instance ID
	// or an invalid token, an error will occur and an InvalidParameterException
	// will be thrown.
    @Test
	public void testDoRequestInstanceFail() throws FogbowException {
		// set up
		VirtualMachine virtualMachine = Mockito.mock(VirtualMachine.class);

		PowerMockito.mockStatic(OpenNebulaClientUtil.class);
		PowerMockito.when(OpenNebulaClientUtil.getVirtualMachine(Mockito.any(Client.class), Mockito.anyString()))
				.thenReturn(virtualMachine);

		String template = this.generateAttachmentTemplate();
		OneResponse response = Mockito.mock(OneResponse.class);
		Mockito.when(virtualMachine.diskAttach(Mockito.anyString())).thenReturn(response);
		Mockito.when(response.isError()).thenReturn(true);

        String expected = String.format(Messages.Error.ERROR_WHILE_ATTACHING_VOLUME, "", response.getMessage());

		// exercise
		try {
			this.plugin.doRequestInstance(Mockito.any(Client.class), Mockito.anyString(), Mockito.anyString(), Mockito.eq(template));
			Assert.fail();
		} catch (InvalidParameterException e) {
			// Verify
			Assert.assertEquals(expected, e.getMessage());
		}
	}

	// test case: When calling the deleteInstance method with an AttachmentOrder
	// null, an InstanceNotFoundException will be thrown.
	@Test(expected = InstanceNotFoundException.class) // verify
	public void testDeleteInstanceThrowInstanceNotFoundException() throws FogbowException {
		// set up
		AttachmentOrder attachmentOrder = null;
		CloudUser cloudUser = createCloudUser();

		// exercise
		this.plugin.deleteInstance(attachmentOrder, cloudUser);
	}
	
	// test case: When calling the deleteInstance method, with an attachmentOrder
	// and cloudUser valid, the volume image disk associated with a virtual machine
	// will be detached.
	@Test
	public void testDeleteInstanceSuccessful() throws FogbowException {
		// set up
		String virtualMachineId = FAKE_VIRTUAL_MACHINE_ID;
		int diskId = Integer.parseInt(FAKE_VOLUME_ID);

		VirtualMachine virtualMachine = Mockito.mock(VirtualMachine.class);
		PowerMockito.mockStatic(OpenNebulaClientUtil.class);
		BDDMockito
				.given(OpenNebulaClientUtil.getVirtualMachine(Mockito.any(Client.class), Mockito.eq(virtualMachineId)))
				.willReturn(virtualMachine);

		Mockito.doReturn(true).when(this.plugin).isPowerOff(virtualMachine);

		OneResponse response = Mockito.mock(OneResponse.class);
		Mockito.when(virtualMachine.diskDetach(diskId)).thenReturn(response);
		Mockito.when(response.isError()).thenReturn(false);

		CloudUser cloudUser = createCloudUser();

		// exercise
		this.plugin.deleteInstance(attachmentOrder, cloudUser);

		// verify
		PowerMockito.verifyStatic(OpenNebulaClientUtil.class, VerificationModeFactory.times(1));
		OpenNebulaClientUtil.createClient(Mockito.anyString(), Mockito.anyString());

		PowerMockito.verifyStatic(OpenNebulaClientUtil.class, VerificationModeFactory.times(1));
		OpenNebulaClientUtil.getVirtualMachine(Mockito.any(Client.class), Mockito.eq(virtualMachineId));

		Mockito.verify(this.plugin, Mockito.times(1)).isPowerOff(virtualMachine);
		Mockito.verify(virtualMachine, Mockito.times(1)).diskDetach(diskId);
		Mockito.verify(response, Mockito.times(1)).isError();
	}
	
	// test case: When calling the deleteInstance method, with an attachmentOrder
	// and cloudUser valid, an error will occur, an error message will be thrown.
	@Test
	public void testDeleteInstanceUnsuccessful() throws FogbowException {
		// set up
		String virtualMachineId = FAKE_VIRTUAL_MACHINE_ID;
		int diskId = Integer.parseInt(FAKE_VOLUME_ID);

		VirtualMachine virtualMachine = Mockito.mock(VirtualMachine.class);
		PowerMockito.mockStatic(OpenNebulaClientUtil.class);
		BDDMockito
				.given(OpenNebulaClientUtil.getVirtualMachine(Mockito.any(Client.class), Mockito.eq(virtualMachineId)))
				.willReturn(virtualMachine);

		Mockito.doReturn(true).when(this.plugin).isPowerOff(virtualMachine);

		OneResponse response = Mockito.mock(OneResponse.class);
		Mockito.when(virtualMachine.diskDetach(diskId)).thenReturn(response);
		Mockito.when(response.isError()).thenReturn(true);

		CloudUser cloudUser = createCloudUser();

		// exercise
		this.plugin.deleteInstance(attachmentOrder, cloudUser);

		// verify
		PowerMockito.verifyStatic(OpenNebulaClientUtil.class, VerificationModeFactory.times(1));
		OpenNebulaClientUtil.createClient(Mockito.anyString(), Mockito.anyString());

		PowerMockito.verifyStatic(OpenNebulaClientUtil.class, VerificationModeFactory.times(1));
		OpenNebulaClientUtil.getVirtualMachine(Mockito.any(Client.class), Mockito.eq(virtualMachineId));

		Mockito.verify(this.plugin, Mockito.times(1)).isPowerOff(virtualMachine);
		Mockito.verify(virtualMachine, Mockito.times(1)).diskDetach(diskId);
		Mockito.verify(response, Mockito.times(1)).isError();
		Mockito.verify(response, Mockito.times(1)).getErrorMessage();
	}
	
	// test case: When calling the deleteInstance method and unable to disconnect
	// the virtual machine, to detach the resource, an UnexpectedException will be
	// thrown.
	@Test(expected = UnexpectedException.class) // verify
	public void testDeleteInstanceThrowUnexpectedException() throws FogbowException {
		// set up
		String virtualMachineId = FAKE_VIRTUAL_MACHINE_ID;

		VirtualMachine virtualMachine = Mockito.mock(VirtualMachine.class);
		PowerMockito.mockStatic(OpenNebulaClientUtil.class);
		BDDMockito
				.given(OpenNebulaClientUtil.getVirtualMachine(Mockito.any(Client.class), Mockito.eq(virtualMachineId)))
				.willReturn(virtualMachine);

		Mockito.when(virtualMachine.stateStr()).thenReturn(VIRTUAL_MACHINE_STATE_FAIL);

		CloudUser cloudUser = createCloudUser();

		// exercise
		this.plugin.deleteInstance(attachmentOrder, cloudUser);
	}
	
	// test case: When calling the getInstance method with an AttachmentOrder
	// null, an InstanceNotFoundException will be thrown.
	@Test(expected = InstanceNotFoundException.class) // verify
	public void testGetInstanceThrowInstanceNotFoundException() throws FogbowException {
		// set up
		AttachmentOrder attachmentOrder = null;
		CloudUser cloudUser = createCloudUser();

		// exercise
		this.plugin.getInstance(attachmentOrder, cloudUser);
	}
	
	// test case: When calling the getInstance method, with the instance ID and a
	// valid token, a set of images will be loaded and the specific instance of the
	// image must be loaded.
	@Test
	public void testGetInstanceSuccessful() throws FogbowException {
		// set up
		ImagePool imagePool = Mockito.mock(ImagePool.class);
		PowerMockito.mockStatic(OpenNebulaClientUtil.class);
		Mockito.when(OpenNebulaClientUtil.getImagePool(Mockito.any(Client.class))).thenReturn(imagePool);

		int diskId = 1;
		Image image = Mockito.mock(Image.class);
		Mockito.when(imagePool.getById(diskId)).thenReturn(image);

		String imageDevice = FAKE_DEVICE;
		Mockito.when(image.xpath(DEFAULT_DEVICE_PREFIX)).thenReturn(imageDevice);
		Mockito.when(image.stateString()).thenReturn(IMAGE_STATE_READY);

		CloudUser cloudUser = createCloudUser();

		// exercise
		this.plugin.getInstance(attachmentOrder, cloudUser);

		// verify
		PowerMockito.verifyStatic(OpenNebulaClientUtil.class, VerificationModeFactory.times(1));
		OpenNebulaClientUtil.createClient(Mockito.anyString(), Mockito.anyString());

		PowerMockito.verifyStatic(OpenNebulaClientUtil.class, VerificationModeFactory.times(1));
		OpenNebulaClientUtil.getImagePool(Mockito.any(Client.class));

		Mockito.verify(imagePool, Mockito.times(1)).getById(Mockito.eq(diskId));
		Mockito.verify(image, Mockito.times(1)).xpath(Mockito.eq(DEFAULT_DEVICE_PREFIX));
		Mockito.verify(image, Mockito.times(1)).stateString();
	}
	
	// test case: When calling the isPowerOff method with a valid virtual machine,
	// it must return true if the state of the virtual machine switches to
	// power-off.
	@Test
	public void testIsPowerOffSuccessful() {
		// set up
		VirtualMachine virtualMachine = Mockito.mock(VirtualMachine.class);
		Mockito.when(virtualMachine.stateStr()).thenReturn(OpenNebulaAttachmentPlugin.POWEROFF_STATE);

		// exercise
		boolean powerOff = this.plugin.isPowerOff(virtualMachine);

		// verify
		Assert.assertTrue(powerOff);
	}
	
	private String generateAttachmentTemplate() {
		String template = "<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"yes\"?>\n" + 
				"<TEMPLATE>\n" + 
				"    <DISK>\n" + 
				"        <IMAGE_ID>fake-volume-id</IMAGE_ID>\n" +
				"        <TARGET>hdb</TARGET>\n" +
				"    </DISK>\n" +
				"</TEMPLATE>\n";
		return template;
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

	private CloudUser createCloudUser() {
		String userId = null;
		String userName = FAKE_USER_NAME;
		String tokenValue = LOCAL_TOKEN_VALUE;
		return new CloudUser(userId, userName, tokenValue);
	}
}
