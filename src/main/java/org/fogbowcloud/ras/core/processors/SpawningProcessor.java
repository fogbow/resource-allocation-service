package org.fogbowcloud.ras.core.processors;

import org.apache.log4j.Logger;
import org.fogbowcloud.ras.core.OrderStateTransitioner;
import org.fogbowcloud.ras.core.SharedOrderHolders;
import org.fogbowcloud.ras.core.cloudconnector.CloudConnector;
import org.fogbowcloud.ras.core.cloudconnector.CloudConnectorFactory;
import org.fogbowcloud.ras.core.constants.Messages;
import org.fogbowcloud.ras.core.exceptions.UnexpectedException;
import org.fogbowcloud.ras.core.models.instances.Instance;
import org.fogbowcloud.ras.core.models.instances.InstanceState;
import org.fogbowcloud.ras.core.models.linkedlists.ChainedList;
import org.fogbowcloud.ras.core.models.orders.Order;
import org.fogbowcloud.ras.core.models.orders.OrderState;

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
                    Thread.sleep(this.sleepTime);
                }
            } catch (InterruptedException e) {
                isActive = false;
                LOGGER.error(Messages.Error.THREAD_HAS_BEEN_INTERRUPTED, e);
            } catch (UnexpectedException e) {
                handleError(order, e.getMessage(), e);
            } catch (Throwable e) {
                handleError(order, Messages.Error.UNEXPECTED_ERROR, e);
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
                processInstance(order);
            }
        }
    }

    private void processInstance(Order order) throws Exception {
        Instance instance = this.localCloudConnector.getInstance(order);

        InstanceState instanceState = instance.getState();

        if (instanceState.equals(InstanceState.FAILED)) {
            OrderStateTransitioner.transition(order, OrderState.FAILED_AFTER_SUCCESSUL_REQUEST);
        } else if (instanceState.equals(InstanceState.READY)) {
            OrderStateTransitioner.transition(order, OrderState.FULFILLED);
        }
    }

    private void handleError(Order order, String message, Throwable e) {
        LOGGER.error(message, e);
        if (order != null) {
            if (!order.getOrderState().equals(OrderState.FAILED_AFTER_SUCCESSUL_REQUEST)) {
                try {
                    OrderStateTransitioner.transition(order, OrderState.FAILED_AFTER_SUCCESSUL_REQUEST);
                } catch (UnexpectedException e2) {
                    LOGGER.error(message, e2);
                }
            }
        }

    }
}
