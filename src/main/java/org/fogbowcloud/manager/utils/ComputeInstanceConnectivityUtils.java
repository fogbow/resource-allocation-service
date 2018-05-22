package org.fogbowcloud.manager.utils;

import java.util.Map;
import org.apache.log4j.Logger;
import org.fogbowcloud.manager.core.models.orders.Order;
import org.fogbowcloud.manager.core.models.orders.instances.ComputeOrderInstance;

/** Class to check SSH connectivity with an compute instance. */
public class ComputeInstanceConnectivityUtils {

    private static final Logger LOGGER = Logger.getLogger(ComputeInstanceConnectivityUtils.class);

    private TunnelingServiceUtil tunnelingService;
    private SshConnectivityUtil sshConnectivity;

    public ComputeInstanceConnectivityUtils(
            TunnelingServiceUtil tunnelingService, SshConnectivityUtil sshConnectivity) {
        this.tunnelingService = tunnelingService;
        this.sshConnectivity = sshConnectivity;
    }

    public void setTunnelingServiceAddresses(
            Order order, ComputeOrderInstance computeOrderInstance) {
        try {
            Map<String, String> externalServiceAddresses =
                    this.tunnelingService.getExternalServiceAddresses(order.getId());
            if (externalServiceAddresses != null) {
                computeOrderInstance.setExternalServiceAddresses(externalServiceAddresses);
            }
        } catch (Throwable e) {
            LOGGER.error(
                    "Error trying to get map of addresses (IP and Port) "
                            + "of the compute instance for order: "
                            + order,
                    e);
        }
    }

    public boolean isInstanceReachable(ComputeOrderInstance computeOrderInstance) {
        LOGGER.debug("Checking the connectivity to the compute instance through SSH.");
        return this.sshConnectivity.checkSSHConnectivity(computeOrderInstance);
    }
}
