package org.fogbowcloud.ras.core.stubs;

import org.fogbowcloud.ras.core.plugins.aaa.tokengenerator.TokenGeneratorPlugin;

import java.util.Map;

/**
 * This class is a stub for the AuthenticationPlugin interface used for tests only.
 * Should not have a proper implementation.
 */
public class StubTokenGeneratorPlugin implements TokenGeneratorPlugin {
    public StubTokenGeneratorPlugin() {
    }

    @Override
    public String createTokenValue(Map<String, String> userCredentials) {
        return null;
    }
}
