package cloud.fogbow.ras.core.processors;

import cloud.fogbow.common.exceptions.RemoteCommunicationException;
import cloud.fogbow.common.exceptions.UnexpectedException;
import cloud.fogbow.common.models.linkedlists.ChainedList;
import cloud.fogbow.ras.constants.Messages;
import cloud.fogbow.ras.core.SharedOrderHolders;
import cloud.fogbow.ras.core.cloudconnector.CloudConnectorFactory;
import cloud.fogbow.ras.core.cloudconnector.RemoteCloudConnector;
import cloud.fogbow.ras.core.models.orders.Order;
import cloud.fogbow.ras.core.models.orders.OrderState;
import com.google.common.annotations.VisibleForTesting;
import org.apache.log4j.Logger;

public class RemoteOrdersStateSynchronizationProcessor implements Runnable {
    private static final Logger LOGGER = Logger.getLogger(RemoteOrdersStateSynchronizationProcessor.class);

    private ChainedList<Order> remoteProviderOrders;
    /**
     * Attribute that represents the thread sleep time when there are no orders to be processed.
     */
    private Long sleepTime;
    private String localProviderId;

    public RemoteOrdersStateSynchronizationProcessor(String localProviderId, String sleepTimeStr) {
        SharedOrderHolders sharedOrdersHolder = SharedOrderHolders.getInstance();
        this.remoteProviderOrders = sharedOrdersHolder.getRemoteProviderOrdersList();
        this.sleepTime = Long.valueOf(sleepTimeStr);
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
                synchronizeWithRemote();
            } catch (InterruptedException e) {
                isActive = false;
            }
        }
    }

    @VisibleForTesting
    void synchronizeWithRemote() throws InterruptedException {
        try {
            Order order = this.remoteProviderOrders.getNext();
            if (order != null) {
                processRemoteProviderOrder(order);
            } else {
                this.remoteProviderOrders.resetPointer();
                Thread.sleep(this.sleepTime);
            }
        } catch (InterruptedException e) {
            LOGGER.error(Messages.Error.THREAD_HAS_BEEN_INTERRUPTED, e);
            throw e;
        } catch (UnexpectedException e) {
            LOGGER.error(e.getMessage(), e);
        } catch (Throwable e) {
            LOGGER.error(Messages.Error.UNEXPECTED_ERROR, e);
        }
    }

    /**
     * The RemoteOrdersStateSynchronization processor monitors the state of remote orders to make their local
     * counterparts consistent.
     */
    @VisibleForTesting
    void processRemoteProviderOrder(Order order) throws UnexpectedException {
        synchronized (order) {
           // Only remote orders need to be synchronized.
            if (order.isProviderLocal(this.localProviderId)) {
                // This should never happen.
                LOGGER.error(Messages.Error.UNEXPECTED_ERROR);
                return;
            }
            try {
                // Orders in state ASSIGNED_FOR_DELETION and FAILED_ON_REQUEST need not be updated. This is
                // because the state of a FAILED_ON_REQUEST order cannot be changed by events happening at the
                // remote provider. ASSIGNED_FOR_DELETION orders will change state only when the remote provider
                // signals the local requester to close the order
                if (!order.getOrderState().equals(OrderState.FAILED_ON_REQUEST) &&
                        !order.getOrderState().equals(OrderState.ASSIGNED_FOR_DELETION)) {
                    // Here we know that the CloudConnector is remote, but the use of CloudConnectFactory facilitates testing.
                    RemoteCloudConnector remoteCloudConnector = (RemoteCloudConnector)
                            CloudConnectorFactory.getInstance().getCloudConnector(order.getProvider(), order.getCloudName());
                    Order remoteOrder = remoteCloudConnector.getRemoteOrder(order);
                    order.updateFromRemote(remoteOrder);
                    order.setOrderState(remoteOrder.getOrderState());
                }
            } catch (RemoteCommunicationException e) {
                // TODO(chico) - Check it with the Team; Should not we change it to Warn Log Level?
                LOGGER.info(String.format(Messages.Exception.GENERIC_EXCEPTION, e.getMessage()));
            }
        }
    }
}
