package cloud.fogbow.ras.core.plugins.interoperability.opennebula.volume.v5_4;

import java.io.File;
import java.util.Properties;

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
import cloud.fogbow.common.exceptions.UnexpectedException;
import cloud.fogbow.common.models.CloudUser;
import cloud.fogbow.common.models.SystemUser;
import cloud.fogbow.common.util.HomeDir;
import cloud.fogbow.ras.constants.SystemConstants;
import cloud.fogbow.ras.core.models.orders.VolumeOrder;
import cloud.fogbow.ras.core.plugins.interoperability.opennebula.OpenNebulaClientUtil;

@RunWith(PowerMockRunner.class)
@PrepareForTest({Image.class, OpenNebulaClientUtil.class})
public class OpenNebulaVolumePluginTest {

	private static final String DISK_VALUE_8GB = "8";
	private static final String FAKE_VOLUME_NAME = "fake-volume-name";
	private static final String FAKE_USER_NAME = "fake-user-name";
	private static final String IMAGE_SIZE_PATH = "SIZE";
	private static final String LOCAL_TOKEN_VALUE = "user:password";
	private static final String STATE_READY = "READY";
	private static final String STRING_VALUE_ONE = "1";
	private static final String EMPTY_STRING = "";

	private OpenNebulaVolumePlugin plugin;

	@Before
	public void setUp() {
		String opennebulaConfFilePath = HomeDir.getPath() + SystemConstants.CLOUDS_CONFIGURATION_DIRECTORY_NAME
				+ File.separator + SystemConstants.OPENNEBULA_CLOUD_NAME_DIRECTORY + File.separator
				+ SystemConstants.CLOUD_SPECIFICITY_CONF_FILE_NAME;
		
		this.plugin = Mockito.spy(new OpenNebulaVolumePlugin(opennebulaConfFilePath));
	}

	// test case: When calling the getDataStoreId method without getting a valid
	// datastore ID from the configuration file, it must throw an
	// UnexpectedException.
	@Test(expected = UnexpectedException.class) // verify
	public void testGetDataStoreIdThrowUnexpectedException() throws FogbowException {
		// set up
		String key = OpenNebulaVolumePlugin.DEFAULT_DATASTORE_ID;
		Properties properties = Mockito.mock(Properties.class);
		Mockito.when(properties.getProperty(Mockito.eq(key))).thenReturn(EMPTY_STRING);
		this.plugin.setProperties(properties);

		// exercise
		this.plugin.getDataStoreId();
	}
	
	// test case: When calling the requestInstance method, with a valid client and a
	// request without a volume name, a template must be generated for a default
	// volume name and the other associated data, to allocate a volume, returning
	// its instance ID.
	@Test
	public void testRequestInstanceSuccessfullyWithDefaultVolumeName() throws FogbowException {
		// set up
		Client client = Mockito.mock(Client.class);
		PowerMockito.mockStatic(OpenNebulaClientUtil.class);
		BDDMockito.given(OpenNebulaClientUtil.createClient(Mockito.anyString(), Mockito.anyString()))
				.willReturn(client);

		Mockito.doReturn(FAKE_VOLUME_NAME).when(this.plugin).getRandomUUID();
		
		String template = generateImageTemplate(0);
		String instanceId = STRING_VALUE_ONE;

		BDDMockito.given(OpenNebulaClientUtil.allocateImage(Mockito.eq(client), Mockito.eq(template), Mockito.anyInt()))
				.willReturn(instanceId);

		OneResponse response = Mockito.mock(OneResponse.class);
		PowerMockito.mockStatic(Image.class);
		BDDMockito.given(Image.allocate(Mockito.eq(client), Mockito.eq(template), Mockito.anyInt()))
				.willReturn(response);
		Mockito.when(response.isError()).thenReturn(false);
		Mockito.when(response.getMessage()).thenReturn(instanceId);

		

		String volumeName = null;
		VolumeOrder volumeOrder = createVolumeOrder(volumeName);
		CloudUser cloudUser = createCloudUser();

		// exercise
		this.plugin.requestInstance(volumeOrder, cloudUser);

		// verify
		PowerMockito.verifyStatic(OpenNebulaClientUtil.class, VerificationModeFactory.times(1));
		OpenNebulaClientUtil.createClient(Mockito.anyString(), Mockito.anyString());

		PowerMockito.verifyStatic(OpenNebulaClientUtil.class, VerificationModeFactory.times(1));
		OpenNebulaClientUtil.allocateImage(Mockito.eq(client), Mockito.eq(template), Mockito.anyInt());
	}
	
	// test case: When calling the requestInstance method, with a valid client and a
	// request with a volume name, a template must be generated with the associated
	// data, to allocate a volume, returning its instance ID.
	@Test
	public void testRequestInstanceSuccessfullyWithoutDefaultVolumeName() throws FogbowException {
		// set up
		Client client = Mockito.mock(Client.class);
		PowerMockito.mockStatic(OpenNebulaClientUtil.class);
		BDDMockito.given(OpenNebulaClientUtil.createClient(Mockito.anyString(), Mockito.anyString()))
				.willReturn(client);

		String template = generateImageTemplate(1);
		String instanceId = STRING_VALUE_ONE;

		BDDMockito.given(OpenNebulaClientUtil.allocateImage(Mockito.eq(client), Mockito.eq(template), Mockito.anyInt()))
				.willReturn(instanceId);

		OneResponse response = Mockito.mock(OneResponse.class);
		PowerMockito.mockStatic(Image.class);
		BDDMockito.given(Image.allocate(Mockito.eq(client), Mockito.eq(template), Mockito.anyInt()))
				.willReturn(response);
		Mockito.when(response.isError()).thenReturn(false);
		Mockito.when(response.getMessage()).thenReturn(instanceId);

		CloudUser cloudUser = createCloudUser();
		VolumeOrder volumeOrder = createVolumeOrder(FAKE_VOLUME_NAME);

		// exercise
		this.plugin.requestInstance(volumeOrder, cloudUser);

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

		CloudUser cloudUser = createCloudUser();
		String instanceId = STRING_VALUE_ONE;
		
		// exercise
		this.plugin.getInstance(instanceId, cloudUser);

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

		CloudUser cloudUser = createCloudUser();
		String instanceId = STRING_VALUE_ONE;
		
		// exercise
		this.plugin.deleteInstance(instanceId, cloudUser);

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

		CloudUser cloudUser = createCloudUser();
		String instanceId = STRING_VALUE_ONE;
		
		// exercise
		this.plugin.deleteInstance(instanceId, cloudUser);

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
	
	private String generateImageTemplate(int choice) {
		String template = null;
		switch (choice) {
		case 0: 
			template = getTemplateWithDefaultName();
			break;
		case 1: 
			template = getTemplateWithouDefaultName();
			break;
		}
		return template;
	}

	private String getTemplateWithDefaultName() {
		String template = "<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"yes\"?>\n"
				+ "<IMAGE>\n"
				+ "    <DEV_PREFIX>vd</DEV_PREFIX>\n"
				+ "    <DISK_TYPE>BLOCK</DISK_TYPE>\n"
				+ "    <FSTYPE>raw</FSTYPE>\n"
				+ "    <PERSISTENT>YES</PERSISTENT>\n"
				+ "    <TYPE>DATABLOCK</TYPE>\n"
				+ "    <NAME>ras-volume-fake-volume-name</NAME>\n"
				+ "    <SIZE>1</SIZE>\n"
				+ "</IMAGE>\n";
		return template;
	}
	
	private String getTemplateWithouDefaultName() {
		String template = "<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"yes\"?>\n"
				+ "<IMAGE>\n"
				+ "    <DEV_PREFIX>vd</DEV_PREFIX>\n"
				+ "    <DISK_TYPE>BLOCK</DISK_TYPE>\n"
				+ "    <FSTYPE>raw</FSTYPE>\n"
				+ "    <PERSISTENT>YES</PERSISTENT>\n"
				+ "    <TYPE>DATABLOCK</TYPE>\n"
				+ "    <NAME>fake-volume-name</NAME>\n"
				+ "    <SIZE>1</SIZE>\n"
				+ "</IMAGE>\n";
		return template;
	}
	
	private VolumeOrder createVolumeOrder(String volumeName) {
		SystemUser systemUser = null;
		String requestingMember = null;
		String providingMember = null;
		String cloudName = null;
		String name = volumeName;
		int volumeSize = 1;
				
		VolumeOrder volumeOrder = new VolumeOrder(
				systemUser,
				requestingMember, 
				providingMember,
				cloudName,
				name, 
				volumeSize);
		
		return volumeOrder;
	}

	private CloudUser createCloudUser() {
		String userId = null;
		String userName = FAKE_USER_NAME;
		String tokenValue = LOCAL_TOKEN_VALUE;

		return new CloudUser(userId, userName, tokenValue);
	}
}
