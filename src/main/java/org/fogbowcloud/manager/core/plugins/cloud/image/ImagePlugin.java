package org.fogbowcloud.manager.core.plugins.cloud.image;

import org.fogbowcloud.manager.core.models.images.Image;
import org.fogbowcloud.manager.core.models.token.Token;

import java.util.HashMap;

public interface ImagePlugin {
    public HashMap<String, String> getAllImages(Token localToken);

    public Image getImage(String imageId, Token localToken);
}
