package org.fogbowcloud.manager.core.processors;

import org.apache.log4j.Logger;
import org.fogbowcloud.manager.core.OrderStateTransitioner;
import org.fogbowcloud.manager.core.SharedOrderHolders;
import org.fogbowcloud.manager.core.cloudconnector.CloudConnectorFactory;
import org.fogbowcloud.manager.core.cloudconnector.CloudConnector;
import org.fogbowcloud.manager.core.exceptions.UnexpectedException;
import org.fogbowcloud.manager.core.models.linkedlists.ChainedList;
import org.fogbowcloud.manager.core.models.orders.Order;
import org.fogbowcloud.manager.core.models.orders.OrderState;

public class OpenProcessor implements Runnable {

    private static final Logger LOGGER = Logger.getLogger(OpenProcessor.class);
    private String localMemberId;
    private ChainedList openOrdersList;
    /**
     * Attribute that represents the thread sleep time when there is no orders to be processed.
     */
    private Long sleepTime;

    public OpenProcessor(String localMemberId, String sleepTimeStr) {
        this.localMemberId = localMemberId;
        SharedOrderHolders sharedOrderHolders = SharedOrderHolders.getInstance();
        this.openOrdersList = sharedOrderHolders.getOpenOrdersList();
        this.sleepTime = Long.valueOf(sleepTimeStr);
    }

    /**
     * Iterates over the open orders list and try to process one open order per time. The order
     * being null indicates that the iteration is in the end of the list or the list is empty.
     */
    @Override
    public void run() {
        boolean isActive = true;
        while (isActive) {
            try {
                Order order = this.openOrdersList.getNext();
                if (order != null) {
                    processOpenOrder(order);
                } else {
                    this.openOrdersList.resetPointer();
                    Thread.sleep(this.sleepTime);
                }
            } catch (InterruptedException e) {
                isActive = false;
                LOGGER.error("Thread interrupted", e);
            } catch (UnexpectedException e) {
                LOGGER.error(e.getMessage(), e);
            } catch (Throwable e) {
                LOGGER.error("Unexpected error", e);
            }
        }
    }

    /**
     * Get an instance for an open order. If the method fails to get the instance, then the order is
     * set to failed, else, is set to spawning or pending if the order is localidentity or intercomponent,
     * respectively.
     */
    protected void processOpenOrder(Order order) throws UnexpectedException {
        // The order object synchronization is needed to prevent a race
        // condition on order access. For example: a user can delete an open
        // order while this method is trying to get an Instance for this order.
        synchronized (order) {
            OrderState orderState = order.getOrderState();

            // Check if the order is still in the Open state (it could have been changed by another thread)
            if (orderState.equals(OrderState.OPEN)) {
                LOGGER.debug("Trying to get an instance for order [" + order.getId() + "]");

                try {
                    CloudConnector cloudConnector =
                            CloudConnectorFactory.getInstance().getCloudConnector(order.getProvidingMember());
                    LOGGER.debug("Processing order [" + order.getId() + "]");
                    String orderInstanceId = cloudConnector.requestInstance(order);
                    order.setInstanceId(orderInstanceId);
                    LOGGER.debug("Updating order state after processing [" + order.getId() + "]");
                    updateOrderStateAfterProcessing(order);
                } catch (Exception e) {
                    LOGGER.error("Error while trying to get an instance for order: " + order, e);
                    OrderStateTransitioner.transition(order, OrderState.FAILED);
                }
            }
        }
    }

    /**
     * Update the order state and do the order state transition after the open order process.
     */
    private void updateOrderStateAfterProcessing(Order order) throws UnexpectedException {
        if (order.isProviderLocal(this.localMemberId)) {
            String orderInstanceId = order.getInstanceId();

            if (orderInstanceId != null) {
                LOGGER.debug("The open order [" + order.getId() + "] got an localidentity instance with id ["
                        + orderInstanceId + "], setting your state to spawning");
                LOGGER.debug("Transition [" + order.getId() + "] order state from open to spawning");
                OrderStateTransitioner.transition(order, OrderState.SPAWNING);
            } else {
                throw new UnexpectedException("Order instance id for order [" + order.getId() + "] is null");
            }
        } else {
            LOGGER.debug("Transition [" + order.getId() + "] order state from open to pending");
            OrderStateTransitioner.transition(order, OrderState.PENDING);
        }
    }

}
