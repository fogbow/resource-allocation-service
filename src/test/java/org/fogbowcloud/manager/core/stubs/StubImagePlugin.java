package org.fogbowcloud.manager.core.stubs;

import java.util.Map;
import org.fogbowcloud.manager.core.exceptions.FogbowManagerException;
import org.fogbowcloud.manager.core.exceptions.UnexpectedException;
import org.fogbowcloud.manager.core.models.images.Image;
import org.fogbowcloud.manager.core.models.tokens.LocalUserAttributes;
import org.fogbowcloud.manager.core.plugins.cloud.ImagePlugin;

/**
 * This class is a stub for the ImagePlugin interface used for tests only.
 * Should not have a proper implementation.
 */
public class StubImagePlugin implements ImagePlugin<LocalUserAttributes> {

    public StubImagePlugin() {}
    
    @Override
    public Map<String, String> getAllImages(LocalUserAttributes localUserAttributes)
            throws FogbowManagerException, UnexpectedException {
        return null;
    }

    @Override
    public Image getImage(String imageId, LocalUserAttributes localUserAttributes)
            throws FogbowManagerException, UnexpectedException {
        return null;
    }

}
