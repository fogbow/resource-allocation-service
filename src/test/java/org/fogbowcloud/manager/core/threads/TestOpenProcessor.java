package org.fogbowcloud.manager.core.threads;

import java.util.Properties;

import org.fogbowcloud.manager.core.constants.ConfigurationConstants;
import org.fogbowcloud.manager.core.instanceprovider.InstanceProvider;
import org.fogbowcloud.manager.core.models.linkedList.ChainedList;
import org.fogbowcloud.manager.core.models.orders.ComputeOrder;
import org.fogbowcloud.manager.core.models.orders.Order;
import org.fogbowcloud.manager.core.models.orders.OrderState;
import org.fogbowcloud.manager.core.models.orders.UserData;
import org.fogbowcloud.manager.core.models.orders.instances.OrderInstance;
import org.fogbowcloud.manager.core.models.token.Token;
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

	private ChainedList openOrdersList;
	private ChainedList pendingOrdersList;
	private ChainedList failedOrdersList;
	private ChainedList spawningOrdersList;

	private Properties properties;

	@Before
	public void setUp() {
		String localMemberId = "local-member";
		this.properties = new Properties();
		this.properties.setProperty(ConfigurationConstants.XMPP_ID_KEY, localMemberId);

		this.localInstanceProvider = Mockito.mock(InstanceProvider.class);
		this.remoteInstanceProvider = Mockito.mock(InstanceProvider.class);

		this.openOrdersList = Mockito.mock(ChainedList.class);
		this.pendingOrdersList = Mockito.mock(ChainedList.class);
		this.failedOrdersList = Mockito.mock(ChainedList.class);
		this.spawningOrdersList = Mockito.mock(ChainedList.class);

		this.openProcessor = Mockito.spy(new OpenProcessor(this.localInstanceProvider, this.remoteInstanceProvider,
				localMemberId, this.properties));
		this.openProcessor.setOpenOrdersList(this.openOrdersList);
		this.openProcessor.setPendingOrdersList(this.pendingOrdersList);
		this.openProcessor.setFailedOrdersList(this.failedOrdersList);
		this.openProcessor.setSpawningOrdersList(this.spawningOrdersList);
	}

	/**
	 * Test if the method processOpenOrder is setting to SPAWNING an Open Local
	 * Order when the requestInstance method of InstanceProvider returns an
	 * Instance.
	 */
	@Test
	public void testProcessOpenLocalOrder() {
		Order localOrder = this.createLocalOrder();

		OrderInstance orderInstance = new OrderInstance("fake-id");

		Mockito.doReturn(orderInstance).when(this.localInstanceProvider).requestInstance(Mockito.any(Order.class));
		Mockito.doReturn(true).when(this.openOrdersList).removeItem(Mockito.any(Order.class));
		Mockito.doNothing().when(this.spawningOrdersList).addItem(Mockito.any(Order.class));

		this.openProcessor.processOpenOrder(localOrder);

		Assert.assertEquals(OrderState.SPAWNING, localOrder.getOrderState());
	}

	/**
	 * Test if the method processOpenOrder is setting to FAILED an Open Local
	 * Order when the requestInstance method of InstanceProvider returns an
	 * Instance with an empty Id.
	 */
	@Test
	public void testProcessOpenLocalOrderWithEmptyInstanceId() {
		Order localOrder = this.createLocalOrder();

		OrderInstance orderInstance = new OrderInstance("");

		Mockito.doReturn(orderInstance).when(this.localInstanceProvider).requestInstance(Mockito.any(Order.class));
		Mockito.doReturn(true).when(this.openOrdersList).removeItem(Mockito.any(Order.class));
		Mockito.doNothing().when(this.failedOrdersList).addItem(Mockito.any(Order.class));

		this.openProcessor.processOpenOrder(localOrder);

		Assert.assertEquals(OrderState.FAILED, localOrder.getOrderState());
	}

	/**
	 * Test if the method processOpenOrder is setting to FAILED an Open Local
	 * Order when the requestInstance method of InstanceProvider returns a null
	 * Instance.
	 */
	@Test
	public void testProcessOpenLocalOrderWithNullInstance() {
		Order localOrder = this.createLocalOrder();

		Mockito.doReturn(null).when(this.localInstanceProvider).requestInstance(Mockito.any(Order.class));
		Mockito.doReturn(true).when(this.openOrdersList).removeItem(Mockito.any(Order.class));
		Mockito.doNothing().when(this.failedOrdersList).addItem(Mockito.any(Order.class));

		this.openProcessor.processOpenOrder(localOrder);

		Assert.assertEquals(OrderState.FAILED, localOrder.getOrderState());
	}

	/**
	 * Test if the method processOpenOrder is setting to FAILED an Open Local
	 * Order when the requestInstance method of InstanceProvider throw an
	 * Exception.
	 */
	@Test
	public void testProcessLocalOpenOrderRequestingException() {
		Order localOrder = this.createLocalOrder();

		Mockito.doThrow(new RuntimeException("Any Exception")).when(this.localInstanceProvider)
				.requestInstance(Mockito.any(Order.class));
		Mockito.doReturn(true).when(this.openOrdersList).removeItem(Mockito.any(Order.class));
		Mockito.doNothing().when(this.failedOrdersList).addItem(Mockito.any(Order.class));

		this.openProcessor.processOpenOrder(localOrder);

		Assert.assertEquals(OrderState.FAILED, localOrder.getOrderState());
	}

	/**
	 * Test if the updateOrderStateAfterProcessing method is setting to SPAWNING
	 * an Open Local Order with an Instance and nonempty Id.
	 */
	@Test
	public void testUpdateLocalOpenOrderStateWithInstance() {
		Order localOrder = this.createLocalOrder();

		localOrder.setOrderInstance(new OrderInstance("fake-id"));

		Mockito.doNothing().when(this.spawningOrdersList).addItem(Mockito.any(Order.class));

		this.openProcessor.updateOrderStateAfterProcessing(localOrder);

		Assert.assertEquals(OrderState.SPAWNING, localOrder.getOrderState());
	}

	/**
	 * Test if the updateOrderStateAfterProcessing method is throwing a
	 * RuntimeException when an Open Local Order has an Instance and empty Id.
	 */
	@Test(expected = RuntimeException.class)
	public void testUpdateLocalOpenOrderStateWithEmptyInstanceId() {
		Order localOrder = this.createLocalOrder();

		localOrder.setOrderInstance(new OrderInstance(""));

		this.openProcessor.updateOrderStateAfterProcessing(localOrder);
	}

	/**
	 * Test if the updateOrderStateAfterProcessing method is throwing a
	 * RuntimeException when an Open Local Order does not have an Instance.
	 */
	@Test(expected = RuntimeException.class)
	public void testUpdateLocalOrderStateWithoutInstance() {
		Order localOrder = this.createLocalOrder();

		this.openProcessor.updateOrderStateAfterProcessing(localOrder);
	}

	/**
	 * Test if the method processOpenOrder is setting to PENDING an Open Remote
	 * Order.
	 */
	@Test
	public void testProcessOpenRemoteOrder() {
		Order remoteOrder = this.createRemoteOrder();

		Mockito.doReturn(null).when(this.remoteInstanceProvider).requestInstance(Mockito.any(Order.class));
		Mockito.doReturn(true).when(this.openOrdersList).removeItem(Mockito.any(Order.class));
		Mockito.doNothing().when(this.pendingOrdersList).addItem(Mockito.any(Order.class));

		this.openProcessor.processOpenOrder(remoteOrder);

		Assert.assertEquals(OrderState.PENDING, remoteOrder.getOrderState());
	}

	/**
	 * Test if the method processOpenOrder is setting to FAILED an Open Remote
	 * Order when the requestInstance method of Remote InstanceProvider throw an
	 * Exception.
	 */
	@Test
	public void testProcessRemoteOpenOrderRequestingException() {
		Order remoteOrder = this.createRemoteOrder();

		Mockito.doThrow(new RuntimeException("Any Exception")).when(this.remoteInstanceProvider)
				.requestInstance(Mockito.any(Order.class));
		Mockito.doNothing().when(this.failedOrdersList).addItem(Mockito.any(Order.class));

		this.openProcessor.processOpenOrder(remoteOrder);

		Assert.assertEquals(OrderState.FAILED, remoteOrder.getOrderState());
	}

	/**
	 * Test if the updateOrderStateAfterProcessing method is setting to PENDING
	 * an Open Remote Order.
	 */
	@Test
	public void testUpdateRemoteOrderState() {
		Order remoteOrder = this.createRemoteOrder();

		this.openProcessor.updateOrderStateAfterProcessing(remoteOrder);

		Assert.assertEquals(remoteOrder.getOrderState(), OrderState.PENDING);
	}

	/**
	 * Test if the getInstanceProvider method is returning the Local Instance
	 * Provider when the Order is to the Local Member.
	 */
	@Test
	public void testGetInstanceProviderForLocalOrder() {
		Order localOrder = this.createLocalOrder();

		InstanceProvider instanceProvider = this.openProcessor.getInstanceProviderForOrder(localOrder);

		Assert.assertSame(this.localInstanceProvider, instanceProvider);
	}

	/**
	 * Test if the getInstanceProvider method is returning the Remote Instance
	 * Provider when the Order is to an Remote Member.
	 */
	@Test
	public void testGetInstanceProviderForRemoteOrder() {
		Order remoteOrder = this.createRemoteOrder();

		InstanceProvider instanceProvider = this.openProcessor.getInstanceProviderForOrder(remoteOrder);

		Assert.assertSame(this.remoteInstanceProvider, instanceProvider);
	}

	/**
	 * Test if the processOpenOrder does not process an Order that is not in the
	 * OPEN State.
	 */
	@Test
	public void testProcessNotOpenOrder() {
		Order order = this.createLocalOrder();

		order.setOrderState(OrderState.PENDING);

		this.openProcessor.processOpenOrder(order);

		Assert.assertEquals(OrderState.PENDING, order.getOrderState());
	}

	/**
	 * Test if the OpenProcessor Thread is setting an Open Local Order to
	 * SPAWNING State after processing.
	 * 
	 * @throws InterruptedException
	 */
	@Test
	public void testRunProcessLocalOpenOrder() throws InterruptedException {
		Order localOrder = this.createLocalOrder();

		OrderInstance orderInstance = new OrderInstance("fake-id");

		Mockito.doReturn(localOrder).when(this.openOrdersList).getNext();
		Mockito.doReturn(orderInstance).when(this.localInstanceProvider).requestInstance(Mockito.any(Order.class));
		Mockito.doReturn(true).when(this.openOrdersList).removeItem(Mockito.any(Order.class));
		Mockito.doNothing().when(this.spawningOrdersList).addItem(Mockito.any(Order.class));

		Thread thread = new Thread(this.openProcessor);
		thread.start();

		Thread.sleep(1000);

		Assert.assertEquals(OrderState.SPAWNING, localOrder.getOrderState());
	}

	/**
	 * Test if the OpenProcessor is not processing an null Order.
	 * 
	 * @throws InterruptedException
	 */
	@Test
	public void testRunProcessWithNullOpenOrder() throws InterruptedException {
		Mockito.doReturn(null).when(this.openOrdersList).getNext();

		Thread thread = new Thread(this.openProcessor);
		thread.start();

		Thread.sleep(1500);
	}

	/**
	 * Test if the OpenProcessor still running even if the method
	 * processOpenOrder throw an Exception.
	 * 
	 * @throws InterruptedException
	 */
	@Test
	public void testRunProcessOpenOrderWithThrowException() throws InterruptedException {
		Order localOrder = this.createLocalOrder();

		Mockito.doReturn(localOrder).when(this.openOrdersList).getNext();
		Mockito.doThrow(new RuntimeException("Any Exception")).when(this.openProcessor)
				.processOpenOrder(Mockito.any(Order.class));

		Thread thread = new Thread(this.openProcessor);
		thread.start();

		Thread.sleep(1000);
	}

	/**
	 * This method tests a race condition when this class thread has the Order
	 * operation priority.
	 */
	@Test
	public void testRaceConditionWithThisThreadPriority() throws InterruptedException {
		Order localOrder = this.createLocalOrder();

		synchronized (localOrder) {
			OrderInstance orderInstance = new OrderInstance("fake-id");
			
			Mockito.doReturn(localOrder).when(this.openOrdersList).getNext();
			Mockito.doReturn(orderInstance).when(this.localInstanceProvider).requestInstance(Mockito.any(Order.class));
			
			Mockito.doReturn(true).when(this.openOrdersList).removeItem(Mockito.any(Order.class));
			Mockito.doNothing().when(this.spawningOrdersList).addItem(Mockito.any(Order.class));

			Thread thread = new Thread(this.openProcessor);
			thread.start();

			Thread.sleep(1000);

			Assert.assertEquals(OrderState.OPEN, localOrder.getOrderState());
		}

		Thread.sleep(1000);

		Assert.assertEquals(OrderState.SPAWNING, localOrder.getOrderState());
	}

	/**
	 * This method tests a race condition when this class thread has the Order
	 * operation priority and change the Open Order to a different State.
	 */
	@Test
	public void testRaceConditionWithThisThreadPriorityAndNotOpenOrder() throws InterruptedException {
		Order localOrder = this.createLocalOrder();

		synchronized (localOrder) {
			Mockito.doReturn(localOrder).when(this.openOrdersList).getNext();
			
			Thread thread = new Thread(this.openProcessor);
			thread.start();

			Thread.sleep(500);

			localOrder.setOrderState(OrderState.CLOSED);
		}

		Thread.sleep(500);

		Assert.assertEquals(OrderState.CLOSED, localOrder.getOrderState());
	}

	/**
	 * This method tests a race condition when the Attend Open Order Thread has
	 * the Order operation priority.
	 */
	@Test
	public void testRaceConditionWithOpenProcessorThreadPriority() throws InterruptedException {
		Order localOrder = this.createLocalOrder();

		OrderInstance orderInstance = new OrderInstance("fake-id");

		Mockito.doReturn(localOrder).when(this.openOrdersList).getNext();
		Mockito.when(this.localInstanceProvider.requestInstance(Mockito.any(Order.class)))
				.thenAnswer(new Answer<OrderInstance>() {
					@Override
					public OrderInstance answer(InvocationOnMock invocation) throws InterruptedException {
						Thread.sleep(500);
						return orderInstance;
					}
				});

		Mockito.doReturn(true).when(this.openOrdersList).removeItem(Mockito.any(Order.class));
		Mockito.doNothing().when(this.spawningOrdersList).addItem(Mockito.any(Order.class));

		Thread thread = new Thread(this.openProcessor);
		thread.start();

		Thread.sleep(500);

		synchronized (localOrder) {
			Thread.sleep(1000);
			Assert.assertEquals(OrderState.SPAWNING, localOrder.getOrderState());
			localOrder.setOrderState(OrderState.OPEN);
		}

		Assert.assertEquals(OrderState.OPEN, localOrder.getOrderState());
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
}
