package org.fogbowcloud.ras.core.stubs;

import org.fogbowcloud.ras.core.plugins.aaa.tokengenerator.TokenGeneratorPlugin;

import java.util.Map;

/**
 * This class is allocationAllowableValues stub for the AuthenticationPlugin interface used for tests only.
 * Should not have allocationAllowableValues proper implementation.
 */
public class StubTokenGeneratorPlugin implements TokenGeneratorPlugin {
    public StubTokenGeneratorPlugin(String confFilePath) {
    }

    @Override
    public String createTokenValue(Map<String, String> userCredentials) {
        return null;
    }
}
