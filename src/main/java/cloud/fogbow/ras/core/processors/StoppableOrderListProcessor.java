package cloud.fogbow.ras.core.processors;

import org.apache.log4j.Logger;

import cloud.fogbow.common.exceptions.FogbowException;
import cloud.fogbow.common.models.linkedlists.ChainedList;
import cloud.fogbow.common.util.StoppableRunner;
import cloud.fogbow.ras.constants.Messages;
import cloud.fogbow.ras.core.models.orders.Order;

public abstract class StoppableOrderListProcessor extends StoppableRunner {
    private static final Logger LOGGER = Logger.getLogger(StoppableOrderListProcessor.class);
    
    private ChainedList<Order> orderList;
    
    protected StoppableOrderListProcessor(Long sleepTime, ChainedList<Order> orderList) {
        super(sleepTime);
        this.orderList = orderList;
    }
    
    @Override
    public void doRun() throws InterruptedException {
        Order order = null;
        try {
            order = getNext();
            
            while (order != null) {
                doProcessing(order);
                order = getNext();
            }
            
            reset();
        } catch (FogbowException e) {
            LOGGER.error(e.getMessage(), e);
        } catch (Throwable e) {
            LOGGER.error(Messages.Log.UNEXPECTED_ERROR, e);
        }
    }
    
    private Order getNext() {
        return this.orderList.getNext();
    }

    private void reset() {
        this.orderList.resetPointer();     
    }
    
    protected abstract void doProcessing(Order order) throws FogbowException;
}
