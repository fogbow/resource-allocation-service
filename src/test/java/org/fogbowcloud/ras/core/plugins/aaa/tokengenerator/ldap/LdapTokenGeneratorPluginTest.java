package org.fogbowcloud.ras.core.plugins.aaa.tokengenerator.ldap;

import java.util.HashMap;
import java.util.Map;

import org.fogbowcloud.ras.core.HomeDir;
import org.fogbowcloud.ras.core.PropertiesHolder;
import org.fogbowcloud.ras.core.constants.ConfigurationConstants;
import org.fogbowcloud.ras.core.exceptions.InvalidParameterException;
import org.fogbowcloud.ras.core.exceptions.InvalidUserCredentialsException;
import org.fogbowcloud.ras.core.exceptions.UnauthenticatedUserException;
import org.fogbowcloud.ras.core.exceptions.UnexpectedException;
import org.fogbowcloud.ras.core.models.tokens.LdapToken;
import org.fogbowcloud.ras.core.plugins.aaa.RASAuthenticationHolder;
import org.fogbowcloud.ras.core.plugins.aaa.authentication.ldap.LdapAuthenticationPlugin;
import org.fogbowcloud.ras.core.plugins.aaa.identity.ldap.LdapIdentityPlugin;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.BDDMockito;
import org.mockito.Mockito;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.core.classloader.annotations.PowerMockIgnore;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;

@PowerMockIgnore({"javax.management.*"})
@PrepareForTest({ RASAuthenticationHolder.class })
@RunWith(PowerMockRunner.class)
public class LdapTokenGeneratorPluginTest {
	
    private static final String FAKE_NAME = "fake-name";
    private static final String FAKE_LOGIN = "fake-login";
    private static final String FAKE_PASSWORD = "fake-password";

    private LdapTokenGeneratorPlugin ldapTokenGenerator;
    private LdapAuthenticationPlugin ldapAuthenticationPlugin;
    private LdapIdentityPlugin ldapIdentityPlugin;
    private String localMemberId;
	private RASAuthenticationHolder genericSignatureAuthenticationHolder;

    @Before
    public void setUp() {
    	PowerMockito.mockStatic(RASAuthenticationHolder.class);
    	this.genericSignatureAuthenticationHolder = Mockito.spy(new RASAuthenticationHolder());
    	BDDMockito.given(RASAuthenticationHolder.getInstance()).willReturn(this.genericSignatureAuthenticationHolder);
    	String path = HomeDir.getPath();
        this.ldapTokenGenerator = Mockito.spy(new LdapTokenGeneratorPlugin(path + "ldap-token-generator-plugin.conf"));
        this.ldapAuthenticationPlugin = new LdapAuthenticationPlugin();
        this.ldapIdentityPlugin = new LdapIdentityPlugin();
        this.localMemberId = PropertiesHolder.getInstance().getProperty(ConfigurationConstants.LOCAL_MEMBER_ID);        
    }

    //test case: createTokenValue with valid credentials should generate allocationAllowableValues string with the appropriate values
    @Test
    public void testCreateTokenValueValidCredentials() throws InvalidParameterException, UnexpectedException,
            InvalidUserCredentialsException, UnauthenticatedUserException {
        //set up
        Map<String, String> userCredentials = new HashMap<String, String>();
        userCredentials.put(LdapTokenGeneratorPlugin.CRED_USERNAME, FAKE_LOGIN);
        userCredentials.put(LdapTokenGeneratorPlugin.CRED_PASSWORD, FAKE_PASSWORD);

        Mockito.doReturn(FAKE_NAME).when(this.ldapTokenGenerator).
                ldapAuthenticate(Mockito.eq(FAKE_LOGIN), Mockito.eq(FAKE_PASSWORD));

		String timeInPass =  String.valueOf(
				System.currentTimeMillis() - RASAuthenticationHolder.EXPIRATION_INTERVAL * 10);
		Mockito.doReturn(timeInPass).when(this.genericSignatureAuthenticationHolder).generateExpirationTime();
        
        //exercise
        String tokenValue = this.ldapTokenGenerator.createTokenValue(userCredentials);
        LdapToken token = this.ldapIdentityPlugin.createToken(tokenValue);

        //verify
        String split[] = tokenValue.split(LdapTokenGeneratorPlugin.TOKEN_VALUE_SEPARATOR);
        Assert.assertEquals(split.length, LdapTokenGeneratorPlugin.LDAP_TOKEN_NUMBER_OF_FIELDS);
        Assert.assertEquals(split[0], PropertiesHolder.getInstance().getProperty(ConfigurationConstants.LOCAL_MEMBER_ID));
        Assert.assertEquals(split[1], FAKE_LOGIN);
        Assert.assertEquals(split[2], FAKE_NAME);
        Assert.assertTrue(this.ldapAuthenticationPlugin.isAuthentic(this.localMemberId, token));
    }

    //test case: createTokenValue with invalid credentials should throw InvalidUserCredentialsException
    @Test(expected = InvalidUserCredentialsException.class)
    public void testCreateTokenValueInvalidCredentials() throws InvalidParameterException, UnexpectedException,
            InvalidUserCredentialsException, UnauthenticatedUserException {
        //set up
        Map<String, String> userCredentials = new HashMap<String, String>();
        userCredentials.put(LdapTokenGeneratorPlugin.CRED_USERNAME, FAKE_LOGIN);
        userCredentials.put(LdapTokenGeneratorPlugin.CRED_PASSWORD, FAKE_PASSWORD);

        Mockito.doThrow(new InvalidUserCredentialsException())
                .when(this.ldapTokenGenerator).ldapAuthenticate(Mockito.eq(FAKE_LOGIN), Mockito.eq(FAKE_PASSWORD));

        //exercise
        this.ldapTokenGenerator.createTokenValue(userCredentials);
    }

    //test case: createTokenValue with incorrect credentials should throw InvalidParameterException
    @Test(expected = InvalidParameterException.class)
    public void testCreateTokenValueIncorrectCredentials() throws InvalidParameterException, UnexpectedException,
            InvalidUserCredentialsException, UnauthenticatedUserException {
        //set up
        Map<String, String> userCredentials = new HashMap<String, String>();
        userCredentials.put(LdapTokenGeneratorPlugin.CRED_USERNAME, FAKE_LOGIN);
        userCredentials.put(LdapTokenGeneratorPlugin.CRED_PASSWORD, FAKE_PASSWORD);

        Mockito.doThrow(new InvalidParameterException())
                .when(this.ldapTokenGenerator).ldapAuthenticate(Mockito.eq(FAKE_LOGIN), Mockito.eq(FAKE_PASSWORD));

        //exercise
        this.ldapTokenGenerator.createTokenValue(userCredentials);
    }
}
