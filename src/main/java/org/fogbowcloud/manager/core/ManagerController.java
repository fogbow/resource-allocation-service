package org.fogbowcloud.manager.core;

import java.util.Date;
import java.util.Properties;
import java.util.TimerTask;
import java.util.concurrent.Executors;
import org.apache.log4j.Logger;
import org.fogbowcloud.manager.core.constants.ConfigurationConstants;
import org.fogbowcloud.manager.core.constants.DefaultConfigurationConstants;
import org.fogbowcloud.manager.core.instanceprovider.InstanceProvider;
import org.fogbowcloud.manager.core.models.orders.Order;
import org.fogbowcloud.manager.core.models.orders.OrderRegistry;
import org.fogbowcloud.manager.core.models.orders.OrderState;
import org.fogbowcloud.manager.core.models.orders.instances.OrderInstance;

public class ManagerController {

	private OrderRegistry orderRegistry;
	private ManagerScheduledExecutorService attendOpenOrdersExecutor;

	private InstanceProvider localInstanceProvider;
	private InstanceProvider remoteInstanceProvider;
	
	private String localMemberId;

	private static final Logger LOGGER = Logger.getLogger(ManagerController.class);
	
	public ManagerController(Properties properties) {
		this.localMemberId = properties.getProperty(ConfigurationConstants.XMPP_ID_KEY);
		this.attendOpenOrdersExecutor = new ManagerScheduledExecutorService(Executors.newScheduledThreadPool(1));

		this.scheduleExecutorsServices(properties);
	}

	private void scheduleExecutorsServices(Properties properties) {
		if (!this.attendOpenOrdersExecutor.isScheduled()) {
			String schedulerPeriodStr = properties.getProperty(ConfigurationConstants.OPEN_ORDERS_SCHEDULER_PERIOD_KEY,
					DefaultConfigurationConstants.OPEN_ORDERS_SCHEDULER_PERIOD);
			Long schedulerPeriod = Long.valueOf(schedulerPeriodStr);
			this.attendOpenOrdersExecutor.scheduleAtFixedRate(new TimerTask() {
				@Override
				public void run() {
					try {
						attendOpenOrders();
					} catch (Throwable e) {
						LOGGER.error("Error while trying to attend open orders", e);
					}
				}
			}, 0, schedulerPeriod);
		}
	}

	/**
	 * Method that try to get an Instance for an Open Order. This method can
	 * generate a race condition. For example: a user can delete a Open Order
	 * while this method is trying to get an Instance for this Order.
	 */
	private synchronized void attendOpenOrders() {
		Order order = this.orderRegistry.getNextOpenOrder();
		if (order != null) {
			LOGGER.info("Trying to get an Instance for Order [" + order.getId() + "]");

			try {
				InstanceProvider instanceProvider = null;
				if (order.isLocal(this.localMemberId)) {
					LOGGER.info("The open order [" + order.getId() + "] is local");
					
					instanceProvider = this.localInstanceProvider;
				} else if (order.isRemote(this.localMemberId)) {
					LOGGER.info("The open order [" + order.getId() + "] is remote for the member ["
							+ order.getProvidingMember() + "]");
					
					instanceProvider = this.remoteInstanceProvider;
				}
				
				order.processOpenOrder(instanceProvider);
				
				if (order.isLocal(this.localMemberId)) {
					OrderInstance orderInstance = order.getOrderInstance();
					String orderInstanceId = orderInstance.getId();
					if (!orderInstanceId.isEmpty()) {
						LOGGER.info("The open order [" + order.getId() + "] got an local instance with id ["
								+ orderInstanceId + "], setting your state to SPAWNING");
						
						order.setOrderState(OrderState.SPAWNING);
					}
				} else if (order.isRemote(this.localMemberId)) {
					LOGGER.info("The open order [" + order.getId()
							+ "] was requested for remote member, setting your state to PENDING");
					
					order.setOrderState(OrderState.PENDING);
				}
			} catch (Exception e) {
				LOGGER.error("Error while trying to get an Instance for Order: " + System.lineSeparator() + order, e);
				order.setOrderState(OrderState.FAILED);
			}
			this.orderRegistry.updateOrder(order);
		}
	}
	
	/**
	 * TODO: this procedure method change all possibles states of an Order to CLOSED, when user try to delete an Order. 
	 */
	private synchronized void setOrderToClosed(String id) {
		Order order = this.orderRegistry.getNextToBeDeletedOrder();
		if (order.getOrderInstance() != null) {				
			try {
				InstanceProvider instanceProvider = null;
				if (order.isLocal(this.localMemberId)) {
					instanceProvider = this.localInstanceProvider;
				} else if (order.isRemote(this.localMemberId)) {
					instanceProvider = this.remoteInstanceProvider;
				}
				instanceProvider.deleteInstance(order);
				if (order.getOrderState().equals(OrderState.FULFILLED)) {
					// TODO: remove attribute fulfilledTime and create initialFulfilledTimestamp and endFulfilledTimestamp in the Order.class...
					order.setFulfilledTime(new Date().getTime()); 
				}				
			} catch (Exception e) {
				// TODO: handle exception
			}			
			order.setOrderState(OrderState.CLOSED);
			this.orderRegistry.updateOrder(order);
		}
	}

	protected void setOrderRegistry(OrderRegistry orderRegistry) {
		this.orderRegistry = orderRegistry;
	}

	protected void setLocalInstanceProvider(InstanceProvider localInstanceProvider) {
		this.localInstanceProvider = localInstanceProvider;
	}

	protected void setRemoteInstanceProvider(InstanceProvider remoteInstanceProvider) {
		this.remoteInstanceProvider = remoteInstanceProvider;
	}

}
