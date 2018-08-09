package org.fogbowcloud.manager.core.plugins.behavior.authentication.ldap;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.security.PublicKey;
import java.security.interfaces.RSAPublicKey;
import java.util.Date;
import java.util.Properties;
import org.apache.commons.codec.Charsets;
import org.apache.commons.codec.binary.Base64;
import org.apache.log4j.Logger;
import org.fogbowcloud.manager.core.HomeDir;
import org.fogbowcloud.manager.core.exceptions.*;
import org.fogbowcloud.manager.core.models.tokens.FederationUserToken;
import org.fogbowcloud.manager.core.plugins.behavior.authentication.AuthenticationPlugin;
import org.fogbowcloud.manager.util.PropertiesUtil;
import org.fogbowcloud.manager.util.RSAUtil;
import org.json.JSONException;
import org.json.JSONObject;

public class LdapAuthenticationPlugin implements AuthenticationPlugin {

    private static final Logger LOGGER = Logger.getLogger(LdapAuthenticationPlugin.class);

    private static final String LDAP_PLUGIN_CONF_FILE = "ldap-identity-plugin.conf";

    private static final String ATT_EXPIRATION_DATE = "expirationDate";
    private static final String ATT_NAME = "name";
    private static final String ATT_LOGIN = "login";

    private static final String PUBLIC_KEY_PATH = "public_key_path";

    public static final String CRED_USERNAME = "username";
    public static final String CRED_PASSWORD = "password";
    public static final String CRED_AUTH_URL = "authUrl";
    public static final String CRED_LDAP_BASE = "base";
    public static final String CRED_LDAP_ENCRYPT = "encrypt";
    public static final String CRED_PRIVATE_KEY = "privateKey";
    public static final String CRED_PUBLIC_KEY = "publicKey";
    public static final String ACCESSID_SEPARATOR = "!#!";

    private String publicKeyPath;

    public LdapAuthenticationPlugin() throws FatalErrorException {
        HomeDir homeDir = HomeDir.getInstance();
        Properties properties =
                PropertiesUtil.readProperties(
                        homeDir.getPath() + File.separator + LDAP_PLUGIN_CONF_FILE);
        this.publicKeyPath = properties.getProperty(PUBLIC_KEY_PATH);
    }

    @Override
    public FederationUserToken getFederationUser(String federationTokenValue) throws UnauthenticatedUserException {

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

            FederationUserToken federationUserToken = new FederationUserToken(federationTokenValue, uuid, null);
            federationUserToken.setName(name);
            return federationUserToken;
        } catch (JSONException e) {
            LOGGER.error("Exception while getting tokens from json.", e);
            throw new UnauthenticatedUserException();
        }
    }

    @Override
    public boolean isAuthentic(String federationTokenValue) {
        try {
            checkTokenValue(federationTokenValue);
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    private void checkTokenValue(String federationTokenValue)
            throws ExpiredTokenException, UnauthenticTokenException {
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

    protected boolean verifySign(String tokenMessage, String signature) {
        RSAPublicKey publicKey = null;
        try {
            publicKey = getPublicKey(this.publicKeyPath);
            return RSAUtil.verify(publicKey, tokenMessage, signature);
        } catch (Exception e) {
            throw new RuntimeException("Error while trying to validate sing of the tokens.", e);
        }
    }

    protected RSAPublicKey getPublicKey(String publicKeyPath) throws IOException, GeneralSecurityException {
        return RSAUtil.getPublicKey(publicKeyPath);
    }
}
