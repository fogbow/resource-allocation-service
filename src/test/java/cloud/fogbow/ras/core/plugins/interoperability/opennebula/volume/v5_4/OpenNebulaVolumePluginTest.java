package cloud.fogbow.ras.core.plugins.interoperability.opennebula.volume.v5_4;

import cloud.fogbow.common.constants.OpenNebulaConstants;
import cloud.fogbow.common.exceptions.*;
import cloud.fogbow.ras.constants.Messages;
import cloud.fogbow.ras.core.SharedOrderHolders;
import cloud.fogbow.ras.core.TestUtils;
import cloud.fogbow.ras.core.datastore.DatabaseManager;
import cloud.fogbow.ras.core.plugins.interoperability.opennebula.OpenNebulaBaseTests;
import cloud.fogbow.ras.core.plugins.interoperability.opennebula.OpenNebulaStateMapper;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;
import org.opennebula.client.Client;
import org.opennebula.client.OneResponse;
import org.opennebula.client.datastore.Datastore;
import org.opennebula.client.datastore.DatastorePool;
import org.opennebula.client.image.Image;
import org.opennebula.client.image.ImagePool;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.core.classloader.annotations.PrepareForTest;

import cloud.fogbow.ras.core.models.orders.VolumeOrder;
import cloud.fogbow.ras.core.plugins.interoperability.opennebula.OpenNebulaClientUtil;

import java.util.Iterator;

import static cloud.fogbow.ras.core.plugins.interoperability.opennebula.volume.v5_4.OpenNebulaVolumePlugin.*;

@PrepareForTest({Image.class, OpenNebulaClientUtil.class, DatabaseManager.class})
public class OpenNebulaVolumePluginTest extends OpenNebulaBaseTests {

	private static final String DISK_VALUE_30GB = "30720";
	private static final String STRING_VALUE_ONE = "1";

	private OpenNebulaVolumePlugin plugin;
	private VolumeOrder volumeOrder;
	private String template;
	private Image image;
	private Client client;

	@Before
	public void setUp() throws FogbowException {
	    super.setUp();

		this.plugin = Mockito.spy(new OpenNebulaVolumePlugin(this.openNebulaConfFilePath));
		this.volumeOrder = this.createVolumeOrder();
		this.template = this.getTemplate();
		this.image = Mockito.mock(Image.class);
		this.client = Mockito.mock(Client.class);
	}

	// test case: When calling the isReady method with the cloud state READY,
	// this means that the state of attachment is READY and it must
	// return true.
	@Test
	public void testIsReady() {
		// set up
		String cloudState = OpenNebulaStateMapper.DEFAULT_READY_STATE;

        // exercise
        boolean status = this.plugin.isReady(cloudState);

        // verify
        Assert.assertTrue(status);
	}

	// test case: When calling the isReady method with the cloud states ERROR, this
	// means that the state of attachment is FAILED and it must return false.
	@Test
	public void testIsReadyFail() {
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
	public void testHasFailed() {
		// set up
		String cloudState = OpenNebulaStateMapper.DEFAULT_ERROR_STATE;

		// exercise
		boolean status = this.plugin.hasFailed(cloudState);

		// verify
		Assert.assertTrue(status);
	}

	// test case: When calling the hasFailed method with the cloud states USED or
	// READY, this means that the state of attachment is READY and it must
	// return false.
	@Test
	public void testHasFailedFail() {
		// set up
		String[] cloudStates = { OpenNebulaStateMapper.USED_STATE,
				OpenNebulaStateMapper.DEFAULT_READY_STATE };

		String cloudState;
		for (int i = 0; i < cloudStates.length; i++) {
			cloudState = cloudStates[i];

			// exercise
			boolean status = this.plugin.hasFailed(cloudState);

			// verify
			Assert.assertFalse(status);
		}
	}

	// test case: when calling requestInstance with valid volume order and cloud user,
	// the plugin should create a CreateVolumeRequest and pass it to the doRequestInstance
	// method, that should return the created volume instance id.
	@Test
	public void testRequestInstance() throws FogbowException {
		// set up
        Mockito.doReturn(STRING_VALUE_ONE).when(this.plugin).doRequestInstance(
        		Mockito.any(CreateVolumeRequest.class), Mockito.any(Client.class));

		// exercise
		String volumeId = this.plugin.requestInstance(this.volumeOrder, this.cloudUser);

		// verify
		PowerMockito.verifyStatic(OpenNebulaClientUtil.class, Mockito.times(TestUtils.RUN_ONCE));
		OpenNebulaClientUtil.createClient(Mockito.anyString(), Mockito.eq(this.cloudUser.getToken()));

		Mockito.verify(this.plugin, Mockito.times(TestUtils.RUN_ONCE)).doRequestInstance(
				Mockito.any(CreateVolumeRequest.class), Mockito.any(Client.class));

		Assert.assertEquals(volumeId, this.volumeOrder.getInstanceId());
	}

	// test case: When calling the doRequestInstance method, with valid create volume request and client,
	// the plugin should call getDataStoreId to discover where to store the volume and then allocate
	// the new volume with the request template.
	@Test
	public void testDoRequestInstance() throws UnexpectedException, InvalidParameterException, NoAvailableResourcesException {
		// set up
		int datastoreId = Integer.parseInt(STRING_VALUE_ONE);
		CreateVolumeRequest createVolumeRequest = Mockito.spy(this.createVolumeRequest(this.volumeOrder));
		Mockito.doReturn(datastoreId).when(this.plugin).getDataStoreId(Mockito.any(Client.class), Mockito.anyLong());

		// exercise
        this.plugin.doRequestInstance(createVolumeRequest, this.client);

		// verify
		PowerMockito.verifyStatic(OpenNebulaClientUtil.class, Mockito.times(TestUtils.RUN_ONCE));
		OpenNebulaClientUtil.allocateImage(Mockito.any(Client.class), Mockito.eq(this.template), Mockito.eq(datastoreId));

		Mockito.verify(createVolumeRequest, Mockito.times(TestUtils.RUN_ONCE)).getVolumeImage();
		Mockito.verify(this.plugin, Mockito.times(TestUtils.RUN_ONCE)).getDataStoreId(
				Mockito.any(Client.class), Mockito.eq(Long.parseLong(DISK_VALUE_30GB)));
	}

	// test case: when calling doRequestInstance and receiving a null value (meaning no datastore with enough available
	// free storage was found) from the getDataStoreId call, then a NoAvailableResources exception should be thrown
	@Test
	public void testDoRequestInstanceFail() throws UnexpectedException, InvalidParameterException {
		// set up
		CreateVolumeRequest createVolumeRequest = Mockito.spy(this.createVolumeRequest(this.volumeOrder));
		Mockito.doReturn(null).when(this.plugin).getDataStoreId(Mockito.any(Client.class), Mockito.anyLong());

		// exercise
        try {
			this.plugin.doRequestInstance(createVolumeRequest, this.client);
			Assert.fail();
		} catch (NoAvailableResourcesException e) {
        	Assert.assertEquals(e.getMessage(), "No available resources.");
		}

		// verify
		Mockito.verify(createVolumeRequest, Mockito.times(TestUtils.RUN_ONCE)).getVolumeImage();
		Mockito.verify(this.plugin, Mockito.times(TestUtils.RUN_ONCE)).getDataStoreId(
				Mockito.any(Client.class), Mockito.eq(Long.parseLong(DISK_VALUE_30GB)));
	}

	// test case: when calling getDataStoreId, with a valid client and volume size, then the id of the first datastore
	// with enough available free space should be returned
	@Test
	public void testGetDataStoreId() throws FogbowException {
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
		this.plugin.getDataStoreId(this.client, Long.parseLong(DISK_VALUE_30GB));

		// verify
		PowerMockito.verifyStatic(OpenNebulaClientUtil.class, Mockito.times(TestUtils.RUN_ONCE));
		OpenNebulaClientUtil.getDatastorePool(Mockito.any(Client.class));

		Mockito.verify(datastore, Mockito.times(TestUtils.RUN_ONCE)).typeStr();
		Mockito.verify(datastore, Mockito.times(TestUtils.RUN_ONCE)).xpath(Mockito.eq(diskSizePath));
		Mockito.verify(datastore, Mockito.times(TestUtils.RUN_ONCE)).id();
	}

	// test case: When calling the getInstance method, with the instance ID and a
	// valid token, a set of images will be loaded and the specific instance of the
	// image must be loaded.
	@Test
	public void testGetInstance() throws FogbowException {
		// set up
        Mockito.when(this.image.xpath(OpenNebulaConstants.SIZE)).thenReturn(DISK_VALUE_30GB);
		Mockito.doReturn(this.image).when(this.plugin).doGetInstance(Mockito.any(Client.class), Mockito.anyString());
		Mockito.doReturn(this.volumeOrder.getVolumeSize()).when(this.plugin).getImageSize(Mockito.anyString());

		// exercise
		this.plugin.getInstance(this.volumeOrder, this.cloudUser);

		// verify
		PowerMockito.verifyStatic(OpenNebulaClientUtil.class, Mockito.times(TestUtils.RUN_ONCE));
		OpenNebulaClientUtil.createClient(Mockito.anyString(), Mockito.eq(this.cloudUser.getToken()));

		Mockito.verify(this.plugin, Mockito.times(TestUtils.RUN_ONCE)).doGetInstance(
				Mockito.any(Client.class), Mockito.eq(this.volumeOrder.getInstanceId()));
		Mockito.verify(this.plugin, Mockito.times(TestUtils.RUN_ONCE)).getImageSize(Mockito.eq(DISK_VALUE_30GB));
		Mockito.verify(this.image, Mockito.times(TestUtils.RUN_ONCE)).xpath(Mockito.eq(OpenNebulaConstants.SIZE));
	}

	// test case: when calling doGetInstance with a valid client and volume instance id, return the ONe image
	// related to that volume order
	@Test
	public void testDoGetInstance() throws UnexpectedException, InstanceNotFoundException {
		// set up
		ImagePool imagePool = Mockito.mock(ImagePool.class);

		Mockito.when(OpenNebulaClientUtil.getImagePool(Mockito.any(Client.class))).thenReturn(imagePool);
		Mockito.when(imagePool.getById(Mockito.anyInt())).thenReturn(this.image);

		// exercise
		this.plugin.doGetInstance(this.client, this.volumeOrder.getInstanceId());

		// verify
		PowerMockito.verifyStatic(OpenNebulaClientUtil.class, Mockito.times(TestUtils.RUN_ONCE));
		OpenNebulaClientUtil.getImagePool(Mockito.any(Client.class));

		Mockito.verify(imagePool, Mockito.times(TestUtils.RUN_ONCE)).getById(Integer.parseInt(this.volumeOrder.getInstanceId()));
	}

	// test case: when calling doGetInstance with an nonexistent volume instance id, throw an InstanceNotFound exception
	@Test
	public void testDoGetInstanceFail() throws UnexpectedException {
		// set up
		ImagePool imagePool = Mockito.mock(ImagePool.class);

		Mockito.when(OpenNebulaClientUtil.getImagePool(Mockito.any(Client.class))).thenReturn(imagePool);
		Mockito.when(imagePool.getById(Mockito.anyInt())).thenReturn(null);

		// exercise
        try {
			this.plugin.doGetInstance(this.client, this.volumeOrder.getInstanceId());
			Assert.fail();
		} catch (InstanceNotFoundException e) {
        	Assert.assertEquals(e.getMessage(), "Instance not found.");
		}

		// verify
		PowerMockito.verifyStatic(OpenNebulaClientUtil.class, Mockito.times(TestUtils.RUN_ONCE));
		OpenNebulaClientUtil.getImagePool(Mockito.any(Client.class));

		Mockito.verify(imagePool, Mockito.times(TestUtils.RUN_ONCE)).getById(Integer.parseInt(this.volumeOrder.getInstanceId()));
	}

	// test case: when calling getImageSize with a int-parsable disk size string in MB, return its int GB value.
	@Test
	public void testGetImageSize() {
		// set up
		int expectedImageSizeInGb = 30;

		// exercise
        int imageSize = this.plugin.getImageSize(DISK_VALUE_30GB);

        // verify
		Assert.assertEquals(expectedImageSizeInGb, imageSize);
	}

	// test case: When calling the deleteInstance method, with the instance ID and a
	// valid token, a set of images will be loaded and the image specified for
	// removal will be deleted.
	@Test
	public void testDeleteInstance() throws FogbowException {
		// set up
        OneResponse response = Mockito.mock(OneResponse.class);

        Mockito.doReturn(this.image).when(this.plugin).doGetInstance(Mockito.any(Client.class), Mockito.anyString());
        Mockito.when(this.image.delete()).thenReturn(response);
        Mockito.when(response.isError()).thenReturn(false);

        // exercise
		this.plugin.deleteInstance(this.volumeOrder, this.cloudUser);

		// verify
		PowerMockito.verifyStatic(OpenNebulaClientUtil.class, Mockito.times(TestUtils.RUN_ONCE));
		OpenNebulaClientUtil.createClient(Mockito.anyString(), Mockito.eq(this.cloudUser.getToken()));

		Mockito.verify(this.plugin, Mockito.times(TestUtils.RUN_ONCE)).doGetInstance(
				Mockito.any(Client.class), Mockito.eq(this.volumeOrder.getInstanceId()));
		Mockito.verify(response, Mockito.times(TestUtils.RUN_ONCE)).isError();
	}

	// test case: When calling the deleteInstance method, if the removal call is not
	// answered an error response is returned.
	@Test
	public void testDeleteInstanceFail() throws FogbowException {
		// set up
		OneResponse response = Mockito.mock(OneResponse.class);
		String message = String.format(
				Messages.Error.ERROR_WHILE_REMOVING_VOLUME_IMAGE, this.volumeOrder.getInstanceId(), null);

		Mockito.doReturn(this.image).when(this.plugin).doGetInstance(Mockito.any(Client.class), Mockito.anyString());
		Mockito.when(this.image.delete()).thenReturn(response);
		Mockito.when(response.isError()).thenReturn(true);

		// exercise
		try {
			this.plugin.deleteInstance(this.volumeOrder, this.cloudUser);
			Assert.fail();
		} catch (UnexpectedException e) {
			Assert.assertEquals(e.getMessage(), message);
		}

		// verify
		PowerMockito.verifyStatic(OpenNebulaClientUtil.class, Mockito.times(TestUtils.RUN_ONCE));
		OpenNebulaClientUtil.createClient(Mockito.anyString(), Mockito.eq(this.cloudUser.getToken()));

		Mockito.verify(this.plugin, Mockito.times(TestUtils.RUN_ONCE)).doGetInstance(
				Mockito.any(Client.class), Mockito.eq(this.volumeOrder.getInstanceId()));
		Mockito.verify(response, Mockito.times(TestUtils.RUN_ONCE)).isError();
		Mockito.verify(response, Mockito.times(TestUtils.RUN_ONCE)).getMessage();
	}

	private String getTemplate() {
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

	private CreateVolumeRequest createVolumeRequest(VolumeOrder order) {
		String name = order.getName();
		String imagePersistent = PERSISTENT_DISK_CONFIRMATION;
		String imageType = DATABLOCK_IMAGE_TYPE;
		String fileSystemType = FILE_SYSTEM_TYPE_RAW;
		String diskType = BLOCK_DISK_TYPE;
		String devicePrefix = DEFAULT_DATASTORE_DEVICE_PREFIX;
		long size = Long.parseLong(DISK_VALUE_30GB);

		CreateVolumeRequest request = new CreateVolumeRequest.Builder()
				.name(name)
				.size(size)
				.imagePersistent(imagePersistent)
				.imageType(imageType)
				.fileSystemType(fileSystemType)
				.diskType(diskType)
				.devicePrefix(devicePrefix)
				.build();

		return request;
	}
}
