package cloud.fogbow.ras.core.plugins.interoperability.opennebula.volume.v5_4;

import java.util.HashMap;
import java.util.Map;

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
import org.powermock.api.mockito.PowerMockito;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;

import cloud.fogbow.common.exceptions.FogbowException;
import cloud.fogbow.common.models.CloudToken;
import cloud.fogbow.common.models.FederationUser;
import cloud.fogbow.ras.core.models.orders.VolumeOrder;
import cloud.fogbow.ras.core.plugins.interoperability.opennebula.OpenNebulaClientUtil;

@RunWith(PowerMockRunner.class)
@PrepareForTest({Image.class, OpenNebulaClientUtil.class})
public class OpenNebulaVolumePluginTest {

	private static final String DISK_VALUE_8GB = "8";
	private static final String FAKE_VOLUME_NAME = "fake-volume-name";
	private static final String IMAGE_SIZE_PATH = "SIZE";
	private static final String LOCAL_TOKEN_VALUE = "user:password";
	private static final String STATE_READY = "READY";
	private static final String STRING_VALUE_ONE = "1";

	private OpenNebulaVolumePlugin plugin;

	@Before
	public void setUp() {
		this.plugin = Mockito.spy(new OpenNebulaVolumePlugin());
	}
	
	// test case: When calling the requestInstance method, with the valid client and
	// template, a volume will be allocated to return instance ID.
	@Test
	public void testRequestInstanceSuccessful() throws FogbowException {
		// set up
		Client client = Mockito.mock(Client.class);
		PowerMockito.mockStatic(OpenNebulaClientUtil.class);
		BDDMockito.given(OpenNebulaClientUtil.createClient(Mockito.anyString(), Mockito.anyString()))
				.willReturn(client);

		String template = generateImageTemplate();
		String instanceId = STRING_VALUE_ONE;

		BDDMockito.given(OpenNebulaClientUtil.allocateImage(Mockito.eq(client), Mockito.eq(template), Mockito.anyInt())).willReturn(instanceId);

		OneResponse response = Mockito.mock(OneResponse.class);
		PowerMockito.mockStatic(Image.class);
		BDDMockito.given(Image.allocate(Mockito.eq(client), Mockito.eq(template), Mockito.anyInt()))
				.willReturn(response);
		Mockito.when(response.isError()).thenReturn(false);
		Mockito.when(response.getMessage()).thenReturn(instanceId);

		CloudToken token = createCloudToken();
		VolumeOrder volumeOrder = createVolumeOrder();

		// exercise
		this.plugin.requestInstance(volumeOrder, token);

		// verify
		PowerMockito.verifyStatic(OpenNebulaClientUtil.class, VerificationModeFactory.times(1));
		OpenNebulaClientUtil.createClient(Mockito.anyString(), Mockito.anyString());

		PowerMockito.verifyStatic(OpenNebulaClientUtil.class, VerificationModeFactory.times(1));
		OpenNebulaClientUtil.allocateImage(Mockito.eq(client), Mockito.eq(template), Mockito.anyInt());
	}
	
	// test case: When calling the getInstance method, with the instance ID and a
	// valid token, a set of images will be loaded and the specific instance of the
	// image must be loaded.
	@Test
	public void testGetInstanceSuccessful() throws FogbowException {
		// set up
		Client client = Mockito.mock(Client.class);
		PowerMockito.mockStatic(OpenNebulaClientUtil.class);
		BDDMockito.given(OpenNebulaClientUtil.createClient(Mockito.anyString(), Mockito.anyString()))
				.willReturn(client);
		
		ImagePool imagePool = Mockito.mock(ImagePool.class);
		BDDMockito.given(OpenNebulaClientUtil.getImagePool(Mockito.any())).willReturn(imagePool);
		
		Image image = Mockito.mock(Image.class);
		Mockito.when(imagePool.getById(Mockito.anyInt())).thenReturn(image);
		Mockito.when(image.xpath(IMAGE_SIZE_PATH)).thenReturn(DISK_VALUE_8GB);
		Mockito.when(image.getName()).thenReturn(FAKE_VOLUME_NAME);
		Mockito.when(image.stateString()).thenReturn(STATE_READY);

		CloudToken token = createCloudToken();
		String instanceId = STRING_VALUE_ONE;
		
		// exercise
		this.plugin.getInstance(instanceId, token);

		// verify
		PowerMockito.verifyStatic(OpenNebulaClientUtil.class, VerificationModeFactory.times(1));
		OpenNebulaClientUtil.createClient(Mockito.anyString(), Mockito.anyString());

		PowerMockito.verifyStatic(OpenNebulaClientUtil.class, VerificationModeFactory.times(1));
		OpenNebulaClientUtil.getImagePool(Mockito.eq(client));
		
		Mockito.verify(imagePool, Mockito.times(1)).getById(Mockito.anyInt());
		Mockito.verify(image, Mockito.times(1)).xpath(Mockito.anyString());
		Mockito.verify(image, Mockito.times(1)).getName();
		Mockito.verify(image, Mockito.times(1)).stateString();
	}
	
	// test case: When calling the deleteInstance method, with the instance ID and a
	// valid token, a set of images will be loaded and the image specified for
	// removal will be deleted.
	@Test
	public void testDeleteInstanceSuccessful() throws FogbowException {
		// set up
		Client client = Mockito.mock(Client.class);
		PowerMockito.mockStatic(OpenNebulaClientUtil.class);
		BDDMockito.given(OpenNebulaClientUtil.createClient(Mockito.anyString(), Mockito.anyString()))
				.willReturn(client);
		
		ImagePool imagePool = Mockito.mock(ImagePool.class);
		BDDMockito.given(OpenNebulaClientUtil.getImagePool(Mockito.any())).willReturn(imagePool);
		
		Image image = Mockito.mock(Image.class);
		Mockito.when(imagePool.getById(Mockito.anyInt())).thenReturn(image);
		OneResponse response = Mockito.mock(OneResponse.class);
		Mockito.when(image.delete()).thenReturn(response);
		Mockito.when(response.isError()).thenReturn(false);

		CloudToken token = createCloudToken();
		String instanceId = STRING_VALUE_ONE;
		
		// exercise
		this.plugin.deleteInstance(instanceId, token);

		// verify
		PowerMockito.verifyStatic(OpenNebulaClientUtil.class, VerificationModeFactory.times(1));
		OpenNebulaClientUtil.createClient(Mockito.anyString(), Mockito.anyString());

		PowerMockito.verifyStatic(OpenNebulaClientUtil.class, VerificationModeFactory.times(1));
		OpenNebulaClientUtil.getImagePool(Mockito.eq(client));

		Mockito.verify(imagePool, Mockito.times(1)).getById(Mockito.anyInt());
		Mockito.verify(image, Mockito.times(1)).delete();
		Mockito.verify(response, Mockito.times(1)).isError();
	}
	
	// test case: When calling the deleteInstance method, if the removal call is not
	// answered an error response is returned.
	@Test
	public void testDeleteInstanceUnsuccessful() throws FogbowException {
		// set up
		Client client = Mockito.mock(Client.class);
		PowerMockito.mockStatic(OpenNebulaClientUtil.class);
		BDDMockito.given(OpenNebulaClientUtil.createClient(Mockito.anyString(), Mockito.anyString()))
				.willReturn(client);
		
		ImagePool imagePool = Mockito.mock(ImagePool.class);
		BDDMockito.given(OpenNebulaClientUtil.getImagePool(Mockito.any())).willReturn(imagePool);
		
		Image image = Mockito.mock(Image.class);
		Mockito.when(imagePool.getById(Mockito.anyInt())).thenReturn(image);
		OneResponse response = Mockito.mock(OneResponse.class);
		Mockito.when(image.delete()).thenReturn(response);
		Mockito.when(response.isError()).thenReturn(true);

		CloudToken token = createCloudToken();
		String instanceId = STRING_VALUE_ONE;
		
		// exercise
		this.plugin.deleteInstance(instanceId, token);

		// verify
		PowerMockito.verifyStatic(OpenNebulaClientUtil.class, VerificationModeFactory.times(1));
		OpenNebulaClientUtil.createClient(Mockito.anyString(), Mockito.anyString());

		PowerMockito.verifyStatic(OpenNebulaClientUtil.class, VerificationModeFactory.times(1));
		OpenNebulaClientUtil.getImagePool(Mockito.eq(client));
		
		Mockito.verify(imagePool, Mockito.times(1)).getById(Mockito.anyInt());
		Mockito.verify(image, Mockito.times(1)).delete();
		Mockito.verify(response, Mockito.times(1)).isError();
		Mockito.verify(response, Mockito.times(1)).getMessage();
	}
	
	private String generateImageTemplate() {
		String template = "<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"yes\"?>\n" + 
				"<IMAGE>\n" + 
				"    <DEV_PREFIX>vd</DEV_PREFIX>\n" + 
				"    <DISK_TYPE>BLOCK</DISK_TYPE>\n" + 
				"    <FSTYPE>raw</FSTYPE>\n" + 
				"    <NAME>fake-volume-name</NAME>\n" + 
				"    <PERSISTENT>YES</PERSISTENT>\n" + 
				"    <SIZE>1</SIZE>\n" + 
				"    <TYPE>DATABLOCK</TYPE>\n" + 
				"</IMAGE>\n";
		
		return template;
	}
	
	private VolumeOrder createVolumeOrder() {
		FederationUser federationUser = null;
		String requestingMember = null;
		String providingMember = null;
		String cloudName = null;
		String name = FAKE_VOLUME_NAME;
		int volumeSize = 1;
				
		VolumeOrder volumeOrder = new VolumeOrder(
				federationUser, 
				requestingMember, 
				providingMember,
				cloudName,
				name, 
				volumeSize);
		
		return volumeOrder;
	}

	private CloudToken createCloudToken() {
		String provider = null;
		String userId = null;
		String userName = null;
		String tokenValue = LOCAL_TOKEN_VALUE;
		Map<String, String> extraAttributes = new HashMap<>();

		FederationUser federationUser = new FederationUser(
				provider, 
				userId, 
				userName, 
				tokenValue, 
				extraAttributes);
		
		return new CloudToken(federationUser);
	}
	
}
