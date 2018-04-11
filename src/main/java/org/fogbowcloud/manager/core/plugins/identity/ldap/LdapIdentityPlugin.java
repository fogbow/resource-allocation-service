package org.fogbowcloud.manager.core.plugins.identity.ldap;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.security.GeneralSecurityException;
import java.security.InvalidKeyException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SignatureException;
import java.security.interfaces.RSAPrivateKey;
import java.security.interfaces.RSAPublicKey;
import java.util.Date;
import java.util.HashMap;
import java.util.Hashtable;
import java.util.Map;
import java.util.Properties;
import java.util.concurrent.TimeUnit;

import javax.naming.Context;
import javax.naming.NamingEnumeration;
import javax.naming.NamingException;
import javax.naming.directory.DirContext;
import javax.naming.directory.InitialDirContext;
import javax.naming.directory.SearchControls;
import javax.naming.directory.SearchResult;

import org.apache.commons.codec.Charsets;
import org.apache.commons.codec.binary.Base64;
import org.apache.log4j.Logger;
import org.fogbowcloud.manager.core.models.token.Token;
import org.fogbowcloud.manager.core.plugins.IdentityPlugin;
import org.fogbowcloud.manager.core.plugins.util.Credential;
import org.fogbowcloud.manager.core.plugins.util.RSAUtils;
import org.fogbowcloud.manager.occi.model.ErrorType;
import org.fogbowcloud.manager.occi.model.OCCIException;
import org.fogbowcloud.manager.occi.model.ResponseConstants;
import org.json.JSONException;
import org.json.JSONObject;

public class LdapIdentityPlugin implements IdentityPlugin {

	private static final String ATT_EXPIRATION_DATE = "expirationDate";
	private static final String ATT_NAME = "name";
	private static final String ATT_LOGIN = "lognin";
	
	private static final Logger LOGGER = Logger.getLogger(LdapIdentityPlugin.class);
	private static final long EXPIRATION_INTERVAL = TimeUnit.DAYS.toMillis(365); // One year

	private static final String PROP_LDAP_BASE = "ldap_base";
	private static final String PROP_LDAP_URL = "ldap_identity_url";
	private static final String PROP_LDAP_ENCRYPT_TYPE = "ldap_encrypt_type";
	
	private static final String PRIVATE_KEY_PATH = "private_key_path";
	private static final String PUBLIC_KEY_PATH = "public_key_path";

	public static final String CRED_USERNAME = "username";
	public static final String CRED_PASSWORD = "password";
	public static final String CRED_AUTH_URL = "authUrl";
	public static final String CRED_LDAP_BASE = "base";
	public static final String CRED_LDAP_ENCRYPT = "encrypt";
	public static final String CRED_PRIVATE_KEY = "privateKey";
	public static final String CRED_PUBLIC_KEY = "publicKey";
	public static final String ENCRYPT_TYPE = ":TYPE:";
	public static final String ENCRYPT_PASS = ":PASS:";
	public static final String PASSWORD_ENCRYPTED = "{" + ENCRYPT_TYPE + "}" + ENCRYPT_PASS;
	public static final String ACCESSID_SEPARATOR = "!#!";

	private String ldapBase;
	private String ldapUrl;
	private String encryptType;
	
	private String privateKeyPath;
	private String publicKeyPath;

	public LdapIdentityPlugin(Properties properties) {

		ldapBase = properties.getProperty(PROP_LDAP_BASE);
		ldapUrl = properties.getProperty(PROP_LDAP_URL);
		encryptType = properties.getProperty(PROP_LDAP_ENCRYPT_TYPE);
		privateKeyPath = properties.getProperty(PRIVATE_KEY_PATH);
		publicKeyPath = properties.getProperty(PUBLIC_KEY_PATH);
	}

	@Override
	public Token createToken(Map<String, String> userCredentials) {
		
		String uid = userCredentials.get(CRED_USERNAME);
		String password = userCredentials.get(CRED_PASSWORD);
		String name = null;
		
		extractLdapPropertiesFromCredentials(userCredentials);
		
		parseCredentials(userCredentials);
		
		try {
			name = ldapAuthenticate(uid, password);
		} catch (Exception e) {
			LOGGER.error("Couldn't load account summary from LDAP Server.", e);
			throw new Exception("UNAUTHORIZED");
		}

		Map<String, String> attributes = new HashMap<String, String>();
		Date expirationDate = new Date(new Date().getTime() + EXPIRATION_INTERVAL);
		
		try {					
			JSONObject json = new JSONObject();
			json.put(ATT_LOGIN, uid);
			json.put(ATT_NAME, name);
			json.put(ATT_EXPIRATION_DATE, expirationDate.getTime());
			
			String signature = createSignature(json);
			
			String accessId = json.toString() + ACCESSID_SEPARATOR + signature;
			
			accessId = new String(Base64.encodeBase64(accessId.getBytes(Charsets.UTF_8), 
					false, false), Charsets.UTF_8);
			
			return new Token(accessId, new Token.User(uid, name), expirationDate, attributes);
			
		} catch (Exception e) {
			LOGGER.error("Erro while trying to sign the token.", e);
			throw new Exception("Erro while trying to sign the token.");
		}
		
	}

	private void extractLdapPropertiesFromCredentials(Map<String, String> userCredentials) {
		if (ldapBase == null || ldapBase.isEmpty()) {
			ldapBase = userCredentials.get(CRED_LDAP_BASE);
		}
		
		if (ldapUrl == null || ldapUrl.isEmpty()) {
			ldapUrl = userCredentials.get(CRED_AUTH_URL);
		}
		
		if (privateKeyPath == null || privateKeyPath.isEmpty()) {
			privateKeyPath = userCredentials.get(CRED_PRIVATE_KEY);
		}
		
		if (publicKeyPath == null || publicKeyPath.isEmpty()) {
			publicKeyPath = userCredentials.get(CRED_PUBLIC_KEY);
		}
	}

	@Override
	public Token reIssueToken(Token token) {
		
		return token;
	}

	@Override
	public Token getToken(String accessId) {
		try {
			
			String decodedAccessId = new String(Base64.decodeBase64(accessId),
					Charsets.UTF_8);
			
			String split[] = decodedAccessId.split(ACCESSID_SEPARATOR);
			if(split == null || split.length < 2){
				LOGGER.error("Invalid accessID: " + decodedAccessId);
				throw new Exception("UNAUTHORIZED");
			}
			
			String tokenMessage = split[0];
			String signature = split[1];
			
			JSONObject root = new JSONObject(tokenMessage);
			Date expirationDate = new Date(root.getLong(ATT_EXPIRATION_DATE));
			
			if(!verifySign(tokenMessage, signature)){
				LOGGER.error("Invalid accessID: "+decodedAccessId);
				throw new Exception("UNAUTHORIZED");
			}
			
			String uuid = root.getString(ATT_LOGIN);
			String name = root.getString(ATT_NAME);
			return new Token(accessId, new Token.User(uuid, name), expirationDate, 
					new HashMap<String, String>());			
		} catch (JSONException e) {
			LOGGER.error("Exception while getting token from json.", e);
			throw new Exception("UNAUTHORIZED");
		}			
	}

	@SuppressWarnings({ "rawtypes", "unchecked" })
	protected String ldapAuthenticate(String uid, String password) throws Exception {

		Hashtable env = new Hashtable();
		env.put(Context.INITIAL_CONTEXT_FACTORY, "com.sun.jndi.ldap.LdapCtxFactory");
		env.put(Context.PROVIDER_URL, ldapUrl);
		env.put(Context.SECURITY_AUTHENTICATION, "simple");

		DirContext ctx = null;
		String name = null;
		try {

			password = encryptPassword(password);

			ctx = new InitialDirContext(env);

			//Search the directory to get User Name and Domain from UID
			String filter = "(&(objectClass=inetOrgPerson)(uid={0}))";
			SearchControls ctls = new SearchControls();
			ctls.setSearchScope(SearchControls.SUBTREE_SCOPE);
			ctls.setReturningAttributes(new String[0]);
			ctls.setReturningObjFlag(true);
			NamingEnumeration enm = ctx.search(ldapBase, filter, new String[] { uid }, ctls);

			String dn = null;

			if (enm.hasMore()) {
				SearchResult result = (SearchResult) enm.next();
				dn = result.getNameInNamespace();
				name = extractUserName(result);
			}

			if (dn == null || enm.hasMore()) {
				// uid not found or not unique
				throw new NamingException("Authentication failed");
			}
			
			// Bind with found DN and given password
			ctx.addToEnvironment(Context.SECURITY_PRINCIPAL, dn);
			ctx.addToEnvironment(Context.SECURITY_CREDENTIALS, password);
			// Perform a lookup in order to force a bind operation with JNDI
			ctx.lookup(dn);

			enm.close();

			return name;
			
		} catch (Exception e) {
			LOGGER.error("Error while authenticate " + uid +" - Error: "+e.getMessage());
			throw e;
		} finally {
			ctx.close();
		}

	}

	private String extractUserName(SearchResult result) {
		String nameGroup[] = result.getName().split(",");
		if(nameGroup != null && nameGroup.length > 0){
			String cnName[] = nameGroup[0].split("=");
			if(cnName != null && cnName.length > 1){
				return cnName[1];
			}
		}
		return null;
	}

	private String encryptPassword(String password) throws NoSuchAlgorithmException, UnsupportedEncodingException {

		if (encryptType == null || encryptType.isEmpty()) {
			return password;
		}

		MessageDigest algorithm = MessageDigest.getInstance(encryptType);
		byte messageDigest[] = algorithm.digest(password.getBytes("UTF-8"));

		StringBuilder hexString = new StringBuilder();
		for (byte b : messageDigest) {
			hexString.append(String.format("%02X", 0xFF & b));
		}

		return PASSWORD_ENCRYPTED.replaceAll(ENCRYPT_TYPE, encryptType).replaceAll(ENCRYPT_PASS, hexString.toString());

	}
	
	@Override
	public boolean isValid(String accessId) {
		try {
			getToken(accessId);
			return true;
		} catch (Exception e) {
			return false;
		}
	}

	protected String createSignature(JSONObject json) throws IOException, GeneralSecurityException,
			NoSuchAlgorithmException, InvalidKeyException, SignatureException, UnsupportedEncodingException {
		RSAPrivateKey privateKey = null;
		privateKey = RSAUtils.getPrivateKey(privateKeyPath);
		String signature = RSAUtils.sign(privateKey, json.toString());
		return signature;
	}
	
	protected boolean verifySign(String tokenMessage, String signature) {
		RSAPublicKey publicKey = null;
		try {
			publicKey = RSAUtils.getPublicKey(publicKeyPath);
			return RSAUtils.verify(publicKey, tokenMessage, signature);	
		} catch (Exception e) {
			LOGGER.error("Erro while trying to validate sing of the token.", e);
			throw new OCCIException(ErrorType.INTERNAL_SERVER_ERROR, "Erro while trying to validate sing of the token.");
		}		
	}
	
	private void parseCredentials(Map<String, String> userCredentials) {
		String credLdapUrl = userCredentials.get(CRED_AUTH_URL);
		if(credLdapUrl != null && !credLdapUrl.isEmpty()){
			ldapUrl = credLdapUrl;
		}
		String credLdapBase = userCredentials.get(CRED_LDAP_BASE);
		if(credLdapBase != null && !credLdapBase.isEmpty()){
			ldapBase = credLdapBase;
		}
		String credEncryptType = userCredentials.get(CRED_LDAP_ENCRYPT);
		if(credEncryptType != null && !credEncryptType.isEmpty()){
			encryptType = credEncryptType;
		}
		
		String credPrivateKeyPath = userCredentials.get(CRED_PRIVATE_KEY);
		if(credPrivateKeyPath != null && !credPrivateKeyPath.isEmpty()){
			privateKeyPath = credPrivateKeyPath;
		}
		
		String credPublicKeyPath = userCredentials.get(CRED_PUBLIC_KEY);
		if(credPublicKeyPath != null && !credPublicKeyPath.isEmpty()){
			publicKeyPath = credPublicKeyPath;
		}
	}

	@Override
	public Credential[] getCredentials() {
		return new Credential[] { 
				new Credential(CRED_USERNAME, true, null),
				new Credential(CRED_PASSWORD, true, null), 
				new Credential(CRED_AUTH_URL, true, null),
				new Credential(CRED_LDAP_BASE, true, null),
				new Credential(CRED_LDAP_ENCRYPT, false, null),
				new Credential(CRED_PRIVATE_KEY, true, null),
				new Credential(CRED_PUBLIC_KEY, false, null)
		};
	}

	@Override
	public String getAuthenticationURI() {
		return null;
	}

	@Override
	public Token getForwardableToken(Token originalToken) {
		return originalToken;
	}

}