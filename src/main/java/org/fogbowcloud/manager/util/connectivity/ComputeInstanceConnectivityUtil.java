package org.fogbowcloud.manager.util.connectivity;

import java.util.Map;

import org.apache.log4j.Logger;
import org.fogbowcloud.manager.core.constants.DefaultConfigurationConstants;
import org.json.JSONObject;

/** Class to check SSH connectivity with an compute instance. */
public class ComputeInstanceConnectivityUtil {

    private static final Logger LOGGER = Logger.getLogger(ComputeInstanceConnectivityUtil.class);

    private TunnelingServiceUtil tunnelingService;
    private SshConnectivityUtil sshConnectivity;

    public ComputeInstanceConnectivityUtil(
            TunnelingServiceUtil tunnelingService, SshConnectivityUtil sshConnectivity) {
        this.tunnelingService = tunnelingService;
        this.sshConnectivity = sshConnectivity;
    }

    public boolean isInstanceReachable(SshTunnelConnectionData sshTunnelConnectionData) {
        LOGGER.debug("Checking the connectivity to the compute instance through SSH.");
        return this.sshConnectivity.checkSSHConnectivity(sshTunnelConnectionData);
    }

    public SshTunnelConnectionData getSshTunnelConnectionData(String orderId) {
        LOGGER.info("Getting tunnel connection data.");
        try {
            Map<String, String> serviceAddresses =
                this.tunnelingService.getExternalServiceAddresses(orderId);
            if (serviceAddresses != null) {
                String sshPublicAddress =
                        serviceAddresses.get(DefaultConfigurationConstants.SSH_SERVICE_NAME);
                String sshUserName = SshCommonUserUtil.getSshCommonUser();
                String sshExtraPorts = new JSONObject(serviceAddresses).toString();
                return new SshTunnelConnectionData(sshPublicAddress, sshUserName, sshExtraPorts);
            }
            return null;
        } catch (Throwable e) {
            LOGGER.error("Error trying to get map of addresses (IP and Port) "
                    + "of the compute instance for orderId: " + orderId, e);
            return null;
        }
    }
}
