package org.fogbowcloud.ras.core.plugins.aaa.authentication;

import org.fogbowcloud.ras.core.models.tokens.FederationUserToken;

public class DefaultAuthenticationPlugin implements AuthenticationPlugin<FederationUserToken> {

    public DefaultAuthenticationPlugin() {
    }

    @Override
    public boolean isAuthentic(String requestingMember, FederationUserToken federationToken) {
        return true;
    }
}
