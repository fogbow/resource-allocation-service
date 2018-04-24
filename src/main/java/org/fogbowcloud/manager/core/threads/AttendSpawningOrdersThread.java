package org.fogbowcloud.manager.core.threads;

import org.fogbowcloud.manager.core.models.orders.Order;
import org.fogbowcloud.manager.core.models.orders.OrderRegistry;
import org.fogbowcloud.manager.core.models.orders.OrderState;
import org.fogbowcloud.manager.core.models.orders.OrderType;
import org.fogbowcloud.manager.core.models.orders.instances.ComputeOrderInstance;
import org.fogbowcloud.manager.core.models.orders.instances.InstanceState;
import org.fogbowcloud.manager.core.models.orders.instances.OrderInstance;

public class AttendSpawningOrdersThread extends Thread {

	private OrderRegistry orderRegistry;

	@Override
	public void run() {
		while (true) {
			try {
				Order order = this.orderRegistry.getNextOrderByState(OrderState.SPAWNING);
				if (order != null) {
					synchronized (order) {
						OrderState orderState = order.getOrderState();

						if (!orderState.equals(OrderState.SPAWNING)) {
							continue;
						}
						
						// get instance of the cloud...
						OrderInstance orderInstance = order.getOrderInstance();
						// check if type of the order is COMPUTE...
						if (order.getType().equals(OrderType.COMPUTE)) {
							// The attribute InstanceState is in ComputeOrderInstance, and not
							// OrderInstance...
							ComputeOrderInstance computeOrderInstance = (ComputeOrderInstance) orderInstance;
							// check if the instance is running...
							if (computeOrderInstance.getState().equals(InstanceState.ACTIVE)) {
								// try to communicate by SSH...
								computeOrderInstance = waitForAnswer(order);

								// if successfully do...
								/// remove Order from spawningOrders...
								//this.orderRegistry.removeFromSpwaningOrders(order);
								/// set Order state to FULFILLED...
								order.setOrderState(OrderState.FULFILLED, this.orderRegistry);
								/// insert Order in fulfilledOrders...
								// this.orderRegistry.insertIntoSpwaningOrders(order);

								// if failed after a few attempts do...
								/// remove Order from spawningOrders
								/// set OrderState to FALLED...
								/// insert Order in failedOrders...
								changeOrderStateToFalled(order);

							} else if (computeOrderInstance.getState().equals(InstanceState.INACTIVE)) {
								changeOrderStateToFalled(order);
							}

						}

					}


				} else {
					Thread.sleep(NORM_PRIORITY);
				}
			} catch (Exception e) {
				// TODO: handle exception
			}
		}
	}

	private void changeOrderStateToFalled(Order order) {
		/// remove Order from spawningOrders
		//this.orderRegistry.removeFromSpwaningOrders(order);									
		/// set OrderState to FALLED...
		order.setOrderState(OrderState.FAILED, this.orderRegistry);
		/// insert Order in failedOrders...
		//this.orderRegistry.insertIntoSpwaningOrders(order);
	}
	
	private ComputeOrderInstance waitForAnswer(Order order) {
		int attempts = 3;
		while (attempts > 0) {
			try {
				// try to get IP and Port of the instance...
				Thread.sleep(NORM_PRIORITY);
			} catch (Exception e) {
				// TODO: handle exception
			}
		}
		return null;
	}

}
