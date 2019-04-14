package cloud.fogbow.ras.core.processors;

import cloud.fogbow.common.exceptions.UnexpectedException;
import cloud.fogbow.common.models.linkedlists.ChainedList;
import cloud.fogbow.ras.constants.Messages;
import cloud.fogbow.ras.core.OrderController;
import cloud.fogbow.ras.core.SharedOrderHolders;
import cloud.fogbow.ras.core.models.orders.Order;
import org.apache.log4j.Logger;

public class ClosedProcessor implements Runnable {
    private static final Logger LOGGER = Logger.getLogger(ClosedProcessor.class);

    private ChainedList<Order> closedOrders;
    /**
     * Attribute that represents the thread sleep time when there are no orders to be processed.
     */
    private Long sleepTime;
    private OrderController orderController;

    public ClosedProcessor(OrderController orderController, String sleepTimeStr) {
        SharedOrderHolders sharedOrdersHolder = SharedOrderHolders.getInstance();
        this.closedOrders = sharedOrdersHolder.getClosedOrdersList();
        this.sleepTime = Long.valueOf(sleepTimeStr);
        this.orderController = orderController;
    }

    /**
     * Iterates over the closed orders list and tries to process one order at a time. When the order
     * is null, it indicates that the iteration ended. A new iteration is started after some time.
     */
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
            // No need to check the state of the order because no other thread will attempt to change the
            // state of an order that is in the CLOSED state.
           this.orderController.deactivateOrder(order);
        }
    }
}
