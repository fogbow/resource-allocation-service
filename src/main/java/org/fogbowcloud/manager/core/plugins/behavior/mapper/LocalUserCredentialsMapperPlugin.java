package org.fogbowcloud.manager.core.plugins.behavior.mapper;

import java.util.Map;

import org.fogbowcloud.manager.core.models.token.FederationUser;

public interface LocalUserCredentialsMapperPlugin {

	public Map<String, String> getCredentials(FederationUser federationUser);
	
}
