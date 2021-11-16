package cloud.fogbow.ras.core.processors;

import cloud.fogbow.common.exceptions.FogbowException;
import cloud.fogbow.common.exceptions.InstanceNotFoundException;
import cloud.fogbow.common.exceptions.InternalServerErrorException;
import cloud.fogbow.ras.constants.Messages;
import cloud.fogbow.ras.core.OrderController;
import cloud.fogbow.ras.core.OrderStateTransitioner;
import cloud.fogbow.ras.core.SharedOrderHolders;
import cloud.fogbow.ras.core.cloudconnector.CloudConnectorFactory;
import cloud.fogbow.ras.core.cloudconnector.LocalCloudConnector;
import cloud.fogbow.ras.core.models.Operation;
import cloud.fogbow.ras.core.models.orders.Order;
import cloud.fogbow.ras.core.models.orders.OrderState;
import com.google.common.annotations.VisibleForTesting;
import org.apache.log4j.Logger;

public class CheckingDeletionProcessor extends StoppableOrderListProcessor implements Runnable {
    private static final Logger LOGGER = Logger.getLogger(CheckingDeletionProcessor.class);

    private OrderController orderController;
    private String localProviderId;

    public CheckingDeletionProcessor(OrderController orderController, String localProviderId, String sleepTimeStr) {
        super(Long.valueOf(sleepTimeStr), SharedOrderHolders.getInstance().getCheckingDeletionOrdersList());
        this.orderController = orderController;
        this.localProviderId = localProviderId;
    }
    
    /**
     * The CheckingDeletion processor monitors when the delete operation issued by the AssignedForDeletion processor
     * has finished. Essentially it keeps repeating getInstance() calls until an InstanceNotFound exception is raised.
     * When the plugin used by the AssignedForDeletion processor does deletion synchronously, then the exception will
     * be raised on the first time the order is processed. Otherwise, it might take a few interactions until the
     * instance is finally deleted, and getInstance() raises the exception.
     *
     * @param order {@link Order}
     */
    @VisibleForTesting
    void processCheckingDeletionOrder(Order order) throws InternalServerErrorException {
        synchronized (order) {
            // Check if the order is still in the CHECKING_DELETION state (for this particular state, this should
            // always happen, since once the order gets in this state, only this thread can operate on it. However,
            // the cost of safe programming is low).
            OrderState orderState = order.getOrderState();
            if (!orderState.equals(OrderState.CHECKING_DELETION)) {
                return;
            }
            // Only local orders need to be monitored. Remote orders are monitored by the remote provider.
            // State changes that happen at the remote provider are synchronized by the RemoteOrdersStateSynchronization
            // processor.
            if (order.isProviderRemote(this.localProviderId)) {
                // This should never happen, but the bug can be mitigated by moving the order to the remoteOrders list
                OrderStateTransitioner.transition(order, OrderState.PENDING);
                LOGGER.error(Messages.Log.UNEXPECTED_ERROR);
                return;
            }
            try {
                // Here we know that the CloudConnector is local, but the use of CloudConnectFactory facilitates testing.
                LocalCloudConnector localCloudConnector = (LocalCloudConnector)
                        CloudConnectorFactory.getInstance().getCloudConnector(this.localProviderId, order.getCloudName());
                // We don't audit requests we make
                localCloudConnector.switchOffAuditing();

                localCloudConnector.getInstance(order);
            } catch (InstanceNotFoundException e) {
                LOGGER.info(String.format(Messages.Log.INSTANCE_NOT_FOUND_S, order.getId()));
                // Remove any references that related dependencies of other orders with the order that has
                // just been deleted. Only the provider that has receiving the delete request through its
                // REST API needs to update order dependencies.
                if (order.isRequesterLocal(this.localProviderId)) {
                    this.orderController.updateOrderDependencies(order, Operation.DELETE);
                }
                this.orderController.closeOrder(order);
            } catch (FogbowException e) {
                LOGGER.info(String.format(Messages.Exception.GENERIC_EXCEPTION_S, e.getMessage()));
            }
        }
    }

    @Override
    protected void doProcessing(Order order) throws FogbowException {
        processCheckingDeletionOrder(order);
    }
}
