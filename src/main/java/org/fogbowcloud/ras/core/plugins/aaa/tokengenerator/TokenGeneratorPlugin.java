package org.fogbowcloud.ras.core.plugins.aaa.tokengenerator;

import org.fogbowcloud.ras.core.exceptions.FogbowRasException;
import org.fogbowcloud.ras.core.exceptions.UnexpectedException;

import java.util.Map;

public interface TokenGeneratorPlugin {
    /**
     * Creates a token string using the user's credentials.
     *
     * @param userCredentials a map containing the credentials to get a token from the identity provider service.
     * @return a string with the tokenValue string and the relevant attributes of the corresponding user.
     */
    public String createTokenValue(Map<String, String> userCredentials) throws UnexpectedException,
            FogbowRasException;
}
