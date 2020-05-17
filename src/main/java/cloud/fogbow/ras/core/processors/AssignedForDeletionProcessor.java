package cloud.fogbow.ras.core.processors;

import cloud.fogbow.common.exceptions.FogbowException;
import cloud.fogbow.common.models.linkedlists.ChainedList;
import cloud.fogbow.ras.api.http.response.OrderInstance;
import cloud.fogbow.ras.constants.Messages;
import cloud.fogbow.ras.core.OrderStateTransitioner;
import cloud.fogbow.ras.core.SharedOrderHolders;
import cloud.fogbow.ras.core.cloudconnector.CloudConnector;
import cloud.fogbow.ras.core.cloudconnector.CloudConnectorFactory;
import cloud.fogbow.ras.core.cloudconnector.LocalCloudConnector;
import cloud.fogbow.ras.core.models.orders.Order;
import cloud.fogbow.ras.core.models.orders.OrderState;
import org.apache.log4j.Logger;

/**
 * Process orders in DELETING state. It keeps checking if the instance is still present
 * in the cloud by calling getInstance(). When getInstance() raises an InstanceNotFound
 * exception, then the processor infers that the instance has already been deleted.
 */
public class AssignedForDeletionProcessor implements Runnable {
    private static final Logger LOGGER = Logger.getLogger(AssignedForDeletionProcessor.class);

    private String localProviderId;
    private ChainedList<Order> deletingOrdersList;
    /**
     * Attribute that represents the thread sleep time when there are no orders to be processed.
     */
    private Long sleepTime;

    public AssignedForDeletionProcessor(String localProviderId, String sleepTimeStr) {
        this.localProviderId = localProviderId;
        SharedOrderHolders sharedOrderHolders = SharedOrderHolders.getInstance();
        this.deletingOrdersList = sharedOrderHolders.getAssignedForDeletionOrdersList();
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
                Order order = this.deletingOrdersList.getNext();

                if (order != null) {
                    processDeletingOrder(order);
                } else {
                    this.deletingOrdersList.resetPointer();
                    Thread.sleep(this.sleepTime);
                }
            } catch (InterruptedException e) {
                isActive = false;
                LOGGER.error(Messages.Error.THREAD_HAS_BEEN_INTERRUPTED, e);
            } catch (FogbowException e) {
                LOGGER.error(e.getMessage(), e);
            } catch (Throwable e) {
                LOGGER.error(Messages.Error.UNEXPECTED_ERROR, e);
            }
        }
    }

    /**
     * Checks whether an instance has completed its deletion. If performs a getInstance() call and an
     * InstanceNotFound exception is thrown, then it transitions the order state to CLOSED.
     *
     * @param order {@link Order}
     */
    protected void processDeletingOrder(Order order) throws FogbowException {
        OrderInstance instance = null;

        // The order object synchronization is needed to prevent a race
        // condition on order access.
        synchronized (order) {
            // Check if the order is still in the ASSIGNED_FOR_DELETION state (it could have been changed
            // by another thread)
            OrderState orderState = order.getOrderState();
            if (!orderState.equals(OrderState.ASSIGNED_FOR_DELETION)) {
                return;
            }
            // Only local orders need to be monitored. Remote orders are monitored by the remote provider
            // and change state when that provider notifies state changes.
            if (order.isProviderRemote(this.localProviderId)) {
                return;
            }
            try {
                // When the instanceId is null, there is nothing to be deleted in the cloud.
                if (order.getInstanceId() != null) {
                    // Here we know that the CloudConnector is local, but the use of CloudConnectFactory facilitates testing.
                    LocalCloudConnector localCloudConnector = (LocalCloudConnector)
                            CloudConnectorFactory.getInstance().getCloudConnector(this.localProviderId, order.getCloudName());
                    localCloudConnector.deleteInstance(order);
                }
                OrderStateTransitioner.transition(order, OrderState.CHECKING_DELETION);
            } catch (FogbowException e) {
                LOGGER.error(String.format(Messages.Error.ERROR_MESSAGE, order.getId()), e);
                throw e;
            }
        }
    }

    protected CloudConnector getCloudConnector(Order order) {
        return CloudConnectorFactory.getInstance().getCloudConnector(order.getProvider(), order.getCloudName());
    }
}
