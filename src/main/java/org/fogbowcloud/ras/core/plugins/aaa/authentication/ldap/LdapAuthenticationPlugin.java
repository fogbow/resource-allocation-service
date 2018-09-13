package org.fogbowcloud.ras.core.plugins.aaa.authentication.ldap;

import java.io.IOException;
import java.security.GeneralSecurityException;
import java.security.interfaces.RSAPublicKey;
import java.util.Date;

import org.apache.log4j.Logger;
import org.fogbowcloud.ras.core.PropertiesHolder;
import org.fogbowcloud.ras.core.constants.ConfigurationConstants;
import org.fogbowcloud.ras.core.constants.Messages;
import org.fogbowcloud.ras.core.exceptions.ExpiredTokenException;
import org.fogbowcloud.ras.core.exceptions.FatalErrorException;
import org.fogbowcloud.ras.core.exceptions.UnauthenticTokenException;
import org.fogbowcloud.ras.core.models.tokens.FederationUserToken;
import org.fogbowcloud.ras.core.plugins.aaa.authentication.AuthenticationPlugin;
import org.fogbowcloud.ras.core.plugins.aaa.tokengenerator.ldap.LdapTokenGeneratorPlugin;
import org.fogbowcloud.ras.util.RSAUtil;

public class LdapAuthenticationPlugin implements AuthenticationPlugin {
    private static final Logger LOGGER = Logger.getLogger(LdapAuthenticationPlugin.class);

    private String localProviderId;
    private RSAPublicKey publicKey;

    public LdapAuthenticationPlugin() throws FatalErrorException {
        this.localProviderId = PropertiesHolder.getInstance().getProperty(ConfigurationConstants.LOCAL_MEMBER_ID);

        try {
            this.publicKey = getPublicKey();
        } catch (IOException | GeneralSecurityException e) {
        	throw new FatalErrorException(Messages.Fatal.PUBLIC_KEY_ERROR);
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
            throw new UnauthenticTokenException(String.format(Messages.Exception.INVALID_TOKEN, federationTokenValue));
        }

        Date currentDate = new Date(System.currentTimeMillis());
        Date expirationDate = new Date(new Long(split[3]).longValue());

        String tokenValue = split[0] + LdapTokenGeneratorPlugin.TOKEN_VALUE_SEPARATOR + split[1] +
                LdapTokenGeneratorPlugin.TOKEN_VALUE_SEPARATOR + split[2] +
                LdapTokenGeneratorPlugin.TOKEN_VALUE_SEPARATOR + split[3];
        String signature = split[4];

        if (expirationDate.before(currentDate)) {
        	throw new UnauthenticTokenException(String.format(Messages.Exception.EXPIRATION_DATE, expirationDate));
        }

        if (!verifySign(tokenValue, signature)) {
            throw new UnauthenticTokenException(String.format(Messages.Exception.INVALID_TOKEN, federationTokenValue));
        }
    }

    protected boolean verifySign(String tokenMessage, String signature) {
        try {
            return RSAUtil.verify(this.publicKey, tokenMessage, signature);
        } catch (Exception e) {
            throw new RuntimeException(Messages.Exception.ERROR_VALIDATE_TOKEN_SINGNATURE, e);
        }
    }

    protected RSAPublicKey getPublicKey() throws IOException, GeneralSecurityException {
        return RSAUtil.getPublicKey();
    }
}
