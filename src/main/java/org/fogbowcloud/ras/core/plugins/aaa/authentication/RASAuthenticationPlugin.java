package org.fogbowcloud.ras.core.plugins.aaa.authentication;

import org.apache.log4j.Logger;
import org.fogbowcloud.ras.core.PropertiesHolder;
import org.fogbowcloud.ras.core.constants.ConfigurationConstants;
import org.fogbowcloud.ras.core.constants.Messages;
import org.fogbowcloud.ras.core.models.tokens.FederationUserToken;
import org.fogbowcloud.ras.core.plugins.aaa.RASAuthenticationHolder;

public abstract class RASAuthenticationPlugin implements AuthenticationPlugin<FederationUserToken> {

	private static final Logger LOGGER = Logger.getLogger(RASAuthenticationPlugin.class);
	
    private String localProviderId;
	protected RASAuthenticationHolder rasAuthenticationHolder;

    public RASAuthenticationPlugin() {
        this.localProviderId = PropertiesHolder.getInstance().getProperty(ConfigurationConstants.LOCAL_MEMBER_ID);
        this.rasAuthenticationHolder = RASAuthenticationHolder.getInstance();
    }

    @Override
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

    protected boolean verifySign(String tokenMessage, String signature) {
        try {
        	return this.rasAuthenticationHolder.verifySignature(tokenMessage, signature);
        } catch (Exception e) {
            LOGGER.error(Messages.Exception.INVALID_TOKEN_SIGNATURE, e);
            return false;
        }
    }

}
