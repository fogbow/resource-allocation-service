package org.fogbowcloud.ras.core.plugins.aaa.identity;

import org.fogbowcloud.ras.core.exceptions.FatalErrorException;
import org.fogbowcloud.ras.core.exceptions.InvalidParameterException;
import org.fogbowcloud.ras.core.models.tokens.FederationUserToken;
import org.fogbowcloud.ras.core.plugins.aaa.tokengenerator.TokenGeneratorPluginProtectionWrapper;
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
        unprotectedToken = decrypt(protectedTokenValue);
        return this.embeddedPlugin.createToken(unprotectedToken);
    }

    public FederationIdentityPlugin getEmbeddedPlugin() {
        return this.embeddedPlugin;
    }

    private String decrypt(String protectedTokenValue) throws InvalidParameterException {
        String unprotectedTokenValue;
        try {
            String split[] = protectedTokenValue.split(TokenGeneratorPluginProtectionWrapper.SEPARATOR);
            if (split.length != TokenGeneratorPluginProtectionWrapper.PROTECTION_WRAPPER_TOKEN_NUMBER_OF_FILEDS) {
                throw new InvalidParameterException();
            }
            String randomKey = RSAUtil.decrypt(split[0], this.privateKey);
            unprotectedTokenValue = RSAUtil.decryptAES(randomKey.getBytes("UTF-8"), split[1]);
        } catch (Exception e) {
            throw new InvalidParameterException();
        }
        return unprotectedTokenValue;
    }
}
