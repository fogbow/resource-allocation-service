package org.fogbowcloud.manager.core.manager.plugins;

import org.fogbowcloud.manager.core.manager.constants.Operation;
import org.fogbowcloud.manager.core.models.orders.Order;
import org.fogbowcloud.manager.core.models.orders.OrderType;
import org.fogbowcloud.manager.core.models.token.FederationUser;

public interface AuthorizationPlugin {

    public boolean isAuthorized(FederationUser federationUser, Operation operation, Order order);

    public boolean isAuthorized(FederationUser federationUser, Operation operation, OrderType type);
    
    public boolean isAuthorized(FederationUser federationUser, Operation operation);
}
