package org.fogbowcloud.manager.core.plugins.behavior.identity;

import org.fogbowcloud.manager.core.exceptions.InvalidParameterException;
import org.fogbowcloud.manager.core.models.tokens.FederationUserToken;

public interface FederationIdentityPlugin<T extends FederationUserToken> {

    /**
     * Creates a token object from a token string.
     *
     * @param tokenValue a token string issued by an object of a class that implememts the TokenGenerator interface.
     * @return a Token with the actual tokenValue string used to validate the token (if necessary) and the relevant
     * attributes extracted from the tokenValue string.
     */
    public T createToken(String tokenValue) throws InvalidParameterException;
}
