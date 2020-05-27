package cloud.fogbow.ras.core.processors;

import cloud.fogbow.common.exceptions.FogbowException;
import cloud.fogbow.common.exceptions.InstanceNotFoundException;
import cloud.fogbow.common.exceptions.UnexpectedException;
import cloud.fogbow.common.models.linkedlists.ChainedList;
import cloud.fogbow.ras.api.http.response.InstanceState;
import cloud.fogbow.ras.api.http.response.OrderInstance;
import cloud.fogbow.ras.constants.Messages;
import cloud.fogbow.ras.core.OrderController;
import cloud.fogbow.ras.core.SharedOrderHolders;
import cloud.fogbow.ras.core.cloudconnector.CloudConnectorFactory;
import cloud.fogbow.ras.core.cloudconnector.RemoteCloudConnector;
import cloud.fogbow.ras.core.models.orders.Order;
import cloud.fogbow.ras.core.models.orders.OrderState;
import org.apache.log4j.Logger;

public class RemoteOrdersStateSynchronizationProcessor implements Runnable {
    private static final Logger LOGGER = Logger.getLogger(RemoteOrdersStateSynchronizationProcessor.class);

    private ChainedList<Order> remoteProviderOrders;
    /**
     * Attribute that represents the thread sleep time when there are no orders to be processed.
     */
    private Long sleepTime;
    private OrderController orderController;
    private String localProviderId;

    public RemoteOrdersStateSynchronizationProcessor(OrderController orderController, String localProviderId, String sleepTimeStr) {
        SharedOrderHolders sharedOrdersHolder = SharedOrderHolders.getInstance();
        this.remoteProviderOrders = sharedOrdersHolder.getRemoteProviderOrdersList();
        this.sleepTime = Long.valueOf(sleepTimeStr);
        this.orderController = orderController;
        this.localProviderId = localProviderId;
    }

    /**
     * Iterates over the remoteProviderOrders list and tries to process one order at a time. When the order
     * is null, it indicates that the iteration ended. A new iteration is started after some time.
     */
    @Override
    public void run() {
        boolean isActive = true;
        while (isActive) {
            try {
                Order order = this.remoteProviderOrders.getNext();
                if (order != null) {
                    processRemoteProviderOrder(order);
                } else {
                    this.remoteProviderOrders.resetPointer();
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
     * The RemoteOrdersStateSynchronization processor monitors the state of remote orders to make their local
     * counterparts consistent. It must take into account that when an unanticipated failure occurs while trying
     * to delete an order whose provider is remote, the local requester has no way to know whether the deletion
     * was actually carried out. Thus, it assumes that it was and moves the local order to ASSIGNED_FOR_DELETION,
     * and tries to synchronize the local and remote system states here. Thus, this processor is also responsible
     * for detecting remote orders that should have been deleted, but were not. In this case, it should make sure
     * these orders are also deleted remotely.
     *
     * @param order {@link Order}
     */
    protected void processRemoteProviderOrder(Order order) throws UnexpectedException {
        OrderInstance remoteInstance;
        synchronized (order) {
           // Only remote orders need to be synchronized.
            if (order.isProviderLocal(this.localProviderId)) {
                // This should never happen.
                LOGGER.error(Messages.Error.UNEXPECTED_ERROR);
                return;
            }
            try {
                // Here we know that the CloudConnector is remote, but the use of CloudConnectFactory facilitates testing.
                RemoteCloudConnector remoteCloudConnector = (RemoteCloudConnector)
                        CloudConnectorFactory.getInstance().getCloudConnector(order.getProvider(), order.getCloudName());
                remoteInstance = remoteCloudConnector.getInstance(order);
                if (order.getOrderState().equals(OrderState.ASSIGNED_FOR_DELETION) &&
                        !remoteInstance.getState().equals(InstanceState.DELETING)) {
                    // Deletion has not been executed at the remote side due to a failure. Try it now.
                    try {
                        remoteCloudConnector.deleteInstance(order);
                    } catch (Exception e) {
                        LOGGER.warn(Messages.Error.UNABLE_TO_DELETE_INSTANCE);
                    }
                } else {
                    order.updateFromRemoteInstance(remoteInstance);
                }
            } catch (InstanceNotFoundException e) {
                LOGGER.info(String.format(Messages.Info.INSTANCE_NOT_FOUND_S, order.getId()));
                this.orderController.closeOrder(order);
            } catch (FogbowException e) {
                LOGGER.info(String.format(Messages.Exception.GENERIC_EXCEPTION, e.getMessage()));
            }
        }
    }
}
