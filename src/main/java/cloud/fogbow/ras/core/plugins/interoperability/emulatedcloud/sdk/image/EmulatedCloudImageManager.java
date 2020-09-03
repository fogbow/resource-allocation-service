package cloud.fogbow.ras.core.plugins.interoperability.emulatedcloud.sdk.image;

import cloud.fogbow.common.exceptions.FatalErrorException;
import cloud.fogbow.ras.core.plugins.interoperability.emulatedcloud.EmulatedCloudConstants;
import cloud.fogbow.ras.core.plugins.interoperability.emulatedcloud.EmulatedCloudUtils;
import cloud.fogbow.ras.core.plugins.interoperability.emulatedcloud.sdk.ResourceManager;
import cloud.fogbow.ras.core.plugins.interoperability.emulatedcloud.sdk.image.models.EmulatedImage;

import java.security.InvalidParameterException;
import java.util.*;

public class EmulatedCloudImageManager implements ResourceManager<EmulatedImage> {
    private static EmulatedCloudImageManager instance;
    private Map<String, EmulatedImage> images;

    private EmulatedCloudImageManager(Properties properties) {
        this.images = new HashMap<>();
        this.loadDefaultImages(properties);
    }

    public static EmulatedCloudImageManager getInstance(Properties properties) {
        if (instance == null) {
            instance = new EmulatedCloudImageManager(properties);
        }
        return instance;
    }


    @Override
    public Optional<EmulatedImage> find(String instanceId) {
        return Optional.ofNullable(images.get(instanceId));
    }

    @Override
    public List<EmulatedImage> list() {
        return new ArrayList<>(images.values());
    }

    @Override
    public String create(EmulatedImage image) {
        EmulatedCloudUtils.validateEmulatedResource(image);
        images.put(image.getInstanceId(), image);
        return image.getInstanceId();
    }

    @Override
    public void delete(String instanceId) {
        if (images.containsKey(instanceId)) {
            images.remove(instanceId);
        } else {
            throw new InvalidParameterException(EmulatedCloudConstants.Exception.RESOURCE_NOT_FOUND);
        }
    }

    private void loadDefaultImages(Properties properties) {
        String imageNamesList = properties.getProperty(EmulatedCloudConstants.Conf.IMAGE_NAMES_KEY);
        if (imageNamesList == null || imageNamesList.isEmpty()) {
            throw new FatalErrorException(EmulatedCloudConstants.Exception.NO_IMAGE_NAMES_SPECIFIED);
        }

        for (String name : imageNamesList.split(",")) {
            String instanceId = EmulatedCloudUtils.getRandomUUID();
            EmulatedImage image = new EmulatedImage.Builder()
                    .name(name)
                    .instanceId(instanceId)
                    .build();
            this.create(image);
        }
    }
}
