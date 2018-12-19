package org.fogbowcloud.ras.core.plugins.aaa.identity.opennebula;

import org.fogbowcloud.ras.core.constants.Messages;
import org.fogbowcloud.ras.core.exceptions.InvalidParameterException;
import org.fogbowcloud.ras.core.models.tokens.OpenNebulaToken;
import org.fogbowcloud.ras.core.plugins.aaa.identity.FederationIdentityPlugin;
import org.fogbowcloud.ras.core.plugins.aaa.tokengenerator.opennebula.OpenNebulaTokenGeneratorPlugin;

public class OpenNebulaIdentityPlugin implements FederationIdentityPlugin<OpenNebulaToken> {

    @Override
    public OpenNebulaToken createToken(String tokenValue) throws InvalidParameterException {
        if(tokenValue == null || tokenValue.isEmpty()){
            throw new InvalidParameterException(String.format(Messages.Error.INVALID_TOKEN_VALUE, Messages.Error.EMPTY_OR_NULL_TOKEN));
        }

        String split[] = tokenValue.split(OpenNebulaTokenGeneratorPlugin.OPENNEBULA_FIELD_SEPARATOR);
        if (split.length != OpenNebulaTokenGeneratorPlugin.FEDERATION_TOKEN_PARAMETER_SIZE) {
            throw new InvalidParameterException(String.format(Messages.Error.INVALID_TOKEN_VALUE, Messages.Error.INVALID_FORMAT_TOKEN));
        }
        String provider = split[OpenNebulaTokenGeneratorPlugin.PROVIDER_ID_TOKEN_VALUE_PARAMETER];
        String oneTokenValue = split[OpenNebulaTokenGeneratorPlugin.ONE_TOKEN_VALUE_PARAMETER];
        String userId = split[OpenNebulaTokenGeneratorPlugin.USER_ID_TOKEN_VALUE_PARAMETER];
        String userName = split[OpenNebulaTokenGeneratorPlugin.USERNAME_TOKEN_VALUE_PARAMETER];
        String signature = split[OpenNebulaTokenGeneratorPlugin.SIGNATURE_TOKEN_VALUE_PARAMETER];

        return new OpenNebulaToken(provider, oneTokenValue, userId, userName, signature);
    }
}
