package cloud.fogbow.ras.core.processors;

import cloud.fogbow.common.exceptions.FogbowException;
import cloud.fogbow.common.exceptions.InternalServerErrorException;
import cloud.fogbow.ras.constants.Messages;
import cloud.fogbow.ras.core.OrderStateTransitioner;
import cloud.fogbow.ras.core.SharedOrderHolders;
import cloud.fogbow.ras.core.cloudconnector.CloudConnector;
import cloud.fogbow.ras.core.cloudconnector.CloudConnectorFactory;
import cloud.fogbow.ras.core.models.orders.Order;
import cloud.fogbow.ras.core.models.orders.OrderState;
import org.apache.log4j.Logger;

public class OpenProcessor extends StoppableOrderListProcessor implements Runnable {
    private static final Logger LOGGER = Logger.getLogger(OpenProcessor.class);

    private String localProviderId;

    public OpenProcessor(String localProviderId, String sleepTimeStr) {
        super(Long.valueOf(sleepTimeStr), 
                SharedOrderHolders.getInstance().getOpenOrdersList());
        this.localProviderId = localProviderId;
    }

    // ToDo: These processors (open, fulfilled, spawning, etc.) may need some refactoring
    //  to remove replicated code.


    /**
     * Get an instance for an order in the OPEN state. If the method fails to get the instance, then the order is
     * set to FAILED_ON_REQUEST state, else, it is set to the SPAWNING state if the order is local, or the PENDING
     * state if the order is remote. The SELECTED state exists simply to implement an "at-most-once" semantics
     * for the requestInstance() call. This transition removes the order from the OPEN list. Thus, once the order
     * is selected by the OpenProcessor, if the provider fails before advancing the state to either FAILED_ON_REQUEST,
     * PENDING or SPAWNING, when the provider recovers, this order will remain in the SELECTED state and will not
     * be retried. This is needed because the provider can't know whether the failure occurred before or after the
     * requestInstance() call. All other order processors call either getInstance() or deleteInstance(), and do not
     * need to bother with the effects of failures. This is because both getInstace() and deleteInstance() cause no
     * undesired collateral effects if invoked more than once.
     */
    protected void processOpenOrder(Order order) throws FogbowException {
        // The order object synchronization is needed to prevent a race
        // condition on order access. For example: a user can delete an open
        // order while this method is trying to get an Instance for this order.
        synchronized (order) {
            // Check if the order is still in the OPEN state (it could have been changed by another thread)
            OrderState orderState = order.getOrderState();
            if (!orderState.equals(OrderState.OPEN)) {
                return;
            }
            try {
                OrderStateTransitioner.transition(order, OrderState.SELECTED);
                CloudConnector cloudConnector = CloudConnectorFactory.getInstance().
                        getCloudConnector(order.getProvider(), order.getCloudName());
                String instanceId = cloudConnector.requestInstance(order);
                order.setInstanceId(instanceId);
                if (order.isProviderLocal(this.localProviderId)) {
                    if (instanceId != null) {
                        OrderStateTransitioner.transition(order, OrderState.SPAWNING);
                    } else {
                        throw new InternalServerErrorException(String.format(Messages.Exception.REQUEST_INSTANCE_NULL_S, order.getId()));
                    }
                } else {
                    OrderStateTransitioner.transition(order, OrderState.PENDING);
                }
            } catch (Exception e) {
                order.setInstanceId(null);
                order.setOnceFaultMessage(e.getMessage());
                if (order.isProviderLocal(this.localProviderId)) {
                    OrderStateTransitioner.transition(order, OrderState.FAILED_ON_REQUEST);
                } else {
                    OrderStateTransitioner.transitionToRemoteList(order, OrderState.FAILED_ON_REQUEST);
                }
                throw e;
            }
        }
    }

    @Override
    protected void doProcessing(Order order) throws FogbowException {
        processOpenOrder(order);
    }
}
