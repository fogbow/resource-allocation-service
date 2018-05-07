package threads;

import org.apache.log4j.Logger;
import org.fogbowcloud.manager.core.constants.ConfigurationConstants;
import org.fogbowcloud.manager.core.constants.DefaultConfigurationConstants;
import org.fogbowcloud.manager.core.datastructures.SharedOrderHolders;
import org.fogbowcloud.manager.core.instanceprovider.InstanceProvider;
import org.fogbowcloud.manager.core.models.linkedList.ChainedList;
import org.fogbowcloud.manager.core.models.orders.Order;
import org.fogbowcloud.manager.core.models.orders.OrderState;

import java.util.Properties;

public class FulfilledProcessor implements Runnable {

    private InstanceProvider localInstanceProvider;
    private InstanceProvider remoteInstanceProvider;

    private String localMemberId;

    private ChainedList fulfilledOrdersList;

    private Long sleepTime;

	private static final Logger LOGGER = Logger.getLogger(FulfilledProcessor.class);

    public FulfilledProcessor(InstanceProvider localInstanceProvider, InstanceProvider remoteInstanceProvider,
                              Properties properties) {
        this.localInstanceProvider = localInstanceProvider;
        this.remoteInstanceProvider = remoteInstanceProvider;
        this.localMemberId = properties.getProperty(ConfigurationConstants.XMPP_ID_KEY);

        SharedOrderHolders sharedOrderHolders = SharedOrderHolders.getInstance();
        this.fulfilledOrdersList = sharedOrderHolders.getFulfilledOrdersList();

        String schedulerPeriodStr = properties.getProperty(ConfigurationConstants.FULFILLED_ORDERS_SLEEP_TIME_KEY,
                DefaultConfigurationConstants.FULFILLED_ORDERS_SLEEP_TIME);
        this.sleepTime = Long.valueOf(schedulerPeriodStr);
    }

    @Override
    public void run() {
        boolean isActive = true;

        while (isActive) {
            try {
                Order order = this.fulfilledOrdersList.getNext();

                if (order != null) {
                    try {
                            this.processFulfilledOrder(order);
                    } catch (Throwable e) {
                            LOGGER.error("Error while trying to process the order" + order, e);
                    }
                    this.processFulfilledOrder(order);
                } else {
                    this.fulfilledOrdersList.resetPointer();
                    Thread.sleep(this.sleepTime);
                }
            } catch (InterruptedException e) {
                isActive = false;
                LOGGER.warn("Thread interrupted", e);
            }
        }
    }

    private void processFulfilledOrder(Order order) {
        synchronized (order) {
            OrderState orderState = order.getOrderState();

            if (order.equals(OrderState.FULFILLED)) {
                LOGGER.info("Trying to get an instance for order [" + order.getId() + "]");

                InstanceProvider instanceProvider = this.getInstanceProviderForOrder(order);


            }
        }
    }

    private InstanceProvider getInstanceProviderForOrder(Order order) {
        InstanceProvider instanceProvider = null;

        if (order.isLocal(this.localMemberId)) {
        	LOGGER.info("The open order [" + order.getId() + "] is local");

            instanceProvider = this.localInstanceProvider;
        } else {
        	LOGGER.info("The open order [" + order.getId() + "] is remote for the member [" + order.getProvidingMember() + "]");

            instanceProvider = this.remoteInstanceProvider;
        }

        return instanceProvider;
    }
}
