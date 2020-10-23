package cloud.fogbow.ras.core.processors;

import org.apache.log4j.Logger;

import com.google.common.annotations.VisibleForTesting;

import cloud.fogbow.common.exceptions.FogbowException;
import cloud.fogbow.ras.constants.Messages;
import cloud.fogbow.ras.core.models.orders.Order;

public abstract class StoppableProcessor implements Runnable {
    private static final Logger LOGGER = Logger.getLogger(StoppableProcessor.class);
    
    protected Boolean mustStop;
    protected Boolean isActive;
    /**
     * Attribute that represents the thread sleep time when there are no orders to be processed.
     */
    protected Long sleepTime;
    
    protected void checkIfMustStop() { 
        if (this.mustStop) {
            this.isActive = false;
        }
    }
    
    public void stop() {
        this.mustStop = true;
        while (this.isActive) {
            try {
                // TODO Currently this stop method only works with this sleep
                // Needs further investigation
                Thread.sleep(50);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }

        this.mustStop = false;
    }
    
    /**
     * Iterates over the orders list and tries to process one order at a time. When the order
     * is null, it indicates that the iteration ended. A new iteration is started after some time.
     */
    @Override
    public void run() {
        this.isActive = true;
        while (isActive) {
            try {
                doRun();
            } catch (InterruptedException e) {
                isActive = false;
            }           
        }
    }
    
    @VisibleForTesting
    void doRun() throws InterruptedException {
        Order order = null;
        try {
            order = getNext();
            if (order != null) {
                doProcessing(order);
            } else {
                reset();
                Thread.sleep(this.sleepTime);
            }
            
            checkIfMustStop();
        } catch (InterruptedException e) {
            LOGGER.error(Messages.Log.THREAD_HAS_BEEN_INTERRUPTED, e);
            throw e;
        } catch (FogbowException e) {
            LOGGER.error(e.getMessage(), e);
        } catch (Throwable e) {
            LOGGER.error(Messages.Log.UNEXPECTED_ERROR, e);
        }
    }
    
    protected abstract void doProcessing(Order order) throws InterruptedException, FogbowException;
    protected abstract Order getNext();
    protected abstract void reset();
}
