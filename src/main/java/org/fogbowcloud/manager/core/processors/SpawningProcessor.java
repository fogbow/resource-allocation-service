package org.fogbowcloud.manager.core.processors;

import org.apache.log4j.Logger;
import org.fogbowcloud.manager.core.exceptions.*;
import org.fogbowcloud.manager.core.OrderStateTransitioner;
import org.fogbowcloud.manager.core.SharedOrderHolders;
import org.fogbowcloud.manager.core.cloudconnector.CloudConnectorFactory;
import org.fogbowcloud.manager.core.cloudconnector.CloudConnector;
import org.fogbowcloud.manager.core.models.instances.InstanceState;
import org.fogbowcloud.manager.core.models.ResourceType;
import org.fogbowcloud.manager.core.models.linkedlists.ChainedList;
import org.fogbowcloud.manager.core.models.orders.Order;
import org.fogbowcloud.manager.core.models.orders.OrderState;
import org.fogbowcloud.manager.core.models.instances.Instance;

public class SpawningProcessor implements Runnable {

    private static final Logger LOGGER = Logger.getLogger(SpawningProcessor.class);

    private ChainedList spawningOrderList;

    private Long sleepTime;

    private CloudConnector localCloudConnector;

    public SpawningProcessor(String memberId, String sleepTimeStr) {
        this.localCloudConnector = CloudConnectorFactory.getInstance().getCloudConnector(memberId);
        SharedOrderHolders sharedOrderHolders = SharedOrderHolders.getInstance();
        this.spawningOrderList = sharedOrderHolders.getSpawningOrdersList();
        this.sleepTime = Long.valueOf(sleepTimeStr);
    }

    @Override
    public void run() {
        boolean isActive = true;
        Order order = null;
        while (isActive) {
            try {
                order = this.spawningOrderList.getNext();
                if (order != null) {
                    processSpawningOrder(order);
                } else {
                    this.spawningOrderList.resetPointer();
                    LOGGER.debug("There is no spawning order to be processed, sleeping for "
                            + this.sleepTime + " milliseconds");
                    Thread.sleep(this.sleepTime);
                }
            } catch (InterruptedException e) {
                isActive = false;
                LOGGER.error("Thread interrupted", e);
            } catch (UnexpectedException e) {
                handleError(order, e.getMessage(), e);
            } catch (Throwable e) {
                handleError(order, "Unexpected error", e);
            }
        }
    }

    protected void processSpawningOrder(Order order) throws Exception {
        // The order object synchronization is needed to prevent a race
        // condition on order access. For example: a user can delete an open
        // order while this method is trying to check the status of an instance
        // that has been requested in the cloud.
        synchronized (order) {
            OrderState orderState = order.getOrderState();

            // Check if the order is still in the Spawning state (it could have been changed by another thread)
            if (orderState.equals(OrderState.SPAWNING)) {
                LOGGER.debug("Trying to process an instance for order [" + order.getId() + "]");
                processInstance(order);
            } else {
                LOGGER.debug("This order state is not spawning for order [" + order.getId() + "]");
            }
        }
    }

    /**
     * This method does not synchronize the order object because it is private and can only be
     * called by the processSpawningOrder method.
     * @throws FogbowManagerException
     */
    private void processInstance(Order order) throws Exception {
        Instance instance = this.localCloudConnector.getInstance(order);
        ResourceType resourceType = order.getType();

        InstanceState instanceState = instance.getState();

        if (instanceState.equals(InstanceState.FAILED)) {
            LOGGER.debug("The compute instance state is failed for order [" + order.getId() + "]");
            OrderStateTransitioner.transition(order, OrderState.FAILED);
        } else if (instanceState.equals(InstanceState.READY)) {
            LOGGER.debug("Processing active compute instance for order [" + order.getId() + "]");
            OrderStateTransitioner.transition(order, OrderState.FULFILLED);
        }
    }

    private void handleError(Order order, String message, Throwable e) {
        LOGGER.error(message, e);
        if (order != null) {
            if (!order.getOrderState().equals(OrderState.FAILED)) {
                try {
                    OrderStateTransitioner.transition(order, OrderState.FAILED);
                } catch (UnexpectedException e2) {
                    LOGGER.error(message, e2);
                }
            }
        }

    }
}
