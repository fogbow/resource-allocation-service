package org.fogbowcloud.ras.core.plugins.aaa.authentication;

import org.apache.log4j.Logger;
import org.fogbowcloud.ras.core.PropertiesHolder;
import org.fogbowcloud.ras.core.constants.ConfigurationConstants;
import org.fogbowcloud.ras.core.constants.Messages;
import org.fogbowcloud.ras.core.exceptions.FatalErrorException;
import org.fogbowcloud.ras.core.models.tokens.FederationUserToken;
import org.fogbowcloud.ras.util.RSAUtil;

import java.io.IOException;
import java.security.GeneralSecurityException;
import java.security.interfaces.RSAPublicKey;

public abstract class RASAuthenticationPlugin implements AuthenticationPlugin<FederationUserToken> {
    private static final Logger LOGGER = Logger.getLogger(RASAuthenticationPlugin.class);

    private String localProviderId;
    private RSAPublicKey publicKey;

    protected RASAuthenticationPlugin() {
        this.localProviderId = PropertiesHolder.getInstance().getProperty(ConfigurationConstants.LOCAL_MEMBER_ID);

        try {
            this.publicKey = getPublicKey();
        } catch (IOException | GeneralSecurityException e) {
            throw new FatalErrorException(Messages.Fatal.ERROR_READING_PUBLIC_KEY_FILE);
        }
    }

    public boolean isAuthentic(String requestingMember, FederationUserToken federationUserToken)  {
        if (federationUserToken.getTokenProvider().equals(this.localProviderId)) {
            String tokenMessage = getTokenMessage(federationUserToken);
            String signature = getSignature(federationUserToken);
            return verifySign(tokenMessage, signature);
        } else if (requestingMember.equals(federationUserToken.getTokenProvider().toLowerCase())) {
            // XMPP does not differentiate lower and upper cases, thus requestingMember is always all lower case
            return true;
        } else {
            return false;
        }
    }

    protected abstract String getTokenMessage(FederationUserToken federationUserToken);

    protected abstract String getSignature(FederationUserToken federationUserToken);

    private boolean verifySign(String tokenMessage, String signature) {
        try {
            return RSAUtil.verify(this.publicKey, tokenMessage, signature);
        } catch (Exception e) {
            LOGGER.error(Messages.Exception.INVALID_TOKEN_SIGNATURE, e);
            return false;
        }
    }

    protected RSAPublicKey getPublicKey() throws IOException, GeneralSecurityException {
        return RSAUtil.getPublicKey();
    }

}
