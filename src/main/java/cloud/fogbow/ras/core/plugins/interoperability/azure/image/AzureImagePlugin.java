package cloud.fogbow.ras.core.plugins.interoperability.azure.image;

import cloud.fogbow.common.constants.AzureConstants;
import cloud.fogbow.common.exceptions.FatalErrorException;
import cloud.fogbow.common.exceptions.FogbowException;
import cloud.fogbow.common.exceptions.InstanceNotFoundException;
import cloud.fogbow.common.exceptions.UnexpectedException;
import cloud.fogbow.common.models.AzureUser;
import cloud.fogbow.common.util.AzureClientCacheManager;
import cloud.fogbow.common.util.PropertiesUtil;
import cloud.fogbow.ras.api.http.response.ImageInstance;
import cloud.fogbow.ras.api.http.response.ImageSummary;
import cloud.fogbow.ras.constants.Messages;
import cloud.fogbow.ras.core.plugins.interoperability.ImagePlugin;
import cloud.fogbow.ras.core.plugins.interoperability.azure.util.AzureGeneralPolicy;
import com.google.common.annotations.VisibleForTesting;
import com.microsoft.azure.management.Azure;

import java.util.*;

public class AzureImagePlugin implements ImagePlugin<AzureUser> {

    private final String defaultRegionName;
    private final List<String> publishers;
    private AzureImageOperation operation;
    private static Map<String, ImageSummary> images = new HashMap<>();
    static final int NO_VALUE_FLAG = -1;
    public static final String ACTIVE_STATE = "active";

    public AzureImagePlugin(String confFilePath) {
        Properties properties = PropertiesUtil.readProperties(confFilePath);
        this.defaultRegionName = properties.getProperty(AzureConstants.DEFAULT_REGION_NAME_KEY);
        AzureGeneralPolicy.checkRegionName(defaultRegionName);
        this.operation = new AzureImageOperation(this.defaultRegionName);
        this.publishers = this.loadPublishers(properties);
    }

    @Override
    public List<ImageSummary> getAllImages(AzureUser cloudUser) throws FogbowException {
        Azure azure = AzureClientCacheManager.getAzure(cloudUser);
        List<ImageSummary> imageSummaryList = new ArrayList<>();

        for (Map.Entry<String, ImageSummary> entry : this.getImageMap(azure).entrySet()) {
            ImageSummary imageSummary = entry.getValue();
            String name = imageSummary.getName();
            String id = entry.getKey();
            imageSummaryList.add(new ImageSummary(id, name));
        }

        return imageSummaryList;
    }

    @Override
    public ImageInstance getImage(String imageId, AzureUser cloudUser) throws FogbowException {
        Azure azure = AzureClientCacheManager.getAzure(cloudUser);
        Map<String, ImageSummary> imageMap = getImageMap(azure);

        if (!imageMap.containsKey(imageId)) {
            throw new InstanceNotFoundException();
        }

        ImageSummary imageSummary = imageMap.get(imageId);
        String imageName = imageSummary.getName();

        return this.buildImageInstance(imageId, imageName);
    }

    @VisibleForTesting
    ImageInstance buildImageInstance(String id, String name) {
        String status = ACTIVE_STATE;
        long size = NO_VALUE_FLAG;
        long minDisk = NO_VALUE_FLAG;
        long minRam = NO_VALUE_FLAG;
        return new ImageInstance(id, name, size, minDisk, minRam, status);
    }

    @VisibleForTesting
    List<String> loadPublishers(Properties properties) {
        List<String> publishers = new ArrayList<>();
        String publisherList = properties.getProperty(AzureConstants.IMAGES_PUBLISHERS_KEY);

        if (publisherList == null || publisherList.isEmpty()) {
            throw new FatalErrorException(Messages.Exception.NO_IMAGES_PUBLISHER);
        }

        for (String publisher : publisherList.split(",")) {
            publisher = publisher.trim();
            publishers.add(publisher);
        }

        return publishers;
    }

    @VisibleForTesting
    Map<String, ImageSummary> getImageMap(Azure azure) throws UnexpectedException {
        if (images.isEmpty()) {
            images = this.operation.getImages(azure, this.publishers);
        }

        return images;
    }

    @VisibleForTesting
    void setAzureImageOperation(AzureImageOperation azureImageOperation) {
        this.operation = azureImageOperation;
    }
}
