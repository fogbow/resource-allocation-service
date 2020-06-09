package cloud.fogbow.ras.core.processors;

import cloud.fogbow.common.exceptions.FogbowException;
import cloud.fogbow.common.exceptions.InstanceNotFoundException;
import cloud.fogbow.common.models.linkedlists.ChainedList;
import cloud.fogbow.ras.constants.Messages;
import cloud.fogbow.ras.core.OrderStateTransitioner;
import cloud.fogbow.ras.core.SharedOrderHolders;
import cloud.fogbow.ras.core.cloudconnector.CloudConnectorFactory;
import cloud.fogbow.ras.core.cloudconnector.LocalCloudConnector;
import cloud.fogbow.ras.core.models.orders.Order;
import cloud.fogbow.ras.core.models.orders.OrderState;
import com.google.common.annotations.VisibleForTesting;
import org.apache.log4j.Logger;

public class AssignedForDeletionProcessor implements Runnable {
    private static final Logger LOGGER = Logger.getLogger(AssignedForDeletionProcessor.class);

    private String localProviderId;
    private ChainedList<Order> assignedForDeletionOrdersList;
    /**
     * Attribute that represents the thread sleep time when there are no orders to be processed.
     */
    private Long sleepTime;

    public AssignedForDeletionProcessor(String localProviderId, String sleepTimeStr) {
        this.localProviderId = localProviderId;
        SharedOrderHolders sharedOrderHolders = SharedOrderHolders.getInstance();
        this.assignedForDeletionOrdersList = sharedOrderHolders.getAssignedForDeletionOrdersList();
        this.sleepTime = Long.valueOf(sleepTimeStr);
    }

    /**
     * Iterates over the assignedForDeletion orders list and tries to process one order at a time. When the order
     * is null, it indicates that the iteration ended. A new iteration is started after some time.
     */
    @Override
    public void run() {
        boolean isActive = true;
        while (isActive) {
            try {
                assignForDeletion();
            } catch (InterruptedException e) {
                isActive = false;
            }
        }
    }

    @VisibleForTesting
    void assignForDeletion() throws InterruptedException {
        try {
            Order order = this.assignedForDeletionOrdersList.getNext();

            if (order != null) {
                processAssignedForDeletionOrder(order);
            } else {
                this.assignedForDeletionOrdersList.resetPointer();
                Thread.sleep(this.sleepTime);
            }
        } catch (InterruptedException e) {
            LOGGER.error(Messages.Error.THREAD_HAS_BEEN_INTERRUPTED, e);
            throw e;
        } catch (FogbowException e) {
            LOGGER.error(e.getMessage(), e);
        } catch (Throwable e) {
            LOGGER.error(Messages.Error.UNEXPECTED_ERROR, e);
        }
    }

    /**
     * Starts the deletion procedure in the cloud. Some plugins do this synchronously, others do asynchronously.
     * Thus, we always assume an asynchronous semantic. This threads issues the delete in the cloud, and transitions
     * the order to the CHECKING_DELETION state. The CheckingDeletion processor monitors when the operation has
     * finished. Essentially it keeps repeating getInstance() calls until an InstanceNotFound exception is raised.
     *
     * @param order {@link Order}
     */
    @VisibleForTesting
    void processAssignedForDeletionOrder(Order order) throws FogbowException {
        // The order object synchronization is needed to prevent a race condition on order access.
        synchronized (order) {
            // Check if the order is still in the ASSIGNED_FOR_DELETION state (for this particular state, this should
            // always happen, since once the order gets to this state, only this thread can operate on it. However,
            // the cost of safe programming is low).
            OrderState orderState = order.getOrderState();
            if (!orderState.equals(OrderState.ASSIGNED_FOR_DELETION)) {
                return;
            }
            // Only local orders need to be monitored. Remote orders are monitored by the remote provider.
            // State changes that happen at the remote provider are synchronized by the RemoteOrdersStateSynchronization
            // processor.
            if (order.isProviderRemote(this.localProviderId)) {
                // This should never happen, but the bug can be mitigated by moving the order to the remoteOrders list
                OrderStateTransitioner.transition(order, OrderState.PENDING);
                LOGGER.error(Messages.Error.UNEXPECTED_ERROR);
                return;
            }
            try {
                // Here we know that the CloudConnector is local, but the use of CloudConnectFactory facilitates testing.
                LocalCloudConnector localCloudConnector = (LocalCloudConnector)
                            CloudConnectorFactory.getInstance().getCloudConnector(this.localProviderId, order.getCloudName());
                localCloudConnector.deleteInstance(order);
                OrderStateTransitioner.transition(order, OrderState.CHECKING_DELETION);
            } catch (InstanceNotFoundException e) {
                // If the provider crashes after calling deleteInstance() and before setting the order's state to
                // CHECKING_DELETION, then the deleteInstance() method will be called again, after recovery.
                // This is not an issue, because calling deleteInstance() multiple times has no undesired collateral
                // effect. The order needs simply to be advanced to the CHECKING_DELETION state, to later be closed
                // by the CheckingDeletion processor.
                OrderStateTransitioner.transition(order, OrderState.CHECKING_DELETION);
            }
        }
    }
}
