package org.fogbowcloud.manager.core.services;

import java.util.Date;
import java.util.Map;
import java.util.HashMap;

import org.fogbowcloud.manager.core.BaseUnitTests;
import org.fogbowcloud.manager.core.datastructures.SharedOrderHolders;
import org.fogbowcloud.manager.core.exceptions.OrderStateTransitionException;
import org.fogbowcloud.manager.core.models.linkedList.ChainedList;
import org.fogbowcloud.manager.core.models.orders.ComputeOrder;
import org.fogbowcloud.manager.core.models.orders.Order;
import org.fogbowcloud.manager.core.models.orders.OrderState;
import org.fogbowcloud.manager.core.models.token.Token;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;

public class OrdersServiceTest extends BaseUnitTests {
	
	private SharedOrderHolders sharedOrderHolders;
	private OrdersService ordersService;

	@Before
	public void setUp() {
		this.sharedOrderHolders = SharedOrderHolders.getInstance();
		this.ordersService = Mockito.spy(new OrdersService());
	}
	
	@After
	public void tearDown() {
		super.tearDown();
	}
	
	@Test (expected = OrderStateTransitionException.class)
	public void testThrowOrderStateTransitionException() {
		String fakeId = "fake-id";
		ComputeOrder computeOrder = this.createComputeOrder(fakeId, OrderState.OPEN);
		
		Token.User fakeUser = this.createFakeUser();
		
		Token token = this.createFakeToken();
		token.setUser(fakeUser);
		
		Map<String, Order> activeOrdersMap = this.sharedOrderHolders.getActiveOrdersMap();
		activeOrdersMap.put(fakeId, computeOrder);
		
		Mockito.doThrow(OrderStateTransitionException.class).when(this.ordersService).deleteOrder(computeOrder, token);
		
		this.ordersService.deleteOrder(computeOrder, token);	
	}
	
	@Test
	public void testDeleteOrderStateClosed() {
		String fakeId = "fake-id";
		ComputeOrder computeOrder = this.createComputeOrder(fakeId, OrderState.CLOSED);
		
		Token.User fakeUser = this.createFakeUser();
		
		Token token = this.createFakeToken();
		token.setUser(fakeUser);
		
		Map<String, Order> activeOrdersMap = this.sharedOrderHolders.getActiveOrdersMap();
		activeOrdersMap.put(fakeId, computeOrder);
		
		ChainedList closedOrdersList = this.sharedOrderHolders.getClosedOrdersList();
		closedOrdersList.addItem(computeOrder);
		
		this.ordersService.deleteOrder(computeOrder, token);
		
		Order test = closedOrdersList.getNext();
		Assert.assertNotNull(test);
        Assert.assertEquals(computeOrder, test);
        Assert.assertEquals(OrderState.CLOSED, test.getOrderState());		
	}
	
	@Test
	public void testDeleteOrderStateFailed() {
		String fakeId = "fake-id";
		ComputeOrder computeOrder = this.createComputeOrder(fakeId, OrderState.FAILED);
		
		Token.User fakeUser = this.createFakeUser();
		
		Token token = this.createFakeToken();
		token.setUser(fakeUser);
		
		Map<String, Order> activeOrdersMap = this.sharedOrderHolders.getActiveOrdersMap();
		activeOrdersMap.put(fakeId, computeOrder);
		
		ChainedList failedOrdersList = this.sharedOrderHolders.getFailedOrdersList();
		failedOrdersList.addItem(computeOrder);
		
		ChainedList closedOrdersList = this.sharedOrderHolders.getClosedOrdersList();
		Assert.assertNull(closedOrdersList.getNext());
		
		this.ordersService.deleteOrder(computeOrder, token);
		
		Order test = closedOrdersList.getNext();
		Assert.assertNotNull(test);
        Assert.assertEquals(computeOrder, test);
        Assert.assertEquals(OrderState.CLOSED, test.getOrderState());		
	}
	
	@Test
	public void testDeleteOrderStateFulfilled() {
		String fakeId = "fake-id";
		ComputeOrder computeOrder = this.createComputeOrder(fakeId, OrderState.FULFILLED);
		
		Token.User fakeUser = this.createFakeUser();
		
		Token token = this.createFakeToken();
		token.setUser(fakeUser);
		
		Map<String, Order> activeOrdersMap = this.sharedOrderHolders.getActiveOrdersMap();
		activeOrdersMap.put(fakeId, computeOrder);
		
		ChainedList fulfilledOrdersList = this.sharedOrderHolders.getFulfilledOrdersList();
		fulfilledOrdersList.addItem(computeOrder);
		
		ChainedList closedOrdersList = this.sharedOrderHolders.getClosedOrdersList();
		Assert.assertNull(closedOrdersList.getNext());
		
		this.ordersService.deleteOrder(computeOrder, token);
		
		Order test = closedOrdersList.getNext();
		Assert.assertNotNull(test);
        Assert.assertEquals(computeOrder, test);
        Assert.assertEquals(OrderState.CLOSED, test.getOrderState());		
	}
	
	@Test
	public void testDeleteOrderStateSpawning() {
		String fakeId = "fake-id";
		ComputeOrder computeOrder = this.createComputeOrder(fakeId, OrderState.SPAWNING);
		
		Token.User fakeUser = this.createFakeUser();
		
		Token token = this.createFakeToken();
		token.setUser(fakeUser);
		
		Map<String, Order> activeOrdersMap = this.sharedOrderHolders.getActiveOrdersMap();
		activeOrdersMap.put(fakeId, computeOrder);
		
		ChainedList spawningOrdersList = this.sharedOrderHolders.getSpawningOrdersList();
		spawningOrdersList.addItem(computeOrder);
		
		ChainedList closedOrdersList = this.sharedOrderHolders.getClosedOrdersList();
		Assert.assertNull(closedOrdersList.getNext());
		
		this.ordersService.deleteOrder(computeOrder, token);
		
		Order test = closedOrdersList.getNext();
		Assert.assertNotNull(test);
        Assert.assertEquals(computeOrder, test);
        Assert.assertEquals(OrderState.CLOSED, test.getOrderState());		
	}
	
	@Test
	public void testDeleteOrderStatePending() {
		String fakeId = "fake-id";
		ComputeOrder computeOrder = this.createComputeOrder(fakeId, OrderState.PENDING);
		
		Token.User fakeUser = this.createFakeUser();
		
		Token token = this.createFakeToken();
		token.setUser(fakeUser);
		
		Map<String, Order> activeOrdersMap = this.sharedOrderHolders.getActiveOrdersMap();
		activeOrdersMap.put(fakeId, computeOrder);
		
		ChainedList pendingOrdersList = this.sharedOrderHolders.getPendingOrdersList();
		pendingOrdersList.addItem(computeOrder);
		
		ChainedList closedOrdersList = this.sharedOrderHolders.getClosedOrdersList();
		Assert.assertNull(closedOrdersList.getNext());
		
		this.ordersService.deleteOrder(computeOrder, token);
		
		Order test = closedOrdersList.getNext();
		Assert.assertNotNull(test);
        Assert.assertEquals(computeOrder, test);
        Assert.assertEquals(OrderState.CLOSED, test.getOrderState());		
	}
	
	@Test
	public void testDeleteOrderStateOpen() {
		String fakeId = "fake-id";
		ComputeOrder computeOrder = this.createComputeOrder(fakeId, OrderState.OPEN);
		
		Token.User fakeUser = this.createFakeUser();
		
		Token token = this.createFakeToken();
		token.setUser(fakeUser);
		
		Map<String, Order> activeOrdersMap = this.sharedOrderHolders.getActiveOrdersMap();
		activeOrdersMap.put(fakeId, computeOrder);
		
		ChainedList openOrdersList = this.sharedOrderHolders.getOpenOrdersList();
		openOrdersList.addItem(computeOrder);
		
		ChainedList closedOrdersList = this.sharedOrderHolders.getClosedOrdersList();
		Assert.assertNull(closedOrdersList.getNext());
		
		this.ordersService.deleteOrder(computeOrder, token);
		
		Order test = closedOrdersList.getNext();
		Assert.assertNotNull(test);
        Assert.assertEquals(computeOrder, test);
        Assert.assertEquals(OrderState.CLOSED, test.getOrderState());		
	}
	
	private Token createFakeToken() {
        String fakeAccessId = "fake-access-id";
        Token.User fakeUser = this.createFakeUser();
        Date fakeExpirationTime = new Date();
        Map<String, String> fakeAttributes = new HashMap<>();
        return  new Token(fakeAccessId, fakeUser, fakeExpirationTime, fakeAttributes);
    }

	private Token.User createFakeUser() {
		String fakeUserId = "fake-user-id";
        String fakeUserName = "fake-user-name";
        Token.User fakeUser = new Token.User(fakeUserId, fakeUserName);
		return fakeUser;
	}
	
	private ComputeOrder createComputeOrder(String id, OrderState orderState) {
		ComputeOrder computeOrder = Mockito.spy(ComputeOrder.class);
		computeOrder.setOrderState(orderState);
		computeOrder.setLocalToken(this.createFakeToken());
		return computeOrder;
	}
}
