package org.fogbowcloud.manager.core.threads;

import java.util.Map;

import org.apache.log4j.Logger;
import org.fogbowcloud.manager.core.models.orders.Order;
import org.fogbowcloud.manager.core.models.orders.OrderRegistry;
import org.fogbowcloud.manager.core.models.orders.OrderState;
import org.fogbowcloud.manager.core.models.orders.OrderType;
import org.fogbowcloud.manager.core.models.orders.instances.ComputeOrderInstance;
import org.fogbowcloud.manager.core.models.orders.instances.InstanceState;
import org.fogbowcloud.manager.core.models.orders.instances.OrderInstance;
import org.fogbowcloud.manager.core.plugins.ComputePlugin;
import org.fogbowcloud.manager.core.utils.SshConnectivityUtil;
import org.fogbowcloud.manager.core.utils.TunnelingServiceUtil;

public class AttendSpawningOrdersThread extends Thread {

	private static final Logger LOGGER = Logger.getLogger(AttendSpawningOrdersThread.class);

	private OrderRegistry orderRegistry;
	private ComputePlugin computePĺugin;
	private Long sleepTime;
	
	private TunnelingServiceUtil tunnelingService;
	private SshConnectivityUtil sshConnectivity;
	
	public AttendSpawningOrdersThread() {}
	
	public AttendSpawningOrdersThread(OrderRegistry orderRegistry, ComputePlugin computePĺugin, Long sleepTime) {
		this.orderRegistry = orderRegistry;
		this.computePĺugin = computePĺugin;
		this.sleepTime = sleepTime;
	}
	
	@Override
	public void run() {
		while(true) {
			Order order = null;
			try {
				order = this.orderRegistry.getNextOrderByState(OrderState.SPAWNING);
				if (order != null) {
					this.processSpawningOrder(order);
				} else {
					LOGGER.info("There is no spawning order to be processed, sleeping attend spawning orders thread."); // TODO Test?
					Thread.sleep(this.sleepTime);
				}
			} catch (Throwable e) {
				String orderId = order != null ? order.getId() : null;
				LOGGER.error("Error while trying to sleep attending order [" + orderId + "] thread.", e); // Test done!
			}
		}
	}
	
	protected void processSpawningOrder(Order order) {
		synchronized (order) {
			OrderState orderState = order.getOrderState();
			if (orderState.equals(OrderState.SPAWNING)) {
				LOGGER.info("Trying to get an instance for order [" + order.getId() + "]");				
				try {
					OrderInstance orderInstance = order.getOrderInstance();
					if (order.getType().equals(OrderType.COMPUTE)) {
						ComputeOrderInstance computeOrderInstance = null;
						try {
							computeOrderInstance = this.computePĺugin.getInstance(order.getLocalToken(), orderInstance.getId());
						} catch (Exception e) {
							LOGGER.error("Failed to catch the compute order instance for order [" + order.getId() + "]", e); // Test done!
						}						
						if (computeOrderInstance == null || computeOrderInstance.getState().equals(InstanceState.FAILED)) {
							// TODO remove Order from spawningOrders ...
							//order.setOrderState(OrderState.FAILED, orderRegistry); // Test done!
							// TODO insert Order in failedOrders...
						} else {
							LOGGER.info("Processing compute order instance active or inactive for order [" + order.getId() + "]");
							this.processSpawningComputeOrderInstance(order, computeOrderInstance);
						}
					}
				} catch (Exception e) {
					LOGGER.error("Error while trying to get an instance for order: " + System.lineSeparator() + order, e);
				}
			} else {
				LOGGER.info("This order state is not spawning: order [" + order.getId() + "]"); // Test done!
			}
		}
	}
	
	private void processSpawningComputeOrderInstance(Order order, ComputeOrderInstance computeOrderInstance) {
		if (computeOrderInstance.getState().equals(InstanceState.ACTIVE)) {
			LOGGER.info("Trying to get map of addresses (IP and Port) of an instance for order [" + order.getId() + "]");
			try {				
				if (this.isAticveTunnelingServiceAddresses(order, computeOrderInstance)) {
					LOGGER.info("Trying to communicate for check the SSH connectivity of an instance for order [" + order.getId() + "]");
					try {
						if (this.isActiveSshConnectivity(computeOrderInstance)) {
							// TODO remove Order from spawningOrders ...
							//order.setOrderState(OrderState.FULFILLED, orderRegistry); // Test done!
							// TODO insert Order in fulfilledOrders ...
						}
					} catch (Exception e) {
						LOGGER.error("Attempt to communicate with ssh connectivity returned inactive to an instance for order ["	+ order.getId() + "]", e);
					}
				}
			} catch (Exception e) {
				LOGGER.error("Error trying to get map of addresses (IP and Port) of an instance for order: " + order, e);
			}
		} else {
			LOGGER.info("The compute instance is inactive for order [" + order.getId() + "]"); // Test done!
		}
	}

	private boolean isActiveSshConnectivity(ComputeOrderInstance computeOrderInstance) {
		SshConnectivityUtil sshConnectivity = this.sshConnectivity != null ? this.sshConnectivity : new SshConnectivityUtil(computeOrderInstance);		
		return sshConnectivity.isActiveConnection();
	}
	
	private boolean isAticveTunnelingServiceAddresses(Order order, ComputeOrderInstance computeOrderInstance) {
		TunnelingServiceUtil tunnelingService = this.tunnelingService != null ? this.tunnelingService : new TunnelingServiceUtil();
		Map<String, String> externalServiceAddresses = tunnelingService.getExternalServiceAddresses(order.getId());
		if (externalServiceAddresses != null) {
			computeOrderInstance.setExternalServiceAddresses(externalServiceAddresses);
			return true;
		}
		return false;
	}
	
	protected void setTunnelingService(TunnelingServiceUtil tunnelingService) {
		this.tunnelingService = tunnelingService;
	}
	
	protected void setSshConnectivity(SshConnectivityUtil sshConnectivity) {
		this.sshConnectivity = sshConnectivity;
	}

}
