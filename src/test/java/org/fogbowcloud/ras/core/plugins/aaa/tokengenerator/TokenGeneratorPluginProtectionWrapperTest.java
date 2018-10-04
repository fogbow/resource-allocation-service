package org.fogbowcloud.ras.core.plugins.aaa.tokengenerator;

import org.fogbowcloud.ras.core.exceptions.FogbowRasException;
import org.fogbowcloud.ras.core.exceptions.InvalidParameterException;
import org.fogbowcloud.ras.core.exceptions.UnexpectedException;
import org.fogbowcloud.ras.util.RSAUtil;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;

import java.io.IOException;
import java.security.GeneralSecurityException;
import java.security.interfaces.RSAPrivateKey;
import java.util.HashMap;

public class TokenGeneratorPluginProtectionWrapperTest {
    private TokenGeneratorPluginProtectionWrapper tokenGeneratorPluginProtectionWrapper;
    private String UNPROTECTED_STRING = "unprotected string";
    private RSAPrivateKey privateKey;

    @Before
    public void setUp() throws UnexpectedException, FogbowRasException, IOException, GeneralSecurityException {
        this.privateKey = RSAUtil.getPrivateKey();
        TokenGeneratorPlugin embeddedPlugin = Mockito.spy(new DefaultTokenGeneratorPlugin());
        Mockito.when(embeddedPlugin.createTokenValue(Mockito.anyMap())).thenReturn(UNPROTECTED_STRING);
        this.tokenGeneratorPluginProtectionWrapper = new TokenGeneratorPluginProtectionWrapper(embeddedPlugin);
    }

    //test case: createTokenValue with valid credentials should generate a protected string with the appropriate values
    @Test
    public void testCreateTokenValueValidCredentials() throws UnexpectedException, FogbowRasException {
        //set up

        //exercise
        String protectedTokenValue = this.tokenGeneratorPluginProtectionWrapper.createTokenValue(new HashMap<>());
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

        //verify
        Assert.assertEquals(UNPROTECTED_STRING, unprotectedTokenValue);
    }
}
