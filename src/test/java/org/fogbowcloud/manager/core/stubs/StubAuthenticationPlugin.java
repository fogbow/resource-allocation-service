package org.fogbowcloud.manager.core.stubs;

import org.fogbowcloud.manager.core.models.tokens.FederationUserToken;
import org.fogbowcloud.manager.core.plugins.behavior.authentication.AuthenticationPlugin;

/**
 * This class is a stub for the AuthenticationPlugin interface used for tests only.
 * Should not have a proper implementation.
 */
public class StubAuthenticationPlugin implements AuthenticationPlugin {

    public StubAuthenticationPlugin() {}

    @Override
    public boolean isAuthentic(FederationUserToken federationToken) {
        return false;
    }

}
