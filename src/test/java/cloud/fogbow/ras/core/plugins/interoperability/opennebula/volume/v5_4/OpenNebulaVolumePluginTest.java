package cloud.fogbow.ras.core.plugins.interoperability.opennebula.volume.v5_4;

import cloud.fogbow.ras.core.constants.SystemConstants;
import cloud.fogbow.ras.core.plugins.interoperability.opennebula.OpenNebulaClientFactory;
import cloud.fogbow.ras.core.models.orders.VolumeOrder;
import org.fogbowcloud.ras.core.models.tokens.FederationUserToken;
import org.fogbowcloud.ras.core.models.tokens.OpenNebulaToken;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.BDDMockito;
import org.mockito.Mockito;
import org.opennebula.client.Client;
import org.opennebula.client.OneResponse;
import org.opennebula.client.image.Image;
import org.opennebula.client.image.ImagePool;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;

import java.io.File;

@RunWith(PowerMockRunner.class)
@PrepareForTest({Image.class})
public class OpenNebulaVolumePluginTest {

	private static final String LOCAL_TOKEN_VALUE = "user:password";
	private static final String FAKE_USER_NAME = "fake-user-name";
	private static final String FAKE_VOLUME_NAME = "fake-volume-name";
	private static final String IMAGE_SIZE_PATH = "SIZE";
	private static final String STATE_READY = "READY";
	private static final String CLOUD_NAME = "opennebula";

	private OpenNebulaClientFactory factory;
	private OpenNebulaVolumePlugin plugin;

	@Before
	public void setUp() {
		String openenbulaConfFilePath = HomeDir.getPath() + SystemConstants.CLOUDS_CONFIGURATION_DIRECTORY_NAME +
				File.separator + CLOUD_NAME + File.separator + SystemConstants.CLOUD_SPECIFICITY_CONF_FILE_NAME;
		this.factory = Mockito.spy(new OpenNebulaClientFactory(openenbulaConfFilePath));
		this.plugin = Mockito.spy(new OpenNebulaVolumePlugin(openenbulaConfFilePath));
	}
	
	// test case: When calling the requestInstance method, if the OpenNebulaClientFactory class
	// can not create a valid client from a token value, it must throw a UnespectedException.
	@Test(expected = UnexpectedException.class) // verify
	public void testRequestInstanceThrowUnespectedException() throws UnexpectedException, FogbowRasException {
		// set up
		VolumeOrder volumeOrder = new VolumeOrder();
		OpenNebulaToken token = createOpenNebulaToken();
		Mockito.doThrow(new UnexpectedException()).when(this.factory).createClient(token.getTokenValue());
		this.plugin.setFactory(this.factory);

		// exercise
		this.plugin.requestInstance(volumeOrder, token);
	}

	// test case: When calling the requestInstance method, with the valid client and
	// template, a volume will be allocated to return instance ID.
	@Test
	public void testRequestInstanceSuccessful() throws UnexpectedException, FogbowRasException {
		// set up
		OpenNebulaToken token = createOpenNebulaToken();
		Client client = this.factory.createClient(token.getTokenValue());
		Mockito.doReturn(client).when(this.factory).createClient(token.getTokenValue());
		this.plugin.setFactory(this.factory);

		VolumeOrder volumeOrder = createVolumeOrder();
		String template = generateImageTemplate();

		int id = 1;
		String instanceId = "1";
		Mockito.doReturn(instanceId).when(this.factory).allocateImage(client, template, id);
		OneResponse response = Mockito.mock(OneResponse.class);
		PowerMockito.mockStatic(Image.class);
		BDDMockito.given(Image.allocate(client, template, id)).willReturn(response);
		Mockito.when(response.isError()).thenReturn(false);
		Mockito.when(response.getMessage()).thenReturn(instanceId);

		// exercise
		this.plugin.requestInstance(volumeOrder, token);

		// verify
		Mockito.verify(this.factory, Mockito.times(2)).createClient(Mockito.anyString());
		Mockito.verify(this.factory, Mockito.times(1)).allocateImage(Mockito.eq(client), 
				Mockito.eq(template), Mockito.eq(id));
	}
	
	// test case: When calling the getInstance method, with the instance ID and a
	// valid token, a set of images will be loaded and the specific instance of the
	// image must be loaded.
	@Test
	public void testGetInstanceSuccessful() throws UnexpectedException, FogbowRasException {
		// set up
		String instanceId = "1";
		OpenNebulaToken token = createOpenNebulaToken();
		Client client = this.factory.createClient(token.getTokenValue());
		Mockito.doReturn(client).when(this.factory).createClient(token.getTokenValue());
		this.plugin.setFactory(this.factory);

		int id = 1;
		String size = "1";
		ImagePool imagePool = Mockito.mock(ImagePool.class);
		Mockito.doReturn(imagePool).when(this.factory).createImagePool(client);
		Image image = Mockito.mock(Image.class);
		Mockito.when(imagePool.getById(id)).thenReturn(image);
		Mockito.when(image.xpath(IMAGE_SIZE_PATH)).thenReturn(size);
		Mockito.when(image.getName()).thenReturn(FAKE_VOLUME_NAME);
		Mockito.when(image.stateString()).thenReturn(STATE_READY);

		// exercise
		this.plugin.getInstance(instanceId, token);

		// verify
		Mockito.verify(this.factory, Mockito.times(2)).createClient(Mockito.anyString());
		Mockito.verify(this.factory, Mockito.times(1)).createImagePool(client);
		Mockito.verify(imagePool, Mockito.times(1)).getById(id);
		Mockito.verify(image, Mockito.times(1)).xpath(IMAGE_SIZE_PATH);
		Mockito.verify(image, Mockito.times(1)).getName();
		Mockito.verify(image, Mockito.times(1)).stateString();
	}
	
	// test case: When calling the deleteInstance method, with the instance ID and a
	// valid token, a set of images will be loaded and the image specified for
	// removal will be deleted.
	@Test
	public void testDeleteInstanceSuccessful() throws UnexpectedException, FogbowRasException {
		// set up
		String instanceId = "1";
		OpenNebulaToken token = createOpenNebulaToken();
		Client client = this.factory.createClient(token.getTokenValue());
		Mockito.doReturn(client).when(this.factory).createClient(token.getTokenValue());
		this.plugin.setFactory(this.factory);

		int id = 1;
		ImagePool imagePool = Mockito.mock(ImagePool.class);
		Mockito.doReturn(imagePool).when(this.factory).createImagePool(client);
		Image image = Mockito.mock(Image.class);
		Mockito.when(imagePool.getById(id)).thenReturn(image);
		OneResponse response = Mockito.mock(OneResponse.class);
		Mockito.when(image.delete()).thenReturn(response);
		Mockito.when(response.isError()).thenReturn(false);

		// exercise
		this.plugin.deleteInstance(instanceId, token);

		// verify
		Mockito.verify(this.factory, Mockito.times(2)).createClient(Mockito.anyString());
		Mockito.verify(this.factory, Mockito.times(1)).createImagePool(client);
		Mockito.verify(imagePool, Mockito.times(1)).getById(id);
		Mockito.verify(image, Mockito.times(1)).delete();
		Mockito.verify(response, Mockito.times(1)).isError();
	}
	
	// test case: When calling the deleteInstance method, if the removal call is not
	// answered an error response is returned.
	@Test
	public void testDeleteInstanceUnsuccessful() throws UnexpectedException, FogbowRasException {
		// set up
		String instanceId = "1";
		OpenNebulaToken token = createOpenNebulaToken();
		Client client = this.factory.createClient(token.getTokenValue());
		Mockito.doReturn(client).when(this.factory).createClient(token.getTokenValue());
		this.plugin.setFactory(this.factory);

		int id = 1;
		ImagePool imagePool = Mockito.mock(ImagePool.class);
		Mockito.doReturn(imagePool).when(this.factory).createImagePool(client);
		Image image = Mockito.mock(Image.class);
		Mockito.when(imagePool.getById(id)).thenReturn(image);
		OneResponse response = Mockito.mock(OneResponse.class);
		Mockito.when(image.delete()).thenReturn(response);
		Mockito.when(response.isError()).thenReturn(true);

		// exercise
		this.plugin.deleteInstance(instanceId, token);

		// verify
		Mockito.verify(this.factory, Mockito.times(2)).createClient(Mockito.anyString());
		Mockito.verify(this.factory, Mockito.times(1)).createImagePool(client);
		Mockito.verify(imagePool, Mockito.times(1)).getById(id);
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
		FederationUserToken federationUserToken = null;
		String requestingMember = null;
		String providingMember = null;
		String cloudName = null;
		String name = FAKE_VOLUME_NAME;
		int volumeSize = 1;
				
		VolumeOrder volumeOrder = new VolumeOrder(
				federationUserToken, 
				requestingMember, 
				providingMember,
				cloudName,
				name, 
				volumeSize);
		
		return volumeOrder;
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
