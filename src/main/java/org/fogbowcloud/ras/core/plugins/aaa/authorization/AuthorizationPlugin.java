package org.fogbowcloud.ras.core.plugins.aaa.authorization;

import org.fogbowcloud.ras.core.constants.Operation;
import org.fogbowcloud.ras.core.models.ResourceType;
import org.fogbowcloud.ras.core.models.tokens.FederationUserToken;

public interface AuthorizationPlugin {

    public boolean isAuthorized(FederationUserToken federationUserToken, Operation operation, ResourceType type);
}
