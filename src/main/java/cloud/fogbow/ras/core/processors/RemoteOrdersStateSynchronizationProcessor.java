package cloud.fogbow.ras.core.processors;

import cloud.fogbow.common.exceptions.FogbowException;
import cloud.fogbow.common.exceptions.InternalServerErrorException;
import cloud.fogbow.common.models.linkedlists.ChainedList;
import cloud.fogbow.ras.constants.Messages;
import cloud.fogbow.ras.core.SharedOrderHolders;
import cloud.fogbow.ras.core.cloudconnector.CloudConnectorFactory;
import cloud.fogbow.ras.core.cloudconnector.RemoteCloudConnector;
import cloud.fogbow.ras.core.models.orders.Order;
import cloud.fogbow.ras.core.models.orders.OrderState;
import com.google.common.annotations.VisibleForTesting;
import org.apache.log4j.Logger;

public class RemoteOrdersStateSynchronizationProcessor extends StoppableProcessor implements Runnable {
    private static final Logger LOGGER = Logger.getLogger(RemoteOrdersStateSynchronizationProcessor.class);

    private ChainedList<Order> remoteProviderOrders;
    private String localProviderId;

    public RemoteOrdersStateSynchronizationProcessor(String localProviderId, String sleepTimeStr) {
        SharedOrderHolders sharedOrdersHolder = SharedOrderHolders.getInstance();
        this.remoteProviderOrders = sharedOrdersHolder.getRemoteProviderOrdersList();
        this.sleepTime = Long.valueOf(sleepTimeStr);
        this.localProviderId = localProviderId;
        this.isActive = false;
        this.mustStop = false;
    }

    public void setSleepTime(Long sleepTime) {
        this.sleepTime = sleepTime;
    }

    /**
     * The RemoteOrdersStateSynchronization processor monitors the state of remote orders to make their local
     * counterparts consistent.
     */
    @VisibleForTesting
    void processRemoteProviderOrder(Order order) throws InternalServerErrorException {
        synchronized (order) {
           // Only remote orders need to be synchronized.
            if (order.isProviderLocal(this.localProviderId)) {
                // This should never happen.
                LOGGER.error(Messages.Log.UNEXPECTED_ERROR);
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
            } catch (FogbowException e) {
                LOGGER.warn(String.format(Messages.Exception.GENERIC_EXCEPTION_S, e.getMessage()));
            }
        }
    }

    @Override
    protected void doProcessing(Order order) throws InterruptedException, FogbowException {
        processRemoteProviderOrder(order);
    }

    @Override
    protected Order getNext() {
        return this.remoteProviderOrders.getNext();
    }

    @Override
    protected void reset() {
        this.remoteProviderOrders.resetPointer();
    }
}
