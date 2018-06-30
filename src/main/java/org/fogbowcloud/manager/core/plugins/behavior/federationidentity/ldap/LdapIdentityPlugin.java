package org.fogbowcloud.manager.core.plugins.behavior.federationidentity.ldap;

import java.io.File;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.security.InvalidKeyException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SignatureException;
import java.security.interfaces.RSAPrivateKey;
import java.security.interfaces.RSAPublicKey;
import java.util.*;
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
import org.fogbowcloud.manager.core.HomeDir;
import org.fogbowcloud.manager.core.exceptions.*;
import org.fogbowcloud.manager.core.plugins.behavior.federationidentity.FederationIdentityPlugin;
import org.fogbowcloud.manager.core.models.tokens.FederationUser;
import org.fogbowcloud.manager.util.PropertiesUtil;
import org.fogbowcloud.manager.util.RSAUtil;
import org.json.JSONException;
import org.json.JSONObject;

/**
 * FIXME: BY THE FACT THAT THIS CODE IS NATURALLY COMPLEX, IS NECESSARY TO WRITE A DOCUMENTATION FOR
 * THIS CODE.
 */
public class LdapIdentityPlugin implements FederationIdentityPlugin {

    private static final Logger LOGGER = Logger.getLogger(LdapIdentityPlugin.class);

    private static final String LDAP_PLUGIN_CONF_FILE = "ldap-identity-plugin.conf";

    private static final String ATT_EXPIRATION_DATE = "expirationDate";
    private static final String ATT_NAME = "name";
    private static final String ATT_LOGIN = "login";

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

    public LdapIdentityPlugin() throws FatalErrorException {
        HomeDir homeDir = HomeDir.getInstance();
        Properties properties = PropertiesUtil.
                readProperties(homeDir.getPath() + File.separator + LDAP_PLUGIN_CONF_FILE);
        this.ldapBase = properties.getProperty(PROP_LDAP_BASE);
        this.ldapUrl = properties.getProperty(PROP_LDAP_URL);
        this.encryptType = properties.getProperty(PROP_LDAP_ENCRYPT_TYPE);
        this.privateKeyPath = properties.getProperty(PRIVATE_KEY_PATH);
        this.publicKeyPath = properties.getProperty(PUBLIC_KEY_PATH);
    }

    @Override
    public String createFederationTokenValue(Map<String, String> userCredentials)
        throws UnauthenticatedUserException, TokenValueCreationException {

        String userId = userCredentials.get(CRED_USERNAME);
        String password = userCredentials.get(CRED_PASSWORD);
        String name = null;

        extractLdapPropertiesFromCredentials(userCredentials);

        parseCredentials(userCredentials);

        try {
            name = ldapAuthenticate(userId, password);
        } catch (Exception e) {
            throw new InvalidCredentialsUserException(
                    "Couldn't load account summary from LDAP Server.", e);
        }

        Map<String, String> attributes = new HashMap<String, String>();
        Date expirationDate = new Date(new Date().getTime() + EXPIRATION_INTERVAL);

        try {
            JSONObject json = new JSONObject();
            json.put(ATT_LOGIN, userId);
            json.put(ATT_NAME, name);
            json.put(ATT_EXPIRATION_DATE, expirationDate.getTime());

            String signature = createSignature(json);

            String federationTokenValue = json.toString() + ACCESSID_SEPARATOR + signature;

            federationTokenValue =
                    new String(Base64.encodeBase64(federationTokenValue.getBytes(StandardCharsets.UTF_8),
                            false, false), StandardCharsets.UTF_8);

            return federationTokenValue;
        } catch (IOException | GeneralSecurityException e) {
            throw new TokenValueCreationException("Error while trying to sign the tokens.", e);
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
    public FederationUser getFederationUser(String federationTokenValue) throws UnauthenticatedUserException {

        try {

            String decodedFederationTokenValue = new String(Base64.decodeBase64(federationTokenValue), Charsets.UTF_8);

            String split[] = decodedFederationTokenValue.split(ACCESSID_SEPARATOR);
            if(split == null || split.length < 2){
                LOGGER.error("Invalid accessID: " + decodedFederationTokenValue);
                throw new UnauthenticatedUserException();
            }

            String tokenValue = split[0];
            String signature = split[1];

            JSONObject root = new JSONObject(tokenValue);

            if(!verifySign(tokenValue, signature)){
                LOGGER.error("Invalid accessID: " + decodedFederationTokenValue);
                throw new UnauthenticatedUserException();
            }

            String uuid = root.getString(ATT_LOGIN);
            String name = root.getString(ATT_NAME);

            HashMap<String, String> attributes = new HashMap<String, String>();
            attributes.put(FederationUser.MANDATORY_NAME_ATTRIBUTE, name);

            return new FederationUser(uuid, attributes);
        } catch (JSONException e) {
            LOGGER.error("Exception while getting tokens from json.", e);
            throw new UnauthenticatedUserException();
        }
    }

    @Override
    public boolean isValid(String federationTokenValue) {
        try {
            checkTokenValue(federationTokenValue);
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    private void checkTokenValue(String federationTokenValue) throws ExpiredTokenException, UnauthenticTokenException {
        try {
            String decodedAccessId =
                    new String(Base64.decodeBase64(federationTokenValue), StandardCharsets.UTF_8);

            String split[] = decodedAccessId.split(ACCESSID_SEPARATOR);
            if (split == null || split.length < 2) {
                throw new UnauthenticTokenException("Invalid tokens: " + decodedAccessId);
            }

            String tokenMessage = split[0];
            String signature = split[1];

            JSONObject root = new JSONObject(tokenMessage);
            Date expirationDate = new Date(root.getLong(ATT_EXPIRATION_DATE));
            Date currentDate = new Date(System.currentTimeMillis());

            if (expirationDate.before(currentDate)) {
                throw new ExpiredTokenException("Expiration date: " + expirationDate);
            }

            if (!verifySign(tokenMessage, signature)) {
                throw new UnauthenticTokenException("Invalid tokens: " + decodedAccessId);
            }
        } catch (JSONException e) {
            throw new UnauthenticTokenException("Exception while getting tokens from json.", e);
        }
    }

    @SuppressWarnings({"rawtypes", "unchecked"})
    protected String ldapAuthenticate(String uid, String password) throws Exception {

        String contextFactory = "com.sun.jndi.ldap.LdapCtxFactory";
        String securityAuthentication = "simple";

        Hashtable env = new Hashtable();
        env.put(Context.INITIAL_CONTEXT_FACTORY, contextFactory);
        env.put(Context.PROVIDER_URL, this.ldapUrl);
        env.put(Context.SECURITY_AUTHENTICATION, securityAuthentication);

        DirContext ctx = null;
        String name = null;
        try {

            password = encryptPassword(password);

            ctx = new InitialDirContext(env);

            // Search the directory to get User Name and Domain from UID
            String filter = "(&(objectClass=inetOrgPerson)(uid={0}))";
            SearchControls ctls = new SearchControls();
            ctls.setSearchScope(SearchControls.SUBTREE_SCOPE);
            ctls.setReturningAttributes(new String[0]);
            ctls.setReturningObjFlag(true);
            NamingEnumeration enm = ctx.search(this.ldapBase, filter, new String[] {uid}, ctls);

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

        } finally {
            ctx.close();
        }
    }

    private String extractUserName(SearchResult result) {
        String nameGroup[] = result.getName().split(",");
        if (nameGroup != null && nameGroup.length > 0) {
            String cnName[] = nameGroup[0].split("=");
            if (cnName != null && cnName.length > 1) {
                return cnName[1];
            }
        }
        return null;
    }

    private String encryptPassword(String password)
            throws NoSuchAlgorithmException, UnsupportedEncodingException {
        if (this.encryptType == null || this.encryptType.isEmpty()) {
            return password;
        }

        MessageDigest algorithm = MessageDigest.getInstance(this.encryptType);
        byte messageDigest[] = algorithm.digest(password.getBytes("UTF-8"));

        StringBuilder hexString = new StringBuilder();
        for (byte b : messageDigest) {
            hexString.append(String.format("%02X", 0xFF & b));
        }

        return PASSWORD_ENCRYPTED
                .replaceAll(ENCRYPT_TYPE, this.encryptType)
                .replaceAll(ENCRYPT_PASS, hexString.toString());
    }

    protected String createSignature(JSONObject json)
            throws IOException, GeneralSecurityException, NoSuchAlgorithmException,
                    InvalidKeyException, SignatureException, UnsupportedEncodingException {
        RSAPrivateKey privateKey = null;
        privateKey = RSAUtil.getPrivateKey(this.privateKeyPath);
        String signature = RSAUtil.sign(privateKey, json.toString());
        return signature;
    }

    protected boolean verifySign(String tokenMessage, String signature) {
        RSAPublicKey publicKey = null;
        try {
            publicKey = RSAUtil.getPublicKey(this.publicKeyPath);
            return RSAUtil.verify(publicKey, tokenMessage, signature);
        } catch (Exception e) {
            throw new RuntimeException("Error while trying to validate sing of the tokens.", e);
        }
    }

    private void parseCredentials(Map<String, String> userCredentials) {
        String credLdapUrl = userCredentials.get(CRED_AUTH_URL);
        if (credLdapUrl != null && !credLdapUrl.isEmpty()) {
            ldapUrl = credLdapUrl;
        }
        String credLdapBase = userCredentials.get(CRED_LDAP_BASE);
        if (credLdapBase != null && !credLdapBase.isEmpty()) {
            ldapBase = credLdapBase;
        }
        String credEncryptType = userCredentials.get(CRED_LDAP_ENCRYPT);
        if (credEncryptType != null && !credEncryptType.isEmpty()) {
            encryptType = credEncryptType;
        }

        String credPrivateKeyPath = userCredentials.get(CRED_PRIVATE_KEY);
        if (credPrivateKeyPath != null && !credPrivateKeyPath.isEmpty()) {
            privateKeyPath = credPrivateKeyPath;
        }

        String credPublicKeyPath = userCredentials.get(CRED_PUBLIC_KEY);
        if (credPublicKeyPath != null && !credPublicKeyPath.isEmpty()) {
            publicKeyPath = credPublicKeyPath;
        }
    }

}
