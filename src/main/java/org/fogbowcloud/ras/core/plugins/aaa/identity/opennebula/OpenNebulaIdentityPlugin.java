package org.fogbowcloud.ras.core.plugins.aaa.identity.opennebula;

import org.apache.log4j.Logger;
import org.fogbowcloud.ras.core.constants.Messages;
import org.fogbowcloud.ras.core.exceptions.InvalidParameterException;
import org.fogbowcloud.ras.core.models.tokens.OpenNebulaToken;
import org.fogbowcloud.ras.core.plugins.aaa.identity.FederationIdentityPlugin;

public class OpenNebulaIdentityPlugin implements FederationIdentityPlugin<OpenNebulaToken> {
    private static final Logger LOGGER = Logger.getLogger(OpenNebulaIdentityPlugin.class);

    private static final String OPENNEBULA_FIELD_SEPARATOR = "#&#";

    public OpenNebulaIdentityPlugin() {
    }

    @Override
    public OpenNebulaToken createToken(String tokenValue) throws InvalidParameterException {
        if(tokenValue == null || tokenValue.trim().equals("")){
            throw new InvalidParameterException(String.format(Messages.Error.INVALID_TOKEN_VALUE, Messages.Error.EMPTY_OR_NULL_TOKEN));
        }

        String split[] = tokenValue.split(OPENNEBULA_FIELD_SEPARATOR);

        String provider = split[0];
        String oneTokenValue = split[1];
        String userName = split[2];
        String signature = split[3];

        return new OpenNebulaToken(provider, oneTokenValue, userName, signature);
    }
}
