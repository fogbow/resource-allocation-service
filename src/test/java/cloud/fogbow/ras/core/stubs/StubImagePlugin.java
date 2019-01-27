package cloud.fogbow.ras.core.stubs;

import cloud.fogbow.ras.core.models.images.Image;
import org.fogbowcloud.ras.core.models.tokens.Token;
import cloud.fogbow.ras.core.plugins.interoperability.ImagePlugin;

import java.util.Map;

/**
 * This class is a stub for the ImagePlugin interface used for tests only.
 * Should not have a proper implementation.
 */
public class StubImagePlugin implements ImagePlugin<Token> {

    public StubImagePlugin(String confFilePath) {
    }

    @Override
    public Map<String, String> getAllImages(Token token) {
        return null;
    }

    @Override
    public Image getImage(String imageId, Token token) {
        return null;
    }
}
