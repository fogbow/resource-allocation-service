package org.fogbowcloud.manager.core.threads;

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
	
	private TunnelingServiceUtil tunnelingServiceUtil;
	private SshConnectivityUtil sshConnectivity;
	
	public AttendSpawningOrdersThread(OrderRegistry orderRegistry, ComputePlugin computePĺugin, Long sleepTime) {
		this.orderRegistry = orderRegistry;
		this.computePĺugin = computePĺugin;
		this.sleepTime = sleepTime;
	}

	@SuppressWarnings("null")
	@Override
	public void run() {
		while(true) {
			Order order = null;
			try {
				order = this.orderRegistry.getNextOrderByState(OrderState.SPAWNING);
				if (order != null) {
					this.processSpawningOrder(order);
				} else {
					LOGGER.info("There is no spawning order to be processed, sleeping attend spawning orders thread for order [" + order.getId() + "]");
					Thread.sleep(this.sleepTime);
				}
			} catch (Throwable e) {
				if (order == null) {
					LOGGER.info("Trying to get an instance for order [" + order.getId() + "]");
				}
				LOGGER.error("Error while trying to sleep attend order thread", e);
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
						ComputeOrderInstance computeOrderInstance = this.computePĺugin
								.getInstance(order.getLocalToken(), orderInstance.getId());
						
						if (computeOrderInstance.getState().equals(InstanceState.FAILED)) {
							this.updateSpawningStateToFailed(order);
						}
						
						LOGGER.info("Processing compute instance for order [" + order.getId() + "]");
						this.processSpawningComputeOrderInstanceActive(order, computeOrderInstance);						
					}

				} catch (Exception e) {
					LOGGER.error("Error while trying to get an instance for order: " + System.lineSeparator() + order, e);
				}
			} else {
				LOGGER.info("this instance not state spawning for order [" + order.getId() + "]");
			}
		}
	}

	private void processSpawningComputeOrderInstanceActive(Order order, ComputeOrderInstance computeOrderInstance) {
		if (computeOrderInstance.getState().equals(InstanceState.ACTIVE)) {
			LOGGER.info("Trying to get map of addresses (IP and Port) of an instance for order [" + order.getId() + "]");

			try {
				this.loadTunnelingServiceAddresses(order, computeOrderInstance);
				LOGGER.info("Trying to communicate for check the SSH connectivity of an instance for order [" + order.getId() + "]");

				if (this.isActiveSshConnectivity(computeOrderInstance)) {
					// TODO remove Order from spawningOrders ...
					order.setOrderState(OrderState.FULFILLED, orderRegistry);
					// TODO insert Order in fulfilledOrders...
				} else {
					LOGGER.info("Trying to communicate for the SSH connectivity return inactive of an instance for order ["	+ order.getId() + "]");
				}

			} catch (Exception e) {
				LOGGER.error("Error while trying to get map of addresses (IP and Port) of an instance for order: " + order, e);
			}
		}
	}

	private boolean isActiveSshConnectivity(ComputeOrderInstance computeOrderInstance) {
		SshConnectivityUtil sshConnectivity = this.sshConnectivity != null 
				? this.sshConnectivity : new SshConnectivityUtil(computeOrderInstance);		
		return sshConnectivity.isActiveConnection();
	}
	
	private void loadTunnelingServiceAddresses(Order order, ComputeOrderInstance computeOrderInstance) {
		TunnelingServiceUtil tunnelingServiceUtil = 
				this.tunnelingServiceUtil != null ? this.tunnelingServiceUtil : new TunnelingServiceUtil();
		computeOrderInstance.setExternalServiceAddresses(
				tunnelingServiceUtil.getExternalServiceAddresses(order.getId()));
	}

	private void updateSpawningStateToFailed(Order order) {
		// TODO remove Order from spawningOrders ...
		order.setOrderState(OrderState.FAILED, orderRegistry);
		// TODO insert Order in failedOrders...
	}	
	
	protected void setTunnelingServiceUtil(TunnelingServiceUtil tunnelingServiceUtil) {
		this.tunnelingServiceUtil = tunnelingServiceUtil;
	}
	
	protected void setSshConnectivity(SshConnectivityUtil sshConnectivity) {
		this.sshConnectivity = sshConnectivity;
	}

}
