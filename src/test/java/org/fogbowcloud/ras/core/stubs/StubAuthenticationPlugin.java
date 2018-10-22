package org.fogbowcloud.ras.core.stubs;

import org.fogbowcloud.ras.core.models.tokens.FederationUserToken;
import org.fogbowcloud.ras.core.plugins.aaa.authentication.AuthenticationPlugin;

/**
 * This class is a stub for the AuthenticationPlugin interface used for tests only.
 * Should not have a proper implementation.
 */
public class StubAuthenticationPlugin implements AuthenticationPlugin<FederationUserToken> {

    public StubAuthenticationPlugin() {
    }

    @Override
    public boolean isAuthentic(String requestingMember, FederationUserToken federationToken) {
        return false;
    }

}
