package org.fogbowcloud.manager.core.stubs;

import java.util.Map;
import org.fogbowcloud.manager.core.exceptions.FogbowManagerException;
import org.fogbowcloud.manager.core.exceptions.UnexpectedException;
import org.fogbowcloud.manager.core.models.tokens.Token;
import org.fogbowcloud.manager.core.plugins.cloud.LocalIdentityPlugin;

/**
 * This class is a stub for the LocalIdentityPlugin interface used for tests only.
 * Should not have a proper implementation.
 */
public class StubLocalIdentityPlugin implements LocalIdentityPlugin {

    public StubLocalIdentityPlugin() {}
    
    @Override
    public Token createToken(Map<String, String> userCredentials)
            throws FogbowManagerException, UnexpectedException {
        return null;
    }

}
