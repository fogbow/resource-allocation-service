package org.fogbowcloud.ras.core.plugins.aaa.identity;

import org.fogbowcloud.ras.core.exceptions.FatalErrorException;
import org.fogbowcloud.ras.core.exceptions.InvalidParameterException;
import org.fogbowcloud.ras.core.models.tokens.FederationUserToken;
import org.fogbowcloud.ras.util.RSAUtil;

import java.io.IOException;
import java.security.GeneralSecurityException;
import java.security.interfaces.RSAPrivateKey;

public class FederationIdentityPluginProtectionWrapper implements FederationIdentityPlugin<FederationUserToken> {
    private FederationIdentityPlugin embeddedPlugin;
    private RSAPrivateKey privateKey;

    public FederationIdentityPluginProtectionWrapper(FederationIdentityPlugin embeddedPlugin) {
        this.embeddedPlugin = embeddedPlugin;
        try {
            this.privateKey = RSAUtil.getPrivateKey();
        } catch (IOException | GeneralSecurityException e) {
            throw new FatalErrorException();
        }
    }

    @Override
    public FederationUserToken createToken(String protectedTokenValue) throws InvalidParameterException {
        String unprotectedToken = null;
        try {
            unprotectedToken = RSAUtil.decrypt(protectedTokenValue, this.privateKey);
        } catch (IOException | GeneralSecurityException e) {
            throw new InvalidParameterException();
        }
        return this.embeddedPlugin.createToken(unprotectedToken);
    }

    public FederationIdentityPlugin getEmbeddedPlugin() {
        return this.embeddedPlugin;
    }
}
