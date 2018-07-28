package org.fogbowcloud.manager.core.plugins.behavior.authorization;

import org.fogbowcloud.manager.core.constants.Operation;
import org.fogbowcloud.manager.core.models.ResourceType;
import org.fogbowcloud.manager.core.models.tokens.FederationUser;

public class DefaultAuthorizationPlugin implements AuthorizationPlugin {

    public DefaultAuthorizationPlugin() {}

    @Override
    public boolean isAuthorized(FederationUser federationUser, Operation operation, ResourceType type) {
        return true;
    }
}
