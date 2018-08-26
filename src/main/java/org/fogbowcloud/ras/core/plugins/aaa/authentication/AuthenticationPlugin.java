package org.fogbowcloud.ras.core.plugins.aaa.authentication;

import org.fogbowcloud.ras.core.exceptions.UnavailableProviderException;
import org.fogbowcloud.ras.core.models.tokens.FederationUserToken;

public interface AuthenticationPlugin {
    /**
     * Verifies if the federationTokenValue is valid against the identity service.
     *
     * @param federationToken
     * @return a boolean stating whether the tokens value is valid or not.
     */
    public boolean isAuthentic(FederationUserToken federationToken) throws UnavailableProviderException;
}
