package org.fogbowcloud.manager.core.plugins.behavior.identity.ldap;

import java.io.UnsupportedEncodingException;
import java.security.NoSuchAlgorithmException;
import java.util.HashMap;
import java.util.Map;

import org.fogbowcloud.manager.core.HomeDir;
import org.fogbowcloud.manager.core.PropertiesHolder;
import org.fogbowcloud.manager.core.constants.ConfigurationConstants;
import org.fogbowcloud.manager.core.exceptions.*;
import org.fogbowcloud.manager.core.models.tokens.LdapToken;
import org.fogbowcloud.manager.core.plugins.behavior.authentication.ldap.LdapAuthenticationPlugin;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import org.mockito.Mockito;

public class LdapFederationIdentityPluginTest {

    private static final String FAKE_NAME = "fake-name";
    private static final String FAKE_USER_ID = "fake-user-id";
    private static final String name = "ldapUser";
    private static final String password = "ldapUserPass";
    private static final String TOKEN_VALUE_SEPARATOR = "!#!";
    private LdapFederationIdentityPlugin ldapFederationIdentityPlugin;
    private LdapAuthenticationPlugin ldapAuthenticationPlugin;

    Map<String, String> userCredentials = new HashMap<String, String>();
    
    @Before
    public void setUp() {
        HomeDir.getInstance().setPath("src/test/resources/private");
        this.userCredentials.put(LdapFederationIdentityPlugin.CRED_USERNAME, FAKE_USER_ID);
        this.userCredentials.put(LdapFederationIdentityPlugin.CRED_PASSWORD, this.password);
        this.userCredentials.put(LdapFederationIdentityPlugin.CRED_AUTH_URL, "ldapUrl");
        this.userCredentials.put(LdapFederationIdentityPlugin.CRED_LDAP_BASE, "ldapBase");
        this.userCredentials.put(LdapFederationIdentityPlugin.CRED_LDAP_ENCRYPT, "");
        this.userCredentials.put(LdapFederationIdentityPlugin.CRED_PRIVATE_KEY, "private_key_path");
        this.userCredentials.put(LdapFederationIdentityPlugin.CRED_PUBLIC_KEY, "public_key_path");
        this.ldapFederationIdentityPlugin = Mockito.spy(new LdapFederationIdentityPlugin());
        this.ldapAuthenticationPlugin = new LdapAuthenticationPlugin();
    }
    
    //test case: check if the token value information is correct when creating a token with the correct user credentials.
    @Test
    public void testCreateToken() throws Exception {
    	//set up
        Mockito.doReturn(this.FAKE_NAME).when(this.ldapFederationIdentityPlugin)
                .ldapAuthenticate(Mockito.anyString(), Mockito.anyString());

        //exercise
        String federationTokenValue = this.ldapFederationIdentityPlugin.createTokenValue(userCredentials);
        LdapToken ldapToken = (LdapToken) this.ldapFederationIdentityPlugin.createToken(federationTokenValue);

        String split[] = federationTokenValue.split(TOKEN_VALUE_SEPARATOR);

        //verify
        Assert.assertEquals(split[0], FAKE_USER_ID);
        Assert.assertEquals(split[1], FAKE_NAME);
        Assert.assertEquals(split[2], ldapToken.getExpirationTime());
        Assert.assertTrue(this.ldapAuthenticationPlugin.isAuthentic(ldapToken));
    }

    //test case: check if createFederationTokenValue throws UnauthenticatedUserException when the user credentials
    // are invalid.
    @Test (expected = UnauthenticatedUserException.class)
    public void testCreateTokenFail() throws Exception {
    	//set up
        Mockito.doThrow(new UnauthenticatedUserException("Invalid User"))
                .when(this.ldapFederationIdentityPlugin)
                .ldapAuthenticate(Mockito.eq(name), Mockito.eq(this.password));
        
        //exercise/verify
        this.ldapFederationIdentityPlugin.createTokenValue(this.userCredentials);
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
    @Ignore
    @Test
    public void testGetTokenInvalidTokenValue() throws Exception {
        // set up
        String userName = "User Full Name";
        LdapAuthenticationPlugin authenticationPlugin = Mockito.spy(new LdapAuthenticationPlugin());
        Mockito.doReturn(userName)
                .when(ldapFederationIdentityPlugin)
                .ldapAuthenticate(Mockito.eq(name), Mockito.eq(password));

        // exercise
        String tokenAValue = this.ldapFederationIdentityPlugin.createTokenValue(this.userCredentials);
        LdapToken tokenA = (LdapToken) this.ldapFederationIdentityPlugin.createToken(tokenAValue);

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

    // test case: try to get federated user info with a valid token returns the user information correctly.
    @Test
    public void testGetFederatedUser() throws FogbowManagerException, NoSuchAlgorithmException,
            UnsupportedEncodingException {
        //set up
        Mockito.doReturn(this.FAKE_NAME).when(this.ldapFederationIdentityPlugin)
                .ldapAuthenticate(Mockito.anyString(), Mockito.anyString());
        String localMember = PropertiesHolder.getInstance().getProperty(ConfigurationConstants.LOCAL_MEMBER_ID);

        //exercise
        String federationTokenValue = this.ldapFederationIdentityPlugin.createTokenValue(userCredentials);
        LdapToken ldapToken = (LdapToken) this.ldapFederationIdentityPlugin.createToken(federationTokenValue);

        String split[] = federationTokenValue.split(TOKEN_VALUE_SEPARATOR);

        //verify
        Assert.assertEquals(ldapToken.getUserId(), FAKE_USER_ID);
        Assert.assertEquals(ldapToken.getUserName(), FAKE_NAME);
        Assert.assertEquals(ldapToken.getTokenProvider(), localMember);
        Assert.assertEquals(ldapToken.getTokenValue(), federationTokenValue);
        Assert.assertTrue(this.ldapAuthenticationPlugin.isAuthentic(ldapToken));
    }
}
