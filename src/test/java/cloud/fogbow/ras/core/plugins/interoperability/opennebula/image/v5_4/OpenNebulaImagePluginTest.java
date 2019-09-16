package cloud.fogbow.ras.core.plugins.interoperability.opennebula.image.v5_4;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import cloud.fogbow.ras.api.http.response.ImageInstance;
import cloud.fogbow.ras.api.http.response.ImageSummary;
import cloud.fogbow.ras.api.http.response.InstanceState;
import cloud.fogbow.ras.core.TestUtils;
import cloud.fogbow.ras.core.datastore.DatabaseManager;
import cloud.fogbow.ras.core.models.ResourceType;
import cloud.fogbow.ras.core.plugins.interoperability.opennebula.OpenNebulaBaseTests;
import cloud.fogbow.ras.core.plugins.interoperability.opennebula.OpenNebulaStateMapper;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;
import org.opennebula.client.Client;
import org.opennebula.client.image.Image;
import org.opennebula.client.image.ImagePool;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.core.classloader.annotations.PrepareForTest;

import cloud.fogbow.common.exceptions.FogbowException;
import cloud.fogbow.ras.core.plugins.interoperability.opennebula.OpenNebulaClientUtil;

import static cloud.fogbow.ras.core.plugins.interoperability.opennebula.OpenNebulaStateMapper.IMAGE_READY_STATE;
import static cloud.fogbow.ras.core.plugins.interoperability.opennebula.image.v5_4.OpenNebulaImagePlugin.*;

@PrepareForTest({OpenNebulaClientUtil.class, OpenNebulaStateMapper.class, DatabaseManager.class})
public class OpenNebulaImagePluginTest extends OpenNebulaBaseTests {

	private static final String EMPTY_STRING = "";
	private static final String FAKE_ID = "fake-id";
	private static final String FAKE_NAME = "fake-name";
	private static final String ID_VALUE_ONE = "1";

	private OpenNebulaImagePlugin plugin;
	private Image image;

	@Before
	public void setUp() throws FogbowException {
		super.setUp();

		this.plugin = Mockito.spy(new OpenNebulaImagePlugin(this.openNebulaConfFilePath));
		this.image = Mockito.mock(Image.class);

		Mockito.when(this.image.getId()).thenReturn(FAKE_ID);
		Mockito.when(this.image.getName()).thenReturn(FAKE_NAME);
	}
	
	// test case: When invoking the getAllImages method, with a valid client, a
	// collection of images must be loaded, returning a list of operating systems
	// images with ID and name.
	@Test
	public void testGetAllImages() throws FogbowException {
		// set up
		ImagePool imagePool = Mockito.mock(ImagePool.class);
		List<ImageSummary> imageSummaryList = new ArrayList<>();

		Mockito.when(OpenNebulaClientUtil.getImagePool(Mockito.any())).thenReturn(imagePool);
		Mockito.doReturn(imageSummaryList).when(this.plugin).getImageSummaryList(imagePool);

		// exercise
		this.plugin.getAllImages(this.cloudUser);

		// verify
		PowerMockito.verifyStatic(OpenNebulaClientUtil.class, Mockito.times(TestUtils.RUN_ONCE));
		OpenNebulaClientUtil.createClient(Mockito.anyString(), Mockito.eq(this.cloudUser.getToken()));

		PowerMockito.verifyStatic(OpenNebulaClientUtil.class, Mockito.times(TestUtils.RUN_ONCE));
		OpenNebulaClientUtil.getImagePool(Mockito.any(Client.class));

		Mockito.verify(this.plugin, Mockito.times(TestUtils.RUN_ONCE)).getImageSummaryList(Mockito.eq(imagePool));
	}

	// test case: When invoking the getImage method, with a valid client, the image
	// of the specific ID passed by parameter will be loaded, to return its instance
	// with name, size and status.
	@Test
	public void testGetImage() throws FogbowException {
		// set up
		ImageInstance imageInstance = Mockito.mock(ImageInstance.class);

		Mockito.when(OpenNebulaClientUtil.getImage(Mockito.any(Client.class), Mockito.anyString())).thenReturn(image);
		Mockito.doReturn(imageInstance).when(this.plugin).mount(this.image);

		// exercise
		this.plugin.getImage(FAKE_ID, cloudUser);

		// verify
		PowerMockito.verifyStatic(OpenNebulaClientUtil.class, Mockito.times(TestUtils.RUN_ONCE));
		OpenNebulaClientUtil.createClient(Mockito.anyString(), Mockito.eq(this.cloudUser.getToken()));

		PowerMockito.verifyStatic(OpenNebulaClientUtil.class, Mockito.times(TestUtils.RUN_ONCE));
		OpenNebulaClientUtil.getImage(Mockito.any(Client.class), Mockito.eq(FAKE_ID));

		Mockito.verify(this.plugin, Mockito.times(TestUtils.RUN_ONCE)).mount(Mockito.eq(this.image));
	}

	// test case: when calling getImageSummaryList with a valid image pool, return a list
	// of the respective ImageSummary list
	@Test
	public void testGetImageSummaryList() {
	    // set up
		ImagePool imagePool = Mockito.mock(ImagePool.class);
		Iterator<Image> iterator = Mockito.mock(Iterator.class);
		String imageTypePath = String.format(FORMAT_IMAGE_TYPE_PATH, ID_VALUE_ONE);

		Mockito.when(iterator.hasNext()).thenReturn(true, false);
		Mockito.when(iterator.next()).thenReturn(image);
		Mockito.when(imagePool.iterator()).thenReturn(iterator);
		Mockito.when(this.image.xpath(Mockito.eq(imageTypePath))).thenReturn(OPERATIONAL_SYSTEM_IMAGE_TYPE);

		// exercise
		List<ImageSummary> imageSummaryList = this.plugin.getImageSummaryList(imagePool);

		// verify
		Mockito.verify(this.image, Mockito.times(TestUtils.RUN_ONCE)).xpath(imageTypePath);

		Assert.assertEquals(1, imageSummaryList.size());
		Assert.assertEquals(FAKE_ID, imageSummaryList.get(0).getId());
		Assert.assertEquals(FAKE_NAME, imageSummaryList.get(0).getName());
	}

	@Test
	public void testMount() {
	    // set up
		Mockito.when(this.image.state()).thenReturn(IMAGE_READY_STATE);
		Mockito.when(this.image.xpath(Mockito.eq(IMAGE_SIZE_PATH))).thenReturn(ID_VALUE_ONE);
		Mockito.doReturn(Integer.parseInt(ID_VALUE_ONE)).when(this.plugin).convertToInteger(ID_VALUE_ONE);

		PowerMockito.mockStatic(OpenNebulaStateMapper.class);
		Mockito.when(OpenNebulaStateMapper.map(Mockito.any(ResourceType.class), Mockito.anyInt()))
				.thenReturn(InstanceState.READY);

		// exercise
		ImageInstance instance = this.plugin.mount(this.image);

		// verify
        Mockito.verify(this.plugin, Mockito.times(TestUtils.RUN_ONCE)).convertToInteger(ID_VALUE_ONE);

		Assert.assertEquals(FAKE_ID, instance.getId());
		Assert.assertEquals(FAKE_NAME, instance.getName());
		Assert.assertEquals(InstanceState.READY.getValue(), instance.getStatus());
		Assert.assertEquals(Integer.parseInt(ID_VALUE_ONE), instance.getSize());
	}

	@Test
	public void testConvertToInteger() {
		// set up
		int expectedIntSuccess = 1;
		int expectedIntFail = 0;

		// exercise
		int convertedIntSuccess = this.plugin.convertToInteger(ID_VALUE_ONE);
		int convertedIntFail = this.plugin.convertToInteger(EMPTY_STRING);

		// verify
		Assert.assertEquals(expectedIntSuccess, convertedIntSuccess);
		Assert.assertEquals(expectedIntFail, convertedIntFail);
	}
}
