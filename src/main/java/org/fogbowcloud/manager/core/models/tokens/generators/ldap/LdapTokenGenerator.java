package org.fogbowcloud.manager.core.models.tokens.generators.ldap;

import org.apache.commons.codec.Charsets;
import org.fogbowcloud.manager.core.models.tokens.FederationUserToken;
import org.fogbowcloud.manager.core.models.tokens.TokenGenerator;
import org.apache.commons.codec.binary.Base64;
import org.apache.log4j.Logger;
import org.fogbowcloud.manager.core.HomeDir;
import org.fogbowcloud.manager.core.exceptions.*;
import org.fogbowcloud.manager.util.PropertiesUtil;
import org.fogbowcloud.manager.util.RSAUtil;
import org.json.JSONException;
import org.json.JSONObject;

import javax.naming.Context;
import javax.naming.NamingEnumeration;
import javax.naming.NamingException;
import javax.naming.directory.DirContext;
import javax.naming.directory.InitialDirContext;
import javax.naming.directory.SearchControls;
import javax.naming.directory.SearchResult;
import java.io.File;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.nio.charset.StandardCharsets;
import java.security.*;
import java.security.interfaces.RSAPrivateKey;
import java.security.interfaces.RSAPublicKey;
import java.util.Date;
import java.util.Hashtable;
import java.util.Map;
import java.util.Properties;
import java.util.concurrent.TimeUnit;

public class LdapTokenGenerator implements TokenGenerator<FederationUserToken> {
    private static final Logger LOGGER = Logger.getLogger(LdapTokenGenerator.class);
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

    public LdapTokenGenerator() throws FatalErrorException {
        HomeDir homeDir = HomeDir.getInstance();
        Properties properties = PropertiesUtil.readProperties(
                homeDir.getPath() + File.separator + LDAP_PLUGIN_CONF_FILE);
        this.ldapBase = properties.getProperty(PROP_LDAP_BASE);
        this.ldapUrl = properties.getProperty(PROP_LDAP_URL);
        this.encryptType = properties.getProperty(PROP_LDAP_ENCRYPT_TYPE);
        this.privateKeyPath = properties.getProperty(PRIVATE_KEY_PATH);
        this.publicKeyPath = properties.getProperty(PUBLIC_KEY_PATH);
        LOGGER.debug("Publickey path: " + this.publicKeyPath);
    }

    @Override
    public FederationUserToken createToken(String federationTokenValue) throws UnauthenticatedUserException {

        try {
            String decodedFederationTokenValue =
                    new String(Base64.decodeBase64(federationTokenValue), Charsets.UTF_8);

            String split[] = decodedFederationTokenValue.split(ACCESSID_SEPARATOR);
            if (split == null || split.length < 2) {
                LOGGER.error("Invalid accessID: " + decodedFederationTokenValue);
                throw new UnauthenticatedUserException();
            }

            String tokenValue = split[0];
            String signature = split[1];

            JSONObject root = new JSONObject(tokenValue);

            if (!verifySign(tokenValue, signature)) {
                LOGGER.error("Invalid accessID: " + decodedFederationTokenValue);
                throw new UnauthenticatedUserException();
            }

            String uuid = root.getString(ATT_LOGIN);
            String name = root.getString(ATT_NAME);

            FederationUserToken federationUserToken =
                    new FederationUserToken(federationTokenValue, uuid, name);
            return federationUserToken;
        } catch (JSONException e) {
            LOGGER.error("Exception while getting tokens from json.", e);
            throw new UnauthenticatedUserException();
        }
    }

    protected boolean verifySign(String tokenMessage, String signature) {
        RSAPublicKey publicKey = null;
        try {
            publicKey = RSAUtil.getPublicKey(this.publicKeyPath);
            LOGGER.debug("Publickey: "+ publicKey.toString());
            return RSAUtil.verify(publicKey, tokenMessage, signature);
        } catch (Exception e) {
            throw new RuntimeException("Error while trying to validate sing of the tokens.", e);
        }
    }

    @Override
    public String createTokenValue(Map<String, String> userCredentials)
            throws InvalidCredentialsUserException, TokenValueCreationException {

        String userId = userCredentials.get(CRED_USERNAME);
        String password = userCredentials.get(CRED_PASSWORD);

        extractLdapPropertiesFromCredentials(userCredentials);

        parseCredentials(userCredentials);

        String name = null;
        try {
            name = ldapAuthenticate(userId, password);
        } catch (Exception e) {
            throw new InvalidCredentialsUserException("Couldn't load account summary from LDAP Network.", e);
        }

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
                            false,
                            false),
                            StandardCharsets.UTF_8);

            return federationTokenValue;
        } catch (IOException | GeneralSecurityException e) {
            throw new TokenValueCreationException("Error while trying to sign the tokens.", e);
        }
    }

    private void extractLdapPropertiesFromCredentials(Map<String, String> userCredentials) {
        if (this.ldapBase == null || this.ldapBase.isEmpty()) {
            this.ldapBase = userCredentials.get(CRED_LDAP_BASE);
        }

        if (this.ldapUrl == null || ldapUrl.isEmpty()) {
            this.ldapUrl = userCredentials.get(CRED_AUTH_URL);
        }

        if (this.privateKeyPath == null || privateKeyPath.isEmpty()) {
            this.privateKeyPath = userCredentials.get(CRED_PRIVATE_KEY);
        }

        if (this.publicKeyPath == null || publicKeyPath.isEmpty()) {
            this.publicKeyPath = userCredentials.get(CRED_PUBLIC_KEY);
        }
    }

    @SuppressWarnings({"rawtypes", "unchecked"})
    public String ldapAuthenticate(String uid, String password) throws NoSuchAlgorithmException,
            UnsupportedEncodingException, FogbowManagerException {

        String contextFactory = "com.sun.jndi.ldap.LdapCtxFactory";
        String securityAuthentication = "simple";

        Hashtable env = new Hashtable();
        env.put(Context.INITIAL_CONTEXT_FACTORY, contextFactory);
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

        } catch (NamingException e) {
            throw new FogbowManagerException("Ldap url is not provided in conf files.");
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

    protected String createSignature(JSONObject json) throws IOException, GeneralSecurityException {
        RSAPrivateKey privateKey = null;
        privateKey = getPrivateKey(this.privateKeyPath);
        String signature = RSAUtil.sign(privateKey, json.toString());
        return signature;
    }

    public RSAPrivateKey getPrivateKey(String privateKeyPath) throws IOException, GeneralSecurityException {
        return RSAUtil.getPrivateKey(privateKeyPath);
    }

    private void parseCredentials(Map<String, String> userCredentials) {
        String credLdapUrl = userCredentials.get(CRED_AUTH_URL);
        if (credLdapUrl != null && !credLdapUrl.isEmpty()) {
            this.ldapUrl = credLdapUrl;
        }
        String credLdapBase = userCredentials.get(CRED_LDAP_BASE);
        if (credLdapBase != null && !credLdapBase.isEmpty()) {
            this.ldapBase = credLdapBase;
        }
        String credEncryptType = userCredentials.get(CRED_LDAP_ENCRYPT);
        if (credEncryptType != null && !credEncryptType.isEmpty()) {
            this.encryptType = credEncryptType;
        }

        String credPrivateKeyPath = userCredentials.get(CRED_PRIVATE_KEY);
        if (credPrivateKeyPath != null && !credPrivateKeyPath.isEmpty()) {
            this.privateKeyPath = credPrivateKeyPath;
        }

        String credPublicKeyPath = userCredentials.get(CRED_PUBLIC_KEY);
        if (credPublicKeyPath != null && !credPublicKeyPath.isEmpty()) {
            this.publicKeyPath = credPublicKeyPath;
        }
    }
}