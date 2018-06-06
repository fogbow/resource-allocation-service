package org.fogbowcloud.manager.core.processors;

import org.apache.log4j.Logger;
import org.fogbowcloud.manager.api.intercomponent.exceptions.RemoteRequestException;
import org.fogbowcloud.manager.core.OrderController;
import org.fogbowcloud.manager.core.SharedOrderHolders;
import org.fogbowcloud.manager.core.cloudconnector.CloudConnectorFactory;
import org.fogbowcloud.manager.core.exceptions.OrderManagementException;
import org.fogbowcloud.manager.core.exceptions.PropertyNotSpecifiedException;
import org.fogbowcloud.manager.core.exceptions.RequestException;
import org.fogbowcloud.manager.core.cloudconnector.CloudConnector;
import org.fogbowcloud.manager.core.plugins.exceptions.TokenCreationException;
import org.fogbowcloud.manager.core.plugins.exceptions.UnauthorizedException;
import org.fogbowcloud.manager.core.models.linkedlist.ChainedList;
import org.fogbowcloud.manager.core.models.orders.Order;

public class ClosedProcessor implements Runnable {

    private static final Logger LOGGER = Logger.getLogger(ClosedProcessor.class);

    private ChainedList closedOrders;

    private Long sleepTime;

    private OrderController orderController;

    public ClosedProcessor(OrderController orderController, String sleepTimeStr) {
        SharedOrderHolders sharedOrdersHolder = SharedOrderHolders.getInstance();
        this.closedOrders = sharedOrdersHolder.getClosedOrdersList();

        this.sleepTime = Long.valueOf(sleepTimeStr);

        this.orderController = orderController;
    }

    @Override
    public void run() {
        boolean isActive = true;
        while (isActive) {
            try {
                Order order = this.closedOrders.getNext();
                if (order != null) {
                    processClosedOrder(order);
                } else {
                    this.closedOrders.resetPointer();
                    LOGGER.debug(
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

    protected void processClosedOrder(Order order)
    		throws PropertyNotSpecifiedException, TokenCreationException, RequestException, UnauthorizedException, RemoteRequestException, OrderManagementException {
        synchronized (order) {
            CloudConnector provider = CloudConnectorFactory.getInstance().getCloudConnector(order.getProvidingMember());
            provider.deleteInstance(order);

            this.closedOrders.removeItem(order);
            this.orderController.removeOrderFromActiveOrdersMap(order);
        }
    }
}
