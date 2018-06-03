package org.fogbowcloud.manager.core.plugins.behavior.authorization;

import java.util.Properties;

import org.fogbowcloud.manager.core.constants.Operation;
import org.fogbowcloud.manager.core.models.orders.Order;
import org.fogbowcloud.manager.core.models.orders.OrderType;
import org.fogbowcloud.manager.core.models.token.FederationUser;

public class DefaultAuthorizationPlugin implements AuthorizationPlugin {

    public DefaultAuthorizationPlugin(Properties properties) {}

    @Override
    public boolean isAuthorized(FederationUser federationUser, Operation operation, Order order) {
        return true;
    }

    @Override
    public boolean isAuthorized(FederationUser federationUser, Operation operation, OrderType type) {
        return true;
    }

	@Override
	public boolean isAuthorized(FederationUser federationUser, Operation operation) {
        return true;
    }
}
