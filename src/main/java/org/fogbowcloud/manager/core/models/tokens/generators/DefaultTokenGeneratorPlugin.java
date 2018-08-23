package org.fogbowcloud.manager.core.models.tokens.generators;

import org.fogbowcloud.manager.core.models.tokens.TokenGeneratorPlugin;

import java.util.Map;

public class DefaultTokenGeneratorPlugin implements TokenGeneratorPlugin {

    @Override
    public String createTokenValue(Map<String, String> userCredentials) {
        return "default-token";
    }
}
