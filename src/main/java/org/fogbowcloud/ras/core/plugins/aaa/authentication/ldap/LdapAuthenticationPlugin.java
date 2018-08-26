package org.fogbowcloud.ras.core.plugins.aaa.authentication.ldap;

import org.apache.log4j.Logger;
import org.fogbowcloud.ras.core.HomeDir;
import org.fogbowcloud.ras.core.PropertiesHolder;
import org.fogbowcloud.ras.core.constants.ConfigurationConstants;
import org.fogbowcloud.ras.core.exceptions.ExpiredTokenException;
import org.fogbowcloud.ras.core.exceptions.FatalErrorException;
import org.fogbowcloud.ras.core.exceptions.UnauthenticTokenException;
import org.fogbowcloud.ras.core.models.tokens.FederationUserToken;
import org.fogbowcloud.ras.core.plugins.aaa.tokengenerator.ldap.LdapTokenGeneratorPlugin;
import org.fogbowcloud.ras.core.plugins.aaa.authentication.AuthenticationPlugin;
import org.fogbowcloud.ras.util.PropertiesUtil;
import org.fogbowcloud.ras.util.RSAUtil;

import java.io.IOException;
import java.security.GeneralSecurityException;
import java.security.interfaces.RSAPublicKey;
import java.util.Date;
import java.util.Properties;

public class LdapAuthenticationPlugin implements AuthenticationPlugin {
    private static final Logger LOGGER = Logger.getLogger(LdapAuthenticationPlugin.class);

    private static final String LDAP_PLUGIN_CONF_FILE = "ldap-identity-plugin.conf";
    private static final String PUBLIC_KEY_PATH = "public_key_path";
    private String localProviderId;
    private RSAPublicKey publicKey;

    public LdapAuthenticationPlugin() throws FatalErrorException {
        this.localProviderId = PropertiesHolder.getInstance().getProperty(ConfigurationConstants.LOCAL_MEMBER_ID);

        Properties properties = PropertiesUtil.readProperties(HomeDir.getPath() + LDAP_PLUGIN_CONF_FILE);
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

    private void checkTokenValue(String federationTokenValue) throws ExpiredTokenException, UnauthenticTokenException {

        String split[] = federationTokenValue.split(LdapTokenGeneratorPlugin.TOKEN_VALUE_SEPARATOR);
        if (split == null || split.length < 5) {
            throw new UnauthenticTokenException("Invalid tokens: " + federationTokenValue);
        }

        Date currentDate = new Date(System.currentTimeMillis());
        Date expirationDate = new Date(new Long(split[3]).longValue());

        String tokenValue = split[0] + LdapTokenGeneratorPlugin.TOKEN_VALUE_SEPARATOR + split[1] +
                LdapTokenGeneratorPlugin.TOKEN_VALUE_SEPARATOR + split[2] +
                LdapTokenGeneratorPlugin.TOKEN_VALUE_SEPARATOR + split[3];
        String signature = split[4];

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
