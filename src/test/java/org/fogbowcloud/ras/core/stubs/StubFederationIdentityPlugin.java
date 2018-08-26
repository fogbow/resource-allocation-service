package org.fogbowcloud.ras.core.stubs;

import org.fogbowcloud.ras.core.models.tokens.FederationUserToken;
import org.fogbowcloud.ras.core.plugins.aaa.identity.FederationIdentityPlugin;

/**
 * This class is a stub for the FederationIdentityPlugin interface used for tests only.
 * Should not have a proper implementation.
 */
public class StubFederationIdentityPlugin implements FederationIdentityPlugin<FederationUserToken> {

    public StubFederationIdentityPlugin() {
    }

    @Override
    public FederationUserToken createToken(String tokenValue) {
        return null;
    }
}
