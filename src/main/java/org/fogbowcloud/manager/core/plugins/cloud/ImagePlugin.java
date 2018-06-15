package org.fogbowcloud.manager.core.plugins.cloud;

import org.fogbowcloud.manager.core.exceptions.ImageException;
import org.fogbowcloud.manager.core.models.images.Image;
import org.fogbowcloud.manager.core.models.token.Token;

import java.util.Map;

public interface ImagePlugin {

    Map<String, String> getAllImages(Token localToken) throws ImageException;

    Image getImage(String imageId, Token localToken) throws ImageException;
}
