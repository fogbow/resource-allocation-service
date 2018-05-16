package org.fogbowcloud.manager.core.services;

import org.fogbowcloud.manager.core.exceptions.UnauthenticatedException;
import org.fogbowcloud.manager.core.models.token.Token;
import org.fogbowcloud.manager.core.plugins.AuthorizationPlugin;
import org.fogbowcloud.manager.core.plugins.IdentityPlugin;
import org.fogbowcloud.manager.core.plugins.identity.exceptions.UnauthorizedException;

// TODO change the name. Fubica's suggestion is : "AAAController"
public class AuthenticationService {
	
	private IdentityPlugin federationIdentityPlugin;
	private AuthorizationPlugin authorizationPlugin;
	
	AuthenticationService (IdentityPlugin federationIdentityPlugin, AuthorizationPlugin authorizationPlugin){
		this.federationIdentityPlugin = federationIdentityPlugin;
		this.authorizationPlugin = authorizationPlugin;
	}
	
	// TODO to use UnautheticatedException
	protected boolean authenticate(String accessId) throws UnauthorizedException {
		return this.federationIdentityPlugin.isValid(accessId);
	}

	// TODO to use UnautheticatedException
	public Token getFederationToken(String accessId) throws UnauthorizedException {
		return this.federationIdentityPlugin.getToken(accessId);
	}

	// TODO to use UnautheticatedException
	public Token getLocalToken(String accessId) throws UnauthorizedException {
		return this.federationIdentityPlugin.getToken(accessId);
	}
	
	protected boolean authorize(Token federationToken) throws UnauthorizedException {
		return this.authorizationPlugin.isAuthorized(federationToken);
	}
	
	public void AutenticateAndAuthorize(String accessId) throws UnauthorizedException, UnauthenticatedException {
		boolean isAuthenticated = authenticate(accessId);
		if (!isAuthenticated) {
			throw new UnauthenticatedException("User not authenticated.");
		}
		Token federationToken = getFederationToken(accessId);
		boolean isAuthorized = authorize(federationToken);
		if (!isAuthorized) {
			throw new UnauthorizedException("User not authorized.");
		}
	}
	
}
