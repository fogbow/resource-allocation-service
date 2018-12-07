package org.fogbowcloud.ras.core.plugins.interoperability.opennebula.attachment.v5_4;

import org.fogbowcloud.ras.core.exceptions.FogbowRasException;
import org.fogbowcloud.ras.core.exceptions.InvalidParameterException;
import org.fogbowcloud.ras.core.exceptions.UnexpectedException;
import org.fogbowcloud.ras.core.models.orders.AttachmentOrder;
import org.fogbowcloud.ras.core.models.tokens.FederationUserToken;
import org.fogbowcloud.ras.core.models.tokens.OpenNebulaToken;
import org.fogbowcloud.ras.core.plugins.interoperability.opennebula.OpenNebulaClientFactory;
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

@RunWith(PowerMockRunner.class)
@PrepareForTest({VirtualMachine.class})
public class OpenNebulaAttachmentPluginTest {

	private static final String LOCAL_TOKEN_VALUE = "user:password";
	private static final String FAKE_USER_NAME = "fake-user-name";
	private static final String VIRTUAL_MACHINE_CONTENT = "<DISK>1</DISK>";
	private static final String DEFAULT_DEVICE_PREFIX = "vd";
	
	private OpenNebulaClientFactory factory;
	private OpenNebulaAttachmentPlugin plugin;

	@Before
	public void setUp() {
		this.factory = Mockito.spy(new OpenNebulaClientFactory());
		this.plugin = Mockito.spy(new OpenNebulaAttachmentPlugin());
	}
	
	// test case: When calling the requestInstance method, if the OpenNebulaClientFactory class
	// can not create a valid client from a token value, it must throw a UnespectedException.
	@Test(expected = UnexpectedException.class) // verify
	public void testRequestInstanceThrowUnespectedException() throws UnexpectedException, FogbowRasException {
		// set up
		AttachmentOrder attachmentOrder = createAttachmentOrder();
		OpenNebulaToken token = createOpenNebulaToken();
		Mockito.doThrow(new UnexpectedException()).when(this.factory).createClient(token.getTokenValue());
		this.plugin.setFactory(this.factory);

		// exercise
		this.plugin.requestInstance(attachmentOrder, token);
	}
	
	// test case: When invoking the requestInstance method, with the valid client
	// and template, a virtual machine will be instantiated to attach a volume image
	// disk, returning the attached disk ID in conjunction with the other IDs of
	// that instance.
	@Test
	public void testRequestInstanceSuccessful() throws UnexpectedException, FogbowRasException {
		// set up
		OpenNebulaToken token = createOpenNebulaToken();
		Client client = this.factory.createClient(token.getTokenValue());
		Mockito.doReturn(client).when(this.factory).createClient(token.getTokenValue());
		this.plugin.setFactory(this.factory);

		AttachmentOrder attachmentOrder = createAttachmentOrder();
		String template = generateAttachmentTemplate();

		String virtualMachineId = "1";
		OneResponse response = Mockito.mock(OneResponse.class);
		VirtualMachine virtualMachine = Mockito.mock(VirtualMachine.class);
		Mockito.doReturn(virtualMachine).when(this.factory).createVirtualMachine(client, virtualMachineId);
		Mockito.when(virtualMachine.diskAttach(Mockito.contains(template))).thenReturn(response);
		Mockito.when(response.isError()).thenReturn(false);

		Mockito.when(virtualMachine.info()).thenReturn(response);
		Mockito.when(response.getMessage()).thenReturn(VIRTUAL_MACHINE_CONTENT);

		// exercise
		this.plugin.requestInstance(attachmentOrder, token);

		// verify
		Mockito.verify(this.factory, Mockito.times(2)).createClient(Mockito.anyString());
		Mockito.verify(this.factory, Mockito.times(1)).createVirtualMachine(Mockito.eq(client),
				Mockito.eq(virtualMachineId));
		Mockito.verify(virtualMachine, Mockito.times(1)).diskAttach(Mockito.eq(template));
		Mockito.verify(virtualMachine, Mockito.times(1)).info();
		Mockito.verify(response, Mockito.times(1)).getMessage();
	}
	
	// test case: When calling the requestInstance method, with the fake instance ID
	// or an invalid token, an error will occur and an InvalidParameterException
	// will be thrown.
	@Test(expected = InvalidParameterException.class)
	public void testRequestInstanceUnsuccessful() throws UnexpectedException, FogbowRasException {
		// set up
		OpenNebulaToken token = createOpenNebulaToken();
		Client client = this.factory.createClient(token.getTokenValue());
		Mockito.doReturn(client).when(this.factory).createClient(token.getTokenValue());
		this.plugin.setFactory(this.factory);

		AttachmentOrder attachmentOrder = createAttachmentOrder();
		String template = generateAttachmentTemplate();

		String virtualMachineId = "1";
		OneResponse response = Mockito.mock(OneResponse.class);
		VirtualMachine virtualMachine = Mockito.mock(VirtualMachine.class);
		Mockito.doReturn(virtualMachine).when(this.factory).createVirtualMachine(client, virtualMachineId);
		Mockito.when(virtualMachine.diskAttach(Mockito.contains(template))).thenReturn(response);
		Mockito.when(response.isError()).thenReturn(true);

		// exercise
		this.plugin.requestInstance(attachmentOrder, token);

		// verify
		Mockito.verify(this.factory, Mockito.times(2)).createClient(Mockito.anyString());
		Mockito.verify(this.factory, Mockito.times(1)).createVirtualMachine(Mockito.eq(client),
				Mockito.eq(virtualMachineId));
		Mockito.verify(virtualMachine, Mockito.times(1)).diskAttach(Mockito.eq(template));
	}

	// test case: When calling the deleteInstance method, with the instance ID and a
	// valid token, the volume image disk associated with a virtual machine will be
	// detached.
	@Test
	public void testDeleteInstanceSuccessful() throws UnexpectedException, FogbowRasException {
		// set up
		OpenNebulaToken token = createOpenNebulaToken();
		Client client = this.factory.createClient(token.getTokenValue());
		Mockito.doReturn(client).when(this.factory).createClient(token.getTokenValue());
		this.plugin.setFactory(this.factory);

		String attachmentInstanceId = "1 1 1";
		int virtualMachineId, diskId;
		virtualMachineId = diskId = 1;

		OneResponse response = Mockito.mock(OneResponse.class);
		PowerMockito.mockStatic(VirtualMachine.class);
		BDDMockito
				.given(VirtualMachine.diskDetach(Mockito.eq(client), Mockito.eq(virtualMachineId), Mockito.eq(diskId)))
				.willReturn(response);
		Mockito.when(response.isError()).thenReturn(false);

		// exercise
		this.plugin.deleteInstance(attachmentInstanceId, token);

		// verify
		Mockito.verify(this.factory, Mockito.times(2)).createClient(Mockito.anyString());
		PowerMockito.verifyStatic(VirtualMachine.class, VerificationModeFactory.times(1));
		VirtualMachine.diskDetach(Mockito.eq(client), Mockito.eq(virtualMachineId), Mockito.eq(diskId));
		Mockito.verify(response, Mockito.times(1)).isError();
	}
	
	// test case: When calling the deleteInstance method, with the fake instance ID
	// or an invalid token, an error will occur and an InvalidParameterException
	// will be thrown.
	@Test
	public void testDeleteInstanceUnsuccessful() throws UnexpectedException, FogbowRasException {
		// set up
		OpenNebulaToken token = createOpenNebulaToken();
		Client client = this.factory.createClient(token.getTokenValue());
		Mockito.doReturn(client).when(this.factory).createClient(token.getTokenValue());
		this.plugin.setFactory(this.factory);

		String attachmentInstanceId = "1 1 1";
		int virtualMachineId, diskId;
		virtualMachineId = diskId = 1;

		OneResponse response = Mockito.mock(OneResponse.class);
		PowerMockito.mockStatic(VirtualMachine.class);
		BDDMockito.given(VirtualMachine.diskDetach(Mockito.eq(client), Mockito.eq(virtualMachineId), 
				Mockito.eq(diskId))).willReturn(response);
		Mockito.when(response.isError()).thenReturn(true);

		// exercise
		this.plugin.deleteInstance(attachmentInstanceId, token);

		// verify
		Mockito.verify(this.factory, Mockito.times(2)).createClient(Mockito.anyString());
		PowerMockito.verifyStatic(VirtualMachine.class, VerificationModeFactory.times(1));
		VirtualMachine.diskDetach(Mockito.eq(client), Mockito.eq(virtualMachineId), Mockito.eq(diskId));
		Mockito.verify(response, Mockito.times(1)).isError();
	}
	
	// test case: When calling the getInstance method, with the instance ID and a
	// valid token, a set of images will be loaded and the specific instance of the
	// image must be loaded.
	@Test
	public void testGetInstanceSuccessful() throws UnexpectedException, FogbowRasException {
		// set up
		OpenNebulaToken token = createOpenNebulaToken();
		Client client = this.factory.createClient(token.getTokenValue());
		Mockito.doReturn(client).when(this.factory).createClient(token.getTokenValue());
		this.plugin.setFactory(this.factory);

		String attachmentInstanceId = "1 1";
		String imageDevice = "fake-image-device";
		int diskId = 1;

		ImagePool imagePool = Mockito.mock(ImagePool.class);
		Mockito.doReturn(imagePool).when(this.factory).createImagePool(client);
		Image image = Mockito.mock(Image.class);
		Mockito.when(imagePool.getById(diskId)).thenReturn(image);
		Mockito.when(image.xpath(DEFAULT_DEVICE_PREFIX)).thenReturn(imageDevice);
		Mockito.when(image.stateString()).thenReturn(""); // FIXME ...

		// exercise
		this.plugin.getInstance(attachmentInstanceId, token);

		// verify
		Mockito.verify(this.factory, Mockito.times(2)).createClient(Mockito.anyString());
		Mockito.verify(this.factory, Mockito.times(1)).createImagePool(Mockito.eq(client));
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
		FederationUserToken federationUserToken = null;
		String requestingMember = null;
		String providingMember = null;
		String cloudName = null;
		String computeId = "1";
		String volumeId = "1";
		String device = null;
		
		AttachmentOrder attachmentOrder = new AttachmentOrder(
				federationUserToken, 
				requestingMember, 
				providingMember,
				cloudName,
				computeId, 
				volumeId, 
				device);
		
		return attachmentOrder;
	}

	private OpenNebulaToken createOpenNebulaToken() {
		String provider = null;
		String tokenValue = LOCAL_TOKEN_VALUE;
		String userId = null;
		String userName = FAKE_USER_NAME;
		String signature = null;
		
		OpenNebulaToken token = new OpenNebulaToken(
				provider, 
				tokenValue, 
				userId, 
				userName, 
				signature);
		
		return token;
	}
}
