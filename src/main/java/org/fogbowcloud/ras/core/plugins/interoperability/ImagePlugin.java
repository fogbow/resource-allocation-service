package org.fogbowcloud.ras.core.plugins.interoperability;

import org.fogbowcloud.ras.core.exceptions.FogbowRasException;
import org.fogbowcloud.ras.core.exceptions.UnexpectedException;
import org.fogbowcloud.ras.core.models.images.Image;
import org.fogbowcloud.ras.core.models.tokens.Token;

import java.util.Map;

public interface ImagePlugin<T extends Token> {

    Map<String, String> getAllImages(T localUserAttributes) throws FogbowRasException, UnexpectedException;

    Image getImage(String imageId, T localUserAttributes) throws FogbowRasException, UnexpectedException;
}
