package org.fogbowcloud.manager.core.plugins.behavior.authentication.ldap;

import java.util.Date;
import java.util.HashMap;
import java.util.Map;

import org.fogbowcloud.manager.core.HomeDir;
import org.fogbowcloud.manager.core.exceptions.UnauthenticatedUserException;
import org.fogbowcloud.manager.core.models.tokens.FederationUserToken;
import org.fogbowcloud.manager.core.models.tokens.generators.ldap.LdapTokenGenerator;
import org.fogbowcloud.manager.core.plugins.behavior.identity.ldap.LdapFederationIdentityPlugin;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;

public class LdapAuthenticationPluginTest {

    private LdapAuthenticationPlugin authenticationPlugin;
    private Map<String, String> userCredentials;
    private String name;
    private String password;

    @Before
    public void setUp() {
        this.authenticationPlugin = Mockito.spy(new LdapAuthenticationPlugin());

        this.name = "ldapUser";
        this.password = "ldapUserPass";

        this.userCredentials = new HashMap<String, String>();
        this.userCredentials.put(LdapTokenGenerator.CRED_USERNAME, this.name);
        this.userCredentials.put(LdapTokenGenerator.CRED_PASSWORD, this.password);
        this.userCredentials.put(LdapTokenGenerator.CRED_AUTH_URL, "ldapUrl");
        this.userCredentials.put(LdapTokenGenerator.CRED_LDAP_BASE, "ldapBase");
        this.userCredentials.put(LdapTokenGenerator.CRED_LDAP_ENCRYPT, "");
        this.userCredentials.put(LdapTokenGenerator.CRED_PRIVATE_KEY, "private_key_path");
        this.userCredentials.put(LdapTokenGenerator.CRED_PUBLIC_KEY, "public_key_path");
    }

    //test case: check if isAuthentic returns true when the tokenValue is not expired.
    @Test
    public void testGetTokenValidTokenValue() throws Exception {
        //set up
        LdapFederationIdentityPlugin identityPlugin = Mockito.spy(new LdapFederationIdentityPlugin());
        Mockito.doReturn(true).when(identityPlugin).verifySign(Mockito.anyString(), Mockito.anyString());
        LdapTokenGenerator tokenGenerator = Mockito.spy(new LdapTokenGenerator());
        Mockito.doReturn(this.name).when(tokenGenerator).ldapAuthenticate(Mockito.anyString(), Mockito.anyString());
        Mockito.doReturn(true).when(this.authenticationPlugin).verifySign(Mockito.anyString(),
                Mockito.anyString());

        //exercise
        String tokenValue = tokenGenerator.createTokenValue(this.userCredentials);
        FederationUserToken token = identityPlugin.createToken(tokenValue);

        //verify
        Assert.assertTrue(this.authenticationPlugin.isAuthentic(token));
    }

    //test case: check if isAuthentic returns false when the tokenValue is invalid (signature doesn't match).
    @Test
    public void testGetTokenExpiredTokenValue() throws Exception {
        //set up
        LdapFederationIdentityPlugin identityPlugin = Mockito.spy(new LdapFederationIdentityPlugin());
        Mockito.doReturn(true).when(identityPlugin).verifySign(Mockito.anyString(), Mockito.anyString());
        LdapTokenGenerator tokenGenerator = Mockito.spy(new LdapTokenGenerator());
        Mockito.doReturn(this.name).when(tokenGenerator).ldapAuthenticate(Mockito.anyString(), Mockito.anyString());

        //exercise
        String tokenValue = tokenGenerator.createTokenValue(this.userCredentials);
        String split[] = tokenValue.split(LdapTokenGenerator.TOKEN_VALUE_SEPARATOR);
        String newTokenValue = split[0] + LdapTokenGenerator.TOKEN_VALUE_SEPARATOR + "another user id" +
                LdapTokenGenerator.TOKEN_VALUE_SEPARATOR + split[2] +
                LdapTokenGenerator.TOKEN_VALUE_SEPARATOR + split[3] +
                LdapTokenGenerator.TOKEN_VALUE_SEPARATOR + split[4];
        FederationUserToken newToken = identityPlugin.createToken(newTokenValue);

        //verify
        Assert.assertFalse(this.authenticationPlugin.isAuthentic(newToken));
    }

    //test case: check if isAuthentic returns false when the tokenValue is expired.
    @Test
    public void testGetTokenInvalidTokenValue() throws Exception {
        //set up
        LdapFederationIdentityPlugin identityPlugin = Mockito.spy(new LdapFederationIdentityPlugin());
        Mockito.doReturn(true).when(identityPlugin).verifySign(Mockito.anyString(), Mockito.anyString());
        LdapTokenGenerator tokenGenerator = Mockito.spy(new LdapTokenGenerator());
        Mockito.doReturn(this.name).when(tokenGenerator).ldapAuthenticate(Mockito.anyString(), Mockito.anyString());

        //exercise
        String tokenValue = tokenGenerator.createTokenValue(this.userCredentials);
        String split[] = tokenValue.split(LdapTokenGenerator.TOKEN_VALUE_SEPARATOR);
        String newExpirationTime = Long.toString((new Date().getTime()));
        String newTokenValue = split[0] + LdapTokenGenerator.TOKEN_VALUE_SEPARATOR + split[1] +
                LdapTokenGenerator.TOKEN_VALUE_SEPARATOR + newExpirationTime
                + LdapTokenGenerator.TOKEN_VALUE_SEPARATOR + split[3]
                + LdapTokenGenerator.TOKEN_VALUE_SEPARATOR + split[4];
        FederationUserToken newToken = identityPlugin.createToken(newTokenValue);

        //verify
        Assert.assertFalse(this.authenticationPlugin.isAuthentic(newToken));
    }
}
