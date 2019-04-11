package cloud.fogbow.ras.core.processors;

import cloud.fogbow.common.exceptions.UnexpectedException;
import cloud.fogbow.common.models.linkedlists.ChainedList;
import cloud.fogbow.ras.constants.Messages;
import cloud.fogbow.ras.core.OrderStateTransitioner;
import cloud.fogbow.ras.core.SharedOrderHolders;
import cloud.fogbow.ras.core.cloudconnector.CloudConnectorFactory;
import cloud.fogbow.ras.core.cloudconnector.LocalCloudConnector;
import cloud.fogbow.ras.core.models.orders.Order;
import cloud.fogbow.ras.core.models.orders.OrderState;
import cloud.fogbow.ras.api.http.response.Instance;
import cloud.fogbow.ras.api.http.response.InstanceState;
import org.apache.log4j.Logger;

/**
 * Process orders in fulfilled state. It monitors the resourced that have been successfully
 * initiated, to check for failures that may affect them.
 */
public class FulfilledProcessor implements Runnable {
    private static final Logger LOGGER = Logger.getLogger(FulfilledProcessor.class);

    private String localMemberId;
    private ChainedList<Order> fulfilledOrdersList;

    /**
     * Attribute that represents the thread sleep time when there is no orders to be processed.
     */
    private Long sleepTime;

    public FulfilledProcessor(String localMemberId, String sleepTimeStr) {
        CloudConnectorFactory cloudConnectorFactory = CloudConnectorFactory.getInstance();
        this.localMemberId = localMemberId;
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
            try {
                LocalCloudConnector localCloudConnector = (LocalCloudConnector) CloudConnectorFactory.getInstance().
                        getCloudConnector(this.localMemberId, order.getCloudName());

                // we won't audit requests we make
                localCloudConnector.switchOffAuditing();

                instance = localCloudConnector.getInstance(order);
            } catch (Exception e) {
                LOGGER.error(Messages.Error.ERROR_WHILE_GETTING_INSTANCE_FROM_CLOUD, e);
                OrderStateTransitioner.transition(order, OrderState.FAILED_AFTER_SUCCESSUL_REQUEST);
                return;
            }
            instanceState = instance.getState();
            if (instanceState.equals(InstanceState.FAILED)) {
                LOGGER.info(String.format(Messages.Info.INSTANCE_HAS_FAILED, order.getId()));
                OrderStateTransitioner.transition(order, OrderState.FAILED_AFTER_SUCCESSUL_REQUEST);
                return;
            }
        }
    }
}
