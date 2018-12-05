package org.fogbowcloud.ras.core.plugins.aaa.identity.shibboleth;

import org.apache.log4j.Logger;
import org.fogbowcloud.ras.core.constants.Messages;
import org.fogbowcloud.ras.core.exceptions.InvalidParameterException;
import org.fogbowcloud.ras.core.models.tokens.ShibbolethToken;
import org.fogbowcloud.ras.core.models.tokens.ShibbolethTokenHolder;
import org.fogbowcloud.ras.core.plugins.aaa.identity.FederationIdentityPlugin;

public class ShibbolethIdentityPlugin implements FederationIdentityPlugin<ShibbolethToken>{

	private static final Logger LOGGER = Logger.getLogger(ShibbolethIdentityPlugin.class);
	
	@Override
	public ShibbolethToken createToken(String tokenValue) throws InvalidParameterException {
		try {
			return ShibbolethTokenHolder.createShibbolethToken(tokenValue);			
		} catch (ShibbolethTokenHolder.CreateTokenException e) {
            LOGGER.error(String.format(Messages.Error.INVALID_TOKEN_VALUE, tokenValue), e);
            throw new InvalidParameterException();
		} 
		
	}
	
}
