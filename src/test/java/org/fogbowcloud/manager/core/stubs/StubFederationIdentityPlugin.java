package org.fogbowcloud.manager.core.stubs;

import org.fogbowcloud.manager.core.models.tokens.FederationUserToken;
import org.fogbowcloud.manager.core.models.tokens.Token;
import org.fogbowcloud.manager.core.plugins.behavior.identity.FederationIdentityPlugin;

import java.util.Map;

/**
 * This class is a stub for the FederationIdentityPlugin interface used for tests only.
 * Should not have a proper implementation.
 */
public class StubFederationIdentityPlugin implements FederationIdentityPlugin<FederationUserToken> {

    public StubFederationIdentityPlugin() {}

    @Override
    public String createTokenValue(Map<String, String> userCredentials) {
        return null;
    }

    @Override
    public FederationUserToken createToken(String tokenValue) {
        return null;
    }

}
