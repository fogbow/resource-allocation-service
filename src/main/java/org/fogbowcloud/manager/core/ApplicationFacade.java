package org.fogbowcloud.manager.core;

import org.fogbowcloud.manager.core.exceptions.OrderManagementException;
import org.fogbowcloud.manager.core.exceptions.UnauthenticatedException;
import org.fogbowcloud.manager.core.models.orders.Order;
import org.fogbowcloud.manager.core.models.orders.OrderType;
import org.fogbowcloud.manager.core.models.token.Token;
import org.fogbowcloud.manager.core.manager.plugins.identity.exceptions.UnauthorizedException;
import org.fogbowcloud.manager.core.services.AuthenticationController;

import java.util.List;

public class ApplicationFacade {

	private static ApplicationFacade instance;

	private AuthenticationController authenticationController;
	private OrderController orderController;

	private ApplicationFacade() {
		this.orderController = new OrderController();
	}

	public static ApplicationFacade getInstance() {
		synchronized (ApplicationFacade.class) {
			if (instance == null) {
				instance = new ApplicationFacade();
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
		this.orderController.deleteOrder(order);
	}

	public void setAuthenticationController(AuthenticationController authenticationController) {
		this.authenticationController = authenticationController;
	}

	protected void setOrderController(OrderController orderController) {
		this.orderController = orderController;
	}

	public List<Order> getAllComputes(String accessId, OrderType orderType) throws UnauthorizedException, UnauthenticatedException {
		this.authenticationController.authenticateAndAuthorize(accessId);
		Token federationToken = this.authenticationController.getFederationToken(accessId);
		return this.orderController.getAllOrdersByType(federationToken, orderType);
	}

	public Order getOrderById(String id, String accessId, OrderType orderType) throws UnauthorizedException, UnauthenticatedException {
		this.authenticationController.authenticateAndAuthorize(accessId);
		Token federationToken = this.authenticationController.getFederationToken(accessId);
		return this.orderController.getOrderByIdAndType(id, federationToken, orderType);
	}

	public void newOrderRequest(Order order, String accessId, String localTokenId)
			throws OrderManagementException, UnauthorizedException, UnauthenticatedException, Exception {
		this.authenticationController.authenticateAndAuthorize(accessId);
		Token federationToken = this.authenticationController.getFederationToken(accessId);
		String providingMember = order.getProvidingMember();
		Token localToken = this.authenticationController.getLocalToken(localTokenId, providingMember);
		this.orderController.newOrderRequest(order, federationToken, localToken);
	}
}
