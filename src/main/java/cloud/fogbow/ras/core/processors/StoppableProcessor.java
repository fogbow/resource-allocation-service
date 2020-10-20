package cloud.fogbow.ras.core.processors;

import org.apache.log4j.Logger;

public abstract class StoppableProcessor {
    private static final Logger LOGGER = Logger.getLogger(StoppableProcessor.class);
    
    protected Boolean mustStop;
    protected Boolean isActive;
    
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
}
