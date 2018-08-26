package org.fogbowcloud.ras.core.plugins.aaa.identity.openstack;

import org.apache.log4j.Logger;
import org.fogbowcloud.ras.core.exceptions.InvalidParameterException;
import org.fogbowcloud.ras.core.models.tokens.OpenStackV3Token;
import org.fogbowcloud.ras.core.plugins.aaa.tokengenerator.openstack.v3.KeystoneV3TokenGeneratorPlugin;
import org.fogbowcloud.ras.core.plugins.aaa.identity.FederationIdentityPlugin;

public class KeystoneV3IdentityPlugin implements FederationIdentityPlugin<OpenStackV3Token> {
    private static final Logger LOGGER = Logger.getLogger(KeystoneV3IdentityPlugin.class);

    public KeystoneV3IdentityPlugin() {
    }

    @Override
    public OpenStackV3Token createToken(String tokenValue) throws InvalidParameterException {

        String split[] = tokenValue.split(KeystoneV3TokenGeneratorPlugin.TOKEN_VALUE_SEPARATOR);
        if (split == null || split.length < 6) {
            LOGGER.error("Invalid token value: " + tokenValue);
            throw new InvalidParameterException();
        }

        String tokenProvider = split[0];
        String keystoneTokenValue = split[1];
        String userId = split[2];
        String userName = split[3];
        String projectId = split[4];
        String projectName = split[5];

        return new OpenStackV3Token(tokenProvider, keystoneTokenValue, userId, userName, projectId, projectName);
    }
}
