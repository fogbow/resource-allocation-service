package org.fogbowcloud.manager.core.threads;

import java.util.Map;
import java.util.Properties;

import org.apache.log4j.Logger;
import org.fogbowcloud.manager.core.constants.ConfigurationConstants;
import org.fogbowcloud.manager.core.constants.DefaultConfigurationConstants;
import org.fogbowcloud.manager.core.datastructures.OrderStateTransitioner;
import org.fogbowcloud.manager.core.datastructures.SharedOrderHolders;
import org.fogbowcloud.manager.core.exceptions.OrderStateTransitionException;
import org.fogbowcloud.manager.core.instanceprovider.InstanceProvider;
import org.fogbowcloud.manager.core.models.linkedList.SynchronizedDoublyLinkedList;
import org.fogbowcloud.manager.core.models.orders.Order;
import org.fogbowcloud.manager.core.models.orders.OrderState;
import org.fogbowcloud.manager.core.models.orders.OrderType;
import org.fogbowcloud.manager.core.models.orders.instances.ComputeOrderInstance;
import org.fogbowcloud.manager.core.models.orders.instances.InstanceState;
import org.fogbowcloud.manager.core.models.orders.instances.OrderInstance;
import org.fogbowcloud.manager.core.utils.SshConnectivityUtil;
import org.fogbowcloud.manager.core.utils.TunnelingServiceUtil;

public class SpawningMonitor extends Thread {

	private static final Logger LOGGER = Logger.getLogger(SpawningMonitor.class);

	private InstanceProvider localInstanceProvider;
	private SynchronizedDoublyLinkedList spawningOrderList;

	private TunnelingServiceUtil tunnelingService;
	private SshConnectivityUtil sshConnectivity;

	private Long sleepTime;

	public SpawningMonitor(InstanceProvider localInstanceProvider, Properties properties) {
		this.localInstanceProvider = localInstanceProvider;
		SharedOrderHolders sharedOrderHolders = SharedOrderHolders.getInstance();
		this.spawningOrderList = sharedOrderHolders.getSpawningOrdersList();

		TunnelingServiceUtil tunnelingService = TunnelingServiceUtil.getInstance();
		this.tunnelingService = tunnelingService;

		SshConnectivityUtil sshConnectivity = SshConnectivityUtil.getInstance();
		this.sshConnectivity = sshConnectivity;

		String schedulerPeriodStr = properties.getProperty(ConfigurationConstants.OPEN_ORDERS_SLEEP_TIME_KEY,
				DefaultConfigurationConstants.OPEN_ORDERS_SLEEP_TIME);
		this.sleepTime = Long.valueOf(schedulerPeriodStr);
	}

	@Override
	public void run() {
		while (true) {
			Order order = null;
			try {
				order = this.spawningOrderList.getNext();
				if (order != null) {
					this.processSpawningOrder(order);
				} else {
					spawningOrderList.resetPointer();
					LOGGER.info("There is no spawning order to be processed, sleeping for " + this.sleepTime
							+ " milliseconds");
					Thread.sleep(this.sleepTime);
				}
			} catch (Throwable e) {
				String orderId = null;
				if (order != null) {
					orderId = order.getId();
				}
				LOGGER.error("Error while trying to thread sleep attending the order: [" + orderId + "]", e);
			}
		}
	}

	protected void processSpawningOrder(Order order) {
		synchronized (order) {
			OrderState orderState = order.getOrderState();
			if (orderState.equals(OrderState.SPAWNING)) {
				LOGGER.info("Trying to get an instance for order [" + order.getId() + "]");
				try {
					
					this.processInstance(order);
					
				} catch (Exception e) {
					LOGGER.error("Error while trying to get an instance for order: " + System.lineSeparator() + order,
							e);
				}
			} else {
				LOGGER.info("This order state is not spawning for order [" + order.getId() + "]");
			}
		}
	}

	private void processInstance(Order order) throws OrderStateTransitionException {
		// This method does not synchronize the order object because it is private and
		// can only be called by the processSpawningOrder method.

		OrderInstance orderInstance = order.getOrderInstance();
		
		if (order.getType().equals(OrderType.COMPUTE)) {
			if (orderInstance.getState().equals(InstanceState.FAILED)) {
				// TODO: log
				OrderStateTransitioner.transition(order, OrderState.FAILED);
			} else if (orderInstance.getState().equals(InstanceState.ACTIVE)) {
				LOGGER.info("Processing active compute instance for order [" + order.getId() + "]");

				ComputeOrderInstance computeOrderInstance = (ComputeOrderInstance) orderInstance;

				this.setTunnelingServiceAddresses(order, computeOrderInstance);
				if (isActiveConnectionFromInstance(computeOrderInstance)) {
					OrderStateTransitioner.transition(order, OrderState.FULFILLED);
				} else {
					LOGGER.warn("Failed attempt to communicate with ssh connectivity for order [" + order.getId() + "]");
				}
			} else {
				LOGGER.info("The compute instance is inactive for order [" + order.getId() + "]");
			}
		}
	}

	private void setTunnelingServiceAddresses(Order order, ComputeOrderInstance computeOrderInstance) {
		try {
			Map<String, String> externalServiceAddresses = tunnelingService.getExternalServiceAddresses(order.getId());
			if (externalServiceAddresses != null) {
				computeOrderInstance.setExternalServiceAddresses(externalServiceAddresses);
			}
		} catch (Exception e) {
			LOGGER.error("Error trying to get map of addresses (IP and Port) of the compute instance for order: "
					+ System.lineSeparator() + order, e);
		}
	}

	private boolean isActiveConnectionFromInstance(ComputeOrderInstance computeOrderInstance) {
		LOGGER.info("Check the communicate at SSH connectivity of the compute instance.");
		return this.sshConnectivity.checkSSHConnectivity(computeOrderInstance);
	}

	protected void setTunnelingService(TunnelingServiceUtil tunnelingService) {
		this.tunnelingService = tunnelingService;
	}

	protected void setSshConnectivity(SshConnectivityUtil sshConnectivity) {
		this.sshConnectivity = sshConnectivity;
	}

}
