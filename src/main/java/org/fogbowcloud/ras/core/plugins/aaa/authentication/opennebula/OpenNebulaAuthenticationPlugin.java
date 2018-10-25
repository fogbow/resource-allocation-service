package org.fogbowcloud.ras.core.plugins.aaa.authentication.opennebula;

import org.apache.commons.lang.StringUtils;
import org.fogbowcloud.ras.core.exceptions.UnexpectedException;
import org.fogbowcloud.ras.core.models.tokens.FederationUserToken;
import org.fogbowcloud.ras.core.models.tokens.OpenNebulaToken;
import org.fogbowcloud.ras.core.plugins.aaa.authentication.RASAuthenticationPlugin;
import org.fogbowcloud.ras.core.plugins.aaa.tokengenerator.cloudstack.CloudStackTokenGeneratorPlugin;
import org.fogbowcloud.ras.core.plugins.interoperability.opennebula.OpenNebulaClientFactory;
import org.opennebula.client.Client;

import java.io.IOException;
import java.security.GeneralSecurityException;
import java.security.interfaces.RSAPublicKey;

public class OpenNebulaAuthenticationPlugin extends RASAuthenticationPlugin {
    @Override
    public boolean isAuthentic(String requestingMember, FederationUserToken federationUserToken) {
        return super.isAuthentic(requestingMember, federationUserToken);
    }

    @Override
    protected String getTokenMessage(FederationUserToken federationUserToken) {
        OpenNebulaToken oneToken = (OpenNebulaToken) federationUserToken;

        OpenNebulaClientFactory factory = new OpenNebulaClientFactory();

        // One more time, test if oneTokenValue is valid for any authentication.
        try {
            Client client = factory.createClient(oneToken.getTokenValue());
        } catch (UnexpectedException e) {
            System.out.println(e.getMessage());
        }


        String[] parameters = new String[]{
            oneToken.getTokenProvider(),
            oneToken.getTokenValue(),
            oneToken.getUserName(),
            oneToken.getSignature()
        };
        return StringUtils.join(parameters, CloudStackTokenGeneratorPlugin.CLOUDSTACK_TOKEN_STRING_SEPARATOR);
    }

    @Override
    protected String getSignature(FederationUserToken federationUserToken) {
        OpenNebulaToken oneToken = (OpenNebulaToken) federationUserToken;
        return oneToken.getSignature();
    }

    @Override
    protected RSAPublicKey getPublicKey() throws IOException, GeneralSecurityException {
        return super.getPublicKey();
    }
}
