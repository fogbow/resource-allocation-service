package org.fogbowcloud.manager.core.plugins.identity.ldap;

import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;

import org.apache.commons.codec.binary.Base64;
import org.fogbowcloud.manager.core.models.token.Token;
import org.json.JSONObject;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;
import org.springframework.http.HttpStatus;

public class TestLdapIdentityPlugin {
	
	private static final String IDENTITY_URL_KEY = "identity_url";
	private final String KEYSTONE_URL = "http://localhost:0000";

	private final String MOCK_SIGNATURE = "mock_signature";

	private LdapIdentityPlugin ldapStoneIdentity;

	@Before
	public void setUp() throws Exception {

		Properties properties = new Properties();
		properties.put(IDENTITY_URL_KEY, KEYSTONE_URL);

		this.ldapStoneIdentity = Mockito.spy(new LdapIdentityPlugin(properties));
		Mockito.doReturn(MOCK_SIGNATURE).when(ldapStoneIdentity).createSignature(Mockito.any(JSONObject.class));
		
	}

	@Test
	public void testCreateToken() throws Exception {

		String name = "ldapUser";
		String password = "ldapUserPass";
		String userName = "User Full Name";

		Map<String, String> userCredentials = new HashMap<String, String>();
		userCredentials.put(LdapIdentityPlugin.CRED_USERNAME, name);
		userCredentials.put(LdapIdentityPlugin.CRED_PASSWORD, password);
		userCredentials.put(LdapIdentityPlugin.CRED_AUTH_URL, "ldapUrl");
		userCredentials.put(LdapIdentityPlugin.CRED_LDAP_BASE, "ldapBase");
		userCredentials.put(LdapIdentityPlugin.CRED_LDAP_ENCRYPT, "");
		userCredentials.put(LdapIdentityPlugin.CRED_PRIVATE_KEY, "private_key_path");
		userCredentials.put(LdapIdentityPlugin.CRED_PUBLIC_KEY, "public_key_path");

		Mockito.doReturn(userName).when(ldapStoneIdentity).ldapAuthenticate(Mockito.eq(name), Mockito.eq(password));

		Token token = ldapStoneIdentity.createToken(userCredentials);

		String decodedAccessId = decodeAccessId(token.getAccessId());

		Assert.assertTrue(decodedAccessId.contains(name));
		Assert.assertTrue(decodedAccessId.contains(userName));
		Assert.assertTrue(decodedAccessId.contains(MOCK_SIGNATURE));

	}

	@Test
	public void testGetToken() throws Exception {

		String login = "ldapUser";
		String password = "ldapUserPass";
		String userName = "User Full Name";

		Map<String, String> userCredentials = new HashMap<String, String>();
		userCredentials.put(LdapIdentityPlugin.CRED_USERNAME, login);
		userCredentials.put(LdapIdentityPlugin.CRED_PASSWORD, password);
		userCredentials.put(LdapIdentityPlugin.CRED_AUTH_URL, "ldapUrl");
		userCredentials.put(LdapIdentityPlugin.CRED_LDAP_BASE, "ldapBase");
		userCredentials.put(LdapIdentityPlugin.CRED_LDAP_ENCRYPT, "");
		userCredentials.put(LdapIdentityPlugin.CRED_PRIVATE_KEY, "private_key_path");
		userCredentials.put(LdapIdentityPlugin.CRED_PUBLIC_KEY, "public_key_path");

		Mockito.doReturn(userName).when(ldapStoneIdentity).ldapAuthenticate(Mockito.eq(login), Mockito.eq(password));

		Token tokenA = ldapStoneIdentity.createToken(userCredentials);

		String decodedAccessId = decodeAccessId(tokenA.getAccessId());

		String split[] = decodedAccessId.split(LdapIdentityPlugin.ACCESSID_SEPARATOR);
		String tokenMessage = split[0];
		String signature = split[1];

		Mockito.doReturn(true).when(ldapStoneIdentity).verifySign(Mockito.eq(tokenMessage), Mockito.eq(signature));
		Token tokenB = ldapStoneIdentity.getToken(tokenA.getAccessId());

		Assert.assertEquals(tokenA.getAccessId(), tokenB.getAccessId());
		Assert.assertEquals(login, tokenA.getUser().getId());
		Assert.assertEquals(userName, tokenA.getUser().getName());
		Assert.assertEquals(login, tokenB.getUser().getId());
		Assert.assertEquals(userName, tokenB.getUser().getName());
		Assert.assertEquals(tokenA.getExpirationDate(), tokenB.getExpirationDate());

	}

	@Test
	public void testCreateTokenFail() throws Exception {

		String name = "ldapUser";
		String password = "ldapUserPass";

		Map<String, String> userCredentials = new HashMap<String, String>();
		userCredentials.put(LdapIdentityPlugin.CRED_USERNAME, name);
		userCredentials.put(LdapIdentityPlugin.CRED_PASSWORD, password);
		userCredentials.put(LdapIdentityPlugin.CRED_AUTH_URL, "ldapUrl");
		userCredentials.put(LdapIdentityPlugin.CRED_LDAP_BASE, "ldapBase");
		userCredentials.put(LdapIdentityPlugin.CRED_LDAP_ENCRYPT, "");
		userCredentials.put(LdapIdentityPlugin.CRED_PRIVATE_KEY, "private_key_path");
		userCredentials.put(LdapIdentityPlugin.CRED_PUBLIC_KEY, "public_key_path");

		Mockito.doThrow(new Exception("Invalid User")).when(ldapStoneIdentity).ldapAuthenticate(Mockito.eq(name),
				Mockito.eq(password));
		try {

			ldapStoneIdentity.createToken(userCredentials);
			Assert.fail();
		} catch (Exception e) {
			Assert.assertEquals(HttpStatus.UNAUTHORIZED.toString(), e.getMessage());
		}

	}

	@Test(expected = Exception.class)
	public void testGetTokenInvalidAccessId() throws Exception {

		String name = "ldapUser";
		String password = "ldapUserPass";
		String userName = "User Full Name";

		Map<String, String> userCredentials = new HashMap<String, String>();
		userCredentials.put(LdapIdentityPlugin.CRED_USERNAME, name);
		userCredentials.put(LdapIdentityPlugin.CRED_PASSWORD, password);
		userCredentials.put(LdapIdentityPlugin.CRED_AUTH_URL, "ldapUrl");
		userCredentials.put(LdapIdentityPlugin.CRED_LDAP_BASE, "ldapBase");
		userCredentials.put(LdapIdentityPlugin.CRED_LDAP_ENCRYPT, "");
		userCredentials.put(LdapIdentityPlugin.CRED_PRIVATE_KEY, "private_key_path");
		userCredentials.put(LdapIdentityPlugin.CRED_PUBLIC_KEY, "public_key_path");

		Mockito.doReturn(userName).when(ldapStoneIdentity).ldapAuthenticate(Mockito.eq(name), Mockito.eq(password));

		Token tokenA = ldapStoneIdentity.createToken(userCredentials);

		String decodedAccessId = decodeAccessId(tokenA.getAccessId());

		String split[] = decodedAccessId.split(LdapIdentityPlugin.ACCESSID_SEPARATOR);
		String tokenMessage = split[0];
		String signature = split[1];

		String newAccessId = "{name:\"nome\", expirationDate:\"123421\"}" + LdapIdentityPlugin.ACCESSID_SEPARATOR
				+ signature;

		newAccessId = new String(Base64.encodeBase64(newAccessId.getBytes(StandardCharsets.UTF_8), false, false),
				StandardCharsets.UTF_8);

		Mockito.doReturn(true).when(ldapStoneIdentity).verifySign(Mockito.eq(tokenMessage), Mockito.eq(signature));
		Mockito.doReturn(false).when(ldapStoneIdentity).verifySign(Mockito.eq(newAccessId), Mockito.eq(signature));

		ldapStoneIdentity.getToken(newAccessId);
	}

	public String decodeAccessId(String accessId) {
		return new String(Base64.decodeBase64(accessId), StandardCharsets.UTF_8);
	}
}
