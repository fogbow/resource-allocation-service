package org.fogbowcloud.manager.core.plugins.behavior.authentication;

import org.fogbowcloud.manager.core.exceptions.InvalidParameterException;
import org.fogbowcloud.manager.core.models.tokens.FederationUserAttributes;

public class DefaultAuthenticationPlugin implements AuthenticationPlugin {
    
    public DefaultAuthenticationPlugin() {}

    @Override
    public FederationUserAttributes getFederationUser(String federationTokenValue) throws InvalidParameterException {
        FederationUserAttributes federationUserAttributes = new FederationUserAttributes("default-id", "default");

        return federationUserAttributes;
    }

    @Override
    public boolean isValid(String federationTokenValue) {
        return true;
    }
}
