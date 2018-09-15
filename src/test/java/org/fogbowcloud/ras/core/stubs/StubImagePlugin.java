package org.fogbowcloud.ras.core.stubs;

import org.fogbowcloud.ras.core.models.images.Image;
import org.fogbowcloud.ras.core.models.tokens.Token;
import org.fogbowcloud.ras.core.plugins.interoperability.ImagePlugin;

import java.util.Map;

/**
 * This class is a stub for the ImagePlugin interface used for tests only.
 * Should not have a proper implementation.
 */
public class StubImagePlugin implements ImagePlugin<Token> {

    public StubImagePlugin() {
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
