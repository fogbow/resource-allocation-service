package org.fogbowcloud.manager.core.processors;

import java.util.Properties;
import org.apache.log4j.Logger;
import org.fogbowcloud.manager.core.OrderStateTransitioner;
import org.fogbowcloud.manager.core.SharedOrderHolders;
import org.fogbowcloud.manager.core.exceptions.OrderStateTransitionException;
import org.fogbowcloud.manager.core.instanceprovider.InstanceProvider;
import org.fogbowcloud.manager.core.manager.constants.ConfigurationConstants;
import org.fogbowcloud.manager.core.manager.constants.DefaultConfigurationConstants;
import org.fogbowcloud.manager.core.models.linkedlist.ChainedList;
import org.fogbowcloud.manager.core.models.orders.Order;
import org.fogbowcloud.manager.core.models.orders.OrderState;
import org.fogbowcloud.manager.core.models.orders.OrderType;
import org.fogbowcloud.manager.core.models.orders.instances.ComputeOrderInstance;
import org.fogbowcloud.manager.core.models.orders.instances.InstanceState;
import org.fogbowcloud.manager.core.models.orders.instances.OrderInstance;
import org.fogbowcloud.manager.utils.ComputeInstanceConnectivityUtils;
import org.fogbowcloud.manager.utils.SshConnectivityUtil;
import org.fogbowcloud.manager.utils.TunnelingServiceUtil;

public class SpawningProcessor implements Runnable {

    private static final Logger LOGGER = Logger.getLogger(SpawningProcessor.class);

    private ChainedList spawningOrderList;
    private ComputeInstanceConnectivityUtils computeInstanceConnectivity;

    private Long sleepTime;

    private InstanceProvider localInstanceProvider;

    public SpawningProcessor(
            TunnelingServiceUtil tunnelingService,
            SshConnectivityUtil sshConnectivity,
            InstanceProvider localInstanceProvider,
            Properties properties) {
        this.computeInstanceConnectivity =
                new ComputeInstanceConnectivityUtils(tunnelingService, sshConnectivity);

        this.localInstanceProvider = localInstanceProvider;

        SharedOrderHolders sharedOrderHolders = SharedOrderHolders.getInstance();
        this.spawningOrderList = sharedOrderHolders.getSpawningOrdersList();

        String sleepTimeStr =
                properties.getProperty(
                        ConfigurationConstants.SPAWNING_ORDERS_SLEEP_TIME_KEY,
                        DefaultConfigurationConstants.SPAWNING_ORDERS_SLEEP_TIME);
        this.sleepTime = Long.valueOf(sleepTimeStr);
    }

    @Override
    public void run() {
        boolean isActive = true;
        while (isActive) {
            try {
                Order order = this.spawningOrderList.getNext();
                if (order != null) {
                    try {
                        processSpawningOrder(order);
                    } catch (OrderStateTransitionException e) {
                        LOGGER.error("Error while trying to change the state of order " + order, e);
                    }
                } else {
                    this.spawningOrderList.resetPointer();
                    LOGGER.debug(
                            "There is no spawning order to be processed, sleeping for "
                                    + this.sleepTime
                                    + " milliseconds");
                    Thread.sleep(this.sleepTime);
                }
            } catch (InterruptedException e) {
                isActive = false;
                LOGGER.warn("Thread interrupted", e);
            } catch (Throwable e) {
                LOGGER.error("Unexpected error", e);
            }
        }
    }

    protected void processSpawningOrder(Order order) throws Exception {
        synchronized (order) {
            OrderState orderState = order.getOrderState();
            if (orderState.equals(OrderState.SPAWNING)) {
                LOGGER.debug("Trying to process an instance for order [" + order.getId() + "]");
                processInstance(order);
            } else {
                LOGGER.debug("This order state is not spawning for order [" + order.getId() + "]");
            }
        }
    }

    /**
     * This method does not synchronize the order object because it is private and can only be
     * called by the processSpawningOrder method.
     */
    private void processInstance(Order order) throws Exception {
        OrderInstance orderInstance = this.localInstanceProvider.getInstance(order);
        OrderType orderType = order.getType();

        if (orderType.equals(OrderType.COMPUTE)) {
            InstanceState instanceState = orderInstance.getState();

            if (instanceState.equals(InstanceState.FAILED)) {
                LOGGER.debug(
                        "The compute instance state is failed for order [" + order.getId() + "]");
                OrderStateTransitioner.transition(order, OrderState.FAILED);

            } else if (instanceState.equals(InstanceState.ACTIVE)) {
                LOGGER.debug("Processing active compute instance for order [" + order.getId() + "]");

                ComputeOrderInstance computeOrderInstance = (ComputeOrderInstance) orderInstance;

                // TODO Check tunnel interaction to try to avoid doing this multiple times
                this.computeInstanceConnectivity.setTunnelingServiceAddresses(
                        order, computeOrderInstance);

                if (this.computeInstanceConnectivity.isInstanceReachable(computeOrderInstance)) {
                    OrderStateTransitioner.transition(order, OrderState.FULFILLED);
                }

            } else {
                LOGGER.debug(
                        "The compute instance state is inactive for order [" + order.getId() + "]");
            }
        }
    }
}
