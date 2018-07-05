package org.fogbowcloud.manager.core.stubs;

import org.fogbowcloud.manager.core.constants.Operation;
import org.fogbowcloud.manager.core.models.instances.InstanceType;
import org.fogbowcloud.manager.core.models.orders.Order;
import org.fogbowcloud.manager.core.models.tokens.FederationUser;
import org.fogbowcloud.manager.core.plugins.behavior.authorization.AuthorizationPlugin;

/**
 * This class is a stub for the AuthorizationPlugin interface used for tests only.
 * Should not have a proper implementation.
 */
public class StubAuthorizationPlugin implements AuthorizationPlugin {

    public StubAuthorizationPlugin() {}
    
    @Override
    public boolean isAuthorized(FederationUser federationUser, Operation operation, Order order) {
        return false;
    }

    @Override
    public boolean isAuthorized(FederationUser federationUser, Operation operation,
            InstanceType type) {
        return false;
    }

    @Override
    public boolean isAuthorized(FederationUser federationUser, Operation operation) {
        return false;
    }

}
