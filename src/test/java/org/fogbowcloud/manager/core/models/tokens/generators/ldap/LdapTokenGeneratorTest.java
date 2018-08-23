package org.fogbowcloud.manager.core.models.tokens.generators.ldap;

import org.fogbowcloud.manager.core.HomeDir;
import org.fogbowcloud.manager.core.PropertiesHolder;
import org.fogbowcloud.manager.core.constants.ConfigurationConstants;
import org.fogbowcloud.manager.core.exceptions.*;
import org.fogbowcloud.manager.core.models.tokens.LdapToken;
import org.fogbowcloud.manager.core.plugins.behavior.authentication.ldap.LdapAuthenticationPlugin;
import org.fogbowcloud.manager.core.plugins.behavior.identity.ldap.LdapFederationIdentityPlugin;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;

import java.util.HashMap;
import java.util.Map;

public class LdapTokenGeneratorTest {
    private static final String FAKE_NAME = "fake-name";
    private static final String FAKE_LOGIN = "fake-login";
    private static final String FAKE_PASSWORD = "fake-password";

    private LdapTokenGenerator ldapTokenGenerator;
    private LdapAuthenticationPlugin ldapAuthenticationPlugin;
    private LdapFederationIdentityPlugin ldapFederationIdentityPlugin;

    @Before
    public void setUp() {
        this.ldapTokenGenerator = Mockito.spy(new LdapTokenGenerator());
        this.ldapAuthenticationPlugin = new LdapAuthenticationPlugin();
        this.ldapFederationIdentityPlugin = new LdapFederationIdentityPlugin();
    }

    //test case: createTokenValue with valid credentials should generate a string with the appropriate values
    @Test
    public void testCreateTokenValueValidCredentials() throws InvalidParameterException, UnexpectedException,
            InvalidUserCredentialsException {
        //set up
        Map<String, String> userCredentials = new HashMap<String, String>();
        userCredentials.put(LdapTokenGenerator.CRED_USERNAME, FAKE_LOGIN);
        userCredentials.put(LdapTokenGenerator.CRED_PASSWORD, FAKE_PASSWORD);

        Mockito.doReturn(FAKE_NAME).when(ldapTokenGenerator).
                ldapAuthenticate(Mockito.eq(FAKE_LOGIN), Mockito.eq(FAKE_PASSWORD));

        //exercise
        String tokenValue = this.ldapTokenGenerator.createTokenValue(userCredentials);
        LdapToken token = this.ldapFederationIdentityPlugin.createToken(tokenValue);

        //verify
        String split[] = tokenValue.split(LdapTokenGenerator.TOKEN_VALUE_SEPARATOR);
        Assert.assertEquals(split.length,5);
        Assert.assertEquals(split[0], PropertiesHolder.getInstance().getProperty(ConfigurationConstants.LOCAL_MEMBER_ID));
        Assert.assertEquals(split[1], FAKE_LOGIN);
        Assert.assertEquals(split[2], FAKE_NAME);
        Assert.assertTrue(this.ldapAuthenticationPlugin.isAuthentic(token));
    }

    //test case: createTokenValue with invalid credentials should throw InvalidUserCredentialsException
    @Test (expected = InvalidUserCredentialsException.class)
    public void testCreateTokenValueInvalidCredentials() throws InvalidParameterException, UnexpectedException,
            InvalidUserCredentialsException {
        //set up
        Map<String, String> userCredentials = new HashMap<String, String>();
        userCredentials.put(LdapTokenGenerator.CRED_USERNAME, FAKE_LOGIN);
        userCredentials.put(LdapTokenGenerator.CRED_PASSWORD, FAKE_PASSWORD);

        Mockito.doThrow(new InvalidUserCredentialsException())
                .when(this.ldapTokenGenerator).ldapAuthenticate(Mockito.eq(FAKE_LOGIN), Mockito.eq(FAKE_PASSWORD));

        //exercise
        String tokenValue = this.ldapTokenGenerator.createTokenValue(userCredentials);
    }

    //test case: createTokenValue with incorrect credentials should throw InvalidParameterException
    @Test (expected = InvalidParameterException.class)
    public void testCreateTokenValueIncorrectCredentials() throws InvalidParameterException, UnexpectedException,
            InvalidUserCredentialsException {
        //set up
        Map<String, String> userCredentials = new HashMap<String, String>();
        userCredentials.put(LdapTokenGenerator.CRED_USERNAME, FAKE_LOGIN);
        userCredentials.put(LdapTokenGenerator.CRED_PASSWORD, FAKE_PASSWORD);

        Mockito.doThrow(new InvalidParameterException())
                .when(this.ldapTokenGenerator).ldapAuthenticate(Mockito.eq(FAKE_LOGIN), Mockito.eq(FAKE_PASSWORD));

        //exercise
        String tokenValue = this.ldapTokenGenerator.createTokenValue(userCredentials);
    }
}
