package org.fogbowcloud.manager.core.plugins.behavior.identity.ldap;

import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;

import org.apache.commons.codec.binary.Base64;
import org.fogbowcloud.manager.core.HomeDir;
import org.fogbowcloud.manager.core.PropertiesHolder;
import org.fogbowcloud.manager.core.exceptions.*;
import org.fogbowcloud.manager.core.models.tokens.FederationUserToken;
import org.fogbowcloud.manager.core.plugins.behavior.authentication.ldap.LdapAuthenticationPlugin;
import org.json.JSONObject;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;

public class LdapFederationIdentityPluginTest {

    private static final String IDENTITY_URL_KEY = "identity_url";
    private final String KEYSTONE_URL = "http://localhost:0000";
    private final String MOCK_SIGNATURE = "mock_signature";
    private final String name = "ldapUser";
    private final String password = "ldapUserPass";
    private final String userName = "User Full Name";
    public static final String TOKEN_VALUE_SEPARATOR = "!#!";
    private LdapFederationIdentityPlugin ldapFederationIdentityPlugin;

    Map<String, String> userCredentials = new HashMap<String, String>();
    
    @Before
    public void setUp() throws Exception {
        HomeDir.getInstance().setPath("src/test/resources/private");
        PropertiesHolder propertiesHolder = PropertiesHolder.getInstance();
        Properties properties = propertiesHolder.getProperties();
        properties.put(IDENTITY_URL_KEY, this.KEYSTONE_URL);
        this.ldapFederationIdentityPlugin = Mockito.spy(new LdapFederationIdentityPlugin());
        this.userCredentials.put(LdapFederationIdentityPlugin.CRED_USERNAME, this.name);
        this.userCredentials.put(LdapFederationIdentityPlugin.CRED_PASSWORD, this.password);
        this.userCredentials.put(LdapFederationIdentityPlugin.CRED_AUTH_URL, "ldapUrl");
        this.userCredentials.put(LdapFederationIdentityPlugin.CRED_LDAP_BASE, "ldapBase");
        this.userCredentials.put(LdapFederationIdentityPlugin.CRED_LDAP_ENCRYPT, "");
        this.userCredentials.put(LdapFederationIdentityPlugin.CRED_PRIVATE_KEY, "private_key_path");
        this.userCredentials.put(LdapFederationIdentityPlugin.CRED_PUBLIC_KEY, "public_key_path");
		Mockito.doReturn(this.MOCK_SIGNATURE).when(this.ldapFederationIdentityPlugin)
				.createSignature(Mockito.any(JSONObject.class));
    }
    
    //test case: check if the token value information is correct when creating a token with the correct user credentials.
    @Test
    public void testCreateToken() throws Exception {
    	//set up
		Mockito.doReturn(this.userName).when(this.ldapFederationIdentityPlugin).ldapAuthenticate(Mockito.eq(this.name),
				Mockito.eq(this.password));
        
        //exercise
        String federationTokenValue = this.ldapFederationIdentityPlugin.createTokenValue(userCredentials);

        //verify
        String decodedTokenValue = decodeTokenValue(federationTokenValue);
        Assert.assertTrue(decodedTokenValue.contains(this.name));
        Assert.assertTrue(decodedTokenValue.contains(this.userName));
        Assert.assertTrue(decodedTokenValue.contains(this.MOCK_SIGNATURE));
    }

    // test case: token information is based on the given credentials
    @Test
    public void testCreateTokenBasedOnGivenCredentials() throws Exception {
        // set up
        String userName = "User Full Name";
        Mockito.doReturn(userName)
                .when(this.ldapFederationIdentityPlugin)
                .ldapAuthenticate(Mockito.eq(name), Mockito.eq(password));

        // exercise: create a token with valid credentials and after decode this token.
        String token = this.ldapFederationIdentityPlugin.createTokenValue(userCredentials);
        String decodedTokenValue = decodeTokenValue(token);

        // verify
        Assert.assertTrue(decodedTokenValue.contains(name));
        Assert.assertTrue(decodedTokenValue.contains(userName));
        Assert.assertTrue(decodedTokenValue.contains(MOCK_SIGNATURE));
    }
    
    //test case: check if createFederationTokenValue throws UnauthenticatedUserException when the user credentials are invalid.
    @Test (expected = UnauthenticatedUserException.class)
    public void testCreateTokenFail() throws Exception {
    	//set up
        Mockito.doThrow(new FogbowManagerException("Invalid User"))
                .when(this.ldapFederationIdentityPlugin)
                .ldapAuthenticate(Mockito.eq(name), Mockito.eq(this.password));
        
        //exercise/verify
        this.ldapFederationIdentityPlugin.createTokenValue(this.userCredentials);
    }

    private String decodeTokenValue(String tokenValue) {
        return new String(Base64.decodeBase64(tokenValue), StandardCharsets.UTF_8);
    }

    // test case: the create token method throws an exception when receiving an empty map
    @SuppressWarnings("unchecked")
    @Test(expected = InvalidCredentialsUserException.class)
    public void testCreateTokenWithoutCredentials() throws UnauthenticatedUserException, TokenValueCreationException {

        // exercise/verify: try create a token with empty credentials
        this.ldapFederationIdentityPlugin.createTokenValue(Mockito.anyMap());
    }
    // test case: authentication with invalids credentials throws an exception.
    @Test(expected = FogbowManagerException.class)
    public void testLdapAuthenticateWithoutLdapUrl() throws Exception {
        // exercise/verify: try to get authentication with invalids user id and password.
        this.ldapFederationIdentityPlugin.ldapAuthenticate(Mockito.anyString(), Mockito.anyString());
    }

    // test case: the creation of the token value throws an exception when ldap plugin does not authenticate the user
    @Test(expected = InvalidCredentialsUserException.class)
    public void testCreateTokenWithInvalidUserName() throws Exception {
        // set up
        Mockito.doThrow(new FogbowManagerException("Invalid User"))
                .when(this.ldapFederationIdentityPlugin)
                .ldapAuthenticate(Mockito.anyString(), Mockito.anyString());

        // exercise/verify: try create a token, must to throws an exception because
        // LDAP will not authenticate the user.
        this.ldapFederationIdentityPlugin.createTokenValue(this.userCredentials);
    }

    // test case: test create token value with a invalid token value returns an invalid token.
    @Test
    public void testGetTokenInvalidTokenValue() throws Exception {
        // set up
        String userName = "User Full Name";
        LdapAuthenticationPlugin authenticationPlugin = Mockito.spy(new LdapAuthenticationPlugin());

        Mockito.doReturn(userName)
                .when(ldapFederationIdentityPlugin)
                .ldapAuthenticate(Mockito.eq(name), Mockito.eq(password));

        // exercise
        String tokenA = this.ldapFederationIdentityPlugin.createTokenValue(this.userCredentials);

        // verify
        Assert.assertFalse(authenticationPlugin.isAuthentic(tokenA));
    }

    // test case: try to get federated user info with an empty token throws an exception.
    @Test(expected = UnauthenticatedUserException.class)
    public void testGetFederatedUserWithEmptyToken()
            throws UnauthenticatedUserException, InvalidParameterException {

        // exercise/verify: try to get federated user info
        this.ldapFederationIdentityPlugin.createToken(Mockito.anyString());
    }

    // test case: try to get federated user info with a invalid token throws an exception.
    @Test(expected = UnauthenticatedUserException.class)
    public void testGetFederatedUserWithInvalidToken()
            throws UnauthenticatedUserException, InvalidParameterException {
        // set up
        String invalidToken =
                "InvalidJsonCredentials" +
                        TOKEN_VALUE_SEPARATOR +
                        "InvalidJsonCredentialsHashed";

        byte[] bytes = invalidToken.getBytes();

        // exercise/verify: try to get federated user info
        this.ldapFederationIdentityPlugin.createToken(new String(Base64.encodeBase64(bytes)));
    }

    // test case: try to get federated user info with a valid token returns the user information correctly.
    @Test
    public void testGetFederatedUser() throws UnauthenticatedUserException, InvalidParameterException {
        // set up
        String jsonCredentials = "{\n\t\"name\": \"user\",\n\t\"login\": \"login\"\n}";
        String token = jsonCredentials + TOKEN_VALUE_SEPARATOR + "fake-json-credentials-hashed";

        byte[] bytes = token.getBytes();
        FederationUserToken federationUserToken = new FederationUserToken("token", "login", "user");

        Mockito.doReturn(true)
                .when(this.ldapFederationIdentityPlugin)
                .verifySign(Mockito.anyString(), Mockito.anyString());

        // exercise: get a federated user from a valid token credential
        FederationUserToken returnedUser =
                this.ldapFederationIdentityPlugin.createToken(new String(Base64.encodeBase64(bytes)));

        // exercise: compares if the returned user info is the expected
        Assert.assertEquals(federationUserToken, returnedUser);
    }

}
