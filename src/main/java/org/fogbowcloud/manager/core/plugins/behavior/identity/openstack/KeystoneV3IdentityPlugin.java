package org.fogbowcloud.manager.core.plugins.behavior.identity.openstack;

import org.apache.log4j.Logger;
import org.fogbowcloud.manager.core.exceptions.FogbowManagerException;
import org.fogbowcloud.manager.core.exceptions.UnauthenticatedUserException;
import org.fogbowcloud.manager.core.exceptions.UnexpectedException;
import org.fogbowcloud.manager.core.models.tokens.OpenStackV3Token;
import org.fogbowcloud.manager.core.models.tokens.generators.openstack.v3.KeystoneV3TokenGenerator;
import org.fogbowcloud.manager.core.plugins.behavior.identity.FederationIdentityPlugin;

import java.util.Map;

public class KeystoneV3IdentityPlugin implements FederationIdentityPlugin<OpenStackV3Token> {
    private static final Logger LOGGER = Logger.getLogger(KeystoneV3IdentityPlugin.class);

    private KeystoneV3TokenGenerator keystoneV3TokenGenerator;
    private static final String TOKEN_VALUE_SEPARATOR = "!#!";

    public KeystoneV3IdentityPlugin() {
        this.keystoneV3TokenGenerator = new KeystoneV3TokenGenerator();
    }

    @Override
    public String createTokenValue(Map<String, String> userCredentials)
            throws UnexpectedException, FogbowManagerException {
        OpenStackV3Token token = (OpenStackV3Token) this.keystoneV3TokenGenerator.createToken(userCredentials);
        String tokenValue = token.getTokenValue() + TOKEN_VALUE_SEPARATOR + token.getUserId() + TOKEN_VALUE_SEPARATOR +
                token.getUserName() + TOKEN_VALUE_SEPARATOR + token.getProjectId() + TOKEN_VALUE_SEPARATOR +
                token.getProjectName();
        return tokenValue;
    }

    @Override
    public OpenStackV3Token createToken(String tokenValue) throws UnauthenticatedUserException {

        String split[] = tokenValue.split(TOKEN_VALUE_SEPARATOR);
        if (split == null || split.length < 5) {
            LOGGER.error("Invalid token value: " + tokenValue);
            throw new UnauthenticatedUserException();
        }

        String keystoneTokenValue = split[0];
        String userId = split[1];
        String userName = split[2];
        String projectId = split[3];
        String projectName = split[4];

        return new OpenStackV3Token(keystoneTokenValue, userId, userName, projectId, projectName);
    }
}
