package org.fogbowcloud.manager.core.plugins.behavior.identity.ldap;

import org.fogbowcloud.manager.core.PropertiesHolder;
import org.fogbowcloud.manager.core.constants.ConfigurationConstants;
import org.fogbowcloud.manager.core.exceptions.InvalidParameterException;
import org.fogbowcloud.manager.core.models.tokens.LdapToken;
import org.fogbowcloud.manager.core.models.tokens.generators.ldap.LdapTokenGenerator;
import org.fogbowcloud.manager.core.plugins.behavior.identity.FederationIdentityPlugin;
import org.apache.log4j.Logger;
import org.fogbowcloud.manager.core.HomeDir;
import org.fogbowcloud.manager.core.exceptions.*;
import org.fogbowcloud.manager.util.PropertiesUtil;
import org.fogbowcloud.manager.util.RSAUtil;

import java.io.File;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.security.*;
import java.security.interfaces.RSAPublicKey;
import java.util.Properties;

public class LdapFederationIdentityPlugin implements FederationIdentityPlugin<LdapToken> {
    private static final Logger LOGGER = Logger.getLogger(LdapFederationIdentityPlugin.class);

    private static final String LDAP_PLUGIN_CONF_FILE = "ldap-identity-plugin.conf";
    private static final String PUBLIC_KEY_PATH = "public_key_path";

    private RSAPublicKey publicKey;

    public LdapFederationIdentityPlugin() throws FatalErrorException {
        HomeDir homeDir = HomeDir.getInstance();
        Properties properties = PropertiesUtil.readProperties(
                homeDir.getPath() + File.separator + LDAP_PLUGIN_CONF_FILE);
        String publicKeyPath = properties.getProperty(PUBLIC_KEY_PATH);
        try {
            this.publicKey = RSAUtil.getPublicKey(publicKeyPath);
        } catch (IOException | GeneralSecurityException e) {
            throw new FatalErrorException("Error reading public key: " + publicKeyPath + e.getMessage());
        }
    }

    @Override
    public LdapToken createToken(String federationTokenValue) throws InvalidParameterException {

        String split[] = federationTokenValue.split(LdapTokenGenerator.TOKEN_VALUE_SEPARATOR);
        if (split == null || split.length < 5) {
            LOGGER.error("Invalid token value: " + federationTokenValue);
            throw new InvalidParameterException();
        }

        String tokenProvider = split[0];
        String uuid = split[1];
        String name = split[2];
        String expirationDate = split[3];
        String signature = split[4];
        String tokenValue = tokenProvider + LdapTokenGenerator.TOKEN_VALUE_SEPARATOR + uuid +
                LdapTokenGenerator.TOKEN_VALUE_SEPARATOR + name +
                LdapTokenGenerator.TOKEN_VALUE_SEPARATOR + expirationDate;

        if (!verifySign(tokenValue, signature)) {
            LOGGER.error("Invalid token value: " + federationTokenValue);
            throw new InvalidParameterException();
        }

        LdapToken ldapToken = new LdapToken(tokenProvider, federationTokenValue, uuid, name, expirationDate);
        return ldapToken;
    }

    // Used for testing
    public boolean verifySign(String tokenMessage, String signature) {
        try {
            return RSAUtil.verify(this.publicKey, tokenMessage, signature);
        } catch (SignatureException | NoSuchAlgorithmException | UnsupportedEncodingException | InvalidKeyException e) {
            return false;
        }
    }
}