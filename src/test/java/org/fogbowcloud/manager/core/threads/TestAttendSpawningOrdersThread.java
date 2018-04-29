package org.fogbowcloud.manager.core.threads;

import static org.junit.Assert.assertNotNull;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

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
	private ComputePlugin computePlugin;
	private Long sleepTime;
	private Properties properties;
	
	@Before
	public void setUp() {		
		this.computePlugin = Mockito.mock(ComputePlugin.class);		
		this.attendSpawningOrdersThread = Mockito.spy(new AttendSpawningOrdersThread(this.computePlugin, this.sleepTime));		
		this.properties = new Properties();
		this.properties.put(ConfigurationConstants.XMPP_ID_KEY, ".");
	}
	
	@SuppressWarnings("unchecked")
	@Test
	public void testProcesseComputeOrderInstanceActive() {
		Order order = createOrder();
		SynchronizedDoublyLinkedList list = addInSpawningOrderList(order);
		order = list.getNext();
		
		OrderInstance orderInstance = new OrderInstance(DEFAULT_ORDER_INSTANCE_ID);
		order.setOrderInstance(orderInstance);
		
		ComputeOrderInstance computeOrderInstance = Mockito.spy(ComputeOrderInstance.class);
		computeOrderInstance.setState(InstanceState.ACTIVE);
		Mockito.when(this.computePlugin.getInstance(Mockito.any(Token.class), Mockito.eq(DEFAULT_ORDER_INSTANCE_ID))).thenReturn(computeOrderInstance);
		
		TunnelingServiceUtil tunnelingService = Mockito.mock(TunnelingServiceUtil.class);
		this.attendSpawningOrdersThread.setTunnelingService(tunnelingService);
		Mockito.doNothing().when(computeOrderInstance).setExternalServiceAddresses(Mockito.anyMap());
		
		SshConnectivityUtil sshConnectivity = Mockito.mock(SshConnectivityUtil.class);
		this.attendSpawningOrdersThread.setSshConnectivity(sshConnectivity);
		Mockito.when(sshConnectivity.checkSSHConnectivity(computeOrderInstance)).thenReturn(true);
		
		Assert.assertNull(SharedOrderHolders.getInstance().getFulfilledOrdersList().getNext());
		this.attendSpawningOrdersThread.processSpawningOrder(order);
		Assert.assertNull(SharedOrderHolders.getInstance().getSpawningOrdersList().getNext());
		
		Order test = SharedOrderHolders.getInstance().getFulfilledOrdersList().getNext();
		assertNotNull(test);
		Assert.assertEquals(order.getOrderInstance(), test.getOrderInstance());
		Assert.assertEquals(OrderState.FULFILLED, test.getOrderState());
	}
	
	@Test
	public void testGetExternalServiceAddressesConfiguredToThrowException() {
		Order order = createOrder();
		SynchronizedDoublyLinkedList list = addInSpawningOrderList(order);
		order = list.getNext();
		
		OrderInstance orderInstance = new OrderInstance(DEFAULT_ORDER_INSTANCE_ID);
		order.setOrderInstance(orderInstance);
		
		ComputeOrderInstance computeOrderInstance = Mockito.spy(ComputeOrderInstance.class);
		computeOrderInstance.setState(InstanceState.ACTIVE);
		Mockito.when(this.computePlugin.getInstance(Mockito.any(Token.class), Mockito.eq(DEFAULT_ORDER_INSTANCE_ID))).thenReturn(computeOrderInstance);
		
		TunnelingServiceUtil tunnelingService = Mockito.mock(TunnelingServiceUtil.class);
		this.attendSpawningOrdersThread.setTunnelingService(tunnelingService);
		when(tunnelingService.getExternalServiceAddresses(order.getId())).thenReturn(null);
		when(tunnelingService.getExternalServiceAddresses(order.getId())).thenThrow(new RuntimeException("Any Exception"));
		
		this.attendSpawningOrdersThread.processSpawningOrder(order);
	}
	
	@Test
	public void testProcesseComputeOrderInstanceInactive() {
		Order order = createOrder();
		SynchronizedDoublyLinkedList list = addInSpawningOrderList(order);
		order = list.getNext();
		list.resetPointer();
		
		OrderInstance orderInstance = new OrderInstance(DEFAULT_ORDER_INSTANCE_ID);
		order.setOrderInstance(orderInstance);
		
		ComputeOrderInstance computeOrderInstance = Mockito.spy(ComputeOrderInstance.class);
		computeOrderInstance.setState(InstanceState.INACTIVE);
		Mockito.when(this.computePlugin.getInstance(Mockito.any(Token.class), Mockito.eq(DEFAULT_ORDER_INSTANCE_ID))).thenReturn(computeOrderInstance);
		
		this.attendSpawningOrdersThread.processSpawningOrder(order);
		Assert.assertEquals(OrderState.SPAWNING, SharedOrderHolders.getInstance().getSpawningOrdersList().getNext().getOrderState());
	}
	
	@Test
	public void testProcesseComputeOrderInstanceFailed() {
		Order order = createOrder();
		SynchronizedDoublyLinkedList list = addInSpawningOrderList(order);
		order = list.getNext();
		
		OrderInstance orderInstance = new OrderInstance(DEFAULT_ORDER_INSTANCE_ID);
		order.setOrderInstance(orderInstance);
		
		ComputeOrderInstance computeOrderInstance = Mockito.spy(ComputeOrderInstance.class);
		computeOrderInstance.setState(InstanceState.FAILED);
		Mockito.when(this.computePlugin.getInstance(Mockito.any(Token.class), Mockito.eq(DEFAULT_ORDER_INSTANCE_ID))).thenReturn(computeOrderInstance);
		
		Assert.assertNull(SharedOrderHolders.getInstance().getFailedOrdersList().getNext());
		this.attendSpawningOrdersThread.processSpawningOrder(order);
		Assert.assertNull(SharedOrderHolders.getInstance().getSpawningOrdersList().getNext());
		
		Order test = SharedOrderHolders.getInstance().getFailedOrdersList().getNext();		
		Assert.assertEquals(order.getOrderInstance(), test.getOrderInstance());
		Assert.assertEquals(OrderState.FAILED, test.getOrderState());
	}
	
	@Test 
	public void testProcesseComputeOrderInstanceConfiguredToThrowException() {
		Order order = this.createOrder();
		order.setOrderState(OrderState.SPAWNING);
		OrderInstance orderInstance = order.getOrderInstance();
		order.setOrderInstance(orderInstance);
		when(this.computePlugin.getInstance(Mockito.any(Token.class), Mockito.eq(DEFAULT_ORDER_INSTANCE_ID))).thenReturn(null);
		when(this.computePlugin.getInstance(Mockito.any(Token.class), Mockito.eq(DEFAULT_ORDER_INSTANCE_ID))).thenThrow(new RuntimeException("Any Exception"));
		this.attendSpawningOrdersThread.processSpawningOrder(order);
	}
	
	@Test
	public void  testMethodProcessSpawningOrderConfiguredToThrowException() {
		OrderInstance orderInstance = Mockito.spy(OrderInstance.class);
		Order order = Mockito.spy(Order.class);
		order.setOrderState(OrderState.SPAWNING);
		order.setOrderInstance(orderInstance);		
		when(order.getOrderInstance()).thenReturn(null);		
		Mockito.doThrow(new RuntimeException("Any Exception")).when(order).getOrderInstance();
		verify(order).setOrderInstance(orderInstance);
		this.attendSpawningOrdersThread.processSpawningOrder(order);
	}
	
	@Test
	public void  testMethodProcessSpawningOrderConfiguredWithAnothersOrderStates() {
		Order order = Mockito.spy(Order.class);
		
		order.setOrderState(OrderState.OPEN);
		this.attendSpawningOrdersThread.processSpawningOrder(order);
		Assert.assertFalse(OrderState.SPAWNING.equals(order.getOrderState()));
		
		order.setOrderState(OrderState.PENDING);
		this.attendSpawningOrdersThread.processSpawningOrder(order);
		Assert.assertFalse(OrderState.SPAWNING.equals(order.getOrderState()));

		order.setOrderState(OrderState.FAILED);
		this.attendSpawningOrdersThread.processSpawningOrder(order);
		Assert.assertFalse(OrderState.SPAWNING.equals(order.getOrderState()));
		
		order.setOrderState(OrderState.FULFILLED);
		this.attendSpawningOrdersThread.processSpawningOrder(order);
		Assert.assertFalse(OrderState.SPAWNING.equals(order.getOrderState()));
		
		order.setOrderState(OrderState.CLOSED);
		this.attendSpawningOrdersThread.processSpawningOrder(order);
		Assert.assertFalse(OrderState.SPAWNING.equals(order.getOrderState()));
	}
	
	@Test
	public void testThreadRunConfiguredToThrowException() throws InterruptedException {
		Mockito.doThrow(new RuntimeException()).when(this.attendSpawningOrdersThread).processSpawningOrder(Mockito.any(Order.class));
		this.attendSpawningOrdersThread.start();
		Thread.sleep(1000);
	}	
	
	private SynchronizedDoublyLinkedList addInSpawningOrderList(Order order) {
		SynchronizedDoublyLinkedList list = SharedOrderHolders.getInstance().getSpawningOrdersList();
		order.setOrderState(OrderState.SPAWNING);
		list.addItem(order);
		return list;
	}
	
	private Order createOrder() {
		Token localToken = Mockito.mock(Token.class);
		Token federationToken = Mockito.mock(Token.class);
		String requestingMember = String.valueOf(this.properties.get(ConfigurationConstants.XMPP_ID_KEY));
		String providingMember = String.valueOf(this.properties.get(ConfigurationConstants.XMPP_ID_KEY));
		UserData userData = Mockito.mock(UserData.class);
		Order order = new ComputeOrder(localToken, federationToken, requestingMember, providingMember, 8, 1024, 30, "Fake_Image_Name", userData);
		return order;
	}
	
}
