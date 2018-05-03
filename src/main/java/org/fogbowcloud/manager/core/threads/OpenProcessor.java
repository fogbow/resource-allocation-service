package org.fogbowcloud.manager.core.threads;

import java.util.Properties;

import org.apache.log4j.Logger;
import org.fogbowcloud.manager.core.constants.ConfigurationConstants;
import org.fogbowcloud.manager.core.constants.DefaultConfigurationConstants;
import org.fogbowcloud.manager.core.datastructures.OrderStateTransitioner;
import org.fogbowcloud.manager.core.datastructures.SharedOrderHolders;
import org.fogbowcloud.manager.core.exceptions.OrderStateTransitionException;
import org.fogbowcloud.manager.core.instanceprovider.InstanceProvider;
import org.fogbowcloud.manager.core.models.linkedList.ChainedList;
import org.fogbowcloud.manager.core.models.orders.Order;
import org.fogbowcloud.manager.core.models.orders.OrderState;
import org.fogbowcloud.manager.core.models.orders.instances.OrderInstance;

public class OpenProcessor implements Runnable {

	private InstanceProvider localInstanceProvider;
	private InstanceProvider remoteInstanceProvider;

	private String localMemberId;

	private ChainedList openOrdersList;

	/**
	 * Attribute that represents the thread sleep time when there is no Orders
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

		String schedulerPeriodStr = properties.getProperty(ConfigurationConstants.OPEN_ORDERS_SLEEP_TIME_KEY,
				DefaultConfigurationConstants.OPEN_ORDERS_SLEEP_TIME);
		this.sleepTime = Long.valueOf(schedulerPeriodStr);
	}

	@Override
	public void run() {
		boolean isActive = true;
		while (isActive) {
			try {
				Order order = this.openOrdersList.getNext();
				if (order != null) {
					try {
						this.processOpenOrder(order);
					} catch (OrderStateTransitionException e) {
						LOGGER.error("Error while trying to changing the state of order " + order, e);
					}
				} else {
					this.openOrdersList.resetPointer();
					LOGGER.info(
							"There is no open order to be processed, sleeping for " + this.sleepTime + " milliseconds");
					Thread.sleep(this.sleepTime);
				}
			} catch (InterruptedException e) {
				isActive = false;
				LOGGER.warn("Thread interrupted", e);
			} catch (Throwable e) {
				// We are not sure about what do if the thread catch a
				// Throwable. For example: stop the thread and the
				// fogbow-manager or continue the thread normally.
				LOGGER.error("Not expected error", e);
			}
		}
	}

	/**
	 * Get an Instance for an Open Order. If the method fail in get the
	 * Instance, then the Order is set to FAILED, else, is set to SPAWNING if
	 * the Order is local or PENDING if the Order is remote.
	 * 
	 * @param order
	 * @throws OrderStateTransitionException
	 */
	protected void processOpenOrder(Order order) throws OrderStateTransitionException {
		// The order object synchronization is needed to prevent a race
		// condition on order access. For example: a user can delete a Open
		// Order while this method is trying to get an Instance for this Order.
		synchronized (order) {
			OrderState orderState = order.getOrderState();

			if (orderState.equals(OrderState.OPEN)) {
				LOGGER.info("Trying to get an instance for order [" + order.getId() + "]");

				try {
					InstanceProvider instanceProvider = this.getInstanceProviderForOrder(order);

					// TODO: prepare Order to Change your State from Open to
					// Spawning.

					LOGGER.info("Processing order [" + order.getId() + "]");
					OrderInstance orderInstance = instanceProvider.requestInstance(order);
					order.setOrderInstance(orderInstance);

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
	 * Update the Order State and do the right Order State Transition.
	 * 
	 * @param order
	 * @throws OrderStateTransitionException
	 */
	private void updateOrderStateAfterProcessing(Order order) throws OrderStateTransitionException {
		if (order.isLocal(this.localMemberId)) {
			OrderInstance orderInstance = order.getOrderInstance();
			String orderInstanceId = orderInstance.getId();

			if (!orderInstanceId.isEmpty()) {
				LOGGER.info("The open order [" + order.getId() + "] got an local instance with id [" + orderInstanceId
						+ "], setting your state to SPAWNING");

				LOGGER.info("Transition [" + order.getId() + "] order state from open to spawning");
				OrderStateTransitioner.transition(order, OrderState.SPAWNING);

			} else {
				LOGGER.error("Order instance id for order [" + order.getId() + "] is empty");
				throw new IllegalArgumentException("Order instance id for order [" + order.getId() + "] is empty");
			}

		} else {
			LOGGER.info("The open order [" + order.getId()
					+ "] was requested for remote member, setting your state to pending");

			LOGGER.info("Transition [" + order.getId() + "] order state from open to pending");
			OrderStateTransitioner.transition(order, OrderState.PENDING);
		}
	}

	/**
	 * Get an Instance Provider for an Order, if the Order is Local, the
	 * returned Instance Provider is the Local, else, is the Remote.
	 * 
	 * @param order
	 * @return Local InstanceProvider if the Order is Local, or Remote
	 *         InstanceProvider if the Order is Remote
	 */
	private InstanceProvider getInstanceProviderForOrder(Order order) {
		InstanceProvider instanceProvider = null;
		if (order.isLocal(this.localMemberId)) {
			LOGGER.info("The open order [" + order.getId() + "] is local");

			instanceProvider = this.localInstanceProvider;
		} else {
			LOGGER.info("The open order [" + order.getId() + "] is remote for the member [" + order.getProvidingMember()
					+ "]");

			instanceProvider = this.remoteInstanceProvider;
		}
		return instanceProvider;
	}

}
