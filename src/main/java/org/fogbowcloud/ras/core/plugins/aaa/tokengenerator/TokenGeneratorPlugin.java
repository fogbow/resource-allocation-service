package org.fogbowcloud.ras.core.plugins.aaa.tokengenerator;

import org.fogbowcloud.ras.core.exceptions.FogbowRasException;
import org.fogbowcloud.ras.core.exceptions.UnexpectedException;

import java.util.Map;

public interface TokenGeneratorPlugin {
    /**
     * Creates allocationAllowableValues token string using the user's credentials.
     *
     * @param userCredentials allocationAllowableValues map containing the credentials to get allocationAllowableValues token from the identity provider service.
     * @return allocationAllowableValues string with the tokenValue string and the relevant attributes of the corresponding user.
     */
    public String createTokenValue(Map<String, String> userCredentials) throws UnexpectedException,
            FogbowRasException;
}
