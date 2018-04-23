package org.fogbowcloud.manager.core.threads;

import java.util.Properties;

import org.apache.log4j.Logger;
import org.fogbowcloud.manager.core.constants.ConfigurationConstants;
import org.fogbowcloud.manager.core.constants.DefaultConfigurationConstants;
import org.fogbowcloud.manager.core.instanceprovider.InstanceProvider;
import org.fogbowcloud.manager.core.models.orders.Order;
import org.fogbowcloud.manager.core.models.orders.OrderRegistry;
import org.fogbowcloud.manager.core.models.orders.OrderState;
import org.fogbowcloud.manager.core.models.orders.instances.OrderInstance;

public class AttendOpenOrdersThread extends Thread {

	private InstanceProvider localInstanceProvider;
	private InstanceProvider remoteInstanceProvider;

	private OrderRegistry orderRegistry;

	private String localMemberId;

	/**
	 * Attribute that represents the thread sleep time when there is no Orders
	 * to be processed.
	 */
	private Long sleepTime;

	private static final Logger LOGGER = Logger.getLogger(AttendOpenOrdersThread.class);

	public AttendOpenOrdersThread(InstanceProvider localInstanceProvider, InstanceProvider remoteInstanceProvider,
			OrderRegistry orderRegistry, String localMemberId, Properties properties) {
		this.localInstanceProvider = localInstanceProvider;
		this.remoteInstanceProvider = remoteInstanceProvider;
		this.orderRegistry = orderRegistry;
		this.localMemberId = localMemberId;

		String schedulerPeriodStr = properties.getProperty(ConfigurationConstants.OPEN_ORDERS_SLEEP_TIME_KEY,
				DefaultConfigurationConstants.OPEN_ORDERS_SLEEP_TIME);
		this.sleepTime = Long.valueOf(schedulerPeriodStr);
	}

	/**
	 * Method that try to get an Instance for an Open Order. This method can
	 * generate a race condition. For example: a user can delete a Open Order
	 * while this method is trying to get an Instance for this Order.
	 * 
	 * What this method should do?
	 */
	@Override
	public void run() {
		while (true) {
			try {
				Order order = this.orderRegistry.getNextOpenOrder();
				if (order != null) {
					synchronized (order) {
						OrderState orderState = order.getOrderState();

						if (orderState.equals(OrderState.OPEN)) {
							LOGGER.info("Trying to get an Instance for Order [" + order.getId() + "]");

							try {
								InstanceProvider instanceProvider = this.getInstanceProviderForOrder(order);

								order.processOpenOrder(instanceProvider);

								if (order.isLocal(this.localMemberId)) {
									OrderInstance orderInstance = order.getOrderInstance();
									String orderInstanceId = orderInstance.getId();
									if (!orderInstanceId.isEmpty()) {
										LOGGER.info(
												"The open order [" + order.getId() + "] got an local instance with id ["
														+ orderInstanceId + "], setting your state to SPAWNING");

										order.setOrderState(OrderState.SPAWNING, this.orderRegistry);
									}
								} else if (order.isRemote(this.localMemberId)) {
									LOGGER.info("The open order [" + order.getId()
											+ "] was requested for remote member, setting your state to PENDING");

									order.setOrderState(OrderState.PENDING, this.orderRegistry);
								}
							} catch (Exception e) {
								LOGGER.error("Error while trying to get an Instance for Order: "
										+ System.lineSeparator() + order, e);
								order.setOrderState(OrderState.FAILED, this.orderRegistry);
							}
						}
					}
				} else {
					Thread.sleep(this.sleepTime);
				}
			} catch (Exception e) {
				LOGGER.error("Error while trying to sleep Attend Order Thread", e);
			}
		}
	}

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

}
