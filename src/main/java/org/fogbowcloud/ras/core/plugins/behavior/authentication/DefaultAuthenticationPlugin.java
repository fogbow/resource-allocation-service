package org.fogbowcloud.ras.core.plugins.behavior.authentication;

import org.fogbowcloud.ras.core.models.tokens.FederationUserToken;

public class DefaultAuthenticationPlugin implements AuthenticationPlugin {

    public DefaultAuthenticationPlugin() {
    }

    @Override
    public boolean isAuthentic(FederationUserToken federationToken) {
        return true;
    }
}
