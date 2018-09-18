package org.fogbowcloud.ras.core.plugins.aaa.authentication.openstack;

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
    private HttpClient client;

    private static final String UTF_8 = "UTF-8";

    private String userId;
    private String projectId;
    private String providerId;
	private RSAPrivateKey privateKey;

    @Before
    public void setUp() {
        this.client = Mockito.spy(HttpClient.class);

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
        		parameters, OpenStackTokenGeneratorPlugin.TOKEN_VALUE_SEPARATOR);
        String signature = createSignature(tokenString);
        
		OpenStackV3Token token = new OpenStackV3Token(providerId, tokenValue,
                userId, userName, projectId, signature);
        HttpResponse httpResponse = Mockito.mock(HttpResponse.class);
        HttpEntity httpEntity = Mockito.mock(HttpEntity.class);
        String content = "any content";
        InputStream contentInputStream = new ByteArrayInputStream(content.getBytes(UTF_8));
        Mockito.when(httpEntity.getContent()).thenReturn(contentInputStream);
        Mockito.when(httpResponse.getEntity()).thenReturn(httpEntity);
        BasicStatusLine basicStatus = new BasicStatusLine(new ProtocolVersion("", 0, 0),
                HttpStatus.SC_OK, "");
        Mockito.when(httpResponse.getStatusLine()).thenReturn(basicStatus);
        Mockito.when(httpResponse.getAllHeaders()).thenReturn(new Header[0]);
        Mockito.when(this.client.execute(Mockito.any(HttpPost.class))).thenReturn(httpResponse);

        //exercise
        boolean isAuthenticated = this.authenticationPlugin.isAuthentic(token);

        //verify
        Assert.assertTrue(isAuthenticated);
    }
    
    // TODO check
    @Test
    public void testGetTokenValidTokenValueProviderIdDiferent() throws IOException, UnavailableProviderException, GeneralSecurityException {
    	//set up
    	String providerId = "Other";
        
		OpenStackV3Token token = new OpenStackV3Token(providerId, "", "", "", "", "");

        //exercise
        boolean isAuthenticated = this.authenticationPlugin.isAuthentic(token);

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
        HttpResponse httpResponse = Mockito.mock(HttpResponse.class);
        HttpEntity httpEntity = Mockito.mock(HttpEntity.class);
        String content = "any content";
        InputStream contentInputStream = new ByteArrayInputStream(content.getBytes(UTF_8));
        Mockito.when(httpEntity.getContent()).thenReturn(contentInputStream);
        Mockito.when(httpResponse.getEntity()).thenReturn(httpEntity);
        BasicStatusLine basicStatus = new BasicStatusLine(new ProtocolVersion("", 0, 0),
                HttpStatus.SC_BAD_GATEWAY, "");
        Mockito.when(httpResponse.getStatusLine()).thenReturn(basicStatus);
        Mockito.when(httpResponse.getAllHeaders()).thenReturn(new Header[0]);
        Mockito.when(this.client.execute(Mockito.any(HttpPost.class))).thenReturn(httpResponse);
        Mockito.when(this.client.execute(Mockito.any(HttpPost.class))).
                thenThrow(new HttpResponseException(HttpStatus.SC_BAD_GATEWAY, ""));
        
        //exercise
        boolean isAuthenticated = this.authenticationPlugin.isAuthentic(token);

        //verify
        Assert.assertFalse(isAuthenticated);
    }
    
    private String createSignature(String message) throws IOException, GeneralSecurityException {
        return RSAUtil.sign(this.privateKey, message);
    }
}

