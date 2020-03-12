package cloud.fogbow.ras.core.plugins.interoperability.azure.image;

import cloud.fogbow.common.constants.AzureConstants;
import cloud.fogbow.common.exceptions.FatalErrorException;
import cloud.fogbow.common.exceptions.FogbowException;
import cloud.fogbow.common.models.AzureUser;
import cloud.fogbow.common.util.AzureClientCacheManager;
import cloud.fogbow.common.util.PropertiesUtil;
import cloud.fogbow.ras.api.http.response.ImageInstance;
import cloud.fogbow.ras.api.http.response.ImageSummary;
import cloud.fogbow.ras.constants.Messages;
import cloud.fogbow.ras.core.plugins.interoperability.ImagePlugin;
import cloud.fogbow.ras.core.plugins.interoperability.azure.util.AzureGeneralPolicy;
import cloud.fogbow.ras.core.plugins.interoperability.azure.util.AzureImageOperationUtil;
import com.google.common.annotations.VisibleForTesting;
import com.microsoft.azure.management.Azure;
import com.microsoft.azure.management.compute.VirtualMachineOffer;
import com.microsoft.azure.management.compute.VirtualMachinePublisher;
import com.microsoft.azure.management.compute.VirtualMachineSku;
import org.apache.commons.lang.NotImplementedException;

import java.util.*;

public class AzureImagePlugin implements ImagePlugin<AzureUser> {

    private final String defaultRegionName;
    private final List<String> publishers;
    private final AzureImageOperation operation;
    private static Map<String, ImageSummary> images = new HashMap<>();

    public AzureImagePlugin(String confFilePath) {
        Properties properties = PropertiesUtil.readProperties(confFilePath);
        this.defaultRegionName = properties.getProperty(AzureConstants.DEFAULT_REGION_NAME_KEY);
        AzureGeneralPolicy.checkRegionName(defaultRegionName);
        this.operation = new AzureImageOperation(this.defaultRegionName);
        this.publishers = this.loadPublishers(properties);
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

    @Override
    public List<ImageSummary> getAllImages(AzureUser cloudUser) throws FogbowException {
        Azure azure = AzureClientCacheManager.getAzure(cloudUser);

        if (this.images.isEmpty()) {
            this.images = this.loadImages(azure);
        }

        return new ArrayList<>(this.images.values());
    }

    @Override
    public ImageInstance getImage(String imageId, AzureUser cloudUser) throws FogbowException {
        // TODO(jadsonluan): Implement this method
        throw new NotImplementedException("getImage is not implemented yet");
    }

    @VisibleForTesting
    Map<String, ImageSummary> loadImages(Azure azure) {
        Map<String, ImageSummary> imageMap = new HashMap<>();

        for (VirtualMachinePublisher publisher : this.operation.getPublishers(azure)) {
            if (this.publishers.contains(publisher.name())) {
                for (VirtualMachineOffer offer : this.operation.getOffersFrom(publisher)) {
                    for (VirtualMachineSku sku : this.operation.getSkusFrom(offer)) {
                        String publisherName = publisher.name();
                        String offerName = offer.name();
                        String skuName = sku.name();
                        ImageSummary imageSummary = AzureImageOperationUtil.buildImageSummaryBy(publisherName, offerName, skuName);
                        imageMap.put(imageSummary.getId(), imageSummary);
                    }
                }
            }
        }

        return imageMap;
    }
}
