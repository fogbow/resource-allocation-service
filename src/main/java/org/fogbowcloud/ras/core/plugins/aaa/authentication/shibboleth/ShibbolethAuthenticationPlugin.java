package org.fogbowcloud.ras.core.plugins.aaa.authentication.shibboleth;

import org.fogbowcloud.ras.core.models.tokens.FederationUserToken;
import org.fogbowcloud.ras.core.models.tokens.ShibbolethToken;
import org.fogbowcloud.ras.core.models.tokens.ShibbolethTokenHolder;
import org.fogbowcloud.ras.core.plugins.aaa.authentication.RASTimestampedAuthenticationPlugin;

public class ShibbolethAuthenticationPlugin extends RASTimestampedAuthenticationPlugin {

	public ShibbolethAuthenticationPlugin(String localProviderId) {
		super(localProviderId);
	}

	@Override
	protected String getTokenMessage(FederationUserToken federationUserToken) {
		ShibbolethToken shibbolethToken = (ShibbolethToken) federationUserToken;
		
		String samlAttributesStr = ShibbolethTokenHolder.normalizeSamlAttribute(shibbolethToken.getSamlAttributes());
		String expirationTimeStr = ShibbolethTokenHolder.normalizeExpirationTime(shibbolethToken.getTimestamp());
		
		String tokenValue = shibbolethToken.getTokenValue();
		String tokenProvider = shibbolethToken.getTokenProvider();
		String userId = shibbolethToken.getUserId();
		String userName = shibbolethToken.getUserName();
		return ShibbolethTokenHolder.createRawToken(tokenValue, tokenProvider, 
				userId, userName, samlAttributesStr, expirationTimeStr);
	}

	@Override
	protected String getSignature(FederationUserToken federationUserToken) {
		ShibbolethToken shibbolethToken = (ShibbolethToken) federationUserToken;
		return shibbolethToken.getSignature();
	}

}
