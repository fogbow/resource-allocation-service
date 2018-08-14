package org.fogbowcloud.manager.core.plugins.behavior.authorization;

import org.fogbowcloud.manager.core.constants.Operation;
import org.fogbowcloud.manager.core.models.ResourceType;
import org.fogbowcloud.manager.core.models.tokens.FederationUserToken;

public interface AuthorizationPlugin {

    public boolean isAuthorized(FederationUserToken federationUserToken, Operation operation, ResourceType type);
}
