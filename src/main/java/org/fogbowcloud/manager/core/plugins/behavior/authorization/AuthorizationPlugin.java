package org.fogbowcloud.manager.core.plugins.behavior.authorization;

import org.fogbowcloud.manager.core.constants.Operation;
import org.fogbowcloud.manager.core.models.ResourceType;
import org.fogbowcloud.manager.core.models.tokens.FederationUserAttributes;

public interface AuthorizationPlugin {

    public boolean isAuthorized(FederationUserAttributes federationUserAttributes, Operation operation, ResourceType type);
}
