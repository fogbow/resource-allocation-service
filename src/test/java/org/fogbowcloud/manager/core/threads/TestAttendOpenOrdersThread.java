package org.fogbowcloud.manager.core.threads;

import java.util.Properties;

import org.fogbowcloud.manager.core.constants.ConfigurationConstants;
import org.fogbowcloud.manager.core.instanceprovider.InstanceProvider;
import org.fogbowcloud.manager.core.models.orders.ComputeOrder;
import org.fogbowcloud.manager.core.models.orders.Order;
import org.fogbowcloud.manager.core.models.orders.OrderRegistry;
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

public class TestAttendOpenOrdersThread {

	private AttendOpenOrdersThread attendOpenOrdersThread;

	private InstanceProvider localInstanceProvider;
	private InstanceProvider remoteInstanceProvider;

	private OrderRegistry orderRegistry;

	private Properties properties;

	@Before
	public void setUp() {
		String localMemberId = "local-member";
		this.properties = new Properties();
		this.properties.setProperty(ConfigurationConstants.XMPP_ID_KEY, localMemberId);

		this.orderRegistry = Mockito.mock(OrderRegistry.class);
		this.localInstanceProvider = Mockito.mock(InstanceProvider.class);
		this.remoteInstanceProvider = Mockito.mock(InstanceProvider.class);

		this.attendOpenOrdersThread = Mockito.spy(new AttendOpenOrdersThread(this.localInstanceProvider,
				this.remoteInstanceProvider, this.orderRegistry, localMemberId, this.properties));
	}

	@Test
	public void testProcessOpenLocalOrder() {
		Order localOrder = this.createLocalOrder();

		OrderInstance orderInstance = new OrderInstance("fake-id");

		Mockito.doReturn(orderInstance).when(this.localInstanceProvider).requestInstance(Mockito.any(Order.class));
		Mockito.doNothing().when(this.orderRegistry).updateOrder(Mockito.any(Order.class));

		this.attendOpenOrdersThread.processOpenOrder(localOrder);

		Assert.assertEquals(OrderState.SPAWNING, localOrder.getOrderState());
	}

	@Test
	public void testProcessOpenLocalOrderWithEmptyInstanceId() {
		Order localOrder = this.createLocalOrder();

		OrderInstance orderInstance = new OrderInstance("");

		Mockito.doReturn(orderInstance).when(this.localInstanceProvider).requestInstance(Mockito.any(Order.class));
		Mockito.doNothing().when(this.orderRegistry).updateOrder(Mockito.any(Order.class));

		this.attendOpenOrdersThread.processOpenOrder(localOrder);

		Assert.assertEquals(OrderState.FAILED, localOrder.getOrderState());
	}

	@Test
	public void testProcessOpenLocalOrderWithNullInstance() {
		Order localOrder = this.createLocalOrder();

		Mockito.doReturn(null).when(this.localInstanceProvider).requestInstance(Mockito.any(Order.class));
		Mockito.doNothing().when(this.orderRegistry).updateOrder(Mockito.any(Order.class));

		this.attendOpenOrdersThread.processOpenOrder(localOrder);

		Assert.assertEquals(OrderState.FAILED, localOrder.getOrderState());
	}

	@Test
	public void testProcessLocalOpenOrderRequestingException() {
		Order localOrder = this.createLocalOrder();

		Mockito.doThrow(new RuntimeException("Any Exception")).when(this.localInstanceProvider)
				.requestInstance(Mockito.any(Order.class));
		Mockito.doNothing().when(this.orderRegistry).updateOrder(Mockito.any(Order.class));

		this.attendOpenOrdersThread.processOpenOrder(localOrder);

		Assert.assertEquals(OrderState.FAILED, localOrder.getOrderState());
	}

	@Test(expected = Exception.class)
	public void testProcessLocalOpenOrderUpdateRegistryException() {
		Order localOrder = this.createLocalOrder();

		OrderInstance orderInstance = new OrderInstance("fake-id");

		Mockito.doReturn(orderInstance).when(this.localInstanceProvider).requestInstance(Mockito.any(Order.class));
		Mockito.doThrow(new RuntimeException("Any Exception")).when(this.orderRegistry)
				.updateOrder(Mockito.any(Order.class));

		this.attendOpenOrdersThread.processOpenOrder(localOrder);
	}

	@Test
	public void testUpdateLocalOpenOrderStateWithInstance() {
		Order localOrder = this.createLocalOrder();

		localOrder.setOrderInstance(new OrderInstance("fake-id"));

		Mockito.doNothing().when(this.orderRegistry).updateOrder(Mockito.any(Order.class));
		this.attendOpenOrdersThread.updateOrderStateAfterProcessing(localOrder);

		Assert.assertEquals(localOrder.getOrderState(), OrderState.SPAWNING);
	}

	@Test(expected = Exception.class)
	public void testUpdateLocalOpenOrderStateWithEmptyInstanceId() {
		Order localOrder = this.createLocalOrder();

		localOrder.setOrderInstance(new OrderInstance(""));

		this.attendOpenOrdersThread.updateOrderStateAfterProcessing(localOrder);
	}

	@Test(expected = Exception.class)
	public void testUpdateLocalOrderStateWithoutInstance() {
		Order localOrder = this.createLocalOrder();

		this.attendOpenOrdersThread.updateOrderStateAfterProcessing(localOrder);
	}

	@Test
	public void testProcessOpenRemoteOrder() {
		Order remoteOrder = this.createRemoteOrder();

		Mockito.doReturn(null).when(this.remoteInstanceProvider).requestInstance(Mockito.any(Order.class));
		Mockito.doNothing().when(this.orderRegistry).updateOrder(Mockito.any(Order.class));

		this.attendOpenOrdersThread.processOpenOrder(remoteOrder);

		Assert.assertEquals(OrderState.PENDING, remoteOrder.getOrderState());
	}

	@Test
	public void testProcessRemoteOpenOrderRequestingException() {
		Order remoteOrder = this.createRemoteOrder();

		Mockito.doThrow(new RuntimeException("Any Exception")).when(this.remoteInstanceProvider)
				.requestInstance(Mockito.any(Order.class));
		Mockito.doNothing().when(this.orderRegistry).updateOrder(Mockito.any(Order.class));

		this.attendOpenOrdersThread.processOpenOrder(remoteOrder);

		Assert.assertEquals(OrderState.FAILED, remoteOrder.getOrderState());
	}

	@Test
	public void testUpdateRemoteOrderState() {
		Order remoteOrder = this.createRemoteOrder();

		this.attendOpenOrdersThread.updateOrderStateAfterProcessing(remoteOrder);

		Assert.assertEquals(remoteOrder.getOrderState(), OrderState.PENDING);
	}

	@Test
	public void testGetInstanceProviderForLocalOrder() {
		Order localOrder = this.createLocalOrder();

		InstanceProvider instanceProvider = this.attendOpenOrdersThread.getInstanceProviderForOrder(localOrder);

		Assert.assertSame(this.localInstanceProvider, instanceProvider);
	}

	@Test
	public void testGetInstanceProviderForRemoteOrder() {
		Order remoteOrder = this.createRemoteOrder();

		InstanceProvider instanceProvider = this.attendOpenOrdersThread.getInstanceProviderForOrder(remoteOrder);

		Assert.assertSame(this.remoteInstanceProvider, instanceProvider);
	}

	@Test
	public void testProcessNotOpenOrder() {
		Order order = this.createLocalOrder();

		Mockito.doNothing().when(this.orderRegistry).updateOrder(Mockito.any(Order.class));
		order.setOrderState(OrderState.PENDING, this.orderRegistry);

		this.attendOpenOrdersThread.processOpenOrder(order);

		Assert.assertEquals(OrderState.PENDING, order.getOrderState());
	}

	@Test
	public void testRunProcessLocalOpenOrder() throws InterruptedException {
		Order localOrder = this.createLocalOrder();

		OrderInstance orderInstance = new OrderInstance("fake-id");

		Mockito.doReturn(orderInstance).when(this.localInstanceProvider).requestInstance(Mockito.any(Order.class));
		Mockito.doReturn(localOrder).when(this.orderRegistry).getNextOrderByState(Mockito.any(OrderState.class));
		Mockito.doNothing().when(this.orderRegistry).updateOrder(Mockito.any(Order.class));

		this.attendOpenOrdersThread.start();

		Thread.sleep(1000);

		Assert.assertEquals(OrderState.SPAWNING, localOrder.getOrderState());
	}

	@Test
	public void testRunProcessWithNullOpenOrder() throws InterruptedException {
		Mockito.doReturn(null).when(this.orderRegistry).getNextOrderByState(Mockito.any(OrderState.class));

		this.attendOpenOrdersThread.start();

		Thread.sleep(1500);
	}

	@Test
	public void testRunProcessOpenOrderWithThrowException() throws InterruptedException {
		Order localOrder = this.createLocalOrder();

		Mockito.doReturn(localOrder).when(this.orderRegistry).getNextOrderByState(Mockito.any(OrderState.class));
		Mockito.doThrow(new RuntimeException()).when(this.attendOpenOrdersThread)
				.processOpenOrder(Mockito.any(Order.class));

		this.attendOpenOrdersThread.start();

		Thread.sleep(1000);
	}

	/**
	 * This method test a race condition when this class thread has the Order
	 * operation priority.
	 */
	@Test
	public void testRaceConditionWithThisThreadPriority() throws InterruptedException {
		Order localOrder = this.createLocalOrder();

		synchronized (localOrder) {
			OrderInstance orderInstance = new OrderInstance("fake-id");

			Mockito.doReturn(orderInstance).when(this.localInstanceProvider).requestInstance(Mockito.any(Order.class));
			Mockito.doReturn(localOrder).when(this.orderRegistry).getNextOrderByState(Mockito.any(OrderState.class));
			Mockito.doNothing().when(this.orderRegistry).updateOrder(Mockito.any(Order.class));

			this.attendOpenOrdersThread.start();

			Thread.sleep(1000);

			Assert.assertEquals(OrderState.OPEN, localOrder.getOrderState());
		}

		Thread.sleep(1000);

		Assert.assertEquals(OrderState.SPAWNING, localOrder.getOrderState());
	}

	/**
	 * This method test a race condition when the Attend Open Order Thread has
	 * the Order operation priority.
	 */
	@Test
	public void testRaceConditionWithAttenOpenOrderThreadPriority() throws InterruptedException {
		Order localOrder = this.createLocalOrder();

		OrderInstance orderInstance = new OrderInstance("fake-id");

		Mockito.when(this.localInstanceProvider.requestInstance(Mockito.any(Order.class)))
				.thenAnswer(new Answer<OrderInstance>() {
					@Override
					public OrderInstance answer(InvocationOnMock invocation) throws InterruptedException {
						Thread.sleep(1000);
						return orderInstance;
					}
				});

		Mockito.doReturn(localOrder).when(this.orderRegistry).getNextOrderByState(Mockito.any(OrderState.class));
		Mockito.doNothing().when(this.orderRegistry).updateOrder(Mockito.any(Order.class));

		this.attendOpenOrdersThread.start();

		Thread.sleep(500);

		synchronized (localOrder) {
			Assert.assertEquals(OrderState.SPAWNING, localOrder.getOrderState());
			localOrder.setOrderState(OrderState.OPEN, this.orderRegistry);
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
