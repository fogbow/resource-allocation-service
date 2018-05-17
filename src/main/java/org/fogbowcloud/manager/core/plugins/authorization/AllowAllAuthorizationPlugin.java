package org.fogbowcloud.manager.core.plugins.authorization;

import java.util.Properties;

import org.fogbowcloud.manager.core.models.token.Token;
import org.fogbowcloud.manager.core.plugins.AuthorizationPlugin;

public class AllowAllAuthorizationPlugin implements AuthorizationPlugin {

	public AllowAllAuthorizationPlugin(Properties properties) {
		// Do Nothing
	}
	
	@Override
	public boolean isAuthorized(Token token) {
		return true;
	}
	
}
