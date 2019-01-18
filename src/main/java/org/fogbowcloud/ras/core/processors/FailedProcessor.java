package org.fogbowcloud.ras.core.processors;

import org.apache.log4j.Logger;
import org.fogbowcloud.ras.core.OrderStateTransitioner;
import org.fogbowcloud.ras.core.SharedOrderHolders;
import org.fogbowcloud.ras.core.cloudconnector.CloudConnectorFactory;
import org.fogbowcloud.ras.core.cloudconnector.LocalCloudConnector;
import org.fogbowcloud.ras.core.constants.Messages;
import org.fogbowcloud.ras.core.exceptions.UnexpectedException;
import org.fogbowcloud.ras.core.models.instances.Instance;
import org.fogbowcloud.ras.core.models.instances.InstanceState;
import org.fogbowcloud.ras.core.models.linkedlists.ChainedList;
import org.fogbowcloud.ras.core.models.orders.Order;
import org.fogbowcloud.ras.core.models.orders.OrderState;

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

	private void processFailedOrder(Order order) throws UnexpectedException {
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
