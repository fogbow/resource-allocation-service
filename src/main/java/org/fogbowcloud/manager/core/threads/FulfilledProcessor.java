package org.fogbowcloud.manager.core.threads;

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

import java.util.Properties;

/**
 * Process orders in fulfilled state. It monitors the resourced that have been
 * successfully initiated, to check for failures that may affect them.
 */
public class FulfilledProcessor implements Runnable {

    private static final Logger LOGGER = Logger.getLogger(FulfilledProcessor.class);

    private InstanceProvider localInstanceProvider;
    private InstanceProvider remoteInstanceProvider;

    private String localMemberId;

    private ChainedList fulfilledOrdersList;

    /**
     * Attribute that represents the thread sleep time when there is no orders
     * to be processed.
     */
    private Long sleepTime;

    public FulfilledProcessor(InstanceProvider localInstanceProvider, InstanceProvider remoteInstanceProvider,
                              Properties properties) {
        this.localInstanceProvider = localInstanceProvider;
        this.remoteInstanceProvider = remoteInstanceProvider;

        this.localMemberId = properties.getProperty(ConfigurationConstants.XMPP_ID_KEY);

        SharedOrderHolders sharedOrderHolders = SharedOrderHolders.getInstance();
        this.fulfilledOrdersList = sharedOrderHolders.getFulfilledOrdersList();

        String schedulerPeriodStr = properties.getProperty(ConfigurationConstants.FULFILLED_ORDERS_SLEEP_TIME_KEY,
                DefaultConfigurationConstants.FULFILLED_ORDERS_SLEEP_TIME);
        this.sleepTime = Long.valueOf(schedulerPeriodStr);
    }

    /**
     * Iterates over the fulfilled orders list and try to process one fulfilled order per
     * time. If the order is null it indicates the iteration is in the end of
     * the list or the list is empty.
     */
    @Override
    public void run() {
        boolean isActive = true;

        while (isActive) {
            try {
                Order order = this.fulfilledOrdersList.getNext();

                if (order != null) {
                    try {
                            this.processFulfilledOrder(order);
                    } catch (Throwable e) {
                            LOGGER.error("Error while trying to process the order" + order, e);
                    }
                } else {
                    this.fulfilledOrdersList.resetPointer();
                    LOGGER.info("There is no fulfilled order to be processed, sleeping " +
                            "for " + this.sleepTime + " milliseconds");
                    Thread.sleep(this.sleepTime);
                }
            } catch (InterruptedException e) {
                isActive = false;
                LOGGER.warn("Thread interrupted", e);
            } catch (Throwable e) {
                LOGGER.error("Not expected error", e);
            }
        }
    }

    /**
     * Get an instance for a fulfilled order. If that instance is not reachable
     * then order state is set to failed.
     *
     * @param order {@link Order}
     * @throws OrderStateTransitionException Could not remove order from list of fulfilled orders.
     */
    private void processFulfilledOrder(Order order) throws OrderStateTransitionException {
        synchronized (order) {
            OrderState orderState = order.getOrderState();

            if (orderState.equals(OrderState.FULFILLED)) {
//                LOGGER.info("Trying to get an instance for order [" + order.getId() + "]");

                try {
                    InstanceProvider instanceProvider = this.getInstanceProviderForOrder(order);

                    // TODO: prepare order to change its state from fulfilled to failed.

//                    LOGGER.info("Processing order [" + order.getId() + "]");
//                    OrderInstance orderInstance = instanceProvider.requestInstance(order);
//                    order.setOrderInstance(orderInstance);
//
//                    LOGGER.info("Updating order state after processing [" + order.getId() + "]");
//                    this.updateOrderStateAfterProcessing(order);

                } catch (Exception e) {
                    LOGGER.error("Error while trying to get an instance for order: " + order, e);

                    LOGGER.info("Transition [" + order.getId() + "] order state from fulfilled to failed");
                    OrderStateTransitioner.transition(order, OrderState.FAILED);
                }
            }
        }
    }

    /**
     * Get an instance provider from an order, if order is local, the
     * returned instance provider is the local, otherwise, it is the remote.
     *
     * @param order {@link Order}
     * @return corresponding instance provider.
     */
    private InstanceProvider getInstanceProviderForOrder(Order order) {
        InstanceProvider instanceProvider;

        if (order.isLocal(this.localMemberId)) {
        	LOGGER.info("The open order [" + order.getId() + "] is local");

            instanceProvider = this.localInstanceProvider;
        } else {
        	LOGGER.info("The open order [" + order.getId() + "] is remote for the " +
                    "member [" + order.getProvidingMember() + "]");

            instanceProvider = this.remoteInstanceProvider;
        }

        return instanceProvider;
    }
}
