package org.fogbowcloud.ras.core.plugins.aaa.identity.openstack.v3;

import org.apache.log4j.Logger;
import org.fogbowcloud.ras.core.constants.Messages;
import org.fogbowcloud.ras.core.exceptions.InvalidParameterException;
import org.fogbowcloud.ras.core.models.tokens.OpenStackV3Token;
import org.fogbowcloud.ras.core.plugins.aaa.identity.FederationIdentityPlugin;
import org.fogbowcloud.ras.core.plugins.aaa.tokengenerator.openstack.v3.OpenStackTokenGeneratorPlugin;

public class OpenStackIdentityPlugin implements FederationIdentityPlugin<OpenStackV3Token> {
    private static final Logger LOGGER = Logger.getLogger(OpenStackIdentityPlugin.class);

    public OpenStackIdentityPlugin() {
    }

    @Override
    public OpenStackV3Token createToken(String tokenValue) throws InvalidParameterException {

        String split[] = tokenValue.split(OpenStackTokenGeneratorPlugin.OPENSTACK_TOKEN_STRING_SEPARATOR);
        if (split == null || split.length != OpenStackTokenGeneratorPlugin.OPENSTACK_TOKEN_NUMBER_OF_FIELDS) {
            LOGGER.error(String.format(Messages.Error.INVALID_TOKEN_VALUE, tokenValue));
            throw new InvalidParameterException();
        }

        String tokenProvider = split[0];
        String keystoneTokenValue = split[1];
        String userId = split[2];
        String userName = split[3];
        String projectId = split[4];
        String signature = split[5];        

        return new OpenStackV3Token(tokenProvider, keystoneTokenValue, userId, userName, projectId, signature);
    }
}
