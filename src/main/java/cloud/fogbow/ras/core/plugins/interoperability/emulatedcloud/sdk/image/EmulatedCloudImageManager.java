package cloud.fogbow.ras.core.plugins.interoperability.emulatedcloud.sdk.image;

import cloud.fogbow.ras.core.plugins.interoperability.emulatedcloud.EmulatedCloudConstants;
import cloud.fogbow.ras.core.plugins.interoperability.emulatedcloud.EmulatedCloudUtils;
import cloud.fogbow.ras.core.plugins.interoperability.emulatedcloud.sdk.ResourceManager;
import cloud.fogbow.ras.core.plugins.interoperability.emulatedcloud.sdk.image.models.EmulatedImage;

import java.security.InvalidParameterException;
import java.util.*;

public class EmulatedCloudImageManager implements ResourceManager<EmulatedImage> {
    private final List<String> IMAGE_NAMES = Arrays.asList("Ubuntu 20.04", "Ubuntu 18.04", "Ubuntu 16.04");

    private static EmulatedCloudImageManager instance;
    private Map<String, EmulatedImage> images;

    private EmulatedCloudImageManager() {
        this.images = new HashMap<>();
        this.loadDefaultImages();
    }

    public static EmulatedCloudImageManager getInstance() {
        if (instance == null) {
            instance = new EmulatedCloudImageManager();
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

    private void loadDefaultImages() {
        for (String name : IMAGE_NAMES) {
            String instanceId = EmulatedCloudUtils.getRandomUUID();
            EmulatedImage image = new EmulatedImage.Builder()
                    .name(name)
                    .instanceId(instanceId)
                    .build();
            this.create(image);
        }
    }
}
