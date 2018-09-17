package org.fogbowcloud.ras.core.plugins.aaa.authentication;

import org.fogbowcloud.ras.core.exceptions.UnavailableProviderException;

public interface AuthenticationPlugin<T> {
    /**
     * Verifies if the federationTokenValue is valid against the identity service.
     *
     * @param federationToken
     * @return a boolean stating whether the tokens value is valid or not.
     */
    public boolean isAuthentic(T federationToken) throws UnavailableProviderException;
}
