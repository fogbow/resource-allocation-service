package org.fogbowcloud.manager.core.models.tokens;

import org.fogbowcloud.manager.core.exceptions.FogbowManagerException;
import org.fogbowcloud.manager.core.exceptions.UnauthenticatedUserException;
import org.fogbowcloud.manager.core.exceptions.UnexpectedException;

import java.util.Map;

public interface TokenGenerator<T extends Token> {

    /**
     * Creates a token string using the user's credentials.
     *
     * @param userCredentials a map containing the credentials to get a token from the identity provider
     *                        service.
     * @return a token string issued by the identity provider service or an auxiliary wrapper.
     *
     */
    public String createTokenValue(Map<String, String> userCredentials)
            throws UnexpectedException, FogbowManagerException;

    /**
     * Creates a token object from a token string.
     *
     * @param tokenValue a token string issued by an identity provider service or an auxiliary wrapper.
     * @return a Token with the tokenValue string and the relevant attributes extracted from the
     * tokenValue string.
     */
    public T createToken(String tokenValue) throws UnauthenticatedUserException;
}
