package org.fogbowcloud.manager.core.utils;

import org.apache.log4j.Logger;
import org.fogbowcloud.manager.core.models.orders.Order;
import org.fogbowcloud.manager.core.models.orders.instances.ComputeOrderInstance;
import org.fogbowcloud.manager.core.threads.FulfilledMonitor;

import java.util.Map;

/**
 * Class to check SSH connectivity with an compute instance.
 */
public class ComputeInstanceConnectivityUtil {

    private static final Logger LOGGER = Logger.getLogger(FulfilledMonitor.class);

    private TunnelingServiceUtil tunnelingService;
    private SshConnectivityUtil sshConnectivity;

    public ComputeInstanceConnectivityUtil(TunnelingServiceUtil tunnelingService, SshConnectivityUtil sshConnectivity) {
        this.tunnelingService = tunnelingService;
        this.sshConnectivity = sshConnectivity;
    }

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

    public boolean isActiveConnectionFromInstance(ComputeOrderInstance computeOrderInstance) {
        LOGGER.info("Check the communicate at SSH connectivity of the compute instance.");
        if (this.sshConnectivity.checkSSHConnectivity(computeOrderInstance)) {
            return true;
        }

        LOGGER.warn("Failed attempt to communicate with ssh connectivity.");
        return false;
    }
}
