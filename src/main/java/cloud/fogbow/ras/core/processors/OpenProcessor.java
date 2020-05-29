package cloud.fogbow.ras.core.processors;

import cloud.fogbow.common.exceptions.FogbowException;
import cloud.fogbow.common.exceptions.UnexpectedException;
import cloud.fogbow.common.models.linkedlists.ChainedList;
import cloud.fogbow.ras.constants.Messages;
import cloud.fogbow.ras.core.OrderStateTransitioner;
import cloud.fogbow.ras.core.SharedOrderHolders;
import cloud.fogbow.ras.core.cloudconnector.CloudConnector;
import cloud.fogbow.ras.core.cloudconnector.CloudConnectorFactory;
import cloud.fogbow.ras.core.models.orders.Order;
import cloud.fogbow.ras.core.models.orders.OrderState;
import org.apache.log4j.Logger;

public class OpenProcessor implements Runnable {
    private static final Logger LOGGER = Logger.getLogger(OpenProcessor.class);

    private String localProviderId;
    private ChainedList<Order> openOrdersList;
    /**
     * Attribute that represents the thread sleep time when there are no orders to be processed.
     */
    private Long sleepTime;

    public OpenProcessor(String localProviderId, String sleepTimeStr) {
        this.localProviderId = localProviderId;
        SharedOrderHolders sharedOrderHolders = SharedOrderHolders.getInstance();
        this.openOrdersList = sharedOrderHolders.getOpenOrdersList();
        this.sleepTime = Long.valueOf(sleepTimeStr);
    }

    /**
     * Iterates over the open orders list and tries to process one order at a time. When the order
     * is null, it indicates that the iteration ended. A new iteration is started after some time.
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
                LOGGER.error(Messages.Error.THREAD_HAS_BEEN_INTERRUPTED, e);
            } catch (UnexpectedException e) {
                LOGGER.error(e.getMessage(), e);
            } catch (Throwable e) {
                LOGGER.error(Messages.Error.UNEXPECTED_ERROR, e);
            }
        }
    }

    /**
     * Get an instance for an order in the OPEN state. If the method fails to get the instance, then the order is
     * set to FAILED_ON_REQUEST state, else, it is set to the SPAWNING state if the order is local, or the PENDING
     * state if the order is remote. The SELECTED state exists simply to implement an "at-most-once" semantics
     * for the requestInstance() call. This transition removes the order from the OPEN list. Thus, once the order
     * is selected by the OpenProcessor, if the provider fails before advancing the state to either FAILED_ON_REQUEST,
     * PENDING or SPAWNING, when the provider recovers, this order will remain in the SELECTED state and will not
     * be retried. This is needed because the provider can't know whether the failure occurred before or after the
     * requestInstance() call. All other order processors call either getInstance() or deleteInstance(), and do not
     * need to bother with the effects of failures. This is because both getInstace() and deleteInstance() cause no
     * undesired collateral effects if invoked more than once.
     */
    protected void processOpenOrder(Order order) throws FogbowException {
        // The order object synchronization is needed to prevent a race
        // condition on order access. For example: a user can delete an open
        // order while this method is trying to get an Instance for this order.
        synchronized (order) {
            // Check if the order is still in the OPEN state (it could have been changed by another thread)
            OrderState orderState = order.getOrderState();
            if (!orderState.equals(OrderState.OPEN)) {
                return;
            }
            try {
                OrderStateTransitioner.transition(order, OrderState.SELECTED);
                CloudConnector cloudConnector = CloudConnectorFactory.getInstance().
                        getCloudConnector(order.getProvider(), order.getCloudName());
                String instanceId = cloudConnector.requestInstance(order);
                order.setInstanceId(instanceId);
                if (order.isProviderLocal(this.localProviderId)) {
                    if (instanceId != null) {
                        OrderStateTransitioner.transition(order, OrderState.SPAWNING);
                    } else {
                        throw new UnexpectedException(String.format(Messages.Exception.REQUEST_INSTANCE_NULL, order.getId()));
                    }
                } else {
                    OrderStateTransitioner.transition(order, OrderState.PENDING);
                }
            } catch (Exception e) {
                order.setInstanceId(null);
                if (order.isProviderLocal(this.localProviderId)) {
                    OrderStateTransitioner.transition(order, OrderState.FAILED_ON_REQUEST);
                } else {
                    // Moves order to remoteProviderOrders list
                    OrderStateTransitioner.transition(order, OrderState.PENDING);
                    // Update order's state
                    order.setOrderState(OrderState.FAILED_ON_REQUEST);
                }
                throw e;
            }
        }
    }
}
