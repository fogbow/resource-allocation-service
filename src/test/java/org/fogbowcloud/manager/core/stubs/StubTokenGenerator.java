package org.fogbowcloud.manager.core.stubs;

import org.fogbowcloud.manager.core.exceptions.FogbowManagerException;
import org.fogbowcloud.manager.core.exceptions.UnexpectedException;
import org.fogbowcloud.manager.core.models.tokens.Token;
import org.fogbowcloud.manager.core.models.tokens.TokenGenerator;

import java.util.Map;

/**
 * This class is a stub for the TokenGenerator interface used for tests only.
 * Should not have a proper implementation.
 */
public class StubTokenGenerator implements TokenGenerator<Token> {

    public StubTokenGenerator() {}

    @Override
    public String createTokenValue(Map<String, String> userCredentials) {
        return null;
    }

    @Override
    public Token createToken(String tokenValue) {
        return null;
    }

}
