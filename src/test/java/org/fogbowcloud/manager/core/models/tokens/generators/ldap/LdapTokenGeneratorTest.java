package org.fogbowcloud.manager.core.models.tokens.generators.ldap;

import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;

import org.apache.commons.codec.binary.Base64;
import org.fogbowcloud.manager.core.HomeDir;
import org.fogbowcloud.manager.core.PropertiesHolder;
import org.fogbowcloud.manager.core.exceptions.FogbowManagerException;
import org.fogbowcloud.manager.core.exceptions.InvalidCredentialsUserException;
import org.fogbowcloud.manager.core.exceptions.TokenValueCreationException;
import org.fogbowcloud.manager.core.exceptions.UnauthenticatedUserException;
import org.fogbowcloud.manager.core.plugins.behavior.authentication.ldap.LdapAuthenticationPlugin;
import org.json.JSONObject;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;

public class LdapTokenGeneratorTest {

    private static final String IDENTITY_URL_KEY = "identity_url";
    private final String KEYSTONE_URL = "http://localhost:0000";
    private final String MOCK_SIGNATURE = "mock_signature";
    private LdapTokenGenerator tokenGenerator;
    private final String name = "ldapUser";
    private final String password = "ldapUserPass";
    private final String userName = "User Full Name";
    Map<String, String> userCredentials = new HashMap<String, String>();
    
    @Before
    public void setUp() throws Exception {
        HomeDir.getInstance().setPath("src/test/resources/private");
        PropertiesHolder propertiesHolder = PropertiesHolder.getInstance();
        Properties properties = propertiesHolder.getProperties();
        properties.put(IDENTITY_URL_KEY, this.KEYSTONE_URL);
        this.tokenGenerator = Mockito.spy(new LdapTokenGenerator());
        this.userCredentials.put(LdapTokenGenerator.CRED_USERNAME, this.name);
        this.userCredentials.put(LdapTokenGenerator.CRED_PASSWORD, this.password);
        this.userCredentials.put(LdapTokenGenerator.CRED_AUTH_URL, "ldapUrl");
        this.userCredentials.put(LdapTokenGenerator.CRED_LDAP_BASE, "ldapBase");
        this.userCredentials.put(LdapTokenGenerator.CRED_LDAP_ENCRYPT, "");
        this.userCredentials.put(LdapTokenGenerator.CRED_PRIVATE_KEY, "private_key_path");
        this.userCredentials.put(LdapTokenGenerator.CRED_PUBLIC_KEY, "public_key_path");
		Mockito.doReturn(this.MOCK_SIGNATURE).when(this.tokenGenerator)
				.createSignature(Mockito.any(JSONObject.class));
    }
    
    //test case: check if the access id information is correct when creating a token with the correct user credentials.
    @Test
    public void testCreateToken() throws Exception {
    	//set up
		Mockito.doReturn(this.userName).when(this.tokenGenerator).ldapAuthenticate(Mockito.eq(this.name),
				Mockito.eq(this.password));
        
        //exercise
        String federationTokenValue = this.tokenGenerator.createTokenValue(userCredentials);

        //verify
        String decodedAccessId = decodeAccessId(federationTokenValue);
        Assert.assertTrue(decodedAccessId.contains(this.name));
        Assert.assertTrue(decodedAccessId.contains(this.userName));
        Assert.assertTrue(decodedAccessId.contains(this.MOCK_SIGNATURE));
    }

    // test case: token information is based on the given credentials
    @Test
    public void testCreateTokenBasedOnGivenCredentials() throws Exception {
        // set up
        String userName = "User Full Name";
        Mockito.doReturn(userName)
                .when(this.tokenGenerator)
                .ldapAuthenticate(Mockito.eq(name), Mockito.eq(password));

        // exercise: create a token with valid credentials and after decode this token.
        String token = this.tokenGenerator.createTokenValue(userCredentials);
        String decodedAccessId = decodeAccessId(token);

        // verify
        Assert.assertTrue(decodedAccessId.contains(name));
        Assert.assertTrue(decodedAccessId.contains(userName));
        Assert.assertTrue(decodedAccessId.contains(MOCK_SIGNATURE));
    }
    
    //test case: check if createFederationTokenValue throws UnauthenticatedUserException when the user credentials are invalid.
    @Test (expected = UnauthenticatedUserException.class)
    public void testCreateTokenFail() throws Exception {
    	//set up
        Mockito.doThrow(new FogbowManagerException("Invalid User"))
                .when(this.tokenGenerator)
                .ldapAuthenticate(Mockito.eq(name), Mockito.eq(this.password));
        
        //exercise/verify
        this.tokenGenerator.createTokenValue(this.userCredentials);
    }

    private String decodeAccessId(String accessId) {
        return new String(Base64.decodeBase64(accessId), StandardCharsets.UTF_8);
    }

    // test case: the create token method throws an exception when receiving an empty map
    @SuppressWarnings("unchecked")
    @Test(expected = InvalidCredentialsUserException.class)
    public void testCreateTokenWithoutCredentials() throws UnauthenticatedUserException, TokenValueCreationException {

        // exercise/verify: try create a token with empty credentials
        this.tokenGenerator.createTokenValue(Mockito.anyMap());
    }
    // test case: authentication with invalids credentials throws an exception.
    @Test(expected = FogbowManagerException.class)
    public void testLdapAuthenticateWithoutLdapUrl() throws Exception {
        // exercise/verify: try to get authentication with invalids user id and password.
        this.tokenGenerator.ldapAuthenticate(Mockito.anyString(), Mockito.anyString());
    }

    // test case: the creation of the token value throws an exception when ldap plugin does not authenticate the user
    @Test(expected = InvalidCredentialsUserException.class)
    public void testCreateTokenWithInvalidUserName() throws Exception {
        // set up
        Mockito.doThrow(new FogbowManagerException("Invalid User"))
                .when(this.tokenGenerator)
                .ldapAuthenticate(Mockito.anyString(), Mockito.anyString());

        // exercise/verify: try create a token, must to throws an exception because
        // LDAP will not authenticate the user.
        this.tokenGenerator.createTokenValue(this.userCredentials);
    }

    // test case: test create token value with a invalid access id returns an invalid token.
    @Test
    public void testGetTokenInvalidAccessId() throws Exception {
        // set up
        String userName = "User Full Name";
        LdapAuthenticationPlugin authenticationPlugin = Mockito.spy(new LdapAuthenticationPlugin());

        Mockito.doReturn(userName)
                .when(tokenGenerator)
                .ldapAuthenticate(Mockito.eq(name), Mockito.eq(password));

        // exercise
        String tokenA = this.tokenGenerator.createTokenValue(this.userCredentials);

        // verify
        Assert.assertFalse(authenticationPlugin.isAuthentic(tokenA));
    }
}
