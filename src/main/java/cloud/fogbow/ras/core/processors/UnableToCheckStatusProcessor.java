package cloud.fogbow.ras.core.processors;

import cloud.fogbow.common.exceptions.FogbowException;
import cloud.fogbow.common.exceptions.InstanceNotFoundException;
import cloud.fogbow.common.exceptions.UnavailableProviderException;
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

/**
 * Process orders in the state UNABLE_TO_CHECK_STATUS. It monitors resources whose status could not be retrieved
 * from the cloud, to check whether they should return to the fulfilled or to the failed states.
 */
public class UnableToCheckStatusProcessor implements Runnable {

	private static final Logger LOGGER = Logger.getLogger(UnableToCheckStatusProcessor.class);

	private ChainedList<Order> unableToCheckStatusOrdersList;
    /**
     * Attribute that represents the thread sleep time when there are no orders to be processed.
     */
	private Long sleepTime;
	private String localMemberId;

	public UnableToCheckStatusProcessor(String localMemberId, String sleepTimeStr) {
        SharedOrderHolders sharedOrderHolders = SharedOrderHolders.getInstance();
        this.unableToCheckStatusOrdersList = sharedOrderHolders.getUnableToCheckStatusOrdersList();
        this.sleepTime = Long.valueOf(sleepTimeStr);
        this.localMemberId = localMemberId;
    }

    /**
     * Iterates over the unableToCheckStatus orders list and tries to process one order at a time. When the order
     * is null, it indicates that the iteration ended. A new iteration is started after some time.
     */
	@Override
	public void run() {
		boolean isActive = true;

        while (isActive) {
            try {
                Order order = this.unableToCheckStatusOrdersList.getNext();

                if (order != null) {
                    processUnableToCheckStatusOrder(order);
                } else {
                    this.unableToCheckStatusOrdersList.resetPointer();
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
	 * Gets an instance for an order whose instance status could not be checked. If that instance is to be reachable
	 * again the order state is set to the current status of the instance.
	 *
	 * @param order {@link Order}
	 */
	protected void processUnableToCheckStatusOrder(Order order) throws FogbowException {
		Instance instance = null;
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
            // Only local orders need to be monitored. Remoted orders are monitored by the remote provider
            // and change state when that provider notifies state changes.
            if (order.isProviderRemote(this.localMemberId)) {
                return;
            }
            try {
                // Here we know that the CloudConnector is local, but the use of CloudConnectFactory facilitates testing.
                LocalCloudConnector localCloudConnector = (LocalCloudConnector)
                        CloudConnectorFactory.getInstance().getCloudConnector(this.localMemberId, order.getCloudName());
                // we won't audit requests we make
                localCloudConnector.switchOffAuditing();

                instance = localCloudConnector.getInstance(order);
                if (instance.isReady()) {
                    OrderStateTransitioner.transition(order, OrderState.FULFILLED);
                } else if (instance.hasFailed()) {
                    OrderStateTransitioner.transition(order, OrderState.FAILED_AFTER_SUCCESSFUL_REQUEST);
                }
            } catch (UnavailableProviderException e1) {
                LOGGER.error(Messages.Error.ERROR_WHILE_GETTING_INSTANCE_FROM_CLOUD, e1);
                throw e1;
            } catch (InstanceNotFoundException e2) {
                LOGGER.info(String.format(Messages.Info.INSTANCE_NOT_FOUND_S, order.getId()));
                OrderStateTransitioner.transition(order, OrderState.FAILED_AFTER_SUCCESSFUL_REQUEST);
                return;
            }
        }
	}
}
