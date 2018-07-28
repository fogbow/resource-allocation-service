package org.fogbowcloud.manager.core.stubs;

import org.fogbowcloud.manager.core.constants.Operation;
import org.fogbowcloud.manager.core.models.ResourceType;
import org.fogbowcloud.manager.core.models.tokens.FederationUser;
import org.fogbowcloud.manager.core.plugins.behavior.authorization.AuthorizationPlugin;

/**
 * This class is a stub for the AuthorizationPlugin interface used for tests only.
 * Should not have a proper implementation.
 */
public class StubAuthorizationPlugin implements AuthorizationPlugin {

    public StubAuthorizationPlugin() {}
    
    @Override
    public boolean isAuthorized(FederationUser federationUser, Operation operation, ResourceType type) {
        return true;
    }
}
