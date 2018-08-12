package org.fogbowcloud.manager.core.plugins.behavior.authentication.ldap;

import java.io.File;
import java.io.IOException;
import java.security.GeneralSecurityException;
import java.security.interfaces.RSAPublicKey;
import java.util.Date;
import java.util.Properties;
import org.apache.log4j.Logger;
import org.fogbowcloud.manager.core.HomeDir;
import org.fogbowcloud.manager.core.PropertiesHolder;
import org.fogbowcloud.manager.core.constants.ConfigurationConstants;
import org.fogbowcloud.manager.core.exceptions.*;
import org.fogbowcloud.manager.core.models.tokens.FederationUserToken;
import org.fogbowcloud.manager.core.plugins.behavior.authentication.AuthenticationPlugin;
import org.fogbowcloud.manager.util.PropertiesUtil;
import org.fogbowcloud.manager.util.RSAUtil;

public class LdapAuthenticationPlugin implements AuthenticationPlugin {

    private static final Logger LOGGER = Logger.getLogger(LdapAuthenticationPlugin.class);

    private static final String LDAP_PLUGIN_CONF_FILE = "ldap-identity-plugin.conf";
    private static final String PUBLIC_KEY_PATH = "public_key_path";

    public static final String CRED_USERNAME = "username";
    public static final String CRED_PASSWORD = "password";
    public static final String CRED_AUTH_URL = "authUrl";
    public static final String CRED_LDAP_BASE = "base";
    public static final String CRED_LDAP_ENCRYPT = "encrypt";
    public static final String CRED_PRIVATE_KEY = "privateKey";
    public static final String CRED_PUBLIC_KEY = "publicKey";
    public static final String TOKEN_VALUE_SEPARATOR = "!#!";

    private String localProviderId;
    private RSAPublicKey publicKey;

    public LdapAuthenticationPlugin() throws FatalErrorException {
        HomeDir homeDir = HomeDir.getInstance();
        this.localProviderId = PropertiesHolder.getInstance().getProperty(ConfigurationConstants.LOCAL_MEMBER_ID);

        Properties properties =
                PropertiesUtil.readProperties(
                        homeDir.getPath() + File.separator + LDAP_PLUGIN_CONF_FILE);
        String publicKeyPath = properties.getProperty(PUBLIC_KEY_PATH);
        try {
            this.publicKey = getPublicKey(publicKeyPath);
        } catch (IOException | GeneralSecurityException e) {
            throw new FatalErrorException("Error reading public key: " + publicKeyPath);
        }
    }

    @Override
    public boolean isAuthentic(FederationUserToken federationToken) {
        if (federationToken.getTokenProvider().equals(this.localProviderId)) {
            try {
                checkTokenValue(federationToken.getTokenValue());
                return true;
            } catch (Exception e) {
                return false;
            }
        } else {
            return true;
        }
    }

    private void checkTokenValue(String federationTokenValue)
            throws ExpiredTokenException, UnauthenticTokenException {

        String split[] = federationTokenValue.split(TOKEN_VALUE_SEPARATOR);
        if (split == null || split.length < 4) {
            throw new UnauthenticTokenException("Invalid tokens: " + federationTokenValue);
        }

        Date currentDate = new Date(System.currentTimeMillis());
        Date expirationDate = new Date(new Long(split[2]).longValue());

        String tokenValue = split[0] + TOKEN_VALUE_SEPARATOR + split[1] + TOKEN_VALUE_SEPARATOR + split[2];
        String signature = split[3];

        if (expirationDate.before(currentDate)) {
            throw new ExpiredTokenException("Expiration date: " + expirationDate);
        }

        if (!verifySign(tokenValue, signature)) {
            throw new UnauthenticTokenException("Invalid tokens: " + federationTokenValue);
        }
    }

    protected boolean verifySign(String tokenMessage, String signature) {
        try {
            return RSAUtil.verify(this.publicKey, tokenMessage, signature);
        } catch (Exception e) {
            throw new RuntimeException("Error while trying to validate sing of the tokens.", e);
        }
    }

    protected RSAPublicKey getPublicKey(String publicKeyPath) throws IOException, GeneralSecurityException {
        return RSAUtil.getPublicKey(publicKeyPath);
    }
}
