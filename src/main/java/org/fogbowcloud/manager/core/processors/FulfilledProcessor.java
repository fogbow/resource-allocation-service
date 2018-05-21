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
import org.fogbowcloud.manager.utils.ComputeInstanceConnectivityUtils;
import org.fogbowcloud.manager.utils.SshConnectivityUtil;
import org.fogbowcloud.manager.utils.TunnelingServiceUtil;

/**
 * Process orders in fulfilled state. It monitors the resourced that have been successfully
 * initiated, to check for failures that may affect them.
 */
public class FulfilledProcessor implements Runnable {

    private static final Logger LOGGER = Logger.getLogger(FulfilledProcessor.class);

    private InstanceProvider localInstanceProvider;
    private InstanceProvider remoteInstanceProvider;
    private String localMemberId;

    private ComputeInstanceConnectivityUtils computeInstanceConnectivity;

    private ChainedList fulfilledOrdersList;

    /** Attribute that represents the thread sleep time when there is no orders to be processed. */
    private Long sleepTime;

    public FulfilledProcessor(
            InstanceProvider localInstanceProvider,
            InstanceProvider remoteInstanceProvider,
            TunnelingServiceUtil tunnelingService,
            SshConnectivityUtil sshConnectivity,
            Properties properties) {
        this.localInstanceProvider = localInstanceProvider;
        this.remoteInstanceProvider = remoteInstanceProvider;
        this.localMemberId = properties.getProperty(ConfigurationConstants.XMPP_ID_KEY);

        this.computeInstanceConnectivity =
                new ComputeInstanceConnectivityUtils(tunnelingService, sshConnectivity);

        SharedOrderHolders sharedOrderHolders = SharedOrderHolders.getInstance();
        this.fulfilledOrdersList = sharedOrderHolders.getFulfilledOrdersList();

        String sleepTimeStr =
                properties.getProperty(
                        ConfigurationConstants.FULFILLED_ORDERS_SLEEP_TIME_KEY,
                        DefaultConfigurationConstants.FULFILLED_ORDERS_SLEEP_TIME);
        this.sleepTime = Long.valueOf(sleepTimeStr);
    }

    /**
     * Iterates over the fulfilled orders list and try to process one fulfilled order per time. If
     * the order is null it indicates the iteration is in the end of the list or the list is empty.
     */
    @Override
    public void run() {
        boolean isActive = true;

        while (isActive) {
            try {
                Order order = this.fulfilledOrdersList.getNext();

                if (order != null) {
                    try {
                        processFulfilledOrder(order);
                    } catch (OrderStateTransitionException e) {
                        LOGGER.error("Error while trying to process the order " + order, e);
                    }
                } else {
                    this.fulfilledOrdersList.resetPointer();
                    LOGGER.info(
                            "There is no fulfilled order to be processed, sleeping for "
                                    + this.sleepTime
                                    + " milliseconds");
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
     * Gets an instance for a fulfilled order. If that instance is not reachable the order state is
     * set to failed.
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
                    processInstance(order);
                } catch (OrderStateTransitionException e) {
                    LOGGER.error(
                            "Transition ["
                                    + order.getId()
                                    + "] order state from fulfilled to failed");
                } catch (Exception e) {
                    LOGGER.error("Error while getting instance from the cloud.", e);
                    OrderStateTransitioner.transition(order, OrderState.FAILED);
                }
            }
        }
    }

    /**
     * Checks if instance state is failed and changes the order state to failed. Checks SSH
     * connectivity if instance state is active and the order type is compute.
     *
     * @param order {@link Order}
     */
    protected void processInstance(Order order) throws Exception {
        InstanceProvider instanceProvider = getInstanceProviderForOrder(order);

        OrderType orderType = order.getType();

        InstanceState instanceState = instanceProvider.getInstance(order).getState();

        if (instanceState.equals(InstanceState.FAILED)) {
            LOGGER.info("Instance state is failed for order [" + order.getId() + "]");
            OrderStateTransitioner.transition(order, OrderState.FAILED);
        } else if (instanceState.equals(InstanceState.ACTIVE)
                && orderType.equals(OrderType.COMPUTE)) {
            LOGGER.info("Processing active compute instance for order [" + order.getId() + "]");

            // TODO Need to get the tunnelling information
            ComputeOrderInstance computeOrderInstance =
                    (ComputeOrderInstance) order.getOrderInstance();
            computeOrderInstance.setState(instanceState);

            if (!this.computeInstanceConnectivity.isInstanceReachable(computeOrderInstance)) {
                OrderStateTransitioner.transition(order, OrderState.FAILED);
            }
        } else {
            LOGGER.info("The instance was processed successfully");
        }
    }

    /**
     * Get an instance provider from an order, if order is local, the returned instance provider is
     * the local, otherwise, it is the remote.
     *
     * @param order {@link Order}
     * @return corresponding instance provider.
     */
    private InstanceProvider getInstanceProviderForOrder(Order order) {
        InstanceProvider instanceProvider;

        if (order.isLocal(this.localMemberId)) {
            LOGGER.info("The open order [" + order.getId() + "] is local");

            instanceProvider = this.localInstanceProvider;
        } else {
            LOGGER.info(
                    "The open order ["
                            + order.getId()
                            + "] is remote for the "
                            + "member ["
                            + order.getProvidingMember()
                            + "]");

            instanceProvider = this.remoteInstanceProvider;
        }

        return instanceProvider;
    }
}
