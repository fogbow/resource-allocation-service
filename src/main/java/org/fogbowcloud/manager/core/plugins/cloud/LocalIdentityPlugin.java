package org.fogbowcloud.manager.core.plugins.cloud;

import java.util.Map;

import org.fogbowcloud.manager.core.exceptions.UnauthenticatedException;
import org.fogbowcloud.manager.core.plugins.exceptions.TokenCreationException;
import org.fogbowcloud.manager.core.plugins.exceptions.UnauthorizedException;
import org.fogbowcloud.manager.core.models.token.Token;

public interface LocalIdentityPlugin {

    /**
     * Creates a token based on the user's credentials.
     *
     * @param userCredentials
     * @return a Token with an access ID provided by the identity service.
     * @throws UnauthorizedException
     * @throws TokenCreationException
     */
    // TODO Change Unauthorized to Unauthenticated
    public Token createToken(Map<String, String> userCredentials)
            throws UnauthorizedException, TokenCreationException;

}
