package org.fogbowcloud.manager.core;

import java.util.Date;
import java.util.HashMap;

import org.fogbowcloud.manager.core.exceptions.OrderManagementException;
import org.fogbowcloud.manager.core.exceptions.UnauthenticatedException;
import org.fogbowcloud.manager.core.models.orders.ComputeOrder;
import org.fogbowcloud.manager.core.models.orders.Order;
import org.fogbowcloud.manager.core.models.orders.OrderType;
import org.fogbowcloud.manager.core.models.orders.UserData;
import org.fogbowcloud.manager.core.manager.plugins.identity.exceptions.UnauthorizedException;
import org.fogbowcloud.manager.core.models.token.Token;
import org.fogbowcloud.manager.core.services.AAAController;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;

public class ApplicationControllerTest extends BaseUnitTests {

//	private ApplicationFacade applicationController;
//	private AAAController AAAController;
//	private OrderController ordersManagerController;
//
//	@Before
//	public void setUp() throws UnauthorizedException {
//		this.AAAController = Mockito.mock(AAAController.class);
//		this.ordersManagerController = Mockito.mock(OrderController.class);
//		this.applicationController = ApplicationFacade.getInstance();
//		this.applicationController.setAAAController(this.AAAController);
//		this.applicationController.setOrderController(this.ordersManagerController);
//	}
//
//	@Test
//	public void testDeleteComputeOrder() throws UnauthorizedException, OrderManagementException, UnauthenticatedException {
//		Mockito.doNothing().when(this.ordersManagerController).deleteOrder(Mockito.any(Order.class));
//
//		Order order = createComputeOrder();
//		Mockito.doReturn(order).when(this.ordersManagerController).getOrder(
//				Mockito.anyString(), Mockito.any(Token.class), Mockito.any(OrderType.class));
//
//		Mockito.doReturn(order.getFederationToken()).when(this.AAAController)
//				.getFederationToken(Mockito.anyString());
//
//		this.applicationController.deleteCompute(order.getId(), order.getFederationToken().getAccessId());
//	}
//
//	@Test(expected = OrderManagementException.class)
//	public void testDeleteComputeOrderNotFound() throws UnauthorizedException, OrderManagementException, UnauthenticatedException {
//		Mockito.doThrow(new OrderManagementException("Not found")).when(this.ordersManagerController)
//				.deleteOrder(Mockito.any(Order.class));
//
//		Order order = createComputeOrder();
//		Mockito.doReturn(order).when(this.ordersManagerController).getOrder(
//				Mockito.anyString(), Mockito.any(Token.class), Mockito.any(OrderType.class));
//
//		Mockito.doReturn(order.getFederationToken()).when(this.AAAController)
//				.getFederationToken(Mockito.anyString());
//
//		this.applicationController.deleteCompute(order.getId(), order.getFederationToken().getAccessId());
//	}
//
//	@Test(expected = UnauthorizedException.class)
//	public void testDeleteComputeUnauthorized() throws UnauthorizedException, OrderManagementException, UnauthenticatedException {
//		Order order = createComputeOrder();
//
//		Mockito.doThrow(new UnauthorizedException()).when(this.AAAController)
//				.getFederationToken(Mockito.anyString());
//
//		this.applicationController.deleteCompute(order.getId(), order.getFederationToken().getAccessId());
//	}
//
//	private Order createComputeOrder() {
//		Token.User user = new Token.User("fake-user-id", "fake-user-name");
//		Token localToken = new Token("fake-accessId", user, new Date(), new HashMap<String, String>());
//		Token federationToken = new Token("fake-accessId", user, new Date(), new HashMap<String, String>());
//
//		Order order = new ComputeOrder(localToken, federationToken, "fake-member-id", "fake-member-id", 2, 2, 30,
//				"fake-image-name", new UserData(), "fake-public-key");
//		return order;
//	}

}
