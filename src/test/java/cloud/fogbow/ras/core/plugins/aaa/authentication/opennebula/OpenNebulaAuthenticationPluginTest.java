package cloud.fogbow.ras.core.plugins.aaa.authentication.opennebula;

import cloud.fogbow.ras.core.PropertiesHolder;
import cloud.fogbow.ras.core.constants.ConfigurationConstants;
import cloud.fogbow.ras.core.constants.Messages;
import org.apache.commons.lang.StringUtils;
import org.fogbowcloud.ras.core.models.tokens.OpenNebulaToken;
import org.fogbowcloud.ras.core.plugins.aaa.RASAuthenticationHolder;
import org.fogbowcloud.ras.core.plugins.aaa.tokengenerator.opennebula.OpenNebulaTokenGeneratorPlugin;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;

import java.io.IOException;
import java.security.GeneralSecurityException;
import java.security.interfaces.RSAPrivateKey;

public class OpenNebulaAuthenticationPluginTest {

    private OpenNebulaAuthenticationPlugin authenticationPlugin;
    private String userId;
    private String projectId;
    private String providerId;
    private RSAPrivateKey privateKey;

    @Before
    public void setUp() {
        this.userId = "userId";
        this.projectId = "projectId";
        this.providerId = PropertiesHolder.getInstance().getProperty(ConfigurationConstants.LOCAL_MEMBER_ID);

        try {
            this.privateKey = RASAuthenticationHolder.getInstance().getPrivateKey();
        } catch (IOException | GeneralSecurityException e) {
            throw new FatalErrorException(String.format(Messages.Fatal.ERROR_READING_PRIVATE_KEY_FILE, e.getMessage()));
        }

        this.authenticationPlugin = Mockito.spy(new OpenNebulaAuthenticationPlugin(this.providerId));
    }

    //test case: check if isAuthentic returns true when the tokenValue is valid.
    @Test
    public void testGetTokenValidTokenValue() throws IOException, GeneralSecurityException {
        //set up
        String providerId = this.providerId;
        String tokenValue = "fake-token";
        String userName = "fake-name";
        String userId = this.userId;

        String[] parameters = new String[] { providerId, tokenValue, userId, userName };
        String tokenString = StringUtils.join(parameters, OpenNebulaTokenGeneratorPlugin.OPENNEBULA_FIELD_SEPARATOR);
        String signature = createSignature(tokenString);

        OpenNebulaToken token = new OpenNebulaToken(providerId, tokenValue, userId, userName, signature);

        //exercise
        boolean isAuthenticated = this.authenticationPlugin.isAuthentic(this.providerId, token);

        //verify
        Assert.assertTrue(isAuthenticated);
    }

    // isAuthentic should return true when the token was issued by another member and the request comes from this
    // member (we assume that authentication was done at the member that issued the request).
    @Test
    public void testGetTokenValidTokenValueProviderIdDifferent() throws IOException, UnavailableProviderException, GeneralSecurityException {
        //set up
        String providerId = "other";

        OpenNebulaToken token = new OpenNebulaToken(providerId, "", "", "", "");

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
        OpenNebulaToken token = new OpenNebulaToken(this.providerId, "fake-token",
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
