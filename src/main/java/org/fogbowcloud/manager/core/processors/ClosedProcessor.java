package org.fogbowcloud.manager.core.processors;

import java.util.Properties;
import javassist.NotFoundException;
import org.apache.log4j.Logger;
import org.fogbowcloud.manager.core.OrderController;
import org.fogbowcloud.manager.core.SharedOrderHolders;
import org.fogbowcloud.manager.core.exceptions.OrderStateTransitionException;
import org.fogbowcloud.manager.core.instanceprovider.InstanceProvider;
import org.fogbowcloud.manager.core.manager.constants.ConfigurationConstants;
import org.fogbowcloud.manager.core.manager.constants.DefaultConfigurationConstants;
import org.fogbowcloud.manager.core.models.linkedlist.ChainedList;
import org.fogbowcloud.manager.core.models.orders.Order;

public class ClosedProcessor implements Runnable {

    private static final Logger LOGGER = Logger.getLogger(ClosedProcessor.class);

    private ChainedList closedOrders;

    private String localMemberId;
    private Long sleepTime;

    private InstanceProvider localInstanceProvider;
    private InstanceProvider remoteInstanceProvider;
    private OrderController orderController;

    private SharedOrderHolders sharedOrderHolders = SharedOrderHolders.getInstance();

    public ClosedProcessor(
            InstanceProvider localInstanceProvider,
            InstanceProvider remoteInstanceProvider,
            OrderController orderController,
            Properties properties) {
        SharedOrderHolders sharedOrdersHolder = SharedOrderHolders.getInstance();
        this.closedOrders = sharedOrdersHolder.getClosedOrdersList();

        String sleepTimeStr =
                properties.getProperty(
                        ConfigurationConstants.CLOSED_ORDERS_SLEEP_TIME_KEY,
                        DefaultConfigurationConstants.CLOSED_ORDERS_SLEEP_TIME);
        this.sleepTime = Long.valueOf(sleepTimeStr);

        this.localMemberId = properties.getProperty(ConfigurationConstants.XMPP_ID_KEY);

        this.localInstanceProvider = localInstanceProvider;
        this.remoteInstanceProvider = remoteInstanceProvider;
        this.orderController = orderController;
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
                    } catch (OrderStateTransitionException e) {
                        LOGGER.error(
                                "Error while trying to changing the state of order " + order, e);
                    }
                } else {
                    this.closedOrders.resetPointer();
                    LOGGER.info(
                            "There is no closed order to be processed, sleeping for "
                                    + this.sleepTime
                                    + " milliseconds");
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
            if (order.getOrderInstance() != null) {
                InstanceProvider provider = getInstanceProviderForOrder(order);
                try {
                    provider.deleteInstance(order.getOrderInstance());
                    // TODO create InstanceNotFoundException
                } catch (NotFoundException e) {
                    LOGGER.warn("", e);
                } catch (Exception e) {
                    LOGGER.error("", e);
                    return;
                }

                this.closedOrders.removeItem(order);
                orderController.removeOrderFromActiveOrdersMap(order);
            } else {
                LOGGER.info(String.format("Order %s has no instance associated", order.getId()));
            }
        }
    }

    private InstanceProvider getInstanceProviderForOrder(Order order) {
        InstanceProvider instanceProvider = null;
        if (order.isLocal(this.localMemberId)) {
            instanceProvider = this.localInstanceProvider;
        } else {
            instanceProvider = this.remoteInstanceProvider;
        }

        return instanceProvider;
    }
}
