package org.fogbowcloud.manager.core.threads;

import org.apache.log4j.Logger;
import org.fogbowcloud.manager.core.constants.ConfigurationConstants;
import org.fogbowcloud.manager.core.constants.DefaultConfigurationConstants;
import org.fogbowcloud.manager.core.datastructures.OrderStateTransitioner;
import org.fogbowcloud.manager.core.datastructures.SharedOrderHolders;
import org.fogbowcloud.manager.core.exceptions.OrderStateTransitionException;
import org.fogbowcloud.manager.core.instanceprovider.InstanceProvider;
import org.fogbowcloud.manager.core.models.linkedList.ChainedList;
import org.fogbowcloud.manager.core.models.orders.Order;
import org.fogbowcloud.manager.core.models.orders.OrderState;
import org.fogbowcloud.manager.core.models.orders.OrderType;
import org.fogbowcloud.manager.core.models.orders.instances.ComputeOrderInstance;
import org.fogbowcloud.manager.core.models.orders.instances.InstanceState;
import org.fogbowcloud.manager.core.models.orders.instances.OrderInstance;
import org.fogbowcloud.manager.core.utils.SshConnectivityUtil;
import org.fogbowcloud.manager.core.utils.TunnelingServiceUtil;

import java.util.Properties;

/**
 * Process orders in fulfilled state. It monitors the resourced that have been
 * successfully initiated, to check for failures that may affect them.
 */
public class FulfilledMonitor implements Runnable {

    private static final Logger LOGGER = Logger.getLogger(FulfilledMonitor.class);

    private InstanceProvider localInstanceProvider;

    private ComputeInstanceConnectivity computeInstanceConnectivity;

    private ChainedList fulfilledOrdersList;

    /**
     * Attribute that represents the thread sleep time when there is no orders
     * to be processed.
     */
    private Long sleepTime;

    public FulfilledMonitor(InstanceProvider localInstanceProvider, TunnelingServiceUtil tunnelingService,
                            SshConnectivityUtil sshConnectivity, Properties properties) {
        this.localInstanceProvider = localInstanceProvider;

        this.computeInstanceConnectivity = new ComputeInstanceConnectivity(tunnelingService, sshConnectivity);

        SharedOrderHolders sharedOrderHolders = SharedOrderHolders.getInstance();
        this.fulfilledOrdersList = sharedOrderHolders.getFulfilledOrdersList();

        String schedulerPeriodStr = properties.getProperty(ConfigurationConstants.FULFILLED_ORDERS_SLEEP_TIME_KEY,
                DefaultConfigurationConstants.FULFILLED_ORDERS_SLEEP_TIME);
        this.sleepTime = Long.valueOf(schedulerPeriodStr);
    }

    /**
     * Iterates over the fulfilled orders list and try to process one fulfilled order per
     * time. If the order is null it indicates the iteration is in the end of
     * the list or the list is empty.
     */
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
                } else {
                    this.fulfilledOrdersList.resetPointer();
                    LOGGER.info("There is no fulfilled order to be processed, sleeping " +
                            "for " + this.sleepTime + " milliseconds");
                    Thread.sleep(this.sleepTime);
                }
            } catch (InterruptedException e) {
                isActive = false;
                LOGGER.warn("Thread interrupted", e);
            } catch (Throwable e) {
                LOGGER.error("Not expected error", e);
            }
        }
    }

    /**
     * Get an instance for a fulfilled order. If that instance is not reachable
     * then order state is set to failed.
     *
     * @param order {@link Order}
     * @throws OrderStateTransitionException Could not remove order from list of fulfilled orders.
     */
    protected void processFulfilledOrder(Order order) throws OrderStateTransitionException {
        synchronized (order) {
            OrderState orderState = order.getOrderState();

            if (orderState.equals(OrderState.FULFILLED)) {
                LOGGER.info("Trying to get an instance for order [" + order.getId() + "]");

                try {
                    this.processInstance(order);
                } catch (Exception e) {
                    LOGGER.error("Error while trying to get an instance for order: " + order, e);

                    LOGGER.info("Transition [" + order.getId() + "] order state from fulfilled to failed");
                    OrderStateTransitioner.transition(order, OrderState.FAILED);
                }
            }
        }
    }

    /**
     * This method does not synchronize the order object because it is private and
     * can only be called by the processSpawningOrder method.
     */
    private void processInstance(Order order) throws OrderStateTransitionException {
        OrderInstance orderInstance = this.localInstanceProvider.getInstance(order);
        OrderType orderType = order.getType();

        InstanceState instanceState = orderInstance.getState();

        if (instanceState.equals(InstanceState.FAILED)) {
            LOGGER.info("Instance state is failed for order [" + order.getId() + "]");
            OrderStateTransitioner.transition(order, OrderState.FAILED);
        } else if (instanceState.equals(InstanceState.ACTIVE) && orderType.equals(OrderType.COMPUTE)) {
            LOGGER.info("Processing active compute instance for order [" + order.getId() + "]");

            ComputeOrderInstance computeOrderInstance = (ComputeOrderInstance) orderInstance;
            this.computeInstanceConnectivity.setTunnelingServiceAddresses(order, computeOrderInstance);

            if (!this.computeInstanceConnectivity.isActiveConnectionFromInstance(computeOrderInstance)) {
                OrderStateTransitioner.transition(order, OrderState.FAILED);
            }
        }
    }
}
