package org.fogbowcloud.manager.core.services;

import org.fogbowcloud.manager.core.models.token.Token;
import org.fogbowcloud.manager.core.plugins.IdentityPlugin;

public class AuthenticationService {
	
	private IdentityPlugin federationIdentityPlugin;
	
	AuthenticationService (IdentityPlugin federationIdentityPlugin){
		this.federationIdentityPlugin = federationIdentityPlugin;
	}
	
	public Token authenticate(String accessId) {
		return federationIdentityPlugin.getToken(accessId);
	}
	
}
