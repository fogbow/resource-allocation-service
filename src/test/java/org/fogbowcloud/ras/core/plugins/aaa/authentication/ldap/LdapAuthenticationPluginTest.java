package org.fogbowcloud.ras.core.plugins.aaa.authentication.ldap;

import java.util.Date;
import java.util.HashMap;
import java.util.Map;

import org.fogbowcloud.ras.core.models.tokens.LdapToken;
import org.fogbowcloud.ras.core.plugins.aaa.identity.ldap.LdapIdentityPlugin;
import org.fogbowcloud.ras.core.plugins.aaa.tokengenerator.ldap.LdapTokenGeneratorPlugin;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;

public class LdapAuthenticationPluginTest {

    private LdapAuthenticationPlugin authenticationPlugin;
    private Map<String, String> userCredentials;
    private String name;
    private String password;
    private static final String FAKE_MEMBER_ID = "fake-member-id";

    @Before
    public void setUp() {
        this.authenticationPlugin = Mockito.spy(new LdapAuthenticationPlugin());

        this.name = "ldapUser";
        this.password = "ldapUserPass";

        this.userCredentials = new HashMap<String, String>();
        this.userCredentials.put(LdapTokenGeneratorPlugin.CRED_USERNAME, this.name);
        this.userCredentials.put(LdapTokenGeneratorPlugin.CRED_PASSWORD, this.password);
        this.userCredentials.put(LdapTokenGeneratorPlugin.CRED_AUTH_URL, "ldapUrl");
        this.userCredentials.put(LdapTokenGeneratorPlugin.CRED_LDAP_BASE, "ldapBase");
        this.userCredentials.put(LdapTokenGeneratorPlugin.CRED_LDAP_ENCRYPT, "");
        this.userCredentials.put(LdapTokenGeneratorPlugin.CRED_PRIVATE_KEY, "private_key_path");
        this.userCredentials.put(LdapTokenGeneratorPlugin.CRED_PUBLIC_KEY, "public_key_path");
    }

    //test case: check if isAuthentic returns false when the tokenValue is invalid (signature doesn't match).
    @Test
    public void testGetTokenExpiredTokenValue() throws Exception {
        //set up
        LdapIdentityPlugin identityPlugin = Mockito.spy(new LdapIdentityPlugin());
        LdapTokenGeneratorPlugin tokenGenerator = Mockito.spy(new LdapTokenGeneratorPlugin());
        Mockito.doReturn(this.name).when(tokenGenerator).ldapAuthenticate(Mockito.anyString(), Mockito.anyString());

        //exercise
        String tokenValue = tokenGenerator.createTokenValue(this.userCredentials);
        String split[] = tokenValue.split(LdapTokenGeneratorPlugin.TOKEN_VALUE_SEPARATOR);
        String newTokenValue = split[0] + LdapTokenGeneratorPlugin.TOKEN_VALUE_SEPARATOR + "another user id" +
                LdapTokenGeneratorPlugin.TOKEN_VALUE_SEPARATOR + split[2] +
                LdapTokenGeneratorPlugin.TOKEN_VALUE_SEPARATOR + split[3] +
                LdapTokenGeneratorPlugin.TOKEN_VALUE_SEPARATOR + split[4];
        LdapToken newToken = identityPlugin.createToken(newTokenValue);

        //verify
        Assert.assertFalse(this.authenticationPlugin.isAuthentic(FAKE_MEMBER_ID, newToken));
    }

    //test case: check if isAuthentic returns false when the tokenValue is expired.
    @Test
    public void testGetTokenInvalidTokenValue() throws Exception {
        //set up
        LdapIdentityPlugin identityPlugin = Mockito.spy(new LdapIdentityPlugin());
        LdapTokenGeneratorPlugin tokenGenerator = Mockito.spy(new LdapTokenGeneratorPlugin());
        Mockito.doReturn(this.name).when(tokenGenerator).ldapAuthenticate(Mockito.anyString(), Mockito.anyString());

        //exercise
        String tokenValue = tokenGenerator.createTokenValue(this.userCredentials);
        String split[] = tokenValue.split(LdapTokenGeneratorPlugin.TOKEN_VALUE_SEPARATOR);
        String newExpirationTime = Long.toString((new Date().getTime()));
        String newTokenValue = split[0] + LdapTokenGeneratorPlugin.TOKEN_VALUE_SEPARATOR + split[1] +
                LdapTokenGeneratorPlugin.TOKEN_VALUE_SEPARATOR + newExpirationTime
                + LdapTokenGeneratorPlugin.TOKEN_VALUE_SEPARATOR + split[3]
                + LdapTokenGeneratorPlugin.TOKEN_VALUE_SEPARATOR + split[4];
        LdapToken newToken = identityPlugin.createToken(newTokenValue);

        //verify
        Assert.assertFalse(this.authenticationPlugin.isAuthentic(FAKE_MEMBER_ID, newToken));
    }
}
