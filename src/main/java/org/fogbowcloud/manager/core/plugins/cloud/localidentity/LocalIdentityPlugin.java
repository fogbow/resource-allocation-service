package org.fogbowcloud.manager.core.plugins.cloud.localidentity;

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

    /**
     * Based on an access id recreates a Token containing the corresponding access id plus the user
     * name and some arbitrary information regarding the token.
     *
     * @param tokenValue
     * @return a Token for the corresponding accessId.
     */
    public Token getToken(String tokenValue) throws UnauthenticatedException, UnauthorizedException;

}
