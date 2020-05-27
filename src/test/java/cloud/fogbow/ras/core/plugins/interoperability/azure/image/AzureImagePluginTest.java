package cloud.fogbow.ras.core.plugins.interoperability.azure.image;

import cloud.fogbow.common.constants.AzureConstants;
import cloud.fogbow.common.exceptions.FatalErrorException;
import cloud.fogbow.common.exceptions.FogbowException;
import cloud.fogbow.common.exceptions.InstanceNotFoundException;
import cloud.fogbow.common.exceptions.UnauthenticatedUserException;
import cloud.fogbow.common.models.AzureUser;
import cloud.fogbow.common.util.AzureClientCacheManager;
import cloud.fogbow.common.util.HomeDir;
import cloud.fogbow.common.util.PropertiesUtil;
import cloud.fogbow.ras.api.http.response.ImageInstance;
import cloud.fogbow.ras.api.http.response.ImageSummary;
import cloud.fogbow.ras.constants.Messages;
import cloud.fogbow.ras.constants.SystemConstants;
import cloud.fogbow.ras.core.TestUtils;
import cloud.fogbow.ras.core.plugins.interoperability.azure.AzureTestUtils;
import com.microsoft.azure.management.Azure;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.junit.runner.RunWith;
import org.mockito.Mockito;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;

import java.io.File;
import java.util.*;

@RunWith(PowerMockRunner.class)
@PrepareForTest({AzureClientCacheManager.class})
public class AzureImagePluginTest extends AzureTestUtils {
    private static final String IMAGE_SUMMARY_NAME = "image-summary-name";
    private static final String IMAGE_SUMMARY_ID = "image-summary-id";
    private AzureImagePlugin plugin;
    private AzureImageOperation operation;
    private AzureUser azureUser;
    private Azure azure;

    @Rule
    private ExpectedException expectedException = ExpectedException.none();
    private Properties properties;

    @Before
    public void setUp() {
        String azureConfFilePath = HomeDir.getPath() +
                SystemConstants.CLOUDS_CONFIGURATION_DIRECTORY_NAME + File.separator
                + AzureTestUtils.AZURE_CLOUD_NAME + File.separator
                + SystemConstants.CLOUD_SPECIFICITY_CONF_FILE_NAME;
        this.properties = PropertiesUtil.readProperties(azureConfFilePath);
        this.plugin = Mockito.spy(new AzureImagePlugin(azureConfFilePath));
        this.operation = Mockito.mock(AzureImageOperation.class);
        this.plugin.setAzureImageOperation(this.operation);
        this.azureUser = AzureTestUtils.createAzureUser();
        this.azure = null;
    }

    // test case: When calling getAllImages method with all secondary methods
    // mocked, it must return a list of ImageSummary
    @Test
    public void testGetAllImagesSuccessfully() throws FogbowException {
        // set up
        mockGetAzureClient();
        Map<String, ImageSummary> images = new HashMap<>();
        this.putImage(IMAGE_SUMMARY_NAME, images);
        Mockito.doReturn(images).when(this.plugin).getImageMap(Mockito.eq(azure));

        // exercise
        List<ImageSummary> allImages = this.plugin.getAllImages(azureUser);

        // verify
        Mockito.verify(this.plugin, Mockito.times(TestUtils.RUN_ONCE)).getImageMap(Mockito.eq(azure));
        Assert.assertFalse(allImages.isEmpty());
    }

    // test case: When calling getImage method with all secondary methods
    // mocked, it must return the right ImageInstance object
    @Test
    public void testGetImageSuccessfully() throws FogbowException {
        // set up
        mockGetAzureClient();

        Map<String, ImageSummary> images = new HashMap<>();
        String imageId = this.putImage(IMAGE_SUMMARY_NAME, images);
        Mockito.doReturn(images).when(this.plugin).getImageMap(Mockito.eq(azure));

        // exercise
        ImageInstance image = this.plugin.getImage(imageId, this.azureUser);

        // verify
        Mockito.verify(this.plugin, Mockito.times(TestUtils.RUN_ONCE)).getImageMap(Mockito.eq(this.azure));

        Assert.assertEquals(IMAGE_SUMMARY_NAME, image.getName());
        Assert.assertEquals(imageId, image.getId());
    }

    // test case: When calling getImage method with a id from a nonexistent image
    // it must throw a InstanceNotFoundException
    @Test
    public void testGetImageNotFound() throws FogbowException {
        // set up
        mockGetAzureClient();

        String imageId = generateUUID();
        Map<String, ImageSummary> images = new HashMap<>();
        Mockito.doReturn(images).when(this.plugin).getImageMap(Mockito.eq(azure));

        // verify
        this.expectedException.expect(InstanceNotFoundException.class);

        // exercise
        this.plugin.getImage(imageId, this.azureUser);
    }

    // test case: When calling buildImageInstance, it must verify if it returns
    // the right ImageInstance object
    @Test
    public void testBuildImageInstanceSuccessfully() {
        // set up
        long expectedSize = AzureImagePlugin.NO_VALUE_FLAG;
        long expectedMinRam = AzureImagePlugin.NO_VALUE_FLAG;
        long expectedMinDisk = AzureImagePlugin.NO_VALUE_FLAG;
        String expectedStatus = AzureImagePlugin.ACTIVE_STATE;
        String expectedName = IMAGE_SUMMARY_NAME;

        String id = this.generateUUID();

        // exercise
        ImageInstance imageInstance = this.plugin.buildImageInstance(id, IMAGE_SUMMARY_NAME);

        // verify
        Assert.assertEquals(id, imageInstance.getId());
        Assert.assertEquals(expectedName, imageInstance.getName());
        Assert.assertEquals(expectedStatus, imageInstance.getStatus());
        Assert.assertEquals(expectedMinDisk, imageInstance.getMinDisk());
        Assert.assertEquals(expectedMinRam, imageInstance.getMinRam());
        Assert.assertEquals(expectedSize, imageInstance.getSize());
    }

    // test case: When calling loadPublishers method with all secondary methods
    // mocked and given a valid properties object, it must return the right list of publishers
    @Test
    public void testLoadPublishersSuccessfully() {
        // set up
        List<String> expectedPublishers = new ArrayList<>();
        String publisherList = this.properties.getProperty(AzureConstants.IMAGES_PUBLISHERS_KEY);
        for (String publisher : publisherList.split(",")) expectedPublishers.add(publisher.trim());

        // exercise
        List<String> publishers = this.plugin.loadPublishers(this.properties);

        // verify
        Assert.assertEquals(expectedPublishers, publishers);
    }

    // test case: When calling loadPublishers method with a Properties object
    // without the images publisher property, it must throw a FatalErrorException
    @Test
    public void testLoadPublishersFail() {
        // set up
        Properties properties = Mockito.mock(Properties.class);

        // verify
        expectedException.expect(FatalErrorException.class);
        expectedException.expectMessage(Messages.Exception.NO_IMAGES_PUBLISHER);

        // exercise
        this.plugin.loadPublishers(properties);
    }

    private String putImage(String name, Map<String, ImageSummary> images) {
        String uuid = generateUUID();
        ImageSummary image = new ImageSummary(IMAGE_SUMMARY_ID, name);
        images.put(uuid, image);
        return uuid;
    }

    private String generateUUID() {
        return UUID.randomUUID().toString();
    }

    private void mockGetAzureClient() throws UnauthenticatedUserException {
        PowerMockito.mockStatic(AzureClientCacheManager.class);
        PowerMockito.when(AzureClientCacheManager.getAzure(Mockito.eq(this.azureUser)))
                .thenReturn(azure);
    }
}
