package cloud.fogbow.ras.core.stubs;

import cloud.fogbow.common.models.CloudUser;
import cloud.fogbow.ras.api.http.response.ImageInstance;
import cloud.fogbow.ras.core.plugins.interoperability.ImagePlugin;

import java.util.Map;

/**
 * This class is a stub for the ImagePlugin interface used for tests only.
 * Should not have a proper implementation.
 */
public class StubImagePlugin implements ImagePlugin<CloudUser> {

    public StubImagePlugin(String confFilePath) {
    }

    @Override
    public Map<String, String> getAllImages(CloudUser cloudUser) {
        return null;
    }

    @Override
    public ImageInstance getImage(String imageId, CloudUser cloudUser) {
        return null;
    }
}
