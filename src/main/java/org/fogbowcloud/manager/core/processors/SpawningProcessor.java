package org.fogbowcloud.manager.core.processors;

import org.apache.log4j.Logger;
import org.fogbowcloud.manager.core.exceptions.*;
import org.fogbowcloud.manager.core.OrderStateTransitioner;
import org.fogbowcloud.manager.core.SharedOrderHolders;
import org.fogbowcloud.manager.core.cloudconnector.CloudConnectorFactory;
import org.fogbowcloud.manager.core.cloudconnector.CloudConnector;
import org.fogbowcloud.manager.core.models.instances.InstanceType;
import org.fogbowcloud.manager.core.models.SshTunnelConnectionData;
import org.fogbowcloud.manager.core.models.linkedlist.ChainedList;
import org.fogbowcloud.manager.core.models.orders.Order;
import org.fogbowcloud.manager.core.models.orders.OrderState;
import org.fogbowcloud.manager.core.models.instances.Instance;
import org.fogbowcloud.manager.core.models.instances.InstanceState;
import org.fogbowcloud.manager.utils.ComputeInstanceConnectivityUtil;
import org.fogbowcloud.manager.utils.SshConnectivityUtil;
import org.fogbowcloud.manager.utils.TunnelingServiceUtil;

public class SpawningProcessor implements Runnable {

    private static final Logger LOGGER = Logger.getLogger(SpawningProcessor.class);

    private ChainedList spawningOrderList;
    private ComputeInstanceConnectivityUtil computeInstanceConnectivity;

    private Long sleepTime;

    private CloudConnector localCloudConnector;

    public SpawningProcessor(
            String memberId,
            TunnelingServiceUtil tunnelingService,
            SshConnectivityUtil sshConnectivity,
            String sleepTimeStr) {

        this.computeInstanceConnectivity =
            new ComputeInstanceConnectivityUtil(tunnelingService, sshConnectivity);

        this.localCloudConnector = CloudConnectorFactory.getInstance().getCloudConnector(memberId);

        SharedOrderHolders sharedOrderHolders = SharedOrderHolders.getInstance();
        this.spawningOrderList = sharedOrderHolders.getSpawningOrdersList();

        this.sleepTime = Long.valueOf(sleepTimeStr);
    }

    @Override
    public void run() {
        boolean isActive = true;
        while (isActive) {
            try {
                Order order = this.spawningOrderList.getNext();
                if (order != null) {
                    processSpawningOrder(order);
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
     * @throws FogbowManagerException
     */
    private void processInstance(Order order) throws FogbowManagerException {
        Instance instance = this.localCloudConnector.getInstance(order);
        InstanceType instanceType = order.getType();

        InstanceState instanceState = instance.getState();

        if (instanceState.equals(InstanceState.FAILED)) {
            LOGGER.debug(
                "The compute instance state is failed for order [" + order.getId() + "]");
            OrderStateTransitioner.transition(order, OrderState.FAILED);

        } else if (instanceState.equals(InstanceState.READY)) {
            LOGGER
                .debug("Processing active compute instance for order [" + order.getId() + "]");

            if (instanceType.equals(InstanceType.COMPUTE)) {
                SshTunnelConnectionData sshTunnelConnectionData = this.computeInstanceConnectivity
                    .getSshTunnelConnectionData(order.getId());
                if (sshTunnelConnectionData != null) {
                    boolean instanceReachable = this.computeInstanceConnectivity
                        .isInstanceReachable(sshTunnelConnectionData);
                    if (!instanceReachable) {
                        // try again later
                        return;
                    }
                }
            }

            OrderStateTransitioner.transition(order, OrderState.FULFILLED);
        }
    }
}
