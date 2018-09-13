package org.fogbowcloud.ras.core.plugins.aaa.identity;

import org.fogbowcloud.ras.core.exceptions.FogbowRasException;
import org.fogbowcloud.ras.core.exceptions.UnexpectedException;
import org.fogbowcloud.ras.core.models.tokens.FederationUserToken;
import org.fogbowcloud.ras.core.plugins.aaa.tokengenerator.DefaultTokenGeneratorPlugin;
import org.fogbowcloud.ras.core.plugins.aaa.tokengenerator.TokenGeneratorPluginProtectionWrapper;
import org.fogbowcloud.ras.util.RSAUtil;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import java.io.IOException;
import java.security.GeneralSecurityException;
import java.security.interfaces.RSAPrivateKey;
import java.security.interfaces.RSAPublicKey;
import java.util.HashMap;

public class FederationIdentityPluginProtectionWrapperTest {
    private FederationIdentityPluginProtectionWrapper federationIdentityPluginProtectionWrapper;
    private RSAPrivateKey privateKey;

    @Before
    public void setUp() throws IOException, GeneralSecurityException {
        this.privateKey = RSAUtil.getPrivateKey();
        FederationIdentityPlugin embeddedPlugin = new DefaultFederationIdentityPlugin();
        this.federationIdentityPluginProtectionWrapper =
                new FederationIdentityPluginProtectionWrapper(embeddedPlugin);
    }

    //test case: createToken with an encrypted tokenValue should generate a Token with the appropriate values
    @Test
    public void testCreateToken() throws UnexpectedException, FogbowRasException, IOException,
            GeneralSecurityException {
        //set up
        TokenGeneratorPluginProtectionWrapper tokenGeneratorPluginProtectionWrapper =
                new TokenGeneratorPluginProtectionWrapper(new DefaultTokenGeneratorPlugin());
        String protectedTokenValue =
                tokenGeneratorPluginProtectionWrapper.createTokenValue(new HashMap<String, String>());
        String unprotectedToken = RSAUtil.decrypt(protectedTokenValue, this.privateKey);

        //exercise
        FederationUserToken token =
                this.federationIdentityPluginProtectionWrapper.createToken(protectedTokenValue);
        FederationUserToken otherToken =
                this.federationIdentityPluginProtectionWrapper.getEmbeddedPlugin().createToken(unprotectedToken);

        //verify
        Assert.assertEquals(token, otherToken);
        Assert.assertEquals(token.getUserId(), otherToken.getUserId());
        Assert.assertEquals(token.getTokenProvider(), otherToken.getTokenProvider());
        Assert.assertEquals(token.getUserName(), otherToken.getUserName());
    }
}
