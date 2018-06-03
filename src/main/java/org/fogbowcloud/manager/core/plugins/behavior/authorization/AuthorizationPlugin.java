package org.fogbowcloud.manager.core.plugins.behavior.authorization;

import org.fogbowcloud.manager.core.constants.Operation;
import org.fogbowcloud.manager.core.models.orders.Order;
import org.fogbowcloud.manager.core.models.instances.InstanceType;
import org.fogbowcloud.manager.core.models.token.FederationUser;

public interface AuthorizationPlugin {

    public boolean isAuthorized(FederationUser federationUser, Operation operation, Order order);

    public boolean isAuthorized(FederationUser federationUser, Operation operation, InstanceType type);
    
    public boolean isAuthorized(FederationUser federationUser, Operation operation);
}
