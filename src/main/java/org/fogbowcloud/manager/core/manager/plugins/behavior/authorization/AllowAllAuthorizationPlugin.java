package org.fogbowcloud.manager.core.manager.plugins.behavior.authorization;

import java.util.Properties;

import org.fogbowcloud.manager.core.manager.constants.Operation;
import org.fogbowcloud.manager.core.models.orders.Order;
import org.fogbowcloud.manager.core.models.orders.OrderType;
import org.fogbowcloud.manager.core.models.token.FederationUser;

public class AllowAllAuthorizationPlugin implements AuthorizationPlugin {

    public AllowAllAuthorizationPlugin(Properties properties) {}

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
		// TODO Auto-generated method stub
		return false;
	}
}
