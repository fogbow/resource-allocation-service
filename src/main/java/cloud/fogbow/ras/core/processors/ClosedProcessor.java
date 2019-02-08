package cloud.fogbow.ras.core.processors;

import cloud.fogbow.common.exceptions.UnexpectedException;
import org.apache.log4j.Logger;
import cloud.fogbow.ras.core.OrderStateTransitioner;
import cloud.fogbow.ras.core.SharedOrderHolders;
import cloud.fogbow.ras.core.constants.Messages;
import cloud.fogbow.ras.core.models.linkedlists.ChainedList;
import cloud.fogbow.ras.core.models.orders.Order;

public class ClosedProcessor implements Runnable {
    private static final Logger LOGGER = Logger.getLogger(ClosedProcessor.class);

    private ChainedList closedOrders;
    private Long sleepTime;

    public ClosedProcessor(String sleepTimeStr) {
        SharedOrderHolders sharedOrdersHolder = SharedOrderHolders.getInstance();
        this.closedOrders = sharedOrdersHolder.getClosedOrdersList();
        this.sleepTime = Long.valueOf(sleepTimeStr);
    }

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
           OrderStateTransitioner.deactivateOrder(order);
        }
    }
}
