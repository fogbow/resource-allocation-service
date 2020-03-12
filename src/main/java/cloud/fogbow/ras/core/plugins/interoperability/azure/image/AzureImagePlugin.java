package cloud.fogbow.ras.core.plugins.interoperability.azure;

import cloud.fogbow.common.constants.AzureConstants;
import cloud.fogbow.common.exceptions.FogbowException;
import cloud.fogbow.common.models.AzureUser;
import cloud.fogbow.common.util.AzureClientCacheManager;
import cloud.fogbow.common.util.PropertiesUtil;
import cloud.fogbow.ras.api.http.response.ImageInstance;
import cloud.fogbow.ras.api.http.response.ImageSummary;
import cloud.fogbow.ras.core.plugins.interoperability.ImagePlugin;
import cloud.fogbow.ras.core.plugins.interoperability.azure.compute.sdk.model.AzureGetImageRef;
import cloud.fogbow.ras.core.plugins.interoperability.azure.util.AzureImageOperationUtil;
import com.microsoft.azure.PagedList;
import com.microsoft.azure.management.Azure;
import com.microsoft.azure.management.compute.VirtualMachinePublisher;
import org.apache.commons.lang.NotImplementedException;

import java.util.ArrayList;
import java.util.List;
import java.util.Properties;
import java.util.stream.Stream;

public class AzureImagePlugin implements ImagePlugin<AzureUser> {

    private final String defaultRegionName;
    private final List<String> publishers;

    public AzureImagePlugin(String confFilePath) {
        Properties properties = PropertiesUtil.readProperties(confFilePath);
        this.defaultRegionName = properties.getProperty(AzureConstants.DEFAULT_REGION_NAME_KEY);
        this.publishers = new ArrayList<>();
        publishers.add("Canonical");
    }

    @Override
    public List<ImageSummary> getAllImages(AzureUser cloudUser) throws FogbowException {
        Azure azure = AzureClientCacheManager.getAzure(cloudUser);
        List<ImageSummary> images = new ArrayList<>();

        Stream<VirtualMachinePublisher> publishersStream = this.getPublishers(azure).stream()
                .filter(publisher -> publishers.contains(publisher.name()));

        publishersStream.forEach(publisher -> {
            publisher.offers().list().stream().forEach(offer -> {
                offer.skus().list().stream().forEach(sku -> {
                    AzureGetImageRef image = new AzureGetImageRef(publisher.toString(), offer.toString(), sku.toString());
                    String id = AzureImageOperationUtil.convertToImageSummaryId(image);
                    String name = AzureImageOperationUtil.convertToImageSummaryName(image);
                    images.add(new ImageSummary(id, name));
                });
            });
        });

        return images;
    }

    private PagedList<VirtualMachinePublisher> getPublishers(Azure azure) {
        return azure.virtualMachineImages().publishers().listByRegion(this.defaultRegionName);
    }

    @Override
    public ImageInstance getImage(String imageId, AzureUser cloudUser) throws FogbowException {
        // TODO(jadsonluan): Implement this method
        throw new NotImplementedException("getImage is not implemented yet");
    }
}
