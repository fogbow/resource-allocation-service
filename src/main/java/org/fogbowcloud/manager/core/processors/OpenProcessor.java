package org.fogbowcloud.manager.core.processors;

import java.util.Properties;

import org.apache.log4j.Logger;
import org.fogbowcloud.manager.core.manager.constants.ConfigurationConstants;
import org.fogbowcloud.manager.core.manager.constants.DefaultConfigurationConstants;
import org.fogbowcloud.manager.core.OrderStateTransitioner;
import org.fogbowcloud.manager.core.SharedOrderHolders;
import org.fogbowcloud.manager.core.exceptions.OrderStateTransitionException;
import org.fogbowcloud.manager.core.instanceprovider.InstanceProvider;
import org.fogbowcloud.manager.core.models.linkedlist.ChainedList;
import org.fogbowcloud.manager.core.models.orders.Order;
import org.fogbowcloud.manager.core.models.orders.OrderState;
import org.fogbowcloud.manager.core.models.orders.instances.OrderInstance;

public class OpenProcessor implements Runnable {

	private InstanceProvider localInstanceProvider;
	private InstanceProvider remoteInstanceProvider;

	private String localMemberId;

	private ChainedList openOrdersList;

	/**
	 * Attribute that represents the thread sleep time when there is no orders
	 * to be processed.
	 */
	private Long sleepTime;

	private static final Logger LOGGER = Logger.getLogger(OpenProcessor.class);

	public OpenProcessor(InstanceProvider localInstanceProvider, InstanceProvider remoteInstanceProvider,
			Properties properties) {
		this.localInstanceProvider = localInstanceProvider;
		this.remoteInstanceProvider = remoteInstanceProvider;
		this.localMemberId = properties.getProperty(ConfigurationConstants.XMPP_ID_KEY);

		SharedOrderHolders sharedOrderHolders = SharedOrderHolders.getInstance();
		this.openOrdersList = sharedOrderHolders.getOpenOrdersList();

		String sleepTimeStr = properties.getProperty(ConfigurationConstants.OPEN_ORDERS_SLEEP_TIME_KEY,
				DefaultConfigurationConstants.OPEN_ORDERS_SLEEP_TIME);
		this.sleepTime = Long.valueOf(sleepTimeStr);
	}

	/**
	 * Iterates over the open orders list and try to process one open order per
	 * time. The order being null indicates that the iteration is in the end of
	 * the list or the list is empty.
	 */
	@Override
	public void run() {
		boolean isActive = true;
		while (isActive) {
			try {
				Order order = this.openOrdersList.getNext();
				if (order != null) {
					try {
						processOpenOrder(order);
					} catch (OrderStateTransitionException e) {
						LOGGER.error("Error while trying to change the state of order " + order, e);
					}
				} else {
					this.openOrdersList.resetPointer();
					LOGGER.debug(
							"There is no open order to be processed, sleeping for " + this.sleepTime + " milliseconds");
					Thread.sleep(this.sleepTime);
				}
			} catch (InterruptedException e) {
				isActive = false;
				LOGGER.warn("Thread interrupted", e);
			} catch (Throwable e) {
				LOGGER.error("Unexpected error", e);
			}
		}
	}

	/**
	 * Get an instance for an open order. If the method fails to get the
	 * instance, then the order is set to failed, else, is set to spawning or
	 * pending if the order is local or remote, respectively.
	 * 
	 * @param order
	 * @throws OrderStateTransitionException
	 */
	protected void processOpenOrder(Order order) throws OrderStateTransitionException {
		// The order object synchronization is needed to prevent a race
		// condition on order access. For example: a user can delete a open
		// order while this method is trying to get an Instance for this order.
		synchronized (order) {
			OrderState orderState = order.getOrderState();

			// check if after order synchronization its state is still open.
			if (orderState.equals(OrderState.OPEN)) {
				LOGGER.info("Trying to get an instance for order [" + order.getId() + "]");

				try {
					InstanceProvider instanceProvider = this.getInstanceProviderForOrder(order);

					LOGGER.info("Processing order [" + order.getId() + "]");
					String orderInstanceId = instanceProvider.requestInstance(order);
					order.setOrderInstance(new OrderInstance(orderInstanceId));

					LOGGER.info("Updating order state after processing [" + order.getId() + "]");
					this.updateOrderStateAfterProcessing(order);
				} catch (Exception e) {
					LOGGER.error("Error while trying to get an instance for order: " + order, e);

					LOGGER.info("Transition [" + order.getId() + "] order state from open to failed");
					OrderStateTransitioner.transition(order, OrderState.FAILED);
				}
			}
		}
	}

	/**
	 * Update the order state and do the order state transition after the open
	 * order process.
	 * 
	 * @param order
	 * @throws OrderStateTransitionException
	 */
	private void updateOrderStateAfterProcessing(Order order) throws OrderStateTransitionException {
		if (order.isLocal(this.localMemberId)) {
			OrderInstance orderInstance = order.getOrderInstance();
			String orderInstanceId = orderInstance.getId();

			if (orderInstanceId != null) {
				LOGGER.info("The open order [" + order.getId() + "] got an local instance with id [" + orderInstanceId
						+ "], setting your state to spawning");

				LOGGER.info("Transition [" + order.getId() + "] order state from open to spawning");
				OrderStateTransitioner.transition(order, OrderState.SPAWNING);

			} else {
				LOGGER.error("Order instance id for order [" + order.getId() + "] is null");
				throw new IllegalArgumentException("Order instance id for order [" + order.getId() + "] is null");
			}

		} else {
			LOGGER.info("The open order [" + order.getId()
					+ "] was requested for remote member, setting your state to pending");

			LOGGER.info("Transition [" + order.getId() + "] order state from open to pending");
			OrderStateTransitioner.transition(order, OrderState.PENDING);
		}
	}

	/**
	 * Get an instance provider for an order, if the order is Local, the
	 * returned instance provider is the local, else, is the remote.
	 * 
	 * @param order
	 * @return InstanceProvider, if the order is local, then returns the local
	 *         instance provider, if the order is remote, then returns the
	 *         remote instance provider.
	 */
	private InstanceProvider getInstanceProviderForOrder(Order order) {
		InstanceProvider instanceProvider = null;
		if (order.isLocal(this.localMemberId)) {
			LOGGER.debug("The open order [" + order.getId() + "] is local");

			instanceProvider = this.localInstanceProvider;
		} else {
			LOGGER.debug("The open order [" + order.getId() + "] is remote for the member [" + order.getProvidingMember()
					+ "]");

			instanceProvider = this.remoteInstanceProvider;
		}
		return instanceProvider;
	}

}
