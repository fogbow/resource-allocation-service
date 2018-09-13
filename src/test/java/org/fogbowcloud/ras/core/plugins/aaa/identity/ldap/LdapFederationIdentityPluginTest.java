package org.fogbowcloud.ras.core.plugins.aaa.identity.ldap;

import org.fogbowcloud.ras.core.exceptions.InvalidParameterException;
import org.fogbowcloud.ras.core.models.tokens.LdapToken;
import org.fogbowcloud.ras.core.plugins.aaa.tokengenerator.ldap.LdapTokenGeneratorPlugin;
import org.fogbowcloud.ras.core.plugins.aaa.authentication.ldap.LdapAuthenticationPlugin;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;

import java.util.HashMap;
import java.util.Map;

public class LdapFederationIdentityPluginTest {

    private static final String FAKE_NAME = "fake-name";
    private static final String FAKE_USER_ID = "fake-user-id";
    private static final String password = "ldapUserPass";
    private static final String TOKEN_VALUE_SEPARATOR = "!#!";
    private LdapFederationIdentityPlugin ldapFederationIdentityPlugin;
    private LdapAuthenticationPlugin ldapAuthenticationPlugin;

    Map<String, String> userCredentials = new HashMap<String, String>();

    @Before
    public void setUp() {
        this.userCredentials.put(LdapTokenGeneratorPlugin.CRED_USERNAME, FAKE_USER_ID);
        this.userCredentials.put(LdapTokenGeneratorPlugin.CRED_PASSWORD, this.password);
        this.userCredentials.put(LdapTokenGeneratorPlugin.CRED_AUTH_URL, "ldapUrl");
        this.userCredentials.put(LdapTokenGeneratorPlugin.CRED_LDAP_BASE, "ldapBase");
        this.userCredentials.put(LdapTokenGeneratorPlugin.CRED_LDAP_ENCRYPT, "");
        this.userCredentials.put(LdapTokenGeneratorPlugin.CRED_PRIVATE_KEY, "private_key_path");
        this.userCredentials.put(LdapTokenGeneratorPlugin.CRED_PUBLIC_KEY, "public_key_path");
        this.ldapFederationIdentityPlugin = Mockito.spy(new LdapFederationIdentityPlugin());
        this.ldapAuthenticationPlugin = new LdapAuthenticationPlugin();
    }

    //test case: check if the token information is correct when creating a token with the correct token value.
    @Test
    public void testCreateToken() throws Exception {
        //set up
        LdapTokenGeneratorPlugin tokenGenerator = Mockito.spy(new LdapTokenGeneratorPlugin());
        Mockito.doReturn(this.FAKE_NAME).when(tokenGenerator).ldapAuthenticate(Mockito.anyString(), Mockito.anyString());


        //exercise
        String federationTokenValue = tokenGenerator.createTokenValue(userCredentials);
        LdapToken ldapToken = this.ldapFederationIdentityPlugin.createToken(federationTokenValue);

        String split[] = federationTokenValue.split(TOKEN_VALUE_SEPARATOR);

        //verify
        Assert.assertEquals(split[0], ldapToken.getTokenProvider());
        Assert.assertEquals(split[1], ldapToken.getUserId());
        Assert.assertEquals(split[2], ldapToken.getUserName());
        Assert.assertEquals(split[3], ldapToken.getExpirationTime());
        Assert.assertTrue(this.ldapAuthenticationPlugin.isAuthentic(ldapToken));
    }

    //test case: check if the token information is correct when creating a token with the correct token value.
    @Test(expected = InvalidParameterException.class)
    public void testCreateTokenIncorrectTokenValue() throws Exception {
        //exercise
        LdapToken ldapToken = this.ldapFederationIdentityPlugin.createToken("anything");
    }
}
