package org.fogbowcloud.ras.core.plugins.aaa.identity.cloudstack;

import org.apache.log4j.Logger;
import org.fogbowcloud.ras.core.constants.Messages;
import org.fogbowcloud.ras.core.exceptions.InvalidParameterException;
import org.fogbowcloud.ras.core.models.tokens.CloudStackToken;
import org.fogbowcloud.ras.core.plugins.aaa.identity.FederationIdentityPlugin;
import org.fogbowcloud.ras.core.plugins.aaa.tokengenerator.cloudstack.CloudStackTokenGeneratorPlugin;

public class CloudStackIdentityPlugin implements FederationIdentityPlugin<CloudStackToken> {
    private static final Logger LOGGER = Logger.getLogger(CloudStackIdentityPlugin.class);

    public CloudStackIdentityPlugin() {
    }

    @Override
    public CloudStackToken createToken(String tokenValue) throws InvalidParameterException {
        String split[] = tokenValue.split(CloudStackTokenGeneratorPlugin.CLOUDSTACK_TOKEN_STRING_SEPARATOR);
        if (split == null || split.length != CloudStackTokenGeneratorPlugin.CLOUDSTACK_TOKEN_NUMBER_OF_FIELDS) {
            LOGGER.error(String.format(Messages.Error.INVALID_TOKEN_VALUE, tokenValue));
            throw new InvalidParameterException();
        }

        String tokenProvider = split[0];
        String cloudStackTokenValue = split[1];
        String userId = split[2];
        String userName = split[3];
        String signature = split[4];

        return new CloudStackToken(tokenProvider, cloudStackTokenValue, userId, userName, signature);
    }
}
