package org.fogbowcloud.manager.core.services;

import java.util.Map;
import java.util.Properties;

import org.apache.log4j.Logger;
import org.fogbowcloud.manager.core.exceptions.UnauthenticatedException;
import org.fogbowcloud.manager.core.models.token.Token;
import org.fogbowcloud.manager.core.plugins.AuthorizationPlugin;
import org.fogbowcloud.manager.core.plugins.IdentityPlugin;
import org.fogbowcloud.manager.core.plugins.identity.exceptions.UnauthorizedException;

// TODO change the name. Fubica's suggestion is : "AAAController"
public class AuthenticationService {
	
	private static final Logger LOGGER = Logger.getLogger(AuthenticationService.class);

	private IdentityPlugin federationIdentityPlugin;
	private IdentityPlugin localIdentityPlugin;
	private AuthorizationPlugin authorizationPlugin;
	private Properties properties;
	
	AuthenticationService (IdentityPlugin federationIdentityPlugin, IdentityPlugin localIdentityPlugin, 
			AuthorizationPlugin authorizationPlugin, Properties properties){
		// TODO check if there are the local token properties
		this.federationIdentityPlugin = federationIdentityPlugin;
		this.localIdentityPlugin = localIdentityPlugin;
		this.authorizationPlugin = authorizationPlugin;
		this.properties = properties;
	}
	
	// TODO to use UnautheticatedException
	protected boolean authenticate(String accessId) throws UnauthorizedException {
		return this.federationIdentityPlugin.isValid(accessId);
	}

	// TODO to use UnautheticatedException
	public Token getFederationToken(String accessId) throws UnauthorizedException {
		return this.federationIdentityPlugin.getToken(accessId);
	}

	public Token getLocalToken(String localTokenId) throws Exception {
		if (localTokenId == null) {
			return createFogbowLocalToken();
		}
		
		return getUserLocalToken(localTokenId);
	}
	
	protected Token getUserLocalToken(String localTokenId) {
		try {
			return this.localIdentityPlugin.getToken(localTokenId);
		} catch (Exception e) {
			LOGGER.warn("Is not possible get token by user local token id: " + localTokenId, e);
			return new Token(localTokenId, null, null, null);
		}		
	}
	
	protected Token createFogbowLocalToken() throws Exception {
		try {
			Map<String, String> userCredentials = getDefaultUserCredentials();
			return this.localIdentityPlugin.createToken(userCredentials);
		} catch (Exception e) {
			String errorMsg = "Is not possible create Fogbow local token.";
			LOGGER.error(errorMsg, e);
			throw new Exception(errorMsg);
		}		
	}
	
	private Map<String, String> getDefaultUserCredentials() {		
		return AuthenticationServiceHelper.getLocalCredentials(this.properties);
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
