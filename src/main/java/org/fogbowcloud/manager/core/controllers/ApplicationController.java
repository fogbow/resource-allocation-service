package org.fogbowcloud.manager.core.controllers;

import org.fogbowcloud.manager.core.models.orders.Order;
import org.fogbowcloud.manager.core.models.orders.OrderType;
import org.fogbowcloud.manager.core.exceptions.OrderManagementException;
import org.fogbowcloud.manager.core.models.token.Token;
import org.fogbowcloud.manager.core.models.token.Token.User;
import org.fogbowcloud.manager.core.plugins.identity.exceptions.UnauthorizedException;
import org.fogbowcloud.manager.core.services.AuthenticationService;

public class  ApplicationController {

	private static ApplicationController instance;
	
	private AuthenticationService authenticationController;
	private OrdersManagerController ordersManagerController;

	private ApplicationController() {
		this.ordersManagerController = new OrdersManagerController();
	}

	public static ApplicationController getInstance() {
		synchronized (ApplicationController.class) {
			if (instance == null) {
				instance = new ApplicationController();
			}
			return instance;
		}
	}

	public Token authenticate(String accessId) throws UnauthorizedException {
		return this.authenticationController.authenticate(accessId);
	}

	public Order getOrder(String orderId, String accessId, OrderType orderType)
			throws UnauthorizedException {
		Token userFederationToken = this.authenticate(accessId);
		User user = userFederationToken.getUser();
		
		Order order = this.ordersManagerController.getOrderByIdAndType(user, orderId, orderType);
		return order;
	}

	public void deleteOrder(String orderId, String accessId, OrderType orderType)
			throws UnauthorizedException, OrderManagementException {
		Order order = getOrder(orderId, accessId, orderType);
		this.ordersManagerController.deleteOrder(order);
	}

	public void newOrderRequest(Order order, String accessId, String localTokenId)
			throws OrderManagementException, UnauthorizedException {
		Token federationToken = this.authenticationController.authenticate(accessId);
		Token localToken = createLocalToken(localTokenId);
		this.ordersManagerController.newOrderRequest(order, federationToken, localToken);
	}

	private Token createLocalToken(String localTokenId) {
		Token localToken = new Token();
		localToken.setAccessId(localTokenId);
		return localToken;
	}

	public void setAuthenticationController(AuthenticationService authenticationController) {
		this.authenticationController = authenticationController;
	}

	protected void setOrdersManagerController(OrdersManagerController ordersManagerController) {
		this.ordersManagerController = ordersManagerController;
	}

}
