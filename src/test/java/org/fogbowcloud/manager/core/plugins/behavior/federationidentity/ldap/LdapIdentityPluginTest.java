package org.fogbowcloud.manager.core.plugins.behavior.federationidentity.ldap;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.doReturn;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.security.GeneralSecurityException;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.security.SignatureException;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;

import org.apache.commons.codec.binary.Base64;
import org.fogbowcloud.manager.core.exceptions.FogbowManagerException;
import org.fogbowcloud.manager.core.exceptions.InvalidCredentialsUserException;
import org.fogbowcloud.manager.core.exceptions.TokenValueCreationException;
import org.fogbowcloud.manager.core.exceptions.UnauthenticatedUserException;
import org.fogbowcloud.manager.core.exceptions.UnexpectedException;
import org.fogbowcloud.manager.core.models.tokens.FederationUser;
import org.fogbowcloud.manager.util.PropertiesUtil;
import org.fogbowcloud.manager.util.RSAUtil;
import org.json.JSONObject;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mockito;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;

import com.google.common.base.Charsets;

@RunWith(PowerMockRunner.class)
@PrepareForTest({PropertiesUtil.class, RSAUtil.class})
public class LdapIdentityPluginTest {
	
    private static final String IDENTITY_URL_KEY = "identity_url";
    private final String KEYSTONE_URL = "http://localhost:" + 3000;
    public static final String ACCESSID_SEPARATOR = "!#!";

    private final String MOCK_SIGNATURE = "mock_signature";

    private LdapIdentityPlugin identityPlugin;
    private Map<String, String> userCredentials;
    private String name;
    private String password;

    @Before
    public void setUp()
            throws InvalidKeyException, NoSuchAlgorithmException, SignatureException,
                    UnsupportedEncodingException, IOException, GeneralSecurityException {
        Properties properties = Mockito.spy(Properties.class);

        PowerMockito.mockStatic(PropertiesUtil.class);
        Mockito.when(PropertiesUtil.readProperties(Mockito.anyString())).thenReturn(properties);
        Mockito.when(properties.getProperty(Mockito.anyString())).thenReturn(Mockito.anyString());

        this.identityPlugin = Mockito.spy(new LdapIdentityPlugin());

        properties.put(IDENTITY_URL_KEY, KEYSTONE_URL);
        doReturn(MOCK_SIGNATURE)
                .when(identityPlugin)
                .createSignature(Mockito.any(JSONObject.class));

        this.name = "ldapUser";
        this.password = "ldapUserPass";

        this.userCredentials = new HashMap<String, String>();
        this.setUpCredentials();
    }

    
    // test case: the create token method must to throw a exception when call him with
    // a credentials map empty 
    @SuppressWarnings("unchecked")
    @Test(expected = InvalidCredentialsUserException.class)
    public void testCreateTokenWithoutCredentials()
            throws UnauthenticatedUserException, TokenValueCreationException {
    	
    	// exercise/verify: try create a token with empty credentials
        this.identityPlugin.createFederationTokenValue(Mockito.anyMap());
    }

    // test case: authentication with invalids credentials must throw a exception.
    @Test(expected = FogbowManagerException.class)
    public void testLdapAuthenticateWithoutLdapUrl() throws Exception {
    	// exercise/verify: try to get authentication with invalids user id and password.
    	this.identityPlugin.ldapAuthenticate(Mockito.anyString(), Mockito.anyString());
    }

    // test case: try to create a token with valid credentials and after try to decode this
    // token to get same credentials.
    @Test
    public void testCreateToken() throws Exception {
    	// set up
        String userName = "User Full Name";
        Mockito.doReturn(userName)
                .when(this.identityPlugin)
                .ldapAuthenticate(Mockito.eq(name), Mockito.eq(password));

        // exercise: create a token with valid credentials and after decode this token. 
        String token = this.identityPlugin.createFederationTokenValue(this.userCredentials);
        String decodedAccessId = decodeAccessId(token);

        // verify
        assertTrue(decodedAccessId.contains(name));
        assertTrue(decodedAccessId.contains(userName));
        assertTrue(decodedAccessId.contains(MOCK_SIGNATURE));
    }

    
    // test case: create authentication token must to throw a exception when ldap can not
    // authenticate passed credentials.
    @Test(expected = InvalidCredentialsUserException.class)
    public void testCreateTokenWithInvalidUserName() throws Exception {
    	// set up
        Mockito.doThrow(new FogbowManagerException("Invalid User"))
                .when(this.identityPlugin)
                .ldapAuthenticate(Mockito.anyString(), Mockito.anyString());

        // exercise/verify: try create a token, must to throw a exception because
        // LDAP will not authenticate the user.
        this.identityPlugin.createFederationTokenValue(this.userCredentials);
    }

    // test case: test create authentication token with a invalid access id,
    // should return a invalid token.
    @Test
    public void testGetTokenInvalidAccessId() throws Exception {
    	// set up
        String userName = "User Full Name";

        Mockito.doReturn(userName)
                .when(identityPlugin)
                .ldapAuthenticate(Mockito.eq(name), Mockito.eq(password));

        // exercise
        String tokenA = this.identityPlugin.createFederationTokenValue(this.userCredentials);

        // verify
        assertFalse(this.identityPlugin.isValid(tokenA));
    }

    
    // test case: try to get federated user info with a empty token,
    // should throw a exception.
    @Test(expected = UnauthenticatedUserException.class)
    public void testGetFederatedUserWithEmptyToken()
            throws UnauthenticatedUserException, UnexpectedException {
    	
    	// exercise/verify: try to get federated user info
        this.identityPlugin.getFederationUser(Mockito.anyString());
    }

    // test case: try to get federated user info with a invalid token,
    // should throw a exception.
    @Test(expected = UnauthenticatedUserException.class)
    public void testGetFederatedUserWithInvalidToken()
            throws UnauthenticatedUserException, UnexpectedException {
        // set up
    	String invalidToken =
                "InvalidJsonCredentials" +
                ACCESSID_SEPARATOR +
                "InvalidJsonCredentialsHashed";

        byte[] bytes = invalidToken.getBytes();
        
        // exercise/verify: try to get federated user info
        this.identityPlugin.getFederationUser(new String(Base64.encodeBase64(bytes)));
    }

    // test case: try to get federated user info with a valid token,
    // should return the user information correctly.
    @Test
    public void testGetFederatedUser() throws UnauthenticatedUserException, UnexpectedException {
        // set up
    	String jsonCredentials = "{\n\t\"name\": \"user\",\n\t\"login\": \"login\"\n}";
        String token = jsonCredentials + ACCESSID_SEPARATOR + "fake-json-credentials-hashed";

        byte[] bytes = token.getBytes();
        HashMap<String, String> attributes = new HashMap<String, String>();
        attributes.put(FederationUser.MANDATORY_NAME_ATTRIBUTE, "user");
        
        FederationUser user = new FederationUser("login", attributes);

        Mockito.doReturn(true)
                .when(this.identityPlugin)
                .verifySign(Mockito.anyString(), Mockito.anyString());
        
        // exercise: get a federated user from a valid token credential
        FederationUser returnedUser =
                this.identityPlugin.getFederationUser(new String(Base64.encodeBase64(bytes)));

        // exercise: compares if the returned user info is the expected
        assertEquals(user, returnedUser);
    }

    private String decodeAccessId(String accessId) {
        return new String(Base64.decodeBase64(accessId), Charsets.UTF_8);
    }
    
    private void setUpCredentials() {
        this.userCredentials.put(LdapIdentityPlugin.CRED_USERNAME, name);
        this.userCredentials.put(LdapIdentityPlugin.CRED_PASSWORD, password);
        this.userCredentials.put(LdapIdentityPlugin.CRED_AUTH_URL, "ldapUrl");
        this.userCredentials.put(LdapIdentityPlugin.CRED_LDAP_BASE, "ldapBase");
        this.userCredentials.put(LdapIdentityPlugin.CRED_LDAP_ENCRYPT, "");
        this.userCredentials.put(LdapIdentityPlugin.CRED_PRIVATE_KEY, "private_key_path");
        this.userCredentials.put(LdapIdentityPlugin.CRED_PUBLIC_KEY, "public_key_path");
    }
}
