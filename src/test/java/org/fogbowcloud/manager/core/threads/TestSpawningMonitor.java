package org.fogbowcloud.manager.core.threads;

import static org.junit.Assert.assertNotNull;
import java.util.Map;
import java.util.Properties;

import org.fogbowcloud.manager.core.BaseUnitTests;
import org.fogbowcloud.manager.core.constants.ConfigurationConstants;
import org.fogbowcloud.manager.core.datastructures.SharedOrderHolders;
import org.fogbowcloud.manager.core.models.linkedList.ChainedList;
import org.fogbowcloud.manager.core.models.orders.ComputeOrder;
import org.fogbowcloud.manager.core.models.orders.Order;
import org.fogbowcloud.manager.core.models.orders.OrderState;
import org.fogbowcloud.manager.core.models.orders.UserData;
import org.fogbowcloud.manager.core.models.orders.instances.ComputeOrderInstance;
import org.fogbowcloud.manager.core.models.orders.instances.InstanceState;
import org.fogbowcloud.manager.core.models.token.Token;
import org.fogbowcloud.manager.core.utils.SshConnectivityUtil;
import org.fogbowcloud.manager.core.utils.TunnelingServiceUtil;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;

public class TestSpawningMonitor extends BaseUnitTests {

	private Properties properties;
	private TunnelingServiceUtil tunnelingService;
	private SshConnectivityUtil sshConnectivity;
	private SpawningMonitor spawningMonitor;
	private Thread thread;
	private ChainedList spawningOrderList;
	private ChainedList fulfilledOrderList;
	private ChainedList failedOrderList;
	private ChainedList openOrderList;
	private ChainedList pendingOrderList;
	private ChainedList closedOrderList;

	@Before
	public void setUp() {
		this.tunnelingService = Mockito.mock(TunnelingServiceUtil.class);
		this.sshConnectivity = Mockito.mock(SshConnectivityUtil.class);
		this.properties = new Properties();
		this.properties.put(ConfigurationConstants.XMPP_ID_KEY, ".");
		this.spawningMonitor = Mockito
				.spy(new SpawningMonitor(this.tunnelingService, this.sshConnectivity, this.properties));
		this.thread = null;

		SharedOrderHolders sharedOrderHolders = SharedOrderHolders.getInstance();
		this.spawningOrderList = sharedOrderHolders.getSpawningOrdersList();
		this.fulfilledOrderList = sharedOrderHolders.getFulfilledOrdersList();
		this.failedOrderList = sharedOrderHolders.getFailedOrdersList();
		this.openOrderList = sharedOrderHolders.getOpenOrdersList();
		this.pendingOrderList = sharedOrderHolders.getPendingOrdersList();
		this.closedOrderList = sharedOrderHolders.getClosedOrdersList();
	}

	@After
	public void tearDown() {
		if (this.thread != null) {
			this.thread.interrupt();
		}

		this.cleanList(this.spawningOrderList);
		this.cleanList(this.fulfilledOrderList);
		this.cleanList(this.failedOrderList);
		this.cleanList(this.openOrderList);
		this.cleanList(this.pendingOrderList);
		this.cleanList(this.closedOrderList);
	}
	
	@Test
	public void testRunThrowableExceptionWhileTryingToProcessOrder() throws InterruptedException {
		Order order = Mockito.mock(Order.class);
		OrderState state = null;
		order.setOrderState(state);
		this.spawningOrderList.addItem(order);
		
		Mockito.doThrow(new RuntimeException("Any Exception")).when(this.spawningMonitor)
		.processSpawningOrder(order);
				
		Thread thread = new Thread(this.spawningMonitor);
		thread.start();
		Thread.sleep(500);
	}
	
	@Test
	public void testRunProcesseComputeOrderInstanceActive() throws InterruptedException {
		Order order = this.createOrder();
		order.setOrderState(OrderState.SPAWNING);
		this.spawningOrderList.addItem(order);

		ComputeOrderInstance computeOrderInstance = Mockito.spy(new ComputeOrderInstance("fake-id"));
		computeOrderInstance.setState(InstanceState.ACTIVE);
		order.setOrderInstance(computeOrderInstance);

		Map<String, String> externalServiceAddresses = this.tunnelingService.getExternalServiceAddresses(order.getId());
		Mockito.doNothing().when(computeOrderInstance).setExternalServiceAddresses(externalServiceAddresses);

		Mockito.when(this.sshConnectivity.checkSSHConnectivity(computeOrderInstance)).thenReturn(true);

		Assert.assertNull(this.fulfilledOrderList.getNext());

		Thread thread = new Thread(this.spawningMonitor);
		thread.start();
		Thread.sleep(500);

		Assert.assertNull(this.spawningOrderList.getNext());

		Order test = this.fulfilledOrderList.getNext();
		assertNotNull(test);
		Assert.assertEquals(order.getOrderInstance(), test.getOrderInstance());
		Assert.assertEquals(OrderState.FULFILLED, test.getOrderState());
	}

	@Test
	public void testRunProcesseComputeOrderInstanceInactive() throws InterruptedException {
		Order order = this.createOrder();
		order.setOrderState(OrderState.SPAWNING);
		this.spawningOrderList.addItem(order);

		ComputeOrderInstance computeOrderInstance = Mockito.spy(new ComputeOrderInstance("fake-id"));
		computeOrderInstance.setState(InstanceState.INACTIVE);
		order.setOrderInstance(computeOrderInstance);

		Thread thread = new Thread(this.spawningMonitor);
		thread.start();
		Thread.sleep(500);

		Order test = this.spawningOrderList.getNext();
		Assert.assertNotNull(test);
		Assert.assertEquals(OrderState.SPAWNING, test.getOrderState());
	}

	@Test
	public void testRunProcesseComputeOrderInstanceFailed() throws InterruptedException {
		Order order = this.createOrder();
		order.setOrderState(OrderState.SPAWNING);
		this.spawningOrderList.addItem(order);

		ComputeOrderInstance computeOrderInstance = Mockito.spy(new ComputeOrderInstance("fake-id"));
		computeOrderInstance.setState(InstanceState.FAILED);
		order.setOrderInstance(computeOrderInstance);

		Assert.assertNull(this.failedOrderList.getNext());

		Thread thread = new Thread(this.spawningMonitor);
		thread.start();
		Thread.sleep(500);

		Assert.assertNull(this.spawningOrderList.getNext());

		Order test = this.failedOrderList.getNext();
		Assert.assertNotNull(test);
		Assert.assertEquals(order.getOrderInstance(), test.getOrderInstance());
		Assert.assertEquals(OrderState.FAILED, test.getOrderState());
	}

	@Test
	public void testProcessWithoutModifyOpenOrderState() throws InterruptedException {
		Order order = this.createOrder();
		order.setOrderState(OrderState.OPEN);
		this.openOrderList.addItem(order);

		this.spawningMonitor.processSpawningOrder(order);

		Order test = this.openOrderList.getNext();
		Assert.assertEquals(OrderState.OPEN, test.getOrderState());
		Assert.assertNull(this.spawningOrderList.getNext());
		Assert.assertNull(this.failedOrderList.getNext());
		Assert.assertNull(this.fulfilledOrderList.getNext());
	}

	@Test
	public void testProcessWithoutModifyPendingOrderState() throws InterruptedException {
		Order order = this.createOrder();
		order.setOrderState(OrderState.PENDING);
		this.pendingOrderList.addItem(order);

		this.spawningMonitor.processSpawningOrder(order);

		Order test = this.pendingOrderList.getNext();
		Assert.assertEquals(OrderState.PENDING, test.getOrderState());
		Assert.assertNull(this.spawningOrderList.getNext());
		Assert.assertNull(this.failedOrderList.getNext());
		Assert.assertNull(this.fulfilledOrderList.getNext());
	}

	@Test
	public void testProcessWithoutModifyFailedOrderState() throws InterruptedException {
		Order order = this.createOrder();
		order.setOrderState(OrderState.FAILED);
		this.failedOrderList.addItem(order);

		this.spawningMonitor.processSpawningOrder(order);

		Order test = this.failedOrderList.getNext();
		Assert.assertEquals(OrderState.FAILED, test.getOrderState());
		Assert.assertNull(this.spawningOrderList.getNext());
		Assert.assertNull(this.fulfilledOrderList.getNext());
	}

	@Test
	public void testProcessWithoutModifyFulfilledOrderState() throws InterruptedException {
		Order order = this.createOrder();
		order.setOrderState(OrderState.FULFILLED);
		this.fulfilledOrderList.addItem(order);

		this.spawningMonitor.processSpawningOrder(order);

		Order test = this.fulfilledOrderList.getNext();
		Assert.assertEquals(OrderState.FULFILLED, test.getOrderState());
		Assert.assertNull(this.spawningOrderList.getNext());
		Assert.assertNull(this.failedOrderList.getNext());
	}

	@Test
	public void testProcessWithoutModifyClosedOrderState() throws InterruptedException {
		Order order = this.createOrder();
		order.setOrderState(OrderState.CLOSED);
		this.closedOrderList.addItem(order);

		this.spawningMonitor.processSpawningOrder(order);

		Order test = this.closedOrderList.getNext();
		Assert.assertEquals(OrderState.CLOSED, test.getOrderState());
		Assert.assertNull(this.spawningOrderList.getNext());
		Assert.assertNull(this.failedOrderList.getNext());
		Assert.assertNull(this.fulfilledOrderList.getNext());
	}

	@Test
	public void testRunThrowableExceptionWhileTryingToGetMapAddressesOfComputeOrderInstance()
			throws InterruptedException {
		Order order = this.createOrder();
		order.setOrderState(OrderState.SPAWNING);
		this.spawningOrderList.addItem(order);

		ComputeOrderInstance computeOrderInstance = Mockito.spy(new ComputeOrderInstance("fake-id"));
		computeOrderInstance.setState(InstanceState.ACTIVE);
		order.setOrderInstance(computeOrderInstance);

		Mockito.doThrow(new RuntimeException("Any Exception")).when(this.tunnelingService)
				.getExternalServiceAddresses(order.getId());

		Thread thread = new Thread(this.spawningMonitor);
		thread.start();
		Thread.sleep(500);
	}

	@Test
	public void testRunConfiguredToFailedAttemptSshConnectivity() throws InterruptedException {
		Order order = this.createOrder();
		order.setOrderState(OrderState.SPAWNING);
		this.spawningOrderList.addItem(order);

		ComputeOrderInstance computeOrderInstance = Mockito.spy(new ComputeOrderInstance("fake-id"));
		computeOrderInstance.setState(InstanceState.ACTIVE);
		order.setOrderInstance(computeOrderInstance);

		Map<String, String> externalServiceAddresses = this.tunnelingService.getExternalServiceAddresses(order.getId());
		Mockito.doNothing().when(computeOrderInstance).setExternalServiceAddresses(externalServiceAddresses);

		Mockito.when(this.sshConnectivity.checkSSHConnectivity(computeOrderInstance)).thenReturn(false);

		Thread thread = new Thread(this.spawningMonitor);
		thread.start();
		Thread.sleep(500);

		Order test = this.spawningOrderList.getNext();
		Assert.assertNotNull(test);
		Assert.assertEquals(OrderState.SPAWNING, test.getOrderState());
	}

	@Test
	public void testRunWithIterruptThread() throws InterruptedException {
		Thread thread = new Thread(this.spawningMonitor);
		thread.start();
		Thread.sleep(500);
		thread.interrupt();
	}

	private Order createOrder() {
		Token localToken = Mockito.mock(Token.class);
		Token federationToken = Mockito.mock(Token.class);
		String requestingMember = String.valueOf(this.properties.get(ConfigurationConstants.XMPP_ID_KEY));
		String providingMember = String.valueOf(this.properties.get(ConfigurationConstants.XMPP_ID_KEY));
		UserData userData = Mockito.mock(UserData.class);
		Order order = new ComputeOrder(localToken, federationToken, requestingMember, providingMember, 8, 1024, 30,
				"fake_image_name", userData, "fake_public_key");
		return order;
	}

}
