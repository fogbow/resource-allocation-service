package org.fogbowcloud.manager.core.threads;

import org.apache.log4j.Logger;
import org.fogbowcloud.manager.core.constants.ConfigurationConstants;
import org.fogbowcloud.manager.core.constants.DefaultConfigurationConstants;
import org.fogbowcloud.manager.core.datastructures.OrderStateTransitioner;
import org.fogbowcloud.manager.core.datastructures.SharedOrderHolders;
import org.fogbowcloud.manager.core.exceptions.OrderStateTransitionException;
import org.fogbowcloud.manager.core.models.linkedList.SynchronizedDoublyLinkedList;
import org.fogbowcloud.manager.core.models.orders.Order;
import org.fogbowcloud.manager.core.models.orders.OrderState;
import org.fogbowcloud.manager.core.models.orders.OrderType;
import org.fogbowcloud.manager.core.models.orders.instances.ComputeOrderInstance;
import org.fogbowcloud.manager.core.models.orders.instances.InstanceState;
import org.fogbowcloud.manager.core.models.orders.instances.OrderInstance;
import org.fogbowcloud.manager.core.utils.ComputeInstanceConnectivityChecker;
import org.fogbowcloud.manager.core.utils.SshConnectivityUtil;
import org.fogbowcloud.manager.core.utils.TunnelingServiceUtil;

import java.util.Properties;

// FIXME change the name to SpawningProcessor
public class SpawningProcessor extends Thread {

	private static final Logger LOGGER = Logger.getLogger(SpawningProcessor.class);

	private SynchronizedDoublyLinkedList spawningOrderList;
	private ComputeInstanceConnectivityChecker computeInstanceConnectivity;
	private Long sleepTime;

	public SpawningProcessor(TunnelingServiceUtil tunnelingService, SshConnectivityUtil sshConnectivity,
							 Properties properties) {
		this.computeInstanceConnectivity = new ComputeInstanceConnectivityChecker(tunnelingService, sshConnectivity);

		SharedOrderHolders sharedOrderHolders = SharedOrderHolders.getInstance();
		this.spawningOrderList = sharedOrderHolders.getSpawningOrdersList();

		String schedulerPeriodStr = properties.getProperty(ConfigurationConstants.SPAWNING_ORDERS_SLEEP_TIME_KEY,
				DefaultConfigurationConstants.SPAWNING_ORDERS_SLEEP_TIME);
		this.sleepTime = Long.valueOf(schedulerPeriodStr);
	}

	@Override
	public void run() {
		boolean isActive = true;
		while (isActive) {
			try {
				Order order = this.spawningOrderList.getNext();
				if (order != null) {
					try {
						this.processSpawningOrder(order);
					} catch (Throwable e) {
						LOGGER.error("Error while trying to process the order " + order, e);
					}
				} else {
					this.spawningOrderList.resetPointer();
					LOGGER.info("There is no spawning order to be processed, sleeping for " + this.sleepTime
							+ " milliseconds");
					Thread.sleep(this.sleepTime);
				}
			} catch (InterruptedException e) {
				isActive = false;
				LOGGER.warn("Thread interrupted", e);
			}
		}
	}

	protected void processSpawningOrder(Order order) {
		synchronized (order) {
			OrderState orderState = order.getOrderState();
			if (orderState.equals(OrderState.SPAWNING)) {
				LOGGER.info("Trying to process an instance for order [" + order.getId() + "]");
				try {
					this.processInstance(order);
				} catch (OrderStateTransitionException e) {
					LOGGER.error("Error while trying to changing the state of order " + order, e);
				}
			} else {
				LOGGER.info("This order state is not spawning for order [" + order.getId() + "]");
			}
		}
	}

	/**
	 * This method does not synchronize the order object because it is private and
	 * can only be called by the processSpawningOrder method.
	 */
	private void processInstance(Order order) throws OrderStateTransitionException {
		OrderInstance orderInstance = order.getOrderInstance();
		OrderType orderType = order.getType();
		
		if (orderType.equals(OrderType.COMPUTE)) {
			InstanceState instanceState = orderInstance.getState();
			
			if (instanceState.equals(InstanceState.FAILED)) {
				LOGGER.info("The compute instance state is failed for order [" + order.getId() + "]");
				OrderStateTransitioner.transition(order, OrderState.FAILED);

			} else if (instanceState.equals(InstanceState.ACTIVE)) {
				LOGGER.info("Processing active compute instance for order [" + order.getId() + "]");

				ComputeOrderInstance computeOrderInstance = (ComputeOrderInstance) orderInstance;
				this.computeInstanceConnectivity.setTunnelingServiceAddresses(order, computeOrderInstance);

				if (this.computeInstanceConnectivity.isInstanceReachable(computeOrderInstance)) {
					OrderStateTransitioner.transition(order, OrderState.FULFILLED);
				}

			} else {
				LOGGER.info("The compute instance state is inactive for order [" + order.getId() + "]");
			}
		}
	}
}
