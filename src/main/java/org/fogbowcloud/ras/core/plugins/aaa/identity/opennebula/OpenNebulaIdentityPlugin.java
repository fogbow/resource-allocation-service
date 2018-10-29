package org.fogbowcloud.ras.core.plugins.aaa.identity.opennebula;

import org.fogbowcloud.ras.core.constants.Messages;
import org.fogbowcloud.ras.core.exceptions.InvalidParameterException;
import org.fogbowcloud.ras.core.models.tokens.OpenNebulaToken;
import org.fogbowcloud.ras.core.plugins.aaa.identity.FederationIdentityPlugin;
import org.fogbowcloud.ras.core.plugins.interoperability.opennebula.OpenNebulaClientFactory;

public class OpenNebulaIdentityPlugin implements FederationIdentityPlugin<OpenNebulaToken> {
    
    private static final String OPENNEBULA_FIELD_SEPARATOR = "#&#";
    private static final String EMPTY_SPACE = "";

    public OpenNebulaIdentityPlugin() {
    }

    @Override
    public OpenNebulaToken createToken(String tokenValue) throws InvalidParameterException {
        if(tokenValue == null || tokenValue.trim().equals(EMPTY_SPACE)){
            throw new InvalidParameterException(String.format(Messages.Error.INVALID_TOKEN_VALUE, Messages.Error.EMPTY_OR_NULL_TOKEN));
        }

        String split[] = tokenValue.split(OPENNEBULA_FIELD_SEPARATOR);

        String provider = split[0];
        String oneTokenValue = split[1];
        String userId = split[2];
        String userName = split[3];
        String signature = split[4];

        try {
            OpenNebulaClientFactory factory = new OpenNebulaClientFactory();

            // Test if oneTokenValue is valid for any authentication.
            factory.createClient(oneTokenValue);
        } catch (Exception e){
            throw new InvalidParameterException(e.getMessage());
        }

        return new OpenNebulaToken(provider, oneTokenValue, userId, userName, signature);
    }
}
