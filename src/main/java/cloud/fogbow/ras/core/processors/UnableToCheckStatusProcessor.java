package cloud.fogbow.ras.core.processors;

import cloud.fogbow.common.exceptions.FogbowException;
import cloud.fogbow.ras.api.http.response.OrderInstance;
import cloud.fogbow.ras.constants.Messages;
import cloud.fogbow.ras.core.OrderStateTransitioner;
import cloud.fogbow.ras.core.SharedOrderHolders;
import cloud.fogbow.ras.core.cloudconnector.CloudConnectorFactory;
import cloud.fogbow.ras.core.cloudconnector.LocalCloudConnector;
import cloud.fogbow.ras.core.models.orders.Order;
import cloud.fogbow.ras.core.models.orders.OrderState;
import org.apache.log4j.Logger;

public class UnableToCheckStatusProcessor extends StoppableOrderListProcessor implements Runnable {

	private static final Logger LOGGER = Logger.getLogger(UnableToCheckStatusProcessor.class);

	private String localProviderId;
	
	public UnableToCheckStatusProcessor(String localProviderId, String sleepTimeStr) {
	    super(Long.valueOf(sleepTimeStr), 
	            SharedOrderHolders.getInstance().getUnableToCheckStatusOrdersList());
        this.localProviderId = localProviderId;
    }

	/**
	 * Gets an instance for an order whose instance status could not be checked. If that instance is to be reachable
	 * again the order state is set to the current status of the instance.
	 *
	 * @param order {@link Order}
	 */
	protected void processUnableToCheckStatusOrder(Order order) throws FogbowException {
		OrderInstance instance = null;
        // The order object synchronization is needed to prevent a race
        // condition on order access. For example: a user can delete the
        // order while this method is trying to check the status of an instance
        // that was allocated to the order.
        synchronized (order) {
            // Check if the order is still in the UNABLE_TO_CHECK_STATUS state (it could have been changed by
            // another thread)
            OrderState orderState = order.getOrderState();
            if (!orderState.equals(OrderState.UNABLE_TO_CHECK_STATUS)) {
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

                instance = localCloudConnector.getInstance(order);
                if (instance.isReady()) {
                    OrderStateTransitioner.transition(order, OrderState.FULFILLED);
                } else if (instance.hasFailed()) {
                    OrderStateTransitioner.transition(order, OrderState.FAILED_AFTER_SUCCESSFUL_REQUEST);
                }
            } catch (Exception e) {
                order.setOnceFaultMessage(e.getMessage());
                LOGGER.info(String.format(Messages.Exception.GENERIC_EXCEPTION_S, e));
                OrderStateTransitioner.transition(order, OrderState.FAILED_AFTER_SUCCESSFUL_REQUEST);
            }
        }
	}

    @Override
    protected void doProcessing(Order order) throws FogbowException {
        processUnableToCheckStatusOrder(order);
    }
}
