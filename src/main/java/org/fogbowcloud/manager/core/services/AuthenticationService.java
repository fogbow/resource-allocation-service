package org.fogbowcloud.manager.core.services;

import org.fogbowcloud.manager.core.models.token.Token;
import org.fogbowcloud.manager.core.plugins.IdentityPlugin;
import org.fogbowcloud.manager.core.plugins.identity.exceptions.UnauthorizedException;

public class AuthenticationService {
	
	private IdentityPlugin federationIdentityPlugin;
	
	public AuthenticationService (IdentityPlugin federationIdentityPlugin){
		this.federationIdentityPlugin = federationIdentityPlugin;
	}
	
	public Token authenticate(String accessId) throws UnauthorizedException {
		return federationIdentityPlugin.getToken(accessId);
	}
	
}
