package org.fogbowcloud.ras.core.processors;

import org.apache.log4j.Logger;
import org.fogbowcloud.ras.core.OrderStateTransitioner;
import org.fogbowcloud.ras.core.SharedOrderHolders;
import org.fogbowcloud.ras.core.cloudconnector.CloudConnector;
import org.fogbowcloud.ras.core.cloudconnector.CloudConnectorFactory;
import org.fogbowcloud.ras.core.exceptions.UnexpectedException;
import org.fogbowcloud.ras.core.models.instances.Instance;
import org.fogbowcloud.ras.core.models.instances.InstanceState;
import org.fogbowcloud.ras.core.models.linkedlists.ChainedList;
import org.fogbowcloud.ras.core.models.orders.Order;
import org.fogbowcloud.ras.core.models.orders.OrderState;

/**
 * Process orders in fulfilled state. It monitors the resourced that have been successfully
 * initiated, to check for failures that may affect them.
 */
public class FulfilledProcessor implements Runnable {
    private static final Logger LOGGER = Logger.getLogger(FulfilledProcessor.class);

    private String localMemberId;
    private CloudConnector localCloudConnector;
    private ChainedList fulfilledOrdersList;

    /**
     * Attribute that represents the thread sleep time when there is no orders to be processed.
     */
    private Long sleepTime;

    public FulfilledProcessor(String localMemberId, String sleepTimeStr) {
        CloudConnectorFactory cloudConnectorFactory = CloudConnectorFactory.getInstance();
        this.localMemberId = localMemberId;
        this.localCloudConnector = cloudConnectorFactory.getCloudConnector(localMemberId);
        SharedOrderHolders sharedOrderHolders = SharedOrderHolders.getInstance();
        this.fulfilledOrdersList = sharedOrderHolders.getFulfilledOrdersList();
        this.sleepTime = Long.valueOf(sleepTimeStr);
    }

    /**
     * Iterates over the fulfilled orders list and try to process one fulfilled order per time. If
     * the order is null it indicates the iteration is in the end of the list or the list is empty.
     */
    @Override
    public void run() {
        boolean isActive = true;

        while (isActive) {
            try {
                Order order = this.fulfilledOrdersList.getNext();

                if (order != null) {
                    processFulfilledOrder(order);
                } else {
                    this.fulfilledOrdersList.resetPointer();
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
     * Gets an instance for a fulfilled order. If that instance is not reachable the order state is
     * set to failed.
     *
     * @param order {@link Order}
     */
    protected void processFulfilledOrder(Order order) throws UnexpectedException {

        Instance instance = null;
        InstanceState instanceState = null;

        // The order object synchronization is needed to prevent a race
        // condition on order access. For example: a user can delete a fulfilled
        // order while this method is trying to check the status of an instance
        // that was allocated to an order.

        synchronized (order) {
            OrderState orderState = order.getOrderState();
            // Only orders that have been served by the local cloud are checked; remote ones are checked by
            // the Fogbow RAS running in the other member, which reports back any changes in the status.
            if (!order.isProviderLocal(this.localMemberId)) {
                return;
            }
            // Check if the order is still in the Fulfilled state (it could have been changed by another thread)
            if (!orderState.equals(OrderState.FULFILLED)) {
                return;
            }
            LOGGER.info("Trying to get an instance for order [" + order.getId() + "]");
            try {
                instance = this.localCloudConnector.getInstance(order);
            } catch (Exception e) {
                LOGGER.error("Error while getting instance from the cloud.", e);
                OrderStateTransitioner.transition(order, OrderState.FAILED);
                return;
            }
            instanceState = instance.getState();
            if (instanceState.equals(InstanceState.FAILED)) {
                LOGGER.info("Instance state is failed for order [" + order.getId() + "]");
                OrderStateTransitioner.transition(order, OrderState.FAILED);
                return;
            }
        }
    }
}
