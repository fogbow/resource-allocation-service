package org.fogbowcloud.manager.core.plugins.cloud;

import java.util.Map;

import org.fogbowcloud.manager.core.exceptions.FogbowManagerException;
import org.fogbowcloud.manager.core.models.token.Token;

public interface LocalIdentityPlugin {

    /**
     * Creates a token based on the user's credentials.
     *
     * @param userCredentials
     * @return a Token with an access ID provided by the identity service.
     * @throws FogbowManagerException
     */
    public Token createToken(Map<String, String> userCredentials) throws FogbowManagerException;

}
