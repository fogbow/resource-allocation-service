package org.fogbowcloud.manager.core.controllers;

import java.util.Date;
import java.util.HashMap;

import org.fogbowcloud.manager.core.BaseUnitTests;
import org.fogbowcloud.manager.core.exceptions.OrderManagementException;
import org.fogbowcloud.manager.core.models.orders.ComputeOrder;
import org.fogbowcloud.manager.core.models.orders.Order;
import org.fogbowcloud.manager.core.models.orders.OrderType;
import org.fogbowcloud.manager.core.models.orders.UserData;
import org.fogbowcloud.manager.core.models.token.Token;
import org.fogbowcloud.manager.core.models.token.Token.User;
import org.fogbowcloud.manager.core.plugins.identity.exceptions.UnauthorizedException;
import org.fogbowcloud.manager.core.services.AuthenticationService;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;

public class ApplicationControllerTest extends BaseUnitTests {

	private ApplicationController applicationController;
	private AuthenticationService authenticationService;
	private OrdersManagerController ordersManagerController;

	@Before
	public void setUp() throws UnauthorizedException {
		this.authenticationService = Mockito.mock(AuthenticationService.class);
		this.ordersManagerController = Mockito.mock(OrdersManagerController.class);
		this.applicationController = ApplicationController.getInstance();
		this.applicationController.setAuthenticationController(this.authenticationService);
		this.applicationController.setOrdersManagerController(this.ordersManagerController);
	}

	@Test
	public void testDeleteComputeOrder() throws UnauthorizedException, OrderManagementException {
		Mockito.doNothing().when(this.ordersManagerController).deleteOrder(Mockito.any(Order.class));

		Order order = createComputeOrder();
		Mockito.doReturn(order).when(this.ordersManagerController).getOrderByIdAndType(Mockito.any(User.class),
				Mockito.anyString(), Mockito.any(OrderType.class));

		Mockito.doReturn(order.getFederationToken()).when(this.authenticationService)
				.authenticate(Mockito.anyString());

		this.applicationController.deleteOrder(order.getId(),
				order.getFederationToken().getAccessId(), OrderType.COMPUTE);
	}

	@Test(expected = OrderManagementException.class)
	public void testDeleteComputeOrderNotFound()
			throws UnauthorizedException, OrderManagementException {
		Mockito.doThrow(new OrderManagementException("Not found")).when(this.ordersManagerController)
				.deleteOrder(Mockito.any(Order.class));

		Order order = createComputeOrder();
		Mockito.doReturn(order).when(this.ordersManagerController).getOrderByIdAndType(Mockito.any(User.class),
				Mockito.anyString(), Mockito.any(OrderType.class));

		Mockito.doReturn(order.getFederationToken()).when(this.authenticationService)
				.authenticate(Mockito.anyString());

		this.applicationController.deleteOrder(order.getId(),
				order.getFederationToken().getAccessId(), OrderType.COMPUTE);
	}

	@Test(expected = UnauthorizedException.class)
	public void testDeleteComputeUnauthorized()
			throws UnauthorizedException, OrderManagementException {
		Order order = createComputeOrder();

		Mockito.doThrow(new UnauthorizedException()).when(this.authenticationService)
				.authenticate(Mockito.anyString());

		this.applicationController.deleteOrder(order.getId(),
				order.getFederationToken().getAccessId(), OrderType.COMPUTE);
	}

	private Order createComputeOrder() {
		User user = new User("fake-user-id", "fake-user-name");
		Token localToken = new Token("fake-accessId", user, new Date(),
				new HashMap<String, String>());
		Token federationToken = new Token("fake-accessId", user, new Date(),
				new HashMap<String, String>());

		Order order = new ComputeOrder(localToken, federationToken, "fake-member-id",
				"fake-member-id", 2, 2, 30, "fake-image-name", new UserData(), "fake-public-key");
		return order;
	}

}
