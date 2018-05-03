package org.fogbowcloud.manager.core.threads;

import static org.junit.Assert.assertNotNull;
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
import org.fogbowcloud.manager.core.models.token.Token;
import org.fogbowcloud.manager.core.utils.SshConnectivityUtil;
import org.fogbowcloud.manager.core.utils.TunnelingServiceUtil;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;

public class TestSpawningMonitor {
	
	private Properties properties;
	private SpawningMonitor spawningMonitor;
	private SynchronizedDoublyLinkedList spawningOrderList;
	private SynchronizedDoublyLinkedList fulfilledOrderList;
	private SynchronizedDoublyLinkedList failedOrderList;
	
	
	@Before
	public void setUp() {		
		this.properties = new Properties();
		this.properties.put(ConfigurationConstants.XMPP_ID_KEY, ".");
		this.spawningMonitor = Mockito.spy(new SpawningMonitor(properties));
		
		SharedOrderHolders sharedOrderHolders = SharedOrderHolders.getInstance();
		this.spawningOrderList = sharedOrderHolders.getSpawningOrdersList();
		this.fulfilledOrderList = sharedOrderHolders.getFulfilledOrdersList();
		this.failedOrderList = sharedOrderHolders.getFailedOrdersList();		
	}
	
	@SuppressWarnings("unchecked")
	@Test
	public void testProcesseComputeOrderInstanceActive() {
		Order order = createOrder();
		order.setOrderState(OrderState.SPAWNING);
		this.spawningOrderList.addItem(order);
		
		order = this.spawningOrderList.getNext();
		
		ComputeOrderInstance computeOrderInstance = Mockito.spy(ComputeOrderInstance.class);				
		computeOrderInstance.setState(InstanceState.ACTIVE);
		order.setOrderInstance(computeOrderInstance);
		
		TunnelingServiceUtil tunnelingService = Mockito.mock(TunnelingServiceUtil.class);
		this.spawningMonitor.setTunnelingService(tunnelingService);
		Mockito.doNothing().when(computeOrderInstance).setExternalServiceAddresses(Mockito.anyMap());
		
		SshConnectivityUtil sshConnectivity = Mockito.mock(SshConnectivityUtil.class);
		this.spawningMonitor.setSshConnectivity(sshConnectivity);
		Mockito.when(sshConnectivity.checkSSHConnectivity(computeOrderInstance)).thenReturn(true);
		
		Assert.assertNull(this.fulfilledOrderList.getNext());
		this.spawningMonitor.processSpawningOrder(order);
		Assert.assertNull(this.spawningOrderList.getNext());

		Order test = this.fulfilledOrderList.getNext();
		assertNotNull(test);
		Assert.assertEquals(order.getOrderInstance(), test.getOrderInstance());
		Assert.assertEquals(OrderState.FULFILLED, test.getOrderState());
	}
	
	@Test
	public void testProcesseComputeOrderInstanceInactive() {
		Order order = createOrder();
		order.setOrderState(OrderState.SPAWNING);
		this.spawningOrderList.addItem(order);
		
		order = this.spawningOrderList.getNext();
		this.spawningOrderList.resetPointer();
		
		ComputeOrderInstance computeOrderInstance = Mockito.spy(ComputeOrderInstance.class);				
		computeOrderInstance.setState(InstanceState.INACTIVE);
		order.setOrderInstance(computeOrderInstance);
		
		this.spawningMonitor.processSpawningOrder(order);

		Order test = this.spawningOrderList.getNext();
		assertNotNull(test);
		Assert.assertEquals(order.getOrderInstance(), test.getOrderInstance());
		Assert.assertEquals(OrderState.SPAWNING, test.getOrderState());	
	}
	
	@Test
	public void testProcesseComputeOrderInstanceFailed() {
		Order order = createOrder();
		order.setOrderState(OrderState.SPAWNING);
		this.spawningOrderList.addItem(order);
		
		order = this.spawningOrderList.getNext();

		ComputeOrderInstance computeOrderInstance = Mockito.spy(ComputeOrderInstance.class);
		computeOrderInstance.setState(InstanceState.FAILED);
		order.setOrderInstance(computeOrderInstance);

		Assert.assertNull(this.failedOrderList.getNext());
		this.spawningMonitor.processSpawningOrder(order);
		Assert.assertNull(this.spawningOrderList.getNext());
		
		Order test = this.failedOrderList.getNext();		
		assertNotNull(test);
		Assert.assertEquals(order.getOrderInstance(), test.getOrderInstance());
		Assert.assertEquals(OrderState.FAILED, test.getOrderState());
	}
	
	@Test
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
	
	@Test
	public void testThreadRunConfiguredToThrowException() throws InterruptedException {
		Mockito.doThrow(new RuntimeException()).when(this.spawningMonitor).processSpawningOrder(Mockito.any(Order.class));
		this.spawningMonitor.start();
		Thread.sleep(1000);
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
