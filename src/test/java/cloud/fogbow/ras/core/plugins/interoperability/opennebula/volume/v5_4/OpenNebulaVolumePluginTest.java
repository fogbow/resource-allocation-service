package cloud.fogbow.ras.core.plugins.interoperability.opennebula.volume.v5_4;

import cloud.fogbow.ras.core.SharedOrderHolders;
import cloud.fogbow.ras.core.TestUtils;
import cloud.fogbow.ras.core.datastore.DatabaseManager;
import cloud.fogbow.ras.core.plugins.interoperability.opennebula.OpenNebulaBaseTests;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.BDDMockito;
import org.mockito.Mockito;
import org.mockito.internal.verification.VerificationModeFactory;
import org.opennebula.client.Client;
import org.opennebula.client.OneResponse;
import org.opennebula.client.datastore.Datastore;
import org.opennebula.client.datastore.DatastorePool;
import org.opennebula.client.image.Image;
import org.opennebula.client.image.ImagePool;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;

import cloud.fogbow.common.exceptions.FogbowException;
import cloud.fogbow.ras.core.models.orders.VolumeOrder;
import cloud.fogbow.ras.core.plugins.interoperability.opennebula.OpenNebulaClientUtil;

import java.util.Iterator;

import static cloud.fogbow.ras.core.plugins.interoperability.opennebula.volume.v5_4.OpenNebulaVolumePlugin.DATASTORE_FREE_PATH_FORMAT;
import static cloud.fogbow.ras.core.plugins.interoperability.opennebula.volume.v5_4.OpenNebulaVolumePlugin.IMAGE_TYPE;

@RunWith(PowerMockRunner.class)
@PrepareForTest({Image.class, OpenNebulaClientUtil.class, SharedOrderHolders.class, DatabaseManager.class})
public class OpenNebulaVolumePluginTest extends OpenNebulaBaseTests {

	private static final String DISK_VALUE_30GB = "30720";
	private static final String FAKE_VOLUME_NAME = "fake-volume-name";
	private static final String IMAGE_SIZE_PATH = "SIZE";
	private static final String STATE_READY = "READY";
	private static final String STRING_VALUE_ONE = "1";
	private static final String EMPTY_STRING = "";

	private OpenNebulaVolumePlugin plugin;
	private VolumeOrder volumeOrder;
	private String template;
	private OneResponse response;

	@Before
	public void setUp() throws FogbowException {
	    super.setUp();

		this.plugin = Mockito.spy(new OpenNebulaVolumePlugin(this.openNebulaConfFilePath));
		this.volumeOrder = this.createVolumeOrder();
		this.template = this.getTemplateWithouDefaultName();
		this.response = Mockito.mock(OneResponse.class);

		Mockito.when(this.response.getMessage()).thenReturn(this.volumeOrder.getInstanceId());
	}

	// test case: When calling the requestInstance method, with valid client and
	// request, a template must be generated for a default
	// volume name and the other associated data, to allocate a volume, returning
	// its instance ID.
	@Test
	public void testRequestInstance() throws FogbowException {
		// set up
        Mockito.doReturn(STRING_VALUE_ONE).when(this.plugin).doRequestInstance(
        		Mockito.any(CreateVolumeRequest.class), Mockito.any(Client.class));

		// exercise
		this.plugin.requestInstance(this.volumeOrder, this.cloudUser);

		// verify
		PowerMockito.verifyStatic(OpenNebulaClientUtil.class, Mockito.times(TestUtils.RUN_ONCE));
		OpenNebulaClientUtil.createClient(Mockito.anyString(), Mockito.eq(this.cloudUser.getToken()));

		Mockito.verify(this.plugin, Mockito.times(TestUtils.RUN_ONCE)).doRequestInstance(
				Mockito.any(CreateVolumeRequest.class), Mockito.any(Client.class));
	}

	// test case: When calling the requestInstance method, with valid client and
	// request, a template must be generated for a default
	// volume name and the other associated data, to allocate a volume, returning
	// its instance ID.
	@Test
	public void testDoRequestInstance() throws FogbowException {
		// set up
		int datastoreId = 0;
		DatastorePool datastorePool = Mockito.mock(DatastorePool.class);
		Datastore datastore = Mockito.mock(Datastore.class);
		Iterator<Datastore> iterator = Mockito.mock(Iterator.class);
		String diskSizePath = String.format(DATASTORE_FREE_PATH_FORMAT, STRING_VALUE_ONE);

		Mockito.when(iterator.hasNext()).thenReturn(true, false);
		Mockito.when(iterator.next()).thenReturn(datastore);
		Mockito.when(datastorePool.iterator()).thenReturn(iterator);
		Mockito.when(datastore.typeStr()).thenReturn(IMAGE_TYPE);
		Mockito.when(datastore.xpath(Mockito.eq(diskSizePath))).thenReturn(DISK_VALUE_30GB);
		Mockito.when(datastore.id()).thenReturn(datastoreId);
		Mockito.when(OpenNebulaClientUtil.getDatastorePool(Mockito.any(Client.class))).thenReturn(datastorePool);

		// exercise
		this.plugin.requestInstance(this.volumeOrder, this.cloudUser);

		// verify
		PowerMockito.verifyStatic(OpenNebulaClientUtil.class, Mockito.times(TestUtils.RUN_ONCE));
		OpenNebulaClientUtil.createClient(Mockito.anyString(), Mockito.eq(this.cloudUser.getToken()));

		PowerMockito.verifyStatic(OpenNebulaClientUtil.class, Mockito.times(TestUtils.RUN_ONCE));
		OpenNebulaClientUtil.allocateImage(Mockito.any(Client.class), Mockito.eq(this.template), Mockito.eq(datastoreId));

		Mockito.verify(this.plugin, Mockito.times(TestUtils.RUN_ONCE)).doRequestInstance(
				Mockito.any(CreateVolumeRequest.class), Mockito.any(Client.class));
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

		// exercise
		this.plugin.requestInstance(this.volumeOrder, this.cloudUser);

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
		Mockito.when(image.xpath(IMAGE_SIZE_PATH)).thenReturn(DISK_VALUE_30GB);
		Mockito.when(image.getName()).thenReturn(FAKE_VOLUME_NAME);
		Mockito.when(image.stateString()).thenReturn(STATE_READY);

		// exercise
		this.plugin.getInstance(this.volumeOrder, this.cloudUser);

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

		// exercise
		this.plugin.deleteInstance(this.volumeOrder, this.cloudUser);

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

		// exercise
		this.plugin.deleteInstance(this.volumeOrder, this.cloudUser);

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
				+ "    <NAME>fogbow-fake-order-name</NAME>\n"
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
				+ "    <NAME>fake-order-name</NAME>\n"
				+ "    <SIZE>30720</SIZE>\n"
				+ "</IMAGE>\n";
		return template;
	}
	
	private VolumeOrder createVolumeOrder() {
	    VolumeOrder volumeOrder = this.testUtils.createLocalVolumeOrder();
	    volumeOrder.setInstanceId(STRING_VALUE_ONE);
		SharedOrderHolders.getInstance().getActiveOrdersMap().put(volumeOrder.getId(), volumeOrder);

		return volumeOrder;
	}
}
