package org.fogbowcloud.manager.core.manager.plugins;

import org.fogbowcloud.manager.core.manager.constants.Operation;
import org.fogbowcloud.manager.core.models.orders.Order;
import org.fogbowcloud.manager.core.models.orders.OrderType;
import org.fogbowcloud.manager.core.models.token.Token;

public interface AuthorizationPlugin {

    public boolean isAuthorized(Token token, Operation operation, Order order);

    public boolean isAuthorized(Token token, Operation operation, OrderType type);
}
