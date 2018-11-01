package org.fogbowcloud.ras.core.plugins.aaa.identity.ldap;

import org.fogbowcloud.ras.core.PropertiesHolder;
import org.fogbowcloud.ras.core.constants.ConfigurationConstants;
import org.fogbowcloud.ras.core.exceptions.InvalidParameterException;
import org.fogbowcloud.ras.core.models.tokens.LdapToken;
import org.fogbowcloud.ras.core.plugins.aaa.authentication.RASAuthenticationHolder;
import org.fogbowcloud.ras.core.plugins.aaa.authentication.ldap.LdapAuthenticationPlugin;
import org.fogbowcloud.ras.core.plugins.aaa.tokengenerator.ldap.LdapTokenGeneratorPlugin;
import org.fogbowcloud.ras.util.RSAUtil;
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

import java.security.PublicKey;
import java.util.HashMap;
import java.util.Map;

@PowerMockIgnore({"javax.management.*"})
@PrepareForTest({ RSAUtil.class, RASAuthenticationHolder.class })
@RunWith(PowerMockRunner.class)
public class LdapIdentityPluginTest {

    private static final String FAKE_NAME = "fake-name";
    private static final String FAKE_USER_ID = "fake-user-id";
    private static final String PASSWORD = "ldapUserPass";
    private static final String TOKEN_VALUE_SEPARATOR = "!#!";
    private LdapIdentityPlugin ldapIdentityPlugin;
    private LdapAuthenticationPlugin ldapAuthenticationPlugin;
    private String localMemberId;

    Map<String, String> userCredentials = new HashMap<String, String>();
	private RASAuthenticationHolder genericSignatureAuthenticationHolder;

    @Before
    public void setUp() {
        this.userCredentials.put(LdapTokenGeneratorPlugin.CRED_USERNAME, FAKE_USER_ID);
        this.userCredentials.put(LdapTokenGeneratorPlugin.CRED_PASSWORD, PASSWORD);
        this.userCredentials.put(LdapTokenGeneratorPlugin.CRED_AUTH_URL, "ldapUrl");
        this.userCredentials.put(LdapTokenGeneratorPlugin.CRED_LDAP_BASE, "ldapBase");
        this.userCredentials.put(LdapTokenGeneratorPlugin.CRED_LDAP_ENCRYPT, "");
        this.userCredentials.put(LdapTokenGeneratorPlugin.CRED_PRIVATE_KEY, "private_key_path");
        this.userCredentials.put(LdapTokenGeneratorPlugin.CRED_PUBLIC_KEY, "public_key_path");
        this.ldapIdentityPlugin = Mockito.spy(new LdapIdentityPlugin());
        this.ldapAuthenticationPlugin = new LdapAuthenticationPlugin();
        this.localMemberId = PropertiesHolder.getInstance().getProperty(ConfigurationConstants.LOCAL_MEMBER_ID);
                
		PowerMockito.mockStatic(RASAuthenticationHolder.class);
		this.genericSignatureAuthenticationHolder = Mockito.mock(RASAuthenticationHolder.class);
		BDDMockito.given(RASAuthenticationHolder.getInstance()).willReturn(this.genericSignatureAuthenticationHolder);
    }

    //test case: check if the token information is correct when creating allocationAllowableValues token with the correct token value.
    @Test
    public void testCreateToken() throws Exception {
        //set up
        LdapTokenGeneratorPlugin tokenGenerator = Mockito.spy(new LdapTokenGeneratorPlugin());
        Mockito.doReturn(FAKE_NAME).when(tokenGenerator).ldapAuthenticate(Mockito.anyString(), Mockito.anyString());       

		PowerMockito.mockStatic(RSAUtil.class);
		BDDMockito.given(RSAUtil.verify(Mockito.any(PublicKey.class), Mockito.anyString(), Mockito.anyString()))
					.willReturn(true);
		
		String timeInPass =  String.valueOf(
				System.currentTimeMillis() - RASAuthenticationHolder.EXPIRATION_INTERVAL * 10);
		Mockito.doReturn(timeInPass).when(this.genericSignatureAuthenticationHolder).generateExpirationTime();		
        
        //exercise
        String federationTokenValue = tokenGenerator.createTokenValue(this.userCredentials);
        LdapToken ldapToken = this.ldapIdentityPlugin.createToken(federationTokenValue);

        String split[] = federationTokenValue.split(TOKEN_VALUE_SEPARATOR);

        //verify
        Assert.assertEquals(split[0], ldapToken.getTokenProvider());
        Assert.assertEquals(split[1], ldapToken.getUserId());
        Assert.assertEquals(split[2], ldapToken.getUserName());
        Assert.assertEquals(split[3], String.valueOf(ldapToken.getTimestamp()));
        Assert.assertTrue(this.ldapAuthenticationPlugin.isAuthentic(this.localMemberId, ldapToken));
    }

    //test case: check if the token information is correct when creating allocationAllowableValues token with the correct token value.
    @Test(expected = InvalidParameterException.class)
    public void testCreateTokenIncorrectTokenValue() throws Exception {
        //exercise
        this.ldapIdentityPlugin.createToken("anything");
    }
}
