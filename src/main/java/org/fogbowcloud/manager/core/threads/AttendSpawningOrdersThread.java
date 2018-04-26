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

	private ComputePlugin computePĺugin;
	private Long sleepTime;
	private OrderRegistry orderRegistry;
	
	@Override
	public void run() {
		while(true) {
			try {
				Order order = this.orderRegistry.getNextOrderByState(OrderState.SPAWNING);
				if (order != null) {
					this.processSpawningOrder(order);
				} else {
					LOGGER.info("There is no spawning order to be processed, sleeping attend spawning orders thread.");
					Thread.sleep(this.sleepTime);
				}
			} catch (Throwable e) {
				LOGGER.error("Error while trying to sleep attend order thread", e);
			}
		}
	}
	
	private void processSpawningOrder(Order order) {
		synchronized (order) {
			OrderState orderState = order.getOrderState();
			
			if (orderState.equals(OrderState.SPAWNING)) {
				LOGGER.info("Trying to get an instance for order [" + order.getId() + "]");
				
				try {
					OrderInstance orderInstance = order.getOrderInstance();
					if (order.getType().equals(OrderType.COMPUTE)) {
						ComputeOrderInstance computeOrderInstance = this.computePĺugin
								.getInstance(order.getLocalToken(), orderInstance.getId());
						
						if (computeOrderInstance.getState().equals(InstanceState.INACTIVE)) {
							this.updateSpawningStateAfterProcessingToFailed(order);
						}
						
						
						if (computeOrderInstance.getState().equals(InstanceState.ACTIVE)) {
							LOGGER.info("Trying to get map of addresses (IP and Port) of an instance for order [" + order.getId() + "]");
							
							try {
								TunnelingServiceUtil tunnelingServiceUtil = new TunnelingServiceUtil();
								computeOrderInstance.setExternalServiceAddresses(
										tunnelingServiceUtil.getExternalServiceAddresses(order.getId()));
								LOGGER.info("Trying to communicate for Check the SSH connectivity of an instance for order [" + order.getId() + "]");
								
								try {
									SshConnectivityUtil sshConnectivity = new SshConnectivityUtil(computeOrderInstance);
									if (sshConnectivity.isActiveConnection()) {
										this.updateSpawningStateAfterProcessingToFulfilled(order);										
									}
								}catch (Exception e) {
									LOGGER.error("Error while trying to communicate for Check the SSH connectivity of an Instance for Order: "
											+ System.lineSeparator() + order, e);
									// set to Failed ?
								}								
								
							} catch (Exception e) {
								LOGGER.error("Error while trying to get map of addresses (IP and Port) of an Instance for Order: "
										+ System.lineSeparator() + order, e);
								// set to Failed ?
							}
						}
						
					}

				} catch (Exception e) {
					LOGGER.error("Error while trying to get an Instance for Order: " + System.lineSeparator() + order,
							e);
					// set to Failed ?
				}
			}
		}
	}

	private void updateSpawningStateAfterProcessingToFulfilled(Order order) {
		// TODO remove Order from spawningOrders ...
		order.setOrderState(OrderState.FULFILLED, orderRegistry);
		// TODO insert Order in fulfilledOrders...		
	}

	private void updateSpawningStateAfterProcessingToFailed(Order order) {
		// TODO remove Order from spawningOrders ...
		order.setOrderState(OrderState.FAILED, orderRegistry);
		// TODO insert Order in failedOrders...
	}	

}
