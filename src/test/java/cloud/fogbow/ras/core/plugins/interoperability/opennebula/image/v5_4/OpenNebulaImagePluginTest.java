package cloud.fogbow.ras.core.plugins.interoperability.opennebula.image.v5_4;

import java.util.Iterator;

import cloud.fogbow.common.exceptions.FogbowException;
import cloud.fogbow.common.models.CloudUser;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.BDDMockito;
import org.mockito.Mockito;
import org.mockito.internal.verification.VerificationModeFactory;
import org.opennebula.client.Client;
import org.opennebula.client.image.Image;
import org.opennebula.client.image.ImagePool;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;
import cloud.fogbow.ras.core.plugins.interoperability.opennebula.OpenNebulaClientUtil;

@RunWith(PowerMockRunner.class)
@PrepareForTest({OpenNebulaClientUtil.class})
public class OpenNebulaImagePluginTest {

	private static final String FAKE_ID = "fake-id";
	private static final String FAKE_NAME = "fake-name";
	private static final String IMAGE_SIZE_PATH = "IMAGE/SIZE";
	private static final String LOCAL_TOKEN_VALUE = "user:password";
	private static final String STRING_VALUE_ONE = "1";
	private static final String STRING_VALUE_TWO = "2";

	private static final int VALUE_ONE = 1;

	private OpenNebulaImagePlugin plugin;

	@Before
	public void setUp() {
		this.plugin = Mockito.spy(new OpenNebulaImagePlugin());
	}
	
	// test case: When invoking the getAllImages method, with a valid client, a
	// collection of images must be loaded, returning a getCloudUser of instances of images
	// with ID and name.
	@Test
	public void testGetAllImagesSuccessful() throws FogbowException {
		// set up
		Client client = Mockito.mock(Client.class);
		PowerMockito.mockStatic(OpenNebulaClientUtil.class);
		BDDMockito.given(OpenNebulaClientUtil.createClient(Mockito.anyString(), Mockito.anyString()))
				.willReturn(client);

		ImagePool imagePool = Mockito.mock(ImagePool.class);
		BDDMockito.given(OpenNebulaClientUtil.getImagePool(Mockito.any())).willReturn(imagePool);

		Image image = Mockito.mock(Image.class);
		Iterator<Image> imageIterator = Mockito.mock(Iterator.class);
		Mockito.when(imagePool.iterator()).thenReturn(imageIterator);
		Mockito.when(imageIterator.hasNext()).thenReturn(true, false);
		Mockito.when(imageIterator.next()).thenReturn(image);
		Mockito.when(image.getId()).thenReturn(FAKE_ID);
		Mockito.when(image.getName()).thenReturn(FAKE_NAME);

		CloudUser cloudUser = createCloudUser();
		
		// exercise
		this.plugin.getAllImages(cloudUser);

		// verify
		PowerMockito.verifyStatic(OpenNebulaClientUtil.class, VerificationModeFactory.times(1));
		OpenNebulaClientUtil.createClient(Mockito.anyString(), Mockito.anyString());

		PowerMockito.verifyStatic(OpenNebulaClientUtil.class, VerificationModeFactory.times(1));
		OpenNebulaClientUtil.getImagePool(Mockito.eq(client));
		
		Mockito.verify(image, Mockito.times(1)).getId();
		Mockito.verify(image, Mockito.times(1)).getName();
	}
	
	// test case: When invoking the getImage method, with a valid client, a collection
	// of images will be loaded to select an Image through a specific ID, returning 
	// the instance of an image, with ID, name, size and status.
	@Test
	public void testGetImageSuccessful() throws FogbowException {
		// set up
		Client client = Mockito.mock(Client.class);
		PowerMockito.mockStatic(OpenNebulaClientUtil.class);
		BDDMockito.given(OpenNebulaClientUtil.createClient(Mockito.anyString(), Mockito.anyString()))
				.willReturn(client);

		ImagePool imagePool = Mockito.mock(ImagePool.class);
		BDDMockito.given(OpenNebulaClientUtil.getImagePool(Mockito.any())).willReturn(imagePool);

		Image image = Mockito.mock(Image.class);
		Iterator<Image> imageIterator = Mockito.mock(Iterator.class);
		Mockito.when(imagePool.iterator()).thenReturn(imageIterator);
		Mockito.when(imageIterator.hasNext()).thenReturn(true, false);
		Mockito.when(imageIterator.next()).thenReturn(image);
		Mockito.when(image.getId()).thenReturn(STRING_VALUE_ONE);
		Mockito.when(image.getName()).thenReturn(FAKE_NAME);
		Mockito.when(image.xpath(IMAGE_SIZE_PATH)).thenReturn(STRING_VALUE_ONE);
		Mockito.when(image.state()).thenReturn(VALUE_ONE);

		CloudUser cloudUser = createCloudUser();
		String imageId = STRING_VALUE_ONE;
		
		// exercise
		this.plugin.getImage(imageId, cloudUser);

		// verify
		PowerMockito.verifyStatic(OpenNebulaClientUtil.class, VerificationModeFactory.times(1));
		OpenNebulaClientUtil.createClient(Mockito.anyString(), Mockito.anyString());

		PowerMockito.verifyStatic(OpenNebulaClientUtil.class, VerificationModeFactory.times(1));
		OpenNebulaClientUtil.getImagePool(Mockito.eq(client));
		
		Mockito.verify(image, Mockito.times(2)).getId();
		Mockito.verify(image, Mockito.times(1)).getName();
		Mockito.verify(image, Mockito.times(1)).xpath(IMAGE_SIZE_PATH);
		Mockito.verify(image, Mockito.times(1)).state();
	}
	
	// Test case: When invoking the getImage method, with a valid client, and an ID
	// divergent from the images contained in the collection, no instance should be
	// found, returning a null image.
	@Test
	public void testGetImageUnsuccessfulByIdNotFound() throws FogbowException {
		// set up
		Client client = Mockito.mock(Client.class);
		PowerMockito.mockStatic(OpenNebulaClientUtil.class);
		BDDMockito.given(OpenNebulaClientUtil.createClient(Mockito.anyString(), Mockito.anyString()))
				.willReturn(client);

		ImagePool imagePool = Mockito.mock(ImagePool.class);
		BDDMockito.given(OpenNebulaClientUtil.getImagePool(Mockito.any())).willReturn(imagePool);

		Image image = Mockito.mock(Image.class);
		Iterator<Image> imageIterator = Mockito.mock(Iterator.class);
		Mockito.when(imagePool.iterator()).thenReturn(imageIterator);
		Mockito.when(imageIterator.hasNext()).thenReturn(true, false);
		Mockito.when(imageIterator.next()).thenReturn(image);
		Mockito.when(image.getId()).thenReturn(STRING_VALUE_ONE);
		
		CloudUser cloudUser = createCloudUser();
		String imageId = STRING_VALUE_TWO;

		// exercise
		this.plugin.getImage(imageId, cloudUser);

		// verify
		PowerMockito.verifyStatic(OpenNebulaClientUtil.class, VerificationModeFactory.times(1));
		OpenNebulaClientUtil.createClient(Mockito.anyString(), Mockito.anyString());

		PowerMockito.verifyStatic(OpenNebulaClientUtil.class, VerificationModeFactory.times(1));
		OpenNebulaClientUtil.getImagePool(Mockito.eq(client));
		
		Mockito.verify(image, Mockito.times(1)).getId();
	}
	
	// test case: When invoking the getImage method with a valid client, and the
	// collection of images is empty, an instance of an image can not be loaded,
	// returning a null image.
	@Test
	public void testGetImageEmptyCollection() throws FogbowException {
		// set up
		Client client = Mockito.mock(Client.class);
		PowerMockito.mockStatic(OpenNebulaClientUtil.class);
		BDDMockito.given(OpenNebulaClientUtil.createClient(Mockito.anyString(), Mockito.anyString()))
				.willReturn(client);

		ImagePool imagePool = Mockito.mock(ImagePool.class);
		BDDMockito.given(OpenNebulaClientUtil.getImagePool(Mockito.any())).willReturn(imagePool);

		Iterator<Image> imageIterator = Mockito.mock(Iterator.class);
		Mockito.when(imagePool.iterator()).thenReturn(imageIterator);
		Mockito.when(imageIterator.hasNext()).thenReturn(false);
		Mockito.when(imageIterator.next()).thenReturn(null);

		CloudUser cloudUser = createCloudUser();
		String imageId = STRING_VALUE_ONE;
		
		// exercise
		this.plugin.getImage(imageId, cloudUser);

		// verify
		PowerMockito.verifyStatic(OpenNebulaClientUtil.class, VerificationModeFactory.times(1));
		OpenNebulaClientUtil.createClient(Mockito.anyString(), Mockito.anyString());

		PowerMockito.verifyStatic(OpenNebulaClientUtil.class, VerificationModeFactory.times(1));
		OpenNebulaClientUtil.getImagePool(Mockito.eq(client));
	}

	private CloudUser createCloudUser() {
		String userId = FAKE_ID;
		String userName = FAKE_NAME;
		String tokenValue = LOCAL_TOKEN_VALUE;

		CloudUser cloudUser = new CloudUser(userId, userName, tokenValue);

		return cloudUser;
	}
}
