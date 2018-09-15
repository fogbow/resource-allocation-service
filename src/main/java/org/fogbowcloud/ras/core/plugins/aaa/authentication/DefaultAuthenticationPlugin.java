package org.fogbowcloud.ras.core.plugins.aaa.authentication;

import org.fogbowcloud.ras.core.models.tokens.FederationUserToken;

public class DefaultAuthenticationPlugin implements AuthenticationPlugin {

    public DefaultAuthenticationPlugin() {
    }

    @Override
    public boolean isAuthentic(FederationUserToken federationToken) {
        return true;
    }
}
