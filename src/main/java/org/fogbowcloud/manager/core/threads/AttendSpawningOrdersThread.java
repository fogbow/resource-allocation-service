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
					LOGGER.info("There is no Spawning Order to be processed, sleeping Attend Spawning Orders Thread...");
					Thread.sleep(this.sleepTime);
				}
			} catch (Throwable e) {
				LOGGER.error("Error while trying to sleep Attend Order Thread", e);
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
						if (computeOrderInstance.getState().equals(InstanceState.ACTIVE)) {
							try {
								TunnelingServiceUtil tunnelingServiceUtil = new TunnelingServiceUtil();
								computeOrderInstance.setExternalServiceAddresses(
										tunnelingServiceUtil.getExternalServiceAddresses(order.getId()));
								SshConnectivityUtil sshConnectivity = new SshConnectivityUtil(computeOrderInstance);
								if (sshConnectivity.isActiveConnection()) {
									this.updateSpawningStateAfterProcessing(order);
								}
							} catch (Exception e) {
								// TODO exception
							}
							this.updateSpawningStateAfterProcessing(order);
						}
						if (computeOrderInstance.getState().equals(InstanceState.INACTIVE)) {
							this.updateSpawningStateAfterProcessing(order);
						}
					}

				} catch (Exception e) {
					LOGGER.error("Error while trying to get an Instance for Order: " + System.lineSeparator() + order,
							e);
					// TODO to specify why it failed
					order.setOrderState(OrderState.FAILED, this.orderRegistry);
				}
			}
		}
	}

	private void updateSpawningStateAfterProcessing(Order order) {
		
		// TODO remove Order from spawningOrders ...
		order.setOrderState(OrderState.FULFILLED, orderRegistry);
		// TODO insert Order in fulfilledOrders...
		
		// TODO remove Order from spawningOrders ...
		order.setOrderState(OrderState.FAILED, orderRegistry);
		// TODO insert Order in failedOrders...
	}	

}
