package cloud.fogbow.ras.core.stubs;

import cloud.fogbow.common.models.CloudToken;
import cloud.fogbow.ras.api.http.response.Image;
import cloud.fogbow.ras.core.plugins.interoperability.ImagePlugin;

import java.util.Map;

/**
 * This class is a stub for the ImagePlugin interface used for tests only.
 * Should not have a proper implementation.
 */
public class StubImagePlugin implements ImagePlugin<CloudToken> {

    public StubImagePlugin(String confFilePath) {
    }

    @Override
    public Map<String, String> getAllImages(CloudToken token) {
        return null;
    }

    @Override
    public Image getImage(String imageId, CloudToken token) {
        return null;
    }
}
