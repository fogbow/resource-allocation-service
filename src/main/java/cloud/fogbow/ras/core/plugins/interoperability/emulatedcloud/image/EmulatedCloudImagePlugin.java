package cloud.fogbow.ras.core.plugins.interoperability.emulatedcloud.image;

import cloud.fogbow.common.exceptions.FatalErrorException;
import cloud.fogbow.common.exceptions.FogbowException;
import cloud.fogbow.common.exceptions.InstanceNotFoundException;
import cloud.fogbow.common.models.CloudUser;
import cloud.fogbow.common.util.PropertiesUtil;
import cloud.fogbow.ras.api.http.response.ImageInstance;
import cloud.fogbow.ras.api.http.response.ImageSummary;
import cloud.fogbow.ras.constants.Messages;
import cloud.fogbow.ras.core.plugins.interoperability.ImagePlugin;
import cloud.fogbow.ras.core.plugins.interoperability.emulatedcloud.EmulatedCloudConstants;
import cloud.fogbow.ras.core.plugins.interoperability.emulatedcloud.sdk.image.EmulatedCloudImageManager;
import cloud.fogbow.ras.core.plugins.interoperability.emulatedcloud.sdk.image.models.EmulatedImage;

import java.util.*;

public class EmulatedCloudImagePlugin implements ImagePlugin<CloudUser> {

    private Properties properties;

    private static final String DEFAULT_IMAGE_STATUS = "active";
    private static final long DEFAULT_IMAGE_SIZE = 2164195328L;
    private static final long DEFAULT_IMAGE_MIN_DISK = 3;
    private static final long DEFAULT_IMAGE_MIN_RAM = 0;

    public EmulatedCloudImagePlugin(String confFilePath) {
        this.properties = PropertiesUtil.readProperties(confFilePath);

        String imageNames = properties.getProperty(EmulatedCloudConstants.Conf.IMAGE_NAMES_KEY);
        if (imageNames == null || imageNames.isEmpty()) {
            throw new FatalErrorException(EmulatedCloudConstants.Exception.NO_IMAGE_NAMES_SPECIFIED);
        }
    }

    @Override
    public List<ImageSummary> getAllImages(CloudUser cloudUser) throws FogbowException {
        List<ImageSummary> imageSummaries = new ArrayList<>();
        List<EmulatedImage> allImages = EmulatedCloudImageManager.getInstance(properties).list();

        for (EmulatedImage emulatedImage : allImages) {
            String imageId = emulatedImage.getInstanceId();
            String imageName = emulatedImage.getName();
            ImageSummary imageSummary = new ImageSummary(imageId, imageName);
            imageSummaries.add(imageSummary);
        }

        return imageSummaries;
    }

    @Override
    public ImageInstance getImage(String imageId, CloudUser cloudUser) throws FogbowException {
        Optional<EmulatedImage> emulatedImage = EmulatedCloudImageManager.getInstance(properties).find(imageId);

        if (emulatedImage.isPresent()) {
            return buildImageInstance(emulatedImage.get());
        } else {
            throw new InstanceNotFoundException(Messages.Exception.IMAGE_NOT_FOUND);
        }
    }

    private ImageInstance buildImageInstance(EmulatedImage emulatedImage) {
        return new ImageInstance(
                emulatedImage.getInstanceId(),
                emulatedImage.getName(),
                DEFAULT_IMAGE_SIZE,
                DEFAULT_IMAGE_MIN_DISK,
                DEFAULT_IMAGE_MIN_RAM,
                DEFAULT_IMAGE_STATUS
        );
    }
}
