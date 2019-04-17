package cloud.fogbow.ras.core.plugins.interoperability.opennebula.image.v5_4;

import java.io.File;
import java.util.Iterator;
import java.util.Map;

import org.junit.Assert;
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

import cloud.fogbow.common.exceptions.FogbowException;
import cloud.fogbow.common.exceptions.InvalidParameterException;
import cloud.fogbow.common.models.CloudUser;
import cloud.fogbow.common.util.HomeDir;
import cloud.fogbow.ras.constants.SystemConstants;
import cloud.fogbow.ras.core.plugins.interoperability.opennebula.OpenNebulaClientUtil;

@RunWith(PowerMockRunner.class)
@PrepareForTest({OpenNebulaClientUtil.class})
public class OpenNebulaImagePluginTest {

	private static final String EMPTY_STRING = "";
	private static final String FAKE_ID = "fake-id";
	private static final String FAKE_NAME = "fake-name";
	private static final String LOCAL_TOKEN_VALUE = "user:password";
	private static final String ID_VALUE_ONE = "1";
	private static final String OPENNEBULA_CLOUD_NAME_DIRECTORY = "opennebula";

	private static final int VALUE_ONE = 1;

	private OpenNebulaImagePlugin plugin;

	@Before
	public void setUp() {
		String opennebulaConfFilePath = HomeDir.getPath() + SystemConstants.CLOUDS_CONFIGURATION_DIRECTORY_NAME
				+ File.separator + OPENNEBULA_CLOUD_NAME_DIRECTORY + File.separator
				+ SystemConstants.CLOUD_SPECIFICITY_CONF_FILE_NAME;

		this.plugin = Mockito.spy(new OpenNebulaImagePlugin(opennebulaConfFilePath));
	}
	
	// test case: When invoking the getAllImages method, with a valid client, a
	// collection of images must be loaded, returning a map of operating systems
	// images with ID and name.
	@Test
	public void testGetAllImagesSuccessfully() throws FogbowException {
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
		Mockito.when(image.xpath(Mockito.eq(String.format(OpenNebulaImagePlugin.FORMAT_IMAGE_TYPE_PATH, VALUE_ONE))))
				.thenReturn(OpenNebulaImagePlugin.OPERATIONAL_SYSTEM_IMAGE_TYPE);
		Mockito.when(image.getId()).thenReturn(FAKE_ID);
		Mockito.when(image.getName()).thenReturn(FAKE_NAME);

		CloudUser cloudUser = createCloudUser();

		// exercise
		Map<String, String> imagens = this.plugin.getAllImages(cloudUser);

		// verify
		PowerMockito.verifyStatic(OpenNebulaClientUtil.class, VerificationModeFactory.times(1));
		OpenNebulaClientUtil.createClient(Mockito.anyString(), Mockito.anyString());

		PowerMockito.verifyStatic(OpenNebulaClientUtil.class, VerificationModeFactory.times(1));
		OpenNebulaClientUtil.getImagePool(Mockito.eq(client));

		Mockito.verify(image, Mockito.times(1))
				.xpath(Mockito.eq(String.format(OpenNebulaImagePlugin.FORMAT_IMAGE_TYPE_PATH, VALUE_ONE)));
		Mockito.verify(image, Mockito.times(1)).getId();
		Mockito.verify(image, Mockito.times(1)).getName();
		
		Assert.assertTrue(imagens.containsKey(FAKE_ID));
		Assert.assertTrue(imagens.containsValue(FAKE_NAME));
	}
	
	// test case: When invoking the getAllImages method, with a collection of images
	// without any OS (Operational System) type, it must return an empty map.
	@Test
	public void testGetAllImagesWithoutOperationalSystemType() throws FogbowException {
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
		Mockito.when(image.xpath(Mockito.eq(String.format(OpenNebulaImagePlugin.FORMAT_IMAGE_TYPE_PATH, VALUE_ONE))))
				.thenReturn(ID_VALUE_ONE);

		CloudUser cloudUser = createCloudUser();

		// exercise
		Map<String, String> imagens = this.plugin.getAllImages(cloudUser);

		// verify
		PowerMockito.verifyStatic(OpenNebulaClientUtil.class, VerificationModeFactory.times(1));
		OpenNebulaClientUtil.createClient(Mockito.anyString(), Mockito.anyString());

		PowerMockito.verifyStatic(OpenNebulaClientUtil.class, VerificationModeFactory.times(1));
		OpenNebulaClientUtil.getImagePool(Mockito.eq(client));

		Mockito.verify(image, Mockito.times(1))
				.xpath(Mockito.eq(String.format(OpenNebulaImagePlugin.FORMAT_IMAGE_TYPE_PATH, VALUE_ONE)));

		Assert.assertTrue(imagens.isEmpty());
	}
	
	// test case: When invoking the getImage method, with a valid client, the image
	// of the specific ID passed by parameter will be loaded, to return its instance
	// with name, size and status.
	@Test
	public void testGetImageSuccessfully() throws FogbowException {
		// set up
		Client client = Mockito.mock(Client.class);
		PowerMockito.mockStatic(OpenNebulaClientUtil.class);
		BDDMockito.given(OpenNebulaClientUtil.createClient(Mockito.anyString(), Mockito.anyString()))
				.willReturn(client);

		String imageID = ID_VALUE_ONE;
		String name = FAKE_NAME;
		Image image = Mockito.mock(Image.class);
		BDDMockito.given(OpenNebulaClientUtil.getImage(Mockito.eq(client), Mockito.eq(imageID))).willReturn(image);

		Mockito.when(image.getId()).thenReturn(imageID);
		Mockito.when(image.getName()).thenReturn(name);
		Mockito.when(image.xpath(OpenNebulaImagePlugin.IMAGE_SIZE_PATH)).thenReturn(imageID);
		Mockito.when(image.state()).thenReturn(VALUE_ONE);

		CloudUser cloudUser = createCloudUser();

		// exercise
		this.plugin.getImage(imageID, cloudUser);

		// verify
		PowerMockito.verifyStatic(OpenNebulaClientUtil.class, VerificationModeFactory.times(1));
		OpenNebulaClientUtil.createClient(Mockito.anyString(), Mockito.anyString());

		PowerMockito.verifyStatic(OpenNebulaClientUtil.class, VerificationModeFactory.times(1));
		OpenNebulaClientUtil.getImage(Mockito.eq(client), Mockito.eq(imageID));

		Mockito.verify(image, Mockito.times(1)).getId();
		Mockito.verify(image, Mockito.times(1)).getName();
		Mockito.verify(image, Mockito.times(1)).xpath(OpenNebulaImagePlugin.IMAGE_SIZE_PATH);
		Mockito.verify(image, Mockito.times(1)).state();
	}
	
	// Test case: When invoking the getImage method, with a valid client and ID, and
	// unable to load a valid size from an instance in the cloud, it must throw an
	// InvalidParameterException.
	@Test(expected = InvalidParameterException.class) // verify
	public void testGetImageThrowInvalidParameterException() throws FogbowException {
		// set up
		Client client = Mockito.mock(Client.class);
		PowerMockito.mockStatic(OpenNebulaClientUtil.class);
		BDDMockito.given(OpenNebulaClientUtil.createClient(Mockito.anyString(), Mockito.anyString()))
				.willReturn(client);

		String imageID = ID_VALUE_ONE;
		Image image = Mockito.mock(Image.class);
		BDDMockito.given(OpenNebulaClientUtil.getImage(Mockito.eq(client), Mockito.eq(imageID))).willReturn(image);
		Mockito.when(image.xpath(OpenNebulaImagePlugin.IMAGE_SIZE_PATH)).thenReturn(EMPTY_STRING);

		CloudUser cloudUser = createCloudUser();

		// exercise
		this.plugin.getImage(imageID, cloudUser);
	}
	
	private CloudUser createCloudUser() {
		String userId = FAKE_ID;
		String userName = FAKE_NAME;
		String tokenValue = LOCAL_TOKEN_VALUE;

		CloudUser cloudUser = new CloudUser(userId, userName, tokenValue);
		return cloudUser;
	}
}
