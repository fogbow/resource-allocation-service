package org.fogbowcloud.manager.core.services;

import java.util.Date;
import java.util.Map;
import java.util.HashMap;

import org.fogbowcloud.manager.core.BaseUnitTests;
import org.fogbowcloud.manager.core.datastructures.SharedOrderHolders;
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

public class ComputeOrdersServiceTest extends BaseUnitTests {

	private ComputeOrdersService computeOrdersService;
	private Map<String, Order> activeOrdersMap;
	private ChainedList openOrdersList;
	private ChainedList pendingOrdersList;
	private ChainedList spawningOrdersList;
	private ChainedList fulfilledOrdersList;
	private ChainedList failedOrdersList;
	private ChainedList closedOrdersList;

	@Before
	public void setUp() {
		SharedOrderHolders sharedOrderHolders = SharedOrderHolders.getInstance();
		this.activeOrdersMap = sharedOrderHolders.getActiveOrdersMap();
		this.openOrdersList = sharedOrderHolders.getOpenOrdersList();
		this.pendingOrdersList = sharedOrderHolders.getPendingOrdersList();
		this.spawningOrdersList = sharedOrderHolders.getSpawningOrdersList();
		this.fulfilledOrdersList = sharedOrderHolders.getFulfilledOrdersList();
		this.failedOrdersList = sharedOrderHolders.getFailedOrdersList();
		this.closedOrdersList = sharedOrderHolders.getClosedOrdersList();
		this.computeOrdersService = Mockito.spy(new ComputeOrdersService());
	}

	@After
	public void tearDown() {
		super.tearDown();
	}

	@Test
	public void testDeleteOrderStateClosed() {
		String orderId = getComputeOrderCreationId(OrderState.CLOSED);
		ComputeOrder computeOrder = (ComputeOrder) activeOrdersMap.get(orderId);

		this.computeOrdersService.deleteOrder(computeOrder);

		Order test = this.closedOrdersList.getNext();
		Assert.assertNotNull(test);
		Assert.assertEquals(computeOrder, test);
		Assert.assertEquals(OrderState.CLOSED, test.getOrderState());
	}

	@Test
	public void testDeleteOrderStateFailed() {
		String orderId = getComputeOrderCreationId(OrderState.FAILED);
		ComputeOrder computeOrder = (ComputeOrder) activeOrdersMap.get(orderId);

		Assert.assertNull(this.closedOrdersList.getNext());

		this.computeOrdersService.deleteOrder(computeOrder);

		Order test = this.closedOrdersList.getNext();
		Assert.assertNotNull(test);
		Assert.assertEquals(computeOrder, test);
		Assert.assertEquals(OrderState.CLOSED, test.getOrderState());
	}

	@Test
	public void testDeleteOrderStateFulfilled() {
		String orderId = getComputeOrderCreationId(OrderState.FULFILLED);
		ComputeOrder computeOrder = (ComputeOrder) activeOrdersMap.get(orderId);

		Assert.assertNull(this.closedOrdersList.getNext());

		this.computeOrdersService.deleteOrder(computeOrder);

		Order test = this.closedOrdersList.getNext();
		Assert.assertNotNull(test);
		Assert.assertEquals(computeOrder, test);
		Assert.assertEquals(OrderState.CLOSED, test.getOrderState());
	}

	@Test
	public void testDeleteOrderStateSpawning() {
		String orderId = getComputeOrderCreationId(OrderState.SPAWNING);
		ComputeOrder computeOrder = (ComputeOrder) activeOrdersMap.get(orderId);

		Assert.assertNull(this.closedOrdersList.getNext());

		this.computeOrdersService.deleteOrder(computeOrder);

		Order test = this.closedOrdersList.getNext();
		Assert.assertNotNull(test);
		Assert.assertEquals(computeOrder, test);
		Assert.assertEquals(OrderState.CLOSED, test.getOrderState());
	}

	@Test
	public void testDeleteOrderStatePending() {
		String orderId = getComputeOrderCreationId(OrderState.PENDING);
		ComputeOrder computeOrder = (ComputeOrder) activeOrdersMap.get(orderId);

		Assert.assertNull(this.closedOrdersList.getNext());

		this.computeOrdersService.deleteOrder(computeOrder);

		Order test = this.closedOrdersList.getNext();
		Assert.assertNotNull(test);
		Assert.assertEquals(computeOrder, test);
		Assert.assertEquals(OrderState.CLOSED, test.getOrderState());
	}

	@Test
	public void testDeleteOrderStateOpen() {
		String orderId = getComputeOrderCreationId(OrderState.OPEN);
		ComputeOrder computeOrder = (ComputeOrder) activeOrdersMap.get(orderId);

		Assert.assertNull(this.closedOrdersList.getNext());

		this.computeOrdersService.deleteOrder(computeOrder);

		Order test = this.closedOrdersList.getNext();
		Assert.assertNotNull(test);
		Assert.assertEquals(computeOrder, test);
		Assert.assertEquals(OrderState.CLOSED, test.getOrderState());
	}

	private Token createToken() {
		String accessId = "fake-access-id";
		Token.User tokenUser = this.createTokenUser();
		Date expirationTime = new Date();
		Map<String, String> attributesMap = new HashMap<>();
		return new Token(accessId, tokenUser, expirationTime, attributesMap);
	}

	private Token.User createTokenUser() {
		String tokenUserId = "fake-user-id";
		String tokenUserName = "fake-user-name";
		Token.User tokenUser = new Token.User(tokenUserId, tokenUserName);
		return tokenUser;
	}

	private String getComputeOrderCreationId(OrderState orderState) {
		String orderId = null;

		Token token = this.createToken();

		ComputeOrder computeOrder = Mockito.spy(new ComputeOrder());
		computeOrder.setLocalToken(token);
		computeOrder.setFederationToken(token);
		computeOrder.setOrderState(orderState);

		orderId = computeOrder.getId();

		this.activeOrdersMap.put(orderId, computeOrder);

		switch (orderState) {
		case OPEN:
			this.openOrdersList.addItem(computeOrder);
			break;
		case PENDING:
			this.pendingOrdersList.addItem(computeOrder);
			break;
		case SPAWNING:
			this.spawningOrdersList.addItem(computeOrder);
			break;
		case FULFILLED:
			this.fulfilledOrdersList.addItem(computeOrder);
			break;
		case FAILED:
			this.failedOrdersList.addItem(computeOrder);
			break;
		case CLOSED:
			this.closedOrdersList.addItem(computeOrder);
		}

		return orderId;
	}
}
