package cloud.fogbow.ras.core.plugins.interoperability.opennebula.image.v5_4;

import cloud.fogbow.common.exceptions.FogbowException;
import cloud.fogbow.common.exceptions.UnexpectedException;
import cloud.fogbow.common.models.CloudToken;
import cloud.fogbow.common.util.HomeDir;
import cloud.fogbow.ras.constants.SystemConstants;
import cloud.fogbow.ras.core.plugins.interoperability.opennebula.OpenNebulaClientFactory;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;
import org.opennebula.client.Client;
import org.opennebula.client.image.Image;
import org.opennebula.client.image.ImagePool;

import java.io.File;
import java.util.Iterator;

public class OpenNebulaImagePluginTest {

	private static final String LOCAL_TOKEN_VALUE = "user:password";
	private static final String FAKE_USER_NAME = "fake-user-name";
	private static final String UNCHECKED_VALUE = "unchecked";
	private static final String IMAGE_SIZE_PATH = "IMAGE/SIZE";
	private static final String FAKE_ID = "fake-id";
	private static final String FAKE_NAME = "fake-name";
	private static final String STRING_VALUE_ONE = "1";
	private static final String STRING_VALUE_TWO = "2";

	private static final int VALUE_ONE = 1;
	private static final String CLOUD_NAME = "opennebula";

	private OpenNebulaClientFactory factory;
	private OpenNebulaImagePlugin plugin;

	@Before
	public void setUp() {
		String openenbulaConfFilePath = HomeDir.getPath() + SystemConstants.CLOUDS_CONFIGURATION_DIRECTORY_NAME +
				File.separator + CLOUD_NAME + File.separator + SystemConstants.CLOUD_SPECIFICITY_CONF_FILE_NAME;
		this.factory = Mockito.spy(new OpenNebulaClientFactory(openenbulaConfFilePath));
		this.plugin = Mockito.spy(new OpenNebulaImagePlugin(openenbulaConfFilePath));
	}
	
	// test case: When calling the getAllImages method, if the createClient method
	// in the OpenNebulaClientFactory class can not create a valid client from a
	// token value, it must throw a UnespectedException.
	@Test(expected = UnexpectedException.class) // verify
	public void testGetAllImagesThrowExceptionWhenCallCreateClientMethod()
			throws FogbowException {
		
		// set up
		CloudToken token = createCloudToken();
		Mockito.doThrow(new UnexpectedException()).when(this.factory).createClient(token.getTokenValue());
		this.plugin.setFactory(this.factory);

		// exercise
		this.plugin.getAllImages(token);
	}
	
	// test case: When calling the getAllImages method, if the createImagePool method
	// in the OpenNebulaClientFactory return an error response, it must throw a 
	// UnespectedException.
	@Test(expected = UnexpectedException.class) // verify
	public void testGetAllImagesThrowExceptionWhenCallCreateImagePoolMethod()
			throws FogbowException {

		// set up
		CloudToken token = createCloudToken();
		Client client = this.factory.createClient(token.getTokenValue());
		Mockito.doReturn(client).when(this.factory).createClient(token.getTokenValue());
		Mockito.doThrow(new UnexpectedException()).when(this.factory).createImagePool(client);
		this.plugin.setFactory(this.factory);

		// exercise
		this.plugin.getAllImages(token);
	}
	
	// test case: When invoking the getAllImages method, with a valid client, a
	// collection of images must be loaded, returning a map of instances of images
	// with ID and name.
	@Test
	@SuppressWarnings(UNCHECKED_VALUE)
	public void testGetAllImagesSuccessful() throws FogbowException {
		// set up
		CloudToken token = createCloudToken();
		Client client = this.factory.createClient(token.getTokenValue());
		Mockito.doReturn(client).when(this.factory).createClient(token.getTokenValue());
		this.plugin.setFactory(this.factory);

		ImagePool imagePool = Mockito.mock(ImagePool.class);
		Mockito.doReturn(imagePool).when(this.factory).createImagePool(client);

		Image image = Mockito.mock(Image.class);
		Iterator<Image> imageIterator = Mockito.mock(Iterator.class);
		Mockito.when(imagePool.iterator()).thenReturn(imageIterator);
		Mockito.when(imageIterator.hasNext()).thenReturn(true, false);
		Mockito.when(imageIterator.next()).thenReturn(image);
		Mockito.when(image.getId()).thenReturn(FAKE_ID);
		Mockito.when(image.getName()).thenReturn(FAKE_NAME);

		// exercise
		this.plugin.getAllImages(token);

		// verify
		Mockito.verify(this.factory, Mockito.times(2)).createClient(Mockito.anyString());
		Mockito.verify(this.factory, Mockito.times(1)).createImagePool(Mockito.eq(client));
		Mockito.verify(image, Mockito.times(1)).getId();
		Mockito.verify(image, Mockito.times(1)).getName();
	}
	
	// test case: When invoking the getImage method, with a valid client, a collection
	// of images will be loaded to select an Image through a specific ID, returning 
	// the instance of an image, with ID, name, size and status.
	@Test
	@SuppressWarnings(UNCHECKED_VALUE)
	public void testGetImageSuccessful() throws FogbowException {
		// set up
		String imageId = STRING_VALUE_ONE;

		CloudToken token = createCloudToken();
		Client client = this.factory.createClient(token.getTokenValue());
		Mockito.doReturn(client).when(this.factory).createClient(token.getTokenValue());
		this.plugin.setFactory(this.factory);

		ImagePool imagePool = Mockito.mock(ImagePool.class);
		Mockito.doReturn(imagePool).when(this.factory).createImagePool(client);

		Image image = Mockito.mock(Image.class);
		Iterator<Image> imageIterator = Mockito.mock(Iterator.class);
		Mockito.when(imagePool.iterator()).thenReturn(imageIterator);
		Mockito.when(imageIterator.hasNext()).thenReturn(true, false);
		Mockito.when(imageIterator.next()).thenReturn(image);
		Mockito.when(image.getId()).thenReturn(STRING_VALUE_ONE);
		Mockito.when(image.getName()).thenReturn(FAKE_NAME);
		Mockito.when(image.xpath(IMAGE_SIZE_PATH)).thenReturn(STRING_VALUE_ONE);
		Mockito.when(image.state()).thenReturn(VALUE_ONE);

		// exercise
		this.plugin.getImage(imageId, token);

		// verify
		Mockito.verify(this.factory, Mockito.times(2)).createClient(Mockito.anyString());
		Mockito.verify(this.factory, Mockito.times(1)).createImagePool(Mockito.eq(client));
		Mockito.verify(image, Mockito.times(2)).getId();
		Mockito.verify(image, Mockito.times(1)).getName();
		Mockito.verify(image, Mockito.times(1)).xpath(IMAGE_SIZE_PATH);
		Mockito.verify(image, Mockito.times(1)).state();
	}
	
	// Test case: When invoking the getImage method, with a valid client, and an ID
	// divergent from the images contained in the collection, no instance should be
	// found, returning a null image.
	@Test
	@SuppressWarnings(UNCHECKED_VALUE)
	public void testGetImageUnsuccessfulByIdNotFound() throws FogbowException {
		// set up
		String imageId = STRING_VALUE_TWO;

		CloudToken token = createCloudToken();
		Client client = this.factory.createClient(token.getTokenValue());
		Mockito.doReturn(client).when(this.factory).createClient(token.getTokenValue());
		this.plugin.setFactory(this.factory);

		ImagePool imagePool = Mockito.mock(ImagePool.class);
		Mockito.doReturn(imagePool).when(this.factory).createImagePool(client);

		Image image = Mockito.mock(Image.class);
		Iterator<Image> imageIterator = Mockito.mock(Iterator.class);
		Mockito.when(imagePool.iterator()).thenReturn(imageIterator);
		Mockito.when(imageIterator.hasNext()).thenReturn(true, false);
		Mockito.when(imageIterator.next()).thenReturn(image);
		Mockito.when(image.getId()).thenReturn(STRING_VALUE_ONE);

		// exercise
		this.plugin.getImage(imageId, token);

		// verify
		Mockito.verify(this.factory, Mockito.times(2)).createClient(Mockito.anyString());
		Mockito.verify(this.factory, Mockito.times(1)).createImagePool(Mockito.eq(client));
		Mockito.verify(image, Mockito.times(1)).getId();
	}
	
	// test case: When invoking the getImage method with a valid client, and the
	// collection of images is empty, an instance of an image can not be loaded,
	// returning a null image.
	@Test
	@SuppressWarnings(UNCHECKED_VALUE)
	public void testGetImageEmptyCollection() throws FogbowException {
		// set up
		String imageId = STRING_VALUE_ONE;

		CloudToken token = createCloudToken();
		Client client = this.factory.createClient(token.getTokenValue());
		Mockito.doReturn(client).when(this.factory).createClient(token.getTokenValue());
		this.plugin.setFactory(this.factory);

		ImagePool imagePool = Mockito.mock(ImagePool.class);
		Mockito.doReturn(imagePool).when(this.factory).createImagePool(client);

		Iterator<Image> imageIterator = Mockito.mock(Iterator.class);
		Mockito.when(imagePool.iterator()).thenReturn(imageIterator);
		Mockito.when(imageIterator.hasNext()).thenReturn(false);
		Mockito.when(imageIterator.next()).thenReturn(null);

		// exercise
		this.plugin.getImage(imageId, token);

		// verify
		Mockito.verify(this.factory, Mockito.times(2)).createClient(Mockito.anyString());
		Mockito.verify(this.factory, Mockito.times(1)).createImagePool(Mockito.eq(client));
	}
	
	private CloudToken createCloudToken() {
		String provider = null;
		String tokenValue = LOCAL_TOKEN_VALUE;
		String userId = null;
		String userName = FAKE_USER_NAME;
		String signature = null;
		
		CloudToken token = new CloudToken(
				provider,
				userId,
				tokenValue);
		
		return token;
	}
}
