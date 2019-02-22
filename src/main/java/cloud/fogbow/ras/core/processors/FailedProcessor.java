package cloud.fogbow.ras.core.processors;

import cloud.fogbow.common.exceptions.UnexpectedException;
import cloud.fogbow.ras.constants.Messages;
import cloud.fogbow.ras.core.OrderStateTransitioner;
import cloud.fogbow.ras.core.SharedOrderHolders;
import cloud.fogbow.ras.core.cloudconnector.CloudConnectorFactory;
import cloud.fogbow.ras.core.cloudconnector.LocalCloudConnector;
import cloud.fogbow.ras.api.http.response.Instance;
import cloud.fogbow.ras.api.http.response.InstanceState;
import cloud.fogbow.ras.core.models.linkedlists.ChainedList;
import cloud.fogbow.ras.core.models.orders.Order;
import cloud.fogbow.ras.core.models.orders.OrderState;
import org.apache.log4j.Logger;

/**
 * Process orders in the state failed. It monitors resources that failed after
 * successful requests, to check that they can return to the fulfilled state.
 */
public class FailedProcessor implements Runnable {

	private static final Logger LOGGER = Logger.getLogger(FailedProcessor.class);

	private ChainedList failedOrdersList;
	private Long sleepTime;
	private String localMemberId;

	public FailedProcessor(String localMemberId, String sleepTimeStr) {
        SharedOrderHolders sharedOrderHolders = SharedOrderHolders.getInstance();
        this.failedOrdersList = sharedOrderHolders.getFailedAfterSuccessfulRequestOrdersList();
        this.sleepTime = Long.valueOf(sleepTimeStr);
        this.localMemberId = localMemberId;
    }
	
	/**
	 * Iterates over the failed orders list and try to process one failed order per
	 * time. If the order is null it indicates the iteration is in the end of the
	 * list or the list is empty.
	 */
	@Override
	public void run() {
		boolean isActive = true;

        while (isActive) {
            try {
                Order order = this.failedOrdersList.getNext();

                if (order != null) {
                    processFailedOrder(order);
                } else {
                    this.failedOrdersList.resetPointer();
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
	 * Gets an instance for a failed order. If that instance is to be reachable
	 * again the order state is set to fulfilled.
	 *
	 * @param order {@link Order}
	 */
	protected void processFailedOrder(Order order) throws UnexpectedException {
		Instance instance = null;
        InstanceState instanceState = null;
        
        synchronized (order) {
            OrderState orderState = order.getOrderState();

            if (!order.isProviderLocal(this.localMemberId)) {
                return;
            }

            if (!orderState.equals(OrderState.FAILED_AFTER_SUCCESSUL_REQUEST)) {
                return;
            }
            try {
                LocalCloudConnector localCloudConnector = (LocalCloudConnector) CloudConnectorFactory.getInstance().
                        getCloudConnector(this.localMemberId, order.getCloudName());
                
                instance = localCloudConnector.getInstance(order);
            } catch (Exception e) {
                LOGGER.error(Messages.Error.ERROR_WHILE_GETTING_INSTANCE_FROM_CLOUD, e);
                return;
            }
            instanceState = instance.getState();
            if (instanceState.equals(InstanceState.READY)) {
                OrderStateTransitioner.transition(order, OrderState.FULFILLED);
                return;
            }
        }
	}

}
