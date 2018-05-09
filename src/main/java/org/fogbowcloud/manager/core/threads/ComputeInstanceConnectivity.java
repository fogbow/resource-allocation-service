package org.fogbowcloud.manager.core.threads;

import org.apache.log4j.Logger;
import org.fogbowcloud.manager.core.models.orders.Order;
import org.fogbowcloud.manager.core.models.orders.instances.ComputeOrderInstance;
import org.fogbowcloud.manager.core.utils.SshConnectivityUtil;
import org.fogbowcloud.manager.core.utils.TunnelingServiceUtil;

import java.util.Map;

public class ComputeInstanceConnectivity {

    private static final Logger LOGGER = Logger.getLogger(FulfilledMonitor.class);

    private TunnelingServiceUtil tunnelingService;
    private SshConnectivityUtil sshConnectivity;

    public ComputeInstanceConnectivity(TunnelingServiceUtil tunnelingService, SshConnectivityUtil sshConnectivity) {
        this.tunnelingService = tunnelingService;
        this.sshConnectivity = sshConnectivity;
    }

    /**
     * This method does not synchronize the order object because it is private and
     * can only be called by the processInstance method.
     */
    public void setTunnelingServiceAddresses(Order order, ComputeOrderInstance computeOrderInstance) {
        try {
            Map<String, String> externalServiceAddresses = this.tunnelingService.getExternalServiceAddresses(order.getId());
            if (externalServiceAddresses != null) {
                computeOrderInstance.setExternalServiceAddresses(externalServiceAddresses);
            }
        } catch (Throwable e) {
            LOGGER.error("Error trying to get map of addresses (IP and Port) " +
                    "of the compute instance for order: " + order, e);
        }
    }

    /**
     * This method does not synchronize the order object because it is private and
     * can only be called by the processInstance method.
     */
    public boolean isActiveConnectionFromInstance(ComputeOrderInstance computeOrderInstance) {
        LOGGER.info("Check the communicate at SSH connectivity of the compute instance.");
        if (this.sshConnectivity.checkSSHConnectivity(computeOrderInstance)) {
            return true;
        }

        LOGGER.warn("Failed attempt to communicate with ssh connectivity.");
        return false;
    }
}
