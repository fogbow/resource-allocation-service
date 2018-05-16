package org.fogbowcloud.manager.core.plugins;

import org.fogbowcloud.manager.core.models.token.Token;

public interface AuthorizationPlugin {

	// TODO check what is necessary for authorization
	public boolean isAuthorized(Token token);
	
}
