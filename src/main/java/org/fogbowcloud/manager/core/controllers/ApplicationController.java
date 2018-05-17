package org.fogbowcloud.manager.core.controllers;

import org.fogbowcloud.manager.core.exceptions.OrderManagementException;
import org.fogbowcloud.manager.core.exceptions.UnauthenticatedException;
import org.fogbowcloud.manager.core.models.orders.Order;
import org.fogbowcloud.manager.core.models.orders.OrderType;
import org.fogbowcloud.manager.core.models.token.Token;
import org.fogbowcloud.manager.core.plugins.identity.exceptions.UnauthorizedException;
import org.fogbowcloud.manager.core.services.AuthenticationController;

import java.util.List;

public class ApplicationController {

	private static ApplicationController instance;

	private AuthenticationController authenticationController;
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
		return this.authenticationController.getFederationToken(accessId);
	}

	public void deleteOrder(String orderId, String accessId, OrderType orderType)
			throws UnauthorizedException, OrderManagementException, UnauthenticatedException {
		Order order = this.getOrderById(orderId, accessId, orderType);
		this.ordersManagerController.deleteOrder(order);
	}

	public void setAuthenticationController(AuthenticationController authenticationController) {
		this.authenticationController = authenticationController;
	}

	protected void setOrdersManagerController(OrdersManagerController ordersManagerController) {
		this.ordersManagerController = ordersManagerController;
	}

	public List<Order> getAllComputes(String accessId, OrderType orderType) throws UnauthorizedException, UnauthenticatedException {
		this.authenticationController.authenticateAndAuthorize(accessId);
		Token federationToken = this.authenticationController.getFederationToken(accessId);
		return this.ordersManagerController.getAllOrdersByType(federationToken, orderType);
	}

	public Order getOrderById(String id, String accessId, OrderType orderType) throws UnauthorizedException, UnauthenticatedException {
		this.authenticationController.authenticateAndAuthorize(accessId);
		Token federationToken = this.authenticationController.getFederationToken(accessId);
		return this.ordersManagerController.getOrderByIdAndType(id, federationToken, orderType);
	}

	public void newOrderRequest(Order order, String accessId, String localTokenId)
			throws OrderManagementException, UnauthorizedException, UnauthenticatedException, Exception {
		this.authenticationController.authenticateAndAuthorize(accessId);
		Token federationToken = this.authenticationController.getFederationToken(accessId);
		String providingMember = order.getProvidingMember();
		Token localToken = this.authenticationController.getLocalToken(localTokenId, providingMember);
		this.ordersManagerController.newOrderRequest(order, federationToken, localToken);
	}
}
