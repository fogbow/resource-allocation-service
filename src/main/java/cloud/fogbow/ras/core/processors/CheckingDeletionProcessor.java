package cloud.fogbow.ras.core.processors;

import cloud.fogbow.common.exceptions.FogbowException;
import cloud.fogbow.common.exceptions.InstanceNotFoundException;
import cloud.fogbow.common.exceptions.UnexpectedException;
import cloud.fogbow.common.models.linkedlists.ChainedList;
import cloud.fogbow.ras.constants.Messages;
import cloud.fogbow.ras.core.OrderController;
import cloud.fogbow.ras.core.SharedOrderHolders;
import cloud.fogbow.ras.core.cloudconnector.CloudConnectorFactory;
import cloud.fogbow.ras.core.cloudconnector.LocalCloudConnector;
import cloud.fogbow.ras.core.models.Operation;
import cloud.fogbow.ras.core.models.orders.Order;
import cloud.fogbow.ras.core.models.orders.OrderState;
import org.apache.log4j.Logger;

public class CheckingDeletionProcessor implements Runnable {
    private static final Logger LOGGER = Logger.getLogger(CheckingDeletionProcessor.class);

    private ChainedList<Order> closedOrders;
    /**
     * Attribute that represents the thread sleep time when there are no orders to be processed.
     */
    private Long sleepTime;
    private OrderController orderController;
    private String localProviderId;

    public CheckingDeletionProcessor(OrderController orderController, String localProviderId, String sleepTimeStr) {
        SharedOrderHolders sharedOrdersHolder = SharedOrderHolders.getInstance();
        this.closedOrders = sharedOrdersHolder.getCheckingDeletionOrdersList();
        this.sleepTime = Long.valueOf(sleepTimeStr);
        this.orderController = orderController;
        this.localProviderId = localProviderId;
    }

    /**
     * Iterates over the closed orders list and tries to process one order at a time. When the order
     * is null, it indicates that the iteration ended. A new iteration is started after some time.
     */
    @Override
    public void run() {
        boolean isActive = true;
        while (isActive) {
            try {
                Order order = this.closedOrders.getNext();
                if (order != null) {
                    processClosedOrder(order);
                } else {
                    this.closedOrders.resetPointer();
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

    protected void processClosedOrder(Order order) throws UnexpectedException {
        synchronized (order) {
            // Check if the order is still in the CHECKING_DELETION state (it could have been changed by another thread)
            OrderState orderState = order.getOrderState();
            if (!orderState.equals(OrderState.CHECKING_DELETION)) {
                return;
            }
            // Only local orders need to be monitored. Remote orders are monitored by the remote provider
            // and change state when that provider notifies state changes.
            if (order.isProviderRemote(this.localProviderId)) {
                return;
            }
            try {
                // Here we know that the CloudConnector is local, but the use of CloudConnectFactory facilitates testing.
                LocalCloudConnector localCloudConnector = (LocalCloudConnector)
                        CloudConnectorFactory.getInstance().getCloudConnector(this.localProviderId, order.getCloudName());
                // we won't audit requests we make
                localCloudConnector.switchOffAuditing();

                localCloudConnector.getInstance(order);
            } catch (InstanceNotFoundException e) {
                LOGGER.info(String.format(Messages.Info.INSTANCE_NOT_FOUND_S, order.getId()));
                // Remove any references that related dependencies of other orders with the order that has
                // just been deleted. Only the provider that has receiving the delete request through its
                // REST API needs to update order dependencies.
                if (order.isRequesterLocal(this.localProviderId)) {
                    this.orderController.updateOrderDependencies(order, Operation.DELETE);
                }
                this.orderController.deactivateOrder(order);
            } catch (FogbowException e) {
                LOGGER.info(String.format(Messages.Exception.GENERIC_EXCEPTION, e.getMessage()));
            }
        }
    }
}
