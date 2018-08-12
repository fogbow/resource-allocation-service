package org.fogbowcloud.manager.core.plugins.behavior.authentication.ldap;

import java.util.Date;
import java.util.HashMap;
import java.util.Map;

import org.fogbowcloud.manager.core.HomeDir;
import org.fogbowcloud.manager.core.exceptions.UnauthenticatedUserException;
import org.fogbowcloud.manager.core.models.tokens.FederationUserToken;
import org.fogbowcloud.manager.core.plugins.behavior.identity.ldap.LdapFederationIdentityPlugin;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;

public class LdapAuthenticationPluginTest {

    public static final String TOKEN_VALUE_SEPARATOR = "!#!";

    private LdapAuthenticationPlugin authenticationPlugin;
    private Map<String, String> userCredentials;
    private String name;
    private String password;

    @Before
    public void setUp() {
        HomeDir homeDir = HomeDir.getInstance();
        homeDir.setPath("src/test/resources/private");

        this.authenticationPlugin = new LdapAuthenticationPlugin();

        this.name = "ldapUser";
        this.password = "ldapUserPass";

        this.userCredentials = new HashMap<String, String>();
        this.userCredentials.put(LdapAuthenticationPlugin.CRED_USERNAME, this.name);
        this.userCredentials.put(LdapAuthenticationPlugin.CRED_PASSWORD, this.password);
        this.userCredentials.put(LdapAuthenticationPlugin.CRED_AUTH_URL, "ldapUrl");
        this.userCredentials.put(LdapAuthenticationPlugin.CRED_LDAP_BASE, "ldapBase");
        this.userCredentials.put(LdapAuthenticationPlugin.CRED_LDAP_ENCRYPT, "");
        this.userCredentials.put(LdapAuthenticationPlugin.CRED_PRIVATE_KEY, "private_key_path");
        this.userCredentials.put(LdapAuthenticationPlugin.CRED_PUBLIC_KEY, "public_key_path");
    }

    //test case: check if isAuthentic returns true when the tokenValue is not expired.
    @Test
    public void testGetTokenValidTokenValue() throws Exception {
        //set up
        LdapFederationIdentityPlugin tokenGenerator = Mockito.spy(new LdapFederationIdentityPlugin());
        Mockito.doReturn(this.name).when(tokenGenerator).ldapAuthenticate(Mockito.anyString(), Mockito.anyString());

        //exercise
        String tokenValue = tokenGenerator.createTokenValue(this.userCredentials);
        FederationUserToken token = tokenGenerator.createToken(tokenValue);

        //verify
        Assert.assertTrue(this.authenticationPlugin.isAuthentic(token));
    }

    //test case: check if isAuthentic returns false when the tokenValue is invalid (signature doesn't match).
    @Test (expected = UnauthenticatedUserException.class)
    public void testGetTokenExpiredTokenValue() throws Exception {
        //set up
        LdapFederationIdentityPlugin tokenGenerator = Mockito.spy(new LdapFederationIdentityPlugin());
        Mockito.doReturn(this.name).when(tokenGenerator).ldapAuthenticate(Mockito.anyString(), Mockito.anyString());

        //exercise
        String tokenValue = tokenGenerator.createTokenValue(this.userCredentials);
        String split[] = tokenValue.split(TOKEN_VALUE_SEPARATOR);
        String newTokenValue = "another user id" + TOKEN_VALUE_SEPARATOR + split[1] + TOKEN_VALUE_SEPARATOR + split[2]
                + TOKEN_VALUE_SEPARATOR + split[3];
        FederationUserToken newToken = tokenGenerator.createToken(newTokenValue);

        //verify
        Assert.assertFalse(this.authenticationPlugin.isAuthentic(newToken));
    }

    //test case: check if isAuthentic returns false when the tokenValue is expired.
    @Test
    public void testGetTokenInvalidTokenValue() throws Exception {
        //set up
        LdapFederationIdentityPlugin tokenGenerator = Mockito.spy(new LdapFederationIdentityPlugin());
        Mockito.doReturn(this.name).when(tokenGenerator).ldapAuthenticate(Mockito.anyString(), Mockito.anyString());
        Mockito.doReturn(true).when(tokenGenerator).verifySign(Mockito.anyString(), Mockito.anyString());

        //exercise
        String tokenValue = tokenGenerator.createTokenValue(this.userCredentials);
        String split[] = tokenValue.split(TOKEN_VALUE_SEPARATOR);
        String newExpirationTime = new Long((new Date().getTime())).toString();
        String newTokenValue = split[0] + TOKEN_VALUE_SEPARATOR + split[1] + TOKEN_VALUE_SEPARATOR + newExpirationTime
                + TOKEN_VALUE_SEPARATOR + split[3];
        FederationUserToken newToken = tokenGenerator.createToken(newTokenValue);

        //verify
        Assert.assertFalse(this.authenticationPlugin.isAuthentic(newToken));
    }
}
