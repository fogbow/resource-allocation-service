package org.fogbowcloud.manager.core.plugins.behavior.federationidentity.ldap;

import java.nio.charset.StandardCharsets;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;
import java.util.concurrent.TimeUnit;

import org.apache.commons.codec.binary.Base64;
import org.fogbowcloud.manager.core.HomeDir;
import org.fogbowcloud.manager.core.PropertiesHolder;
import org.fogbowcloud.manager.core.exceptions.FogbowManagerException;
import org.fogbowcloud.manager.core.exceptions.UnauthenticatedUserException;
import org.json.JSONObject;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;

public class LdapLocalIdentityPluginTest {

    private static final String IDENTITY_URL_KEY = "identity_url";
    private final String KEYSTONE_URL = "http://localhost:0000";

    private final String MOCK_SIGNATURE = "mock_signature";

    private LdapIdentityPlugin ldapStoneIdentity;
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
        this.ldapStoneIdentity = Mockito.spy(new LdapIdentityPlugin());
        Mockito.doReturn(this.MOCK_SIGNATURE)
                .when(this.ldapStoneIdentity)
                .createSignature(Mockito.any(JSONObject.class));

        this.userCredentials.put(LdapIdentityPlugin.CRED_USERNAME, this.name);
        this.userCredentials.put(LdapIdentityPlugin.CRED_PASSWORD, this.password);
        this.userCredentials.put(LdapIdentityPlugin.CRED_AUTH_URL, "ldapUrl");
        this.userCredentials.put(LdapIdentityPlugin.CRED_LDAP_BASE, "ldapBase");
        this.userCredentials.put(LdapIdentityPlugin.CRED_LDAP_ENCRYPT, "");
        this.userCredentials.put(LdapIdentityPlugin.CRED_PRIVATE_KEY, "private_key_path");
        this.userCredentials.put(LdapIdentityPlugin.CRED_PUBLIC_KEY, "public_key_path");
    }
    
    //test case: check if the access id information is correct when the user credentials are correct.
    @Test
    public void testCreateToken() throws Exception {
    	//set up
        Mockito.doReturn(this.userName)
                .when(this.ldapStoneIdentity)
                .ldapAuthenticate(Mockito.eq(this.name), Mockito.eq(this.password));
        
        //exercise
        String federationTokenValue = this.ldapStoneIdentity.createFederationTokenValue(userCredentials);
        
        //verify
        String decodedAccessId = decodeAccessId(federationTokenValue);
        Assert.assertTrue(decodedAccessId.contains(this.name));
        Assert.assertTrue(decodedAccessId.contains(this.userName));
        Assert.assertTrue(decodedAccessId.contains(this.MOCK_SIGNATURE));
    }
    
    //test case: check if createFederationTokenValue throws UnauthenticatedUserException when the user credentials are invalid.
    @Test (expected = UnauthenticatedUserException.class)
    public void testCreateTokenFail() throws Exception {
    	//set up
        Mockito.doThrow(new FogbowManagerException("Invalid User"))
                .when(this.ldapStoneIdentity)
                .ldapAuthenticate(Mockito.eq(name), Mockito.eq(this.password));
        
        //exercise/verify
        this.ldapStoneIdentity.createFederationTokenValue(this.userCredentials);
    }
    
    //test case: check if isValid returns false when the accessId is invalid.
    @Test
    public void testGetTokenInvalidAccessId() throws Exception {
    	//set up
        Mockito.doReturn(this.userName)
                .when(this.ldapStoneIdentity)
                .ldapAuthenticate(Mockito.eq(this.name), Mockito.eq(this.password));
        String tokenValueA = this.ldapStoneIdentity.createFederationTokenValue(this.userCredentials);
        String decodedAccessId = decodeAccessId(tokenValueA);
        String split[] = decodedAccessId.split(LdapIdentityPlugin.ACCESSID_SEPARATOR);
        String signature = split[1];
        String newAccessId =
                "{name:\"nome\", expirationDate:\"" + (new Date(new Date().getTime() + TimeUnit.DAYS.toMillis(365))).getTime() + "\"}"
                        + LdapIdentityPlugin.ACCESSID_SEPARATOR
                        + signature;
        newAccessId =
                new String(
                        Base64.encodeBase64(
                                newAccessId.getBytes(StandardCharsets.UTF_8), false, false),
                        StandardCharsets.UTF_8);

        //exercise/verify
        Assert.assertFalse(this.ldapStoneIdentity.isValid(newAccessId));
    }

    public String decodeAccessId(String accessId) {
        return new String(Base64.decodeBase64(accessId), StandardCharsets.UTF_8);
    }
}
