package org.fogbowcloud.manager.core.models.tokens.generators.ldap;

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

import java.io.UnsupportedEncodingException;
import java.security.NoSuchAlgorithmException;

public class LdapTokenGeneratorTest {
    @Before
    public void setUp() {
    }

    //test case: createTokenValue with valid credentials should generate a string with the appropriate values
    @Ignore
    @Test
    public void testCreateTokenValueValidCredentials() {
    }

    //test case: createTokenValue with incorrect credentials should throw InvalidParameterException
    @Ignore
    @Test (expected = InvalidUserCredentialsException.class)
    public void testCreateTokenValueIncorrectCredentials() {
    }

    //test case: createTokenValue with invalid credentials should throw FogbowManagerException
    @Ignore
    @Test (expected = TokenValueCreationException.class)
    public void testCreateTokenValueInvalidCredentials() {
    }

//    //test case: check if createToken throws UnauthenticatedUserException when the token is invalid.
//    @Test (expected = UnauthenticatedUserException.class)
//    public void testCreateTokenFail() throws Exception {
//        //set up
//        LdapTokenGenerator tokenGenerator = Mockito.spy(new LdapTokenGenerator());
//        Mockito.doThrow(new UnauthenticatedUserException("Invalid User"))
//                .when(tokenGenerator).ldapAuthenticate(Mockito.eq(name), Mockito.eq(this.password));
//
//        //exercise/verify
//        this.ldapFederationIdentityPlugin.createToken();
//    }
//
//    // test case: the create token method throws an exception when receiving an empty map
//    @SuppressWarnings("unchecked")
//    @Test(expected = InvalidUserCredentialsException.class)
//    public void testCreateTokenWithoutCredentials() throws UnauthenticatedUserException, TokenValueCreationException {
//
//        // exercise/verify: try create a token with empty credentials
//        this.ldapFederationIdentityPlugin.createTokenValue(Mockito.anyMap());
//    }
//
//    // test case: authentication with invalids credentials throws an exception.
//    @Test(expected = FogbowManagerException.class)
//    public void testLdapAuthenticateWithoutLdapUrl() throws Exception {
//        // exercise/verify: try to get authentication with invalids user id and password.
//        this.ldapFederationIdentityPlugin.ldapAuthenticate(Mockito.anyString(), Mockito.anyString());
//    }
//
//    // test case: the creation of the token value throws an exception when ldap plugin does not authenticate the user
//    @Test(expected = InvalidUserCredentialsException.class)
//    public void testCreateTokenWithInvalidUserName() throws Exception {
//        // set up
//        Mockito.doThrow(new FogbowManagerException("Invalid User"))
//                .when(this.ldapFederationIdentityPlugin)
//                .ldapAuthenticate(Mockito.anyString(), Mockito.anyString());
//
//        // exercise/verify: try create a token, must to throws an exception because
//        // LDAP will not authenticate the user.
//        this.ldapFederationIdentityPlugin.createTokenValue(this.userCredentials);
//    }
//
//    // test case: test create token value with a invalid token value returns an invalid token.
//    @Ignore
//    @Test
//    public void testGetTokenInvalidTokenValue() throws Exception {
//        // set up
//        String userName = "User Full Name";
//        LdapAuthenticationPlugin authenticationPlugin = Mockito.spy(new LdapAuthenticationPlugin());
//        Mockito.doReturn(userName)
//                .when(ldapFederationIdentityPlugin)
//                .ldapAuthenticate(Mockito.eq(name), Mockito.eq(password));
//
//        // exercise
//        String tokenAValue = this.ldapFederationIdentityPlugin.createTokenValue(this.userCredentials);
//        LdapToken tokenA = (LdapToken) this.ldapFederationIdentityPlugin.createToken(tokenAValue);
//
//        // verify
//        Assert.assertFalse(authenticationPlugin.isAuthentic(tokenA));
//    }
//
//    // test case: try to get federated user info with an empty token throws an exception.
//    @Test(expected = UnauthenticatedUserException.class)
//    public void testGetFederatedUserWithEmptyToken()
//            throws UnauthenticatedUserException, InvalidParameterException {
//
//        // exercise/verify: try to get federated user info
//        this.ldapFederationIdentityPlugin.createToken(Mockito.anyString());
//    }
//
//    // test case: try to get federated user info with a valid token returns the user information correctly.
//    @Test
//    public void testGetFederatedUser() throws FogbowManagerException, NoSuchAlgorithmException,
//            UnsupportedEncodingException {
//        //set up
//        Mockito.doReturn(this.FAKE_NAME).when(this.ldapFederationIdentityPlugin)
//                .ldapAuthenticate(Mockito.anyString(), Mockito.anyString());
//        String localMember = PropertiesHolder.getInstance().getProperty(ConfigurationConstants.LOCAL_MEMBER_ID);
//
//        //exercise
//        String federationTokenValue = this.ldapFederationIdentityPlugin.createTokenValue(userCredentials);
//        LdapToken ldapToken = (LdapToken) this.ldapFederationIdentityPlugin.createToken(federationTokenValue);
//
//        String split[] = federationTokenValue.split(TOKEN_VALUE_SEPARATOR);
//
//        //verify
//        Assert.assertEquals(ldapToken.getUserId(), FAKE_USER_ID);
//        Assert.assertEquals(ldapToken.getUserName(), FAKE_NAME);
//        Assert.assertEquals(ldapToken.getTokenProvider(), localMember);
//        Assert.assertEquals(ldapToken.getTokenValue(), federationTokenValue);
//        Assert.assertTrue(this.ldapAuthenticationPlugin.isAuthentic(ldapToken));
//    }

}
