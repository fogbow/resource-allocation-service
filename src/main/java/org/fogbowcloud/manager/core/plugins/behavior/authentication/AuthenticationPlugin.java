package org.fogbowcloud.manager.core.plugins.behavior.authentication;

public interface AuthenticationPlugin {

    /**
     * Verifies if the federationTokenValue is valid against the identity service.
     *
     * @param federationTokenValue
     * @return a boolean stating whether the tokens value is valid or not.
     */
    public boolean isAuthentic(String federationTokenValue);

}
