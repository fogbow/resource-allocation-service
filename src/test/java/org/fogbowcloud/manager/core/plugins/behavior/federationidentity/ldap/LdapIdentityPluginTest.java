package org.fogbowcloud.manager.core.plugins.behavior.federationidentity.ldap;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.when;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.security.GeneralSecurityException;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.security.SignatureException;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;

import org.apache.commons.codec.binary.Base64;
import org.fogbowcloud.manager.core.exceptions.FogbowManagerException;
import org.fogbowcloud.manager.core.exceptions.InvalidCredentialsUserException;
import org.fogbowcloud.manager.core.exceptions.TokenValueCreationException;
import org.fogbowcloud.manager.core.exceptions.UnauthenticatedUserException;
import org.fogbowcloud.manager.core.exceptions.UnexpectedException;
import org.fogbowcloud.manager.core.models.tokens.FederationUser;
import org.fogbowcloud.manager.util.PropertiesUtil;
import org.fogbowcloud.manager.util.RSAUtil;
import org.json.JSONObject;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mockito;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;

import com.google.common.base.Charsets;

@RunWith(PowerMockRunner.class)
@PrepareForTest({PropertiesUtil.class, RSAUtil.class})
public class LdapIdentityPluginTest {
	private static final String IDENTITY_URL_KEY = "identity_url";
	private final String KEYSTONE_URL = "http://localhost:" + 3000;
    public static final String ACCESSID_SEPARATOR = "!#!";

	
	private final String MOCK_SIGNATURE = "mock_signature";
	private final String LDAP_URL_NOT_PROVIDED_ERROR_MESSAGE = "Ldap url is not provided in conf files.";

	private LdapIdentityPlugin identityPlugin;
	private Map<String, String> userCredentials;
	private String name;
	private String password;
	
	@Before
	public void setUp() throws InvalidKeyException, NoSuchAlgorithmException, SignatureException, UnsupportedEncodingException, IOException, GeneralSecurityException {
		Properties properties = Mockito.spy(Properties.class);
		
		PowerMockito.mockStatic(PropertiesUtil.class);
		when(PropertiesUtil.readProperties(Mockito.anyString())).thenReturn(properties);
		when(properties.getProperty(Mockito.anyString())).thenReturn(Mockito.anyString());
		
		this.identityPlugin = Mockito.spy(new LdapIdentityPlugin());
		
		properties.put(IDENTITY_URL_KEY, KEYSTONE_URL);
		doReturn(MOCK_SIGNATURE).when(identityPlugin).createSignature(Mockito.any(JSONObject.class));
		
		this.name = "ldapUser";
		this.password = "ldapUserPass";
		
		userCredentials = new HashMap<String, String>();
		userCredentials.put(LdapIdentityPlugin.CRED_USERNAME, name);
		userCredentials.put(LdapIdentityPlugin.CRED_PASSWORD, password);
		userCredentials.put(LdapIdentityPlugin.CRED_AUTH_URL, "ldapUrl");
		userCredentials.put(LdapIdentityPlugin.CRED_LDAP_BASE, "ldapBase");
		userCredentials.put(LdapIdentityPlugin.CRED_LDAP_ENCRYPT, "");
		userCredentials.put(LdapIdentityPlugin.CRED_PRIVATE_KEY, "private_key_path");
		userCredentials.put(LdapIdentityPlugin.CRED_PUBLIC_KEY, "public_key_path");
	}
	
	@SuppressWarnings("unchecked")
	@Test(expected = InvalidCredentialsUserException.class)
	public void testCreateTokenWithoutCredentals() throws UnauthenticatedUserException, TokenValueCreationException {
		this.identityPlugin.createFederationTokenValue(Mockito.anyMap());
	}
	
	
	@Test
	public void testLdapAuthenticateWithoutLdapUrl() throws Exception {
		try {
			this.identityPlugin.ldapAuthenticate(Mockito.anyString(), Mockito.anyString());
			fail("Code above must trhow a exception");
		} catch (FogbowManagerException e) {
			assertEquals(e.getMessage(), LDAP_URL_NOT_PROVIDED_ERROR_MESSAGE);
		}
	}
	
	
	@Test
	public void testCreateToken() throws Exception {
		String userName = "User Full Name";
		doReturn(userName).when(identityPlugin).ldapAuthenticate(Mockito.eq(name), Mockito.eq(password));
		
		String token = identityPlugin.createFederationTokenValue(userCredentials);
		String decodedAccessId = decodeAccessId(token);
		
		assertTrue(decodedAccessId.contains(name));
		assertTrue(decodedAccessId.contains(userName));
		assertTrue(decodedAccessId.contains(MOCK_SIGNATURE));
		
	}

	@Test(expected=InvalidCredentialsUserException.class)
	public void testCreateTokenWithInvalidUserName() throws Exception {
		doThrow(new Exception("Invalid User")).when(identityPlugin).ldapAuthenticate(Mockito.eq(name), Mockito.eq(password));
		
		identityPlugin.createFederationTokenValue(userCredentials);		
	}
	
	@Test
	public void testGetTokenInvalidAccessId() throws Exception {
		String userName = "User Full Name";
		
		doReturn(userName).when(identityPlugin).ldapAuthenticate(Mockito.eq(name), Mockito.eq(password));
		
		String tokenA = identityPlugin.createFederationTokenValue(userCredentials);
		
		assertFalse(this.identityPlugin.isValid(tokenA));
		
	}
	
	@Test(expected = UnauthenticatedUserException.class)
	public void testGetFederailsUserWithEmptyToken() throws UnauthenticatedUserException, UnexpectedException {
		this.identityPlugin.getFederationUser(Mockito.anyString());
	}
	
	@Test(expected = UnauthenticatedUserException.class)
	public void testGetFederailsUserWithInvalidToken() throws UnauthenticatedUserException, UnexpectedException {
		String invalidToken = "InvalidJsonCredentials" + ACCESSID_SEPARATOR + "InvalidJsonCredentialsHashed";
		
		byte[] bytes = invalidToken.getBytes();
		this.identityPlugin.getFederationUser(new String(Base64.encodeBase64(bytes)));
	}
	
	
	@Test
	public void testGetFederatedUser() throws UnauthenticatedUserException, UnexpectedException {
		String jsonCredentials = "{\n\t\"name\": \"user\",\n\t\"login\": \"login\"\n}";
		String token = jsonCredentials + ACCESSID_SEPARATOR + "fake-json-credentials-hashed";
		
		byte[] bytes = token.getBytes();
		
		doReturn(true).when(this.identityPlugin).verifySign(Mockito.anyString(), Mockito.anyString());
		FederationUser returnedUser =  this.identityPlugin.getFederationUser(new String(Base64.encodeBase64(bytes)));

        HashMap<String, String> attributes = new HashMap<String, String>();
        attributes.put(FederationUser.MANDATORY_NAME_ATTRIBUTE, "user");

        FederationUser user = new FederationUser("login", attributes);
        
        assertEquals(user, returnedUser);
	}
	
	private String decodeAccessId(String accessId){
		return new String(Base64.decodeBase64(accessId),
				Charsets.UTF_8);
	}

}
