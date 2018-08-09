package org.fogbowcloud.manager.core.plugins.behavior.authentication;

import org.fogbowcloud.manager.core.exceptions.InvalidParameterException;
import org.fogbowcloud.manager.core.models.tokens.FederationUserToken;

public class DefaultAuthenticationPlugin implements AuthenticationPlugin {
    
    public DefaultAuthenticationPlugin() {}

    @Override
    public FederationUserToken getFederationUser(String federationTokenValue) throws InvalidParameterException {
        FederationUserToken federationUserToken = new FederationUserToken(federationTokenValue, "default-id", "default");

        return federationUserToken;
    }

    @Override
    public boolean isAuthentic(String federationTokenValue) {
        return true;
    }
}
