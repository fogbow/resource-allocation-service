package org.fogbowcloud.ras.core.plugins.aaa.tokengenerator.ldap;

import org.apache.log4j.Logger;
import org.fogbowcloud.ras.core.HomeDir;
import org.fogbowcloud.ras.core.PropertiesHolder;
import org.fogbowcloud.ras.core.constants.ConfigurationConstants;
import org.fogbowcloud.ras.core.exceptions.FatalErrorException;
import org.fogbowcloud.ras.core.exceptions.InvalidParameterException;
import org.fogbowcloud.ras.core.exceptions.InvalidUserCredentialsException;
import org.fogbowcloud.ras.core.exceptions.UnexpectedException;
import org.fogbowcloud.ras.core.plugins.aaa.tokengenerator.TokenGeneratorPlugin;
import org.fogbowcloud.ras.core.plugins.aaa.identity.ldap.LdapFederationIdentityPlugin;
import org.fogbowcloud.ras.util.PropertiesUtil;
import org.fogbowcloud.ras.util.RSAUtil;

import javax.naming.Context;
import javax.naming.NamingEnumeration;
import javax.naming.NamingException;
import javax.naming.directory.DirContext;
import javax.naming.directory.InitialDirContext;
import javax.naming.directory.SearchControls;
import javax.naming.directory.SearchResult;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.security.GeneralSecurityException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.interfaces.RSAPrivateKey;
import java.util.Date;
import java.util.Hashtable;
import java.util.Map;
import java.util.Properties;
import java.util.concurrent.TimeUnit;

public class LdapTokenGeneratorPlugin implements TokenGeneratorPlugin {
    private static final Logger LOGGER = Logger.getLogger(LdapFederationIdentityPlugin.class);

    private static final String LDAP_PLUGIN_CONF_FILE = "ldap-identity-plugin.conf";
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
    public static final String TOKEN_VALUE_SEPARATOR = "!#!";
    private static final String ENCRYPT_TYPE = ":TYPE:";
    private static final String ENCRYPT_PASS = ":PASS:";
    private static final String PASSWORD_ENCRYPTED = "{" + ENCRYPT_TYPE + "}" + ENCRYPT_PASS;
    private String tokenProviderId;
    private String ldapBase;
    private String ldapUrl;
    private String encryptType;
    private String privateKeyPath;
    private String publicKeyPath;
    private RSAPrivateKey privateKey;

    public LdapTokenGeneratorPlugin() throws FatalErrorException {
        this.tokenProviderId = PropertiesHolder.getInstance().getProperty(ConfigurationConstants.LOCAL_MEMBER_ID);

        Properties properties = PropertiesUtil.readProperties(
                HomeDir.getPath() + LDAP_PLUGIN_CONF_FILE);
        this.ldapBase = properties.getProperty(PROP_LDAP_BASE);
        this.ldapUrl = properties.getProperty(PROP_LDAP_URL);
        this.encryptType = properties.getProperty(PROP_LDAP_ENCRYPT_TYPE);
        this.privateKeyPath = properties.getProperty(PRIVATE_KEY_PATH);
        this.publicKeyPath = properties.getProperty(PUBLIC_KEY_PATH);
        try {
            this.privateKey = RSAUtil.getPrivateKey(this.privateKeyPath);
        } catch (IOException | GeneralSecurityException e) {
            throw new FatalErrorException("Error reading private key: " + this.privateKeyPath + e.getMessage());
        }
    }

    @Override
    public String createTokenValue(Map<String, String> userCredentials)
            throws InvalidUserCredentialsException, UnexpectedException, InvalidParameterException {

        String userId = userCredentials.get(CRED_USERNAME);
        String password = userCredentials.get(CRED_PASSWORD);

        extractLdapPropertiesFromCredentials(userCredentials);

        parseCredentials(userCredentials);

        String name = null;
        name = ldapAuthenticate(userId, password);

        Date expirationDate = new Date(new Date().getTime() + EXPIRATION_INTERVAL);
        String expirationTime = Long.toString(expirationDate.getTime());

        try {
            String tokenValue = this.tokenProviderId + TOKEN_VALUE_SEPARATOR + userId + TOKEN_VALUE_SEPARATOR +
                    name + TOKEN_VALUE_SEPARATOR + expirationTime;
            String signature = createSignature(tokenValue);
            return tokenValue + TOKEN_VALUE_SEPARATOR + signature;
        } catch (IOException | GeneralSecurityException e) {
            throw new UnexpectedException("Error while trying to sign the tokens.", e);
        }
    }

    @SuppressWarnings({"rawtypes", "unchecked"})
    public String ldapAuthenticate(String uid, String password) throws UnexpectedException, InvalidParameterException,
            InvalidUserCredentialsException {

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
            NamingEnumeration enm = ctx.search(this.ldapBase, filter, new String[]{uid}, ctls);

            String dn = null;

            if (enm.hasMore()) {
                SearchResult result = (SearchResult) enm.next();
                dn = result.getNameInNamespace();
                name = extractUserName(result);
            }

            if (dn == null || enm.hasMore()) {
                // uid not found or not unique
                throw new InvalidUserCredentialsException("Couldn't load account summary from LDAP Network.");
            }

            // Bind with found DN and given password
            ctx.addToEnvironment(Context.SECURITY_PRINCIPAL, dn);
            ctx.addToEnvironment(Context.SECURITY_CREDENTIALS, password);
            // Perform a lookup in order to force a bind operation with JNDI
            ctx.lookup(dn);

            enm.close();

            return name;

        } catch (NamingException e1) {
            throw new InvalidParameterException("Ldap url is not provided in conf files.", e1);
        } catch (NoSuchAlgorithmException | UnsupportedEncodingException e2) {
            throw new UnexpectedException(e2.getMessage(), e2);
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

    private String createSignature(String message) throws IOException, GeneralSecurityException {
        return RSAUtil.sign(this.privateKey, message);
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
