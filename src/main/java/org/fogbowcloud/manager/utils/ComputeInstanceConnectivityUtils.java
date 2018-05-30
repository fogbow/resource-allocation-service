package org.fogbowcloud.manager.utils;

import java.util.Map;

import org.apache.log4j.Logger;
import org.fogbowcloud.manager.core.manager.constants.CommonConfigurationConstants;
import org.fogbowcloud.manager.core.models.SshTunnelConnectionData;
import org.json.JSONObject;

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

    public boolean isInstanceReachable(SshTunnelConnectionData sshTunnelConnectionData) {
        LOGGER.debug("Checking the connectivity to the compute instance through SSH.");
        return this.sshConnectivity.checkSSHConnectivity(sshTunnelConnectionData);
    }

    public SshTunnelConnectionData getSshTunnelConnectionData(String orderId) {
        try {
            Map<String, String> serviceAddresses =
                this.tunnelingService.getExternalServiceAddresses(orderId);

            String sshPublicAddress =
                serviceAddresses.get(CommonConfigurationConstants.SSH_SERVICE_NAME);
            String sshUserName = SshCommonUserUtil.getSshCommonUser();
            String sshExtraPorts = new JSONObject(serviceAddresses).toString();

            return new SshTunnelConnectionData(sshPublicAddress, sshUserName, sshExtraPorts);
        } catch (Throwable e) {
            LOGGER.error(
                "Error trying to get map of addresses (IP and Port) "
                    + "of the compute instance for orderId: "
                    + orderId,
                e);
            return null;
        }
    }
}
