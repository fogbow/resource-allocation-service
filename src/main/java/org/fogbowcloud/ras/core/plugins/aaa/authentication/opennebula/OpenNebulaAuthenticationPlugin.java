package org.fogbowcloud.ras.core.plugins.aaa.authentication.opennebula;

import org.apache.commons.lang.StringUtils;
import org.fogbowcloud.ras.core.models.tokens.FederationUserToken;
import org.fogbowcloud.ras.core.models.tokens.OpenNebulaToken;
import org.fogbowcloud.ras.core.plugins.aaa.authentication.RASAuthenticationPlugin;
import org.fogbowcloud.ras.core.plugins.aaa.tokengenerator.opennebula.OpenNebulaTokenGeneratorPlugin;

public class OpenNebulaAuthenticationPlugin extends RASAuthenticationPlugin {

    @Override
    protected String getTokenMessage(FederationUserToken federationUserToken) {
        OpenNebulaToken oneToken = (OpenNebulaToken) federationUserToken;

        String[] parameters = new String[]{
            oneToken.getTokenProvider(),
            oneToken.getTokenValue(),
            oneToken.getUserName(),
            oneToken.getSignature()
        };
        return StringUtils.join(parameters, OpenNebulaTokenGeneratorPlugin.OPENNEBULA_FIELD_SEPARATOR);
    }

    @Override
    protected String getSignature(FederationUserToken federationUserToken) {
        OpenNebulaToken oneToken = (OpenNebulaToken) federationUserToken;
        return oneToken.getSignature();
    }
}
