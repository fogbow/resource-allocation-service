package org.fogbowcloud.manager.core.threads;

import static org.junit.Assert.assertNotNull;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Properties;

import org.fogbowcloud.manager.core.constants.ConfigurationConstants;
import org.fogbowcloud.manager.core.datastructures.SharedOrderHolders;
import org.fogbowcloud.manager.core.instanceprovider.InstanceProvider;
import org.fogbowcloud.manager.core.models.linkedList.SynchronizedDoublyLinkedList;
import org.fogbowcloud.manager.core.models.orders.ComputeOrder;
import org.fogbowcloud.manager.core.models.orders.Order;
import org.fogbowcloud.manager.core.models.orders.OrderState;
import org.fogbowcloud.manager.core.models.orders.UserData;
import org.fogbowcloud.manager.core.models.orders.instances.ComputeOrderInstance;
import org.fogbowcloud.manager.core.models.orders.instances.InstanceState;
import org.fogbowcloud.manager.core.models.orders.instances.OrderInstance;
import org.fogbowcloud.manager.core.models.token.Token;
import org.fogbowcloud.manager.core.utils.SshConnectivityUtil;
import org.fogbowcloud.manager.core.utils.TunnelingServiceUtil;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;

public class TestAttendSpawningOrdersThread {

	private static final String DEFAULT_ORDER_INSTANCE_ID = "1";
	
	private SpawningMonitor spawningMonitor;
	private InstanceProvider localInstanceProvider;

	private Properties properties;
	
	@Before
	public void setUp() {		
		this.localInstanceProvider = Mockito.mock(InstanceProvider.class);	
		this.properties = new Properties();
		this.properties.put(ConfigurationConstants.XMPP_ID_KEY, ".");
		this.spawningMonitor = Mockito.spy(new SpawningMonitor(localInstanceProvider, properties));
	}
	
	@SuppressWarnings("unchecked")
	@Test
	public void testProcesseComputeOrderInstanceActive() {
		Order order = createOrder();
		SynchronizedDoublyLinkedList list = addInSpawningOrderList(order);
		order = list.getNext();
		
		
		ComputeOrderInstance orderInstance = Mockito.mock(ComputeOrderInstance.class);				
		orderInstance.setState(InstanceState.ACTIVE);
		order.setOrderInstance(orderInstance);
		
//		ComputeOrderInstance computeOrderInstance = Mockito.mock(ComputeOrderInstance.class);
//		ComputeOrderInstance computeOrderInstance =	(ComputeOrderInstance) order.getOrderInstance();
//		order.setOrderInstance(computeOrderInstance);
		
		Mockito.when(this.localInstanceProvider.requestInstance(order)).thenReturn(orderInstance);
		
		TunnelingServiceUtil tunnelingService = Mockito.mock(TunnelingServiceUtil.class);
		this.spawningMonitor.setTunnelingService(tunnelingService);
		Mockito.doNothing().when(orderInstance).setExternalServiceAddresses(Mockito.anyMap());
		
		SshConnectivityUtil sshConnectivity = Mockito.mock(SshConnectivityUtil.class);
		this.spawningMonitor.setSshConnectivity(sshConnectivity);
		Mockito.when(sshConnectivity.checkSSHConnectivity(orderInstance)).thenReturn(true);
		
		Assert.assertNull(SharedOrderHolders.getInstance().getFulfilledOrdersList().getNext());
		this.spawningMonitor.processSpawningOrder(order);
		Assert.assertNull(SharedOrderHolders.getInstance().getSpawningOrdersList().getNext());

		Order test = SharedOrderHolders.getInstance().getFulfilledOrdersList().getNext();
		assertNotNull(test);
		Assert.assertEquals(order.getOrderInstance(), test.getOrderInstance());
		Assert.assertEquals(OrderState.FULFILLED, test.getOrderState());
	}
	
	//@Test
	public void testGetExternalServiceAddressesConfiguredToThrowException() {
		Order order = createOrder();
		SynchronizedDoublyLinkedList list = addInSpawningOrderList(order);
		order = list.getNext();
		list.resetPointer();
		
		OrderInstance orderInstance = new OrderInstance(DEFAULT_ORDER_INSTANCE_ID);
		order.setOrderInstance(orderInstance);
		
		ComputeOrderInstance computeOrderInstance = Mockito.spy(ComputeOrderInstance.class);
		computeOrderInstance.setState(InstanceState.ACTIVE);
//		Mockito.when(this.localInatanceProvider.requestInstance(order)).thenReturn(computeOrderInstance);
//		Mockito.when(this.computePlugin.getInstance(Mockito.any(Token.class), Mockito.eq(DEFAULT_ORDER_INSTANCE_ID))).thenReturn(computeOrderInstance);
		
		TunnelingServiceUtil tunnelingService = Mockito.mock(TunnelingServiceUtil.class);
//		this.attendSpawningOrdersThread.setTunnelingService(tunnelingService);
//		when(tunnelingService.getExternalServiceAddresses(order.getId())).thenReturn(null);
		when(tunnelingService.getExternalServiceAddresses(order.getId())).thenThrow(new RuntimeException("Any Exception"));
		
		this.spawningMonitor.processSpawningOrder(order);
		
		Order test = list.getNext();
		assertNotNull(test);
		Assert.assertEquals(OrderState.SPAWNING, test.getOrderState());
	}
	
	//@Test
	public void testProcesseComputeOrderInstanceInactive() {
		Order order = createOrder();
		SynchronizedDoublyLinkedList list = addInSpawningOrderList(order);
		order = list.getNext();
		list.resetPointer();
		
		OrderInstance orderInstance = new OrderInstance(DEFAULT_ORDER_INSTANCE_ID);
		order.setOrderInstance(orderInstance);
		
		ComputeOrderInstance computeOrderInstance = Mockito.spy(ComputeOrderInstance.class);
		computeOrderInstance.setState(InstanceState.INACTIVE);
//		Mockito.when(this.computePlugin.getInstance(Mockito.any(Token.class), Mockito.eq(DEFAULT_ORDER_INSTANCE_ID))).thenReturn(computeOrderInstance);
		
		this.spawningMonitor.processSpawningOrder(order);

		Order test = SharedOrderHolders.getInstance().getSpawningOrdersList().getNext();
		assertNotNull(test);
		Assert.assertEquals(OrderState.SPAWNING, test.getOrderState());	
	}
	
	//@Test
	public void testProcesseComputeOrderInstanceFailed() {
		Order order = createOrder();
		SynchronizedDoublyLinkedList list = addInSpawningOrderList(order);
		order = list.getNext();
		
		OrderInstance orderInstance = new OrderInstance(DEFAULT_ORDER_INSTANCE_ID);
		order.setOrderInstance(orderInstance);
		
		ComputeOrderInstance computeOrderInstance = Mockito.spy(ComputeOrderInstance.class);
		computeOrderInstance.setState(InstanceState.FAILED);
//		Mockito.when(this.computePlugin.getInstance(Mockito.any(Token.class), Mockito.eq(DEFAULT_ORDER_INSTANCE_ID))).thenReturn(computeOrderInstance);
		
		Assert.assertNull(SharedOrderHolders.getInstance().getFailedOrdersList().getNext());
		this.spawningMonitor.processSpawningOrder(order);
		Assert.assertNull(SharedOrderHolders.getInstance().getSpawningOrdersList().getNext());
		
		Order test = SharedOrderHolders.getInstance().getFailedOrdersList().getNext();		
		Assert.assertEquals(order.getOrderInstance(), test.getOrderInstance());
		Assert.assertEquals(OrderState.FAILED, test.getOrderState());
	}
	
	//@Test // discuss about this test case...
	public void testProcesseComputeOrderInstanceConfiguredToThrowException() {
		Order order = this.createOrder();
//		order.setOrderState(OrderState.SPAWNING);
//		OrderInstance orderInstance = order.getOrderInstance();
//		order.setOrderInstance(orderInstance);
//		when(this.computePlugin.getInstance(Mockito.any(Token.class), Mockito.eq(DEFAULT_ORDER_INSTANCE_ID))).thenReturn(null);
//		when(this.computePlugin.getInstance(Mockito.any(Token.class), Mockito.eq(DEFAULT_ORDER_INSTANCE_ID))).thenThrow(new RuntimeException("Any Exception"));
		when(this.localInstanceProvider.requestInstance(order)).thenThrow(new RuntimeException("Any Exception"));
		
		this.spawningMonitor.processSpawningOrder(order);
	}
	
	//@Test // check need...
	public void  testMethodProcessSpawningOrderConfiguredToThrowException() {
		OrderInstance orderInstance = Mockito.spy(OrderInstance.class);
		Order order = Mockito.spy(Order.class);
		order.setOrderState(OrderState.SPAWNING);
		order.setOrderInstance(orderInstance);		
		when(order.getOrderInstance()).thenReturn(null);		
		Mockito.doThrow(new RuntimeException("Any Exception")).when(order).getOrderInstance();
		verify(order).setOrderInstance(orderInstance);
		this.spawningMonitor.processSpawningOrder(order);
	}
	
	//@Test
	public void  testMethodProcessSpawningOrderConfiguredWithAnothersOrderStates() {
		Order order = Mockito.spy(Order.class);
		
		order.setOrderState(OrderState.OPEN);
		this.spawningMonitor.processSpawningOrder(order);
		Assert.assertFalse(OrderState.SPAWNING.equals(order.getOrderState()));
		
		order.setOrderState(OrderState.PENDING);
		this.spawningMonitor.processSpawningOrder(order);
		Assert.assertFalse(OrderState.SPAWNING.equals(order.getOrderState()));

		order.setOrderState(OrderState.FAILED);
		this.spawningMonitor.processSpawningOrder(order);
		Assert.assertFalse(OrderState.SPAWNING.equals(order.getOrderState()));
		
		order.setOrderState(OrderState.FULFILLED);
		this.spawningMonitor.processSpawningOrder(order);
		Assert.assertFalse(OrderState.SPAWNING.equals(order.getOrderState()));
		
		order.setOrderState(OrderState.CLOSED);
		this.spawningMonitor.processSpawningOrder(order);
		Assert.assertFalse(OrderState.SPAWNING.equals(order.getOrderState()));
	}
	
	//@Test // check need...
	public void testThreadRun() throws InterruptedException {
		OrderInstance orderInstance = Mockito.spy(OrderInstance.class);
		Order order = Mockito.spy(Order.class);
		order.setOrderInstance(orderInstance);
		Mockito.doNothing().when(this.spawningMonitor).processSpawningOrder(order);
		verify(order).setOrderInstance(orderInstance);
		this.spawningMonitor.start();
		Thread.sleep(2000);
	}
	
	//@Test
	public void testThreadRunConfiguredToThrowException() throws InterruptedException {
		Mockito.doThrow(new RuntimeException()).when(this.spawningMonitor).processSpawningOrder(Mockito.any(Order.class));
		this.spawningMonitor.start();
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
