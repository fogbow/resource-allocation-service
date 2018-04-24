package org.fogbowcloud.manager.core.threads;

import java.util.Properties;

import org.apache.log4j.Logger;
import org.fogbowcloud.manager.core.constants.ConfigurationConstants;
import org.fogbowcloud.manager.core.constants.DefaultConfigurationConstants;
import org.fogbowcloud.manager.core.instanceprovider.InstanceProvider;
import org.fogbowcloud.manager.core.models.linkedList.ChainedList;
import org.fogbowcloud.manager.core.models.orders.Order;
import org.fogbowcloud.manager.core.models.orders.OrderState;
import org.fogbowcloud.manager.core.models.orders.instances.OrderInstance;

public class OpenProcessor implements Runnable {

	private InstanceProvider localInstanceProvider;
	private InstanceProvider remoteInstanceProvider;

	private ChainedList openOrdersList;
	private ChainedList pendingOrdersList;
	private ChainedList failedOrdersList;
	private ChainedList spawningOrdersList;

	private String localMemberId;

	/**
	 * Attribute that represents the thread sleep time when there is no Orders
	 * to be processed.
	 */
	private Long sleepTime;

	private static final Logger LOGGER = Logger.getLogger(OpenProcessor.class);

	public OpenProcessor(InstanceProvider localInstanceProvider, InstanceProvider remoteInstanceProvider,
			String localMemberId, Properties properties) {
		this.localInstanceProvider = localInstanceProvider;
		this.remoteInstanceProvider = remoteInstanceProvider;
		this.localMemberId = localMemberId;

		// TODO: ChainedLists instanciation by Singleton pattern.

		String schedulerPeriodStr = properties.getProperty(ConfigurationConstants.OPEN_ORDERS_SLEEP_TIME_KEY,
				DefaultConfigurationConstants.OPEN_ORDERS_SLEEP_TIME);
		this.sleepTime = Long.valueOf(schedulerPeriodStr);
	}

	@Override
	public void run() {
		while (true) {
			try {
				Order order = this.openOrdersList.getNext();
				if (order != null) {
					this.processOpenOrder(order);
				} else {
					LOGGER.info("There is no Open Order to be processed, sleeping OpenProcessor Thread...");
					Thread.sleep(this.sleepTime);
				}
			} catch (Throwable e) {
				LOGGER.error("Error while trying to Process an Open Order with the OpenProcessor Thread", e);
			}
		}
	}

	/**
	 * Method that try to get an Instance for an Open Order. This method can
	 * generate a race condition. For example: a user can delete a Open Order
	 * while this method is trying to get an Instance for this Order.
	 */
	protected void processOpenOrder(Order order) {
		synchronized (order) {
			OrderState orderState = order.getOrderState();

			if (orderState.equals(OrderState.OPEN)) {
				LOGGER.info("Trying to get an Instance for Order [" + order.getId() + "]");

				try {
					InstanceProvider instanceProvider = this.getInstanceProviderForOrder(order);

					LOGGER.info("Processing Order [" + order.getId() + "]");
					order.handleOpenOrder();
					OrderInstance orderInstance = instanceProvider.requestInstance(order);
					order.setOrderInstance(orderInstance);

					LOGGER.info("Removing Order [" + order.getId() + "] from Open Orders List");
					this.openOrdersList.removeItem(order);

					LOGGER.info("Updating Order State after processing [" + order.getId() + "]");
					this.updateOrderStateAfterProcessing(order);

				} catch (Exception e) {
					LOGGER.error("Error while trying to get an Instance for Order: " + System.lineSeparator() + order,
							e);
					order.setOrderState(OrderState.FAILED);

					LOGGER.info("Adding Order [" + order.getId() + "] to Failed Orders List");
					this.failedOrdersList.addItem(order);
				}
			}
		}
	}

	/**
	 * After processing an Open Order, is necessary update it state.
	 * 
	 * @param order
	 */
	protected void updateOrderStateAfterProcessing(Order order) {
		if (order.isLocal(this.localMemberId)) {
			OrderInstance orderInstance = order.getOrderInstance();
			String orderInstanceId = orderInstance.getId();

			if (!orderInstanceId.isEmpty()) {
				LOGGER.info("The open order [" + order.getId() + "] got an local instance with id [" + orderInstanceId
						+ "], setting your state to SPAWNING");

				order.setOrderState(OrderState.SPAWNING);

				LOGGER.info("Adding Order [" + order.getId() + "] to Spawning Orders List");
				this.spawningOrdersList.addItem(order);

			} else {
				LOGGER.error("Order Instance Id for Order [" + order.getId() + "] is Empty");
				throw new RuntimeException("Order Instance Id for Order [" + order.getId() + "] is Empty");
			}

		} else if (order.isRemote(this.localMemberId)) {
			LOGGER.info("The open order [" + order.getId()
					+ "] was requested for remote member, setting your state to PENDING");

			order.setOrderState(OrderState.PENDING);

			LOGGER.info("Adding Order [" + order.getId() + "] to Pending Orders List");
			this.pendingOrdersList.addItem(order);
		}
	}

	/**
	 * Get the Instance Provider for an Order, if the Order is Local, the
	 * returned Instance Provider is the Local, else, is the Remote.
	 * 
	 * @param order
	 * @return
	 */
	protected InstanceProvider getInstanceProviderForOrder(Order order) {
		InstanceProvider instanceProvider = null;
		if (order.isLocal(this.localMemberId)) {
			LOGGER.info("The open order [" + order.getId() + "] is local");

			instanceProvider = this.localInstanceProvider;
		} else if (order.isRemote(this.localMemberId)) {
			LOGGER.info("The open order [" + order.getId() + "] is remote for the member [" + order.getProvidingMember()
					+ "]");

			instanceProvider = this.remoteInstanceProvider;
		}
		return instanceProvider;
	}

	// TODO: all these setters method should be removed when the ChainedList be
	// instanciated by the Singleton Pattern.
	protected void setOpenOrdersList(ChainedList openOrdersList) {
		this.openOrdersList = openOrdersList;
	}

	protected void setPendingOrdersList(ChainedList pendingOrdersList) {
		this.pendingOrdersList = pendingOrdersList;
	}

	protected void setFailedOrdersList(ChainedList failedOrdersList) {
		this.failedOrdersList = failedOrdersList;
	}

	protected void setSpawningOrdersList(ChainedList spawningOrdersList) {
		this.spawningOrdersList = spawningOrdersList;
	}

}
