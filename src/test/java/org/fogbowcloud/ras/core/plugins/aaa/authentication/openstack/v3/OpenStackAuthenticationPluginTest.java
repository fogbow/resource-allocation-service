package org.fogbowcloud.ras.core.plugins.aaa.authentication.openstack.v3;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.security.GeneralSecurityException;
import java.security.interfaces.RSAPrivateKey;

import org.apache.commons.lang.StringUtils;
import org.apache.http.Header;
import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.HttpStatus;
import org.apache.http.ProtocolVersion;
import org.apache.http.client.HttpClient;
import org.apache.http.client.HttpResponseException;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.message.BasicStatusLine;
import org.fogbowcloud.ras.core.PropertiesHolder;
import org.fogbowcloud.ras.core.constants.ConfigurationConstants;
import org.fogbowcloud.ras.core.constants.Messages;
import org.fogbowcloud.ras.core.exceptions.FatalErrorException;
import org.fogbowcloud.ras.core.exceptions.UnavailableProviderException;
import org.fogbowcloud.ras.core.models.tokens.OpenStackV3Token;
import org.fogbowcloud.ras.core.plugins.aaa.tokengenerator.openstack.v3.OpenStackTokenGeneratorPlugin;
import org.fogbowcloud.ras.util.RSAUtil;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;

public class OpenStackAuthenticationPluginTest {
    private OpenStackAuthenticationPlugin authenticationPlugin;

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
            this.privateKey = RSAUtil.getPrivateKey();
        } catch (IOException | GeneralSecurityException e) {
            throw new FatalErrorException(String.format(Messages.Fatal.ERROR_READING_PRIVATE_KEY_FILE, e.getMessage()));
        }  
        
        this.authenticationPlugin = Mockito.spy(new OpenStackAuthenticationPlugin());
    }

    //test case: check if isAuthentic returns true when the tokenValue is valid.
    @Test
    public void testGetTokenValidTokenValue() throws IOException, UnavailableProviderException, GeneralSecurityException {
    	//set up
    	String providerId = this.providerId;
    	String projectId = this.projectId;
    	String tokenValue = "fake-token";
    	String userName = "fake-name";
    	String userId = this.userId;

		String[] parameters = new String[] {providerId, tokenValue, 
        		userId, userName, projectId}; 
        String tokenString = StringUtils.join(
        		parameters, OpenStackTokenGeneratorPlugin.OPENSTACK_TOKEN_STRING_SEPARATOR);
        String signature = createSignature(tokenString);
        
		OpenStackV3Token token = new OpenStackV3Token(providerId, tokenValue,
                userId, userName, projectId, signature);

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
        
		OpenStackV3Token token = new OpenStackV3Token(providerId, "", "", "", "", "");

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
		OpenStackV3Token token = new OpenStackV3Token(this.providerId, "fake-token",
                this.userId, "fake-name", this.projectId, signature);

        //exercise
        boolean isAuthenticated = this.authenticationPlugin.isAuthentic(this.providerId, token);

        //verify
        Assert.assertFalse(isAuthenticated);
    }
    
    private String createSignature(String message) throws IOException, GeneralSecurityException {
        return RSAUtil.sign(this.privateKey, message);
    }
}

