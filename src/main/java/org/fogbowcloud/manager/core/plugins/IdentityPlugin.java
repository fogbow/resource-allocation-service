package org.fogbowcloud.manager.core.plugins;

import java.util.Map;

import org.fogbowcloud.manager.core.exceptions.CreateTokenException;
import org.fogbowcloud.manager.core.models.Credential;
import org.fogbowcloud.manager.core.models.token.Token;

public interface IdentityPlugin {
	/**
	 * Creates a token based on the user's credentials.
	 * @param userCredentials
	 * @return a Token with an access ID provided by the identity service.
	 * @throws CreateTokenException 
	 */
	public Token createToken(Map<String, String> userCredentials) throws CreateTokenException;

	/**
	 * Some cloud middleware require tokens to be renewed before its
	 * expiration date. This method reissues a token against the 
	 * identity service.
	 * @param token
	 * @return a new Token based on the same credentials of the given token.
	 */
	public Token reIssueToken(Token token);

	/**
	 * Based on an access id recreates a Token containing the corresponding 
	 * access id plus the user name and some arbitrary information regarding
	 * the token.
	 * @param accessId
	 * @return a Token for the corresponding accessId.
	 */
	public Token getToken(String accessId);

	/**
	 * Verifies if the accessId is valid against the identity service.
	 * @param accessId
	 * @return a boolean stating whether the access id is valid or not.
	 */
	public boolean isValid(String accessId);
	
	/**
	 * @return an array of the required and optional fields for token creation.
	 */
	public Credential[] getCredentials();

	/**
	 * @return If the identity provider endpoint is different from the service one,
	 * then this method should return its address. It should return null otherwise.
	 */
	public String getAuthenticationURI();
	
	/**
	 * Creates a copy of a token containing only public data, e.g. the public certificate
	 * of the user.
	 * @param originalToken
	 * @return a publishable copy of the original token.
	 */
	public Token getForwardableToken(Token originalToken);
	
}
