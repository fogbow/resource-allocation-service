package org.fogbowcloud.manager.core.plugins.behavior.identity.openstack;

import org.fogbowcloud.manager.core.exceptions.FogbowManagerException;
import org.fogbowcloud.manager.core.exceptions.UnauthenticatedUserException;
import org.fogbowcloud.manager.core.exceptions.UnexpectedException;
import org.fogbowcloud.manager.core.models.tokens.OpenStackV3Token;
import org.fogbowcloud.manager.core.models.tokens.generators.openstack.v3.KeystoneV3TokenGenerator;
import org.fogbowcloud.manager.core.plugins.behavior.identity.FederationIdentityPlugin;

import java.util.Map;

public class KeystoneV3IdentityPlugin implements FederationIdentityPlugin<OpenStackV3Token> {

    private KeystoneV3TokenGenerator keystoneV3TokenGenerator;

    public KeystoneV3IdentityPlugin() {
        this.keystoneV3TokenGenerator = new KeystoneV3TokenGenerator();
    }

    @Override
    public String createTokenValue(Map<String, String> userCredentials) throws UnexpectedException, FogbowManagerException {
        OpenStackV3Token token = (OpenStackV3Token) this.keystoneV3TokenGenerator.createToken(userCredentials);
        return token.getTokenValue();
    }

    @Override
    public OpenStackV3Token createToken(String tokenValue) throws UnauthenticatedUserException {
        return null;
    }
}
