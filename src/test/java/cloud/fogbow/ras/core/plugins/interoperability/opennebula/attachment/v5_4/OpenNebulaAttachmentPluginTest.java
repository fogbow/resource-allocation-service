package cloud.fogbow.ras.core.plugins.interoperability.opennebula.attachment.v5_4;

import java.io.File;

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
import cloud.fogbow.common.exceptions.InvalidParameterException;
import cloud.fogbow.common.models.CloudUser;
import cloud.fogbow.common.models.SystemUser;
import cloud.fogbow.common.util.HomeDir;
import cloud.fogbow.ras.constants.SystemConstants;
import cloud.fogbow.ras.core.models.orders.AttachmentOrder;
import cloud.fogbow.ras.core.plugins.interoperability.opennebula.OpenNebulaClientUtil;

@RunWith(PowerMockRunner.class)
@PrepareForTest({OpenNebulaClientUtil.class, VirtualMachine.class})
public class OpenNebulaAttachmentPluginTest {

	private static final String FAKE_IMAGE_DEVICE = "fake-image-device";
	private static final String FAKE_USER_NAME = "fake-user-name";
	private static final String FAKE_INSTANCE_ID = "1 1 1";
	private static final String DEFAULT_DEVICE_PREFIX = "vd";
	private static final String IMAGE_STATE_READY = "READY";
	private static final String LOCAL_TOKEN_VALUE = "user:password";
	private static final String VIRTUAL_MACHINE_CONTENT = "<DISK_ID>1</DISK_ID>";

	private OpenNebulaAttachmentPlugin plugin;

	@Before
	public void setUp() {
		String opennebulaConfFilePath = HomeDir.getPath() + SystemConstants.CLOUDS_CONFIGURATION_DIRECTORY_NAME
				+ File.separator + SystemConstants.OPENNEBULA_CLOUD_NAME_DIRECTORY + File.separator
				+ SystemConstants.CLOUD_SPECIFICITY_CONF_FILE_NAME;

		this.plugin = Mockito.spy(new OpenNebulaAttachmentPlugin(opennebulaConfFilePath));
	}
	
	// test case: When invoking the requestInstance method, with the valid client
	// and template, a virtual machine will be instantiated to attach a volume image
	// disk, returning the attached disk ID in conjunction with the other IDs of
	// that instance.
	@Test
	public void testRequestInstanceSuccessful() throws FogbowException {
		// set up
		VirtualMachine virtualMachine = Mockito.mock(VirtualMachine.class);
		PowerMockito.mockStatic(OpenNebulaClientUtil.class);
		BDDMockito.given(OpenNebulaClientUtil.getVirtualMachine(Mockito.any(Client.class), Mockito.anyString())).willReturn(virtualMachine);

		String template = generateAttachmentTemplate();
		OneResponse response = Mockito.mock(OneResponse.class);
		Mockito.when(virtualMachine.diskAttach(Mockito.contains(template))).thenReturn(response);
		Mockito.when(response.isError()).thenReturn(false);

		Mockito.when(virtualMachine.info()).thenReturn(response);
		Mockito.when(response.getMessage()).thenReturn(VIRTUAL_MACHINE_CONTENT);

		CloudUser cloudUser = createCloudUser();
		AttachmentOrder attachmentOrder = createAttachmentOrder();
		
		// exercise
		this.plugin.requestInstance(attachmentOrder, cloudUser);

		// verify
		PowerMockito.verifyStatic(OpenNebulaClientUtil.class, VerificationModeFactory.times(1));
		OpenNebulaClientUtil.createClient(Mockito.anyString(), Mockito.anyString());

		PowerMockito.verifyStatic(OpenNebulaClientUtil.class, VerificationModeFactory.times(1));
		OpenNebulaClientUtil.getVirtualMachine(Mockito.any(Client.class), Mockito.anyString());
		
		Mockito.verify(virtualMachine, Mockito.times(1)).diskAttach(Mockito.eq(template));
		Mockito.verify(virtualMachine, Mockito.times(1)).info();
		Mockito.verify(response, Mockito.times(1)).getMessage();
	}
	
	// test case: When calling the requestInstance method, with the fake instance ID
	// or an invalid token, an error will occur and an InvalidParameterException
	// will be thrown.
	@Test(expected = InvalidParameterException.class)
	public void testRequestInstanceUnsuccessful() throws FogbowException {
		// set up
		VirtualMachine virtualMachine = Mockito.mock(VirtualMachine.class);
		PowerMockito.mockStatic(OpenNebulaClientUtil.class);
		BDDMockito.given(OpenNebulaClientUtil.getVirtualMachine(Mockito.any(Client.class), Mockito.anyString()))
				.willReturn(virtualMachine);

		String template = generateAttachmentTemplate();
		OneResponse response = Mockito.mock(OneResponse.class);
		Mockito.when(virtualMachine.diskAttach(Mockito.contains(template))).thenReturn(response);
		Mockito.when(response.isError()).thenReturn(true);

		CloudUser cloudUser = createCloudUser();
		AttachmentOrder attachmentOrder = createAttachmentOrder();

		// exercise
		this.plugin.requestInstance(attachmentOrder, cloudUser);

		// verify
		PowerMockito.verifyStatic(OpenNebulaClientUtil.class, VerificationModeFactory.times(1));
		OpenNebulaClientUtil.createClient(Mockito.anyString(), Mockito.anyString());

		PowerMockito.verifyStatic(OpenNebulaClientUtil.class, VerificationModeFactory.times(1));
		OpenNebulaClientUtil.getVirtualMachine(Mockito.any(Client.class), Mockito.anyString());

		Mockito.verify(virtualMachine, Mockito.times(1)).diskAttach(Mockito.eq(template));
	}

	// test case: When calling the deleteInstance method, with the instance ID and a
	// valid token, the volume image disk associated with a virtual machine will be
	// detached.
	@Test
	public void testDeleteInstanceSuccessful() throws FogbowException {
		// set up
		int virtualMachineId, diskId;
		virtualMachineId = diskId = 1;
		OneResponse response = Mockito.mock(OneResponse.class);
		PowerMockito.mockStatic(VirtualMachine.class);
		BDDMockito
				.given(VirtualMachine.diskDetach(Mockito.any(Client.class), Mockito.eq(virtualMachineId), Mockito.eq(diskId)))
				.willReturn(response);
		Mockito.when(response.isError()).thenReturn(false);

		CloudUser cloudUser = createCloudUser();
		String attachmentInstanceId = FAKE_INSTANCE_ID;
		
		// exercise
		this.plugin.deleteInstance(attachmentInstanceId, cloudUser);

		// verify
		PowerMockito.verifyStatic(VirtualMachine.class, VerificationModeFactory.times(1));
		VirtualMachine.diskDetach(Mockito.any(Client.class), Mockito.eq(virtualMachineId), Mockito.eq(diskId));
		Mockito.verify(response, Mockito.times(1)).isError();
	}
	
	// test case: When calling the deleteInstance method, with the fake instance ID
	// or an invalid token, an error will occur and an InvalidParameterException
	// will be thrown.
	@Test
	public void testDeleteInstanceUnsuccessful() throws FogbowException {
		// set up
		int virtualMachineId, diskId;
		virtualMachineId = diskId = 1;
		OneResponse response = Mockito.mock(OneResponse.class);
		PowerMockito.mockStatic(VirtualMachine.class);
		BDDMockito.given(VirtualMachine.diskDetach(Mockito.any(Client.class), Mockito.eq(virtualMachineId), 
				Mockito.eq(diskId))).willReturn(response);
		Mockito.when(response.isError()).thenReturn(true);

		CloudUser cloudUser = createCloudUser();
		String attachmentInstanceId = FAKE_INSTANCE_ID;
		
		// exercise
		this.plugin.deleteInstance(attachmentInstanceId, cloudUser);

		// verify
		PowerMockito.verifyStatic(VirtualMachine.class, VerificationModeFactory.times(1));
		VirtualMachine.diskDetach(Mockito.any(Client.class), Mockito.eq(virtualMachineId), Mockito.eq(diskId));
		Mockito.verify(response, Mockito.times(1)).isError();
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
		
		String imageDevice = FAKE_IMAGE_DEVICE;
		Mockito.when(image.xpath(DEFAULT_DEVICE_PREFIX)).thenReturn(imageDevice);
		Mockito.when(image.stateString()).thenReturn(IMAGE_STATE_READY);

		CloudUser cloudUser = createCloudUser();
		String attachmentInstanceId = "1 1";
		
		// exercise
		this.plugin.getInstance(attachmentInstanceId, cloudUser);

		// verify
		PowerMockito.verifyStatic(OpenNebulaClientUtil.class, VerificationModeFactory.times(1));
		OpenNebulaClientUtil.createClient(Mockito.anyString(), Mockito.anyString());
		
		PowerMockito.verifyStatic(OpenNebulaClientUtil.class, VerificationModeFactory.times(1));
		OpenNebulaClientUtil.getImagePool(Mockito.any(Client.class));
		
		Mockito.verify(imagePool, Mockito.times(1)).getById(Mockito.eq(diskId));
		Mockito.verify(image, Mockito.times(1)).xpath(Mockito.eq(DEFAULT_DEVICE_PREFIX));
		Mockito.verify(image, Mockito.times(1)).stateString();
	}
	
	private String generateAttachmentTemplate() {
		String template = "<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"yes\"?>\n" + 
				"<TEMPLATE>\n" + 
				"    <DISK>\n" + 
				"        <IMAGE_ID>1</IMAGE_ID>\n" + 
				"    </DISK>\n" + 
				"</TEMPLATE>\n";
		
		return template;
	}

	private AttachmentOrder createAttachmentOrder() {
		SystemUser systemUser = null;
		String requestingMember = null;
		String providingMember = null;
		String cloudName = null;
		String computeId = "1";
		String volumeId = "1";
		String device = null;
		
		AttachmentOrder attachmentOrder = new AttachmentOrder(
				systemUser,
				requestingMember, 
				providingMember,
				cloudName,
				computeId, 
				volumeId, 
				device);
		
		return attachmentOrder;
	}

	private CloudUser createCloudUser() {
		String userId = null;
		String userName = FAKE_USER_NAME;
		String tokenValue = LOCAL_TOKEN_VALUE;

		return new CloudUser(userId, userName, tokenValue);
	}
}
