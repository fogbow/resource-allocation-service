package org.fogbowcloud.manager.core.manager.plugins.authorization;

import java.util.Properties;

import org.fogbowcloud.manager.core.manager.constants.Operation;
import org.fogbowcloud.manager.core.models.orders.Order;
import org.fogbowcloud.manager.core.models.orders.OrderType;
import org.fogbowcloud.manager.core.models.token.Token;
import org.fogbowcloud.manager.core.manager.plugins.AuthorizationPlugin;

public class AllowAllAuthorizationPlugin implements AuthorizationPlugin {

	public AllowAllAuthorizationPlugin(Properties properties) {
	}
	
	@Override
	public boolean isAuthorized(Token token, Operation operation, Order order) {
		return true;
	}

	@Override
	public boolean isAuthorized(Token token, Operation operation, OrderType type) {
		return true;
	}

}
