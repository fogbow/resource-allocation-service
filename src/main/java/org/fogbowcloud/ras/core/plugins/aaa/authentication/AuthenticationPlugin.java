package org.fogbowcloud.ras.core.plugins.aaa.authentication;

import org.fogbowcloud.ras.core.exceptions.UnavailableProviderException;
import org.fogbowcloud.ras.core.models.tokens.FederationUserToken;

public interface AuthenticationPlugin<T extends FederationUserToken> {
    /**
     * Verifies if the federationTokenValue is valid against the identity service.
     *
     * @param requestingMember the member from where the request was issued
     * @param federationUserToken the token describing the user to be authenticated
     * @return a boolean stating whether the tokens value is valid or not.
     */
    public boolean isAuthentic(String requestingMember, T federationUserToken) throws UnavailableProviderException;
}
