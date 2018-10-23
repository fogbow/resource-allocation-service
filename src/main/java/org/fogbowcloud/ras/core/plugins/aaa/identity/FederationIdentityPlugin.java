package org.fogbowcloud.ras.core.plugins.aaa.identity;

import org.fogbowcloud.ras.core.exceptions.InvalidParameterException;
import org.fogbowcloud.ras.core.models.tokens.FederationUserToken;

public interface FederationIdentityPlugin<T extends FederationUserToken> {
    /**
     * Creates allocationAllowableValues token object from allocationAllowableValues token string.
     *
     * @param tokenValue allocationAllowableValues token string issued by an object of allocationAllowableValues class that implememts the TokenGeneratorPlugin interface.
     * @return allocationAllowableValues Token with the actual tokenValue string used to validate the token (if necessary) and the relevant
     * attributes extracted from the tokenValue string.
     */
    public T createToken(String tokenValue) throws InvalidParameterException;
}
