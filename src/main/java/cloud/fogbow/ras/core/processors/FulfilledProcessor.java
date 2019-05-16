package cloud.fogbow.ras.core.processors;

import cloud.fogbow.common.exceptions.FogbowException;
import cloud.fogbow.common.exceptions.InstanceNotFoundException;
import cloud.fogbow.common.exceptions.UnavailableProviderException;
import cloud.fogbow.common.exceptions.UnexpectedException;
import cloud.fogbow.common.models.linkedlists.ChainedList;
import cloud.fogbow.ras.api.http.response.OrderInstance;
import cloud.fogbow.ras.constants.Messages;
import cloud.fogbow.ras.core.OrderStateTransitioner;
import cloud.fogbow.ras.core.SharedOrderHolders;
import cloud.fogbow.ras.core.cloudconnector.CloudConnectorFactory;
import cloud.fogbow.ras.core.cloudconnector.LocalCloudConnector;
import cloud.fogbow.ras.core.models.orders.Order;
import cloud.fogbow.ras.core.models.orders.OrderState;
import org.apache.log4j.Logger;

/**
 * Process orders in FULFILLED state. It monitors the resourced that have been successfully
 * initiated, to check for failures that may affect them.
 */
public class FulfilledProcessor implements Runnable {
    private static final Logger LOGGER = Logger.getLogger(FulfilledProcessor.class);

    private String localMemberId;
    private ChainedList<Order> fulfilledOrdersList;
    /**
     * Attribute that represents the thread sleep time when there are no orders to be processed.
     */
    private Long sleepTime;

    public FulfilledProcessor(String localMemberId, String sleepTimeStr) {
        this.localMemberId = localMemberId;
        SharedOrderHolders sharedOrderHolders = SharedOrderHolders.getInstance();
        this.fulfilledOrdersList = sharedOrderHolders.getFulfilledOrdersList();
        this.sleepTime = Long.valueOf(sleepTimeStr);
    }

    /**
     * Iterates over the fulfilled orders list and tries to process one order at a time. When the order
     * is null, it indicates that the iteration ended. A new iteration is started after some time.
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
                LOGGER.error(Messages.Error.THREAD_HAS_BEEN_INTERRUPTED, e);
            } catch (UnexpectedException e) {
                LOGGER.error(e.getMessage(), e);
            } catch (Throwable e) {
                LOGGER.error(Messages.Error.UNEXPECTED_ERROR, e);
            }
        }
    }

    /**
     * Gets an instance for a fulfilled order. If that instance is not reachable the order state is
     * set to UNABLE_TO_CHECK_STATUS. Otherwise, if the instance has failed, then the order state is
     * set to FAILED_AFTER_SUCCESSFUL_REQUEST.
     *
     * @param order {@link Order}
     */
    protected void processFulfilledOrder(Order order) throws FogbowException {
        OrderInstance instance = null;

        // The order object synchronization is needed to prevent a race
        // condition on order access. For example: a user can delete a fulfilled
        // order while this method is trying to check the status of an instance
        // that was allocated to an order.
        synchronized (order) {
            // Check if the order is still in the FULFILLED state (it could have been changed by another thread)
            OrderState orderState = order.getOrderState();
            if (!orderState.equals(OrderState.FULFILLED)) {
                return;
            }
            // Only local orders need to be monitored. Remote orders are monitored by the remote provider
            // and change state when that provider notifies state changes.
            if (order.isProviderRemote(this.localMemberId)) {
                return;
            }
            try {
                // Here we know that the CloudConnector is local, but the use of CloudConnectFactory facilitates testing.
                LocalCloudConnector localCloudConnector = (LocalCloudConnector)
                        CloudConnectorFactory.getInstance().getCloudConnector(this.localMemberId, order.getCloudName());
                // we won't audit requests we make
                localCloudConnector.switchOffAuditing();

                instance = localCloudConnector.getInstance(order);
                if (instance.hasFailed()) {
                    LOGGER.info(String.format(Messages.Info.INSTANCE_HAS_FAILED, order.getId()));
                    OrderStateTransitioner.transition(order, OrderState.FAILED_AFTER_SUCCESSFUL_REQUEST);
                }
            } catch (UnavailableProviderException e1) {
                OrderStateTransitioner.transition(order, OrderState.UNABLE_TO_CHECK_STATUS);
                throw e1;
            } catch (InstanceNotFoundException e2) {
                LOGGER.info(String.format(Messages.Info.INSTANCE_NOT_FOUND_S, order.getId()));
                OrderStateTransitioner.transition(order, OrderState.FAILED_AFTER_SUCCESSFUL_REQUEST);
            }
        }
    }
}
