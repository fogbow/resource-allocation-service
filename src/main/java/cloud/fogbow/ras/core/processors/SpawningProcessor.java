package cloud.fogbow.ras.core.processors;

import cloud.fogbow.common.exceptions.FogbowException;
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

public class SpawningProcessor implements Runnable {
    private static final Logger LOGGER = Logger.getLogger(SpawningProcessor.class);

    private ChainedList<Order> spawningOrderList;
    private Long sleepTime;
    private String localMemberId;

    public SpawningProcessor(String memberId, String sleepTimeStr) {
        SharedOrderHolders sharedOrderHolders = SharedOrderHolders.getInstance();
        this.spawningOrderList = sharedOrderHolders.getSpawningOrdersList();
        this.sleepTime = Long.valueOf(sleepTimeStr);
        this.localMemberId = memberId;
    }

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
                handleError(order, e.getMessage(), e);
            } catch (Throwable e) {
                handleError(order, Messages.Error.UNEXPECTED_ERROR, e);
            }
        }
    }

    protected void processSpawningOrder(Order order) throws FogbowException {
        // The order object synchronization is needed to prevent a race
        // condition on order access. For example: a user can delete an open
        // order while this method is trying to check the status of an instance
        // that has been requested in the cloud.
        synchronized (order) {
            OrderState orderState = order.getOrderState();
            // Check if the order is still in the Spawning state (it could have been changed by another thread)
            if (orderState.equals(OrderState.SPAWNING)) {
                processInstance(order);
            }
        }
    }

    private void processInstance(Order order) throws FogbowException {
        LocalCloudConnector localCloudConnector = (LocalCloudConnector) CloudConnectorFactory.getInstance().
                getCloudConnector(this.localMemberId, order.getCloudName());

        // we won't audit requests we make
        localCloudConnector.switchOffAuditing();

        Instance instance = localCloudConnector.getInstance(order);

        InstanceState instanceState = instance.getState();

        if (instanceState.equals(InstanceState.FAILED)) {
            OrderStateTransitioner.transition(order, OrderState.FAILED_AFTER_SUCCESSUL_REQUEST);
        } else if (instanceState.equals(InstanceState.READY)) {
            OrderStateTransitioner.transition(order, OrderState.FULFILLED);
        }
    }

    private void handleError(Order order, String message, Throwable e) {
        LOGGER.error(message, e);
        if (order != null) {
            if (!order.getOrderState().equals(OrderState.FAILED_AFTER_SUCCESSUL_REQUEST)) {
                try {
                    OrderStateTransitioner.transition(order, OrderState.FAILED_AFTER_SUCCESSUL_REQUEST);
                } catch (UnexpectedException e1) {
                    LOGGER.error(Messages.Error.UNEXPECTED_ERROR);
                }
            }
        }
    }
}
