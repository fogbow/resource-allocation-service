package org.fogbowcloud.manager.core.threads;

import java.util.Properties;

import org.fogbowcloud.manager.core.constants.ConfigurationConstants;
import org.fogbowcloud.manager.core.models.orders.ComputeOrder;
import org.fogbowcloud.manager.core.models.orders.Order;
import org.fogbowcloud.manager.core.models.orders.OrderRegistry;
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
	
	private OrderRegistry orderRegistry;
	private ComputePlugin computePĺugin;
	private Long sleepTime;
	
	@Before
	public void setUp() {
		this.properties = new Properties();
		this.orderRegistry = Mockito.mock(OrderRegistry.class);
		this.computePĺugin = Mockito.mock(ComputePlugin.class);
		this.attendSpawningOrdersThread = new AttendSpawningOrdersThread(this.orderRegistry, this.computePĺugin, this.sleepTime);
		this.properties.put(ConfigurationConstants.XMPP_ID_KEY, ".");
	}
	
	@Test
	public void testProcessSpawningOrderFailed() {
		Order order = createOrder();
		ComputeOrderInstance computeOrderInstance = Mockito.spy(ComputeOrderInstance.class);
		computeOrderInstance.setState(InstanceState.FAILED);
		Mockito.when(this.computePĺugin.getInstance(Mockito.any(Token.class), Mockito.eq(DEFAULT_ORDER_INSTANCE_ID))).thenReturn(computeOrderInstance);
		this.attendSpawningOrdersThread.processSpawningOrder(order);
		Assert.assertEquals(OrderState.FAILED, order.getOrderState());
	}
	
	@Test
	public void testProcessSpawningOrderInactive() {
		Order order = createOrder();
		ComputeOrderInstance computeOrderInstance = Mockito.spy(ComputeOrderInstance.class);
		computeOrderInstance.setState(InstanceState.INACTIVE);
		Mockito.when(this.computePĺugin.getInstance(Mockito.any(Token.class), Mockito.eq(DEFAULT_ORDER_INSTANCE_ID))).thenReturn(computeOrderInstance);
		this.attendSpawningOrdersThread.processSpawningOrder(order);
		Assert.assertEquals(OrderState.SPAWNING, order.getOrderState());
	}
	
	@Test(expected = Exception.class)
	public void testProcessSpawningComputeOrderInstanceActiveException() {
		Order order = createOrder();
		ComputeOrderInstance computeOrderInstance = Mockito.spy(ComputeOrderInstance.class);
		
		computeOrderInstance.setState(InstanceState.ACTIVE);
		Mockito.when(this.computePĺugin.getInstance(Mockito.any(Token.class), Mockito.eq(DEFAULT_ORDER_INSTANCE_ID))).thenReturn(computeOrderInstance);
		
		TunnelingServiceUtil tunnelingServiceUtil = Mockito.mock(TunnelingServiceUtil.class);
		this.attendSpawningOrdersThread.setTunnelingServiceUtil(tunnelingServiceUtil);	
		
		System.out.println(order);
		
		this.attendSpawningOrdersThread.processSpawningOrder(order);
		

		
		Mockito.doThrow(new Exception()).when(computeOrderInstance).setExternalServiceAddresses(Mockito.anyMap());
	}
		
	@Test
	public void testProcessSpawningComputeOrderInstanceActiveWithoutSshConnectivity() {
		Order order = createOrder();
		ComputeOrderInstance computeOrderInstance = Mockito.spy(ComputeOrderInstance.class);
		
		computeOrderInstance.setState(InstanceState.ACTIVE);
		Mockito.when(this.computePĺugin.getInstance(Mockito.any(Token.class), Mockito.eq(DEFAULT_ORDER_INSTANCE_ID))).thenReturn(computeOrderInstance);
				
		TunnelingServiceUtil tunnelingServiceUtil = Mockito.mock(TunnelingServiceUtil.class);
		this.attendSpawningOrdersThread.setTunnelingServiceUtil(tunnelingServiceUtil);
		Mockito.doNothing().when(computeOrderInstance).setExternalServiceAddresses(Mockito.anyMap());
		
		SshConnectivityUtil sshConnectivityUtil = Mockito.mock(SshConnectivityUtil.class);
		this.attendSpawningOrdersThread.setSshConnectivity(sshConnectivityUtil);
		Mockito.when(sshConnectivityUtil.isActiveConnection()).thenReturn(false);
		
		this.attendSpawningOrdersThread.processSpawningOrder(order);		
		Assert.assertFalse(sshConnectivityUtil.isActiveConnection());
	}
	
	@Test
	public void testProcessSpawningOrderToFulfilled() {
		Order order = createOrder();
		ComputeOrderInstance computeOrderInstance = Mockito.spy(ComputeOrderInstance.class);
		
		// Order Inactive
		computeOrderInstance.setState(InstanceState.INACTIVE);
		Mockito.when(this.computePĺugin.getInstance(Mockito.any(Token.class), Mockito.eq(DEFAULT_ORDER_INSTANCE_ID))).thenReturn(computeOrderInstance);

		this.attendSpawningOrdersThread.processSpawningOrder(order);	
		Assert.assertEquals(OrderState.SPAWNING, order.getOrderState());
		
		// Order Active
		computeOrderInstance.setState(InstanceState.ACTIVE);
		Mockito.when(this.computePĺugin.getInstance(Mockito.any(Token.class), Mockito.eq(DEFAULT_ORDER_INSTANCE_ID))).thenReturn(computeOrderInstance);
		
		TunnelingServiceUtil tunnelingServiceUtil = Mockito.mock(TunnelingServiceUtil.class);
		this.attendSpawningOrdersThread.setTunnelingServiceUtil(tunnelingServiceUtil);
		Mockito.doNothing().when(computeOrderInstance).setExternalServiceAddresses(Mockito.anyMap());
		
		SshConnectivityUtil sshConnectivityUtil = Mockito.mock(SshConnectivityUtil.class);
		this.attendSpawningOrdersThread.setSshConnectivity(sshConnectivityUtil);
		Mockito.when(sshConnectivityUtil.isActiveConnection()).thenReturn(true);
		
		this.attendSpawningOrdersThread.processSpawningOrder(order);		
		Assert.assertEquals(OrderState.FULFILLED, order.getOrderState());
	}
	
	private Order createOrder() {
		Token localToken = Mockito.mock(Token.class);
		Token federationToken = Mockito.mock(Token.class);
		UserData userData = Mockito.mock(UserData.class);
		String imageName = "fake-image-name";
		String requestingMember = String.valueOf(this.properties.get(ConfigurationConstants.XMPP_ID_KEY));
		String providingMember = String.valueOf(this.properties.get(ConfigurationConstants.XMPP_ID_KEY));
		Order order = new ComputeOrder(localToken, federationToken, requestingMember, providingMember, 8, 1024, 30,
				imageName, userData);
		OrderState state = OrderState.SPAWNING;
		OrderInstance orderInstance = new OrderInstance(DEFAULT_ORDER_INSTANCE_ID);
		order.setOrderInstance(orderInstance);
		Mockito.doNothing().when(this.orderRegistry).updateOrder(Mockito.any(Order.class));
		order.setOrderState(state, this.orderRegistry);
		return order;
	}
	
}
