package org.fogbowcloud.manager.core.stubs;

import java.util.Map;
import org.fogbowcloud.manager.core.exceptions.FogbowManagerException;
import org.fogbowcloud.manager.core.exceptions.UnexpectedException;
import org.fogbowcloud.manager.core.models.tokens.LocalUserAttributes;
import org.fogbowcloud.manager.core.models.tokens.generators.LocalTokenGenerator;

/**
 * This class is a stub for the LocalTokenGenerator interface used for tests only.
 * Should not have a proper implementation.
 */
public class StubLocalTokenGenerator implements LocalTokenGenerator<LocalUserAttributes> {

    public StubLocalTokenGenerator() {}
    
    @Override
    public LocalUserAttributes createToken(Map<String, String> userCredentials)
            throws FogbowManagerException, UnexpectedException {
        return null;
    }

}
