package org.fogbowcloud.manager.core.services;

import java.util.Date;
import java.util.Map;
import java.util.Properties;

import org.apache.log4j.Logger;
import org.fogbowcloud.manager.core.exceptions.UnauthenticatedException;
import org.fogbowcloud.manager.core.models.token.Token;
import org.fogbowcloud.manager.core.manager.plugins.AuthorizationPlugin;
import org.fogbowcloud.manager.core.manager.plugins.IdentityPlugin;
import org.fogbowcloud.manager.core.manager.plugins.identity.exceptions.UnauthorizedException;

// TODO change the name.
public class AuthenticationController {
	
	private static final Logger LOGGER = Logger.getLogger(AuthenticationController.class);

	private IdentityPlugin federationIdentityPlugin;
	private IdentityPlugin localIdentityPlugin;
	private AuthorizationPlugin authorizationPlugin;
	private Properties properties;
	
	public AuthenticationController(IdentityPlugin federationIdentityPlugin, IdentityPlugin localIdentityPlugin,
									AuthorizationPlugin authorizationPlugin, Properties properties){
		// TODO check if there are the local token properties
		this.federationIdentityPlugin = federationIdentityPlugin;
		this.localIdentityPlugin = localIdentityPlugin;
		this.authorizationPlugin = authorizationPlugin;
		this.properties = properties;
	}
	
	// TODO to use UnautheticatedException
	protected boolean authenticate(String federationTokenId) throws UnauthorizedException {
		LOGGER.debug("Trying authenticate the federation token id: " + federationTokenId);
		return this.federationIdentityPlugin.isValid(federationTokenId);
	}

	// TODO to use UnautheticatedException
	public Token getFederationToken(String federationTokenId) throws UnauthorizedException {
		LOGGER.debug("Trying to get the federation token by federation token id: " + federationTokenId);
		return this.federationIdentityPlugin.getToken(federationTokenId);
	}

	public Token getLocalToken(String localTokenId, String provadingMember) throws Exception {
		LOGGER.debug("Trying to get the local token by local token id: " + localTokenId);
		Token localToken = null;
		
		boolean isOrderProvidingLocal = AuthenticationControllerUtil
				.isOrderProvadingLocally(provadingMember, this.properties);
		if (isOrderProvidingLocal) {
			if (localTokenId == null) {
				localToken = createFogbowLocalToken();
			} else {
				localToken = getUserLocalToken(localTokenId);
			}
		} else {
			localToken = createTokenBypass(localTokenId); 			
		}
		return localToken;
	}

	protected Token createTokenBypass(String tokenId) {
		// TODO check if is necessary this Date. Check in the token object 
		Date date = new Date(); 
		return new Token(tokenId, null, date, null);
	}
	
	protected Token getUserLocalToken(String localTokenId) {
		try {
			return this.localIdentityPlugin.getToken(localTokenId);
		} catch (Exception e) {
			LOGGER.warn("Is not possible get token by user local token id: " + localTokenId, e);
			return createTokenBypass(localTokenId);
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
		return AuthenticationControllerUtil.getDefaultLocalTokenCredentials(this.properties);
	}

	protected boolean authorize(Token federationToken) throws UnauthorizedException {
		return this.authorizationPlugin.isAuthorized(federationToken);
	}
	
	// TODO think about this method. 
	public void authenticateAndAuthorize(String federationTokenId) throws UnauthorizedException, UnauthenticatedException {
		boolean isAuthenticated = authenticate(federationTokenId);
		if (!isAuthenticated) {
			throw new UnauthenticatedException("User not authenticated.");
		}
		Token federationToken = getFederationToken(federationTokenId);
		boolean isAuthorized = authorize(federationToken);
		if (!isAuthorized) {
			throw new UnauthorizedException("User not authorized.");
		}
	}
	
}
