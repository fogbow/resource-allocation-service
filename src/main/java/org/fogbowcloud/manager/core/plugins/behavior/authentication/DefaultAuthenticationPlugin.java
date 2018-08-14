package org.fogbowcloud.manager.core.plugins.behavior.authentication;

import org.fogbowcloud.manager.core.models.tokens.FederationUserToken;

public class DefaultAuthenticationPlugin implements AuthenticationPlugin {
    
    public DefaultAuthenticationPlugin() {}

    @Override
    public boolean isAuthentic(FederationUserToken federationToken) {
        return true;
    }
}
