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

public class SpawningProcessor implements Runnable {
    private static final Logger LOGGER = Logger.getLogger(SpawningProcessor.class);

    private ChainedList<Order> spawningOrderList;
    /**
     * Attribute that represents the thread sleep time when there are no orders to be processed.
     */
    private Long sleepTime;
    private String localProviderId;

    public SpawningProcessor(String providerId, String sleepTimeStr) {
        SharedOrderHolders sharedOrderHolders = SharedOrderHolders.getInstance();
        this.spawningOrderList = sharedOrderHolders.getSpawningOrdersList();
        this.sleepTime = Long.valueOf(sleepTimeStr);
        this.localProviderId = providerId;
    }

    /**
     * Iterates over the spawning orders list and tries to process one order at a time. When the order
     * is null, it indicates that the iteration ended. A new iteration is started after some time.
     */
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
                LOGGER.error(e.getMessage(), e);
            } catch (Throwable e) {
                LOGGER.error(Messages.Error.UNEXPECTED_ERROR, e);
            }
        }
    }

    protected void processSpawningOrder(Order order) throws FogbowException {
        // The order object synchronization is needed to prevent a race
        // condition on order access. For example: a user can delete an spawning
        // order while this method is trying to check the status of an instance
        // that has been requested in the cloud.
        synchronized (order) {
            OrderState orderState = order.getOrderState();
            // Check if the order is still in the SPAWNING state (it could have been changed by another thread)
            if (!orderState.equals(OrderState.SPAWNING)) {
                return;
            }
            // Only local orders need to be monitored. Remote orders are monitored by the remote provider
            // and change state when that provider notifies state changes.
            if (order.isProviderRemote(this.localProviderId)) {
                return;
            }
            // Here we know that the CloudConnector is local, but the use of CloudConnectFactory facilitates testing.
            LocalCloudConnector localCloudConnector = (LocalCloudConnector)
                    CloudConnectorFactory.getInstance().getCloudConnector(this.localProviderId, order.getCloudName());
            // We don't audit requests we make
            localCloudConnector.switchOffAuditing();

            try {
                OrderInstance instance = localCloudConnector.getInstance(order);
                if (instance.hasFailed()) {
                    // Signalling is only important for the business logic when it concerns the states
                    // CHECKING_DELETION and CLOSED. In this case, transitionOnSuccessfulSignalIfNeeded()
                    // must be called, when transitioning the state of an order. For the other states,
                    // the only effect is that the states of the instances that are returned in the
                    // OrderController getInstancesStatus() call may be stale. This is documented in the
                    // API. A client can always refresh the state of a particular instance by calling
                    // getInstance(). In these cases, the best effort transitionAndTryToSignalRequesterIfNeeded(),
                    // should be called.
                    OrderStateTransitioner.transitionAndTryToSignalRequesterIfNeeded(order, OrderState.FAILED_AFTER_SUCCESSFUL_REQUEST);
                } else if (instance.isReady()) {
                    OrderStateTransitioner.transitionAndTryToSignalRequesterIfNeeded(order, OrderState.FULFILLED);
                }
            } catch (UnavailableProviderException e1) {
                OrderStateTransitioner.transitionAndTryToSignalRequesterIfNeeded(order, OrderState.UNABLE_TO_CHECK_STATUS);
                throw e1;
            } catch (InstanceNotFoundException e2) {
                LOGGER.info(String.format(Messages.Info.INSTANCE_NOT_FOUND_S, order.getId()));
                OrderStateTransitioner.transitionAndTryToSignalRequesterIfNeeded(order, OrderState.FAILED_AFTER_SUCCESSFUL_REQUEST);
            }
        }
    }
}
