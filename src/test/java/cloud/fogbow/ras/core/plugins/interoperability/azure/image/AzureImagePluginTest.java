package cloud.fogbow.ras.core.plugins.interoperability.azure.image;

import cloud.fogbow.common.constants.AzureConstants;
import cloud.fogbow.common.models.AzureUser;
import cloud.fogbow.common.util.HomeDir;
import cloud.fogbow.common.util.PropertiesUtil;
import cloud.fogbow.ras.api.http.response.ImageSummary;
import cloud.fogbow.ras.constants.SystemConstants;
import cloud.fogbow.ras.core.plugins.interoperability.azure.AzureTestUtils;
import com.microsoft.azure.management.Azure;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;

import java.io.File;
import java.util.*;

public class AzureImagePluginTest extends AzureTestUtils {
    private static final String IMAGE_SUMMARY_NAME = "image-summary-name";
    private static final String IMAGE_SUMMARY_ID = "image-summary-id";
    private String defaultRegionName;
    private AzureImagePlugin plugin;
    private AzureImageOperation operation;
    private AzureUser azureUser;
    private Azure azure;

    @Before
    public void setUp() {
        String azureConfFilePath = HomeDir.getPath() +
                SystemConstants.CLOUDS_CONFIGURATION_DIRECTORY_NAME + File.separator
                + AzureTestUtils.AZURE_CLOUD_NAME + File.separator
                + SystemConstants.CLOUD_SPECIFICITY_CONF_FILE_NAME;
        Properties properties = PropertiesUtil.readProperties(azureConfFilePath);
        this.defaultRegionName = properties.getProperty(AzureConstants.DEFAULT_REGION_NAME_KEY);
        this.plugin = Mockito.spy(new AzureImagePlugin(azureConfFilePath));
        this.operation = Mockito.mock(AzureImageOperation.class);
        this.plugin.setAzureImageOperation(this.operation);
        this.azureUser = AzureTestUtils.createAzureUser();
        this.azure = null;
    }

    // test case: When calling getAllImages method with all secondary methods
    // mocked, it must return a list of ImageSummary
    @Test
    public void testGetAllImagesSuccessfully() {
        // set up
        Map<String, ImageSummary> images = new HashMap<>();
        String uuid = generateUUID();
        ImageSummary imageSummary = createImageSummary();

        Mockito.doReturn(images).when(this.plugin).getImageMap(azure);
    }

    private ImageSummary createImageSummary() {
        return createImageSummary(IMAGE_SUMMARY_ID, IMAGE_SUMMARY_NAME);
    }

    private ImageSummary createImageSummary(String id, String name) {
        return new ImageSummary(id, name);
    }

    private String generateUUID() {
        return UUID.randomUUID().toString();
    }
}
