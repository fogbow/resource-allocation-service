package org.fogbowcloud.manager.core.threads;

import java.util.Properties;

import org.fogbowcloud.manager.core.constants.ConfigurationConstants;
import org.fogbowcloud.manager.core.datastructures.SharedOrderHolders;
import org.fogbowcloud.manager.core.instanceprovider.InstanceProvider;
import org.fogbowcloud.manager.core.models.linkedList.ChainedList;
import org.fogbowcloud.manager.core.models.orders.ComputeOrder;
import org.fogbowcloud.manager.core.models.orders.Order;
import org.fogbowcloud.manager.core.models.orders.OrderState;
import org.fogbowcloud.manager.core.models.orders.UserData;
import org.fogbowcloud.manager.core.models.orders.instances.OrderInstance;
import org.fogbowcloud.manager.core.models.token.Token;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;

public class TestOpenProcessor {

	private OpenProcessor openProcessor;

	private InstanceProvider localInstanceProvider;
	private InstanceProvider remoteInstanceProvider;

	private Properties properties;

	@Before
	public void setUp() {
		String localMemberId = "local-member";
		this.properties = new Properties();
		this.properties.setProperty(ConfigurationConstants.XMPP_ID_KEY, localMemberId);

		this.localInstanceProvider = Mockito.mock(InstanceProvider.class);
		this.remoteInstanceProvider = Mockito.mock(InstanceProvider.class);

		this.openProcessor = Mockito
				.spy(new OpenProcessor(this.localInstanceProvider, this.remoteInstanceProvider, this.properties));
	}
	
	@After
	public void tearDown() {
		SharedOrderHolders sharedOrderHolders = SharedOrderHolders.getInstance();
		this.cleanList(sharedOrderHolders.getClosedOrdersList());
		this.cleanList(sharedOrderHolders.getFailedOrdersList());
		this.cleanList(sharedOrderHolders.getFulfilledOrdersList());
		this.cleanList(sharedOrderHolders.getOpenOrdersList());
		this.cleanList(sharedOrderHolders.getPendingOrdersList());
		this.cleanList(sharedOrderHolders.getSpawningOrdersList());
	}
	
	private void cleanList(ChainedList list) {
		list.resetPointer();
		Order order = null;
		do {
			order = list.getNext();
			if (order != null) {
				list.removeItem(order);
			}
		} while (order != null);
		list.resetPointer();
	}

	/**
	 * Test if the method processOpenOrder is setting to SPAWNING an Open Local
	 * Order when the requestInstance method of InstanceProvider returns an
	 * Instance.
	 * 
	 * @throws Exception
	 */
	@Test
	public void testProcessOpenLocalOrder() throws Exception {
		Order localOrder = this.createLocalOrder();

		OrderInstance orderInstance = new OrderInstance("fake-id");

		SharedOrderHolders sharedOrderHolders = SharedOrderHolders.getInstance();
		ChainedList openOrdersList = sharedOrderHolders.getOpenOrdersList();
		openOrdersList.addItem(localOrder);

		Mockito.doReturn(orderInstance).when(this.localInstanceProvider).requestInstance(Mockito.any(Order.class));

		Thread thread = new Thread(this.openProcessor);
		thread.start();

		Thread.sleep(1000);

		Assert.assertEquals(OrderState.SPAWNING, localOrder.getOrderState());

		// test if the open order list is empty and the spawningList is with the
		// localOrder
		ChainedList spawningOrdersList = sharedOrderHolders.getSpawningOrdersList();
		Assert.assertTrue(this.listIsEmpty(openOrdersList));
		Assert.assertSame(localOrder, spawningOrdersList.getNext());
		
		thread.interrupt();
	}

	/**
	 * Test if the method processOpenOrder is setting to FAILED an Open Local
	 * Order when the requestInstance method of InstanceProvider returns an
	 * Instance with an empty Id.
	 * 
	 * @throws Exception
	 */
	@Test
	public void testProcessOpenLocalOrderWithEmptyInstanceId() throws Exception {
		Order localOrder = this.createLocalOrder();

		OrderInstance orderInstance = new OrderInstance("");

		SharedOrderHolders sharedOrderHolders = SharedOrderHolders.getInstance();
		ChainedList openOrdersList = sharedOrderHolders.getOpenOrdersList();
		openOrdersList.addItem(localOrder);

		Mockito.doReturn(orderInstance).when(this.localInstanceProvider).requestInstance(Mockito.any(Order.class));

		Thread thread = new Thread(this.openProcessor);
		thread.start();

		Thread.sleep(1000);

		Assert.assertEquals(OrderState.FAILED, localOrder.getOrderState());

		// test if the open order list is empty and the failedList is with the
		// localOrder
		ChainedList failedOrdersList = sharedOrderHolders.getFailedOrdersList();
		Assert.assertTrue(this.listIsEmpty(openOrdersList));
		Assert.assertSame(localOrder, failedOrdersList.getNext());
		
		thread.interrupt();
	}

	/**
	 * Test if the method processOpenOrder is setting to FAILED an Open Local
	 * Order when the requestInstance method of InstanceProvider returns a null
	 * Instance.
	 * 
	 * @throws Exception
	 */
	@Test
	public void testProcessOpenLocalOrderWithNullInstance() throws Exception {
		Order localOrder = this.createLocalOrder();

		SharedOrderHolders sharedOrderHolders = SharedOrderHolders.getInstance();
		ChainedList openOrdersList = sharedOrderHolders.getOpenOrdersList();
		openOrdersList.addItem(localOrder);

		Mockito.doReturn(null).when(this.localInstanceProvider).requestInstance(Mockito.any(Order.class));

		Thread thread = new Thread(this.openProcessor);
		thread.start();

		Thread.sleep(1000);

		Assert.assertEquals(OrderState.FAILED, localOrder.getOrderState());

		// test if the open order list is empty and the failedList is with the
		// localOrder
		ChainedList failedOrdersList = sharedOrderHolders.getFailedOrdersList();
		Assert.assertTrue(this.listIsEmpty(openOrdersList));
		Assert.assertEquals(localOrder, failedOrdersList.getNext());
		
		thread.interrupt();
	}

	/**
	 * Test if the method processOpenOrder is setting to FAILED an Open Local
	 * Order when the requestInstance method of InstanceProvider throw an
	 * Exception.
	 * 
	 * @throws Exception
	 */
	@Test
	public void testProcessLocalOpenOrderRequestingException() throws Exception {
		Order localOrder = this.createLocalOrder();

		SharedOrderHolders sharedOrderHolders = SharedOrderHolders.getInstance();
		ChainedList openOrdersList = sharedOrderHolders.getOpenOrdersList();
		openOrdersList.addItem(localOrder);

		Mockito.doThrow(new RuntimeException("Any Exception")).when(this.localInstanceProvider)
				.requestInstance(Mockito.any(Order.class));

		Thread thread = new Thread(this.openProcessor);
		thread.start();

		Thread.sleep(1000);

		Assert.assertEquals(OrderState.FAILED, localOrder.getOrderState());

		// test if the open order list is empty and the failedList is with the
		// localOrder
		ChainedList failedOrdersList = sharedOrderHolders.getFailedOrdersList();
		Assert.assertTrue(this.listIsEmpty(openOrdersList));
		Assert.assertSame(localOrder, failedOrdersList.getNext());
		
		thread.interrupt();
	}

	/**
	 * Test if the method processOpenOrder is setting to PENDING an Open Remote
	 * Order.
	 * 
	 * @throws Exception
	 */
	@Test
	public void testProcessOpenRemoteOrder() throws Exception {
		Order remoteOrder = this.createRemoteOrder();

		SharedOrderHolders sharedOrderHolders = SharedOrderHolders.getInstance();
		ChainedList openOrdersList = sharedOrderHolders.getOpenOrdersList();
		openOrdersList.addItem(remoteOrder);

		Mockito.doReturn(null).when(this.remoteInstanceProvider).requestInstance(Mockito.any(Order.class));

		Thread thread = new Thread(this.openProcessor);
		thread.start();

		Thread.sleep(1000);

		Assert.assertEquals(OrderState.PENDING, remoteOrder.getOrderState());

		// test if the open order list is empty and the failedList is with the
		// localOrder
		ChainedList pendingOrdersList = sharedOrderHolders.getPendingOrdersList();
		Assert.assertTrue(this.listIsEmpty(openOrdersList));
		Assert.assertSame(remoteOrder, pendingOrdersList.getNext());
		
		thread.interrupt();
	}

	/**
	 * Test if the method processOpenOrder is setting to FAILED an Open Remote
	 * Order when the requestInstance method of Remote InstanceProvider throw an
	 * Exception.
	 * 
	 * @throws Exception
	 */
	@Test
	public void testProcessRemoteOpenOrderRequestingException() throws Exception {
		Order remoteOrder = this.createRemoteOrder();

		SharedOrderHolders sharedOrderHolders = SharedOrderHolders.getInstance();
		ChainedList openOrdersList = sharedOrderHolders.getOpenOrdersList();
		openOrdersList.addItem(remoteOrder);

		Mockito.doThrow(new RuntimeException("Any Exception")).when(this.remoteInstanceProvider)
				.requestInstance(Mockito.any(Order.class));

		Thread thread = new Thread(this.openProcessor);
		thread.start();

		Thread.sleep(1000);

		Assert.assertEquals(OrderState.FAILED, remoteOrder.getOrderState());

		// test if the open order list is empty and the failedList is with the
		// localOrder

		ChainedList failedOrdersList = sharedOrderHolders.getFailedOrdersList();
		Assert.assertTrue(this.listIsEmpty(openOrdersList));
		Assert.assertEquals(remoteOrder, failedOrdersList.getNext());
		
		thread.interrupt();
	}

	/**
	 * Test if the processOpenOrder does not process an Order that is not in the
	 * OPEN State.
	 * 
	 * @throws InterruptedException
	 */
	@Test
	public void testProcessNotOpenOrder() throws InterruptedException {
		Order order = this.createLocalOrder();

		SharedOrderHolders sharedOrderHolders = SharedOrderHolders.getInstance();
		ChainedList openOrdersList = sharedOrderHolders.getOpenOrdersList();
		openOrdersList.addItem(order);

		order.setOrderState(OrderState.PENDING);

		Thread thread = new Thread(this.openProcessor);
		thread.start();

		Thread.sleep(1000);

		Assert.assertEquals(OrderState.PENDING, order.getOrderState());
		
		thread.interrupt();
	}

	/**
	 * Test if the OpenProcessor is not processing an null Order.
	 * 
	 * @throws InterruptedException
	 */
	@Test
	public void testRunProcessWithNullOpenOrder() throws InterruptedException {
		Thread thread = new Thread(this.openProcessor);
		thread.start();

		Thread.sleep(1000);
		thread.interrupt();
	}

	/**
	 * This method tests a race condition when this class thread has the Order
	 * operation priority.
	 * 
	 * @throws Exception
	 */
	@Test
	public void testRaceConditionWithThisThreadPriority() throws Exception {
		Order localOrder = this.createLocalOrder();

		SharedOrderHolders sharedOrderHolders = SharedOrderHolders.getInstance();
		ChainedList openOrdersList = sharedOrderHolders.getOpenOrdersList();
		openOrdersList.addItem(localOrder);

		OrderInstance orderInstance = new OrderInstance("fake-id");

		Mockito.doReturn(orderInstance).when(this.localInstanceProvider).requestInstance(Mockito.any(Order.class));

		Thread thread;
		synchronized (localOrder) {
			thread = new Thread(this.openProcessor);
			thread.start();

			Thread.sleep(500);

			Assert.assertEquals(OrderState.OPEN, localOrder.getOrderState());
		}

		Thread.sleep(500);

		Assert.assertEquals(OrderState.SPAWNING, localOrder.getOrderState());
		
		thread.interrupt();
	}

	/**
	 * This method tests a race condition when this class thread has the Order
	 * operation priority and change the Open Order to a different State.
	 */
	@Test
	public void testRaceConditionWithThisThreadPriorityAndNotOpenOrder() throws InterruptedException {
		Order localOrder = this.createLocalOrder();

		SharedOrderHolders sharedOrderHolders = SharedOrderHolders.getInstance();
		ChainedList openOrdersList = sharedOrderHolders.getOpenOrdersList();
		openOrdersList.addItem(localOrder);

		Thread thread;
		synchronized (localOrder) {
			thread = new Thread(this.openProcessor);
			thread.start();

			Thread.sleep(500);

			localOrder.setOrderState(OrderState.CLOSED);
		}

		Thread.sleep(500);

		Assert.assertEquals(OrderState.CLOSED, localOrder.getOrderState());
		
		thread.interrupt();
	}

	/**
	 * This method tests a race condition when the Attend Open Order Thread has
	 * the Order operation priority.
	 * 
	 * @throws Exception
	 */
	@Test
	public void testRaceConditionWithOpenProcessorThreadPriority() throws Exception {
		Order localOrder = this.createLocalOrder();

		SharedOrderHolders sharedOrderHolders = SharedOrderHolders.getInstance();
		ChainedList openOrdersList = sharedOrderHolders.getOpenOrdersList();
		openOrdersList.addItem(localOrder);

		OrderInstance orderInstance = new OrderInstance("fake-id");

		Mockito.when(this.localInstanceProvider.requestInstance(Mockito.any(Order.class)))
				.thenAnswer(new Answer<OrderInstance>() {
					@Override
					public OrderInstance answer(InvocationOnMock invocation) throws InterruptedException {
						Thread.sleep(500);
						return orderInstance;
					}
				});

		Thread thread = new Thread(this.openProcessor);
		thread.start();

		Thread.sleep(400);

		synchronized (localOrder) {
			Thread.sleep(1000);
			Assert.assertEquals(OrderState.SPAWNING, localOrder.getOrderState());
			localOrder.setOrderState(OrderState.OPEN);
		}

		Assert.assertEquals(OrderState.OPEN, localOrder.getOrderState());
		
		thread.interrupt();
	}

	private Order createLocalOrder() {
		Token localToken = Mockito.mock(Token.class);
		Token federationToken = Mockito.mock(Token.class);
		UserData userData = Mockito.mock(UserData.class);
		String imageName = "fake-image-name";
		String requestingMember = String.valueOf(this.properties.get(ConfigurationConstants.XMPP_ID_KEY));
		String providingMember = String.valueOf(this.properties.get(ConfigurationConstants.XMPP_ID_KEY));
		Order localOrder = new ComputeOrder(localToken, federationToken, requestingMember, providingMember, 8, 1024, 30,
				imageName, userData);
		return localOrder;
	}

	private Order createRemoteOrder() {
		Token localToken = Mockito.mock(Token.class);
		Token federationToken = Mockito.mock(Token.class);
		UserData userData = Mockito.mock(UserData.class);
		String imageName = "fake-image-name";
		String requestingMember = String.valueOf(this.properties.get(ConfigurationConstants.XMPP_ID_KEY));
		String providingMember = "fake-remote-member";
		Order remoteOrder = new ComputeOrder(localToken, federationToken, requestingMember, providingMember, 8, 1024,
				30, imageName, userData);
		return remoteOrder;
	}

	private boolean listIsEmpty(ChainedList list) {
		list.resetPointer();
		return list.getNext() == null;
	}
}
