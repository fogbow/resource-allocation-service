package org.fogbowcloud.manager.core.processors;

import org.apache.log4j.Logger;
import org.fogbowcloud.manager.core.manager.constants.ConfigurationConstants;
import org.fogbowcloud.manager.core.manager.constants.DefaultConfigurationConstants;
import org.fogbowcloud.manager.core.SharedOrderHolders;
import org.fogbowcloud.manager.core.exceptions.OrderStateTransitionException;
import org.fogbowcloud.manager.core.instanceprovider.InstanceProvider;
import org.fogbowcloud.manager.core.models.linkedlist.ChainedList;
import org.fogbowcloud.manager.core.models.orders.Order;

import java.util.Map;
import java.util.Properties;

public class ClosedComputeOrdersProcessor implements Runnable {

    private static final Logger LOGGER = Logger.getLogger(ClosedComputeOrdersProcessor.class);

    private ChainedList closedOrders;

    private String localMemberId;
    private Long sleepTime;

    private InstanceProvider localProvider;
    private InstanceProvider remoteProvider;

    private SharedOrderHolders sharedOrderHolders = SharedOrderHolders.getInstance();

    public ClosedComputeOrdersProcessor(Properties properties,InstanceProvider localProvider,
                                        InstanceProvider remoteProvider) {
        SharedOrderHolders sharedOrdersHolder = SharedOrderHolders.getInstance();
        this.closedOrders = sharedOrdersHolder.getClosedOrdersList();

        String closedOrdersSleepTime = properties.getProperty(ConfigurationConstants.CLOSED_ORDERS_SLEEP_TIME_KEY,
                DefaultConfigurationConstants.CLOSED_ORDERS_SLEEP_TIME);
        this.sleepTime = Long.valueOf(closedOrdersSleepTime);

        this.localMemberId = properties.getProperty(ConfigurationConstants.XMPP_ID_KEY);

        this.localProvider = localProvider;
        this.remoteProvider = remoteProvider;
    }

    @Override
    public void run() {
        boolean isActive = true;
        while (isActive) {
            try {
                Order order = this.closedOrders.getNext();
                if (order != null) {
                    try {
                        processClosedOrder(order);

                        closedOrders.removeItem(order);
                        deactivateOrder(order);
                    } catch (OrderStateTransitionException e) {
                        LOGGER.error("Error while trying to changing the state of order " + order, e);
                    }
                } else {
                    this.closedOrders.resetPointer();
                    LOGGER.info(
                            "There is no closed order to be processed, sleeping for " + this.sleepTime + " milliseconds");
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

    private void processClosedOrder(Order order) throws Exception {
        synchronized (order) {
            if (order.getOrderInstance() != null &&
                    order.getOrderInstance().getId() != null) {
                InstanceProvider provider = getInstanceProviderForOrder(order);
                provider.deleteInstance(order.getLocalToken(), order.getOrderInstance());
            } else {
                LOGGER.info(String.format("Order %s has no instance associated", order.getId()));
            }
        }
    }

    private void deactivateOrder(Order order) {
        Map<String, Order> activeOrdersMap = sharedOrderHolders.getActiveOrdersMap();
        if (activeOrdersMap.containsKey(order.getId())) {
            activeOrdersMap.remove(order.getId());
        } else {
            String message = "Tried to remove order %s from the active orders but there were no order with this id";
            LOGGER.error(String.format(message, order.getId()));
        }
    }

    private InstanceProvider getInstanceProviderForOrder(Order order) {
        InstanceProvider instanceProvider = null;
        if (order.isLocal(this.localMemberId)) {
            instanceProvider = this.localProvider;
        } else {
            instanceProvider = this.remoteProvider;
        }

        return instanceProvider;
    }
}
