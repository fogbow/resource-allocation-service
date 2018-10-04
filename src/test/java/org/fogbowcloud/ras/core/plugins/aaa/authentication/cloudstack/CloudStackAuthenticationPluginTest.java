package org.fogbowcloud.ras.core.plugins.aaa.authentication.cloudstack;

import org.apache.commons.lang.StringUtils;
import org.apache.http.*;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.message.BasicStatusLine;
import org.fogbowcloud.ras.core.PropertiesHolder;
import org.fogbowcloud.ras.core.constants.ConfigurationConstants;
import org.fogbowcloud.ras.core.constants.Messages;
import org.fogbowcloud.ras.core.exceptions.FatalErrorException;
import org.fogbowcloud.ras.core.exceptions.UnavailableProviderException;
import org.fogbowcloud.ras.core.models.tokens.CloudStackToken;
import org.fogbowcloud.ras.core.plugins.aaa.authentication.openstack.v3.OpenStackAuthenticationPlugin;
import org.fogbowcloud.ras.core.plugins.aaa.tokengenerator.cloudstack.CloudStackTokenGeneratorPlugin;
import org.fogbowcloud.ras.util.RSAUtil;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.security.GeneralSecurityException;
import java.security.interfaces.RSAPrivateKey;

public class CloudStackAuthenticationPluginTest {
    private CloudStackAuthenticationPlugin authenticationPlugin;

    private String userId;
    private String username;
    private String providerId;
    private RSAPrivateKey privateKey;

    @Before
    public void setUp() {
        this.userId = "fake-user-id";
        this.username = "fake-username";
        this.providerId = PropertiesHolder.getInstance().getProperty(ConfigurationConstants.LOCAL_MEMBER_ID);

        try {
            this.privateKey = RSAUtil.getPrivateKey();
        } catch (IOException | GeneralSecurityException e) {
            throw new FatalErrorException(String.format(Messages.Fatal.ERROR_READING_PRIVATE_KEY_FILE, e.getMessage()));
        }

        this.authenticationPlugin = Mockito.spy(new CloudStackAuthenticationPlugin());
    }

    //test case: check if isAuthentic returns true when the tokenValue is valid.
    @Test
    public void testGetTokenValidTokenValue() throws IOException, GeneralSecurityException {
        //set up
        String tokenValue = "fake-api-key:fake-secret-key";

        String[] parameters = {providerId, tokenValue, userId, username};
        String tokenString = StringUtils.join(parameters, CloudStackTokenGeneratorPlugin.CLOUDSTACK_TOKEN_STRING_SEPARATOR);
        String signature = createSignature(tokenString);

        CloudStackToken token = new CloudStackToken(providerId, tokenValue, userId, username, signature);

        //exercise
        boolean isAuthenticated = this.authenticationPlugin.isAuthentic(this.providerId, token);

        //verify
        Assert.assertTrue(isAuthenticated);
    }

    // Test case: isAuthentic should return true when the token was issued by another member and the request comes from this
    // member (we assume that authentication was done at the member that issued the request).
    @Test
    public void testGetTokenValidTokenValueProviderIdDifferent() throws IOException, UnavailableProviderException, GeneralSecurityException {
        //set up
        String providerId = "other";

        CloudStackToken token = new CloudStackToken(providerId, "", "", "", "");

        //exercise
        boolean isAuthenticated = this.authenticationPlugin.isAuthentic(providerId, token);

        //verify
        Assert.assertTrue(isAuthenticated);
    }

    //test case: check if isAuthentic returns false when the tokenValue is not valid
    @Test
    public void testGetTokenError() throws Exception {
        //set up
        String signature = "anySignature";
        CloudStackToken token = new CloudStackToken(this.providerId, "fake-token",
                this.userId, "fake-name", signature);

        //exercise
        boolean isAuthenticated = this.authenticationPlugin.isAuthentic(this.providerId, token);

        //verify
        Assert.assertFalse(isAuthenticated);
    }

    private String createSignature(String message) throws IOException, GeneralSecurityException {
        return RSAUtil.sign(this.privateKey, message);
    }
}
