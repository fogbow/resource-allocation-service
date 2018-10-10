package org.fogbowcloud.ras.core.plugins.aaa.tokengenerator.ldap;

import java.io.UnsupportedEncodingException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Hashtable;
import java.util.Map;
import java.util.Properties;

import javax.naming.AuthenticationException;
import javax.naming.Context;
import javax.naming.NamingEnumeration;
import javax.naming.NamingException;
import javax.naming.directory.DirContext;
import javax.naming.directory.InitialDirContext;
import javax.naming.directory.SearchControls;
import javax.naming.directory.SearchResult;

import org.fogbowcloud.ras.core.HomeDir;
import org.fogbowcloud.ras.core.PropertiesHolder;
import org.fogbowcloud.ras.core.constants.ConfigurationConstants;
import org.fogbowcloud.ras.core.constants.Messages;
import org.fogbowcloud.ras.core.exceptions.FatalErrorException;
import org.fogbowcloud.ras.core.exceptions.InvalidParameterException;
import org.fogbowcloud.ras.core.exceptions.InvalidUserCredentialsException;
import org.fogbowcloud.ras.core.exceptions.UnauthenticatedUserException;
import org.fogbowcloud.ras.core.exceptions.UnexpectedException;
import org.fogbowcloud.ras.core.plugins.aaa.authentication.RASAuthenticationHolder;
import org.fogbowcloud.ras.core.plugins.aaa.tokengenerator.TokenGeneratorPlugin;
import org.fogbowcloud.ras.util.PropertiesUtil;
import org.fogbowcloud.ras.util.RSAUtil;

public class LdapTokenGeneratorPlugin implements TokenGeneratorPlugin {
	
    private static final String LDAP_PLUGIN_CONF_FILE = "ldap-token-generator-plugin.conf";
    private static final String PROP_LDAP_BASE = "ldap_base";
    private static final String PROP_LDAP_URL = "ldap_identity_url";
    private static final String PROP_LDAP_ENCRYPT_TYPE = "ldap_encrypt_type";
    public static final String CRED_USERNAME = "username";
    public static final String CRED_PASSWORD = "password";
    public static final String CRED_AUTH_URL = "authUrl";
    public static final String CRED_LDAP_BASE = "base";
    public static final String CRED_LDAP_ENCRYPT = "encrypt";
    public static final String CRED_PRIVATE_KEY = "privateKey";
    public static final String CRED_PUBLIC_KEY = "publicKey";
    public static final String TOKEN_VALUE_SEPARATOR = "!#!";
    public static final int LDAP_TOKEN_NUMBER_OF_FIELDS = 5;
    private static final String ENCRYPT_TYPE = ":TYPE:";
    private static final String ENCRYPT_PASS = ":PASS:";
    private static final String PASSWORD_ENCRYPTED = "{" + ENCRYPT_TYPE + "}" + ENCRYPT_PASS;
    private String tokenProviderId;
    private String ldapBase;
    private String ldapUrl;
    private String encryptType;
	private RASAuthenticationHolder rasAuthenticationHolder;

    public LdapTokenGeneratorPlugin() throws FatalErrorException {
        this.tokenProviderId = PropertiesHolder.getInstance().getProperty(ConfigurationConstants.LOCAL_MEMBER_ID);

        this.rasAuthenticationHolder = RASAuthenticationHolder.getInstance();
        
        Properties properties = PropertiesUtil.readProperties(
                HomeDir.getPath() + LDAP_PLUGIN_CONF_FILE);
        this.ldapBase = properties.getProperty(PROP_LDAP_BASE);
        this.ldapUrl = properties.getProperty(PROP_LDAP_URL);
        this.encryptType = properties.getProperty(PROP_LDAP_ENCRYPT_TYPE);
    }

    @Override
    public String createTokenValue(Map<String, String> userCredentials) throws InvalidUserCredentialsException,
            UnexpectedException, InvalidParameterException, UnauthenticatedUserException {

        String userId = userCredentials.get(CRED_USERNAME);
        String password = userCredentials.get(CRED_PASSWORD);

        String name = null;
        name = ldapAuthenticate(userId, password);

        String expirationTime = this.rasAuthenticationHolder.generateExpirationTime();

        try {
            String tokenValue = this.tokenProviderId + TOKEN_VALUE_SEPARATOR + userId + TOKEN_VALUE_SEPARATOR +
                    name + TOKEN_VALUE_SEPARATOR + expirationTime;
            String signature = this.rasAuthenticationHolder.createSignature(tokenValue);
            return tokenValue + TOKEN_VALUE_SEPARATOR + signature;
        } catch (Exception e) {
            throw new UnexpectedException(Messages.Exception.UNABLE_TO_SIGN_LDAP_TOKEN, e);
        }
    }

    @SuppressWarnings({"rawtypes", "unchecked"})
    public String ldapAuthenticate(String uid, String password) throws UnexpectedException, InvalidParameterException,
            InvalidUserCredentialsException, UnauthenticatedUserException {

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
            NamingEnumeration enm = ctx.search(this.ldapBase, filter, new String[]{uid}, ctls);

            String dn = null;

            if (enm.hasMore()) {
                SearchResult result = (SearchResult) enm.next();
                dn = result.getNameInNamespace();
                name = extractUserName(result);
            }

            if (dn == null || enm.hasMore()) {
                // uid not found or not unique
                throw new InvalidUserCredentialsException(Messages.Exception.UNABLE_TO_LOAD_LDAP_ACCOUNT);
            }

            // Bind with found DN and given password
            ctx.addToEnvironment(Context.SECURITY_PRINCIPAL, dn);
            ctx.addToEnvironment(Context.SECURITY_CREDENTIALS, password);
            // Perform a lookup in order to force a bind operation with JNDI
            ctx.lookup(dn);

            enm.close();

            return name;

        } catch (AuthenticationException e0) {
            throw new UnauthenticatedUserException(Messages.Exception.AUTHENTICATION_ERROR);
        } catch (NamingException e1) {
            throw new InvalidParameterException(Messages.Exception.LDAP_URL_MISSING, e1);
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
        byte messageDigest[] = algorithm.digest(password.getBytes(RSAUtil.UTF_8));

        StringBuilder hexString = new StringBuilder();
        for (byte b : messageDigest) {
            hexString.append(String.format("%02X", 0xFF & b));
        }

        return PASSWORD_ENCRYPTED
                .replaceAll(ENCRYPT_TYPE, this.encryptType)
                .replaceAll(ENCRYPT_PASS, hexString.toString());
    }

}
