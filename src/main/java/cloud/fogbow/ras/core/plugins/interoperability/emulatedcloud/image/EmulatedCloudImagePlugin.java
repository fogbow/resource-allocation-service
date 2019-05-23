package cloud.fogbow.ras.core.plugins.interoperability.emulatedcloud.image;

import cloud.fogbow.common.exceptions.FogbowException;
import cloud.fogbow.common.models.CloudUser;
import cloud.fogbow.common.util.PropertiesUtil;
import cloud.fogbow.ras.api.http.response.ImageInstance;
import cloud.fogbow.ras.constants.Messages;
import cloud.fogbow.ras.core.plugins.interoperability.ImagePlugin;
import cloud.fogbow.ras.core.plugins.interoperability.emulatedcloud.EmulatedCloudConstants;
import cloud.fogbow.ras.core.plugins.interoperability.emulatedcloud.EmulatedCloudUtils;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;

public class EmulatedCloudImagePlugin implements ImagePlugin<CloudUser> {

    private Properties properties;

    private static final String DEFAULT_IMAGE_STATUS = "active";
    private static final long DEFAULT_IMAGE_SIZE = 2164195328L;
    private static final long DEFAULT_IMAGE_MIN_DISK = 3;
    private static final long DEFAULT_IMAGE_MIN_RAM = 0;

    public EmulatedCloudImagePlugin(String confFilePath) {
        this.properties = PropertiesUtil.readProperties(confFilePath);
    }

    @Override
    public Map<String, String> getAllImages(CloudUser cloudUser) throws FogbowException {
        String imagesPath = EmulatedCloudUtils.getStaticResourcesPath(
                this.properties, EmulatedCloudConstants.File.ALL_IMAGES);

        HashMap allImages;

        try {

            allImages = EmulatedCloudUtils.readJsonAsHashMap(imagesPath);
        } catch (IOException e) {
            throw new FogbowException(e.getMessage());
        }

        return allImages;
    }

    @Override
    public ImageInstance getImage(String imageId, CloudUser cloudUser) throws FogbowException {

        Map<String, String> allImages = this.getAllImages(cloudUser);

        if (!allImages.containsKey(imageId)){
            throw new FogbowException(Messages.Exception.IMAGE_NOT_FOUND);
        }

        String imageName = allImages.get(imageId);

        return new ImageInstance(
                imageId,
                imageName,
                DEFAULT_IMAGE_SIZE,
                DEFAULT_IMAGE_MIN_DISK,
                DEFAULT_IMAGE_MIN_RAM,
                DEFAULT_IMAGE_STATUS
        );
    }
}
