package org.fogbowcloud.ras.core.plugins.aaa.tokengenerator;

import java.util.Map;

public class DefaultTokenGeneratorPlugin implements TokenGeneratorPlugin {

    @Override
    public String createTokenValue(Map<String, String> userCredentials) {
        return "default-token";
    }
}
