package org.fogbowcloud.manager.core.controllers;

import java.util.Date;
import java.util.HashMap;

import org.fogbowcloud.manager.core.BaseUnitTests;
import org.fogbowcloud.manager.core.exceptions.OrderManagementException;
import org.fogbowcloud.manager.core.exceptions.UnauthenticatedException;
import org.fogbowcloud.manager.core.models.orders.ComputeOrder;
import org.fogbowcloud.manager.core.models.orders.Order;
import org.fogbowcloud.manager.core.models.orders.OrderType;
import org.fogbowcloud.manager.core.models.orders.UserData;
import org.fogbowcloud.manager.core.models.token.Token;
import org.fogbowcloud.manager.core.models.token.Token.User;
import org.fogbowcloud.manager.core.plugins.identity.exceptions.UnauthorizedException;
import org.fogbowcloud.manager.core.services.AuthenticationController;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;

public class ApplicationControllerTest extends BaseUnitTests {

	private ApplicationController applicationController;
	private AuthenticationController authenticationController;
	private OrdersManagerController ordersManagerController;

	@Before
	public void setUp() throws UnauthorizedException {
		this.authenticationController = Mockito.mock(AuthenticationController.class);
		this.ordersManagerController = Mockito.mock(OrdersManagerController.class);
		this.applicationController = ApplicationController.getInstance();
		this.applicationController.setAuthenticationController(this.authenticationController);
		this.applicationController.setOrdersManagerController(this.ordersManagerController);
	}

	@Test
	public void testDeleteComputeOrder() throws UnauthorizedException, OrderManagementException, UnauthenticatedException {
		Mockito.doNothing().when(this.ordersManagerController).deleteOrder(Mockito.any(Order.class));

		Order order = createComputeOrder();
		Mockito.doReturn(order).when(this.ordersManagerController).getOrderByIdAndType(
				Mockito.anyString(), Mockito.any(Token.class), Mockito.any(OrderType.class));

		Mockito.doReturn(order.getFederationToken()).when(this.authenticationController)
				.getFederationToken(Mockito.anyString());

		this.applicationController.deleteOrder(order.getId(), order.getFederationToken().getAccessId(),
				OrderType.COMPUTE);
	}

	@Test(expected = OrderManagementException.class)
	public void testDeleteComputeOrderNotFound() throws UnauthorizedException, OrderManagementException, UnauthenticatedException {
		Mockito.doThrow(new OrderManagementException("Not found")).when(this.ordersManagerController)
				.deleteOrder(Mockito.any(Order.class));

		Order order = createComputeOrder();
		Mockito.doReturn(order).when(this.ordersManagerController).getOrderByIdAndType(
				Mockito.anyString(), Mockito.any(Token.class), Mockito.any(OrderType.class));

		Mockito.doReturn(order.getFederationToken()).when(this.authenticationController)
				.getFederationToken(Mockito.anyString());

		this.applicationController.deleteOrder(order.getId(), order.getFederationToken().getAccessId(),
				OrderType.COMPUTE);
	}

	@Test(expected = UnauthorizedException.class)
	public void testDeleteComputeUnauthorized() throws UnauthorizedException, OrderManagementException, UnauthenticatedException {
		Order order = createComputeOrder();

		Mockito.doThrow(new UnauthorizedException()).when(this.authenticationController)
				.getFederationToken(Mockito.anyString());

		this.applicationController.deleteOrder(order.getId(), order.getFederationToken().getAccessId(),
				OrderType.COMPUTE);
	}

	private Order createComputeOrder() {
		User user = new User("fake-user-id", "fake-user-name");
		Token localToken = new Token("fake-accessId", user, new Date(), new HashMap<String, String>());
		Token federationToken = new Token("fake-accessId", user, new Date(), new HashMap<String, String>());

		Order order = new ComputeOrder(localToken, federationToken, "fake-member-id", "fake-member-id", 2, 2, 30,
				"fake-image-name", new UserData(), "fake-public-key");
		return order;
	}

}
