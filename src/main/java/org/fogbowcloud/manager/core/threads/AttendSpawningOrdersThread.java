package org.fogbowcloud.manager.core.threads;

import java.util.Map;

import org.apache.log4j.Logger;
import org.fogbowcloud.manager.core.datastructures.OrderStateTransitioner;
import org.fogbowcloud.manager.core.datastructures.SharedOrderHolders;
import org.fogbowcloud.manager.core.exceptions.OrderStateTransitionException;
import org.fogbowcloud.manager.core.models.orders.Order;
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

	private ComputePlugin computePlugin;
	private Long sleepTime;
	private TunnelingServiceUtil tunnelingService;
	private SshConnectivityUtil sshConnectivity;
	
	public AttendSpawningOrdersThread() {}
	
	public AttendSpawningOrdersThread(ComputePlugin computePlugin, Long sleepTime) {
		this.computePlugin = computePlugin;
		this.sleepTime = sleepTime;
		this.tunnelingService = TunnelingServiceUtil.getInstance();
		this.sshConnectivity = SshConnectivityUtil.getInstance();
	}
	
	@Override
	public void run() {
		while(true) {
			Order order = null;
			try {
				order = SharedOrderHolders.getInstance().getSpawningOrdersList().getNext();
				if (order != null) {
					this.processSpawningOrder(order);
				} else {
					LOGGER.info("There is no spawning order to be processed, sleeping attend spawning orders thread.");
					Thread.sleep(this.sleepTime);
				}
			} catch (Throwable e) {
				String orderId = order != null ? order.getId() : null;
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
					OrderInstance orderInstance = order.getOrderInstance();
					if (order.getType().equals(OrderType.COMPUTE)) {
						processeComputeInstance(order, orderInstance);
					}					
				} catch (Exception e) {
					LOGGER.error("Error while trying to get an instance for order: " + System.lineSeparator() + order, e);
				}
			} else {
				LOGGER.info("This order state is not spawning for order [" + order.getId() + "]");
			}
		}
	}

	private void processeComputeInstance(Order order, OrderInstance orderInstance) throws OrderStateTransitionException {
		ComputeOrderInstance computeOrderInstance = null;
		try {
			computeOrderInstance = this.computePlugin.getInstance(order.getLocalToken(), orderInstance.getId());
			if (computeOrderInstance == null || computeOrderInstance.getState().equals(InstanceState.FAILED)) {
				OrderStateTransitioner.transition(order, OrderState.FAILED);
			} else {
				if (computeOrderInstance.getState().equals(InstanceState.ACTIVE)) {
					LOGGER.info("Processing active compute instance for order [" + order.getId() + "]");
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
		} catch (Exception e) {
			LOGGER.error("Error while trying to get the compute instance for order: " + System.lineSeparator() + order, e);
		}
	}

	private void setTunnelingServiceAddresses(Order order, ComputeOrderInstance computeOrderInstance) {
		try {
			Map<String, String> externalServiceAddresses = tunnelingService.getExternalServiceAddresses(order.getId());
			if (externalServiceAddresses != null) {
				computeOrderInstance.setExternalServiceAddresses(externalServiceAddresses);
			}	
		} catch (Exception e) {
			LOGGER.error("Error trying to get map of addresses (IP and Port) of the compute instance for order: " + System.lineSeparator() + order, e); // DONE!
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
