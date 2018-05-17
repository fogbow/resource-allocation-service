package org.fogbowcloud.manager.core.manager.plugins;

import org.fogbowcloud.manager.core.models.token.Token;

public interface AuthorizationPlugin {

	// TODO check what is necessary for authorization. Token ?
	public boolean isAuthorized(Token token);
	
}
