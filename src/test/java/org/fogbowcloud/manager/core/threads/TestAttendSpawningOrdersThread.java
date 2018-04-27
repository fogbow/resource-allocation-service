package org.fogbowcloud.manager.core.threads;

import static org.junit.Assert.assertEquals;
import java.util.Properties;

import org.fogbowcloud.manager.core.constants.ConfigurationConstants;
import org.fogbowcloud.manager.core.datastructures.SharedOrderHolders;
import org.fogbowcloud.manager.core.models.linkedList.SynchronizedDoublyLinkedList;
import org.fogbowcloud.manager.core.models.orders.ComputeOrder;
import org.fogbowcloud.manager.core.models.orders.Order;
import org.fogbowcloud.manager.core.models.orders.OrderState;
import org.fogbowcloud.manager.core.models.orders.UserData;
import org.fogbowcloud.manager.core.models.orders.instances.ComputeOrderInstance;
import org.fogbowcloud.manager.core.models.orders.instances.InstanceState;
import org.fogbowcloud.manager.core.models.orders.instances.OrderInstance;
import org.fogbowcloud.manager.core.models.token.Token;
import org.fogbowcloud.manager.core.plugins.ComputePlugin;
import org.fogbowcloud.manager.core.utils.SshConnectivityUtil;
import org.fogbowcloud.manager.core.utils.TunnelingServiceUtil;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;

public class TestAttendSpawningOrdersThread {

	private static final String DEFAULT_ORDER_INSTANCE_ID = "1";

	private AttendSpawningOrdersThread attendSpawningOrdersThread;
	
	private Properties properties;
	
	private ComputePlugin computePĺugin;
	private Long sleepTime;
	
	@Before
	public void setUp() {		
		this.properties = new Properties();
		this.properties.put(ConfigurationConstants.XMPP_ID_KEY, ".");
		this.computePĺugin = Mockito.mock(ComputePlugin.class);		
		this.attendSpawningOrdersThread = Mockito.spy(new AttendSpawningOrdersThread(this.computePĺugin, this.sleepTime));
	}
	
	@Test
	public void testProcessSpawningOrderNullToFailed() {
		Mockito.doThrow(Exception.class).when(this.computePĺugin).getInstance(Mockito.any(Token.class), Mockito.eq(DEFAULT_ORDER_INSTANCE_ID));
		
		Order order = createOrder(OrderState.SPAWNING);
		OrderInstance orderInstance = new OrderInstance(DEFAULT_ORDER_INSTANCE_ID);
		order.setOrderInstance(orderInstance);
		
		this.attendSpawningOrdersThread.processSpawningOrder(order);
		Assert.assertEquals(OrderState.FAILED, this.getFailedOrder(order));
	}
	
	@Test
	public void testToProcessOthersOrderStates() {
		Order order = this.createOrder(OrderState.OPEN);
		this.attendSpawningOrdersThread.processSpawningOrder(order);
		Assert.assertEquals(OrderState.OPEN, order.getOrderState());
		
		order = this.createOrder(OrderState.PENDING);
		this.attendSpawningOrdersThread.processSpawningOrder(order);
		Assert.assertEquals(OrderState.PENDING, order.getOrderState());
		
		order = this.createOrder(OrderState.FULFILLED);
		this.attendSpawningOrdersThread.processSpawningOrder(order);
		Assert.assertEquals(OrderState.FULFILLED, order.getOrderState());
		
		order = this.createOrder(OrderState.CLOSED);
		this.attendSpawningOrdersThread.processSpawningOrder(order);
		Assert.assertEquals(OrderState.CLOSED, order.getOrderState());
	}
	
	@Test
	public void testRunProcessSpawningOrderWithThrowException() throws InterruptedException {
		Mockito.doThrow(new RuntimeException()).when(this.attendSpawningOrdersThread).processSpawningOrder(Mockito.any(Order.class));
		this.attendSpawningOrdersThread.start();
		Thread.sleep(1000);
	}
	
	@Test
	public void testProcessSpawningOrderFailed() {
		ComputeOrderInstance computeOrderInstance = Mockito.spy(ComputeOrderInstance.class);
		computeOrderInstance.setState(InstanceState.FAILED);
		Mockito.when(this.computePĺugin.getInstance(Mockito.any(Token.class), Mockito.eq(DEFAULT_ORDER_INSTANCE_ID))).thenReturn(computeOrderInstance);
		
		Order order = createOrder(OrderState.SPAWNING);
		SharedOrderHolders.getInstance().getSpawningOrdersList().addItem(order);
		OrderInstance orderInstance = new OrderInstance(DEFAULT_ORDER_INSTANCE_ID);
		order.setOrderInstance(orderInstance);
		
		this.attendSpawningOrdersThread.processSpawningOrder(order);
		
		assertEquals(OrderState.FAILED, this.getFailedOrder(order));
	}
	
	public Order getFailedOrder(Order order) {
		SynchronizedDoublyLinkedList synchronizedDoublyLinkedList = SharedOrderHolders.getInstance().getFailedOrdersList();
		Order element;
		while ((element = synchronizedDoublyLinkedList.getNext()) != null) {
			if (element.getId().equals(order.getId())) {
				return element;				
			}
		}
		return null;
	}
	
	@SuppressWarnings("unchecked")
	@Test
	public void testProcessSpawningOrderToFulfilled() {
		ComputeOrderInstance computeOrderInstance = Mockito.spy(ComputeOrderInstance.class);
		computeOrderInstance.setState(InstanceState.ACTIVE);
		Mockito.when(this.computePĺugin.getInstance(Mockito.any(Token.class), Mockito.eq(DEFAULT_ORDER_INSTANCE_ID))).thenReturn(computeOrderInstance);
		
		TunnelingServiceUtil tunnelingServiceUtil = Mockito.mock(TunnelingServiceUtil.class);
		this.attendSpawningOrdersThread.setTunnelingService(tunnelingServiceUtil);
		Mockito.doNothing().when(computeOrderInstance).setExternalServiceAddresses(Mockito.anyMap());
		
		SshConnectivityUtil sshConnectivityUtil = Mockito.mock(SshConnectivityUtil.class);
		this.attendSpawningOrdersThread.setSshConnectivity(sshConnectivityUtil);
		Mockito.when(sshConnectivityUtil.isActiveConnection()).thenReturn(true);

		Order order = createOrder(OrderState.SPAWNING);
		OrderInstance orderInstance = new OrderInstance(DEFAULT_ORDER_INSTANCE_ID);
		order.setOrderInstance(orderInstance);
		
		this.attendSpawningOrdersThread.processSpawningOrder(order);		
		Assert.assertEquals(OrderState.FULFILLED, this.getFailedOrder(order));
	}
	
	@Test
	public void testProcessSpawningOrderInactive() {
		ComputeOrderInstance computeOrderInstance = Mockito.spy(ComputeOrderInstance.class);
		computeOrderInstance.setState(InstanceState.INACTIVE);
		Mockito.when(this.computePĺugin.getInstance(Mockito.any(Token.class), Mockito.eq(DEFAULT_ORDER_INSTANCE_ID))).thenReturn(computeOrderInstance);
		
		Order order = createOrder(OrderState.SPAWNING);
		OrderInstance orderInstance = new OrderInstance(DEFAULT_ORDER_INSTANCE_ID);
		order.setOrderInstance(orderInstance);
		
		this.attendSpawningOrdersThread.processSpawningOrder(order);
		Assert.assertEquals(OrderState.SPAWNING, order.getOrderState());
	}
		
	@SuppressWarnings("unchecked")
	@Test
	public void testProcessSpawningComputeOrderInstanceActiveWithoutSshConnectivity() {
		ComputeOrderInstance computeOrderInstance = Mockito.spy(ComputeOrderInstance.class);
		computeOrderInstance.setState(InstanceState.ACTIVE);
		Mockito.when(this.computePĺugin.getInstance(Mockito.any(Token.class), Mockito.eq(DEFAULT_ORDER_INSTANCE_ID))).thenReturn(computeOrderInstance);
				
		TunnelingServiceUtil tunnelingServiceUtil = Mockito.mock(TunnelingServiceUtil.class);
		this.attendSpawningOrdersThread.setTunnelingService(tunnelingServiceUtil);
		Mockito.doNothing().when(computeOrderInstance).setExternalServiceAddresses(Mockito.anyMap());
		
		SshConnectivityUtil sshConnectivityUtil = Mockito.mock(SshConnectivityUtil.class);
		this.attendSpawningOrdersThread.setSshConnectivity(sshConnectivityUtil);
		Mockito.when(sshConnectivityUtil.isActiveConnection()).thenReturn(false);
		
		Order order = createOrder(OrderState.SPAWNING);
		OrderInstance orderInstance = new OrderInstance(DEFAULT_ORDER_INSTANCE_ID);
		order.setOrderInstance(orderInstance);
		
		this.attendSpawningOrdersThread.processSpawningOrder(order);		
		Assert.assertFalse(sshConnectivityUtil.isActiveConnection());
	}
	
	private Order createOrder(OrderState orderState) {
		Token localToken = Mockito.mock(Token.class);
		Token federationToken = Mockito.mock(Token.class);
		UserData userData = Mockito.mock(UserData.class);
		String imageName = "fake-image-name";
		String requestingMember = String.valueOf(this.properties.get(ConfigurationConstants.XMPP_ID_KEY));
		String providingMember = String.valueOf(this.properties.get(ConfigurationConstants.XMPP_ID_KEY));
		Order order = new ComputeOrder(localToken, federationToken, requestingMember, providingMember, 8, 1024, 30, imageName, userData);
		return order;
	}
	
}
