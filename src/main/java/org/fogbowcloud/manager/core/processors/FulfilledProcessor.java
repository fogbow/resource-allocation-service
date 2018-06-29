package org.fogbowcloud.manager.core.processors;

import org.apache.log4j.Logger;
import org.fogbowcloud.manager.core.intercomponent.exceptions.RemoteRequestException;
import org.fogbowcloud.manager.core.OrderStateTransitioner;
import org.fogbowcloud.manager.core.SharedOrderHolders;
import org.fogbowcloud.manager.core.cloudconnector.CloudConnector;
import org.fogbowcloud.manager.core.cloudconnector.CloudConnectorFactory;
import org.fogbowcloud.manager.core.exceptions.InstanceNotFoundException;
import org.fogbowcloud.manager.core.exceptions.OrderStateTransitionException;
import org.fogbowcloud.manager.core.exceptions.PropertyNotSpecifiedException;
import org.fogbowcloud.manager.core.exceptions.RequestException;
import org.fogbowcloud.manager.core.plugins.exceptions.TokenCreationException;
import org.fogbowcloud.manager.core.plugins.exceptions.UnauthorizedException;
import org.fogbowcloud.manager.core.models.SshTunnelConnectionData;
import org.fogbowcloud.manager.core.models.linkedlist.ChainedList;
import org.fogbowcloud.manager.core.models.orders.Order;
import org.fogbowcloud.manager.core.models.orders.OrderState;
import org.fogbowcloud.manager.core.models.instances.InstanceType;
import org.fogbowcloud.manager.core.models.instances.Instance;
import org.fogbowcloud.manager.core.models.instances.InstanceState;
import org.fogbowcloud.manager.utils.ComputeInstanceConnectivityUtil;
import org.fogbowcloud.manager.utils.SshConnectivityUtil;
import org.fogbowcloud.manager.utils.TunnelingServiceUtil;

/**
 * Process orders in fulfilled state. It monitors the resourced that have been successfully
 * initiated, to check for failures that may affect them.
 */
public class FulfilledProcessor implements Runnable {

    private static final Logger LOGGER = Logger.getLogger(FulfilledProcessor.class);

    private String localMemberId;

    private CloudConnector localCloudConnector;

    private ComputeInstanceConnectivityUtil computeInstanceConnectivity;

    private ChainedList fulfilledOrdersList;

    /**
     * Attribute that represents the thread sleep time when there is no orders to be processed.
     */
    private Long sleepTime;

    public FulfilledProcessor(
            String localMemberId,
            TunnelingServiceUtil tunnelingService,
            SshConnectivityUtil sshConnectivity, String sleepTimeStr) {

        CloudConnectorFactory cloudConnectorFactory = CloudConnectorFactory.getInstance();

        this.localMemberId = localMemberId;

        this.localCloudConnector = cloudConnectorFactory.getCloudConnector(localMemberId);

        this.computeInstanceConnectivity =
            new ComputeInstanceConnectivityUtil(tunnelingService, sshConnectivity);

        SharedOrderHolders sharedOrderHolders = SharedOrderHolders.getInstance();
        this.fulfilledOrdersList = sharedOrderHolders.getFulfilledOrdersList();

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
                    LOGGER.debug(
                        "There is no fulfilled order to be processed, sleeping for "
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

    /**
     * Gets an instance for a fulfilled order. If that instance is not reachable the order state is
     * set to failed.
     *
     * @param order {@link Order}
     * @throws OrderStateTransitionException Could not remove order from list of fulfilled orders.
     */
    protected void processFulfilledOrder(Order order) throws OrderStateTransitionException {

        Instance instance = null;
        InstanceState instanceState = null;

        //this block tries to get the info about the instance in the cloud and check it is up.
        //it runs exclusive in the order object
        synchronized (order) {
            OrderState orderState = order.getOrderState();

            if (!order.isProviderLocal(this.localMemberId)) {
                return;
            }

            if (!orderState.equals(OrderState.FULFILLED)) {
                return;
            }

            LOGGER.info("Trying to get an instance for order [" + order.getId() + "]");

            try {
                instance = this.localCloudConnector.getInstance(order);
            } catch (Exception e) {
                LOGGER.error("Error while getting instance from the cloud.", e);
                OrderStateTransitioner.transition(order, OrderState.FAILED);
                return;
            }

            instanceState = instance.getState();
            if (instanceState.equals(InstanceState.FAILED)) {
                LOGGER.info("Instance state is failed for order [" + order.getId() + "]");
                OrderStateTransitioner.transition(order, OrderState.FAILED);
                return;
            }
        }

        //now, we know the cloud is able to manage the resource and it is up. then, we try to ssh it
        //below code DOES NOT run exclusive in the order. we relax it because the isInstanceReachable may take too long
        //to finish. So, a get to this order or, even worse, a get all call in the API will be blocked

        InstanceType instanceType = order.getType();
        if (instanceState.equals(InstanceState.READY) && instanceType.equals(InstanceType.COMPUTE)) {
            LOGGER.info("Processing active compute instance for order [" + order.getId() + "]");

            SshTunnelConnectionData sshTunnelConnectionData = this.computeInstanceConnectivity
                    .getSshTunnelConnectionData(order.getId());
            if (sshTunnelConnectionData != null) {
                boolean instanceReachable = this.computeInstanceConnectivity.isInstanceReachable(sshTunnelConnectionData);

                if (!instanceReachable) {

                    //taking the mutex back.
                    //Since we might have lost the CPU, it is possible the order is not longer FULFILLED
                    synchronized (order) {

                        OrderStateTransitioner.transition(order, OrderState.FAILED);
                        return;
                    }
                }
            }
        }

        LOGGER.debug("The instance was processed successfully");
    }

}
